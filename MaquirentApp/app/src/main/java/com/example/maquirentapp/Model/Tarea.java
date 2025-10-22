package com.example.maquirentapp.Model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.PropertyName;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Tarea {
    private String id;
    private String titulo;
    private String descripcion;
    private boolean completada;
    private Date fechaCreacion;
    private Date fechaCompletada;
    private String creadoPor; // UID del usuario
    private String nombreCreador;
    private String completadaPor; // UID del usuario que la completó
    private String nombreCompletador;
    private List<ResponsableAsignado> responsables = new ArrayList<>();

    public Tarea() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public boolean isCompletada() {
        return completada;
    }

    public void setCompletada(boolean completada) {
        this.completada = completada;
    }

    public Date getFechaCreacion() {
        return fechaCreacion;
    }

    @Exclude
    public void setFechaCreacion(Date fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    @PropertyName("fechaCreacion")
    public void setFechaCreacionRaw(Object fechaCreacion) {
        this.fechaCreacion = convertToDate(fechaCreacion);
    }

    public Date getFechaCompletada() {
        return fechaCompletada;
    }

    @Exclude
    public void setFechaCompletada(Date fechaCompletada) {
        this.fechaCompletada = fechaCompletada;
    }

    @PropertyName("fechaCompletada")
    public void setFechaCompletadaRaw(Object fechaCompletada) {
        this.fechaCompletada = convertToDate(fechaCompletada);
    }

    public long getFechaCreacionEpoch() {
        return fechaCreacion != null ? fechaCreacion.getTime() : 0L;
    }

    public long getFechaCompletadaEpoch() {
        return fechaCompletada != null ? fechaCompletada.getTime() : 0L;
    }

    public String getCreadoPor() {
        return creadoPor;
    }

    public void setCreadoPor(String creadoPor) {
        this.creadoPor = creadoPor;
    }

    public String getNombreCreador() {
        return nombreCreador;
    }

    public void setNombreCreador(String nombreCreador) {
        this.nombreCreador = nombreCreador;
    }

    public String getCompletadaPor() {
        return completadaPor;
    }

    public void setCompletadaPor(String completadaPor) {
        this.completadaPor = completadaPor;
    }

    public String getNombreCompletador() {
        return nombreCompletador;
    }

    public void setNombreCompletador(String nombreCompletador) {
        this.nombreCompletador = nombreCompletador;
    }

    public List<ResponsableAsignado> getResponsables() {
        if (responsables == null) {
            responsables = new ArrayList<>();
        }
        return responsables;
    }

    public void setResponsables(List<ResponsableAsignado> responsables) {
        if (responsables == null) {
            this.responsables = new ArrayList<>();
        } else {
            this.responsables = responsables;
        }
    }

    private Date convertToDate(Object value) {
        if (value instanceof Date) {
            return (Date) value;
        }
        if (value instanceof Timestamp) {
            return ((Timestamp) value).toDate();
        }
        if (value instanceof Long) {
            return new Date((Long) value);
        }
        return null;
    }
}
