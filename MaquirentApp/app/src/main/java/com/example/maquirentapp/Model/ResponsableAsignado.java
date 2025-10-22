package com.example.maquirentapp.Model;

public class ResponsableAsignado {
    private String uid;
    private String nombre;
    private String fotoUrl;

    public ResponsableAsignado() {
    }

    public ResponsableAsignado(String uid, String nombre, String fotoUrl) {
        this.uid = uid;
        this.nombre = nombre;
        this.fotoUrl = fotoUrl;
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

    public String getFotoUrl() {
        return fotoUrl;
    }

    public void setFotoUrl(String fotoUrl) {
        this.fotoUrl = fotoUrl;
    }
}
