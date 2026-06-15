package com.mindfulanki.core.apkg

/**
 * Minimal read-only SQL access used by the collection readers.
 *
 * Abstracted so the parsing logic stays platform-agnostic: tests back it with
 * sqlite-jdbc, while the Android app backs it with `android.database.sqlite`.
 */
interface SqlQuerier {

    /** Run [sql] and map each result row with [map]. */
    fun <T> query(sql: String, map: (Row) -> T): List<T>

    /** Whether a table with [name] exists in the database. */
    fun hasTable(name: String): Boolean =
        query("SELECT name FROM sqlite_master WHERE type='table' AND name='$name'") {
            it.getString("name")
        }.isNotEmpty()

    /** A single result row, addressed by column name. */
    interface Row {
        fun getString(column: String): String?
        fun getLong(column: String): Long
        fun getBlob(column: String): ByteArray?
    }
}
