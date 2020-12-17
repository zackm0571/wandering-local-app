package life.wanderinglocal

import android.Manifest

object Constants {
    // Preference keys
    const val PREFS_NAME = "life.wanderinglocal.widget.WanderingWidget"
    const val PREF_PREFIX_KEY = "appwidget_"
    const val PREF_LOCATION_KEY = "location_"
    const val PREF_LAT_KEY = "location_lat_"
    const val PREF_LNG_KEY = "location_lng_"
    const val PREF_CATEGORY_KEY = "category_"
    const val PREF_LAST_SEARCHED_CATEGORY_KEY = "last_category_"
    const val PREF_WIDGET_ID_KEY = "widgetId"

    // Fragments
    const val TIMELINE_FRAGMENT_TAG = "TimelineFragment"
    const val TIMELINE_DETAIL_FRAGMENT_TAG = "TimelineDetailFragment"

    // Defaults
    @JvmField
    val DEFAULT_SEARCH_CATEGORIES = arrayOf("American", "Bars", "Breweries", "Breakfast", "British", "Burmese", "Burgers", "Chinese", "Food Trucks", "Farmer's Market", "Coffee", "Dessert", "Ethiopian", "Indian", "Jamaican", "Mexican", "Ramen", "Spanish", "Sushi", "Wings")
    const val DEFAULT_SEARCH_TERM = "Coffee"
    const val SORT_RATING = "rating"
    const val DEFAULT_RESULT_LIMIT = 50
    const val DEFAULT_SEARCH_RADIUS = 10000
    const val DEFAULT_MIN_RATING = 4.3
    const val SEARCH_BY_OPEN_NOW = true
    const val DISABLED_WIDGET = "-1"

    // Manifest / Permissions
    const val WL_ACTION_WIDGET_CLICK = "life.wanderinglocal.WIDGET_CLICK"
    @JvmField
    val PERMISSIONS = arrayOf(Manifest.permission_group.LOCATION, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
    const val PERMISSION_REQUEST_CODE = 71
}