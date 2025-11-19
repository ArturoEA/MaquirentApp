package com.example.maquirentapp.Model;

public class Plano {
    private String id;
    private String urlImagen;
    private String nombreArchivo;

    public Plano() {
        // Constructor vacío requerido por Firebase
    }

    public Plano(String id, String urlImagen, String nombreArchivo) {
        this.id = id;
        this.urlImagen = urlImagen;
        this.nombreArchivo = nombreArchivo;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUrlImagen() { return urlImagen; }
    public void setUrlImagen(String urlImagen) { this.urlImagen = urlImagen; }
    public String getNombreArchivo() { return nombreArchivo; }
    public void setNombreArchivo(String nombreArchivo) { this.nombreArchivo = nombreArchivo; }
}