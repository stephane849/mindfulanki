package com.mindfulanki.core.template

/**
 * Converts Anki's HTML field/template output into clean plain text for the
 * E Ink reviewer. Pure Kotlin (no android.text.Html) so it is fully testable
 * on the JVM and behaves identically on-device.
 */
object HtmlStripper {

    private val STYLE_OR_SCRIPT = Regex("(?is)<(style|script)[^>]*>.*?</\\1>")
    private val SOUND_TAG = Regex("\\[sound:[^]]*]")
    private val LINE_BREAK = Regex("(?i)<\\s*(br|hr)[^>]*?/?>")
    private val BLOCK_END = Regex("(?i)</\\s*(div|p|li|tr|h[1-6])\\s*>")
    private val ANY_TAG = Regex("<[^>]+>")
    private val MANY_NEWLINES = Regex("\\n{3,}")
    private val TRAILING_SPACES = Regex("[ \\t]+\\n")

    fun strip(html: String): String {
        var s = html
        s = STYLE_OR_SCRIPT.replace(s, "")
        s = SOUND_TAG.replace(s, "")
        s = LINE_BREAK.replace(s, "\n")
        s = BLOCK_END.replace(s, "\n")
        s = ANY_TAG.replace(s, "")
        s = decodeEntities(s)
        s = TRAILING_SPACES.replace(s, "\n")
        s = MANY_NEWLINES.replace(s, "\n\n")
        return s.trim()
    }

    private fun decodeEntities(input: String): String {
        if (!input.contains('&')) return input
        val out = StringBuilder(input.length)
        var i = 0
        while (i < input.length) {
            val c = input[i]
            if (c == '&') {
                val semi = input.indexOf(';', i + 1)
                if (semi in (i + 1)..(i + 12)) {
                    val entity = input.substring(i + 1, semi)
                    val decoded = decodeEntity(entity)
                    if (decoded != null) {
                        out.append(decoded)
                        i = semi + 1
                        continue
                    }
                }
            }
            out.append(c)
            i++
        }
        return out.toString()
    }

    private fun decodeEntity(entity: String): String? = when {
        entity == "nbsp" -> " "
        entity == "amp" -> "&"
        entity == "lt" -> "<"
        entity == "gt" -> ">"
        entity == "quot" -> "\""
        entity == "apos" -> "'"
        entity.startsWith("#x") || entity.startsWith("#X") ->
            entity.substring(2).toIntOrNull(16)?.let { codePointToString(it) }
        entity.startsWith("#") ->
            entity.substring(1).toIntOrNull()?.let { codePointToString(it) }
        else -> null
    }

    private fun codePointToString(cp: Int): String? =
        if (cp in 0..0x10FFFF) String(Character.toChars(cp)) else null
}
