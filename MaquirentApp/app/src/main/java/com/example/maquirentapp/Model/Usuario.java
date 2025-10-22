package com.example.maquirentapp.Model;

public class Usuario {
    private String uid;
    private String nombre;
    private String email;
    private String rol; // "admin" o "empleado"
    private String estado; // "pendiente", "activo", "inactivo"
    private long fechaCreacion;
    private long fechaUltimaActividad;
    private String creadoPor; // UID del admin que aprobó
    private String fotoUrl;

    public Usuario() {
    }

    public Usuario(String uid, String nombre, String email) {
        this.uid = uid;
        this.nombre = nombre;
        this.email = email;
        this.rol = "empleado";
        this.estado = "pendiente";
        this.fechaCreacion = System.currentTimeMillis();
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public long getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(long fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public long getFechaUltimaActividad() {
        return fechaUltimaActividad;
    }

    public void setFechaUltimaActividad(long fechaUltimaActividad) {
        this.fechaUltimaActividad = fechaUltimaActividad;
    }

    public String getCreadoPor() {
        return creadoPor;
    }

    public void setCreadoPor(String creadoPor) {
        this.creadoPor = creadoPor;
    }

    public String getFotoUrl() {
        return fotoUrl;
    }

    public void setFotoUrl(String fotoUrl) {
        this.fotoUrl = fotoUrl;
    }
}
