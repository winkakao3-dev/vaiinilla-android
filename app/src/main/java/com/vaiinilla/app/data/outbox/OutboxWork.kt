package com.vaiinilla.app.data.outbox

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.vaiinilla.app.domain.outbox.DrainOutcome
import com.vaiinilla.app.domain.outbox.OutboxBackoff
import com.vaiinilla.app.domain.outbox.OutboxScheduler
import dagger.Module
import dagger.Provides
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@EntryPoint
@InstallIn(SingletonComponent::class)
interface OutboxEntryPoint {
    fun drainer(): OutboxDrainer

    fun scheduler(): OutboxScheduler
}

/**
 * Manda lo guardado sin internet aunque la app esté cerrada. Cada recorrido que deja algo pendiente agenda el
 * siguiente con la espera de [OutboxBackoff] (tope de 5 minutos), y sin red espera a que vuelva.
 */
class OutboxDrainWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val entry = EntryPointAccessors.fromApplication(applicationContext, OutboxEntryPoint::class.java)
        val attempt = inputData.getInt(KEY_ATTEMPT, 0)
        return when (entry.drainer().drain()) {
            DrainOutcome.DONE -> Result.success()
            DrainOutcome.RETRY_LATER -> {
                entry.scheduler().scheduleRetry(attempt + 1, OutboxBackoff.delayMillis(attempt))
                Result.success()
            }
        }
    }

    companion object {
        const val KEY_ATTEMPT = "attempt"
    }
}

class WorkManagerOutboxScheduler(
    private val context: Context,
) : OutboxScheduler {
    private val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

    override fun requestDrain() {
        enqueue(UNIQUE_NOW, ExistingWorkPolicy.APPEND_OR_REPLACE, attempt = 0, delayMillis = 0L)
    }

    override fun scheduleRetry(
        attempt: Int,
        delayMillis: Long,
    ) {
        enqueue(UNIQUE_RETRY, ExistingWorkPolicy.REPLACE, attempt, delayMillis)
    }

    private fun enqueue(
        name: String,
        policy: ExistingWorkPolicy,
        attempt: Int,
        delayMillis: Long,
    ) {
        // Sin WorkManager (pruebas de pantalla sin inicializar) la bandeja sigue mandando con la app abierta.
        runCatching {
            val request =
                OneTimeWorkRequestBuilder<OutboxDrainWorker>()
                    .setConstraints(constraints)
                    .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, OutboxBackoff.FIRST_DELAY_MS, TimeUnit.MILLISECONDS)
                    .setInputData(Data.Builder().putInt(OutboxDrainWorker.KEY_ATTEMPT, attempt).build())
                    .build()
            WorkManager.getInstance(context).enqueueUniqueWork(name, policy, request)
        }
    }

    private companion object {
        const val UNIQUE_NOW = "outbox-drain"
        const val UNIQUE_RETRY = "outbox-drain-retry"
    }
}

@Module
@InstallIn(SingletonComponent::class)
object OutboxModule {
    @Provides
    @Singleton
    fun provideOutboxScheduler(
        @ApplicationContext context: Context,
    ): OutboxScheduler = WorkManagerOutboxScheduler(context)
}
