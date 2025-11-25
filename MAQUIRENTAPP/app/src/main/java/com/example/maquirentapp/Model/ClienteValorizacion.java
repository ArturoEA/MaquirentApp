package com.example.maquirentapp.Model;

import java.io.Serializable;

public class ClienteValorizacion implements Serializable {
    private String id;
    private String nombreEmpresa;
    private String ruc;
    private String direccion;
    private String ubicacionTrabajo;
    private int anio;
    private long timestampCreacion;

    public ClienteValorizacion() {
    }

    public ClienteValorizacion(String nombreEmpresa, String ruc, String direccion, String ubicacionTrabajo, int anio) {
        this.nombreEmpresa = nombreEmpresa;
        this.ruc = ruc;
        this.direccion = direccion;
        this.ubicacionTrabajo = ubicacionTrabajo;
        this.anio = anio;
        this.timestampCreacion = System.currentTimeMillis();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNombreEmpresa() {
        return nombreEmpresa;
    }

    public void setNombreEmpresa(String nombreEmpresa) {
        this.nombreEmpresa = nombreEmpresa;
    }

    public String getRuc() {
        return ruc;
    }

    public void setRuc(String ruc) {
        this.ruc = ruc;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getUbicacionTrabajo() {
        return ubicacionTrabajo;
    }

    public void setUbicacionTrabajo(String ubicacionTrabajo) {
        this.ubicacionTrabajo = ubicacionTrabajo;
    }

    public int getAnio() {
        return anio;
    }

    public void setAnio(int anio) {
        this.anio = anio;
    }

    public long getTimestampCreacion() {
        return timestampCreacion;
    }

    public void setTimestampCreacion(long timestampCreacion) {
        this.timestampCreacion = timestampCreacion;
    }
}