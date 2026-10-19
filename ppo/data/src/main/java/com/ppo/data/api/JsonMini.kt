package com.ppo.data.api

object JsonMini {
    fun parse(json: String): Any? {
        val trimmed = json.trim()
        return when {
            trimmed.startsWith("{") -> parseObject(trimmed)
            trimmed.startsWith("[") -> parseArray(trimmed)
            trimmed.startsWith("\"") && trimmed.endsWith("\"") -> unescapeString(trimmed.substring(1, trimmed.length - 1))
            trimmed == "true" -> true
            trimmed == "false" -> false
            trimmed == "null" -> null
            trimmed.toLongOrNull() != null -> trimmed.toLong()
            trimmed.toDoubleOrNull() != null -> trimmed.toDouble()
            else -> trimmed
        }
    }

    fun parseObject(json: String): Map<String, Any?> {
        val trimmed = json.trim()
        if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) return emptyMap()
        val content = trimmed.substring(1, trimmed.length - 1).trim()
        if (content.isEmpty()) return emptyMap()

        val result = mutableMapOf<String, Any?>()
        val tokens = splitJsonTokens(content)
        for (token in tokens) {
            val colonIdx = findColonIndex(token)
            if (colonIdx != -1) {
                val rawKey = token.substring(0, colonIdx).trim()
                val rawVal = token.substring(colonIdx + 1).trim()
                val key = if (rawKey.startsWith("\"") && rawKey.endsWith("\"")) {
                    unescapeString(rawKey.substring(1, rawKey.length - 1))
                } else rawKey
                result[key] = parse(rawVal)
            }
        }
        return result
    }

    fun parseArray(json: String): List<Any?> {
        val trimmed = json.trim()
        if (!trimmed.startsWith("[") || !trimmed.endsWith("]")) return emptyList()
        val content = trimmed.substring(1, trimmed.length - 1).trim()
        if (content.isEmpty()) return emptyList()

        val result = mutableListOf<Any?>()
        val tokens = splitJsonTokens(content)
        for (token in tokens) {
            result.add(parse(token.trim()))
        }
        return result
    }

    private fun splitJsonTokens(str: String): List<String> {
        val list = mutableListOf<String>()
        var depthBrace = 0
        var depthBracket = 0
        var inQuotes = false
        var escape = false
        val current = StringBuilder()

        for (ch in str) {
            if (escape) {
                current.append(ch)
                escape = false
                continue
            }
            if (ch == '\\') {
                current.append(ch)
                escape = true
                continue
            }
            if (ch == '"') {
                inQuotes = !inQuotes
                current.append(ch)
                continue
            }
            if (!inQuotes) {
                when (ch) {
                    '{' -> depthBrace++
                    '}' -> depthBrace--
                    '[' -> depthBracket++
                    ']' -> depthBracket--
                    ',' -> {
                        if (depthBrace == 0 && depthBracket == 0) {
                            list.add(current.toString().trim())
                            current.clear()
                            continue
                        }
                    }
                }
            }
            current.append(ch)
        }
        if (current.isNotBlank()) {
            list.add(current.toString().trim())
        }
        return list
    }

    private fun findColonIndex(token: String): Int {
        var inQuotes = false
        var escape = false
        for (i in token.indices) {
            val ch = token[i]
            if (escape) {
                escape = false
                continue
            }
            if (ch == '\\') {
                escape = true
                continue
            }
            if (ch == '"') {
                inQuotes = !inQuotes
                continue
            }
            if (ch == ':' && !inQuotes) return i
        }
        return -1
    }

    private fun unescapeString(s: String): String {
        return s.replace("\\\"", "\"")
            .replace("\\\\", "\\")
            .replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t")
    }
}
