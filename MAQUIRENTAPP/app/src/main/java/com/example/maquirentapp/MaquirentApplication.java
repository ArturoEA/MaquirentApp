package com.example.maquirentapp;

import android.app.Application;

import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.cache.InternalCacheDiskCacheFactory;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.PersistentCacheSettings;

public class MaquirentApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // 1. Configuración de persistencia Firestore (100 MB para documentos estructurados)
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(PersistentCacheSettings.newBuilder()
                        .setSizeBytes(100 * 1024 * 1024L) // Límite de 100 MB para evitar degradación de rendimiento
                        .build())
                .build();

        FirebaseFirestore.getInstance().setFirestoreSettings(settings);

        // 2. Configuración global de Glide para Imágenes Offline por defecto
        GlideBuilder glideBuilder = new GlideBuilder();
        int diskCacheSizeBytes = 250 * 1024 * 1024; // 250 MB dedicados a imágenes offline (grupos, perfiles, etc.)
        glideBuilder.setDiskCache(new InternalCacheDiskCacheFactory(this, "image_cache_offline", diskCacheSizeBytes));
        glideBuilder.setDefaultRequestOptions(
                new RequestOptions()
                        .diskCacheStrategy(DiskCacheStrategy.DATA) // Almacena los bytes originales en disco para lectura offline
                        .timeout(15000)
        );
        Glide.init(this, glideBuilder);
    }
}
