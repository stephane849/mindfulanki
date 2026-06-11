package com.mindfulanki.core.apkg

import java.io.ByteArrayOutputStream

/**
 * Minimal protobuf encoder for tests — writes length-delimited string fields,
 * mirroring [ProtoReader]. Used to build modern (v18) `templates.config` blobs.
 */
object ProtoWriter {

    fun message(vararg stringFields: Pair<Int, String>): ByteArray {
        val out = ByteArrayOutputStream()
        for ((fieldNumber, value) in stringFields) {
            val bytes = value.toByteArray(Charsets.UTF_8)
            writeVarint(out, (fieldNumber.toLong() shl 3) or 2L) // wire type 2 = length-delimited
            writeVarint(out, bytes.size.toLong())
            out.write(bytes)
        }
        return out.toByteArray()
    }

    private fun writeVarint(out: ByteArrayOutputStream, value: Long) {
        var v = value
        while (true) {
            val b = (v and 0x7F).toInt()
            v = v ushr 7
            if (v == 0L) {
                out.write(b)
                return
            }
            out.write(b or 0x80)
        }
    }
}
