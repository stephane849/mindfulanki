package com.mindfulanki.core.template

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class HtmlStripperTest {

    @Test
    fun `removes tags and decodes entities`() {
        val out = HtmlStripper.strip("<p>Caf&#233; &amp; <i>th&#233;</i></p>")
        assertEquals("Café & thé", out)
    }

    @Test
    fun `named and numeric entities decode`() {
        assertEquals("a < b & c > d", HtmlStripper.strip("a &lt; b &amp; c &gt; d"))
        assertEquals("A", HtmlStripper.strip("&#65;"))
        assertEquals("A", HtmlStripper.strip("&#x41;"))
        assertEquals("a b", HtmlStripper.strip("a&nbsp;b"))
    }

    @Test
    fun `block elements and breaks produce newlines`() {
        assertEquals("one\ntwo", HtmlStripper.strip("one<br>two"))
        assertEquals("one\ntwo", HtmlStripper.strip("<div>one</div><div>two</div>"))
    }

    @Test
    fun `strips sound references and script blocks`() {
        assertEquals("word", HtmlStripper.strip("word[sound:audio.mp3]"))
        assertEquals("visible", HtmlStripper.strip("<script>alert(1)</script>visible"))
    }

    @Test
    fun `collapses excessive blank lines`() {
        assertEquals("a\n\nb", HtmlStripper.strip("a<br><br><br><br>b"))
    }
}
