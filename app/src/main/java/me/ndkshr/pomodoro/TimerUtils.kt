package me.ndkshr.pomodoro

import java.util.Locale

object TimerUtils {
    fun timeString(totalTimeLeft: Long):String {
        val mins = (totalTimeLeft / 1000) / 60
        val secs = (totalTimeLeft / 1000) % 60
        return String.format(Locale.getDefault(), "%02d:%02d", mins, secs)
    }
}