package com.example.memora.worker
import com.example.memora.data.*

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.memora.data.MemoraDatabase
import com.example.memora.data.MemoraRepository

class ArchivePurgeWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val repository = MemoraRepository(MemoraDatabase.get(applicationContext).memoraDao())
            repository.purgeExpiredArchive()
            Log.d("ArchivePurgeWorker", "Automatic archive purge executed successfully.")
            Result.success()
        } catch (e: Exception) {
            Log.e("ArchivePurgeWorker", "Error executing automatic archive purge", e)
            Result.retry()
        }
    }
}
