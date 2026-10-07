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
 * Pure Kotlin Decision Engine (Domain Layer)
 * Independent of Android Framework for deterministic, sub-millisecond execution and testability.
 */
class CallDecisionEngine(
    private val failSafePolicy: FailSafePolicy = FailSafePolicy.ALLOW
) {

    fun decide(
        caller: CallerIdentity,
        activeMode: CallMode?
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

        // 2. Explicit Allowed List (Whitelist) has top priority
        if (caller.isAllowedList) {
            val rule = activeMode.rules.firstOrNull { it.categoryId == SystemCategories.ALLOWED_NUMBER }
            val action = rule?.action ?: CallAction.ALLOW
            return DecisionResult(
                action = action,
                reason = "Caller is in Allowed list",
                matchedCategoryId = SystemCategories.ALLOWED_NUMBER,
                evaluationTimeMs = System.currentTimeMillis() - start
            )
        }

        // 3. Explicit Blocked List (Blacklist) has second priority
        if (caller.isBlockedList) {
            val rule = activeMode.rules.firstOrNull { it.categoryId == SystemCategories.BLOCKED_NUMBER }
            val action = rule?.action ?: CallAction.REJECT
            return DecisionResult(
                action = action,
                reason = "Caller is in Blocked list",
                matchedCategoryId = SystemCategories.BLOCKED_NUMBER,
                evaluationTimeMs = System.currentTimeMillis() - start
            )
        }

        // 4. Custom User-Created Categories Check (sorted by rule priority descending)
        if (caller.customCategoryIds.isNotEmpty()) {
            val matchingCustomRule = activeMode.rules
                .filter { it.categoryId in caller.customCategoryIds }
                .maxByOrNull { it.priority }

            if (matchingCustomRule != null) {
                return DecisionResult(
                    action = matchingCustomRule.action,
                    reason = "Caller matched custom user category: ${matchingCustomRule.categoryId}",
                    matchedCategoryId = matchingCustomRule.categoryId,
                    evaluationTimeMs = System.currentTimeMillis() - start
                )
            }
        }

        // 5. Favorite Contact Check
        if (caller.isFavorite) {
            val rule = activeMode.rules.firstOrNull { it.categoryId == SystemCategories.FAVORITE }
            if (rule != null) {
                return DecisionResult(
                    action = rule.action,
                    reason = "Caller is a favorite contact",
                    matchedCategoryId = SystemCategories.FAVORITE,
                    evaluationTimeMs = System.currentTimeMillis() - start
                )
            }
        }

        // 6. General Contact Check
        if (caller.isContact) {
            val rule = activeMode.rules.firstOrNull { it.categoryId == SystemCategories.CONTACT }
            if (rule != null) {
                return DecisionResult(
                    action = rule.action,
                    reason = "Caller is in contacts",
                    matchedCategoryId = SystemCategories.CONTACT,
                    evaluationTimeMs = System.currentTimeMillis() - start
                )
            }
        }

        // 7. Unknown / Non-contact / Private Number Check
        val unknownRule = activeMode.rules.firstOrNull { it.categoryId == SystemCategories.UNKNOWN }
        if (unknownRule != null) {
            return DecisionResult(
                action = unknownRule.action,
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
}
