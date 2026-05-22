package com.example.newshub.core

object RestIdNormalizer {
    fun normalize(id: String): String {
        val trimmed = id.trim()
        if (trimmed.isBlank()) return trimmed
        trimmed.toDoubleOrNull()?.let { value ->
            val asLong = value.toLong()
            if (value == asLong.toDouble()) return asLong.toString()
        }
        return trimmed
    }
}
