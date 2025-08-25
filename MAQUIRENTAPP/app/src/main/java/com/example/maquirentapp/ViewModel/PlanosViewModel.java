package com.example.maquirentapp.ViewModel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.maquirentapp.Model.Plano;

import java.util.ArrayList;
import java.util.List;

public class PlanosViewModel extends ViewModel {
    private final MutableLiveData<List<Plano>> planos = new MutableLiveData<>();

    public PlanosViewModel() {
        // Datos iniciales
        List<Plano> listaInicial = new ArrayList<>();
        listaInicial.add(new Plano("1", "drawable/item1_planos_cambio_voltaje", "Plano 1"));
        listaInicial.add(new Plano("2", "drawable/item1_planos_cambio_voltaje", "Plano 2"));
        planos.setValue(listaInicial);
    }

    public LiveData<List<Plano>> getPlanos() {
        return planos;
    }

    public void addPlano(Plano nuevoPlano) {
        List<Plano> currentList = planos.getValue();
        if (currentList == null) {
            currentList = new ArrayList<>();
        }
        currentList.add(nuevoPlano);
        planos.setValue(currentList);
    }

    public void removePlano(Plano plano) {
        List<Plano> currentList = planos.getValue();
        if (currentList != null) {
            currentList.remove(plano);
            planos.setValue(currentList);
        }
    }
}