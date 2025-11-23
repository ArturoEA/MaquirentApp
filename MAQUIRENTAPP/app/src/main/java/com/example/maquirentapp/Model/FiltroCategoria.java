package com.example.maquirentapp.Model;

import java.util.ArrayList;
import java.util.List;

public class FiltroCategoria {
    private String id;
    private String idGrupo;
    private String nombreCategoria;
    private List<FiltroItem> items;

    public FiltroCategoria() {
        this.items = new ArrayList<>();
    }

    public FiltroCategoria(String id, String idGrupo, String nombreCategoria) {
        this.id = id;
        this.idGrupo = idGrupo;
        this.nombreCategoria = nombreCategoria;
        this.items = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getIdGrupo() {
        return idGrupo;
    }

    public void setIdGrupo(String idGrupo) {
        this.idGrupo = idGrupo;
    }

    public String getNombreCategoria() {
        return nombreCategoria;
    }

    public void setNombreCategoria(String nombreCategoria) {
        this.nombreCategoria = nombreCategoria;
    }

    public List<FiltroItem> getItems() {
        return items;
    }

    public void setItems(List<FiltroItem> items) {
        this.items = items;
    }
}