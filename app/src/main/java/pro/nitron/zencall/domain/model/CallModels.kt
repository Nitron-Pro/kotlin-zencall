package pro.nitron.zencall.domain.model

enum class CallAction {
    ALLOW,
    SILENCE,
    REJECT
}

enum class ModeControl {
    AUTO,
    MANUAL
}

object SystemCategories {
    const val ALLOWED_NUMBER = "ALLOWED_NUMBER"
    const val BLOCKED_NUMBER = "BLOCKED_NUMBER"
    const val FAVORITE = "FAVORITE"
    const val CONTACT = "CONTACT"
    const val UNKNOWN = "UNKNOWN"
    const val PRIVATE = "PRIVATE"

    val virtualIds = setOf(CONTACT, UNKNOWN, PRIVATE)
    val numberListIds = setOf(ALLOWED_NUMBER, BLOCKED_NUMBER, FAVORITE)

    fun holdsNumbers(categoryId: String): Boolean = categoryId !in virtualIds
    fun isBlockList(categoryId: String): Boolean = categoryId == BLOCKED_NUMBER
}

data class CallerCategory(
    val id: String,
    val name: String,
    val isSystemDefault: Boolean = false,
    val description: String? = null,
    val colorHex: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    val holdsNumbers: Boolean get() = SystemCategories.holdsNumbers(id)
    val isBlockList: Boolean get() = SystemCategories.isBlockList(id)

    companion object {
        val Allowed = CallerCategory(
            id = SystemCategories.ALLOWED_NUMBER,
            name = "مجاز",
            isSystemDefault = true,
            description = "وایت‌لیست عمومی"
        )
        val Blocked = CallerCategory(
            id = SystemCategories.BLOCKED_NUMBER,
            name = "مسدود",
            isSystemDefault = true,
            description = "همیشه قطع می‌شود"
        )
        val Favorite = CallerCategory(
            id = SystemCategories.FAVORITE,
            name = "VIP",
            isSystemDefault = true,
            description = "شماره‌های ویژه"
        )
        val Contact = CallerCategory(
            id = SystemCategories.CONTACT,
            name = "مخاطبین",
            isSystemDefault = true,
            description = "دفترچه تلفن گوشی"
        )
        val Unknown = CallerCategory(
            id = SystemCategories.UNKNOWN,
            name = "ناشناس",
            isSystemDefault = true,
            description = "شماره ذخیره‌نشده"
        )
        val Private = CallerCategory(
            id = SystemCategories.PRIVATE,
            name = "مخفی",
            isSystemDefault = true,
            description = "بدون شناسه تماس"
        )

        val defaultCategories = listOf(Allowed, Blocked, Favorite, Contact, Unknown, Private)
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

data class WeeklySchedule(
    val isEnabled: Boolean = false,
    val daysOfWeek: Set<Int> = setOf(1, 2, 3, 4, 5, 6, 7),
    val startMinuteOfDay: Int = 0,
    val endMinuteOfDay: Int = 1439,
    val outsideScheduleAction: CallAction? = null
) {
    fun isMatching(dayOfWeekValue: Int, minuteOfDay: Int): Boolean {
        if (!isEnabled) return false
        if (dayOfWeekValue !in daysOfWeek) return false

        return if (startMinuteOfDay <= endMinuteOfDay) {
            minuteOfDay in startMinuteOfDay..endMinuteOfDay
        } else {
            minuteOfDay >= startMinuteOfDay || minuteOfDay <= endMinuteOfDay
        }
    }
}

data class CallRule(
    val id: String,
    val modeId: String,
    val categoryId: String,
    val action: CallAction,
    val priority: Int = 0,
    val schedule: WeeklySchedule = WeeklySchedule(isEnabled = false)
)

data class CallMode(
    val id: String,
    val name: String,
    val isEnabled: Boolean = true,
    val isSystemDefault: Boolean = false,
    val defaultAction: CallAction = CallAction.REJECT,
    val schedulePriority: Int = 0,
    val schedule: WeeklySchedule = WeeklySchedule(isEnabled = false),
    val rules: List<CallRule> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * Returns explicit action configured for this category in this mode (ALLOW, SILENCE, REJECT),
     * or null if category has no explicit override (inheriting mode's defaultAction).
     */
    fun getCategoryAction(categoryId: String): CallAction? {
        return rules.firstOrNull { it.categoryId == categoryId }?.action
    }

    fun isCategoryWhitelisted(categoryId: String): Boolean {
        return getCategoryAction(categoryId) == CallAction.ALLOW
    }
}
