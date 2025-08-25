package com.example.maquirentapp.ViewModel;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.maquirentapp.Model.AlquilerDia;
import com.example.maquirentapp.Repository.AlquilerDiaRepository;

import java.util.Date;

public class AlquilerDiaViewModel extends ViewModel {
    // Datos del formulario como LiveData
    private final MutableLiveData<Integer> idGrupo = new MutableLiveData<>();
    private final MutableLiveData<String> cliente = new MutableLiveData<>();
    private final MutableLiveData<String> ubicacion = new MutableLiveData<>();
    private final MutableLiveData<Double> horometroInicial = new MutableLiveData<>();
    private final MutableLiveData<Double> horometroFinal = new MutableLiveData<>();
    private final MutableLiveData<Date> fechaInicial = new MutableLiveData<>();
    private final MutableLiveData<Date> fechaFinal = new MutableLiveData<>();
    private final MutableLiveData<Double> precioDia = new MutableLiveData<>();

    // Para manejar el estado del formulario
    private final MutableLiveData<Boolean> isFormValid = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> saveSuccess = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();

    private final AlquilerDiaRepository repository;

    public AlquilerDiaViewModel() {
        repository = new AlquilerDiaRepository();
        isFormValid.setValue(false);
        isLoading.setValue(false);
    }

    // Getters para los LiveData
    public MutableLiveData<Integer> getIdGrupo() { return idGrupo; }
    public MutableLiveData<String> getCliente() { return cliente; }
    public MutableLiveData<String> getUbicacion() { return ubicacion; }
    public MutableLiveData<Double> getHorometroInicial() { return horometroInicial; }
    public MutableLiveData<Double> getHorometroFinal() { return horometroFinal; }
    public MutableLiveData<Date> getFechaInicial() { return fechaInicial; }
    public MutableLiveData<Date> getFechaFinal() { return fechaFinal; }
    public MutableLiveData<Double> getPrecioDia() { return precioDia; }
    public MutableLiveData<Boolean> getIsFormValid() { return isFormValid; }
    public MutableLiveData<String> getErrorMessage() { return errorMessage; }
    public MutableLiveData<Boolean> getSaveSuccess() { return saveSuccess; }
    public MutableLiveData<Boolean> getIsLoading() { return isLoading; }

    // Método para validar el formulario
    public void validateForm() {
        boolean isValid = true;
        StringBuilder errorMsg = new StringBuilder();

        if (cliente.getValue() == null || cliente.getValue().isEmpty()) {
            isValid = false;
            errorMsg.append("El cliente es requerido\n");
        }

        if (ubicacion.getValue() == null || ubicacion.getValue().isEmpty()) {
            isValid = false;
            errorMsg.append("La ubicación es requerida\n");
        }

        if (horometroInicial.getValue() == null) {
            isValid = false;
            errorMsg.append("El horómetro inicial es requerido\n");
        }

        if (fechaInicial.getValue() == null) {
            isValid = false;
            errorMsg.append("La fecha inicial es requerida\n");
        }

        if (precioDia.getValue() == null || precioDia.getValue() <= 0) {
            isValid = false;
            errorMsg.append("El precio por día debe ser mayor a 0\n");
        }

        errorMessage.setValue(errorMsg.toString());
        isFormValid.setValue(isValid);
    }

    // Método para guardar el alquiler
    public void saveAlquiler() {
        validateForm();

        if (Boolean.TRUE.equals(isFormValid.getValue())) {
            isLoading.setValue(true);

            AlquilerDia alquiler = createAlquilerFromFormData();

            repository.guardarAlquiler(alquiler, new AlquilerDiaRepository.AlquilerCallback() {
                @Override
                public void onSuccess(AlquilerDia alquiler) {
                    isLoading.postValue(false);
                    saveSuccess.postValue(true);
                }

                @Override
                public void onFailure(Throwable t) {
                    isLoading.postValue(false);
                    errorMessage.postValue("Error al guardar: " + t.getMessage());
                }
            });
        }
    }

    private AlquilerDia createAlquilerFromFormData() {
        return new AlquilerDia(
                0, // ID se generará en el servidor
                idGrupo.getValue(),
                cliente.getValue(),
                ubicacion.getValue(),
                horometroInicial.getValue(),
                horometroFinal.getValue(),
                fechaInicial.getValue(),
                fechaFinal.getValue(),
                precioDia.getValue(),
                false, // varillaTierra
                false, // cableElectrico
                false, // tableroDistribucion
                false, // extensionCaja
                false, // bidonCombustible
                false, // llaveHexagonal
                false, // llavesPuertas
                false, // tacoMadera
                false, // manguera
                false, // embudo
                false, // fajasSogas
                false, // pinPerno
                ""     // comentarios
        );
    }
}