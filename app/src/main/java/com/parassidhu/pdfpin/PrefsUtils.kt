package com.parassidhu.pdfpin

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager

private lateinit var sharedPreferences: SharedPreferences
private lateinit var editor: SharedPreferences.Editor

private const val ads_key = "Ads-Key"

fun initialize(context: Context) {
    sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    editor = sharedPreferences.edit()
}

fun saveOffline(name: String, value: String) {
    editor.putString(name, value)
    editor.apply()
}

fun saveOffline(name: String, value: Int) {
    editor.putInt(name, value)
    editor.apply()
}

fun getValue(query: String): String {
    return sharedPreferences.getString(query,
            "Connect to the internet to get content!")?:"Connect to the internet to get content!"
}

fun getIntValue(query: String, def: Int): Int {
    return sharedPreferences.getInt(query, def)
}

fun setShowAds(boolean: Boolean) {
    editor.putBoolean(ads_key, boolean)
    editor.apply()
}

fun getShowAds(): Boolean = sharedPreferences.getBoolean(ads_key,true)