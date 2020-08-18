package com.zackmathews.myapplication;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class LocationUtils {
    public static String getCityStateFormattedStringFromLocation(Context context, Location location){
        if(context == null || location == null) return "";
        Geocoder geocoder;
        List<Address> addresses;
        geocoder = new Geocoder(context, Locale.getDefault());

        try {
            addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1); // Here 1 represent max location result to returned, by documents it recommended 1 to 5
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }

        String address = addresses.get(0).getAddressLine(0);
        String city = addresses.get(0).getLocality();
        String state = addresses.get(0).getAdminArea();
        String country = addresses.get(0).getCountryName();
        String postalCode = addresses.get(0).getPostalCode();
        String knownName = addresses.get(0).getFeatureName(); // On

        return city + ",  " + state;
    }
}
