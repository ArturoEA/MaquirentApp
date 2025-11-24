package com.example.maquirentapp.Model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class InfoPlaca {
    private String id;
    private String idGrupo;
    private List<String> imagenesUrls;
    private List<Map<String, String>> especificaciones;

    private String marcaGrupo = "";
    private String modeloGrupo = "";
    private String serieGrupo = "";
    private String marcaMotor = "";
    private String modeloMotor = "";
    private String serieMotor = "";
    private String marcaGenerador = "";
    private String modeloGenerador = "";
    private String serieGenerador = "";

    private String potenciaStandBy = "";
    private String potenciaContinua= "";

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

    public String getMarcaGrupo() {
        return marcaGrupo;
    }

    public void setMarcaGrupo(String marcaGrupo) {
        this.marcaGrupo = marcaGrupo;
    }

    public String getModeloGrupo() {
        return modeloGrupo;
    }

    public void setModeloGrupo(String modeloGrupo) {
        this.modeloGrupo = modeloGrupo;
    }

    public String getSerieGrupo() {
        return serieGrupo;
    }

    public void setSerieGrupo(String serieGrupo) {
        this.serieGrupo = serieGrupo;
    }

    public String getMarcaMotor() {
        return marcaMotor;
    }

    public void setMarcaMotor(String marcaMotor) {
        this.marcaMotor = marcaMotor;
    }

    public String getModeloMotor() {
        return modeloMotor;
    }

    public void setModeloMotor(String modeloMotor) {
        this.modeloMotor = modeloMotor;
    }

    public String getSerieMotor() {
        return serieMotor;
    }

    public void setSerieMotor(String serieMotor) {
        this.serieMotor = serieMotor;
    }

    public String getMarcaGenerador() {
        return marcaGenerador;
    }

    public void setMarcaGenerador(String marcaGenerador) {
        this.marcaGenerador = marcaGenerador;
    }

    public String getModeloGenerador() {
        return modeloGenerador;
    }

    public void setModeloGenerador(String modeloGenerador) {
        this.modeloGenerador = modeloGenerador;
    }

    public String getSerieGenerador() {
        return serieGenerador;
    }

    public void setSerieGenerador(String serieGenerador) {
        this.serieGenerador = serieGenerador;
    }

    public String getPotenciaStandBy() {
        return potenciaStandBy;
    }

    public void setPotenciaStandBy(String potenciaStandBy) {
        this.potenciaStandBy = potenciaStandBy;
    }

    public String getPotenciaContinua() {
        return potenciaContinua;
    }

    public void setPotenciaContinua(String PotenciaContinua) {
        this.potenciaContinua = potenciaContinua;
    }
}