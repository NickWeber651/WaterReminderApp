package de.nick.waterreminderapp

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

// ---------------------------------------------------------------------------
// WaterReminderWorker – Stub
// Wird in einem späteren Schritt vollständig implementiert.
//
// inputData-Keys:
//   KEY_SOURCE → String – z.B. "snooze" oder "scheduled"
// ---------------------------------------------------------------------------
class WaterReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_SOURCE = "source"
    }

    override suspend fun doWork(): Result {
        // Wird später implementiert
        return Result.success()
    }
}

