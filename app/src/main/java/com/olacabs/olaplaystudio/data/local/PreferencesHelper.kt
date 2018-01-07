package com.olacabs.olaplaystudio.data.local

import android.content.Context
import android.preference.PreferenceManager
import com.chibatching.kotpref.KotprefModel


object UserPrefs : KotprefModel() {
}

class AppPrefs(context: Context) {
    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)
    val favList: MutableSet<String> get() = preferences.getStringSet(PREF_FAV_LIST, emptySet())

    fun addToFav(item: String) {
        val oldList = mutableSetOf<String>().apply {
            addAll(preferences.getStringSet(PREF_FAV_LIST, emptySet()))
            add(item)
        }
        preferences.edit().putStringSet(PREF_FAV_LIST, oldList).apply()
    }

    fun removeFromFav(item: String) {
        val oldList = mutableSetOf<String>().apply {
            addAll(preferences.getStringSet(PREF_FAV_LIST, emptySet()))
            remove(item)
        }
        preferences.edit().putStringSet(PREF_FAV_LIST, oldList).apply()
    }

    companion object {
        private val PREF_FAV_LIST = "favList"
    }
}

