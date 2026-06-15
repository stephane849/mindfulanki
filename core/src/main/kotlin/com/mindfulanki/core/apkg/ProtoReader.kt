package com.mindfulanki.core.apkg

/**
 * A tiny protobuf wire-format reader — just enough to pull length-delimited
 * (string/bytes) fields out of a message by field number.
 *
 * The modern Anki schema (v18) stores card-template format strings inside a
 * protobuf-encoded `templates.config` BLOB rather than as plain columns. We
 * only need two string fields out of `CardTemplateConfig`:
 *   field 1 = q_format, field 2 = a_format.
 *
 * Rather than depend on a full protobuf runtime + generated classes, we scan
 * the wire format directly. Unknown fields are skipped per their wire type.
 */
object ProtoReader {

    private const val WIRE_VARINT = 0
    private const val WIRE_I64 = 1
    private const val WIRE_LEN = 2
    private const val WIRE_I32 = 5

    /** Return the bytes of the first length-delimited field numbered [fieldNumber]. */
    fun lengthDelimited(message: ByteArray, fieldNumber: Int): ByteArray? {
        var pos = 0
        while (pos < message.size) {
            val (tag, afterTag) = readVarint(message, pos)
            pos = afterTag
            val field = (tag ushr 3).toInt()
            when ((tag and 0x7).toInt()) {
                WIRE_VARINT -> pos = readVarint(message, pos).second
                WIRE_I64 -> pos += 8
                WIRE_I32 -> pos += 4
                WIRE_LEN -> {
                    val (len, afterLen) = readVarint(message, pos)
                    val start = afterLen
                    val end = start + len.toInt()
                    if (field == fieldNumber) return message.copyOfRange(start, end)
                    pos = end
                }
                else -> return null // malformed; bail out
            }
        }
        return null
    }

    /** Convenience: read a UTF-8 string field, or "" if absent. */
    fun stringField(message: ByteArray, fieldNumber: Int): String =
        lengthDelimited(message, fieldNumber)?.toString(Charsets.UTF_8) ?: ""

    /** Reads a base-128 varint; returns (value, nextPosition). */
    private fun readVarint(bytes: ByteArray, start: Int): Pair<Long, Int> {
        var result = 0L
        var shift = 0
        var pos = start
        while (pos < bytes.size) {
            val b = bytes[pos].toInt() and 0xFF
            result = result or ((b and 0x7F).toLong() shl shift)
            pos++
            if (b and 0x80 == 0) break
            shift += 7
        }
        return result to pos
    }
}
