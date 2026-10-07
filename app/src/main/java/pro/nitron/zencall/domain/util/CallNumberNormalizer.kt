package pro.nitron.zencall.domain.util

class CallNumberNormalizer(
    private val defaultCountryIso: String = "IR",
    private val defaultCountryPrefix: String = "98"
) {

    /**
     * Normalizes a phone number into an E.164-like canonical representation:
     * - Strips non-digit characters except leading '+'
     * - Handles international formats like 00... -> +...
     * - Handles local prefixes with national trunk (e.g. 0912 -> +98912)
     */
    fun normalize(rawNumber: String?): String? {
        if (rawNumber.isNullOrBlank()) return null

        val trimmed = rawNumber.trim()
        if (trimmed.equals("UNKNOWN", ignoreCase = true) ||
            trimmed.equals("PRIVATE", ignoreCase = true) ||
            trimmed.equals("RESTRICTED", ignoreCase = true)) {
            return null
        }

        // Keep leading '+' if present, strip all spaces, dashes, brackets, dots
        val hasPlus = trimmed.startsWith("+")
        val digitsOnly = trimmed.replace(Regex("[^0-9]"), "")
        if (digitsOnly.isEmpty()) return null

        return when {
            hasPlus -> "+$digitsOnly"
            digitsOnly.startsWith("00") -> "+" + digitsOnly.substring(2)
            digitsOnly.startsWith("0") -> "+$defaultCountryPrefix" + digitsOnly.substring(1)
            digitsOnly.startsWith(defaultCountryPrefix) -> "+$digitsOnly"
            else -> "+$defaultCountryPrefix$digitsOnly"
        }
    }

    /**
     * Determines whether two phone numbers are equivalent.
     * Compares full normalized form first; falls back to matching the last 7 to 9 significant national digits.
     */
    fun matches(number1: String?, number2: String?): Boolean {
        val norm1 = normalize(number1)
        val norm2 = normalize(number2)

        if (norm1 == null || norm2 == null) return false
        if (norm1 == norm2) return true

        // Match national significant suffix (minimum 7 digits, typically 7-9)
        val digits1 = norm1.replace("+", "")
        val digits2 = norm2.replace("+", "")

        val minLen = 7
        if (digits1.length >= minLen && digits2.length >= minLen) {
            val suffix1 = digits1.takeLast(minLen)
            val suffix2 = digits2.takeLast(minLen)
            if (suffix1 == suffix2) return true
        }

        return false
    }

    /**
     * Formats a phone number for privacy-safe debug logs (masks middle digits)
     * e.g., +989123456789 -> +98912****789
     */
    fun maskForLog(rawNumber: String?): String {
        val norm = normalize(rawNumber) ?: return "[UNKNOWN]"
        if (norm.length <= 6) return "***"
        val prefix = norm.take(5)
        val suffix = norm.takeLast(3)
        return "$prefix****$suffix"
    }
}
