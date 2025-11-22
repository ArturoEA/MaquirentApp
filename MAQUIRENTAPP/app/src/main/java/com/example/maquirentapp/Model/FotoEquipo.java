package com.example.maquirentapp.Model;

public class FotoEquipo {
    private String id;
    private String urlImagen;
    private String nombreArchivo;
    private String idGrupo;

    public FotoEquipo() {
    }

    public FotoEquipo(String id, String urlImagen, String nombreArchivo, String idGrupo) {
        this.id = id;
        this.urlImagen = urlImagen;
        this.nombreArchivo = nombreArchivo;
        this.idGrupo = idGrupo;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUrlImagen() {
        return urlImagen;
    }

    public void setUrlImagen(String urlImagen) {
        this.urlImagen = urlImagen;
    }

    public String getNombreArchivo() {
        return nombreArchivo;
    }

    public void setNombreArchivo(String nombreArchivo) {
        this.nombreArchivo = nombreArchivo;
    }

    public String getIdGrupo() {
        return idGrupo;
    }

    public void setIdGrupo(String idGrupo) {
        this.idGrupo = idGrupo;
    }
}