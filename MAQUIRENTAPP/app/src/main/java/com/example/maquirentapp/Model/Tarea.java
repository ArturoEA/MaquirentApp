package com.example.maquirentapp.Model;

public class Tarea {
    private String id;
    private String titulo;
    private String descripcion;
    private boolean completada;
    private long fechaCreacion;
    private long fechaCompletada;
    private String creadoPor; // UID del usuario
    private String nombreCreador;
    private String completadaPor; // UID del usuario que la completó
    private String nombreCompletador;

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

    public long getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(long fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public long getFechaCompletada() {
        return fechaCompletada;
    }

    public void setFechaCompletada(long fechaCompletada) {
        this.fechaCompletada = fechaCompletada;
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
}