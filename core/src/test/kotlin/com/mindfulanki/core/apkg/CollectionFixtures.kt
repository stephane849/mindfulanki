package com.mindfulanki.core.apkg

import java.io.File
import java.sql.Connection
import java.sql.DriverManager

/**
 * Builds equivalent legacy (v11) and modern (v18) Anki collection databases
 * representing the *same* logical deck, so tests can assert both readers produce
 * identical results.
 *
 * Logical collection:
 *  - Deck 1 = "Spanish"
 *  - Note type 1001 "Basic": fields [Front, Back]
 *      template 0: qfmt="{{Front}}", afmt="{{FrontSide}}<hr id=answer>{{Back}}"
 *  - Note 2001 = (Hola, Hello), Note 2002 = (Gracias, Thank you)
 *  - One card per note on template 0, both in deck 1
 */
object CollectionFixtures {

    const val US = '' // unit separator between note fields

    private const val QFMT = "{{Front}}"
    private const val AFMT = "{{FrontSide}}<hr id=answer>{{Back}}"

    fun legacy(dir: File): File = build(dir, "legacy.anki2") { conn ->
        conn.exec(
            """
            CREATE TABLE col (id INTEGER PRIMARY KEY, ver INTEGER, models TEXT, decks TEXT);
            CREATE TABLE notes (id INTEGER PRIMARY KEY, mid INTEGER, flds TEXT);
            CREATE TABLE cards (id INTEGER PRIMARY KEY, nid INTEGER, ord INTEGER, did INTEGER);
            """.trimIndent(),
        )
        val models = """
            {"1001":{"id":1001,"name":"Basic",
              "flds":[{"name":"Front","ord":0},{"name":"Back","ord":1}],
              "tmpls":[{"name":"Card 1","ord":0,"qfmt":${jsonStr(QFMT)},"afmt":${jsonStr(AFMT)}}]}}
        """.trimIndent()
        val decks = """{"1":{"id":1,"name":"Spanish"}}"""
        conn.prepareStatement("INSERT INTO col(id,ver,models,decks) VALUES (1,11,?,?)").use {
            it.setString(1, models)
            it.setString(2, decks)
            it.executeUpdate()
        }
        insertNotesAndCards(conn)
    }

    fun modern(dir: File): File = build(dir, "modern.anki21b.sqlite") { conn ->
        conn.exec(
            """
            CREATE TABLE col (id INTEGER PRIMARY KEY, ver INTEGER, models TEXT, decks TEXT);
            CREATE TABLE decks (id INTEGER PRIMARY KEY, name TEXT);
            CREATE TABLE notetypes (id INTEGER PRIMARY KEY, name TEXT);
            CREATE TABLE fields (ntid INTEGER, ord INTEGER, name TEXT);
            CREATE TABLE templates (ntid INTEGER, ord INTEGER, name TEXT, config BLOB);
            CREATE TABLE notes (id INTEGER PRIMARY KEY, mid INTEGER, flds TEXT);
            CREATE TABLE cards (id INTEGER PRIMARY KEY, nid INTEGER, ord INTEGER, did INTEGER);
            """.trimIndent(),
        )
        conn.exec("INSERT INTO col(id,ver,models,decks) VALUES (1,18,'{}','{}')")
        conn.exec("INSERT INTO decks(id,name) VALUES (1,'Spanish')")
        conn.exec("INSERT INTO notetypes(id,name) VALUES (1001,'Basic')")
        conn.exec("INSERT INTO fields(ntid,ord,name) VALUES (1001,0,'Front'),(1001,1,'Back')")
        // q_format=1, a_format=2 inside CardTemplateConfig
        val config = ProtoWriter.message(1 to QFMT, 2 to AFMT)
        conn.prepareStatement("INSERT INTO templates(ntid,ord,name,config) VALUES (1001,0,'Card 1',?)").use {
            it.setBytes(1, config)
            it.executeUpdate()
        }
        insertNotesAndCards(conn)
    }

    private fun insertNotesAndCards(conn: Connection) {
        conn.exec(
            "INSERT INTO notes(id,mid,flds) VALUES " +
                "(2001,1001,'Hola${US}Hello'),(2002,1001,'Gracias${US}Thank you')",
        )
        conn.exec(
            "INSERT INTO cards(id,nid,ord,did) VALUES (3001,2001,0,1),(3002,2002,0,1)",
        )
    }

    private fun build(dir: File, name: String, populate: (Connection) -> Unit): File {
        val file = File(dir, name)
        DriverManager.getConnection("jdbc:sqlite:${file.absolutePath}").use(populate)
        return file
    }

    private fun Connection.exec(sql: String) = createStatement().use { st ->
        sql.split(";").map { it.trim() }.filter { it.isNotEmpty() }.forEach { st.executeUpdate(it) }
    }

    private fun jsonStr(s: String): String =
        "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
}
