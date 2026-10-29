package com.example.bivoca.data.local;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(entities = {TopicEntity.class, FlashcardEntity.class, CacheSyncEntity.class},
        version = 2, exportSchema = true)
public abstract class LearningDatabase extends RoomDatabase {
    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE cached_topics ADD COLUMN flashcardCount INTEGER NOT NULL DEFAULT 0");
        }
    };
    private static volatile LearningDatabase instance;

    public abstract LearningCacheDao learningCacheDao();

    static LearningDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (LearningDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(context.getApplicationContext(),
                            LearningDatabase.class, "bivoca-learning-cache.db")
                            .addMigrations(MIGRATION_1_2)
                            .build();
                }
            }
        }
        return instance;
    }
}
