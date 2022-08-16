package com.keystone.cold.mpc.db;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;
import com.keystone.cold.AppExecutors;
import com.keystone.cold.mpc.db.dao.MPCWalletDao;
import com.keystone.cold.mpc.db.entity.MPCWalletEntity;

/**
 * @author yudenghao
 * @date 2022/8/1
 */

@Database(entities = { MPCWalletEntity.class}, version = 1)
abstract public class MPCDatabase extends RoomDatabase {

  private static final String DATABASE_NAME = "mpc-db";

  private static volatile MPCDatabase sInstance;

  private final MutableLiveData<Boolean> mIsDatabaseCreated = new MutableLiveData<>();

  abstract public MPCWalletDao getMPCWalletDao();


  public static MPCDatabase getInstance(final Context context) {
    if (sInstance == null) {
      synchronized (MPCDatabase.class) {
        if (sInstance == null) {
          sInstance = buildDatabase(context.getApplicationContext());
          sInstance.updateDatabaseCreated(context.getApplicationContext());
        }
      }
    }
    return sInstance;
  }

  private static MPCDatabase buildDatabase(final Context appContext) {
    return Room.databaseBuilder(appContext, MPCDatabase.class, DATABASE_NAME)
        .addCallback(new Callback() {
          @Override
          public void onCreate(@NonNull SupportSQLiteDatabase db) {
            super.onCreate(db);
            AppExecutors.getInstance().diskIO().execute(() -> {
              MPCDatabase database = MPCDatabase.getInstance(appContext);
              database.setDatabaseCreated();
            });
          }
        })
        .fallbackToDestructiveMigration()
        .build();
  }


  private void updateDatabaseCreated(final Context context) {
    if (context.getDatabasePath(DATABASE_NAME).exists()) {
      setDatabaseCreated();
    }
  }


  private void setDatabaseCreated() {
    mIsDatabaseCreated.postValue(true);
  }

}
