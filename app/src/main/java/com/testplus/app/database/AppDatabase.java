package com.testplus.app.database;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import com.testplus.app.database.dao.*;
import com.testplus.app.database.entities.*;

@Database(
    entities = {OptikForm.class, OptikFormAlan.class, Sinav.class, CevapAnahtari.class, OgrenciKagidi.class},
    version = 1,
    exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {
    private static volatile AppDatabase INSTANCE;

    public abstract OptikFormDao optikFormDao();
    public abstract OptikFormAlanDao optikFormAlanDao();
    public abstract SinavDao sinavDao();
    public abstract CevapAnahtariDao cevapAnahtariDao();
    public abstract OgrenciKagidiDao ogrenciKagidiDao();

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                        context.getApplicationContext(),
                        AppDatabase.class,
                        "testplus_db"
                    ).build();
                }
            }
        }
        return INSTANCE;
    }
}
