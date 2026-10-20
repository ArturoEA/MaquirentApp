package com.example.maquirentapp.Repository;

import android.net.Uri;

import com.example.maquirentapp.Model.FichaTecnica;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Repositorio de Fichas Técnicas con soporte Offline-First por defecto.
 * Los metadatos se almacenan en Firestore para consulta instantánea sin conexión,
 * y los archivos se sincronizan con Firebase Storage.
 */
public class FichaTecnicaRepository {

    private static FichaTecnicaRepository instance;
    private final FirebaseStorage storage;
    private final FirebaseFirestore db;
    private final StorageReference fichasTecnicasRef;
    private static final String CARPETA_STORAGE = "fichas_tecnicas/";
    private static final String COL_FICHAS = "fichasTecnicas";

    public interface ProgressCallback {
        void onProgress(int percent);
    }

    public FichaTecnicaRepository() {
        this.storage = FirebaseStorage.getInstance();
        this.db = FirebaseFirestore.getInstance();
        this.fichasTecnicasRef = storage.getReference().child(CARPETA_STORAGE);
    }

    public static synchronized FichaTecnicaRepository getInstance() {
        if (instance == null) {
            instance = new FichaTecnicaRepository();
        }
        return instance;
    }

    public void cargarFichasTecnicas(Callback<List<FichaTecnica>> callback) {
        // 1. Primero intentar cargar desde Firestore (aprovecha la persistencia offline de 100 MB)
        db.collection(COL_FICHAS).get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot != null && !snapshot.isEmpty()) {
                        List<FichaTecnica> lista = new ArrayList<>();
                        for (QueryDocumentSnapshot doc : snapshot) {
                            FichaTecnica f = doc.toObject(FichaTecnica.class);
                            if (f.getId() == null) f.setId(doc.getId());
                            lista.add(f);
                        }
                        callback.onSuccess(lista);

                        // Si hay conexión, sincronizar en segundo plano con Storage para archivos nuevos
                        sincronizarConStorage(null);
                    } else {
                        // Si Firestore aún no tiene datos (primera sincronización), traer desde Storage
                        sincronizarConStorage(callback);
                    }
                })
                .addOnFailureListener(e -> {
                    // Si falla Firestore, intentar Storage como respaldo
                    sincronizarConStorage(callback);
                });
    }

    private void sincronizarConStorage(Callback<List<FichaTecnica>> callback) {
        fichasTecnicasRef.listAll()
                .addOnSuccessListener(listResult -> {
                    List<FichaTecnica> fichas = Collections.synchronizedList(new ArrayList<>());
                    int totalArchivos = listResult.getItems().size();

                    if (totalArchivos == 0) {
                        if (callback != null) callback.onSuccess(new ArrayList<>());
                        return;
                    }

                    AtomicInteger counter = new AtomicInteger(totalArchivos);

                    for (StorageReference item : listResult.getItems()) {
                        item.getDownloadUrl().addOnSuccessListener(uri -> {
                            item.getMetadata().addOnSuccessListener(metadata -> {
                                FichaTecnica ficha = new FichaTecnica();
                                ficha.setId(item.getName());
                                ficha.setNombreArchivo(item.getName());
                                ficha.setUrlPdf(uri.toString());
                                ficha.setTamanio(metadata.getSizeBytes());

                                long timeCreated = metadata.getCreationTimeMillis();
                                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                                ficha.setFechaSubida(sdf.format(new Date(timeCreated)));

                                // Almacenar en Firestore para disponibilidad offline permanente
                                db.collection(COL_FICHAS).document(item.getName()).set(ficha);

                                fichas.add(ficha);
                                if (counter.decrementAndGet() == 0) {
                                    if (callback != null) callback.onSuccess(new ArrayList<>(fichas));
                                }
                            }).addOnFailureListener(e -> {
                                if (counter.decrementAndGet() == 0) {
                                    if (callback != null) callback.onSuccess(new ArrayList<>(fichas));
                                }
                            });
                        }).addOnFailureListener(e -> {
                            if (counter.decrementAndGet() == 0) {
                                if (callback != null) callback.onSuccess(new ArrayList<>(fichas));
                            }
                        });
                    }
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e);
                });
    }

    public void subirPdf(Uri pdfUri, String nombreFinal, ProgressCallback progressCallback, Callback<Void> callback) {
        StorageReference fileRef = fichasTecnicasRef.child(nombreFinal);
        UploadTask uploadTask = fileRef.putFile(pdfUri);

        if (progressCallback != null) {
            uploadTask.addOnProgressListener(snapshot -> {
                double progress = (100.0 * snapshot.getBytesTransferred()) / snapshot.getTotalByteCount();
                progressCallback.onProgress((int) progress);
            });
        }

        uploadTask.addOnSuccessListener(taskSnapshot -> {
            fileRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                fileRef.getMetadata().addOnSuccessListener(metadata -> {
                    FichaTecnica ficha = new FichaTecnica();
                    ficha.setId(nombreFinal);
                    ficha.setNombreArchivo(nombreFinal);
                    ficha.setUrlPdf(downloadUri.toString());
                    ficha.setTamanio(metadata.getSizeBytes());

                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                    ficha.setFechaSubida(sdf.format(new Date(metadata.getCreationTimeMillis())));

                    // Guardar en Firestore para modo offline
                    db.collection(COL_FICHAS).document(nombreFinal).set(ficha)
                            .addOnSuccessListener(v -> callback.onSuccess(null))
                            .addOnFailureListener(e -> callback.onSuccess(null));
                }).addOnFailureListener(e -> callback.onSuccess(null));
            }).addOnFailureListener(e -> callback.onSuccess(null));
        }).addOnFailureListener(callback::onError);
    }

    public void eliminarPdf(String urlPdf, Callback<Void> callback) {
        try {
            StorageReference fileRef = storage.getReferenceFromUrl(urlPdf);
            String fileName = fileRef.getName();
            fileRef.delete()
                    .addOnSuccessListener(aVoid -> {
                        // Eliminar de Firestore
                        db.collection(COL_FICHAS).document(fileName).delete();
                        callback.onSuccess(null);
                    })
                    .addOnFailureListener(callback::onError);
        } catch (Exception e) {
            callback.onError(e);
        }
    }

    public void descargarPdfAArchivo(String urlPdf, File destFile, Callback<File> callback) {
        try {
            StorageReference fileRef = storage.getReferenceFromUrl(urlPdf);
            fileRef.getFile(destFile)
                    .addOnSuccessListener(taskSnapshot -> callback.onSuccess(destFile))
                    .addOnFailureListener(callback::onError);
        } catch (Exception e) {
            callback.onError(e);
        }
    }
}
