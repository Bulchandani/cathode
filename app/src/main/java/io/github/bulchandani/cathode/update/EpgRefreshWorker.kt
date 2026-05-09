package io.github.bulchandani.cathode.update

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import io.github.bulchandani.cathode.data.epg.EpgRepo
import io.github.bulchandani.cathode.data.store.SourcesStore
import java.util.concurrent.TimeUnit

/**
 * Refreshes EPG for the active source every ~6 hours, even when the
 * app process isn't running. Schedule once on app launch via
 * [schedule]; WorkManager will keep firing in the background. Skips
 * gracefully if no active source or no network.
 */
class EpgRefreshWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        SourcesStore.init(applicationContext)
        EpgRepo.init(applicationContext)
        val source = SourcesStore.active.value ?: return Result.success()
        EpgRepo.load(source.host, source.user, source.pass, force = true)
        return Result.success()
    }

    companion object {
        private const val NAME = "cathode_epg_refresh"

        fun schedule(context: Context) {
            val req = PeriodicWorkRequestBuilder<EpgRefreshWorker>(6, TimeUnit.HOURS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build(),
                )
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                req,
            )
        }
    }
}
