package com.example.maquirentapp.Repository;

import com.example.maquirentapp.Model.Usuario;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.functions.FirebaseFunctions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Repositorio centralizado para Autenticación, Gestión de Roles,
 * Estados de Aprobación de Usuarios y Tokens de Notificación Push.
 */
public class UsuarioRepository {

    private final FirebaseAuth auth;
    private final FirebaseFirestore db;
    private final FirebaseFunctions functions;

    private static final String COL_USUARIOS = "usuarios";

    public UsuarioRepository() {
        this.auth = FirebaseAuth.getInstance();
        this.db = FirebaseFirestore.getInstance();
        this.functions = FirebaseFunctions.getInstance();
    }

    public String getCurrentUserUid() {
        FirebaseUser user = auth.getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    public String getCurrentUserEmail() {
        FirebaseUser user = auth.getCurrentUser();
        return user != null ? user.getEmail() : null;
    }

    public void cerrarSesion() {
        auth.signOut();
    }

    public void iniciarSesion(String email, String password, Callback<Usuario> callback) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user = auth.getCurrentUser();
                    if (user != null) {
                        getUsuarioPorUid(user.getUid(), callback);
                    } else {
                        callback.onError(new Exception("Error al obtener usuario"));
                    }
                })
                .addOnFailureListener(callback::onError);
    }

    public void registrarUsuario(String email, String password, String nombre, Callback<Usuario> callback) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user = auth.getCurrentUser();
                    if (user != null) {
                        Usuario nuevo = new Usuario(user.getUid(), nombre, email);
                        nuevo.setRol("empleado");
                        nuevo.setEstado("pendiente");
                        db.collection(COL_USUARIOS).document(user.getUid())
                                .set(nuevo)
                                .addOnSuccessListener(v -> callback.onSuccess(nuevo))
                                .addOnFailureListener(callback::onError);
                    }
                })
                .addOnFailureListener(callback::onError);
    }

    public void getUsuarioPorUid(String uid, Callback<Usuario> callback) {
        db.collection(COL_USUARIOS).document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Usuario u = doc.toObject(Usuario.class);
                        callback.onSuccess(u);
                    } else {
                        callback.onError(new Exception("Usuario no encontrado en la base de datos"));
                    }
                })
                .addOnFailureListener(callback::onError);
    }

    public void verificarSiEsAdmin(Callback<Boolean> callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onSuccess(false);
            return;
        }

        db.collection(COL_USUARIOS).document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String rol = doc.getString("rol");
                        callback.onSuccess("admin".equalsIgnoreCase(rol));
                    } else {
                        callback.onSuccess(false);
                    }
                })
                .addOnFailureListener(e -> callback.onSuccess(false));
    }

    public void getUsuariosActivos(Callback<List<Usuario>> callback) {
        db.collection(COL_USUARIOS)
                .whereEqualTo("estado", "activo")
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<Usuario> lista = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        Usuario u = doc.toObject(Usuario.class);
                        lista.add(u);
                    }
                    callback.onSuccess(lista);
                })
                .addOnFailureListener(callback::onError);
    }

    public void getUsuariosPendientes(Callback<List<Usuario>> callback) {
        db.collection(COL_USUARIOS)
                .whereEqualTo("estado", "pendiente")
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<Usuario> lista = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        Usuario u = doc.toObject(Usuario.class);
                        lista.add(u);
                    }
                    callback.onSuccess(lista);
                })
                .addOnFailureListener(callback::onError);
    }

    public void aprobarUsuario(String uid, Callback<Void> callback) {
        db.collection(COL_USUARIOS).document(uid)
                .update("estado", "activo")
                .addOnSuccessListener(v -> callback.onSuccess(null))
                .addOnFailureListener(callback::onError);
    }

    public void desactivarUsuario(String uid, Callback<Void> callback) {
        db.collection(COL_USUARIOS).document(uid)
                .update("estado", "inactivo")
                .addOnSuccessListener(v -> callback.onSuccess(null))
                .addOnFailureListener(callback::onError);
    }

    public void actualizarEstadoUsuario(String uid, String nuevoEstado, Callback<Void> callback) {
        db.collection(COL_USUARIOS).document(uid)
                .update("estado", nuevoEstado)
                .addOnSuccessListener(v -> callback.onSuccess(null))
                .addOnFailureListener(callback::onError);
    }

    public void actualizarRolUsuario(String uid, String rol, Callback<Void> callback) {
        db.collection(COL_USUARIOS).document(uid)
                .update("rol", rol)
                .addOnSuccessListener(v -> callback.onSuccess(null))
                .addOnFailureListener(callback::onError);
    }

    public void solicitarCodigoEliminacion(String uid, String email, Callback<Void> callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("usuarioId", uid);
        data.put("usuarioEmail", email);

        functions.getHttpsCallable("enviarCodigoEliminacionUsuario")
                .call(data)
                .addOnSuccessListener(result -> callback.onSuccess(null))
                .addOnFailureListener(callback::onError);
    }

    public void confirmarEliminacion(String uid, String codigo, Callback<Void> callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("usuarioId", uid);
        data.put("codigoIngresado", codigo);

        functions.getHttpsCallable("confirmarEliminacionUsuario")
                .call(data)
                .addOnSuccessListener(result -> callback.onSuccess(null))
                .addOnFailureListener(callback::onError);
    }

    public void enviarCorreoRecuperacion(String email, Callback<Void> callback) {
        auth.sendPasswordResetEmail(email)
                .addOnSuccessListener(v -> callback.onSuccess(null))
                .addOnFailureListener(callback::onError);
    }

    public void getTodosLosUsuarios(Callback<List<Usuario>> callback) {
        db.collection(COL_USUARIOS)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<Usuario> lista = new ArrayList<>();
                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : snapshot) {
                        Usuario u = doc.toObject(Usuario.class);
                        if (u != null) {
                            u.setUid(doc.getId());
                            lista.add(u);
                        }
                    }
                    callback.onSuccess(lista);
                })
                .addOnFailureListener(callback::onError);
    }

    public void guardarFCMToken(String token) {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null && token != null) {
            db.collection(COL_USUARIOS).document(user.getUid())
                    .update("fcmTokens", FieldValue.arrayUnion(token));
        }
    }
}
