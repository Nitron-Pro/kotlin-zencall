package pro.nitron.zencall.call

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import pro.nitron.zencall.data.prefs.FirewallConfigSnapshot
import pro.nitron.zencall.data.prefs.FirewallMode
import pro.nitron.zencall.data.prefs.FirewallPreferences

sealed class ScreeningDecision {
    data object Allow : ScreeningDecision()
    data class Block(
        val reason: String,
        val shouldDisallowCall: Boolean = true,
        val shouldRejectCall: Boolean = true,
        val shouldSkipCallLog: Boolean = false,
        val shouldSkipNotification: Boolean = true
    ) : ScreeningDecision()
}

class ScreeningEngine(private val context: Context) {

    fun evaluate(rawNumber: String?, snapshot: FirewallConfigSnapshot): ScreeningDecision {
        if (!snapshot.isEnabled) {
            return ScreeningDecision.Allow
        }

        // 1. Private / Hidden / Anonymous Number Check
        val isPrivate = rawNumber.isNullOrBlank() || rawNumber.equals("UNKNOWN", ignoreCase = true) || rawNumber.equals("PRIVATE", ignoreCase = true)
        if (isPrivate) {
            return if (snapshot.blockPrivateNumbers) {
                ScreeningDecision.Block(
                    reason = "Private / Unknown Caller ID",
                    shouldSkipNotification = snapshot.silentDrop
                )
            } else {
                ScreeningDecision.Allow
            }
        }

        val normalized = FirewallPreferences.normalizeNumber(rawNumber)

        // 2. Whitelist Check (always highest priority)
        if (snapshot.whitelist.any { matchesNumber(it, normalized) }) {
            return ScreeningDecision.Allow
        }

        // 3. Blacklist Check
        if (snapshot.blacklist.any { matchesNumber(it, normalized) }) {
            return ScreeningDecision.Block(
                reason = "Matched Blacklist",
                shouldSkipNotification = snapshot.silentDrop
            )
        }

        // 4. Mode-based Evaluation
        return when (snapshot.mode) {
            FirewallMode.ALLOW_ALL -> {
                ScreeningDecision.Allow
            }
            FirewallMode.BLOCK_BLACKLIST -> {
                // If not in blacklist, allow
                ScreeningDecision.Allow
            }
            FirewallMode.BLOCK_UNKNOWN, FirewallMode.BLOCK_ALL_EXCEPT_CONTACTS -> {
                val isContact = isNumberInContacts(context, normalized)
                if (isContact) {
                    ScreeningDecision.Allow
                } else {
                    ScreeningDecision.Block(
                        reason = "Non-contact Number (Firewall Active)",
                        shouldSkipNotification = snapshot.silentDrop
                    )
                }
            }
            FirewallMode.BLOCK_ALL -> {
                ScreeningDecision.Block(
                    reason = "Block All Calls Mode Active",
                    shouldSkipNotification = snapshot.silentDrop
                )
            }
        }
    }

    private fun matchesNumber(pattern: String, candidate: String): Boolean {
        val normPattern = FirewallPreferences.normalizeNumber(pattern)
        if (candidate == normPattern) return true
        // Allow suffix match for country codes e.g., 0912... vs +98912...
        if (candidate.length >= 7 && normPattern.length >= 7) {
            val suffixCandidate = candidate.takeLast(7)
            val suffixPattern = normPattern.takeLast(7)
            return suffixCandidate == suffixPattern
        }
        return false
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
            // If contact permission not granted, fail safe to allow or treat as non-contact based on policy
            false
        } catch (_: Exception) {
            false
        }
    }
}
