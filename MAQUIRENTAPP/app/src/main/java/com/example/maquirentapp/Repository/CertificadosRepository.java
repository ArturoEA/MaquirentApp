package com.example.maquirentapp.Repository;

import com.example.maquirentapp.Model.CertificadoOperatividad;
import com.example.maquirentapp.Model.GrupoElectrogeno;
import com.example.maquirentapp.Model.InfoPlaca;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.Transaction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CertificadosRepository {

    private final FirebaseFirestore db;
    private static final String COL_CERTIFICADOS = "certificadosOperatividad";
    private static final String COL_CONFIG = "configuracion";
    private static final String DOC_SECUENCIAS = "secuencias";
    private static final String FIELD_ULTIMO_CERT = "ultimo_certificado";
    private static final String COL_INFO_PLACA = "infoPlacaGrupo";
    private static final String COL_GRUPOS = "gruposElectrogenos";

    public CertificadosRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    public interface Callback<T> {
        void onSuccess(T result);
        void onError(Exception e);
    }
    public void getGruposActivos(Callback<List<GrupoElectrogeno>> callback) {
        db.collection(COL_GRUPOS)
                .whereEqualTo("eliminado", false)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<GrupoElectrogeno> lista = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        GrupoElectrogeno g = doc.toObject(GrupoElectrogeno.class);
                        g.setId(doc.getId());
                        lista.add(g);
                    }
                    callback.onSuccess(lista);
                })
                .addOnFailureListener(callback::onError);
    }
    public void getDatosTecnicos(String idGrupo, Callback<InfoPlaca> callback) {
        db.collection(COL_INFO_PLACA)
                .whereEqualTo("idGrupo", idGrupo)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        InfoPlaca info = querySnapshot.getDocuments().get(0).toObject(InfoPlaca.class);
                        callback.onSuccess(info);
                    } else {
                        callback.onSuccess(null);
                    }
                })
                .addOnFailureListener(callback::onError);
    }

    public void crearCertificado(CertificadoOperatividad certificado, Callback<String> callback) {
        final DocumentReference secuenciaRef = db.collection(COL_CONFIG).document(DOC_SECUENCIAS);
        final DocumentReference nuevoCertRef = db.collection(COL_CERTIFICADOS).document();

        db.runTransaction((Transaction.Function<Void>) transaction -> {
                    DocumentSnapshot snapshot = transaction.get(secuenciaRef);
                    long nuevoNumero = 1;

                    if (snapshot.exists()) {
                        Double ultimo = snapshot.getDouble(FIELD_ULTIMO_CERT);
                        if (ultimo != null) {
                            nuevoNumero = ultimo.longValue() + 1;
                        }
                    }

                    String codigoGenerado = String.format("CERT-%06d", nuevoNumero);
                    certificado.setNumeroCertificado(codigoGenerado);
                    certificado.setId(nuevoCertRef.getId());

                    Map<String, Object> datosContador = new HashMap<>();
                    datosContador.put(FIELD_ULTIMO_CERT, nuevoNumero);

                    transaction.set(nuevoCertRef, certificado);
                    transaction.set(secuenciaRef, datosContador);

                    return null;
                }).addOnSuccessListener(aVoid -> callback.onSuccess(certificado.getNumeroCertificado()))
                .addOnFailureListener(callback::onError);
    }
}