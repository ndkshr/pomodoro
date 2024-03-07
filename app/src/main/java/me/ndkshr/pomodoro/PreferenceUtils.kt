package me.ndkshr.pomodoro

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager

class PreferenceUtils(context: Context) {

    private val sharedPref: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    fun writeString(key: String, value: String) {
        with(sharedPref.edit()) {
            putString(key, value)
        }
    }

    fun writeLong(key: String, value: Long) {
        with(sharedPref.edit()) {
            putLong(key, value)
        }
    }

    fun writeInt(key: String, value: Int) {
        with(sharedPref.edit()) {
            putInt(key, value)
        }
    }

    fun writeBoolean(key: String, value: Boolean) {
        with(sharedPref.edit()) {
            putBoolean(key, value)
        }
    }

    fun getLong(key: String, value: Long = 0): Long {
        return sharedPref.getLong(key, value)
    }

    fun getInt(key: String, value: Int = 0): Int {
        return sharedPref.getInt(key, value)
    }
}
