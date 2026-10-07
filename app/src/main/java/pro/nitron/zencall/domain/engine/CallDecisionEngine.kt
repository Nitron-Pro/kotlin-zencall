package pro.nitron.zencall.domain.engine

import pro.nitron.zencall.domain.model.CallAction
import pro.nitron.zencall.domain.model.CallMode
import pro.nitron.zencall.domain.model.CallerCategory
import pro.nitron.zencall.domain.model.CallerIdentity

data class DecisionResult(
    val action: CallAction,
    val reason: String,
    val matchedCategory: CallerCategory? = null,
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
            val rule = activeMode.rules.firstOrNull { it.category == CallerCategory.ALLOWED_NUMBER }
            val action = rule?.action ?: CallAction.ALLOW
            return DecisionResult(
                action = action,
                reason = "Caller is in Allowed list",
                matchedCategory = CallerCategory.ALLOWED_NUMBER,
                evaluationTimeMs = System.currentTimeMillis() - start
            )
        }

        // 3. Explicit Blocked List (Blacklist) has second priority
        if (caller.isBlockedList) {
            val rule = activeMode.rules.firstOrNull { it.category == CallerCategory.BLOCKED_NUMBER }
            val action = rule?.action ?: CallAction.REJECT
            return DecisionResult(
                action = action,
                reason = "Caller is in Blocked list",
                matchedCategory = CallerCategory.BLOCKED_NUMBER,
                evaluationTimeMs = System.currentTimeMillis() - start
            )
        }

        // 4. Favorite Contact Check
        if (caller.isFavorite) {
            val rule = activeMode.rules.firstOrNull { it.category == CallerCategory.FAVORITE }
            if (rule != null) {
                return DecisionResult(
                    action = rule.action,
                    reason = "Caller is a favorite contact",
                    matchedCategory = CallerCategory.FAVORITE,
                    evaluationTimeMs = System.currentTimeMillis() - start
                )
            }
        }

        // 5. General Contact Check
        if (caller.isContact) {
            val rule = activeMode.rules.firstOrNull { it.category == CallerCategory.CONTACT }
            if (rule != null) {
                return DecisionResult(
                    action = rule.action,
                    reason = "Caller is in contacts",
                    matchedCategory = CallerCategory.CONTACT,
                    evaluationTimeMs = System.currentTimeMillis() - start
                )
            }
        }

        // 6. Unknown / Non-contact / Private Number Check
        val unknownRule = activeMode.rules.firstOrNull { it.category == CallerCategory.UNKNOWN }
        if (unknownRule != null) {
            return DecisionResult(
                action = unknownRule.action,
                reason = if (caller.isPrivateOrUnknown) "Caller ID is hidden/private" else "Caller is unknown",
                matchedCategory = CallerCategory.UNKNOWN,
                evaluationTimeMs = System.currentTimeMillis() - start
            )
        }

        // 7. Fallback to Mode's default action
        return DecisionResult(
            action = activeMode.defaultAction,
            reason = "Default action of mode '${activeMode.name}'",
            evaluationTimeMs = System.currentTimeMillis() - start
        )
    }
}
