package com.zackmathews.myapplication;

public class ServiceLocator {
    private static YelpRepo yelpRepo;

    public static YelpRepo getYelpRepo() {
        if (yelpRepo == null) {
            yelpRepo = new YelpRepo();
        }
        return yelpRepo;
    }
}
