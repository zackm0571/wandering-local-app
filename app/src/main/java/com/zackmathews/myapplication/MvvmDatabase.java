package com.zackmathews.myapplication;

import androidx.room.Database;
import androidx.room.RoomDatabase;
@Database(entities = {YelpData.class}, version = 1)
public abstract class MvvmDatabase extends RoomDatabase {
    public MvvmDatabase(){}
    public abstract YelpData.YelpDAO dao();

}
