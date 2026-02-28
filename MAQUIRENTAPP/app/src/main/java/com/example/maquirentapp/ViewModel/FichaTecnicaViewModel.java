package com.example.maquirentapp.ViewModel;

import android.app.Application;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.webkit.MimeTypeMap;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.maquirentapp.Model.FichaTecnica;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class FichaTecnicaViewModel extends AndroidViewModel {
    private MutableLiveData<List<FichaTecnica>> fichasLiveData = new MutableLiveData<>();
    private MutableLiveData<String> operacionStatus = new MutableLiveData<>();
    private MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    private FirebaseStorage storage;
    private StorageReference fichasTecnicasRef;
    private static final String CARPETA_STORAGE = "fichas_tecnicas/";

    public FichaTecnicaViewModel(@NonNull Application application) {
        super(application);
        storage = FirebaseStorage.getInstance();
        fichasTecnicasRef = storage.getReference().child(CARPETA_STORAGE);
    }

    public LiveData<List<FichaTecnica>> getFichasLiveData() {
        return fichasLiveData;
    }

    public LiveData<String> getOperacionStatus() {
        return operacionStatus;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    // Cargar todas las fichas desde Firebase Storage
    public void cargarFichasTecnicas() {
        isLoading.setValue(true);

        fichasTecnicasRef.listAll()
                .addOnSuccessListener(listResult -> {
                    List<FichaTecnica> fichas = new ArrayList<>();
                    int totalArchivos = listResult.getItems().size();

                    if (totalArchivos == 0) {
                        fichasLiveData.setValue(new ArrayList<>());
                        isLoading.setValue(false);
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

                                // Formatear fecha
                                long timeCreated = metadata.getCreationTimeMillis();
                                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                                ficha.setFechaSubida(sdf.format(new Date(timeCreated)));

                                fichas.add(ficha);
                                procesados[0]++;

                                if (procesados[0] == totalArchivos) {
                                    fichasLiveData.setValue(fichas);
                                    isLoading.setValue(false);
                                }
                            });
                        });
                    }
                })
                .addOnFailureListener(e -> {
                    operacionStatus.setValue("Error al cargar fichas: " + e.getMessage());
                    isLoading.setValue(false);
                });
    }

    // Subir un nuevo PDF
    public void subirPdf(Context context, Uri pdfUri, String nombrePersonalizado) {
        if (pdfUri == null || nombrePersonalizado == null || nombrePersonalizado.trim().isEmpty()) {
            operacionStatus.setValue("Error: archivo o nombre no válido");
            return;
        }

        isLoading.setValue(true);

        try {
            // Nos aseguramos de que el nombre personalizado termine en .pdf
            String nombreFinal = nombrePersonalizado.trim();
            if (!nombreFinal.toLowerCase().endsWith(".pdf")) {
                nombreFinal = nombreFinal + ".pdf";
            }

            String nombreUnico = nombreFinal;

            StorageReference fileRef = fichasTecnicasRef.child(nombreUnico);

            // Subir archivo
            UploadTask uploadTask = fileRef.putFile(pdfUri);

            uploadTask.addOnProgressListener(snapshot -> {
                double progress = (100.0 * snapshot.getBytesTransferred()) / snapshot.getTotalByteCount();
                operacionStatus.setValue("Subiendo: " + (int) progress + "%");
            }).addOnSuccessListener(taskSnapshot -> {
                operacionStatus.setValue("Archivo subido correctamente");
                isLoading.setValue(false);
                cargarFichasTecnicas(); // Recargar lista
            }).addOnFailureListener(e -> {
                operacionStatus.setValue("Error al subir: " + e.getMessage());
                isLoading.setValue(false);
            });

        } catch (Exception e) {
            operacionStatus.setValue("Error: " + e.getMessage());
            isLoading.setValue(false);
        }
    }
    // Abrir PDF en visor integrado
    public void abrirPdf(Context context, FichaTecnica ficha) {
        Intent intent = com.example.maquirentapp.View.PdfViewerActivity.newIntent(
                context,
                ficha.getUrlPdf(),
                ficha.getNombreArchivo()
        );
        context.startActivity(intent);
    }

    // Compartir PDF
    public void compartirPdf(Context context, FichaTecnica ficha) {
        isLoading.setValue(true);

        // Descargar el archivo temporalmente
        descargarPdfTemporal(context, ficha, file -> {
            Uri fileUri = FileProvider.getUriForFile(context,
                    context.getPackageName() + ".provider", file);

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("application/pdf");
            shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, ficha.getNombreArchivo());
            shareIntent.putExtra(Intent.EXTRA_TEXT, "Te comparto esta ficha técnica");
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            Intent chooser = Intent.createChooser(shareIntent, "Compartir PDF");
            chooser.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            context.startActivity(chooser);
            isLoading.setValue(false);
            operacionStatus.setValue("Compartiendo archivo...");
        });
    }

    // Descargar PDF a la carpeta de Descargas
    public void descargarPdf(Context context, FichaTecnica ficha) {
        isLoading.setValue(true);
        operacionStatus.setValue("Descargando...");

        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File destFile = new File(downloadsDir, ficha.getNombreArchivo());

        StorageReference fileRef = storage.getReferenceFromUrl(ficha.getUrlPdf());

        fileRef.getFile(destFile)
                .addOnSuccessListener(taskSnapshot -> {
                    operacionStatus.setValue("Descargado en: " + destFile.getAbsolutePath());
                    isLoading.setValue(false);
                })
                .addOnFailureListener(e -> {
                    operacionStatus.setValue("Error al descargar: " + e.getMessage());
                    isLoading.setValue(false);
                });
    }

    // Eliminar PDF
    public void eliminarPdf(FichaTecnica ficha) {
        isLoading.setValue(true);

        StorageReference fileRef = storage.getReferenceFromUrl(ficha.getUrlPdf());

        fileRef.delete()
                .addOnSuccessListener(aVoid -> {
                    operacionStatus.setValue("Archivo eliminado");
                    isLoading.setValue(false);
                    cargarFichasTecnicas(); // Recargar lista
                })
                .addOnFailureListener(e -> {
                    operacionStatus.setValue("Error al eliminar: " + e.getMessage());
                    isLoading.setValue(false);
                });
    }

    // Mét0do auxiliar para descargar PDF temporal
    private void descargarPdfTemporal(Context context, FichaTecnica ficha, OnFileDownloadedListener listener) {
        try {
            File tempFile = new File(context.getCacheDir(), ficha.getNombreArchivo());
            StorageReference fileRef = storage.getReferenceFromUrl(ficha.getUrlPdf());

            fileRef.getFile(tempFile)
                    .addOnSuccessListener(taskSnapshot -> listener.onFileDownloaded(tempFile))
                    .addOnFailureListener(e -> {
                        operacionStatus.setValue("Error al preparar archivo: " + e.getMessage());
                        isLoading.setValue(false);
                    });
        } catch (Exception e) {
            operacionStatus.setValue("Error: " + e.getMessage());
            isLoading.setValue(false);
        }
    }
    // Interface para callback de descarga
    private interface OnFileDownloadedListener {
        void onFileDownloaded(File file);
    }
}