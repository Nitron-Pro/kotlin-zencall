package pro.nitron.zencall.domain.engine

import pro.nitron.zencall.domain.model.CallAction
import pro.nitron.zencall.domain.model.CallMode
import pro.nitron.zencall.domain.model.CallRule
import pro.nitron.zencall.domain.model.CallerIdentity
import pro.nitron.zencall.domain.model.SystemCategories
import java.time.LocalDateTime

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
 * Pure Kotlin Decision Engine (Domain Layer)
 * Independent of Android Framework for deterministic, sub-millisecond execution and testability.
 */
class CallDecisionEngine(
    private val failSafePolicy: FailSafePolicy = FailSafePolicy.ALLOW
) {

    fun decide(
        caller: CallerIdentity,
        activeMode: CallMode?,
        callTime: LocalDateTime = LocalDateTime.now()
    ): DecisionResult {
        val start = System.currentTimeMillis()

        // 1. Fail-Safe: If mode is absent or inactive
        if (activeMode == null || !activeMode.isEnabled) {
            return DecisionResult(
                action = CallAction.ALLOW,
                reason = "Firewall mode is inactive or disabled",
                evaluationTimeMs = System.currentTimeMillis() - start
            )
        }

        val dayOfWeekValue = callTime.dayOfWeek.value // 1 (Mon) .. 7 (Sun)
        val minuteOfDay = callTime.hour * 60 + callTime.minute

        // 2. Explicit Allowed List (Whitelist) has top priority
        if (caller.isAllowedList) {
            val rule = activeMode.rules.firstOrNull { it.categoryId == SystemCategories.ALLOWED_NUMBER }
            val resolvedAction = resolveRuleAction(rule, dayOfWeekValue, minuteOfDay) ?: CallAction.ALLOW
            return DecisionResult(
                action = resolvedAction,
                reason = "Caller is in Allowed list",
                matchedCategoryId = SystemCategories.ALLOWED_NUMBER,
                evaluationTimeMs = System.currentTimeMillis() - start
            )
        }

        // 3. Explicit Blocked List (Blacklist) has second priority
        if (caller.isBlockedList) {
            val rule = activeMode.rules.firstOrNull { it.categoryId == SystemCategories.BLOCKED_NUMBER }
            val resolvedAction = resolveRuleAction(rule, dayOfWeekValue, minuteOfDay) ?: CallAction.REJECT
            return DecisionResult(
                action = resolvedAction,
                reason = "Caller is in Blocked list",
                matchedCategoryId = SystemCategories.BLOCKED_NUMBER,
                evaluationTimeMs = System.currentTimeMillis() - start
            )
        }

        // 4. Custom User-Created Categories Check (sorted by rule priority descending)
        if (caller.customCategoryIds.isNotEmpty()) {
            val matchingRules = activeMode.rules
                .filter { it.categoryId in caller.customCategoryIds }
                .sortedByDescending { it.priority }

            for (rule in matchingRules) {
                val resolvedAction = resolveRuleAction(rule, dayOfWeekValue, minuteOfDay)
                if (resolvedAction != null) {
                    return DecisionResult(
                        action = resolvedAction,
                        reason = "Caller matched custom user category: ${rule.categoryId} (Action: $resolvedAction)",
                        matchedCategoryId = rule.categoryId,
                        evaluationTimeMs = System.currentTimeMillis() - start
                    )
                }
            }
        }

        // 5. Favorite Contact Check
        if (caller.isFavorite) {
            val rule = activeMode.rules.firstOrNull { it.categoryId == SystemCategories.FAVORITE }
            val resolvedAction = resolveRuleAction(rule, dayOfWeekValue, minuteOfDay)
            if (resolvedAction != null) {
                return DecisionResult(
                    action = resolvedAction,
                    reason = "Caller is a favorite contact",
                    matchedCategoryId = SystemCategories.FAVORITE,
                    evaluationTimeMs = System.currentTimeMillis() - start
                )
            }
        }

        // 6. General Contact Check
        if (caller.isContact) {
            val rule = activeMode.rules.firstOrNull { it.categoryId == SystemCategories.CONTACT }
            val resolvedAction = resolveRuleAction(rule, dayOfWeekValue, minuteOfDay)
            if (resolvedAction != null) {
                return DecisionResult(
                    action = resolvedAction,
                    reason = "Caller is in contacts",
                    matchedCategoryId = SystemCategories.CONTACT,
                    evaluationTimeMs = System.currentTimeMillis() - start
                )
            }
        }

        // 7. Unknown / Non-contact / Private Number Check
        val unknownRule = activeMode.rules.firstOrNull { it.categoryId == SystemCategories.UNKNOWN }
        val resolvedUnknownAction = resolveRuleAction(unknownRule, dayOfWeekValue, minuteOfDay)
        if (resolvedUnknownAction != null) {
            return DecisionResult(
                action = resolvedUnknownAction,
                reason = if (caller.isPrivateOrUnknown) "Caller ID is hidden/private" else "Caller is unknown",
                matchedCategoryId = SystemCategories.UNKNOWN,
                evaluationTimeMs = System.currentTimeMillis() - start
            )
        }

        // 8. Fallback to Mode's default action
        return DecisionResult(
            action = activeMode.defaultAction,
            reason = "Default action of mode '${activeMode.name}'",
            evaluationTimeMs = System.currentTimeMillis() - start
        )
    }

    /**
     * Resolves the effective action of a rule given the current day and minute of day.
     * Returns null if the rule is outside its active schedule and has no outsideScheduleAction defined.
     */
    private fun resolveRuleAction(rule: CallRule?, dayOfWeekValue: Int, minuteOfDay: Int): CallAction? {
        if (rule == null) return null
        if (!rule.schedule.isEnabled) return rule.action

        val isInside = rule.schedule.isMatching(dayOfWeekValue, minuteOfDay)
        return if (isInside) {
            rule.action
        } else {
            rule.schedule.outsideScheduleAction
        }
    }
}
