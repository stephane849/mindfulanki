package com.mindfulanki.data.apkg

import android.database.sqlite.SQLiteDatabase
import com.mindfulanki.core.apkg.SqlQuerier

/**
 * [SqlQuerier] backed by Android's [SQLiteDatabase], used to read the SQLite
 * collection extracted from an `.apkg`. Mirrors the JDBC-backed querier used in
 * core's tests so the readers behave identically.
 */
class AndroidSqlQuerier(private val db: SQLiteDatabase) : SqlQuerier {

    override fun <T> query(sql: String, map: (SqlQuerier.Row) -> T): List<T> {
        db.rawQuery(sql, null).use { cursor ->
            val out = ArrayList<T>(cursor.count)
            val row = object : SqlQuerier.Row {
                override fun getString(column: String): String? {
                    val i = cursor.getColumnIndexOrThrow(column)
                    return if (cursor.isNull(i)) null else cursor.getString(i)
                }

                override fun getLong(column: String): Long =
                    cursor.getLong(cursor.getColumnIndexOrThrow(column))

                override fun getBlob(column: String): ByteArray? {
                    val i = cursor.getColumnIndexOrThrow(column)
                    return if (cursor.isNull(i)) null else cursor.getBlob(i)
                }
            }
            while (cursor.moveToNext()) out += map(row)
            return out
        }
    }
}
