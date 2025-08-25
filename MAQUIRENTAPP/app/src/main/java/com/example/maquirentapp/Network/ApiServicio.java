package com.example.maquirentapp.Network;

import com.example.maquirentapp.Model.AlquilerDia;
import com.example.maquirentapp.Model.AlquilerMensual;
import com.example.maquirentapp.Model.GrupoElectrogeno;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface ApiServicio {
    @GET("GruposElectrogenos")
    Call<List<GrupoElectrogeno>>GetGruposElectrogenos();
    @POST("GruposElectrogenos")
    Call<GrupoElectrogeno>PostGrupoElectrogeno(@Body GrupoElectrogeno grupoElectrogeno);

    @POST("AlquileresDia")
    Call<AlquilerDia> crearAlquilerDia(@Body AlquilerDia alquilerDia);

    @GET("AlquileresMensuales")
    Call<List<AlquilerMensual>>GetAlquileresMensuales();
    @POST("AlquileresMensuales")
    Call<AlquilerMensual> crearAlquilerMensual(@Body AlquilerMensual alquilerMensual);

}
