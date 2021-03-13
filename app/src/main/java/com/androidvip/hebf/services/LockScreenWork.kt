package com.androidvip.hebf.services

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.work.*
import com.androidvip.hebf.receivers.UnlockReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class LockScreenWork(context: Context, params: WorkerParameters): CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        unregisterReceiver(applicationContext)
        return@withContext registerUnlockReceiver(applicationContext)
    }

    companion object {
        private const val WORK_TAG = "LOCK_SCREEN_WORK_REQUEST"
        var receiver: UnlockReceiver? = null

        fun registerUnlockReceiver(context: Context): Result {
            return runCatching {
                receiver = UnlockReceiver()
                IntentFilter(Intent.ACTION_SCREEN_OFF).apply {
                    addAction(Intent.ACTION_SCREEN_ON)
                    context.applicationContext.registerReceiver(receiver, this)
                }
                Result.success()
            }.getOrDefault(Result.failure())
        }

        fun unregisterReceiver(context: Context) {
            try {
                if (receiver != null) {
                    context.applicationContext.unregisterReceiver(receiver)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                receiver = null
            }
        }

        fun scheduleJobPeriodic(context: Context?) {
            if (context == null) return

            val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                    .setRequiresCharging(false)
                    .build()

            val request = PeriodicWorkRequest.Builder(
                    LockScreenWork::class.java,
                    PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS,
                    TimeUnit.MILLISECONDS,
                    PeriodicWorkRequest.MIN_PERIODIC_FLEX_MILLIS,
                    TimeUnit.MILLISECONDS
            ).setConstraints(constraints).build()

            val workManager = WorkManager.getInstance(context.applicationContext)
            workManager.enqueueUniquePeriodicWork(
                    WORK_TAG,
                    ExistingPeriodicWorkPolicy.KEEP,
                    request
            )
        }

        fun cancelJob(context: Context) {
            WorkManager.getInstance(context.applicationContext).cancelAllWorkByTag(WORK_TAG)
            unregisterReceiver(context)
        }
    }

}