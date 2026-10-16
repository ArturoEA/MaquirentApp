package com.example.maquirentapp.Repository;

import android.net.Uri;

import com.example.maquirentapp.Model.Mantenimiento;
import com.example.maquirentapp.Model.MantenimientoConfiguracion;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Repositorio centralizado para la gestión de Mantenimientos Preventivos/Correctivos
 * y Plantillas de Configuración de Mantenimientos.
 */
public class MantenimientoRepository {

    private final FirebaseFirestore db;
    private final FirebaseStorage storage;

    private static final String COL_MANTENIMIENTOS = "mantenimientos";
    private static final String COL_CONFIG = "mantenimientos_configuracion";

    public MantenimientoRepository() {
        this.db = FirebaseFirestore.getInstance();
        this.storage = FirebaseStorage.getInstance();
    }

    public void getMantenimientosPorGrupo(String idGrupo, Callback<List<Mantenimiento>> callback) {
        db.collection(COL_MANTENIMIENTOS)
                .whereEqualTo("idGrupo", idGrupo)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<Mantenimiento> lista = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        Mantenimiento m = doc.toObject(Mantenimiento.class);
                        m.setId(doc.getId());
                        lista.add(m);
                    }
                    callback.onSuccess(lista);
                })
                .addOnFailureListener(callback::onError);
    }

    public void getMantenimientoPorId(String id, Callback<Mantenimiento> callback) {
        db.collection(COL_MANTENIMIENTOS).document(id)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Mantenimiento m = doc.toObject(Mantenimiento.class);
                        if (m != null) m.setId(doc.getId());
                        callback.onSuccess(m);
                    } else {
                        callback.onError(new Exception("Mantenimiento no encontrado"));
                    }
                })
                .addOnFailureListener(callback::onError);
    }

    public void guardarMantenimiento(Mantenimiento mant, Callback<String> callback) {
        DocumentReference docRef = db.collection(COL_MANTENIMIENTOS).document();
        mant.setId(docRef.getId());
        docRef.set(mant)
                .addOnSuccessListener(v -> callback.onSuccess(docRef.getId()))
                .addOnFailureListener(callback::onError);
    }

    public void actualizarMantenimiento(String id, Mantenimiento mant, Callback<Void> callback) {
        db.collection(COL_MANTENIMIENTOS).document(id)
                .set(mant)
                .addOnSuccessListener(v -> callback.onSuccess(null))
                .addOnFailureListener(callback::onError);
    }

    public void eliminarMantenimiento(String id, Callback<Void> callback) {
        db.collection(COL_MANTENIMIENTOS).document(id)
                .delete()
                .addOnSuccessListener(v -> callback.onSuccess(null))
                .addOnFailureListener(callback::onError);
    }

    public void actualizarDatosInforme(String id, Map<String, Object> datos, Callback<Void> callback) {
        db.collection(COL_MANTENIMIENTOS).document(id)
                .update("datosInforme", datos)
                .addOnSuccessListener(v -> callback.onSuccess(null))
                .addOnFailureListener(callback::onError);
    }

    // ==========================================
    // CONFIGURACIÓN DE MANTENIMIENTOS
    // ==========================================

    public void getConfiguraciones(Callback<List<MantenimientoConfiguracion>> callback) {
        db.collection(COL_CONFIG)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<MantenimientoConfiguracion> lista = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        MantenimientoConfiguracion c = doc.toObject(MantenimientoConfiguracion.class);
                        c.setId(doc.getId());
                        lista.add(c);
                    }
                    callback.onSuccess(lista);
                })
                .addOnFailureListener(callback::onError);
    }

    public void crearConfiguracion(MantenimientoConfiguracion config, Callback<Void> callback) {
        db.collection(COL_CONFIG)
                .add(config)
                .addOnSuccessListener(doc -> callback.onSuccess(null))
                .addOnFailureListener(callback::onError);
    }

    public void actualizarConfiguracion(String id, MantenimientoConfiguracion config, Callback<Void> callback) {
        db.collection(COL_CONFIG).document(id)
                .set(config)
                .addOnSuccessListener(v -> callback.onSuccess(null))
                .addOnFailureListener(callback::onError);
    }

    public void eliminarConfiguracion(String id, Callback<Void> callback) {
        db.collection(COL_CONFIG).document(id)
                .delete()
                .addOnSuccessListener(v -> callback.onSuccess(null))
                .addOnFailureListener(callback::onError);
    }
}
