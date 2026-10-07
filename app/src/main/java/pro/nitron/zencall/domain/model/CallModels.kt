package pro.nitron.zencall.domain.model

enum class CallAction {
    ALLOW,
    SILENCE,
    REJECT
}

enum class CallerCategory {
    FAVORITE,
    CONTACT,
    UNKNOWN,
    BLOCKED_NUMBER,
    ALLOWED_NUMBER
}

data class CallerIdentity(
    val rawNumber: String?,
    val normalizedNumber: String?,
    val isContact: Boolean = false,
    val isFavorite: Boolean = false,
    val isBlockedList: Boolean = false,
    val isAllowedList: Boolean = false
) {
    val isPrivateOrUnknown: Boolean
        get() = rawNumber.isNullOrBlank() ||
                rawNumber.equals("UNKNOWN", ignoreCase = true) ||
                rawNumber.equals("PRIVATE", ignoreCase = true) ||
                rawNumber.equals("RESTRICTED", ignoreCase = true)
}

data class CallRule(
    val id: String,
    val modeId: String,
    val category: CallerCategory,
    val action: CallAction,
    val priority: Int = 0
)

data class CallMode(
    val id: String,
    val name: String,
    val isEnabled: Boolean = true,
    val defaultAction: CallAction = CallAction.SILENCE,
    val rules: List<CallRule> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
