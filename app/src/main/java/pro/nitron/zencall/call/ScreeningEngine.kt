package pro.nitron.zencall.call

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import pro.nitron.zencall.data.repository.FirewallRepository
import pro.nitron.zencall.data.repository.FirewallRepositoryImpl
import pro.nitron.zencall.data.repository.ScreeningSnapshot
import pro.nitron.zencall.domain.engine.CallDecisionEngine
import pro.nitron.zencall.domain.engine.ModeResolver
import pro.nitron.zencall.domain.model.CallAction
import pro.nitron.zencall.domain.model.CallerIdentity
import pro.nitron.zencall.domain.model.SystemCategories
import pro.nitron.zencall.domain.util.CallNumberNormalizer

sealed class ScreeningDecision {
    data class Allow(val reason: String = "Allowed") : ScreeningDecision()
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

    fun evaluateWithSnapshot(rawNumber: String?, snapshot: ScreeningSnapshot): Pair<ScreeningDecision, CallerIdentity> {
        if (!snapshot.isFirewallEnabled) {
            val emptyCaller = CallerIdentity(rawNumber = rawNumber, normalizedNumber = normalizer.normalize(rawNumber))
            return Pair(ScreeningDecision.Allow("Firewall Disabled"), emptyCaller)
        }

        val normalized = normalizer.normalize(rawNumber)
        val contactName = if (normalized != null) getContactName(context, normalized) else null
        val isContact = contactName != null

        // Match categories from persistent fast lookup map
        // 1. Exact match on normalized number
        // 2. Suffix match fallback (last 7 to 9 digits) to handle national vs international format variations (+98 vs 0)
        val matchedCategories = if (normalized != null) {
            val exact = snapshot.numbersToCategoriesMap[normalized]
            if (!exact.isNullOrEmpty()) {
                exact
            } else {
                val digitsOnly = normalized.replace("+", "")
                val suffix7 = if (digitsOnly.length >= 7) digitsOnly.takeLast(7) else digitsOnly
                snapshot.numbersToCategoriesMap.entries.firstOrNull { (key, _) ->
                    val keyDigits = key.replace("+", "")
                    normalizer.matches(normalized, key) || (keyDigits.length >= 7 && keyDigits.endsWith(suffix7))
                }?.value ?: emptySet()
            }
        } else {
            emptySet()
        }

        val isAllowed = matchedCategories.contains(SystemCategories.ALLOWED_NUMBER)
        val isBlocked = matchedCategories.contains(SystemCategories.BLOCKED_NUMBER)
        val isFavorite = matchedCategories.contains(SystemCategories.FAVORITE)

        val caller = CallerIdentity(
            rawNumber = rawNumber,
            normalizedNumber = normalized,
            isContact = isContact,
            isFavorite = isFavorite,
            isBlockedList = isBlocked,
            isAllowedList = isAllowed,
            customCategoryIds = matchedCategories
        )

        val resolvedMode = ModeResolver.resolve(
            modes = snapshot.allModes.ifEmpty { listOfNotNull(snapshot.activeMode) },
            control = snapshot.modeControl,
            manualModeId = snapshot.manualModeId
        ) ?: snapshot.activeMode

        android.util.Log.i("ZenCallScreening", "Screening caller: raw=$rawNumber, norm=$normalized, isContact=$isContact, categories=$matchedCategories, mode=${resolvedMode?.name} (id=${resolvedMode?.id}), rulesCount=${resolvedMode?.rules?.size}")
        if (resolvedMode != null) {
            for (rule in resolvedMode.rules) {
                android.util.Log.i("ZenCallScreening", "Mode rule: cat=${rule.categoryId}, action=${rule.action}")
            }
        }

        val decision = domainEngine.decide(caller, resolvedMode)
        android.util.Log.i("ZenCallScreening", "Engine decision: action=${decision.action}, reason=${decision.reason}, matchedCat=${decision.matchedCategoryId}")

        val screeningDecision = when (decision.action) {
            CallAction.ALLOW -> ScreeningDecision.Allow(decision.reason)
            CallAction.SILENCE -> ScreeningDecision.Silence(decision.reason)
            CallAction.REJECT -> ScreeningDecision.Block(
                reason = decision.reason,
                shouldDisallowCall = true,
                shouldRejectCall = true,
                shouldSkipNotification = true
            )
        }

        return Pair(screeningDecision, caller)
    }

    suspend fun evaluateFromRepositoryWithDetails(rawNumber: String?): Pair<ScreeningDecision, CallerIdentity> {
        // Ensure fresh snapshot from database and preferences (avoids cold cache race condition)
        val snapshot = repository.getFastScreeningSnapshot()
        return evaluateWithSnapshot(rawNumber, snapshot)
    }

    suspend fun evaluateFromRepository(rawNumber: String?): ScreeningDecision {
        return evaluateFromRepositoryWithDetails(rawNumber).first
    }

    private fun isNumberInContacts(context: Context, number: String): Boolean {
        return getContactName(context, number) != null
    }

    fun getContactName(context: Context, number: String): String? {
        return try {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(number)
            )
            val projection = arrayOf(
                ContactsContract.PhoneLookup._ID,
                ContactsContract.PhoneLookup.DISPLAY_NAME
            )
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
                    if (nameIdx != -1) cursor.getString(nameIdx) else null
                } else null
            }
        } catch (_: SecurityException) {
            null
        } catch (_: Exception) {
            null
        }
    }
}
