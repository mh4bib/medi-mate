package com.medimate.app

import android.content.Context
import java.time.LocalDate

/**
 * Stateless alternation: we store an "anchor" (a date + which hand that date is),
 * and derive every other day from it. A missed day can never break the pattern.
 */
class Prefs(ctx: Context) {
    private val sp = ctx.applicationContext
        .getSharedPreferences("medi_mate", Context.MODE_PRIVATE)

    var enabled: Boolean
        get() = sp.getBoolean("enabled", true)
        set(v) = sp.edit().putBoolean("enabled", v).apply()

    var hour: Int
        get() = sp.getInt("hour", 21)
        set(v) = sp.edit().putInt("hour", v).apply()

    var minute: Int
        get() = sp.getInt("minute", 0)
        set(v) = sp.edit().putInt("minute", v).apply()

    private var anchorDay: Long
        get() = sp.getLong("anchor_day", LocalDate.now().toEpochDay())
        set(v) = sp.edit().putLong("anchor_day", v).apply()

    private var anchorLeft: Boolean
        get() = sp.getBoolean("anchor_left", true)
        set(v) = sp.edit().putBoolean("anchor_left", v).apply()

    /** "Today is <left/right>" — becomes the new anchor. */
    fun setTodayHand(left: Boolean) {
        anchorDay = LocalDate.now().toEpochDay()
        anchorLeft = left
    }

    /** true = left hand, false = right hand */
    fun isLeftOn(date: LocalDate): Boolean {
        val diff = date.toEpochDay() - anchorDay
        val sameAsAnchor = Math.floorMod(diff, 2L) == 0L
        return if (sameAsAnchor) anchorLeft else !anchorLeft
    }
}
