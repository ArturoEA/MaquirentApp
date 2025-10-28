package com.example.maquirentapp.Model;

public class Accesorio {
    private String id;
    private String nombre;
    private String icono;
    private String tipo; // "mensual" o "diario"
    private long fechaCreacion;

    public Accesorio() {
    }

    public Accesorio(String nombre, String icono, String tipo) {
        this.nombre = nombre;
        this.icono = icono;
        this.tipo = tipo;
        this.fechaCreacion = System.currentTimeMillis();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getIcono() {
        return icono;
    }

    public void setIcono(String icono) {
        this.icono = icono;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public long getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(long fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }
}