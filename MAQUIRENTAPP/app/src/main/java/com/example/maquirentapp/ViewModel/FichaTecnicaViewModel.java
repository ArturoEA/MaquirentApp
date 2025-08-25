package com.example.maquirentapp.ViewModel;


import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;


import com.example.maquirentapp.Model.FichaTecnica;
import com.example.maquirentapp.adaptadores.PdfManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class FichaTecnicaViewModel extends ViewModel {
    private MutableLiveData<List<FichaTecnica>> fichasLiveData = new MutableLiveData<>();
    private MutableLiveData<String> operacionStatus = new MutableLiveData<>();
    private String fichaIdParaPdf;

    public FichaTecnicaViewModel() {
        cargarDatosIniciales();
    }

    public void cargarDatosIniciales() {
        List<FichaTecnica> fichas = new ArrayList<>();
        fichas.add(new FichaTecnica("1", "Generador Diesel", null, null, 0, null, false));
        fichas.add(new FichaTecnica("2", "Transformador 220V", null, null, 0, null, false));
        fichasLiveData.setValue(fichas);
    }

    public LiveData<List<FichaTecnica>> getFichasLiveData() {
        return fichasLiveData;
    }

    public LiveData<String> getOperacionStatus() {
        return operacionStatus;
    }

    public void prepararParaSubirPdf(String fichaId) {
        this.fichaIdParaPdf = fichaId;
    }

    public void procesarPdfSeleccionado(Context context, Uri pdfUri) {
        if (fichaIdParaPdf == null) {
            operacionStatus.setValue("No se ha seleccionado ninguna ficha");
            return;
        }

        new Thread(() -> {
            try {
                String localPath = PdfManager.guardarPdfLocal(context, pdfUri);
                String nombreArchivo = obtenerNombreArchivo(context, pdfUri);
                long tamanioArchivo = obtenerTamanioArchivo(context, pdfUri);

                List<FichaTecnica> fichasActuales = fichasLiveData.getValue();
                if (fichasActuales != null) {
                    List<FichaTecnica> nuevasFichas = new ArrayList<>();

                    for (FichaTecnica ficha : fichasActuales) {
                        if (ficha.getId().equals(fichaIdParaPdf)) {
                            // Crear nueva ficha con los datos actualizados
                            FichaTecnica fichaActualizada = new FichaTecnica(
                                    ficha.getId(),
                                    ficha.getNombre(),
                                    localPath,
                                    nombreArchivo,
                                    tamanioArchivo,
                                    new Date(),
                                    true
                            );
                            nuevasFichas.add(fichaActualizada);
                        } else {
                            nuevasFichas.add(ficha);
                        }
                    }

                    fichasLiveData.postValue(nuevasFichas);
                    operacionStatus.postValue("PDF guardado correctamente");
                }
            } catch (IOException e) {
                operacionStatus.postValue("Error al guardar PDF: " + e.getMessage());
            }
        }).start();
    }

    private String obtenerNombreArchivo(Context context, Uri uri) {
        String nombreArchivo = "documento.pdf";

        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        nombreArchivo = cursor.getString(nameIndex);
                    }
                }
            } catch (Exception e) {
                operacionStatus.postValue("Error al obtener nombre del archivo");
            }
        }
        return nombreArchivo;
    }

    private long obtenerTamanioArchivo(Context context, Uri uri) {
        long tamanio = 0;

        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                    if (sizeIndex != -1) {
                        tamanio = cursor.getLong(sizeIndex);
                    }
                }
            } catch (Exception e) {
                operacionStatus.postValue("Error al obtener tamaño del archivo");
            }
        }
        return tamanio;
    }

    public void abrirPdf(Context context, FichaTecnica ficha) {
        if (ficha == null || !ficha.tienePdf() || !ficha.isPdfLocal()) {
            operacionStatus.setValue("No hay PDF disponible para abrir");
            return;
        }

        try {
            Uri pdfUri = PdfManager.getPdfUri(context, ficha.getPdfUrl());
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(pdfUri, "application/pdf");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            if (intent.resolveActivity(context.getPackageManager()) != null) {
                context.startActivity(intent);
            } else {
                operacionStatus.setValue("No hay aplicación para ver PDFs");
            }
        } catch (Exception e) {
            operacionStatus.setValue("Error al abrir PDF: " + e.getMessage());
        }
    }
}
