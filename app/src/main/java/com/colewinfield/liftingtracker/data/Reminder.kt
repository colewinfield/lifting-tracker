package com.colewinfield.liftingtracker.data

/**
 * Tone of voice for reminder copy. The JSX surfaces this as a "MEANNESS LEVEL" picker — the
 * label is the user-facing tier name, [sample] is the preview shown next to each option in the
 * Reminders screen.
 */
enum class ReminderTier(val label: String, val sample: String) {
    POLITE("Polite", "Don't forget about your session today."),
    FIRM("Firm", "Gym bag's waiting. Session in 90."),
    PLAYFUL_SHAME("Playful shame", "Really? Couch again? Your squat misses you."),
}

/**
 * The four nudge categories from the JSX `NotificationsScreen`. Three are pre-session leads
 * (fire N minutes before the configured session start), and [MISSED] fires after the session
 * window if the user hasn't logged anything for the day.
 *
 * - [minutesBefore] is positive for pre-session leads. [MISSED] uses the [ReminderDefaults]
 *   `MISSED_DELAY_MINUTES` offset *after* the session start instead.
 * - [label] is the user-facing nudge name.
 * - [sample] is the JSX preview copy.
 */
enum class ReminderLead(
    val minutesBefore: Int,
    val label: String,
    val sample: String,
) {
    THREE_HOURS(180, "3 hours before", "Lifting in 3 hours — make sure you've got a shaker."),
    NINETY_MIN(90, "90 minutes before", "90 minutes out. Change, hydrate, and start stretching."),
    THIRTY_MIN(30, "30 minutes before", "30 minutes. Get moving."),
    MISSED(0, "If you skipped", "You missed today. Flex that discipline muscle instead."),
}

/**
 * Reminder defaults that aren't user-configurable yet. The JSX intentionally doesn't expose a
 * session-time picker, so we hardcode 18:00 as the assumed lift hour. If/when the design adds a
 * picker, persist these in DataStore and route through this object.
 */
object ReminderDefaults {
    const val SESSION_START_HOUR = 18
    const val SESSION_START_MINUTE = 0

    /** Minutes after session start when MISSED fires (4 hours → 22:00 with default 18:00). */
    const val MISSED_DELAY_MINUTES = 4 * 60
}

/**
 * Final notification copy for a (tier, lead) pair. The lead carries the timing, the tier shapes
 * the tone — same scheduling for all tiers, only the rendered text differs at fire time.
 */
data class ReminderCopy(val title: String, val body: String)

/** Build the title + body for a reminder fire event. Pure: lookup table indexed by (tier, lead). */
fun reminderCopyFor(tier: ReminderTier, lead: ReminderLead): ReminderCopy = when (tier) {
    ReminderTier.POLITE -> when (lead) {
        ReminderLead.THREE_HOURS -> ReminderCopy(
            title = "Reminder",
            body = "Session in 3 hours. Plenty of time to get your gear together.",
        )
        ReminderLead.NINETY_MIN -> ReminderCopy(
            title = "Reminder",
            body = "Session in 90 minutes. Maybe start winding down.",
        )
        ReminderLead.THIRTY_MIN -> ReminderCopy(
            title = "Reminder",
            body = "Session in 30 minutes. Almost time.",
        )
        ReminderLead.MISSED -> ReminderCopy(
            title = "No worries",
            body = "Missed today's session — tomorrow's a new chance.",
        )
    }
    ReminderTier.FIRM -> when (lead) {
        ReminderLead.THREE_HOURS -> ReminderCopy(
            title = "Lifting Tracker",
            body = "Lifting in 3 hours — make sure you've got a shaker.",
        )
        ReminderLead.NINETY_MIN -> ReminderCopy(
            title = "Lifting Tracker",
            body = "90 minutes out. Change, hydrate, and start stretching.",
        )
        ReminderLead.THIRTY_MIN -> ReminderCopy(
            title = "Lifting Tracker",
            body = "30 minutes. Get moving.",
        )
        ReminderLead.MISSED -> ReminderCopy(
            title = "Missed it",
            body = "You missed today. Flex that discipline muscle instead.",
        )
    }
    ReminderTier.PLAYFUL_SHAME -> when (lead) {
        ReminderLead.THREE_HOURS -> ReminderCopy(
            title = "Heads up \uD83D\uDC40",
            body = "3 hours. The bar's not going to lift itself.",
        )
        ReminderLead.NINETY_MIN -> ReminderCopy(
            title = "Tick tock",
            body = "Gym bag's waiting. Session in 90.",
        )
        ReminderLead.THIRTY_MIN -> ReminderCopy(
            title = "Move it",
            body = "30 minutes. Stop scrolling.",
        )
        ReminderLead.MISSED -> ReminderCopy(
            title = "Really?",
            body = "Couch again? Your squat misses you.",
        )
    }
}
