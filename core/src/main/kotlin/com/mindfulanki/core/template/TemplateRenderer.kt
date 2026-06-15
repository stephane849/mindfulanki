package com.mindfulanki.core.template

/**
 * Renders Anki card templates to plain text.
 *
 * Supports the subset of Anki's mustache-like syntax that matters for a
 * text-only reviewer:
 *  - `{{Field}}`               field substitution
 *  - `{{prefix:Field}}`        field modifiers (type:/hint:/cloze:/text:) — the
 *                              prefix is stripped and the field value is used
 *  - `{{#Field}} ... {{/Field}}`  shown when the field is non-empty
 *  - `{{^Field}} ... {{/Field}}`  shown when the field is empty
 *  - `{{FrontSide}}`           supplied by the caller for back templates
 *
 * The final output is HTML-stripped via [HtmlStripper]. Cloze deletion is
 * intentionally simplified (markers removed) and full cloze support is future
 * work.
 */
class TemplateRenderer {

    /**
     * @param template the qfmt or afmt string from the note type
     * @param fields   field name -> raw (HTML) field value; include "FrontSide"
     *                 when rendering a back template
     */
    fun render(template: String, fields: Map<String, String>): String {
        val tokens = tokenize(template)
        val raw = StringBuilder()
        evaluate(tokens, 0, tokens.size, fields, raw)
        return HtmlStripper.strip(raw.toString())
    }

    // --- evaluation ---------------------------------------------------------

    /** Evaluate tokens in [start, end) appending rendered output to [out]. */
    private fun evaluate(
        tokens: List<Token>,
        start: Int,
        end: Int,
        fields: Map<String, String>,
        out: StringBuilder,
    ) {
        var i = start
        while (i < end) {
            when (val t = tokens[i]) {
                is Token.Text -> out.append(t.value)
                is Token.Var -> out.append(lookup(t.name, fields))
                is Token.SectionOpen -> {
                    val close = matchingClose(tokens, i, end)
                    val present = fieldIsNonEmpty(t.name, fields)
                    val include = if (t.inverted) !present else present
                    if (include) evaluate(tokens, i + 1, close, fields, out)
                    i = close // skip to the close tag
                }
                is Token.SectionClose -> Unit // handled by matchingClose
            }
            i++
        }
    }

    private fun matchingClose(tokens: List<Token>, openIndex: Int, end: Int): Int {
        val name = (tokens[openIndex] as Token.SectionOpen).name
        var depth = 0
        for (j in openIndex + 1 until end) {
            val t = tokens[j]
            if (t is Token.SectionOpen && t.name == name) depth++
            else if (t is Token.SectionClose && t.name == name) {
                if (depth == 0) return j
                depth--
            }
        }
        // Unbalanced template: treat the rest as the section body.
        return end - 1
    }

    private fun lookup(rawName: String, fields: Map<String, String>): String {
        val name = stripModifier(rawName)
        return fields[name] ?: ""
    }

    private fun fieldIsNonEmpty(rawName: String, fields: Map<String, String>): Boolean {
        val value = fields[stripModifier(rawName)] ?: return false
        return HtmlStripper.strip(value).isNotBlank()
    }

    /** `type:Foo` / `cloze:Foo` / `hint:Foo` / `text:Foo` -> `Foo`. */
    private fun stripModifier(name: String): String {
        val colon = name.indexOf(':')
        return if (colon >= 0) name.substring(colon + 1).trim() else name.trim()
    }

    // --- tokenizing ---------------------------------------------------------

    private sealed interface Token {
        data class Text(val value: String) : Token
        data class Var(val name: String) : Token
        data class SectionOpen(val name: String, val inverted: Boolean) : Token
        data class SectionClose(val name: String) : Token
    }

    private fun tokenize(template: String): List<Token> {
        val tokens = mutableListOf<Token>()
        var i = 0
        while (i < template.length) {
            val open = template.indexOf("{{", i)
            if (open < 0) {
                tokens += Token.Text(template.substring(i))
                break
            }
            if (open > i) tokens += Token.Text(template.substring(i, open))
            val close = template.indexOf("}}", open + 2)
            if (close < 0) {
                tokens += Token.Text(template.substring(open))
                break
            }
            val tag = template.substring(open + 2, close).trim()
            tokens += parseTag(tag)
            i = close + 2
        }
        return tokens
    }

    private fun parseTag(tag: String): Token = when {
        tag.startsWith("#") -> Token.SectionOpen(tag.substring(1).trim(), inverted = false)
        tag.startsWith("^") -> Token.SectionOpen(tag.substring(1).trim(), inverted = true)
        tag.startsWith("/") -> Token.SectionClose(tag.substring(1).trim())
        else -> Token.Var(tag)
    }
}
