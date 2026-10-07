package pro.nitron.zencall.domain.model

enum class CallAction {
    ALLOW,
    SILENCE,
    REJECT
}

/**
 * Built-in default category keys that ZenCall provides out-of-the-box.
 * Users can also create arbitrary custom categories with unique string IDs.
 */
object SystemCategories {
    const val ALLOWED_NUMBER = "ALLOWED_NUMBER"
    const val BLOCKED_NUMBER = "BLOCKED_NUMBER"
    const val FAVORITE = "FAVORITE"
    const val CONTACT = "CONTACT"
    const val UNKNOWN = "UNKNOWN"
}

data class CallerCategory(
    val id: String,
    val name: String,
    val isSystemDefault: Boolean = false,
    val description: String? = null,
    val colorHex: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        val Allowed = CallerCategory(
            id = SystemCategories.ALLOWED_NUMBER,
            name = "Allowed Numbers (Whitelist)",
            isSystemDefault = true,
            description = "Numbers explicitly granted full bypass permission"
        )
        val Blocked = CallerCategory(
            id = SystemCategories.BLOCKED_NUMBER,
            name = "Blocked Numbers (Blacklist)",
            isSystemDefault = true,
            description = "Numbers explicitly blocked"
        )
        val Favorite = CallerCategory(
            id = SystemCategories.FAVORITE,
            name = "Favorite Contacts",
            isSystemDefault = true,
            description = "Starred or VIP contacts"
        )
        val Contact = CallerCategory(
            id = SystemCategories.CONTACT,
            name = "Contacts",
            isSystemDefault = true,
            description = "Anyone in device address book"
        )
        val Unknown = CallerCategory(
            id = SystemCategories.UNKNOWN,
            name = "Unknown Callers",
            isSystemDefault = true,
            description = "Numbers not saved in contacts or private caller IDs"
        )

        val defaultCategories = listOf(Allowed, Blocked, Favorite, Contact, Unknown)
    }
}

data class CallerIdentity(
    val rawNumber: String?,
    val normalizedNumber: String?,
    val isContact: Boolean = false,
    val isFavorite: Boolean = false,
    val isBlockedList: Boolean = false,
    val isAllowedList: Boolean = false,
    val customCategoryIds: Set<String> = emptySet()
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
    val categoryId: String,
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
