package com.treemapper

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class MarkerStorage(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveMarkers(markers: List<TreeMarker>) {
        val json = gson.toJson(markers)
        prefs.edit().putString(KEY_MARKERS, json).apply()
    }

    fun loadMarkers(): MutableList<TreeMarker> {
        val json = prefs.getString(KEY_MARKERS, null) ?: return mutableListOf()
        val type = object : TypeToken<MutableList<TreeMarker>>() {}.type
        return gson.fromJson(json, type) ?: mutableListOf()
    }

    companion object {
        private const val PREFS_NAME = "tree_mapper_prefs"
        private const val KEY_MARKERS = "markers"
    }
}
