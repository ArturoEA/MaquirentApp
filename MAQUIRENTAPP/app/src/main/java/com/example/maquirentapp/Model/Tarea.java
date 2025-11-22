package com.example.maquirentapp.Model;

import java.util.ArrayList;
import java.util.List;

public class Tarea {
    private String id;
    private String titulo;
    private String fechaCreacion;
    private boolean completada;
    private List<String> participantesIds;

    public Tarea() {
        this.participantesIds = new ArrayList<>();
    }

    public Tarea(String id, String titulo, String fechaCreacion, boolean completada) {
        this.id = id;
        this.titulo = titulo;
        this.fechaCreacion = fechaCreacion;
        this.completada = completada;
        this.participantesIds = new ArrayList<>();
    }

    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(String fechaCreacion) { this.fechaCreacion = fechaCreacion; }
    public boolean isCompletada() { return completada; }
    public void setCompletada(boolean completada) { this.completada = completada; }
    public List<String> getParticipantesIds() { return participantesIds; }
    public void setParticipantesIds(List<String> participantesIds) { this.participantesIds = participantesIds; }
}