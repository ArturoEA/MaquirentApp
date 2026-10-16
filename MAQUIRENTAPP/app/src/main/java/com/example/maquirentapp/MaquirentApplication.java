package com.example.maquirentapp;

import android.app.Application;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.PersistentCacheSettings;

public class MaquirentApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(PersistentCacheSettings.newBuilder()
                        .setSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                        .build())
                .build();

        FirebaseFirestore.getInstance().setFirestoreSettings(settings);
    }
}