package com.example.maquirentapp.Network;

import android.net.Uri;

import com.example.maquirentapp.Model.AlquilerMensual;
import com.example.maquirentapp.Model.GrupoElectrogeno;
import com.example.maquirentapp.Model.Usuario;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FirebaseServicio {
    private final FirebaseFirestore db;
    private final FirebaseStorage storage;
    private final FirebaseAuth auth;
    public FirebaseServicio() {
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        auth = FirebaseAuth.getInstance();
    }
    public void registrarUsuario(String email, String password, String nombre, OnAuthListener listener) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null) {
                            crearPerfilUsuario(user.getUid(), nombre, email, listener);
                        }
                    } else {
                        listener.onError(task.getException());
                    }
                });
    }
    private void crearPerfilUsuario(String uid, String nombre, String email, OnAuthListener listener) {
        Usuario usuario = new Usuario(uid, nombre, email);

        db.collection("usuarios")
                .document(uid)
                .set(usuario)
                .addOnSuccessListener(aVoid -> {
                    listener.onRegistroExitoso(usuario);
                })
                .addOnFailureListener(listener::onError);
    }
    public void verificarEstadoUsuario(OnAuthListener listener) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            listener.onError(new Exception("Usuario no autenticado"));
            return;
        }

        db.collection("usuarios")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        Usuario usuario = document.toObject(Usuario.class);
                        if (usuario != null) {
                            switch (usuario.getEstado()) {
                                case "activo":
                                    listener.onLoginExitoso(usuario);
                                    break;
                                case "pendiente":
                                    listener.onUsuarioPendiente();
                                    break;
                                case "inactivo":
                                    listener.onUsuarioInactivo();
                                    break;
                            }
                        }
                    } else {
                        listener.onError(new Exception("Perfil de usuario no encontrado"));
                    }
                })
                .addOnFailureListener(listener::onError);
    }
    public void getUsuariosPendientes(OnUsuariosListener listener) {
        db.collection("usuarios")
                .whereEqualTo("estado", "pendiente")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Usuario> usuarios = querySnapshot.toObjects(Usuario.class);
                    listener.onSuccess(usuarios);
                })
                .addOnFailureListener(listener::onError);
    }
    public void aprobarUsuario(String uid, OnUsuarioUpdatedListener listener) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) return;

        db.collection("usuarios")
                .document(uid)
                .update(
                        "estado", "activo",
                        "creadoPor", currentUser.getUid()
                )
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(listener::onError);
    }
    public void desactivarUsuario(String uid, OnUsuarioUpdatedListener listener) {
        db.collection("usuarios")
                .document(uid)
                .update("estado", "inactivo")
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(listener::onError);
    }
    public void iniciarSesion(String email, String password, OnAuthListener listener) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        verificarEstadoUsuario(listener);
                    } else {
                        listener.onError(task.getException());
                    }
                });
    }
    public void cerrarSesion() {
        auth.signOut();
    }
    public void crearGrupoConImagen(String codigo, Uri imagenUri, OnGrupoCreatedListener listener) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference docRef = db.collection("gruposElectrogenos").document();
        String idGenerado = docRef.getId();

        GrupoElectrogeno grupo = new GrupoElectrogeno();
        grupo.setId(idGenerado);
        grupo.setCodigo(codigo);
        grupo.setEliminado(false);

        if (imagenUri != null) {
            StorageReference storageRef = FirebaseStorage.getInstance()
                    .getReference()
                    .child("grupos/" + idGenerado + ".jpg");

            storageRef.putFile(imagenUri)
                    .addOnSuccessListener(taskSnapshot -> storageRef.getDownloadUrl()
                            .addOnSuccessListener(downloadUri -> {
                                grupo.setFoto(downloadUri.toString());

                                docRef.set(grupo)
                                        .addOnSuccessListener(aVoid -> listener.onSuccess(grupo))
                                        .addOnFailureListener(listener::onError);
                            })
                            .addOnFailureListener(listener::onError))
                    .addOnFailureListener(listener::onError);
        } else {
            docRef.set(grupo)
                    .addOnSuccessListener(aVoid -> listener.onSuccess(grupo))
                    .addOnFailureListener(listener::onError);
        }
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
    // Interface para callbacks simples
    public interface OnSimpleCallback {
        void onSuccess();
        void onError(Exception e);
    }

    // Método para actualizar código del grupo
    public void actualizarCodigoGrupo(String grupoId, String nuevoCodigo, OnSimpleCallback callback) {
        db.collection("gruposElectrogenos").document(grupoId)
                .update("codigo", nuevoCodigo)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onError);
    }

    // Método para eliminación suave
    public void eliminarGrupoSuave(String grupoId, Map<String, Object> updates, OnSimpleCallback callback) {
        db.collection("gruposElectrogenos").document(grupoId)
                .update(updates)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onError);
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
                            alquiler.setId(document.getId());
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
        if (alquiler.getIdGrupo() == null || alquiler.getIdGrupo().isEmpty()) {
            listener.onError(new Exception("idGrupo no puede estar vacío"));
            return;
        }

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
    public interface OnAuthListener {
        void onLoginExitoso(Usuario usuario);
        void onRegistroExitoso(Usuario usuario);
        void onUsuarioPendiente();
        void onUsuarioInactivo();
        void onError(Exception e);
    }

    public interface OnUsuariosListener {
        void onSuccess(List<Usuario> usuarios);
        void onError(Exception e);
    }

    public interface OnUsuarioUpdatedListener {
        void onSuccess();
        void onError(Exception e);
    }
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