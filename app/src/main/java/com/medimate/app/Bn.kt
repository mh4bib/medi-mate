package com.medimate.app

import android.content.Context
import java.util.Locale

/** Bengali digits and Bengali-style time formatting. */
object Bn {
    private val digits = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')

    fun num(s: String): String =
        s.map { if (it in '0'..'9') digits[it - '0'] else it }.joinToString("")

    /** e.g. 21:05 -> "রাত ৯:০৫" */
    fun time(hour: Int, minute: Int): String {
        val period = when (hour) {
            in 4..5 -> "ভোর"
            in 6..11 -> "সকাল"
            in 12..15 -> "দুপুর"
            in 16..17 -> "বিকাল"
            in 18..19 -> "সন্ধ্যা"
            else -> "রাত"
        }
        val h12 = if (hour % 12 == 0) 12 else hour % 12
        val mm = String.format(Locale.ROOT, "%02d", minute)
        return "$period ${num("$h12:$mm")}"
    }

    /** "বাম হাতে" / "ডান হাতে" */
    fun handLoc(ctx: Context, left: Boolean): String =
        ctx.getString(if (left) R.string.hand_left_loc else R.string.hand_right_loc)
}
