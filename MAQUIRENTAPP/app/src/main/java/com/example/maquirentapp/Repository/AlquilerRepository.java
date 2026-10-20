package com.example.maquirentapp.Repository;

import com.example.maquirentapp.Model.Accesorio;
import com.example.maquirentapp.Model.AlquilerDia;
import com.example.maquirentapp.Model.AlquilerMensual;
import com.example.maquirentapp.Model.DetalleMes;
import com.example.maquirentapp.Model.Ingreso;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.functions.FirebaseFunctions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import android.net.Uri;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

/**
 * Repositorio centralizado para la gestión de Alquileres Mensuales, Alquileres Diarios,
 * Detalles de Facturación Mensual, Accesorios e Ingresos de Caja.
 */
public class AlquilerRepository {

    private final FirebaseFirestore db;
    private final FirebaseFunctions functions;
    private final FirebaseStorage storage;

    private static final String COL_MENSUALES = "alquileresMensuales";
    private static final String COL_DETALLES_MES = "detallesMes";
    private static final String COL_DIARIOS = "alquileresDiarios";
    private static final String COL_ACCESORIOS = "accesorios";
    private static final String COL_INGRESOS = "ingresosRegistrados";

    public AlquilerRepository() {
        this.db = FirebaseFirestore.getInstance();
        this.functions = FirebaseFunctions.getInstance();
        this.storage = FirebaseStorage.getInstance();
    }

    // ==========================================
    // 1. ALQUILERES MENSUALES
    // ==========================================

    public void getAlquileresMensuales(Callback<List<AlquilerMensual>> callback) {
        db.collection(COL_MENSUALES)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<AlquilerMensual> lista = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        AlquilerMensual alq = doc.toObject(AlquilerMensual.class);
                        alq.setId(doc.getId());
                        lista.add(alq);
                    }
                    lista.sort((a, b) -> {
                        if (a.getTimestampCreacion() > 0 && b.getTimestampCreacion() > 0) {
                            return Long.compare(b.getTimestampCreacion(), a.getTimestampCreacion());
                        }
                        if (a.getFechaInicial() != null && b.getFechaInicial() != null) {
                            return b.getFechaInicial().compareTo(a.getFechaInicial());
                        }
                        return 0;
                    });
                    callback.onSuccess(lista);
                })
                .addOnFailureListener(callback::onError);
    }

    public void getAlquileresMensualesActivos(Callback<List<AlquilerMensual>> callback) {
        db.collection(COL_MENSUALES)
                .whereEqualTo("finalizado", false)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<AlquilerMensual> lista = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        AlquilerMensual alq = doc.toObject(AlquilerMensual.class);
                        alq.setId(doc.getId());
                        lista.add(alq);
                    }
                    callback.onSuccess(lista);
                })
                .addOnFailureListener(callback::onError);
    }

    public void getAlquilerMensualPorId(String id, Callback<AlquilerMensual> callback) {
        db.collection(COL_MENSUALES).document(id)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        AlquilerMensual alq = doc.toObject(AlquilerMensual.class);
                        if (alq != null) alq.setId(doc.getId());
                        callback.onSuccess(alq);
                    } else {
                        callback.onError(new Exception("Alquiler mensual no encontrado"));
                    }
                })
                .addOnFailureListener(callback::onError);
    }

    public void crearAlquilerMensual(AlquilerMensual alquiler, Callback<AlquilerMensual> callback) {
        DocumentReference docRef = db.collection(COL_MENSUALES).document();
        alquiler.setId(docRef.getId());
        if (alquiler.getTimestampCreacion() == 0) {
            alquiler.setTimestampCreacion(System.currentTimeMillis());
        }

        docRef.set(alquiler)
                .addOnSuccessListener(v -> callback.onSuccess(alquiler))
                .addOnFailureListener(callback::onError);
    }

    public void crearAlquilerMensualConPrimerMes(AlquilerMensual alquiler, DetalleMes primerMes, Callback<AlquilerMensual> callback) {
        WriteBatch batch = db.batch();
        DocumentReference docRef = db.collection(COL_MENSUALES).document();
        alquiler.setId(docRef.getId());
        if (alquiler.getTimestampCreacion() == 0) {
            alquiler.setTimestampCreacion(System.currentTimeMillis());
        }
        batch.set(docRef, alquiler);

        if (primerMes != null) {
            DocumentReference detalleRef = db.collection(COL_DETALLES_MES).document();
            primerMes.setId(detalleRef.getId());
            primerMes.setIdAlquilerMensual(alquiler.getId());
            batch.set(detalleRef, primerMes);
        }

        batch.commit()
                .addOnSuccessListener(v -> callback.onSuccess(alquiler))
                .addOnFailureListener(callback::onError);
    }

    public void actualizarAlquilerMensual(AlquilerMensual alquiler, Callback<Void> callback) {
        db.collection(COL_MENSUALES).document(alquiler.getId())
                .set(alquiler)
                .addOnSuccessListener(v -> callback.onSuccess(null))
                .addOnFailureListener(callback::onError);
    }

    public void eliminarAlquilerMensual(String idAlquiler, Callback<Void> callback) {
        db.collection(COL_MENSUALES).document(idAlquiler)
                .delete()
                .addOnSuccessListener(v -> callback.onSuccess(null))
                .addOnFailureListener(callback::onError);
    }

    public void solicitarCodigoFinalizacion(String alquilerId, String nombreCliente, Callback<Void> callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("alquilerId", alquilerId);
        data.put("nombreCliente", nombreCliente);

        functions.getHttpsCallable("enviarCodigoFinalizarAlquiler")
                .call(data)
                .addOnSuccessListener(result -> callback.onSuccess(null))
                .addOnFailureListener(callback::onError);
    }

    public void confirmarFinalizacion(String alquilerId, String codigo, double horometroFinal, String fechaFinal, Callback<Void> callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("alquilerId", alquilerId);
        data.put("codigoIngresado", codigo);
        data.put("horometroFinal", horometroFinal);
        data.put("fechaFinal", fechaFinal);

        functions.getHttpsCallable("confirmarFinalizacionAlquiler")
                .call(data)
                .addOnSuccessListener(result -> callback.onSuccess(null))
                .addOnFailureListener(callback::onError);
    }

    // ==========================================
    // 2. DETALLES DE MES (FACTURACIÓN MENSUAL)
    // ==========================================

    public void getDetallesMesPorAlquiler(String idAlquilerMensual, Callback<List<DetalleMes>> callback) {
        db.collection(COL_DETALLES_MES)
                .whereEqualTo("idAlquilerMensual", idAlquilerMensual)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<DetalleMes> detalles = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        DetalleMes d = doc.toObject(DetalleMes.class);
                        d.setId(doc.getId());
                        detalles.add(d);
                    }
                    detalles.sort((d1, d2) -> Integer.compare(d1.getNumeroMes(), d2.getNumeroMes()));
                    callback.onSuccess(detalles);
                })
                .addOnFailureListener(callback::onError);
    }

    public void getDetalleMesPorId(String id, Callback<DetalleMes> callback) {
        db.collection(COL_DETALLES_MES).document(id)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        DetalleMes d = doc.toObject(DetalleMes.class);
                        if (d != null) d.setId(doc.getId());
                        callback.onSuccess(d);
                    } else {
                        callback.onError(new Exception("Detalle de mes no encontrado"));
                    }
                })
                .addOnFailureListener(callback::onError);
    }

    public void crearDetalleMes(DetalleMes detalle, Callback<DetalleMes> callback) {
        db.collection(COL_DETALLES_MES)
                .add(detalle)
                .addOnSuccessListener(ref -> {
                    detalle.setId(ref.getId());
                    callback.onSuccess(detalle);
                })
                .addOnFailureListener(callback::onError);
    }

    public void actualizarDetalleMes(DetalleMes detalle, Callback<Void> callback) {
        db.collection(COL_DETALLES_MES).document(detalle.getId())
                .set(detalle)
                .addOnSuccessListener(v -> callback.onSuccess(null))
                .addOnFailureListener(callback::onError);
    }

    public void eliminarDetalleMes(String id, Callback<Void> callback) {
        db.collection(COL_DETALLES_MES).document(id)
                .delete()
                .addOnSuccessListener(v -> callback.onSuccess(null))
                .addOnFailureListener(callback::onError);
    }

    public void getUltimoDetalleMes(String idAlquilerMensual, Callback<DetalleMes> callback) {
        db.collection(COL_DETALLES_MES)
                .whereEqualTo("idAlquilerMensual", idAlquilerMensual)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.isEmpty()) {
                        DetalleMes ultimo = null;
                        for (QueryDocumentSnapshot doc : snapshot) {
                            DetalleMes d = doc.toObject(DetalleMes.class);
                            d.setId(doc.getId());
                            if (ultimo == null || d.getNumeroMes() > ultimo.getNumeroMes()) {
                                ultimo = d;
                            }
                        }
                        if (ultimo != null) {
                            callback.onSuccess(ultimo);
                        } else {
                            callback.onError(new Exception("No hay meses registrados"));
                        }
                    } else {
                        callback.onError(new Exception("No hay meses registrados"));
                    }
                })
                .addOnFailureListener(callback::onError);
    }

    public void getAlquileresMensualesPorGrupoYMes(String idGrupo, int mes, int anio, Callback<List<AlquilerMensual>> callback) {
        db.collection(COL_MENSUALES)
                .whereEqualTo("idGrupo", idGrupo)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<AlquilerMensual> lista = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        AlquilerMensual alq = doc.toObject(AlquilerMensual.class);
                        alq.setId(doc.getId());
                        lista.add(alq);
                    }
                    callback.onSuccess(lista);
                })
                .addOnFailureListener(callback::onError);
    }

    // ==========================================
    // 3. ALQUILERES DIARIOS
    // ==========================================

    public void getAlquileresDiarios(Callback<List<AlquilerDia>> callback) {
        db.collection(COL_DIARIOS)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<AlquilerDia> lista = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        AlquilerDia alq = doc.toObject(AlquilerDia.class);
                        alq.setId(doc.getId());
                        lista.add(alq);
                    }
                    lista.sort((a, b) -> {
                        if (a.getTimestampCreacion() > 0 && b.getTimestampCreacion() > 0) {
                            return Long.compare(b.getTimestampCreacion(), a.getTimestampCreacion());
                        }
                        if (a.getFechaInicial() != null && b.getFechaInicial() != null) {
                            return b.getFechaInicial().compareTo(a.getFechaInicial());
                        }
                        return 0;
                    });
                    callback.onSuccess(lista);
                })
                .addOnFailureListener(callback::onError);
    }

    public void getAlquileresDiariosActivos(Callback<List<AlquilerDia>> callback) {
        db.collection(COL_DIARIOS)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<AlquilerDia> lista = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        AlquilerDia alq = doc.toObject(AlquilerDia.class);
                        alq.setId(doc.getId());
                        if (!alq.isFinalizado()) {
                            lista.add(alq);
                        }
                    }
                    lista.sort((a, b) -> {
                        if (a.getTimestampCreacion() > 0 && b.getTimestampCreacion() > 0) {
                            return Long.compare(b.getTimestampCreacion(), a.getTimestampCreacion());
                        }
                        if (a.getFechaInicial() != null && b.getFechaInicial() != null) {
                            return b.getFechaInicial().compareTo(a.getFechaInicial());
                        }
                        return 0;
                    });
                    callback.onSuccess(lista);
                })
                .addOnFailureListener(callback::onError);
    }

    public void getAlquilerDiaPorId(String id, Callback<AlquilerDia> callback) {
        db.collection(COL_DIARIOS).document(id)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        AlquilerDia alq = doc.toObject(AlquilerDia.class);
                        if (alq != null) alq.setId(doc.getId());
                        callback.onSuccess(alq);
                    } else {
                        callback.onError(new Exception("Alquiler diario no encontrado"));
                    }
                })
                .addOnFailureListener(callback::onError);
    }

    public void crearAlquilerDia(AlquilerDia alquiler, Callback<AlquilerDia> callback) {
        DocumentReference docRef = db.collection(COL_DIARIOS).document();
        alquiler.setId(docRef.getId());
        if (alquiler.getTimestampCreacion() == 0) {
            alquiler.setTimestampCreacion(System.currentTimeMillis());
        }

        docRef.set(alquiler)
                .addOnSuccessListener(v -> callback.onSuccess(alquiler))
                .addOnFailureListener(callback::onError);
    }

    public void actualizarAlquilerDia(AlquilerDia alquiler, Callback<Void> callback) {
        db.collection(COL_DIARIOS).document(alquiler.getId())
                .set(alquiler)
                .addOnSuccessListener(v -> callback.onSuccess(null))
                .addOnFailureListener(callback::onError);
    }

    public void eliminarAlquilerDia(String idAlquiler, Callback<Void> callback) {
        db.collection(COL_DIARIOS).document(idAlquiler)
                .delete()
                .addOnSuccessListener(v -> callback.onSuccess(null))
                .addOnFailureListener(callback::onError);
    }

    public void finalizarAlquilerDiario(AlquilerDia alquiler, Callback<Void> callback) {
        if (alquiler == null || alquiler.getPrecioTotal() == 0 ||
                alquiler.getFechaInicial() == null || alquiler.getFechaFinal() == null ||
                alquiler.getFechaInicial().isEmpty() || alquiler.getFechaFinal().isEmpty()) {
            callback.onError(new Exception("Datos incompletos para prorrateo (Fechas, Precio)"));
            return;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        try {
            Date fechaInicio = sdf.parse(alquiler.getFechaInicial());
            Date fechaFin = sdf.parse(alquiler.getFechaFinal());
            if (fechaInicio == null || fechaFin == null || fechaInicio.after(fechaFin)) {
                callback.onError(new Exception("Fechas inválidas"));
                return;
            }

            long diffMillis = fechaFin.getTime() - fechaInicio.getTime();
            long diasTotales = TimeUnit.MILLISECONDS.toDays(diffMillis) + 1;
            if (diasTotales <= 0) {
                callback.onError(new Exception("El período debe ser de al menos 1 día"));
                return;
            }

            double precioPorDia = alquiler.getPrecioTotal() / diasTotales;
            WriteBatch batch = db.batch();
            Calendar cal = Calendar.getInstance();
            cal.setTime(fechaInicio);

            while (!cal.getTime().after(fechaFin)) {
                int mesActual = cal.get(Calendar.MONTH) + 1;
                int anioActual = cal.get(Calendar.YEAR);

                int diasEnEsteMes = 0;
                while (cal.get(Calendar.MONTH) + 1 == mesActual && !cal.getTime().after(fechaFin)) {
                    diasEnEsteMes++;
                    cal.add(Calendar.DAY_OF_YEAR, 1);
                }

                if (diasEnEsteMes > 0) {
                    double montoProrrateado = precioPorDia * diasEnEsteMes;
                    Ingreso ingreso = new Ingreso(
                            montoProrrateado,
                            alquiler.getMoneda(),
                            "Alquiler Diario",
                            alquiler.getIdGrupo(),
                            alquiler.getId(),
                            alquiler.getNombreCliente(),
                            mesActual,
                            anioActual
                    );
                    DocumentReference ingresoRef = db.collection(COL_INGRESOS).document();
                    batch.set(ingresoRef, ingreso);
                }
            }

            DocumentReference alquilerRef = db.collection(COL_DIARIOS).document(alquiler.getId());
            batch.update(alquilerRef, "finalizado", true);

            batch.commit()
                    .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                    .addOnFailureListener(callback::onError);

        } catch (Exception e) {
            callback.onError(e);
        }
    }

    public void getAlquileresDiariosPorGrupo(String idGrupo, int mes, int anio, Callback<List<AlquilerDia>> callback) {
        db.collection(COL_DIARIOS)
                .whereEqualTo("idGrupo", idGrupo)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<AlquilerDia> lista = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        AlquilerDia alq = doc.toObject(AlquilerDia.class);
                        alq.setId(doc.getId());
                        lista.add(alq);
                    }
                    callback.onSuccess(lista);
                })
                .addOnFailureListener(callback::onError);
    }

    // ==========================================
    // 4. ACCESORIOS
    // ==========================================

    public void getAccesorios(String tipo, Callback<List<Accesorio>> callback) {
        db.collection(COL_ACCESORIOS)
                .whereEqualTo("tipo", tipo)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<Accesorio> lista = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        Accesorio acc = doc.toObject(Accesorio.class);
                        acc.setId(doc.getId());
                        lista.add(acc);
                    }
                    callback.onSuccess(lista);
                })
                .addOnFailureListener(callback::onError);
    }

    public void getAccesorioPorId(String id, Callback<Accesorio> callback) {
        db.collection(COL_ACCESORIOS).document(id)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Accesorio acc = doc.toObject(Accesorio.class);
                        if (acc != null) acc.setId(doc.getId());
                        callback.onSuccess(acc);
                    } else {
                        callback.onError(new Exception("Accesorio no encontrado"));
                    }
                })
                .addOnFailureListener(callback::onError);
    }

    public void crearAccesorio(Accesorio accesorio, Callback<Void> callback) {
        db.collection(COL_ACCESORIOS)
                .add(accesorio)
                .addOnSuccessListener(doc -> callback.onSuccess(null))
                .addOnFailureListener(callback::onError);
    }

    public void actualizarAccesorio(String id, Map<String, Object> updates, Callback<Void> callback) {
        db.collection(COL_ACCESORIOS).document(id)
                .update(updates)
                .addOnSuccessListener(v -> callback.onSuccess(null))
                .addOnFailureListener(callback::onError);
    }

    public void eliminarAccesorio(String id, Callback<Void> callback) {
        db.collection(COL_ACCESORIOS).document(id).get()
                .addOnSuccessListener(doc -> {
                    String fotoUrl = doc != null ? doc.getString("foto") : null;
                    db.collection(COL_ACCESORIOS).document(id)
                            .delete()
                            .addOnSuccessListener(v -> {
                                if (fotoUrl != null && !fotoUrl.isEmpty() && fotoUrl.contains("firebasestorage")) {
                                    try {
                                        storage.getReferenceFromUrl(fotoUrl).delete();
                                    } catch (Exception ignored) {}
                                }
                                callback.onSuccess(null);
                            })
                            .addOnFailureListener(callback::onError);
                })
                .addOnFailureListener(callback::onError);
    }

    public void subirFotoAccesorio(Uri uri, Callback<String> callback) {
        String fileName = "accesorios/" + System.currentTimeMillis() + ".jpg";
        StorageReference ref = storage.getReference().child(fileName);
        ref.putFile(uri)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) throw task.getException();
                    return ref.getDownloadUrl();
                })
                .addOnSuccessListener(downloadUri -> callback.onSuccess(downloadUri.toString()))
                .addOnFailureListener(callback::onError);
    }

    // ==========================================
    // 5. INGRESOS
    // ==========================================

    public void registrarIngreso(Ingreso ingreso, Callback<String> callback) {
        db.collection(COL_INGRESOS)
                .add(ingreso)
                .addOnSuccessListener(doc -> callback.onSuccess(doc.getId()))
                .addOnFailureListener(callback::onError);
    }

    public void getIngresosPorGrupoYMes(String idGrupo, int mes, int anio, Callback<List<Ingreso>> callback) {
        db.collection(COL_INGRESOS)
                .whereEqualTo("idGrupo", idGrupo)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<Ingreso> lista = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        Ingreso ing = doc.toObject(Ingreso.class);
                        ing.setId(doc.getId());
                        lista.add(ing);
                    }
                    callback.onSuccess(lista);
                })
                .addOnFailureListener(callback::onError);
    }
}
