package io.github.akrishna87.policyvault.core

import kotlinx.serialization.Serializable

/** The kinds of insurance a household keeps. */
enum class PolicyType(val label: String) {
    HEALTH("Health"),
    TERM("Term"),
    LIFE("Life"),
    CAR("Car"),
    BIKE("Bike"),
    ACCIDENT("Accident"),
    TRAVEL("Travel"),
    HOME("Home"),
    OFFICE("Office / group"),
    OTHER("Other");

    val isMotor: Boolean get() = this == CAR || this == BIKE
}

/** How often the premium is paid. [perYear] is null for a one-time premium. */
enum class Frequency(val label: String, val perYear: Int?) {
    ONE_TIME("One-time", null),
    MONTHLY("Monthly", 12),
    QUARTERLY("Quarterly", 4),
    HALF_YEARLY("Half-yearly", 2),
    YEARLY("Yearly", 1),
}

/**
 * One insurance policy. Dates are epoch days (LocalDate.toEpochDay) so the model stays
 * plain Kotlin with no Android or java.time dependency at the field level.
 * All fields except [type] and [insurer] are optional: a group policy may have no premium
 * the user pays, a term policy may have no fixed end date, and so on.
 */
@Serializable
data class Policy(
    val id: String,
    val type: PolicyType,
    val insurer: String,
    val planName: String = "",
    val policyNumber: String = "",
    /** The cover amount, in rupees. */
    val sumInsured: Double? = null,
    /** The premium per payment, in rupees. */
    val premium: Double? = null,
    val frequency: Frequency? = null,
    /** When cover started. */
    val startDate: Long? = null,
    /** When the policy comes up for renewal; null when there is no fixed expiry. */
    val renewalDate: Long? = null,
    /** Who is covered, by name (e.g. Self, Spouse, parents' names). */
    val members: List<String> = emptyList(),
    val nominee: String = "",
    /** Registration number, for car and bike policies. */
    val vehicleNumber: String = "",
    /** Employer or group name, for office insurance. */
    val employer: String = "",
    /** Third-party administrator or claim contact. */
    val claimContact: String = "",
    val notes: String = "",
    /** Days before the renewal date to start reminding; 0 turns reminders off. */
    val reminderDays: Int = DEFAULT_REMINDER_DAYS,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
) {
    companion object {
        const val DEFAULT_REMINDER_DAYS = 30
    }
}

/** The whole vault: every policy, plus what has already been notified (for reminders). */
@Serializable
data class Vault(
    val version: Int = 1,
    val policies: List<Policy> = emptyList(),
    /** policy id -> epoch day it was last notified on, so each threshold fires once. */
    val notified: Map<String, Long> = emptyMap(),
)

/** Where a policy stands today. */
enum class Status { ACTIVE, EXPIRED }

object PolicyLogic {

    /** Today as an epoch day. */
    fun today(): Long = java.time.LocalDate.now().toEpochDay()

    fun status(policy: Policy, today: Long = today()): Status =
        when {
            policy.renewalDate == null || policy.renewalDate >= today -> Status.ACTIVE
            else -> Status.EXPIRED
        }

    /** Days until the renewal date; negative once expired, null when there is no date. */
    fun daysLeft(policy: Policy, today: Long = today()): Long? =
        policy.renewalDate?.let { it - today }

    /**
     * The days-left values on which a reminder notification is sent: the policy's own
     * lead time, then 7 and 1 day before, and the day itself. Distinct and descending.
     */
    fun reminderDaysLeft(policy: Policy): List<Long> =
        if (policy.reminderDays <= 0) emptyList()
        else listOf(policy.reminderDays.toLong(), 7L, 1L, 0L).distinct().sortedDescending()

    /** True when a notification for [policy] is due today (and has not been sent yet). */
    fun reminderDue(policy: Policy, vault: Vault, today: Long = today()): Boolean {
        val days = daysLeft(policy, today) ?: return false
        if (days !in reminderDaysLeft(policy)) return false
        if (status(policy, today) == Status.EXPIRED && days < 0) return false
        return vault.notified[policy.id] != today
    }

    /** The premium paid per year; null for one-time premiums or unknown premiums. */
    fun annualPremium(policy: Policy): Double? {
        val premium = policy.premium ?: return null
        val perYear = policy.frequency?.perYear ?: return null
        return premium * perYear
    }

    /** Cover amounts that count towards the household's total. */
    fun totalCover(policies: List<Policy>): Double =
        policies.filter { status(it) == Status.ACTIVE }.sumOf { it.sumInsured ?: 0.0 }

    fun totalAnnualPremium(policies: List<Policy>): Double =
        policies.filter { status(it) == Status.ACTIVE }.sumOf { annualPremium(it) ?: 0.0 }

    /** Active policies that come up for renewal within [withinDays], soonest first. */
    fun renewingSoon(policies: List<Policy>, withinDays: Long = 90, today: Long = today()): List<Policy> =
        policies
            .filter { it.renewalDate != null && status(it, today) == Status.ACTIVE && (it.renewalDate - today) <= withinDays }
            .sortedBy { it.renewalDate }

    fun expired(policies: List<Policy>, today: Long = today()): List<Policy> =
        policies.filter { status(it, today) == Status.EXPIRED }.sortedByDescending { it.renewalDate }

    /** Shifts a policy one year forward for "mark renewed": new start is the old renewal. */
    fun renewed(policy: Policy, today: Long = today()): Policy {
        val oldRenewal = policy.renewalDate ?: return policy
        val newStart = oldRenewal
        val newRenewal = java.time.LocalDate.ofEpochDay(oldRenewal).plusYears(1).toEpochDay()
        return policy.copy(
            startDate = newStart,
            renewalDate = newRenewal,
            updatedAt = today,
        )
    }
}

/** Formatting helpers shared by the app (Indian grouping and lakh/crore short forms). */
object Format {
    private fun group(whole: Long): String {
        val digits = kotlin.math.abs(whole).toString()
        val last3 = digits.takeLast(3)
        val rest = digits.dropLast(3)
        // Indian grouping: the last three digits, then pairs from the right of the rest.
        val grouped = if (rest.isEmpty()) last3
        else rest.reversed().chunked(2).map { it.reversed() }.reversed().joinToString(",") + "," + last3
        return (if (whole < 0) "-" else "") + grouped
    }

    /** ₹12,50,000 style, whole rupees. */
    fun inr(amount: Double?): String =
        if (amount == null) "—" else "₹" + group(amount.toLong())

    /** ₹1.25 Cr / ₹12.5 L / full rupees below a lakh. */
    fun short(amount: Double?): String {
        if (amount == null) return "—"
        return when {
            amount >= 1e7 -> "₹" + trim(amount / 1e7) + " Cr"
            amount >= 1e5 -> "₹" + trim(amount / 1e5) + " L"
            else -> inr(amount)
        }
    }

    private fun trim(value: Double): String {
        val s = if (value >= 100) "%.0f" else "%.2f"
        return s.format(java.util.Locale.US, value).trimEnd('0').trimEnd('.')
    }

    private val MONTHS = listOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
    )

    /** 17 Nov 2026 style. */
    fun date(epochDay: Long?): String =
        if (epochDay == null) "—" else java.time.LocalDate.ofEpochDay(epochDay).let {
            "${it.dayOfMonth} ${MONTHS[it.monthValue - 1]} ${it.year}"
        }
}

/** Reads and writes the vault as JSON; unknown keys are kept out of the way of upgrades. */
object PolicyIO {
    private val json = kotlinx.serialization.json.Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    fun encode(vault: Vault): String = json.encodeToString(Vault.serializer(), vault)

    fun decode(text: String): Vault = json.decodeFromString(Vault.serializer(), text)
}
