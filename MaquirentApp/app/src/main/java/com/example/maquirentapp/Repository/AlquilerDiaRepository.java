package com.example.maquirentapp.Repository;

import com.example.maquirentapp.Model.AlquilerDia;
import com.example.maquirentapp.Network.ApiServicio;
import com.example.maquirentapp.Network.RetrofitCliente;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AlquilerDiaRepository {
    private ApiServicio apiServicio;

    public AlquilerDiaRepository() {
        apiServicio = RetrofitCliente.getCliente().create(ApiServicio.class);
    }

    public interface AlquilerCallback {
        void onSuccess(AlquilerDia alquiler);
        void onFailure(Throwable t);
    }

    public void guardarAlquiler(AlquilerDia alquiler, AlquilerCallback callback) {
        Call<AlquilerDia> call = apiServicio.crearAlquilerDia(alquiler);
        call.enqueue(new Callback<AlquilerDia>() {
            @Override
            public void onResponse(Call<AlquilerDia> call, Response<AlquilerDia> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onFailure(new Exception("Error en la respuesta: " + response.code()));
                }
            }

            @Override
            public void onFailure(Call<AlquilerDia> call, Throwable t) {
                callback.onFailure(t);
            }
        });
    }
}