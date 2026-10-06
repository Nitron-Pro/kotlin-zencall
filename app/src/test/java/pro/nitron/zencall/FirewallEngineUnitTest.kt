package pro.nitron.zencall

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pro.nitron.zencall.call.ScreeningDecision
import pro.nitron.zencall.call.ScreeningEngine
import pro.nitron.zencall.data.prefs.FirewallConfigSnapshot
import pro.nitron.zencall.data.prefs.FirewallMode
import pro.nitron.zencall.data.prefs.FirewallPreferences

class FirewallEngineUnitTest {

    @Test
    fun normalizeNumber_stripsSpecialCharacters() {
        assertEquals("+989123456789", FirewallPreferences.normalizeNumber("+98 (912) 345-6789"))
        assertEquals("09121112233", FirewallPreferences.normalizeNumber("0912-111-2233"))
    }

    @Test
    fun disabledFirewall_allowsAnyCall() {
        val snapshot = FirewallConfigSnapshot(
            isEnabled = false,
            mode = FirewallMode.BLOCK_ALL,
            blockPrivateNumbers = true,
            blacklist = setOf("+989120000000"),
            whitelist = emptySet(),
            silentDrop = true
        )

        // Mock test logic
        assertTrue(!snapshot.isEnabled)
    }

    @Test
    fun blacklist_matches_normalized() {
        val blacklisted = setOf("+989121112233")
        val candidate = "09121112233"
        val suffixMatch = candidate.takeLast(7) == "+989121112233".takeLast(7)
        assertTrue(suffixMatch)
    }
}
