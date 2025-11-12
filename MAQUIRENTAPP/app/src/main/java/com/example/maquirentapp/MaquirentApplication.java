package com.example.maquirentapp;

import android.app.Application;

import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.example.maquirentapp.workers.AlquilerMensualCheckWorker;

import java.util.concurrent.TimeUnit;

public class MaquirentApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        configurarAlquilerMensualWorker();
    }

    private void configurarAlquilerMensualWorker() {
        // Constraints: solo ejecutar si hay conexión a internet
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        // Crear tarea periódica que se ejecuta cada 24 horas
        PeriodicWorkRequest alquilerCheckWork =
                new PeriodicWorkRequest.Builder(
                        AlquilerMensualCheckWorker.class,
                        24,
                        TimeUnit.HOURS,
                        15,
                        TimeUnit.MINUTES
                )
                        .setConstraints(constraints)
                        .build();

        // Encolar el trabajo
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "AlquilerMensualCheck",
                ExistingPeriodicWorkPolicy.KEEP, // Mantener el trabajo existente si ya está programado
                alquilerCheckWork
        );
    }
}