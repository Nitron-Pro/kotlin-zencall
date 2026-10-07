package pro.nitron.zencall.call

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import pro.nitron.zencall.data.repository.FirewallRepository
import pro.nitron.zencall.data.repository.FirewallRepositoryImpl
import pro.nitron.zencall.data.repository.ScreeningSnapshot
import pro.nitron.zencall.domain.engine.CallDecisionEngine
import pro.nitron.zencall.domain.model.CallAction
import pro.nitron.zencall.domain.model.CallerIdentity
import pro.nitron.zencall.domain.model.SystemCategories
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

class ScreeningEngine(
    private val context: Context,
    private val repository: FirewallRepository = FirewallRepositoryImpl(context)
) {

    private val normalizer = CallNumberNormalizer(defaultCountryIso = "IR", defaultCountryPrefix = "98")
    private val domainEngine = CallDecisionEngine()

    suspend fun evaluateFromRepository(rawNumber: String?): ScreeningDecision {
        val snapshot = repository.getFastScreeningSnapshot()
        return evaluateWithSnapshot(rawNumber, snapshot)
    }

    fun evaluateWithSnapshot(rawNumber: String?, snapshot: ScreeningSnapshot): ScreeningDecision {
        if (!snapshot.isFirewallEnabled) {
            return ScreeningDecision.Allow
        }

        val normalized = normalizer.normalize(rawNumber)
        val isContact = if (normalized != null) isNumberInContacts(context, normalized) else false

        // Match categories from persistent fast lookup map
        val matchedCategories = if (normalized != null) {
            snapshot.numbersToCategoriesMap[normalized] ?: emptySet()
        } else {
            emptySet()
        }

        val isAllowed = matchedCategories.contains(SystemCategories.ALLOWED_NUMBER)
        val isBlocked = matchedCategories.contains(SystemCategories.BLOCKED_NUMBER)
        val isFavorite = matchedCategories.contains(SystemCategories.FAVORITE)

        // Custom categories assigned to this number by the user
        val customCategories = matchedCategories.filter {
            it !in setOf(
                SystemCategories.ALLOWED_NUMBER,
                SystemCategories.BLOCKED_NUMBER,
                SystemCategories.FAVORITE,
                SystemCategories.CONTACT,
                SystemCategories.UNKNOWN
            )
        }.toSet()

        val caller = CallerIdentity(
            rawNumber = rawNumber,
            normalizedNumber = normalized,
            isContact = isContact,
            isFavorite = isFavorite,
            isBlockedList = isBlocked,
            isAllowedList = isAllowed,
            customCategoryIds = customCategories
        )

        val decision = domainEngine.decide(caller, snapshot.activeMode)

        return when (decision.action) {
            CallAction.ALLOW -> ScreeningDecision.Allow
            CallAction.SILENCE -> ScreeningDecision.Silence(decision.reason)
            CallAction.REJECT -> ScreeningDecision.Block(
                reason = decision.reason,
                shouldDisallowCall = true,
                shouldRejectCall = true,
                shouldSkipNotification = true
            )
        }
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
