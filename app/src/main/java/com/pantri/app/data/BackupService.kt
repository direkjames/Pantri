package com.pantri.app.data

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import android.provider.DocumentsContract
import com.pantri.app.domain.isBackupDue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.IOException
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private const val PREFIX = "Pantri-backup-"
private const val SAFETY_PREFIX = "Pantri-before-restore-"
private const val KEEP = 7
private const val KEEP_SAFETY = 3
private const val MAX_BYTES = 10 * 1024 * 1024 // real backups are a few hundred KB; a bigger file is not ours and could run the phone out of memory

fun BackupData.isEmpty() = lists.isEmpty() && recipes.isEmpty() && stores.isEmpty() && products.isEmpty() && meals.isEmpty()

/**
 * Writes backups to a folder the user picked (no storage permission needed, no internet) and reads them back.
 * Nothing runs in the background: the app checks [autoBackupIfDue] when it opens, which is enough because
 * your data only changes while you use the app.
 */
class BackupService(private val context: Context, private val repo: PantriRepository) {
    private val mutex = Mutex()

    /** Backs up now. Never throws; the failure is saved so Settings can show it, and returned for a snackbar. */
    suspend fun backupNow(): Result<String> = write(PREFIX, skipIfEmpty = false)

    suspend fun autoBackupIfDue() {
        val s = repo.settingsFlow.first()
        if (s.backupFolder == null) return
        val zone = ZoneId.systemDefault()
        val last = if (s.lastBackupAt > 0) Instant.ofEpochMilli(s.lastBackupAt).atZone(zone).toLocalDate() else null
        if (isBackupDue(LocalDate.now(zone), last, s.backupDays, s.backupWeekday)) write(PREFIX, skipIfEmpty = true)
    }

    /** Reads and checks a backup file without changing anything. */
    suspend fun read(uri: Uri): BackupData = withContext(Dispatchers.IO) {
        val bytes = try {
            context.contentResolver.openInputStream(uri)?.use { it.readCapped() }
        } catch (e: IOException) { null } catch (e: SecurityException) { null }
        parseBackup((bytes ?: throw BackupException("Couldn't open that file.")).toString(Charsets.UTF_8))
    }

    /**
     * Replaces everything with [data]. When a backup folder is set, a safety copy of what is there now is saved first,
     * and the restore is cancelled if that copy cannot be written. Without a folder there is no safety copy.
     */
    suspend fun restore(data: BackupData) {
        if (repo.settingsFlow.first().backupFolder != null) {
            write(SAFETY_PREFIX, skipIfEmpty = true, record = false).getOrElse {
                throw BackupException("Couldn't save a safety copy of your current data first, so nothing was changed. ${it.message}")
            }
        }
        repo.restoreBackup(data)
    }

    private suspend fun write(prefix: String, skipIfEmpty: Boolean, record: Boolean = true): Result<String> = mutex.withLock {
        withContext(Dispatchers.IO) {
            try {
                val folder = repo.settingsFlow.first().backupFolder ?: throw BackupException("Choose a backup folder first.")
                val data = repo.exportBackup()
                if (skipIfEmpty && data.isEmpty()) return@withContext Result.success("Nothing to back up yet")
                val tree = folder.toUri()
                val cr = context.contentResolver
                val dir = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
                val name = prefix + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmmss")) + ".json"
                val bytes = data.toJson().toByteArray(Charsets.UTF_8)
                val doc = DocumentsContract.createDocument(cr, dir, "application/json", name) ?: throw IOException("The folder refused the file")
                cr.openOutputStream(doc, "wt")!!.use { it.write(bytes) }
                // Read it back: a backup you cannot read is worse than none.
                val ok = cr.openInputStream(doc)?.use { it.readCapped().contentEquals(bytes) } == true
                if (!ok) {
                    runCatching { DocumentsContract.deleteDocument(cr, doc) }
                    throw IOException("The saved file did not match")
                }
                prune(tree, prefix, if (prefix == PREFIX) KEEP else KEEP_SAFETY)
                if (record) repo.setBackupResult(System.currentTimeMillis(), null)
                Result.success(name)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val msg = if (e is BackupException) e.message!! else "Couldn't write to the backup folder. Choose it again in Settings."
                if (record) runCatching { repo.setBackupResult(System.currentTimeMillis(), msg) }
                Result.failure(BackupException(msg))
            }
        }
    }

    /** Keeps the newest [keep] files that start with [prefix] and deletes older ones. Failing to tidy up never fails a backup. */
    private fun prune(tree: Uri, prefix: String, keep: Int) = runCatching {
        val cr = context.contentResolver
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
        val mine = mutableListOf<Pair<String, String>>()
        cr.query(children, arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME), null, null, null)?.use { c ->
            while (c.moveToNext()) {
                val n = c.getString(1) ?: continue
                if (n.startsWith(prefix) && n.endsWith(".json")) mine += c.getString(0) to n
            }
        }
        mine.sortedByDescending { it.second }.drop(keep).forEach {
            runCatching { DocumentsContract.deleteDocument(cr, DocumentsContract.buildDocumentUriUsingTree(tree, it.first)) }
        }
    }
}

private fun java.io.InputStream.readCapped(): ByteArray {
    val out = java.io.ByteArrayOutputStream()
    val buf = ByteArray(64 * 1024)
    while (true) {
        val n = read(buf)
        if (n < 0) break
        out.write(buf, 0, n)
        if (out.size() > MAX_BYTES) throw BackupException("That file is too big to be a Pantri backup.")
    }
    return out.toByteArray()
}
