package com.example.maquirentapp.Repository;

import android.net.Uri;

import com.example.maquirentapp.Model.FiltroCategoria;
import com.example.maquirentapp.Model.FotoEquipo;
import com.example.maquirentapp.Model.GrupoElectrogeno;
import com.example.maquirentapp.Model.InfoPlaca;
import com.example.maquirentapp.Model.Plano;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Repositorio centralizado para la gestión de Grupos Electrógenos,
 * Especificaciones de Placa, Filtros, Galería de Fotos y Planos de Conexión.
 */
public class GeneradorRepository {

    private final FirebaseFirestore db;
    private final FirebaseStorage storage;
    private final com.google.firebase.functions.FirebaseFunctions functions;

    private static final String COL_GRUPOS = "gruposElectrogenos";
    private static final String COL_INFO_PLACA = "infoPlacaGrupo";
    private static final String COL_FILTROS = "filtrosGrupo";
    private static final String COL_FOTOS = "fotosEquipo";
    private static final String COL_PLANOS = "planosVoltaje";

    public GeneradorRepository() {
        this.db = FirebaseFirestore.getInstance();
        this.storage = FirebaseStorage.getInstance();
        this.functions = com.google.firebase.functions.FirebaseFunctions.getInstance();
    }

    // ==========================================
    // 1. GRUPOS ELECTRÓGENOS
    // ==========================================

    public void getGruposElectrogenos(boolean incluirEliminados, Callback<List<GrupoElectrogeno>> callback) {
        Query query = db.collection(COL_GRUPOS);
        if (!incluirEliminados) {
            query = query.whereEqualTo("eliminado", false);
        }
        query.get().addOnSuccessListener(snapshot -> {
            List<GrupoElectrogeno> lista = new ArrayList<>();
            for (QueryDocumentSnapshot doc : snapshot) {
                GrupoElectrogeno grupo = doc.toObject(GrupoElectrogeno.class);
                grupo.setId(doc.getId());
                lista.add(grupo);
            }
            Collections.sort(lista, (g1, g2) -> Integer.compare(g1.getOrden(), g2.getOrden()));
            callback.onSuccess(lista);
        }).addOnFailureListener(callback::onError);
    }

    public void getGruposParaSpinner(Callback<List<GrupoElectrogeno>> callback) {
        db.collection(COL_GRUPOS)
                .whereEqualTo("eliminado", false)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<GrupoElectrogeno> lista = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        GrupoElectrogeno grupo = doc.toObject(GrupoElectrogeno.class);
                        grupo.setId(doc.getId());
                        lista.add(grupo);
                    }
                    callback.onSuccess(lista);
                })
                .addOnFailureListener(callback::onError);
    }

    public void getGrupoPorId(String idGrupo, Callback<GrupoElectrogeno> callback) {
        db.collection(COL_GRUPOS).document(idGrupo)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        GrupoElectrogeno grupo = doc.toObject(GrupoElectrogeno.class);
                        if (grupo != null) grupo.setId(doc.getId());
                        callback.onSuccess(grupo);
                    } else {
                        callback.onError(new Exception("Grupo no encontrado"));
                    }
                })
                .addOnFailureListener(callback::onError);
    }

    public void crearGrupoConImagen(String codigo, Uri imagenUri, Callback<GrupoElectrogeno> callback) {
        DocumentReference docRef = db.collection(COL_GRUPOS).document();
        GrupoElectrogeno grupo = new GrupoElectrogeno(codigo, "");
        grupo.setId(docRef.getId());

        if (imagenUri != null) {
            StorageReference storageRef = storage.getReference()
                    .child("grupos/" + docRef.getId() + ".jpg");

            StorageMetadata metadata = new StorageMetadata.Builder()
                    .setContentType("image/jpeg")
                    .build();

            storageRef.putFile(imagenUri, metadata)
                    .continueWithTask(task -> {
                        if (!task.isSuccessful()) throw task.getException();
                        return storageRef.getDownloadUrl();
                    })
                    .addOnSuccessListener(downloadUri -> {
                        grupo.setFoto(downloadUri.toString());
                        docRef.set(grupo)
                                .addOnSuccessListener(v -> callback.onSuccess(grupo))
                                .addOnFailureListener(callback::onError);
                    })
                    .addOnFailureListener(callback::onError);
        } else {
            docRef.set(grupo)
                    .addOnSuccessListener(v -> callback.onSuccess(grupo))
                    .addOnFailureListener(callback::onError);
        }
    }

    public void crearGrupoConImagenBytes(String codigo, byte[] data, Callback<GrupoElectrogeno> callback) {
        DocumentReference docRef = db.collection(COL_GRUPOS).document();
        GrupoElectrogeno grupo = new GrupoElectrogeno(codigo, "");
        grupo.setId(docRef.getId());

        if (data != null && data.length > 0) {
            StorageReference storageRef = storage.getReference()
                    .child("grupos/" + docRef.getId() + ".jpg");

            StorageMetadata metadata = new StorageMetadata.Builder()
                    .setContentType("image/jpeg")
                    .build();

            storageRef.putBytes(data, metadata)
                    .continueWithTask(task -> {
                        if (!task.isSuccessful()) throw task.getException();
                        return storageRef.getDownloadUrl();
                    })
                    .addOnSuccessListener(downloadUri -> {
                        grupo.setFoto(downloadUri.toString());
                        docRef.set(grupo)
                                .addOnSuccessListener(v -> callback.onSuccess(grupo))
                                .addOnFailureListener(callback::onError);
                    })
                    .addOnFailureListener(callback::onError);
        } else {
            docRef.set(grupo)
                    .addOnSuccessListener(v -> callback.onSuccess(grupo))
                    .addOnFailureListener(callback::onError);
        }
    }

    public void actualizarCodigoGrupo(String grupoId, String nuevoCodigo, Callback<Void> callback) {
        db.collection(COL_GRUPOS).document(grupoId)
                .update("codigo", nuevoCodigo)
                .addOnSuccessListener(v -> callback.onSuccess(null))
                .addOnFailureListener(callback::onError);
    }

    public void actualizarOrdenGrupos(List<GrupoElectrogeno> listaOrdenada, Callback<Void> callback) {
        WriteBatch batch = db.batch();
        for (int i = 0; i < listaOrdenada.size(); i++) {
            GrupoElectrogeno g = listaOrdenada.get(i);
            g.setOrden(i);
            DocumentReference ref = db.collection(COL_GRUPOS).document(g.getId());
            batch.update(ref, "orden", i);
        }
        batch.commit()
                .addOnSuccessListener(v -> callback.onSuccess(null))
                .addOnFailureListener(callback::onError);
    }

    public void eliminarGrupoSuave(String grupoId, Map<String, Object> updates, Callback<Void> callback) {
        db.collection(COL_GRUPOS).document(grupoId)
                .update(updates)
                .addOnSuccessListener(v -> callback.onSuccess(null))
                .addOnFailureListener(callback::onError);
    }

    public void actualizarGrupoConImagenBytes(String grupoId, String nuevoCodigo, byte[] dataImagen, Callback<Void> callback) {
        String fileName = "grupos/" + nuevoCodigo + "_" + System.currentTimeMillis() + ".jpg";
        StorageReference storageRef = storage.getReference().child(fileName);

        db.collection(COL_GRUPOS).document(grupoId).get()
                .addOnSuccessListener(doc -> {
                    String oldFotoUrl = doc != null ? doc.getString("foto") : null;
                    StorageMetadata metadata = new StorageMetadata.Builder()
                            .setContentType("image/jpeg")
                            .build();

                    storageRef.putBytes(dataImagen, metadata)
                            .continueWithTask(task -> {
                                if (!task.isSuccessful()) throw task.getException();
                                return storageRef.getDownloadUrl();
                            })
                            .addOnSuccessListener(downloadUri -> {
                                Map<String, Object> updates = new HashMap<>();
                                updates.put("codigo", nuevoCodigo);
                                updates.put("foto", downloadUri.toString());
                                db.collection(COL_GRUPOS).document(grupoId).update(updates)
                                        .addOnSuccessListener(v -> {
                                            if (oldFotoUrl != null && !oldFotoUrl.isEmpty() && oldFotoUrl.contains("firebasestorage")) {
                                                try {
                                                    storage.getReferenceFromUrl(oldFotoUrl).delete();
                                                } catch (Exception ignored) {}
                                            }
                                            callback.onSuccess(null);
                                        })
                                        .addOnFailureListener(callback::onError);
                            })
                            .addOnFailureListener(callback::onError);
                })
                .addOnFailureListener(callback::onError);
    }

    public void solicitarEliminacionGrupo(GrupoElectrogeno grupo, String userEmail, Callback<String> callback) {
        String codigo = String.valueOf(100000 + new java.util.Random().nextInt(900000));
        long timestamp = System.currentTimeMillis();

        Map<String, Object> updates = new HashMap<>();
        updates.put("codigoVerificacion", codigo);
        updates.put("codigoGeneradoEn", timestamp);

        db.collection(COL_GRUPOS).document(grupo.getId()).update(updates)
                .addOnSuccessListener(aVoid -> {
                    Map<String, Object> data = new HashMap<>();
                    data.put("email", userEmail);
                    data.put("codigo", codigo);
                    data.put("grupoNombre", grupo.getCodigo());

                    functions.getHttpsCallable("enviarCodigoEliminacion").call(data)
                            .addOnSuccessListener(result -> callback.onSuccess(codigo))
                            .addOnFailureListener(callback::onError);
                })
                .addOnFailureListener(callback::onError);
    }

    public void verificarCodigoEliminacion(String grupoId, String codigoIngresado, String userUid, Callback<Void> callback) {
        db.collection(COL_GRUPOS).document(grupoId).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        callback.onError(new Exception("Documento no encontrado"));
                        return;
                    }

                    String codigoGuardado = doc.getString("codigoVerificacion");
                    Long tsGenerado = doc.getLong("codigoGeneradoEn");

                    if (codigoGuardado == null || tsGenerado == null) {
                        callback.onError(new Exception("Código no encontrado. Solicita nuevamente."));
                        return;
                    }

                    long ahora = System.currentTimeMillis();
                    if ((ahora - tsGenerado) > (15 * 60 * 1000)) {
                        limpiarCodigoVerificacion(grupoId);
                        callback.onError(new Exception("El código ha expirado. Solicita nuevamente."));
                    } else if (codigoGuardado.equals(codigoIngresado)) {
                        limpiarCodigoVerificacion(grupoId);
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("eliminado", true);
                        updates.put("fechaEliminacion", System.currentTimeMillis());
                        updates.put("eliminadoPor", userUid);
                        eliminarGrupoSuave(grupoId, updates, callback);
                    } else {
                        callback.onError(new Exception("Código incorrecto"));
                    }
                })
                .addOnFailureListener(callback::onError);
    }

    public void limpiarCodigoVerificacion(String grupoId) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("codigoVerificacion", null);
        updates.put("codigoGeneradoEn", null);
        db.collection(COL_GRUPOS).document(grupoId).update(updates);
    }

    // ==========================================
    // 2. ESPECIFICACIONES DE PLACA
    // ==========================================

    public void getInfoPlaca(String idGrupo, Callback<InfoPlaca> callback) {
        if (idGrupo == null || idGrupo.isEmpty()) {
            callback.onSuccess(null);
            return;
        }
        db.collection(COL_INFO_PLACA)
                .whereEqualTo("idGrupo", idGrupo)
                .limit(1)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.isEmpty()) {
                        DocumentSnapshot doc = snapshot.getDocuments().get(0);
                        InfoPlaca info = doc.toObject(InfoPlaca.class);
                        if (info != null) info.setId(doc.getId());
                        callback.onSuccess(info);
                    } else {
                        // Respaldo por compatibilidad
                        db.collection(COL_INFO_PLACA).document(idGrupo)
                                .get()
                                .addOnSuccessListener(doc -> {
                                    if (doc.exists()) {
                                        InfoPlaca info = doc.toObject(InfoPlaca.class);
                                        if (info != null) info.setId(doc.getId());
                                        callback.onSuccess(info);
                                    } else {
                                        callback.onSuccess(null);
                                    }
                                })
                                .addOnFailureListener(callback::onError);
                    }
                })
                .addOnFailureListener(callback::onError);
    }

    public void guardarInfoPlaca(InfoPlaca info, Callback<Void> callback) {
        if (info == null || info.getIdGrupo() == null || info.getIdGrupo().isEmpty()) {
            callback.onError(new Exception("El ID de grupo es obligatorio"));
            return;
        }
        if (info.getId() != null && !info.getId().isEmpty()) {
            db.collection(COL_INFO_PLACA).document(info.getId())
                    .set(info)
                    .addOnSuccessListener(v -> callback.onSuccess(null))
                    .addOnFailureListener(callback::onError);
        } else {
            // Verificar si ya existe para este idGrupo antes de crear uno nuevo
            db.collection(COL_INFO_PLACA)
                    .whereEqualTo("idGrupo", info.getIdGrupo())
                    .limit(1)
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        DocumentReference docRef;
                        if (!snapshot.isEmpty()) {
                            docRef = snapshot.getDocuments().get(0).getReference();
                            info.setId(docRef.getId());
                        } else {
                            docRef = db.collection(COL_INFO_PLACA).document();
                            info.setId(docRef.getId());
                        }
                        docRef.set(info)
                                .addOnSuccessListener(v -> callback.onSuccess(null))
                                .addOnFailureListener(callback::onError);
                    })
                    .addOnFailureListener(callback::onError);
        }
    }

    public void subirFotoPlaca(String nombreCarpeta, Uri uri, Callback<String> callback) {
        String nombreArchivo = "placa_" + System.currentTimeMillis() + ".jpg";
        StorageReference ref = storage.getReference().child("placas/" + nombreCarpeta + "/" + nombreArchivo);
        StorageMetadata metadata = new StorageMetadata.Builder()
                .setContentType("image/jpeg")
                .build();
        ref.putFile(uri, metadata)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) throw task.getException();
                    return ref.getDownloadUrl();
                })
                .addOnSuccessListener(downloadUri -> callback.onSuccess(downloadUri.toString()))
                .addOnFailureListener(callback::onError);
    }

    public void subirFotoPlacaBytes(String nombreCarpeta, byte[] data, Callback<String> callback) {
        String nombreArchivo = "placa_" + System.currentTimeMillis() + ".jpg";
        StorageReference ref = storage.getReference().child("placas/" + nombreCarpeta + "/" + nombreArchivo);
        StorageMetadata metadata = new StorageMetadata.Builder()
                .setContentType("image/jpeg")
                .build();
        ref.putBytes(data, metadata)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) throw task.getException();
                    return ref.getDownloadUrl();
                })
                .addOnSuccessListener(downloadUri -> callback.onSuccess(downloadUri.toString()))
                .addOnFailureListener(callback::onError);
    }

    public void eliminarArchivoStorage(String url, Callback<Void> callback) {
        try {
            StorageReference ref = storage.getReferenceFromUrl(url);
            ref.delete()
                    .addOnSuccessListener(v -> callback.onSuccess(null))
                    .addOnFailureListener(callback::onError);
        } catch (Exception e) {
            callback.onError(e);
        }
    }

    // ==========================================
    // 3. FILTROS POR CATEGORÍA
    // ==========================================

    public void getFiltrosPorGrupo(String idGrupo, Callback<List<FiltroCategoria>> callback) {
        db.collection(COL_FILTROS)
                .whereEqualTo("idGrupo", idGrupo)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<FiltroCategoria> categorias = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        FiltroCategoria cat = doc.toObject(FiltroCategoria.class);
                        cat.setId(doc.getId());
                        categorias.add(cat);
                    }
                    callback.onSuccess(categorias);
                })
                .addOnFailureListener(callback::onError);
    }

    public void crearCategoriaFiltro(FiltroCategoria categoria, Callback<Void> callback) {
        DocumentReference docRef = db.collection(COL_FILTROS).document();
        categoria.setId(docRef.getId());
        docRef.set(categoria)
                .addOnSuccessListener(v -> callback.onSuccess(null))
                .addOnFailureListener(callback::onError);
    }

    public void actualizarCategoriaFiltro(FiltroCategoria categoria, Callback<Void> callback) {
        db.collection(COL_FILTROS).document(categoria.getId())
                .set(categoria)
                .addOnSuccessListener(v -> callback.onSuccess(null))
                .addOnFailureListener(callback::onError);
    }

    public void eliminarCategoriaFiltro(String idCategoria, Callback<Void> callback) {
        db.collection(COL_FILTROS).document(idCategoria)
                .delete()
                .addOnSuccessListener(v -> callback.onSuccess(null))
                .addOnFailureListener(callback::onError);
    }

    // ==========================================
    // 4. GALERÍA DE FOTOS DE EQUIPO
    // ==========================================

    public void getFotosEquipo(String idGrupo, Callback<List<FotoEquipo>> callback) {
        db.collection(COL_FOTOS)
                .whereEqualTo("idGrupo", idGrupo)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<FotoEquipo> lista = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        FotoEquipo foto = doc.toObject(FotoEquipo.class);
                        foto.setId(doc.getId());
                        lista.add(foto);
                    }
                    callback.onSuccess(lista);
                })
                .addOnFailureListener(callback::onError);
    }

    public void subirFotoEquipo(String idGrupo, Uri imagenUri, Callback<Void> callback) {
        String nombreArchivo = System.currentTimeMillis() + ".jpg";
        StorageReference ref = storage.getReference().child("fotos_equipo/" + idGrupo + "/" + nombreArchivo);
        StorageMetadata metadata = new StorageMetadata.Builder()
                .setContentType("image/jpeg")
                .build();

        ref.putFile(imagenUri, metadata)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) throw task.getException();
                    return ref.getDownloadUrl();
                })
                .addOnSuccessListener(uri -> {
                    FotoEquipo foto = new FotoEquipo(null, uri.toString(), nombreArchivo, idGrupo);
                    db.collection(COL_FOTOS).add(foto)
                            .addOnSuccessListener(doc -> callback.onSuccess(null))
                            .addOnFailureListener(callback::onError);
                })
                .addOnFailureListener(callback::onError);
    }

    public void subirFotoEquipoBytes(String idGrupo, byte[] data, Callback<Void> callback) {
        String nombreArchivo = System.currentTimeMillis() + ".jpg";
        StorageReference ref = storage.getReference().child("fotos_equipo/" + idGrupo + "/" + nombreArchivo);
        StorageMetadata metadata = new StorageMetadata.Builder()
                .setContentType("image/jpeg")
                .build();

        ref.putBytes(data, metadata)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) throw task.getException();
                    return ref.getDownloadUrl();
                })
                .addOnSuccessListener(uri -> {
                    FotoEquipo foto = new FotoEquipo(null, uri.toString(), nombreArchivo, idGrupo);
                    db.collection(COL_FOTOS).add(foto)
                            .addOnSuccessListener(doc -> callback.onSuccess(null))
                            .addOnFailureListener(callback::onError);
                })
                .addOnFailureListener(callback::onError);
    }

    public void eliminarFotoEquipo(FotoEquipo foto, Callback<Void> callback) {
        if (foto.getId() == null) {
            callback.onError(new Exception("Foto sin ID"));
            return;
        }

        db.collection(COL_FOTOS).document(foto.getId()).delete()
                .addOnSuccessListener(v -> {
                    if (foto.getUrlImagen() != null && !foto.getUrlImagen().isEmpty()) {
                        try {
                            StorageReference ref = storage.getReferenceFromUrl(foto.getUrlImagen());
                            ref.delete();
                        } catch (Exception ignored) {}
                    }
                    callback.onSuccess(null);
                })
                .addOnFailureListener(callback::onError);
    }

    // ==========================================
    // 5. PLANOS DE CAMBIO DE VOLTAJE
    // ==========================================

    public void getPlanosVoltaje(Callback<List<Plano>> callback) {
        db.collection(COL_PLANOS).get()
                .addOnSuccessListener(snapshot -> {
                    List<Plano> planos = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        Plano plano = doc.toObject(Plano.class);
                        plano.setId(doc.getId());
                        planos.add(plano);
                    }
                    callback.onSuccess(planos);
                })
                .addOnFailureListener(callback::onError);
    }

    public void subirPlano(Uri uri, Callback<Void> callback) {
        String nombreArchivo = System.currentTimeMillis() + ".jpg";
        StorageReference ref = storage.getReference().child("planos/" + nombreArchivo);
        StorageMetadata metadata = new StorageMetadata.Builder()
                .setContentType("image/jpeg")
                .build();

        ref.putFile(uri, metadata)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) throw task.getException();
                    return ref.getDownloadUrl();
                })
                .addOnSuccessListener(downloadUri -> {
                    Plano plano = new Plano(null, downloadUri.toString(), nombreArchivo);
                    db.collection(COL_PLANOS).add(plano)
                            .addOnSuccessListener(doc -> callback.onSuccess(null))
                            .addOnFailureListener(callback::onError);
                })
                .addOnFailureListener(callback::onError);
    }

    public void subirPlanoBytes(byte[] data, Callback<Void> callback) {
        String nombreArchivo = System.currentTimeMillis() + ".jpg";
        StorageReference ref = storage.getReference().child("planos/" + nombreArchivo);
        StorageMetadata metadata = new StorageMetadata.Builder()
                .setContentType("image/jpeg")
                .build();

        ref.putBytes(data, metadata)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) throw task.getException();
                    return ref.getDownloadUrl();
                })
                .addOnSuccessListener(downloadUri -> {
                    Plano plano = new Plano(null, downloadUri.toString(), nombreArchivo);
                    db.collection(COL_PLANOS).add(plano)
                            .addOnSuccessListener(doc -> callback.onSuccess(null))
                            .addOnFailureListener(callback::onError);
                })
                .addOnFailureListener(callback::onError);
    }

    public void eliminarPlano(Plano plano, Callback<Void> callback) {
        if (plano.getId() == null) {
            callback.onError(new Exception("Plano sin ID"));
            return;
        }

        db.collection(COL_PLANOS).document(plano.getId()).delete()
                .addOnSuccessListener(v -> {
                    if (plano.getUrlImagen() != null && !plano.getUrlImagen().isEmpty()) {
                        try {
                            StorageReference ref = storage.getReferenceFromUrl(plano.getUrlImagen());
                            ref.delete();
                        } catch (Exception ignored) {}
                    }
                    callback.onSuccess(null);
                })
                .addOnFailureListener(callback::onError);
    }
}
