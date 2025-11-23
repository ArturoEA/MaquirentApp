package com.example.maquirentapp.Network;

import android.net.Uri;
import android.util.Log;

import com.example.maquirentapp.Model.Accesorio;
import com.example.maquirentapp.Model.AlquilerDia;
import com.example.maquirentapp.Model.AlquilerMensual;
import com.example.maquirentapp.Model.DetalleMes;
import com.example.maquirentapp.Model.FiltroCategoria;
import com.example.maquirentapp.Model.FotoEquipo;
import com.example.maquirentapp.Model.InfoPlaca;
import com.example.maquirentapp.Model.GrupoElectrogeno;
import com.example.maquirentapp.Model.Ingreso;
import com.example.maquirentapp.Model.Plano;
import com.example.maquirentapp.Model.Tarea;
import com.example.maquirentapp.Model.Usuario;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class FirebaseServicio {
    private final FirebaseFirestore db;
    private final FirebaseStorage storage;
    private final FirebaseFunctions functions;
    private final FirebaseAuth auth;

    public FirebaseServicio() {
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        auth = FirebaseAuth.getInstance();
        functions = FirebaseFunctions.getInstance();
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

    public void actualizarCodigoGrupo(String grupoId, String nuevoCodigo, OnSimpleCallback callback) {
        db.collection("gruposElectrogenos").document(grupoId)
                .update("codigo", nuevoCodigo)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onError);
    }

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
    public void getGruposElectrogenos(boolean incluirEliminados, OnGruposLoadedListener listener) {
        Query query = db.collection("gruposElectrogenos");

        if (!incluirEliminados) {
            query = query.whereEqualTo("eliminado", false);
        }
        query.get().addOnCompleteListener(task -> {
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

    //Mét0dos para accesorios
    public void getAccesorios(String tipo, OnAccesoriosLoadedListener listener) {
        db.collection("accesorios")
                .whereEqualTo("tipo", tipo)
                .orderBy("fechaCreacion", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Accesorio> accesorios = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Accesorio accesorio = doc.toObject(Accesorio.class);
                        accesorio.setId(doc.getId());
                        accesorios.add(accesorio);
                    }
                    listener.onSuccess(accesorios);
                })
                .addOnFailureListener(listener::onError);
    }

    public void getAccesorioPorId(String id, OnAccesorioLoadedListener listener) {
        db.collection("accesorios")
                .document(id)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Accesorio accesorio = documentSnapshot.toObject(Accesorio.class);
                        if (accesorio != null) {
                            accesorio.setId(documentSnapshot.getId());
                            listener.onSuccess(accesorio);
                        } else {
                            listener.onError(new Exception("Accesorio no encontrado"));
                        }
                    } else {
                        listener.onError(new Exception("Accesorio no existe"));
                    }
                })
                .addOnFailureListener(listener::onError);
    }

    //Mét0dos para alquileres mensuales
    public void getAlquilerMensualPorId(String id, OnAlquilerMensualLoadedListener listener) {
        db.collection("alquileresMensuales")
                .document(id)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        AlquilerMensual alquiler = documentSnapshot.toObject(AlquilerMensual.class);
                        if (alquiler != null) {
                            alquiler.setId(documentSnapshot.getId());
                            listener.onSuccess(alquiler);
                        } else {
                            listener.onError(new Exception("Alquiler no encontrado"));
                        }
                    } else {
                        listener.onError(new Exception("Alquiler no existe"));
                    }
                })
                .addOnFailureListener(listener::onError);
    }

    public void actualizarAlquilerMensual(AlquilerMensual alquiler, OnAlquilerUpdatedListener listener) {
        if (alquiler.getId() == null || alquiler.getId().isEmpty()) {
            listener.onError(new Exception("ID de alquiler inválido"));
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("nombreCliente", alquiler.getNombreCliente());
        updates.put("ubicacion", alquiler.getUbicacion());
        updates.put("fechaInicial", alquiler.getFechaInicial());
        updates.put("fechaFinal", alquiler.getFechaFinal());
        updates.put("horometroInicial", alquiler.getHorometroInicial());
        updates.put("horometroFinal", alquiler.getHorometroFinal());
        updates.put("precioAlquiler", alquiler.getPrecioAlquiler());
        updates.put("moneda", alquiler.getMoneda());
        updates.put("horasMinimas", alquiler.getHorasMinimas());
        updates.put("precioHoraExtra", alquiler.getPrecioHoraExtra());
        updates.put("idGrupo", alquiler.getIdGrupo());
        updates.put("accesoriosIds", alquiler.getAccesoriosIds());
        updates.put("finalizado", alquiler.isFinalizado());

        db.collection("alquileresMensuales")
                .document(alquiler.getId())
                .update(updates)
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(listener::onError);
    }

    public void eliminarAlquilerMensual(String id, OnAlquilerDeletedListener listener) {
        if (id == null || id.isEmpty()) {
            listener.onError(new Exception("ID de alquiler inválido"));
            return;
        }

        db.collection("alquileresMensuales")
                .document(id)
                .delete()
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(listener::onError);
    }

    // Mét0dos para DetalleMes
    public void crearDetalleMes(DetalleMes detalle, OnDetalleMesCreatedListener listener) {
        if (detalle.getIdAlquilerMensual() == null || detalle.getIdAlquilerMensual().isEmpty()) {
            listener.onError(new Exception("idAlquilerMensual no puede estar vacío"));
            return;
        }

        db.collection("detallesMes")
                .add(detalle)
                .addOnSuccessListener(documentReference -> {
                    detalle.setId(documentReference.getId());
                    listener.onSuccess(detalle);
                })
                .addOnFailureListener(listener::onError);
    }

    public void getDetallesMesPorAlquiler(String idAlquilerMensual, OnDetallesMesLoadedListener listener) {
        db.collection("detallesMes")
                .whereEqualTo("idAlquilerMensual", idAlquilerMensual)
                .orderBy("numeroMes", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<DetalleMes> detalles = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        DetalleMes detalle = doc.toObject(DetalleMes.class);
                        detalle.setId(doc.getId());
                        detalles.add(detalle);
                    }
                    listener.onSuccess(detalles);
                })
                .addOnFailureListener(listener::onError);
    }

    public void getDetalleMesPorId(String id, OnDetalleMesLoadedListener listener) {
        db.collection("detallesMes")
                .document(id)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        DetalleMes detalle = documentSnapshot.toObject(DetalleMes.class);
                        if (detalle != null) {
                            detalle.setId(documentSnapshot.getId());
                            listener.onSuccess(detalle);
                        } else {
                            listener.onError(new Exception("Detalle no encontrado"));
                        }
                    } else {
                        listener.onError(new Exception("Detalle no existe"));
                    }
                })
                .addOnFailureListener(listener::onError);
    }

    public void actualizarDetalleMes(DetalleMes detalle, OnDetalleMesUpdatedListener listener) {
        if (detalle.getId() == null || detalle.getId().isEmpty()) {
            listener.onError(new Exception("ID de detalle inválido"));
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("tituloPeriodo", detalle.getTituloPeriodo());
        updates.put("fechaInicio", detalle.getFechaInicio());
        updates.put("fechaFin", detalle.getFechaFin());
        updates.put("horometro", detalle.getHorometro());
        updates.put("horasExtras", detalle.getHorasExtras());
        updates.put("precioHorasExtras", detalle.getPrecioHorasExtras());
        updates.put("pagoMesConfirmado", detalle.isPagoMesConfirmado());
        updates.put("pagoHEConfirmado", detalle.isPagoHEConfirmado());
        updates.put("numeroMes", detalle.getNumeroMes());
        updates.put("fechaConfirmacionPagoMes", detalle.getFechaConfirmacionPagoMes());
        updates.put("fechaConfirmacionPagoHE", detalle.getFechaConfirmacionPagoHE());

        db.collection("detallesMes")
                .document(detalle.getId())
                .update(updates)
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(listener::onError);
    }

    public void eliminarDetalleMes(String id, OnDetalleMesDeletedListener listener) {
        if (id == null || id.isEmpty()) {
            listener.onError(new Exception("ID de detalle inválido"));
            return;
        }

        db.collection("detallesMes")
                .document(id)
                .delete()
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(listener::onError);
    }

    // Obtener el último DetalleMes de un alquiler
    public void getUltimoDetalleMes(String idAlquilerMensual, OnDetalleMesLoadedListener listener) {
        db.collection("detallesMes")
                .whereEqualTo("idAlquilerMensual", idAlquilerMensual)
                .orderBy("numeroMes", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DetalleMes detalle = queryDocumentSnapshots.getDocuments().get(0).toObject(DetalleMes.class);
                        if (detalle != null) {
                            detalle.setId(queryDocumentSnapshots.getDocuments().get(0).getId());
                            listener.onSuccess(detalle);
                        } else {
                            listener.onError(new Exception("Detalle no encontrado"));
                        }
                    } else {
                        listener.onError(new Exception("No hay detalles de mes"));
                    }
                })
                .addOnFailureListener(listener::onError);
    }

    // Obtener DetalleMes por número de mes
    public void getDetalleMesPorNumero(String idAlquilerMensual, int numeroMes, OnDetalleMesLoadedListener listener) {
        db.collection("detallesMes")
                .whereEqualTo("idAlquilerMensual", idAlquilerMensual)
                .whereEqualTo("numeroMes", numeroMes)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DetalleMes detalle = queryDocumentSnapshots.getDocuments().get(0).toObject(DetalleMes.class);
                        if (detalle != null) {
                            detalle.setId(queryDocumentSnapshots.getDocuments().get(0).getId());
                            listener.onSuccess(detalle);
                        } else {
                            listener.onError(new Exception("Detalle no encontrado"));
                        }
                    } else {
                        listener.onError(new Exception("No existe detalle para ese mes"));
                    }
                })
                .addOnFailureListener(listener::onError);
    }

    // Obtener alquileres mensuales por grupo y mes
    public void getAlquileresMensualesPorGrupoYMes(String idGrupo, int mes, int anio, OnAlquileresActivosListener listener) {
        db.collection("alquileresMensuales")
                .whereEqualTo("idGrupo", idGrupo)
                .whereEqualTo("finalizado", false)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<AlquilerMensual> alquileres = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        AlquilerMensual alquiler = doc.toObject(AlquilerMensual.class);
                        alquiler.setId(doc.getId());

                        // Filtrar por mes y año
                        if (alquilerEnMes(alquiler, mes, anio)) {
                            alquileres.add(alquiler);
                        }
                    }
                    listener.onSuccess(alquileres);
                })
                .addOnFailureListener(listener::onError);
    }

    private boolean alquilerEnMes(AlquilerMensual alquiler, int mes, int anio) {
        // Lógica para verificar si un alquiler está activo en un mes específico
        // Esto requeriría parsear las fechas y verificar
        return true; // Implementar lógica completa
    }

    public void solicitarCodigoFinalizacion(String alquilerId, String nombreCliente, OnSimpleCallback listener) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null || user.getEmail() == null) {
            listener.onError(new Exception("Usuario no autenticado"));
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("alquilerId", alquilerId);
        data.put("nombreCliente", nombreCliente);
        data.put("emailUsuario", user.getEmail());

        functions.getHttpsCallable("enviarCodigoFinalizarAlquiler")
                .call(data)
                .addOnSuccessListener(result -> listener.onSuccess())
                .addOnFailureListener(listener::onError);
    }

    public void confirmarFinalizacion(String alquilerId, String codigo,
                                      double horometroFinal, String fechaFinal,
                                      OnSimpleCallback listener) {
        Map<String, Object> data = new HashMap<>();
        data.put("alquilerId", alquilerId);
        data.put("codigoIngresado", codigo);
        data.put("horometroFinal", horometroFinal);
        data.put("fechaFinal", fechaFinal);

        functions.getHttpsCallable("confirmarFinalizacionAlquiler")
                .call(data)
                .addOnSuccessListener(result -> listener.onSuccess())
                .addOnFailureListener(listener::onError);
    }

    public void guardarFCMToken(String token) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null || token == null || token.isEmpty()) {
            return;
        }

        String uid = user.getUid();
        db.collection("usuarios").document(uid)
                .update("fcmTokens", FieldValue.arrayUnion(token))
                .addOnSuccessListener(aVoid -> {
                    Log.d("FirebaseServicio", "FCM Token guardado para " + uid);
                })
                .addOnFailureListener(e -> {
                    Log.e("FirebaseServicio", "Error al guardar FCM Token", e);
                });
    }

    public void registrarIngreso(Ingreso ingreso, OnIngresoRegistradoListener listener) {
        db.collection("ingresosRegistrados")
                .add(ingreso)
                .addOnSuccessListener(documentReference -> {
                    listener.onSuccess(documentReference.getId());
                })
                .addOnFailureListener(listener::onError);
    }

    public void getIngresosPorGrupoYMes(String idGrupo, int mes, int anio, OnIngresosLoadedListener listener) {
        db.collection("ingresosRegistrados")
                .whereEqualTo("idGrupo", idGrupo)
                .whereEqualTo("mes", mes + 1)
                .whereEqualTo("anio", anio)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Ingreso> ingresos = queryDocumentSnapshots.toObjects(Ingreso.class);
                    listener.onSuccess(ingresos);
                })
                .addOnFailureListener(listener::onError);
    }

    //MÉT0DOS PARA ALQUILER DIARIO
    public void crearAlquilerDia(AlquilerDia alquiler, OnAlquilerDiaCreadoListener listener) {
        db.collection("alquileresDiarios")
                .add(alquiler)
                .addOnSuccessListener(documentReference -> {
                    alquiler.setId(documentReference.getId());
                    listener.onSuccess(alquiler);
                })
                .addOnFailureListener(listener::onError);
    }

    public void actualizarAlquilerDia(AlquilerDia alquiler, OnSimpleCallback listener) {
        if (alquiler.getId() == null || alquiler.getId().isEmpty()) {
            listener.onError(new Exception("ID de alquiler diario inválido"));
            return;
        }
        db.collection("alquileresDiarios")
                .document(alquiler.getId())
                .set(alquiler)
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(listener::onError);
    }

    public void eliminarAlquilerDia(String id, OnSimpleCallback listener) {
        if (id == null || id.isEmpty()) {
            listener.onError(new Exception("ID de alquiler inválido"));
            return;
        }
        db.collection("alquileresDiarios")
                .document(id)
                .delete()
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(listener::onError);
    }

    public void getAlquilerDiaPorId(String id, OnAlquilerDiaLoadedListener listener) {
        db.collection("alquileresDiarios")
                .document(id)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        AlquilerDia alquiler = documentSnapshot.toObject(AlquilerDia.class);
                        if (alquiler != null) {
                            alquiler.setId(documentSnapshot.getId());
                            listener.onSuccess(alquiler);
                        } else {
                            listener.onError(new Exception("Alquiler no encontrado"));
                        }
                    } else {
                        listener.onError(new Exception("Alquiler no existe"));
                    }
                })
                .addOnFailureListener(listener::onError);
    }

    // Mét0do para la pantalla de historial
    public void getAlquileresDiariosPorGrupo(String idGrupo, int mes, int anio, OnAlquileresDiariosLoadedListener listener) {
        db.collection("alquileresDiarios")
                .whereEqualTo("idGrupo", idGrupo)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<AlquilerDia> alquileres = new ArrayList<>();
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        AlquilerDia alquiler = doc.toObject(AlquilerDia.class);
                        alquiler.setId(doc.getId());

                        try {
                            if(alquiler.getFechaInicial() == null || alquiler.getFechaInicial().isEmpty()) continue;

                            Date fechaInicio = sdf.parse(alquiler.getFechaInicial());
                            Calendar cal = Calendar.getInstance();
                            cal.setTime(fechaInicio);
                            if (cal.get(Calendar.MONTH) == mes && cal.get(Calendar.YEAR) == anio) {
                                alquileres.add(alquiler);
                            }
                        } catch (Exception e) {
                            Log.w("FirebaseServicio", "Fecha inválida en alquiler diario: " + doc.getId(), e);
                        }
                    }
                    listener.onSuccess(alquileres);
                })
                .addOnFailureListener(listener::onError);
    }

    // Mét0do para el Spinner en NuevoAlquilerDiaFragment (cuando viene de Home)
    public void getGruposParaSpinner(OnGruposLoadedListener listener) {
        db.collection("gruposElectrogenos")
                .whereEqualTo("eliminado", false)
                .orderBy("codigo")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<GrupoElectrogeno> grupos = new ArrayList<>();
                    for(QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        GrupoElectrogeno g = doc.toObject(GrupoElectrogeno.class);
                        g.setId(doc.getId());
                        grupos.add(g);
                    }
                    listener.onSuccess(grupos);
                })
                .addOnFailureListener(listener::onError);
    }

    // Mét0do para finalizar y registrar el ingreso prorrateado
    public void finalizarAlquilerDiario(AlquilerDia alquiler, OnSimpleCallback listener) {
        // 1. Validar que tengamos toda la info
        if (alquiler == null || alquiler.getPrecioTotal() == 0 ||
                alquiler.getFechaInicial() == null || alquiler.getFechaFinal() == null ||
                alquiler.getFechaInicial().isEmpty() || alquiler.getFechaFinal().isEmpty()) {
            listener.onError(new Exception("Datos incompletos para prorrateo (Fechas, Precio)"));
            return;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

        try {
            Date fechaInicio = sdf.parse(alquiler.getFechaInicial());
            Date fechaFin = sdf.parse(alquiler.getFechaFinal());
            if (fechaInicio == null || fechaFin == null || fechaInicio.after(fechaFin)) {
                listener.onError(new Exception("Fechas inválidas"));
                return;
            }

            // 2. Calcular días totales y precio por día
            long diffMillis = fechaFin.getTime() - fechaInicio.getTime();
            long diasTotales = TimeUnit.MILLISECONDS.toDays(diffMillis) + 1;

            if (diasTotales <= 0) {
                listener.onError(new Exception("El período debe ser de al menos 1 día"));
                return;
            }

            double precioPorDia = alquiler.getPrecioTotal() / diasTotales;
            Log.d("Prorrateo", "Dias Totales: " + diasTotales + ", Precio/Día: " + precioPorDia);

            // 3. Crear los documentos de Ingreso Prorrateados en un Lote
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
                    Log.d("Prorrateo", "Mes: " + mesActual + ", Días: " + diasEnEsteMes + ", Monto: " + montoProrrateado);

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

                    DocumentReference ingresoRef = db.collection("ingresosRegistrados").document();
                    batch.set(ingresoRef, ingreso);
                }
            }

            // 4. Marcar el alquiler como finalizado
            DocumentReference alquilerRef = db.collection("alquileresDiarios").document(alquiler.getId());
            batch.update(alquilerRef, "finalizado", true);

            // 5. Ejecutar todas las tareas
            batch.commit()
                    .addOnSuccessListener(aVoid -> listener.onSuccess())
                    .addOnFailureListener(listener::onError);

        } catch (Exception e) {
            listener.onError(e);
        }
    }
    public void getAlquileresDiariosActivos(OnAlquileresDiariosLoadedListener listener) {
        db.collection("alquileresDiarios")
                .whereEqualTo("finalizado", false)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<AlquilerDia> alquileres = new ArrayList<>();
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        AlquilerDia alquiler = doc.toObject(AlquilerDia.class);
                        alquiler.setId(doc.getId());
                        alquileres.add(alquiler);
                    }

                    Collections.sort(alquileres, (a1, a2) -> {
                        try {
                            Date d1 = sdf.parse(a1.getFechaInicial());
                            Date d2 = sdf.parse(a2.getFechaInicial());
                            if (d1 != null && d2 != null) {
                                return d2.compareTo(d1);
                            }
                        } catch (Exception e) {}
                        return 0;
                    });

                    listener.onSuccess(alquileres);
                })
                .addOnFailureListener(listener::onError);
    }
    public void subirPlano(Uri imagenUri, OnSimpleCallback listener) {
        String nombreArchivo = "plano_" + System.currentTimeMillis() + ".jpg";
        StorageReference storageRef = storage.getReference().child("planos_voltaje/" + nombreArchivo);

        storageRef.putFile(imagenUri)
                .addOnSuccessListener(taskSnapshot -> storageRef.getDownloadUrl()
                        .addOnSuccessListener(downloadUri -> {
                            DocumentReference docRef = db.collection("planosVoltaje").document();

                            Plano plano = new Plano(docRef.getId(), downloadUri.toString(), nombreArchivo);

                            docRef.set(plano)
                                    .addOnSuccessListener(aVoid -> listener.onSuccess())
                                    .addOnFailureListener(listener::onError);
                        })
                        .addOnFailureListener(listener::onError))
                .addOnFailureListener(listener::onError);
    }

    public void getPlanosVoltaje(OnPlanosLoadedListener listener) {
        db.collection("planosVoltaje")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Plano> planos = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Plano p = doc.toObject(Plano.class);
                        p.setId(doc.getId());
                        planos.add(p);
                    }
                    listener.onSuccess(planos);
                })
                .addOnFailureListener(listener::onError);
    }
    public void eliminarPlano(Plano plano, OnSimpleCallback listener) {
        if (plano == null || plano.getId() == null) {
            listener.onError(new Exception("Plano inválido"));
            return;
        }

        // 1. Eliminar documento de Firestore
        db.collection("planosVoltaje").document(plano.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    // 2. Intentar eliminar imagen de Storage
                    if (plano.getUrlImagen() != null && !plano.getUrlImagen().isEmpty()) {
                        try {
                            StorageReference photoRef = storage.getReferenceFromUrl(plano.getUrlImagen());
                            photoRef.delete()
                                    .addOnSuccessListener(aVoid1 -> listener.onSuccess())
                                    .addOnFailureListener(e -> {
                                        Log.w("FirebaseServicio", "No se pudo borrar imagen de Storage: " + e.getMessage());
                                        listener.onSuccess();
                                    });
                        } catch (Exception e) {
                            listener.onSuccess();
                        }
                    } else {
                        listener.onSuccess();
                    }
                })
                .addOnFailureListener(listener::onError);
    }
    // --- SECCIÓN TAREAS ---
    public void crearTarea(String titulo, OnSimpleCallback listener) {
        DocumentReference docRef = db.collection("tareas").document();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        String fecha = sdf.format(new Date());

        Tarea tarea = new Tarea(docRef.getId(), titulo, fecha, false);

        docRef.set(tarea)
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(listener::onError);
    }

    public void getTareas(OnTareasLoadedListener listener) {
        db.collection("tareas")
                .orderBy("fechaCreacion", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        listener.onError(error);
                        return;
                    }
                    List<Tarea> tareas = new ArrayList<>();
                    if (value != null) {
                        for (QueryDocumentSnapshot doc : value) {
                            tareas.add(doc.toObject(Tarea.class));
                        }
                    }
                    listener.onSuccess(tareas);
                });
    }

    public void completarTarea(String idTarea, List<String> participantesIds, OnSimpleCallback listener) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("completada", true);
        updates.put("participantesIds", participantesIds);

        db.collection("tareas").document(idTarea)
                .update(updates)
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(listener::onError);
    }
    public void eliminarTarea(String idTarea, OnSimpleCallback listener) {
        db.collection("tareas").document(idTarea)
                .delete()
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(listener::onError);
    }
    public void verificarSiEsAdmin(OnAdminCheckListener listener) {
        if (auth.getCurrentUser() == null) return;

        db.collection("usuarios").document(auth.getCurrentUser().getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String rol = documentSnapshot.getString("rol");
                        boolean esAdmin = "admin".equalsIgnoreCase(rol) || "administrador".equalsIgnoreCase(rol);
                        listener.onCheck(esAdmin);
                    } else {
                        listener.onCheck(false);
                    }
                })
                .addOnFailureListener(e -> listener.onCheck(false));
    }
        // Obtener todos los usuarios activos para la lista de selección
    public void getUsuariosActivos(OnUsuariosListener listener) {
        db.collection("usuarios")
                .whereEqualTo("estado", "activo")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Usuario> usuarios = querySnapshot.toObjects(Usuario.class);
                    listener.onSuccess(usuarios);
                })
                .addOnFailureListener(listener::onError);
    }
    // --- SECCIÓN FOTOS EQUIPO ---

    public void subirFotoEquipo(String idGrupo, Uri imagenUri, OnSimpleCallback listener) {
        String nombreArchivo = "foto_equipo_" + System.currentTimeMillis() + ".jpg";
        StorageReference storageRef = storage.getReference().child("grupos/" + idGrupo + "/fotos/" + nombreArchivo);

        storageRef.putFile(imagenUri)
                .addOnSuccessListener(taskSnapshot -> storageRef.getDownloadUrl()
                        .addOnSuccessListener(downloadUri -> {
                            DocumentReference docRef = db.collection("fotosEquipo").document();

                            FotoEquipo foto = new FotoEquipo(docRef.getId(), downloadUri.toString(), nombreArchivo, idGrupo);

                            docRef.set(foto)
                                    .addOnSuccessListener(aVoid -> listener.onSuccess())
                                    .addOnFailureListener(listener::onError);
                        })
                        .addOnFailureListener(listener::onError))
                .addOnFailureListener(listener::onError);
    }

    public void getFotosEquipo(String idGrupo, OnFotosEquipoLoadedListener listener) {
        db.collection("fotosEquipo")
                .whereEqualTo("idGrupo", idGrupo)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<FotoEquipo> fotos = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        FotoEquipo f = doc.toObject(FotoEquipo.class);
                        f.setId(doc.getId());
                        fotos.add(f);
                    }
                    listener.onSuccess(fotos);
                })
                .addOnFailureListener(listener::onError);
    }

    public void eliminarFotoEquipo(FotoEquipo foto, OnSimpleCallback listener) {
        if (foto == null || foto.getId() == null) {
            listener.onError(new Exception("Foto inválida"));
            return;
        }

        db.collection("fotosEquipo").document(foto.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    if (foto.getUrlImagen() != null && !foto.getUrlImagen().isEmpty()) {
                        try {
                            StorageReference photoRef = storage.getReferenceFromUrl(foto.getUrlImagen());
                            photoRef.delete()
                                    .addOnSuccessListener(aVoid1 -> listener.onSuccess())
                                    .addOnFailureListener(e -> {
                                        Log.w("FirebaseServicio", "Error borrar storage: " + e.getMessage());
                                        listener.onSuccess();
                                    });
                        } catch (Exception e) {
                            listener.onSuccess();
                        }
                    } else {
                        listener.onSuccess();
                    }
                })
                .addOnFailureListener(listener::onError);
    }
    // SECCIÓN INFORMACIÓN GENERAL (FILTROS Y PLACA)
    // FILTROS
    public void crearCategoriaFiltro(FiltroCategoria categoria, OnSimpleCallback listener) {
        DocumentReference docRef = db.collection("filtrosGrupo").document();
        categoria.setId(docRef.getId());
        docRef.set(categoria)
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(listener::onError);
    }
    public void actualizarCategoriaFiltro(FiltroCategoria categoria, OnSimpleCallback listener) {
        db.collection("filtrosGrupo").document(categoria.getId())
                .set(categoria)
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(listener::onError);
    }
    public void eliminarCategoriaFiltro(String idCategoria, OnSimpleCallback listener) {
        db.collection("filtrosGrupo").document(idCategoria)
                .delete()
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(listener::onError);
    }
    public void getFiltrosPorGrupo(String idGrupo, OnFiltrosLoadedListener listener) {
        db.collection("filtrosGrupo")
                .whereEqualTo("idGrupo", idGrupo)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<FiltroCategoria> lista = new ArrayList<>();
                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        lista.add(doc.toObject(FiltroCategoria.class));
                    }
                    listener.onSuccess(lista);
                })
                .addOnFailureListener(listener::onError);
    }
    //INFO PLACA
    public void getInfoPlaca(String idGrupo, OnInfoPlacaLoadedListener listener) {
        db.collection("infoPlacaGrupo")
                .whereEqualTo("idGrupo", idGrupo)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        InfoPlaca info = queryDocumentSnapshots.getDocuments().get(0).toObject(InfoPlaca.class);
                        listener.onSuccess(info);
                    } else {
                        listener.onSuccess(null);
                    }
                })
                .addOnFailureListener(listener::onError);
    }
    public void guardarInfoPlaca(InfoPlaca info, OnSimpleCallback listener) {
        DocumentReference docRef;
        if (info.getId() == null) {
            docRef = db.collection("infoPlacaGrupo").document();
            info.setId(docRef.getId());
        } else {
            docRef = db.collection("infoPlacaGrupo").document(info.getId());
        }
        docRef.set(info)
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(listener::onError);
    }
    public void subirFotoPlaca(String nombreCarpeta, Uri uri, OnUrlUploadedListener listener) {
        String nombreArchivo = "placa_" + System.currentTimeMillis() + ".jpg";

        StorageReference ref = storage.getReference().child("placas/" + nombreCarpeta + "/" + nombreArchivo);

        ref.putFile(uri).addOnSuccessListener(taskSnapshot ->
                ref.getDownloadUrl().addOnSuccessListener(uri1 -> listener.onSuccess(uri1.toString()))
        ).addOnFailureListener(listener::onError);
    }
    public void eliminarArchivoStorage(String url, OnSimpleCallback listener) {
        try {
            StorageReference ref = storage.getReferenceFromUrl(url);
            ref.delete()
                    .addOnSuccessListener(aVoid -> listener.onSuccess())
                    .addOnFailureListener(listener::onError);
        } catch (Exception e) {
            listener.onError(e);
        }
    }

    // Interfaces para callbacks
    public interface OnFiltrosLoadedListener {
        void onSuccess(List<FiltroCategoria> categorias);
        void onError(Exception e);
    }
    public interface OnInfoPlacaLoadedListener {
        void onSuccess(InfoPlaca info);
        void onError(Exception e);
    }
    public interface OnUrlUploadedListener {
        void onSuccess(String url);
        void onError(Exception e);
    }
    public interface OnFotosEquipoLoadedListener {
        void onSuccess(List<FotoEquipo> fotos);
        void onError(Exception e);
    }
    public interface OnAdminCheckListener {
        void onCheck(boolean isAdmin);
    }
    public interface OnTareasLoadedListener {
        void onSuccess(List<Tarea> tareas);
        void onError(Exception e);
    }
    public interface OnPlanosLoadedListener {
        void onSuccess(List<Plano> planos);
        void onError(Exception e);
    }
    public interface OnAlquilerDiaCreadoListener {
        void onSuccess(AlquilerDia alquiler);
        void onError(Exception e);
    }

    public interface OnAlquilerDiaLoadedListener {
        void onSuccess(AlquilerDia alquiler);
        void onError(Exception e);
    }

    public interface OnAlquileresDiariosLoadedListener {
        void onSuccess(List<AlquilerDia> alquileres);
        void onError(Exception e);
    }
    public interface OnIngresosLoadedListener {
        void onSuccess(List<Ingreso> ingresos);

        void onError(Exception e);
    }

    public interface OnIngresoRegistradoListener {
        void onSuccess(String id);

        void onError(Exception e);
    }

    public interface OnDetalleMesCreatedListener {
        void onSuccess(DetalleMes detalle);

        void onError(Exception e);
    }

    public interface OnDetallesMesLoadedListener {
        void onSuccess(List<DetalleMes> detalles);

        void onError(Exception e);
    }

    public interface OnDetalleMesLoadedListener {
        void onSuccess(DetalleMes detalle);

        void onError(Exception e);
    }

    public interface OnDetalleMesUpdatedListener {
        void onSuccess();

        void onError(Exception e);
    }

    public interface OnDetalleMesDeletedListener {
        void onSuccess();

        void onError(Exception e);
    }

    public interface OnAlquileresActivosListener {
        void onSuccess(List<AlquilerMensual> alquileres);

        void onError(Exception e);
    }

    public interface OnAccesoriosLoadedListener {
        void onSuccess(List<Accesorio> accesorios);

        void onError(Exception e);
    }

    public interface OnAccesorioLoadedListener {
        void onSuccess(Accesorio accesorio);

        void onError(Exception e);
    }

    public interface OnAlquilerMensualLoadedListener {
        void onSuccess(AlquilerMensual alquiler);

        void onError(Exception e);
    }

    public interface OnAlquilerUpdatedListener {
        void onSuccess();

        void onError(Exception e);
    }

    public interface OnAlquilerDeletedListener {
        void onSuccess();

        void onError(Exception e);
    }

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