package com.example.maquirentapp.Repository;

import com.example.maquirentapp.Model.Tarea;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Repositorio centralizado para la gestión de Tareas operativas asignadas al personal.
 */
public class TareaRepository {

    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    private static final String COL_TAREAS = "tareas";

    public TareaRepository() {
        this.db = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
    }

    public void getTareas(Callback<List<Tarea>> callback) {
        db.collection(COL_TAREAS)
                .orderBy("creadaEn", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<Tarea> lista = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        Tarea t = doc.toObject(Tarea.class);
                        t.setId(doc.getId());
                        lista.add(t);
                    }
                    callback.onSuccess(lista);
                })
                .addOnFailureListener(callback::onError);
    }

    public void crearTarea(String titulo, Callback<Void> callback) {
        DocumentReference docRef = db.collection(COL_TAREAS).document();
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault());
        String fecha = sdf.format(new java.util.Date());

        Tarea tarea = new Tarea(docRef.getId(), titulo, fecha, false);
        docRef.set(tarea)
                .addOnSuccessListener(v -> callback.onSuccess(null))
                .addOnFailureListener(callback::onError);
    }

    public void completarTarea(String idTarea, List<String> participantesIds, Callback<Void> callback) {
        completarTareaConChecklist(idTarea, participantesIds, null, callback);
    }

    public void completarTareaConChecklist(String idTarea, List<String> participantesIds, List<Map<String, Object>> checklist, Callback<Void> callback) {
        FirebaseUser user = auth.getCurrentUser();
        String completadorUid = user != null ? user.getUid() : "sistema";

        Map<String, Object> updates = new HashMap<>();
        updates.put("completada", true);
        updates.put("completadaPor", completadorUid);
        updates.put("participantes", participantesIds);
        if (checklist != null) {
            updates.put("checklist", checklist);
        }
        updates.put("completadaEn", System.currentTimeMillis());

        db.collection(COL_TAREAS).document(idTarea)
                .update(updates)
                .addOnSuccessListener(v -> callback.onSuccess(null))
                .addOnFailureListener(callback::onError);
    }

    public void aprobarPorGerencia(String idTarea, String aprobadorNombre, Callback<Void> callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("aprobadoPorGerencia", true);
        updates.put("fechaAprobacion", System.currentTimeMillis());
        if (aprobadorNombre != null && !aprobadorNombre.isEmpty()) {
            updates.put("aprobadoPorNombre", aprobadorNombre);
        }

        db.collection(COL_TAREAS).document(idTarea)
                .update(updates)
                .addOnSuccessListener(v -> callback.onSuccess(null))
                .addOnFailureListener(callback::onError);
    }

    public void eliminarTarea(String idTarea, Callback<Void> callback) {
        db.collection(COL_TAREAS).document(idTarea)
                .delete()
                .addOnSuccessListener(v -> callback.onSuccess(null))
                .addOnFailureListener(callback::onError);
    }
}
