package com.example.newshub.core.location

import android.content.Context

data class NewsLocationContext(
    val label: String = "",
    val area: String = "",
    val country: String = "",
    val scope: String = "Local",
    val latitude: Double? = null,
    val longitude: Double? = null
) : java.io.Serializable

class NewsLocationStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): NewsLocationContext {
        val lat = prefs.getString(KEY_LAT, null)?.toDoubleOrNull()
        val lng = prefs.getString(KEY_LNG, null)?.toDoubleOrNull()
        return NewsLocationContext(
            label = prefs.getString(KEY_LABEL, "").orEmpty(),
            area = prefs.getString(KEY_AREA, "").orEmpty(),
            country = prefs.getString(KEY_COUNTRY, "").orEmpty(),
            scope = prefs.getString(KEY_SCOPE, "Local") ?: "Local",
            latitude = lat,
            longitude = lng
        )
    }

    fun save(context: NewsLocationContext) {
        prefs.edit()
            .putString(KEY_LABEL, context.label)
            .putString(KEY_AREA, context.area)
            .putString(KEY_COUNTRY, context.country)
            .putString(KEY_SCOPE, context.scope)
            .putString(KEY_LAT, context.latitude?.toString())
            .putString(KEY_LNG, context.longitude?.toString())
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "news_location_prefs"
        private const val KEY_LABEL = "label"
        private const val KEY_AREA = "area"
        private const val KEY_COUNTRY = "country"
        private const val KEY_SCOPE = "scope"
        private const val KEY_LAT = "lat"
        private const val KEY_LNG = "lng"
    }
}
