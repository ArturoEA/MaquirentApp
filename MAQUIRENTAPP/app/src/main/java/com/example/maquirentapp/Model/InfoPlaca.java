package com.example.maquirentapp.Model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InfoPlaca {
    private String id;
    private String idGrupo;
    private List<String> imagenesUrls;
    private List<Map<String, String>> especificaciones;

    public InfoPlaca() {
        this.imagenesUrls = new ArrayList<>();
        this.especificaciones = new ArrayList<>();
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

    public List<String> getImagenesUrls() {
        return imagenesUrls;
    }

    public void setImagenesUrls(List<String> imagenesUrls) {
        this.imagenesUrls = imagenesUrls;
    }

    public List<Map<String, String>> getEspecificaciones() {
        return especificaciones;
    }

    public void setEspecificaciones(List<Map<String, String>> especificaciones) {
        this.especificaciones = especificaciones;
    }
}