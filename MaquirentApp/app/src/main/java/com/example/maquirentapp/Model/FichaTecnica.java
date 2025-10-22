package com.example.maquirentapp.Model;

import java.util.Date;

public class FichaTecnica {
    private String id;
    private String nombre;
    private String pdfUrl;  // URL o path local
    private String pdfNombre;
    private long pdfSize;
    private Date fechaSubida;
    private boolean pdfLocal;

    public FichaTecnica(String id, String nombre, String pdfUrl, String pdfNombre, long pdfSize, Date fechaSubida, boolean pdfLocal) {
        this.id = id;
        this.nombre = nombre;
        this.pdfUrl = pdfUrl;
        this.pdfNombre = pdfNombre;
        this.pdfSize = pdfSize;
        this.fechaSubida = fechaSubida;
        this.pdfLocal = pdfLocal;
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

    public String getPdfUrl() {
        return pdfUrl;
    }

    public void setPdfUrl(String pdfUrl) {
        this.pdfUrl = pdfUrl;
    }

    public String getPdfNombre() {
        return pdfNombre;
    }

    public void setPdfNombre(String pdfNombre) {
        this.pdfNombre = pdfNombre;
    }

    public long getPdfSize() {
        return pdfSize;
    }

    public void setPdfSize(long pdfSize) {
        this.pdfSize = pdfSize;
    }

    public Date getFechaSubida() {
        return fechaSubida;
    }

    public void setFechaSubida(Date fechaSubida) {
        this.fechaSubida = fechaSubida;
    }

    public boolean isPdfLocal() {
        return pdfLocal;
    }

    public void setPdfLocal(boolean pdfLocal) {
        this.pdfLocal = pdfLocal;
    }

    public boolean tienePdf() {
        return pdfUrl != null && !pdfUrl.isEmpty();
    }
}
