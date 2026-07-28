package com.guru2.payday.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.guru2.payday.R
import com.guru2.payday.data.local.ExpenseEntity
import com.guru2.payday.data.local.PaydayDatabase
import com.guru2.payday.ui.expense.ExpenseListActivity
import java.text.NumberFormat
import java.time.LocalDate
import java.util.Locale
import kotlinx.coroutines.runBlocking

/**
 * 예약 시각에 결제 예정 알림을 표시하고 다음 결제 회차의 알림을 다시 예약한다.
 */
class ExpenseNotificationWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : Worker(appContext, workerParams) {
    // 알림 권한을 확인한 뒤 정기 지출 목록으로 이동하는 알림을 표시한다.
    override fun doWork(): Result {
        val expenseId = inputData.getLong(KEY_EXPENSE_ID, 0L)
        val name = inputData.getString(KEY_NAME) ?: return Result.failure()
        val amount = inputData.getLong(KEY_AMOUNT, 0L)
        createNotificationChannel()

        val permissionGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        if (permissionGranted) {
            val intent = Intent(applicationContext, ExpenseListActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                applicationContext,
                expenseId.toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            val formattedAmount = NumberFormat
                .getNumberInstance(Locale.KOREA)
                .format(amount)
            val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_expense_notification)
                .setContentTitle(applicationContext.getString(R.string.payment_notification_title))
                .setContentText(
                    applicationContext.getString(
                        R.string.payment_notification_message,
                        name,
                        formattedAmount,
                    ),
                )
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()
            NotificationManagerCompat.from(applicationContext).notify(
                expenseId.toInt(),
                notification,
            )
        }

        scheduleFollowingNotification(expenseId)
        return Result.success()
    }

    // Android 8 이상에서 결제 예정 알림 채널을 생성한다.
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                applicationContext.getString(R.string.payment_notification_channel),
                NotificationManager.IMPORTANCE_HIGH,
            )
            applicationContext
                .getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    // 반복 주기에 따라 다음 결제일을 계산하고 다음 알림 작업을 예약한다.
    private fun scheduleFollowingNotification(expenseId: Long) = runBlocking {
        val dao = PaydayDatabase.getInstance(applicationContext).expenseDao()
        val expense = dao.getById(expenseId) ?: return@runBlocking
        val currentDate = expense.nextPaymentDate?.let(LocalDate::parse) ?: return@runBlocking
        val nextDate = if (expense.recurrence == ExpenseEntity.RECURRENCE_YEARLY) {
            currentDate.plusYears(1)
        } else {
            val nextMonth = currentDate.plusMonths(1)
            nextMonth.withDayOfMonth(
                (expense.recurringDay ?: nextMonth.dayOfMonth)
                    .coerceAtMost(nextMonth.lengthOfMonth()),
            )
        }
        val updated = expense.copy(nextPaymentDate = nextDate.toString())
        dao.update(updated)
        ExpenseNotificationScheduler.schedule(applicationContext, updated)
    }

    companion object {
        const val KEY_EXPENSE_ID = "expense_id"
        const val KEY_NAME = "expense_name"
        const val KEY_AMOUNT = "expense_amount"
        const val KEY_NEXT_DATE = "expense_next_date"
        const val KEY_RECURRENCE = "expense_recurrence"
        private const val CHANNEL_ID = "scheduled_expense"
    }
}
