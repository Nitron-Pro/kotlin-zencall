package pro.nitron.zencall.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "zencall_settings")

enum class FirewallMode {
    ALLOW_ALL,
    BLOCK_BLACKLIST,
    BLOCK_UNKNOWN,
    BLOCK_ALL_EXCEPT_CONTACTS,
    BLOCK_ALL
}

class FirewallPreferences(private val context: Context) {
    companion object {
        val KEY_FIREWALL_ENABLED = booleanPreferencesKey("firewall_enabled")
        val KEY_FIREWALL_MODE = stringPreferencesKey("firewall_mode")
        val KEY_BLOCK_PRIVATE_NUMBERS = booleanPreferencesKey("block_private_numbers")
        val KEY_BLACKLIST_NUMBERS = stringSetPreferencesKey("blacklist_numbers")
        val KEY_WHITELIST_NUMBERS = stringSetPreferencesKey("whitelist_numbers")
        val KEY_SILENT_DROP = booleanPreferencesKey("silent_drop")
        val KEY_SKIP_CALL_LOG = booleanPreferencesKey("skip_call_log")
        val KEY_SKIP_NOTIFICATION = booleanPreferencesKey("skip_notification")

        fun normalizeNumber(raw: String): String {
            return raw.replace(Regex("[^0-9+]"), "").trim()
        }
    }

    val isFirewallEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_FIREWALL_ENABLED] ?: true
    }

    val firewallMode: Flow<FirewallMode> = context.dataStore.data.map { prefs ->
        val modeName = prefs[KEY_FIREWALL_MODE] ?: FirewallMode.BLOCK_UNKNOWN.name
        try {
            FirewallMode.valueOf(modeName)
        } catch (_: Exception) {
            FirewallMode.BLOCK_UNKNOWN
        }
    }

    val blockPrivateNumbers: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_BLOCK_PRIVATE_NUMBERS] ?: true
    }

    val blacklist: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[KEY_BLACKLIST_NUMBERS] ?: emptySet()
    }

    val whitelist: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[KEY_WHITELIST_NUMBERS] ?: emptySet()
    }

    val silentDrop: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_SILENT_DROP] ?: true
    }

    suspend fun setFirewallEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_FIREWALL_ENABLED] = enabled
        }
    }

    suspend fun setFirewallMode(mode: FirewallMode) {
        context.dataStore.edit { prefs ->
            prefs[KEY_FIREWALL_MODE] = mode.name
        }
    }

    suspend fun setBlockPrivateNumbers(block: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_BLOCK_PRIVATE_NUMBERS] = block
        }
    }

    suspend fun addToBlacklist(number: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[KEY_BLACKLIST_NUMBERS]?.toMutableSet() ?: mutableSetOf()
            current.add(normalizeNumber(number))
            prefs[KEY_BLACKLIST_NUMBERS] = current
        }
    }

    suspend fun removeFromBlacklist(number: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[KEY_BLACKLIST_NUMBERS]?.toMutableSet() ?: return@edit
            current.remove(normalizeNumber(number))
            prefs[KEY_BLACKLIST_NUMBERS] = current
        }
    }

    suspend fun addToWhitelist(number: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[KEY_WHITELIST_NUMBERS]?.toMutableSet() ?: mutableSetOf()
            current.add(normalizeNumber(number))
            prefs[KEY_WHITELIST_NUMBERS] = current
        }
    }

    suspend fun removeFromWhitelist(number: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[KEY_WHITELIST_NUMBERS]?.toMutableSet() ?: return@edit
            current.remove(normalizeNumber(number))
            prefs[KEY_WHITELIST_NUMBERS] = current
        }
    }

    // Synchronous reads for rapid CallScreening evaluation (<5ms budget)
    suspend fun getSnapshot(): FirewallConfigSnapshot {
        val prefs = context.dataStore.data.first()
        val enabled = prefs[KEY_FIREWALL_ENABLED] ?: true
        val mode = try {
            FirewallMode.valueOf(prefs[KEY_FIREWALL_MODE] ?: FirewallMode.BLOCK_UNKNOWN.name)
        } catch (_: Exception) {
            FirewallMode.BLOCK_UNKNOWN
        }
        val blockPrivate = prefs[KEY_BLOCK_PRIVATE_NUMBERS] ?: true
        val blacklistSet = prefs[KEY_BLACKLIST_NUMBERS] ?: emptySet()
        val whitelistSet = prefs[KEY_WHITELIST_NUMBERS] ?: emptySet()
        val silent = prefs[KEY_SILENT_DROP] ?: true

        return FirewallConfigSnapshot(
            isEnabled = enabled,
            mode = mode,
            blockPrivateNumbers = blockPrivate,
            blacklist = blacklistSet,
            whitelist = whitelistSet,
            silentDrop = silent
        )
    }
}

data class FirewallConfigSnapshot(
    val isEnabled: Boolean,
    val mode: FirewallMode,
    val blockPrivateNumbers: Boolean,
    val blacklist: Set<String>,
    val whitelist: Set<String>,
    val silentDrop: Boolean
)
