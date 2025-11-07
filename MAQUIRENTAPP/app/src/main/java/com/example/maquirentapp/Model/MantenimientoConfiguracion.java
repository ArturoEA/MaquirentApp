package com.example.maquirentapp.Model;

public class MantenimientoConfiguracion {
    private String id;
    private String nombre;
    private String icono;
    private long fechaCreacion;

    public MantenimientoConfiguracion() {
    }

    public MantenimientoConfiguracion(String nombre, String icono) {
        this.nombre = nombre;
        this.icono = icono;
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
    public long getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(long fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }
}
