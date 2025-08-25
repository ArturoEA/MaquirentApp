package com.example.maquirentapp.Network;

import com.example.maquirentapp.Model.AlquilerMensual;
import com.example.maquirentapp.Model.GrupoElectrogeno;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class FirebaseServicio {
    private final FirebaseFirestore db;

    public FirebaseServicio() {
        db = FirebaseFirestore.getInstance();
    }

    // Obtener alquileres mensuales
    public void getAlquileresMensuales(OnAlquileresLoadedListener listener) {
        db.collection("alquileresMensuales")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<AlquilerMensual> alquileres = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            AlquilerMensual alquiler = document.toObject(AlquilerMensual.class);
                            alquiler.setId(document.getId()); // Asignar el ID del documento
                            alquileres.add(alquiler);
                        }
                        listener.onSuccess(alquileres);
                    } else {
                        listener.onError(task.getException());
                    }
                });
    }

    // Crear alquiler mensual
    public void crearAlquilerMensual(AlquilerMensual alquiler, OnAlquilerCreatedListener listener) {
        db.collection("alquileresMensuales")
                .add(alquiler)
                .addOnSuccessListener(documentReference -> {
                    alquiler.setId(documentReference.getId());
                    listener.onSuccess(alquiler);
                })
                .addOnFailureListener(listener::onError);
    }

    // Obtener grupos electrógenos
    public void getGruposElectrogenos(OnGruposLoadedListener listener) {
        db.collection("gruposElectrogenos")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<GrupoElectrogeno> grupos = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            GrupoElectrogeno grupo = document.toObject(GrupoElectrogeno.class);
                            grupos.add(grupo);
                        }
                        listener.onSuccess(grupos);
                    } else {
                        listener.onError(task.getException());
                    }
                });
    }

    // Interfaces para callbacks
    public interface OnAlquileresLoadedListener {
        void onSuccess(List<AlquilerMensual> alquileres);
        void onError(Exception e);
    }

    public interface OnAlquilerCreatedListener {
        void onSuccess(AlquilerMensual alquiler);
        void onError(Exception e);
    }

    public interface OnGruposLoadedListener {
        void onSuccess(List<GrupoElectrogeno> grupos);
        void onError(Exception e);
    }
}