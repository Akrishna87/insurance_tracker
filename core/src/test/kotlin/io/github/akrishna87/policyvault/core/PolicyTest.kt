package io.github.akrishna87.policyvault.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class PolicyTest {

    private fun daysFromToday(days: Long): Long = LocalDate.now().plusDays(days).toEpochDay()

    private fun policy(
        type: PolicyType = PolicyType.HEALTH,
        insurer: String = "Star Health",
        renewalDate: Long? = null,
        premium: Double? = null,
        frequency: Frequency? = null,
        sumInsured: Double? = null,
        reminderDays: Int = 30,
        startDate: Long? = null,
    ) = Policy(
        id = "p1",
        type = type,
        insurer = insurer,
        renewalDate = renewalDate,
        startDate = startDate,
        premium = premium,
        frequency = frequency,
        sumInsured = sumInsured,
        reminderDays = reminderDays,
    )

    @Test
    fun `status active when no renewal date`() {
        assertEquals(Status.ACTIVE, PolicyLogic.status(policy(renewalDate = null)))
    }

    @Test
    fun `status active today expires tomorrow`() {
        assertEquals(Status.ACTIVE, PolicyLogic.status(policy(renewalDate = daysFromToday(1))))
        assertEquals(Status.EXPIRED, PolicyLogic.status(policy(renewalDate = daysFromToday(-1))))
        // The renewal day itself is still active.
        assertEquals(Status.ACTIVE, PolicyLogic.status(policy(renewalDate = LocalDate.now().toEpochDay())))
    }

    @Test
    fun `days left counts down and goes negative`() {
        assertEquals(7L, PolicyLogic.daysLeft(policy(renewalDate = daysFromToday(7))))
        assertEquals(-2L, PolicyLogic.daysLeft(policy(renewalDate = daysFromToday(-2))))
        assertNull(PolicyLogic.daysLeft(policy(renewalDate = null)))
    }

    @Test
    fun `reminder thresholds are lead then seven one and zero`() {
        assertEquals(listOf(30L, 7L, 1L, 0L), PolicyLogic.reminderDaysLeft(policy()))
        assertEquals(listOf(15L, 7L, 1L, 0L), PolicyLogic.reminderDaysLeft(policy(reminderDays = 15)))
        // A custom lead that collides with 7 is not repeated.
        assertEquals(listOf(7L, 1L, 0L), PolicyLogic.reminderDaysLeft(policy(reminderDays = 7)))
        assertTrue(PolicyLogic.reminderDaysLeft(policy(reminderDays = 0)).isEmpty())
    }

    @Test
    fun `reminder due once per day per policy`() {
        val p = policy(renewalDate = daysFromToday(7))
        val empty = Vault(policies = listOf(p))
        assertTrue(PolicyLogic.reminderDue(p, empty))
        // Already notified today: not due again.
        assertFalse(PolicyLogic.reminderDue(p, empty.copy(notified = mapOf("p1" to LocalDate.now().toEpochDay()))))
        // Not a threshold day.
        assertFalse(PolicyLogic.reminderDue(policy(renewalDate = daysFromToday(10)), empty))
    }

    @Test
    fun `annual premium multiplies by frequency`() {
        assertEquals(12000.0, PolicyLogic.annualPremium(policy(premium = 1000.0, frequency = Frequency.MONTHLY))!!, 0.001)
        assertEquals(8000.0, PolicyLogic.annualPremium(policy(premium = 4000.0, frequency = Frequency.HALF_YEARLY))!!, 0.001)
        assertNull(PolicyLogic.annualPremium(policy(premium = 500000.0, frequency = Frequency.ONE_TIME)))
        assertNull(PolicyLogic.annualPremium(policy(premium = null, frequency = Frequency.YEARLY)))
    }

    @Test
    fun `totals count only active policies`() {
        val active = policy(sumInsured = 500000.0, premium = 12000.0, frequency = Frequency.YEARLY)
        val expired = policy(insurer = "Old", renewalDate = daysFromToday(-5), sumInsured = 200000.0, premium = 5000.0, frequency = Frequency.YEARLY)
        val policies = listOf(active, expired)
        assertEquals(500000.0, PolicyLogic.totalCover(policies), 0.001)
        assertEquals(12000.0, PolicyLogic.totalAnnualPremium(policies), 0.001)
    }

    @Test
    fun `renewing soon lists active within the window sorted soonest first`() {
        val soon = policy(insurer = "Soon", renewalDate = daysFromToday(10))
        val sooner = policy(insurer = "Sooner", renewalDate = daysFromToday(3))
        val far = policy(insurer = "Far", renewalDate = daysFromToday(200))
        val list = PolicyLogic.renewingSoon(listOf(far, soon, sooner))
        assertEquals(listOf("Sooner", "Soon"), list.map { it.insurer })
    }

    @Test
    fun `mark renewed shifts start and renewal a year forward`() {
        val renewal = LocalDate.now().plusDays(3)
        val p = policy(renewalDate = renewal.toEpochDay(), startDate = renewal.minusYears(1).toEpochDay())
        val renewed = PolicyLogic.renewed(p)
        assertEquals(renewal.toEpochDay(), renewed.startDate)
        assertEquals(renewal.plusYears(1).toEpochDay(), renewed.renewalDate)
    }

    @Test
    fun `json round trip keeps every field`() {
        val vault = Vault(
            policies = listOf(
                Policy(
                    id = "abc",
                    type = PolicyType.OFFICE,
                    insurer = "HDFC Ergo",
                    planName = "Group Health",
                    policyNumber = "G-123",
                    sumInsured = 500000.0,
                    premium = null,
                    frequency = null,
                    startDate = 20000,
                    renewalDate = 21000,
                    members = listOf("Self", "Spouse"),
                    nominee = "Wife",
                    vehicleNumber = "",
                    employer = "Acme Corp",
                    claimContact = "MediAssist",
                    notes = "Covers parents too",
                    reminderDays = 15,
                ),
            ),
            notified = mapOf("abc" to 20500L),
        )
        val decoded = PolicyIO.decode(PolicyIO.encode(vault))
        assertEquals(vault, decoded)
    }

    @Test
    fun `decode ignores unknown keys from future versions`() {
        val text = """{"version":1,"policies":[{"id":"x","type":"HEALTH","insurer":"LIC","futureField":true}],"notified":{}}"""
        val vault = PolicyIO.decode(text)
        assertEquals(1, vault.policies.size)
        assertEquals("LIC", vault.policies[0].insurer)
    }

    @Test
    fun `indian grouping and short forms`() {
        assertEquals("₹12,50,000", Format.inr(1250000.0))
        assertEquals("₹1,00,000", Format.inr(100000.0))
        assertEquals("₹999", Format.inr(999.0))
        assertEquals("—", Format.inr(null))
        assertEquals("₹1.5 Cr", Format.short(15000000.0))
        assertEquals("₹12.5 L", Format.short(1250000.0))
        assertEquals("₹99,999", Format.short(99999.0))
        assertEquals("17 Nov 2026", Format.date(LocalDate.of(2026, 11, 17).toEpochDay()))
    }
}
