package com.example.maquirentapp.Network;

import android.net.Uri;

import com.example.maquirentapp.Model.AlquilerMensual;
import com.example.maquirentapp.Model.GrupoElectrogeno;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;

public class FirebaseServicio {
    private final FirebaseFirestore db;
    private final FirebaseStorage storage;
    public FirebaseServicio() {
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
    }

    // Subir imagen y crear grupo
    public void crearGrupoConImagen(String codigo, Uri imageUri, OnGrupoCreatedListener listener) {
        if (imageUri == null) {
            // Crear grupo sin imagen
            crearGrupo(codigo, null, listener);
            return;
        }

        // Subir imagen primero
        String fileName = "grupos/" + codigo + "_" + System.currentTimeMillis() + ".jpg";
        StorageReference storageRef = storage.getReference().child(fileName);

        storageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    // Obtener URL de descarga
                    storageRef.getDownloadUrl()
                            .addOnSuccessListener(downloadUri -> {
                                // Crear grupo con URL de imagen
                                crearGrupo(codigo, downloadUri.toString(), listener);
                            })
                            .addOnFailureListener(listener::onError);
                })
                .addOnFailureListener(listener::onError);
    }

    private void crearGrupo(String codigo, String fotoUrl, OnGrupoCreatedListener listener) {
        GrupoElectrogeno grupo = new GrupoElectrogeno();
        grupo.setCodigo(codigo);
        grupo.setFoto(fotoUrl);

        db.collection("gruposElectrogenos")
                .add(grupo)
                .addOnSuccessListener(documentReference -> {
                    grupo.setId(documentReference.getId());
                    listener.onSuccess(grupo);
                })
                .addOnFailureListener(listener::onError);
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
                            grupo.setId(document.getId());
                            grupos.add(grupo);
                        }
                        listener.onSuccess(grupos);
                    } else {
                        listener.onError(task.getException());
                    }
                });
    }

    // Interfaces para callbacks
    public interface OnGrupoCreatedListener {
        void onSuccess(GrupoElectrogeno grupo);
        void onError(Exception e);
    }
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