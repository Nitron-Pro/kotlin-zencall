package pro.nitron.zencall.domain.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import pro.nitron.zencall.domain.model.CallAction
import pro.nitron.zencall.domain.model.CallMode
import pro.nitron.zencall.domain.model.CallRule
import pro.nitron.zencall.domain.model.CallerIdentity
import pro.nitron.zencall.domain.model.SystemCategories
import pro.nitron.zencall.domain.util.CallNumberNormalizer

class CallDecisionEngineUnitTest {

    private lateinit var normalizer: CallNumberNormalizer
    private lateinit var engine: CallDecisionEngine

    @Before
    fun setUp() {
        normalizer = CallNumberNormalizer(defaultCountryIso = "IR", defaultCountryPrefix = "98")
        engine = CallDecisionEngine()
    }

    @Test
    fun `normalizer correctly handles international, leading zero and dashed numbers`() {
        assertEquals("+989123456789", normalizer.normalize("09123456789"))
        assertEquals("+989123456789", normalizer.normalize("0912-345-6789"))
        assertEquals("+989123456789", normalizer.normalize("+98 912 345 6789"))
        assertEquals("+989123456789", normalizer.normalize("00989123456789"))
        assertEquals(null, normalizer.normalize("UNKNOWN"))
        assertEquals(null, normalizer.normalize(null))
    }

    @Test
    fun `normalizer matches different representations of same number`() {
        assertTrue(normalizer.matches("09123456789", "+989123456789"))
        assertTrue(normalizer.matches("0912-345-6789", "09123456789"))
        assertFalse(normalizer.matches("09123456789", "09129999999"))
    }

    @Test
    fun `disabled mode allows all calls`() {
        val disabledMode = CallMode(
            id = "personal",
            name = "Personal",
            isEnabled = false,
            defaultAction = CallAction.REJECT
        )
        val caller = CallerIdentity(rawNumber = "09990000000", normalizedNumber = "+989990000000")

        val result = engine.decide(caller, disabledMode)
        assertEquals(CallAction.ALLOW, result.action)
    }

    @Test
    fun `allowed list has top priority over blocked rules`() {
        val personalMode = CallMode(
            id = "personal",
            name = "Personal",
            isEnabled = true,
            rules = listOf(
                CallRule("r1", "personal", SystemCategories.ALLOWED_NUMBER, CallAction.ALLOW),
                CallRule("r2", "personal", SystemCategories.BLOCKED_NUMBER, CallAction.REJECT),
                CallRule("r3", "personal", SystemCategories.UNKNOWN, CallAction.REJECT)
            )
        )

        val caller = CallerIdentity(
            rawNumber = "09121112233",
            normalizedNumber = "+989121112233",
            isAllowedList = true,
            isBlockedList = true // even if flagged blocked, whitelist wins
        )

        val result = engine.decide(caller, personalMode)
        assertEquals(CallAction.ALLOW, result.action)
        assertEquals(SystemCategories.ALLOWED_NUMBER, result.matchedCategoryId)
    }

    @Test
    fun `blocked list rejects caller`() {
        val workMode = CallMode(
            id = "work",
            name = "Work",
            isEnabled = true,
            rules = listOf(
                CallRule("r1", "work", SystemCategories.BLOCKED_NUMBER, CallAction.REJECT),
                CallRule("r2", "work", SystemCategories.CONTACT, CallAction.ALLOW)
            )
        )

        val caller = CallerIdentity(
            rawNumber = "09129990000",
            normalizedNumber = "+989129990000",
            isBlockedList = true,
            isContact = true // blocked takes priority over standard contact
        )

        val result = engine.decide(caller, workMode)
        assertEquals(CallAction.REJECT, result.action)
        assertEquals(SystemCategories.BLOCKED_NUMBER, result.matchedCategoryId)
    }

    @Test
    fun `custom user-created categories evaluate correctly based on rule priority`() {
        val customDeliveryCategoryId = "delivery_drivers"
        val customFamilyCategoryId = "close_family"

        val nightMode = CallMode(
            id = "night",
            name = "Night",
            isEnabled = true,
            rules = listOf(
                CallRule("r1", "night", customFamilyCategoryId, CallAction.ALLOW, priority = 10),
                CallRule("r2", "night", customDeliveryCategoryId, CallAction.SILENCE, priority = 5),
                CallRule("r3", "night", SystemCategories.UNKNOWN, CallAction.REJECT, priority = 0)
            )
        )

        // Caller belongs to custom user category "close_family"
        val familyCaller = CallerIdentity(
            rawNumber = "09120001122",
            normalizedNumber = "+989120001122",
            customCategoryIds = setOf(customFamilyCategoryId)
        )
        val familyResult = engine.decide(familyCaller, nightMode)
        assertEquals(CallAction.ALLOW, familyResult.action)
        assertEquals(customFamilyCategoryId, familyResult.matchedCategoryId)

        // Caller belongs to custom user category "delivery_drivers"
        val deliveryCaller = CallerIdentity(
            rawNumber = "09301234567",
            normalizedNumber = "+989301234567",
            customCategoryIds = setOf(customDeliveryCategoryId)
        )
        val deliveryResult = engine.decide(deliveryCaller, nightMode)
        assertEquals(CallAction.SILENCE, deliveryResult.action)
        assertEquals(customDeliveryCategoryId, deliveryResult.matchedCategoryId)
    }

    @Test
    fun `personal mode allows contact but silences or rejects unknown`() {
        val personalMode = CallMode(
            id = "personal",
            name = "Personal",
            isEnabled = true,
            rules = listOf(
                CallRule("r1", "personal", SystemCategories.CONTACT, CallAction.ALLOW),
                CallRule("r2", "personal", SystemCategories.UNKNOWN, CallAction.SILENCE)
            )
        )

        val contactCaller = CallerIdentity(
            rawNumber = "09121111111",
            normalizedNumber = "+989121111111",
            isContact = true
        )
        val unknownCaller = CallerIdentity(
            rawNumber = "09350000000",
            normalizedNumber = "+989350000000",
            isContact = false
        )

        val contactDecision = engine.decide(contactCaller, personalMode)
        assertEquals(CallAction.ALLOW, contactDecision.action)

        val unknownDecision = engine.decide(unknownCaller, personalMode)
        assertEquals(CallAction.SILENCE, unknownDecision.action)
    }

    @Test
    fun `privacy mask masks middle digits`() {
        val masked = normalizer.maskForLog("09123456789")
        assertEquals("+9891****789", masked)
    }
}
