package com.mindfulanki.core.apkg

import java.sql.Connection

/** A [SqlQuerier] backed by a JDBC connection — used to exercise readers in tests. */
class JdbcSqlQuerier(private val connection: Connection) : SqlQuerier {

    override fun <T> query(sql: String, map: (SqlQuerier.Row) -> T): List<T> {
        connection.prepareStatement(sql).use { stmt ->
            stmt.executeQuery().use { rs ->
                val out = mutableListOf<T>()
                val row = object : SqlQuerier.Row {
                    override fun getString(column: String): String? = rs.getString(column)
                    override fun getLong(column: String): Long = rs.getLong(column)
                    override fun getBlob(column: String): ByteArray? = rs.getBytes(column)
                }
                while (rs.next()) out += map(row)
                return out
            }
        }
    }
}
