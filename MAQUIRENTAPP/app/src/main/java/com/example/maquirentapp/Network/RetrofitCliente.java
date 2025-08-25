package com.example.maquirentapp.Network;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitCliente {
    private static Retrofit retrofit = null;
    private static final String URL = "http://maquirent234.somee.com/api/";

    public static Retrofit getCliente(){
        if(retrofit == null){
            retrofit = new Retrofit.Builder().baseUrl(URL).addConverterFactory(GsonConverterFactory.create()).build();
        }
        return retrofit;
    }
}
