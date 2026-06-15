package com.mindfulanki.core.template

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TemplateRendererTest {

    private val renderer = TemplateRenderer()

    @Test
    fun `substitutes fields and strips html`() {
        val out = renderer.render("{{Front}}", mapOf("Front" to "<b>Bonjour</b>"))
        assertEquals("Bonjour", out)
    }

    @Test
    fun `embeds FrontSide and br becomes newline`() {
        val out = renderer.render(
            "{{FrontSide}}<br>{{Back}}",
            mapOf("FrontSide" to "Hola", "Back" to "Hello"),
        )
        assertEquals("Hola\nHello", out)
    }

    @Test
    fun `positive conditional includes body only when field present`() {
        val template = "{{Front}}{{#Extra}} ({{Extra}}){{/Extra}}"

        val withExtra = renderer.render(template, mapOf("Front" to "Q", "Extra" to "note"))
        val withoutExtra = renderer.render(template, mapOf("Front" to "Q", "Extra" to ""))

        assertEquals("Q (note)", withExtra)
        assertEquals("Q", withoutExtra)
    }

    @Test
    fun `inverted conditional includes body only when field empty`() {
        val template = "{{^Back}}No answer{{/Back}}"

        assertEquals("No answer", renderer.render(template, mapOf("Back" to "")))
        assertEquals("", renderer.render(template, mapOf("Back" to "Yes")))
    }

    @Test
    fun `field modifier prefixes are ignored`() {
        val out = renderer.render("{{type:Answer}}", mapOf("Answer" to "42"))
        assertEquals("42", out)
    }

    @Test
    fun `missing field renders as empty string`() {
        val out = renderer.render("{{Front}}{{Missing}}", mapOf("Front" to "X"))
        assertEquals("X", out)
    }
}
