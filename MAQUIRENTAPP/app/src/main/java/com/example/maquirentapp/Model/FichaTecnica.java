package com.example.maquirentapp.Model;

public class FichaTecnica {
    private String id;
    private String nombreArchivo;
    private String urlPdf;
    private String fechaSubida;
    private long tamanio; // en bytes

    public FichaTecnica() {
        // Constructor vacío requerido por Firebase
    }

    public FichaTecnica(String id, String nombreArchivo, String urlPdf, String fechaSubida, long tamanio) {
        this.id = id;
        this.nombreArchivo = nombreArchivo;
        this.urlPdf = urlPdf;
        this.fechaSubida = fechaSubida;
        this.tamanio = tamanio;
    }

    // Getters y Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNombreArchivo() {
        return nombreArchivo;
    }

    public void setNombreArchivo(String nombreArchivo) {
        this.nombreArchivo = nombreArchivo;
    }

    public String getUrlPdf() {
        return urlPdf;
    }

    public void setUrlPdf(String urlPdf) {
        this.urlPdf = urlPdf;
    }

    public String getFechaSubida() {
        return fechaSubida;
    }

    public void setFechaSubida(String fechaSubida) {
        this.fechaSubida = fechaSubida;
    }

    public long getTamanio() {
        return tamanio;
    }

    public void setTamanio(long tamanio) {
        this.tamanio = tamanio;
    }

    public boolean tienePdf() {
        return urlPdf != null && !urlPdf.isEmpty();
    }

    // Método para obtener el tamaño formateado
    public String getTamanioFormateado() {
        if (tamanio < 1024) return tamanio + " B";
        if (tamanio < 1024 * 1024) return String.format("%.2f KB", tamanio / 1024.0);
        return String.format("%.2f MB", tamanio / (1024.0 * 1024.0));
    }
}