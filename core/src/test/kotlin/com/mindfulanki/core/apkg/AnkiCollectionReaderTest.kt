package com.mindfulanki.core.apkg

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.sql.DriverManager

class AnkiCollectionReaderTest {

    @Test
    fun `legacy and modern schemas produce identical rendered cards`(@TempDir dir: File) {
        val legacyCards = readAndAssemble(CollectionFixtures.legacy(dir))
        val modernCards = readAndAssemble(CollectionFixtures.modern(dir))

        assertEquals(legacyCards, modernCards, "both schemas should yield the same cards")
    }

    @Test
    fun `legacy reader renders fronts, backs and deck names`(@TempDir dir: File) {
        val cards = readAndAssemble(CollectionFixtures.legacy(dir)).sortedBy { it.front }

        assertEquals(2, cards.size)
        assertEquals("Spanish", cards[0].deckName)
        assertEquals("Gracias", cards[0].front)
        assertEquals("Gracias\nThank you", cards[0].back) // FrontSide + <hr> -> newline
        assertEquals("Hola", cards[1].front)
        assertEquals("Hola\nHello", cards[1].back)
    }

    @Test
    fun `modern reader decodes protobuf template config and picks ModernReaderV18`(@TempDir dir: File) {
        val db = CollectionFixtures.modern(dir)
        DriverManager.getConnection("jdbc:sqlite:${db.absolutePath}").use { conn ->
            val querier = JdbcSqlQuerier(conn)
            assertTrue(AnkiCollectionReader.forDatabase(querier) is ModernReaderV18)
        }
    }

    private fun readAndAssemble(db: File): List<ImportedCard> =
        DriverManager.getConnection("jdbc:sqlite:${db.absolutePath}").use { conn ->
            val querier = JdbcSqlQuerier(conn)
            val collection = AnkiCollectionReader.forDatabase(querier).read(querier)
            CollectionAssembler().assemble(collection)
        }
}
