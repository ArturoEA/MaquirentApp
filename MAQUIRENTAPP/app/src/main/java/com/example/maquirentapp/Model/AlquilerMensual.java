package com.example.maquirentapp.Model;

import java.util.ArrayList;
import java.util.List;

public class AlquilerMensual {
    private String id;
    private String idGrupo;
    private String nombreCliente;
    private double horometroInicial;
    private String fechaInicial;
    private String ubicacion;
    private double horometroFinal;
    private String fechaFinal;
    private double precioAlquiler;
    private int horasMinimas;
    private double precioHoraExtra;
    private List<String> accesoriosIds;

    public AlquilerMensual() {
        this.accesoriosIds = new ArrayList<>();
    }
    public int getHorasMinimas() {
        return horasMinimas;
    }

    public void setHorasMinimas(int horasMinimas) {
        this.horasMinimas = horasMinimas;
    }

    public double getPrecioAlquiler() {
        return precioAlquiler;
    }

    public void setPrecioAlquiler(double precioAlquiler) {
        this.precioAlquiler = precioAlquiler;
    }

    public String getFechaFinal() {
        return fechaFinal;
    }

    public void setFechaFinal(String fechaFinal) {
        this.fechaFinal = fechaFinal;
    }

    public double getHorometroFinal() {
        return horometroFinal;
    }

    public void setHorometroFinal(double horometroFinal) {
        this.horometroFinal = horometroFinal;
    }

    public String getUbicacion() {
        return ubicacion;
    }

    public void setUbicacion(String ubicacion) {
        this.ubicacion = ubicacion;
    }

    public String getFechaInicial() {
        return fechaInicial;
    }

    public void setFechaInicial(String fechaInicial) {
        this.fechaInicial = fechaInicial;
    }

    public String getNombreCliente() {
        return nombreCliente;
    }

    public void setNombreCliente(String empresa) {
        this.nombreCliente = empresa;
    }

    public String getIdGrupo() {
        return idGrupo;
    }

    public void setIdGrupo(String idGrupo) {
        this.idGrupo = idGrupo;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public double getHorometroInicial() {
        return horometroInicial;
    }

    public void setHorometroInicial(double horometroInicial) {
        this.horometroInicial = horometroInicial;
    }
    public double getPrecioHoraExtra() {
        return precioHoraExtra;
    }

    public void setPrecioHoraExtra(double precioHoraExtra) {
        this.precioHoraExtra = precioHoraExtra;
    }
    public List<String> getAccesoriosIds() {
        return accesoriosIds;
    }

    public void setAccesoriosIds(List<String> accesoriosIds) {
        this.accesoriosIds = accesoriosIds;
    }
}
