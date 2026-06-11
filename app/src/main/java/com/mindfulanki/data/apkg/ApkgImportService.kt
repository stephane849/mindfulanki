package com.mindfulanki.data.apkg

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import com.mindfulanki.core.apkg.AnkiCollectionReader
import com.mindfulanki.core.apkg.ApkgImporter
import com.mindfulanki.core.apkg.CollectionAssembler
import com.mindfulanki.core.apkg.ImportedCard
import com.mindfulanki.data.db.AppDatabase
import com.mindfulanki.data.db.CardEntity
import com.mindfulanki.data.db.DeckEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Outcome of importing an `.apkg`, surfaced to the UI. */
data class ImportSummary(val deckCount: Int, val cardCount: Int, val modern: Boolean)

/**
 * Imports an `.apkg` (chosen via the system file picker) into the local Room
 * database. Reuses the platform-agnostic core pipeline: [ApkgImporter] (unzip +
 * zstd), [AnkiCollectionReader] (legacy/modern schema), [CollectionAssembler]
 * (template rendering). Fresh FSRS state is created per card — Anki's own
 * scheduling is intentionally not imported.
 */
class ApkgImportService(
    private val context: Context,
    private val db: AppDatabase = AppDatabase.get(context),
) {
    suspend fun import(apkgUri: Uri): ImportSummary = withContext(Dispatchers.IO) {
        val workDir = File(context.cacheDir, "apkg-import-${System.currentTimeMillis()}")
        try {
            val apkgFile = copyToCache(apkgUri, workDir)
            val extracted = ApkgImporter().extract(apkgFile, workDir)

            val cards = readCards(extracted.database)
            persist(cards)

            ImportSummary(
                deckCount = cards.map { it.deckName }.distinct().size,
                cardCount = cards.size,
                modern = extracted.modern,
            )
        } finally {
            workDir.deleteRecursively()
        }
    }

    private fun readCards(database: File): List<ImportedCard> {
        val sqlite = SQLiteDatabase.openDatabase(
            database.absolutePath,
            null,
            SQLiteDatabase.OPEN_READONLY,
        )
        return sqlite.use {
            val querier = AndroidSqlQuerier(it)
            val collection = AnkiCollectionReader.forDatabase(querier).read(querier)
            CollectionAssembler().assemble(collection)
        }
    }

    private suspend fun persist(cards: List<ImportedCard>) {
        val now = System.currentTimeMillis()
        val deckDao = db.deckDao()
        val cardDao = db.cardDao()
        // Resolve (creating if needed) a deck id per distinct deck name.
        val deckIds = HashMap<String, Long>()
        for (name in cards.map { it.deckName }.distinct()) {
            deckIds[name] = deckDao.findByName(name) ?: deckDao.insert(DeckEntity(name = name))
        }
        cardDao.insertAll(
            cards.map { card ->
                CardEntity(
                    deckId = deckIds.getValue(card.deckName),
                    front = card.front,
                    back = card.back,
                    dueEpochMillis = now, // new cards are due immediately
                )
            },
        )
    }

    private fun copyToCache(uri: Uri, workDir: File): File {
        workDir.mkdirs()
        val target = File(workDir, "deck.apkg")
        context.contentResolver.openInputStream(uri)?.use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        } ?: error("Could not open $uri")
        return target
    }
}
