package com.example.maquirentapp.Repository;

import android.util.Log;

import com.example.maquirentapp.Model.Cotizacion;
import com.example.maquirentapp.Model.GrupoElectrogeno;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.Transaction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CotizacionesRepository {

    private final FirebaseFirestore db;
    private static final String COL_COTIZACIONES = "cotizaciones";
    private static final String COL_CONFIG = "configuracion";
    private static final String DOC_SECUENCIAS = "secuencias";
    private static final String FIELD_ULTIMA_COT = "ultima_cotizacion";
    private static final String COL_GRUPOS = "gruposElectrogenos";

    public CotizacionesRepository() {
        this.db = FirebaseFirestore.getInstance();
    }
    public interface Callback<T> {
        void onSuccess(T result);
        void onError(Exception e);
    }
    public void crearCotizacion(Cotizacion cotizacion, Callback<String> callback) {
        final DocumentReference secuenciaRef = db.collection(COL_CONFIG).document(DOC_SECUENCIAS);
        final DocumentReference nuevaCotizacionRef = db.collection(COL_COTIZACIONES).document();

        db.runTransaction((Transaction.Function<Void>) transaction -> {
                    DocumentSnapshot snapshot = transaction.get(secuenciaRef);

                    long nuevoNumero = 1;

                    if (snapshot.exists()) {
                        Double ultimo = snapshot.getDouble(FIELD_ULTIMA_COT);
                        if (ultimo != null) {
                            nuevoNumero = ultimo.longValue() + 1;
                        }
                    }

                    String codigoGenerado = String.format("COT-%06d", nuevoNumero);
                    cotizacion.setNumeroCotizacion(codigoGenerado);
                    cotizacion.setId(nuevaCotizacionRef.getId());

                    Map<String, Object> datosContador = new HashMap<>();
                    datosContador.put(FIELD_ULTIMA_COT, nuevoNumero);

                    transaction.set(nuevaCotizacionRef, cotizacion);
                    transaction.set(secuenciaRef, datosContador);

                    return null;
                }).addOnSuccessListener(aVoid -> callback.onSuccess(cotizacion.getNumeroCotizacion()))
                .addOnFailureListener(callback::onError);
    }
    public void getGruposParaSeleccion(Callback<List<GrupoElectrogeno>> callback) {
        db.collection(COL_GRUPOS)
                .whereEqualTo("eliminado", false)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<GrupoElectrogeno> grupos = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        GrupoElectrogeno g = doc.toObject(GrupoElectrogeno.class);
                        g.setId(doc.getId());
                        grupos.add(g);
                    }
                    callback.onSuccess(grupos);
                })
                .addOnFailureListener(callback::onError);
    }
}