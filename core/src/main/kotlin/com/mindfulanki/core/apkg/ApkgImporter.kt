package com.mindfulanki.core.apkg

import com.github.luben.zstd.ZstdInputStream
import java.io.File
import java.util.zip.ZipInputStream

/**
 * Extracts the SQLite collection database out of an `.apkg` archive, handling
 * both export formats:
 *
 *  - **Modern** (`collection.anki21b`): zstd-compressed; decompressed here.
 *  - **Legacy** (`collection.anki2`): plain SQLite; copied out as-is.
 *
 * A modern package often also ships a stub `collection.anki2` for backward
 * compatibility, so we prefer `collection.anki21b` when both are present.
 *
 * Returns the path to a ready-to-open `.sqlite` file. The caller opens it with
 * a platform [SqlQuerier], then uses [AnkiCollectionReader.forDatabase] and
 * [CollectionAssembler]. (Opening SQLite differs per platform — JDBC on the
 * JVM, `SQLiteDatabase` on Android — so it is intentionally left to the caller.)
 */
class ApkgImporter {

    data class Extracted(val database: File, val modern: Boolean)

    /**
     * @param apkg    the `.apkg` file (a ZIP archive)
     * @param workDir a writable directory for the extracted database
     */
    fun extract(apkg: File, workDir: File): Extracted {
        workDir.mkdirs()
        var legacy: File? = null
        var modern: File? = null

        apkg.inputStream().use { raw ->
            ZipInputStream(raw.buffered()).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    when (entry.name) {
                        MODERN_DB -> modern = zip.copyEntryTo(File(workDir, MODERN_DB))
                        LEGACY_DB -> legacy = zip.copyEntryTo(File(workDir, LEGACY_DB))
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        }

        val out = File(workDir, "collection.sqlite")
        return when {
            modern != null -> {
                decompressZstd(modern!!, out)
                Extracted(out, modern = true)
            }
            legacy != null -> {
                legacy!!.copyTo(out, overwrite = true)
                Extracted(out, modern = false)
            }
            else -> error("No Anki collection found in ${apkg.name} (expected $MODERN_DB or $LEGACY_DB)")
        }
    }

    private fun decompressZstd(source: File, target: File) {
        source.inputStream().use { input ->
            ZstdInputStream(input.buffered()).use { zstd ->
                target.outputStream().use { out -> zstd.copyTo(out) }
            }
        }
    }

    private fun ZipInputStream.copyEntryTo(target: File): File {
        target.outputStream().use { out -> copyTo(out) }
        return target
    }

    private companion object {
        const val MODERN_DB = "collection.anki21b"
        const val LEGACY_DB = "collection.anki2"
    }
}
