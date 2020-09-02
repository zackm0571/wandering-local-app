package com.zackmathews.myapplication;

import android.Manifest;

public class Constants {
    public static final String PREFS_NAME = "com.zackmathews.myapplication.widget.WanderingWidget";
    public static final String PREF_PREFIX_KEY = "appwidget_";
    public static final String PREF_LOCATION_KEY = "location_";
    public static final String PREF_CATEGORY_KEY = "category_";

    public static final String DEFAULT_SEARCH_TERM = "coffee";

    public static final String[] PERMISSIONS = new String[]{Manifest.permission_group.LOCATION, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
    public static final String[] DEFAULT_SEARCH_CATEGORIES = new String[]{"American", "Burmese", "Chinese", "Coffee", "Dessert", "Ethiopian", "Indian", "Mexican", "Ramen", "Sushi"};
    public static final int PERMISSION_REQUEST_CODE = 71;
}
