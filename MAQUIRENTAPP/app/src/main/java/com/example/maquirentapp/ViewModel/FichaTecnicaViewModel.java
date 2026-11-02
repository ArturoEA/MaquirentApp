package com.example.maquirentapp.ViewModel;

import android.net.Uri;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.maquirentapp.Model.FichaTecnica;
import com.example.maquirentapp.Repository.Callback;
import com.example.maquirentapp.Repository.FichaTecnicaRepository;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FichaTecnicaViewModel extends ViewModel {

    private final FichaTecnicaRepository repository;
    private final MutableLiveData<List<FichaTecnica>> fichasLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> operacionStatus = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    public FichaTecnicaViewModel() {
        this(FichaTecnicaRepository.getInstance());
    }

    public FichaTecnicaViewModel(FichaTecnicaRepository repository) {
        this.repository = repository;
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

    public void cargarFichasTecnicas() {
        isLoading.setValue(true);
        repository.cargarFichasTecnicas(new Callback<List<FichaTecnica>>() {
            @Override
            public void onSuccess(List<FichaTecnica> result) {
                fichasLiveData.setValue(result != null ? result : new ArrayList<>());
                isLoading.setValue(false);
            }

            @Override
            public void onError(Exception e) {
                operacionStatus.setValue("Error al cargar fichas: " + (e != null ? e.getMessage() : "Desconocido"));
                isLoading.setValue(false);
            }
        });
    }

    public void subirPdf(Uri pdfUri, String nombrePersonalizado) {
        if (pdfUri == null || nombrePersonalizado == null || nombrePersonalizado.trim().isEmpty()) {
            operacionStatus.setValue("Error: archivo o nombre no válido");
            return;
        }

        isLoading.setValue(true);
        String nombreFinal = nombrePersonalizado.trim();
        if (!nombreFinal.toLowerCase().endsWith(".pdf")) {
            nombreFinal = nombreFinal + ".pdf";
        }

        repository.subirPdf(pdfUri, nombreFinal,
                progress -> operacionStatus.setValue("Subiendo: " + progress + "%"),
                new Callback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        operacionStatus.setValue("Archivo subido correctamente");
                        isLoading.setValue(false);
                        cargarFichasTecnicas();
                    }

                    @Override
                    public void onError(Exception e) {
                        operacionStatus.setValue("Error al subir: " + (e != null ? e.getMessage() : "Desconocido"));
                        isLoading.setValue(false);
                    }
                });
    }

    public void actualizarNombrePdf(FichaTecnica ficha, String nuevoNombre) {
        if (ficha == null || nuevoNombre == null || nuevoNombre.trim().isEmpty()) {
            operacionStatus.setValue("El nombre no puede estar vacío");
            return;
        }

        isLoading.setValue(true);
        repository.actualizarNombrePdf(ficha, nuevoNombre, new Callback<Void>() {
            @Override
            public void onSuccess(Void result) {
                operacionStatus.setValue("Nombre actualizado correctamente");
                isLoading.setValue(false);
                cargarFichasTecnicas();
            }

            @Override
            public void onError(Exception e) {
                operacionStatus.setValue("Error al actualizar nombre: " + (e != null ? e.getMessage() : "Desconocido"));
                isLoading.setValue(false);
            }
        });
    }

    public void eliminarPdf(FichaTecnica ficha) {
        if (ficha == null || ficha.getUrlPdf() == null) return;
        isLoading.setValue(true);

        repository.eliminarPdf(ficha, new Callback<Void>() {
            @Override
            public void onSuccess(Void result) {
                operacionStatus.setValue("Archivo eliminado");
                isLoading.setValue(false);
                cargarFichasTecnicas();
            }

            @Override
            public void onError(Exception e) {
                operacionStatus.setValue("Error al eliminar: " + (e != null ? e.getMessage() : "Desconocido"));
                isLoading.setValue(false);
            }
        });
    }

    public void descargarPdfAArchivo(String urlPdf, File destFile, Callback<File> callback) {
        isLoading.setValue(true);
        operacionStatus.setValue("Descargando...");

        repository.descargarPdfAArchivo(urlPdf, destFile, new Callback<File>() {
            @Override
            public void onSuccess(File file) {
                isLoading.setValue(false);
                if (callback != null) {
                    callback.onSuccess(file);
                }
            }

            @Override
            public void onError(Exception e) {
                isLoading.setValue(false);
                operacionStatus.setValue("Error al descargar: " + (e != null ? e.getMessage() : "Desconocido"));
                if (callback != null) {
                    callback.onError(e);
                }
            }
        });
    }
}