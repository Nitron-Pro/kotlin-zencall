package pro.nitron.zencall.domain.engine

import pro.nitron.zencall.domain.model.CallAction
import pro.nitron.zencall.domain.model.CallMode
import pro.nitron.zencall.domain.model.CallerIdentity
import pro.nitron.zencall.domain.model.SystemCategories

data class DecisionResult(
    val action: CallAction,
    val reason: String,
    val matchedCategoryId: String? = null,
    val evaluationTimeMs: Long = 0
)

enum class FailSafePolicy {
    ALLOW,
    REJECT
}

/**
 * Mode default = block/silence everyone.
 * Number groups and virtual sources (contacts / unknown / private) only add whitelist exceptions.
 * Explicit block list always wins.
 */
class CallDecisionEngine(
    private val failSafePolicy: FailSafePolicy = FailSafePolicy.ALLOW
) {

    fun decide(
        caller: CallerIdentity,
        activeMode: CallMode?
    ): DecisionResult {
        val start = System.currentTimeMillis()

        if (activeMode == null || !activeMode.isEnabled) {
            val action = if (failSafePolicy == FailSafePolicy.ALLOW) CallAction.ALLOW else CallAction.REJECT
            return DecisionResult(
                action = action,
                reason = "Firewall mode is inactive or disabled",
                evaluationTimeMs = System.currentTimeMillis() - start
            )
        }

        fun result(action: CallAction, reason: String, categoryId: String? = null) =
            DecisionResult(
                action = action,
                reason = reason,
                matchedCategoryId = categoryId,
                evaluationTimeMs = System.currentTimeMillis() - start
            )

        // Global Blacklist always drops
        if (caller.isBlockedList) {
            return result(CallAction.REJECT, "در لیست مسدود", SystemCategories.BLOCKED_NUMBER)
        }

        // 1. Private / Unknown caller ID
        if (caller.isPrivateOrUnknown) {
            val privateAction = activeMode.getCategoryAction(SystemCategories.PRIVATE)
            return if (privateAction != null) {
                result(privateAction, "سیاست صریح شماره مخفی: $privateAction", SystemCategories.PRIVATE)
            } else {
                result(activeMode.defaultAction, "شماره مخفی / پیش‌فرض حالت", SystemCategories.PRIVATE)
            }
        }

        // 2. Specific Number Groups (Allowed list, Favorite, Custom Groups)
        val numberGroupIds = buildSet {
            if (caller.isAllowedList) add(SystemCategories.ALLOWED_NUMBER)
            if (caller.isFavorite) add(SystemCategories.FAVORITE)
            addAll(caller.customCategoryIds)
        }

        // Find highest priority matching group that has an explicit policy in this mode
        for (groupId in numberGroupIds) {
            val action = activeMode.getCategoryAction(groupId)
            if (action != null) {
                return result(action, "قانون گروه در حالت «${activeMode.name}»: $action", groupId)
            }
        }

        // 3. Virtual Contact Group
        if (caller.isContact) {
            val contactAction = activeMode.getCategoryAction(SystemCategories.CONTACT)
            if (contactAction != null) {
                return result(contactAction, "مخاطب دفترچه تلفن: $contactAction", SystemCategories.CONTACT)
            }
        }

        // 4. Virtual Unknown / Non-contact Group
        if (!caller.isContact) {
            val unknownAction = activeMode.getCategoryAction(SystemCategories.UNKNOWN)
            if (unknownAction != null) {
                return result(unknownAction, "شماره ناشناس: $unknownAction", SystemCategories.UNKNOWN)
            }
        }

        // 5. Fallback to Mode's default action
        return result(
            activeMode.defaultAction,
            "پیش‌فرض حالت «${activeMode.name}»: ${activeMode.defaultAction}"
        )
    }
}
