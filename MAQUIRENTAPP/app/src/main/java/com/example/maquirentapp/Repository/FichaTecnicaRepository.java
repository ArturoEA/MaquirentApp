package com.example.maquirentapp.Repository;

import android.net.Uri;

import com.example.maquirentapp.Model.FichaTecnica;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class FichaTecnicaRepository {

    private static FichaTecnicaRepository instance;
    private final FirebaseStorage storage;
    private final StorageReference fichasTecnicasRef;
    private static final String CARPETA_STORAGE = "fichas_tecnicas/";

    public interface ProgressCallback {
        void onProgress(int percent);
    }

    public FichaTecnicaRepository() {
        this.storage = FirebaseStorage.getInstance();
        this.fichasTecnicasRef = storage.getReference().child(CARPETA_STORAGE);
    }

    public static synchronized FichaTecnicaRepository getInstance() {
        if (instance == null) {
            instance = new FichaTecnicaRepository();
        }
        return instance;
    }

    public void cargarFichasTecnicas(Callback<List<FichaTecnica>> callback) {
        fichasTecnicasRef.listAll()
                .addOnSuccessListener(listResult -> {
                    List<FichaTecnica> fichas = new ArrayList<>();
                    int totalArchivos = listResult.getItems().size();

                    if (totalArchivos == 0) {
                        callback.onSuccess(fichas);
                        return;
                    }

                    final int[] procesados = {0};

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

                                fichas.add(ficha);
                                procesados[0]++;

                                if (procesados[0] == totalArchivos) {
                                    callback.onSuccess(fichas);
                                }
                            }).addOnFailureListener(e -> {
                                procesados[0]++;
                                if (procesados[0] == totalArchivos) {
                                    callback.onSuccess(fichas);
                                }
                            });
                        }).addOnFailureListener(e -> {
                            procesados[0]++;
                            if (procesados[0] == totalArchivos) {
                                callback.onSuccess(fichas);
                            }
                        });
                    }
                })
                .addOnFailureListener(callback::onError);
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

        uploadTask.addOnSuccessListener(taskSnapshot -> callback.onSuccess(null))
                .addOnFailureListener(callback::onError);
    }

    public void eliminarPdf(String urlPdf, Callback<Void> callback) {
        try {
            StorageReference fileRef = storage.getReferenceFromUrl(urlPdf);
            fileRef.delete()
                    .addOnSuccessListener(aVoid -> callback.onSuccess(null))
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
