package com.example.maquirentapp.Repository;

import com.example.maquirentapp.Model.AlquilerMensual;
import com.example.maquirentapp.Model.ClienteValorizacion;
import com.example.maquirentapp.Model.DetalleMes;
import com.example.maquirentapp.Model.Valorizacion;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.Transaction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class ValorizacionesRepository {

    private final FirebaseFirestore db;
    private static final String COL_CLIENTES_VAL = "clientesValorizacion";
    private static final String COL_VALORIZACIONES = "valorizaciones";
    private static final String COL_CONFIG = "configuracion";
    private static final String DOC_SECUENCIAS = "secuencias";
    private static final String FIELD_ULTIMA_VAL = "ultima_valorizacion";

    public ValorizacionesRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    public interface Callback<T> {
        void onSuccess(T result);

        void onError(Exception e);
    }

    // 1. Obtener lista de clientes con alquileres activos
    public void getClientesConAlquileresActivos(Callback<List<String>> callback) {
        db.collection("alquileresMensuales")
                .whereEqualTo("finalizado", false)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<String> clientes = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        String cliente = doc.getString("nombreCliente");
                        if (cliente != null && !clientes.contains(cliente)) {
                            clientes.add(cliente);
                        }
                    }
                    callback.onSuccess(clientes);
                })
                .addOnFailureListener(callback::onError);
    }

    // 2. Obtener los equipos activos de un cliente específico
    public void getAlquileresPorCliente(String nombreCliente, Callback<List<AlquilerMensual>> callback) {
        db.collection("alquileresMensuales")
                .whereEqualTo("nombreCliente", nombreCliente)
                .whereEqualTo("finalizado", false)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<AlquilerMensual> alquileres = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        AlquilerMensual a = doc.toObject(AlquilerMensual.class);
                        a.setId(doc.getId());
                        alquileres.add(a);
                    }
                    callback.onSuccess(alquileres);
                })
                .addOnFailureListener(callback::onError);
    }

    // 3. Obtener el ÚLTIMO detalleMes de un alquiler
    public void getUltimosDetallesMes(String idAlquiler, Callback<List<DetalleMes>> callback) {
        db.collection("detallesMes")
                .whereEqualTo("idAlquilerMensual", idAlquiler)
                .orderBy("numeroMes", com.google.firebase.firestore.Query.Direction.DESCENDING) // Del más nuevo al más viejo
                .limit(2) // Pedimos los dos últimos
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<DetalleMes> lista = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        DetalleMes d = doc.toObject(DetalleMes.class);
                        d.setId(doc.getId());
                        lista.add(d);
                    }
                    callback.onSuccess(lista);
                })
                .addOnFailureListener(callback::onError);
    }
    public void crearClienteValorizacion(ClienteValorizacion cliente, Callback<Void> callback) {
        DocumentReference ref = db.collection(COL_CLIENTES_VAL).document();
        cliente.setId(ref.getId());
        ref.set(cliente)
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(callback::onError);
    }
    public void getClientesPorAnio(int anio, Callback<List<ClienteValorizacion>> callback) {
        db.collection(COL_CLIENTES_VAL)
                .whereEqualTo("anio", anio)
                .orderBy("nombreEmpresa", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<ClienteValorizacion> lista = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        lista.add(doc.toObject(ClienteValorizacion.class));
                    }
                    callback.onSuccess(lista);
                })
                .addOnFailureListener(callback::onError);
    }
    public void getValorizacionesPorCliente(String idClienteValorizacion, Callback<List<Valorizacion>> callback) {
        db.collection(COL_VALORIZACIONES)
                .whereEqualTo("idClienteValorizacion", idClienteValorizacion)
                .orderBy("timestampCreacion", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Valorizacion> lista = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Valorizacion v = doc.toObject(Valorizacion.class);
                        v.setId(doc.getId());
                        lista.add(v);
                    }
                    callback.onSuccess(lista);
                })
                .addOnFailureListener(callback::onError);
    }

    // Crear valorización con correlativo automático
    public void crearValorizacion(Valorizacion valorizacion, Callback<String> callback) {
        final DocumentReference secuenciaRef = db.collection(COL_CONFIG).document(DOC_SECUENCIAS);
        final DocumentReference nuevaValRef = db.collection(COL_VALORIZACIONES).document();

        db.runTransaction((Transaction.Function<Void>) transaction -> {
                    DocumentSnapshot snapshot = transaction.get(secuenciaRef);
                    long nuevoNumero = 1;

                    if (snapshot.exists()) {
                        Double ultimo = snapshot.getDouble(FIELD_ULTIMA_VAL);
                        if (ultimo != null) {
                            nuevoNumero = ultimo.longValue() + 1;
                        }
                    }

                    String codigoGenerado = String.format("VAL-%06d", nuevoNumero);
                    valorizacion.setNumeroValorizacion(codigoGenerado);
                    valorizacion.setId(nuevaValRef.getId());

                    Map<String, Object> datosContador = new HashMap<>();
                    datosContador.put(FIELD_ULTIMA_VAL, nuevoNumero);

                    transaction.set(nuevaValRef, valorizacion);
                    transaction.set(secuenciaRef, datosContador);

                    return null;
                }).addOnSuccessListener(aVoid -> callback.onSuccess(valorizacion.getNumeroValorizacion()))
                .addOnFailureListener(callback::onError);
    }

    public void eliminarValorizacion(String id, Callback<Void> callback) {
        db.collection(COL_VALORIZACIONES).document(id).delete()
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(callback::onError);
    }
}