package pro.nitron.zencall.call

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import pro.nitron.zencall.data.prefs.FirewallConfigSnapshot
import pro.nitron.zencall.data.prefs.FirewallMode
import pro.nitron.zencall.domain.engine.CallDecisionEngine
import pro.nitron.zencall.domain.model.CallAction
import pro.nitron.zencall.domain.model.CallMode
import pro.nitron.zencall.domain.model.CallRule
import pro.nitron.zencall.domain.model.CallerCategory
import pro.nitron.zencall.domain.model.CallerIdentity
import pro.nitron.zencall.domain.util.CallNumberNormalizer

sealed class ScreeningDecision {
    data object Allow : ScreeningDecision()
    data class Silence(val reason: String) : ScreeningDecision()
    data class Block(
        val reason: String,
        val shouldDisallowCall: Boolean = true,
        val shouldRejectCall: Boolean = true,
        val shouldSkipCallLog: Boolean = false,
        val shouldSkipNotification: Boolean = true
    ) : ScreeningDecision()
}

class ScreeningEngine(private val context: Context) {

    private val normalizer = CallNumberNormalizer(defaultCountryIso = "IR", defaultCountryPrefix = "98")
    private val domainEngine = CallDecisionEngine()

    fun evaluate(rawNumber: String?, snapshot: FirewallConfigSnapshot): ScreeningDecision {
        val normalized = normalizer.normalize(rawNumber)
        val isContact = if (normalized != null) isNumberInContacts(context, normalized) else false
        val isAllowed = snapshot.whitelist.any { normalizer.matches(it, rawNumber) }
        val isBlocked = snapshot.blacklist.any { normalizer.matches(it, rawNumber) }

        val caller = CallerIdentity(
            rawNumber = rawNumber,
            normalizedNumber = normalized,
            isContact = isContact,
            isFavorite = false,
            isBlockedList = isBlocked,
            isAllowedList = isAllowed
        )

        // Map preferences to domain CallMode
        val domainMode = createDomainMode(snapshot)
        val decision = domainEngine.decide(caller, domainMode)

        return when (decision.action) {
            CallAction.ALLOW -> ScreeningDecision.Allow
            CallAction.SILENCE -> ScreeningDecision.Silence(decision.reason)
            CallAction.REJECT -> ScreeningDecision.Block(
                reason = decision.reason,
                shouldDisallowCall = true,
                shouldRejectCall = true,
                shouldSkipNotification = snapshot.silentDrop
            )
        }
    }

    private fun createDomainMode(snapshot: FirewallConfigSnapshot): CallMode {
        val rules = mutableListOf<CallRule>()

        when (snapshot.mode) {
            FirewallMode.ALLOW_ALL -> {
                rules.add(CallRule("all", "active", CallerCategory.UNKNOWN, CallAction.ALLOW))
            }
            FirewallMode.BLOCK_BLACKLIST -> {
                rules.add(CallRule("bl", "active", CallerCategory.BLOCKED_NUMBER, CallAction.REJECT))
                rules.add(CallRule("un", "active", CallerCategory.UNKNOWN, CallAction.ALLOW))
            }
            FirewallMode.BLOCK_UNKNOWN, FirewallMode.BLOCK_ALL_EXCEPT_CONTACTS -> {
                rules.add(CallRule("wl", "active", CallerCategory.ALLOWED_NUMBER, CallAction.ALLOW))
                rules.add(CallRule("bl", "active", CallerCategory.BLOCKED_NUMBER, CallAction.REJECT))
                rules.add(CallRule("ct", "active", CallerCategory.CONTACT, CallAction.ALLOW))
                rules.add(CallRule("un", "active", CallerCategory.UNKNOWN, CallAction.REJECT))
            }
            FirewallMode.BLOCK_ALL -> {
                rules.add(CallRule("wl", "active", CallerCategory.ALLOWED_NUMBER, CallAction.ALLOW))
                rules.add(CallRule("all", "active", CallerCategory.UNKNOWN, CallAction.REJECT))
            }
        }

        return CallMode(
            id = "active_mode",
            name = snapshot.mode.name,
            isEnabled = snapshot.isEnabled,
            defaultAction = if (snapshot.mode == FirewallMode.ALLOW_ALL) CallAction.ALLOW else CallAction.REJECT,
            rules = rules
        )
    }

    private fun isNumberInContacts(context: Context, number: String): Boolean {
        return try {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(number)
            )
            val projection = arrayOf(ContactsContract.PhoneLookup._ID)
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                cursor.count > 0
            } ?: false
        } catch (_: SecurityException) {
            false
        } catch (_: Exception) {
            false
        }
    }
}
