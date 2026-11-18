package com.example.maquirentapp.Model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Tarea {
    private String id;
    private String titulo;
    private String fechaCreacion;
    private boolean completada;
    private List<String> participantesIds;

    // Campos de seguimiento operativo y mantenimiento
    private String tipoTarea = "GENERAL"; // "PREVENTIVO", "RETORNO", "CORRECTIVO", "GENERAL"
    private String codigoGrupo = "";
    private List<Map<String, Object>> checklist = new ArrayList<>();
    private String tecnicoId = "";
    private String tecnicoNombre = "";
    private String tecnicoFirmaUrl = "";
    private boolean aprobadoPorGerencia = false;
    private long fechaAprobacion = 0;
    private String aprobadoPorNombre = "";

    public Tarea() {
        this.participantesIds = new ArrayList<>();
        this.checklist = new ArrayList<>();
    }

    public Tarea(String id, String titulo, String fechaCreacion, boolean completada) {
        this.id = id;
        this.titulo = titulo;
        this.fechaCreacion = fechaCreacion;
        this.completada = completada;
        this.participantesIds = new ArrayList<>();
        this.checklist = new ArrayList<>();
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

    public String getTipoTarea() { return tipoTarea; }
    public void setTipoTarea(String tipoTarea) { this.tipoTarea = tipoTarea; }

    public String getCodigoGrupo() { return codigoGrupo; }
    public void setCodigoGrupo(String codigoGrupo) { this.codigoGrupo = codigoGrupo; }

    public List<Map<String, Object>> getChecklist() {
        if (checklist == null) checklist = new ArrayList<>();
        return checklist;
    }
    public void setChecklist(List<Map<String, Object>> checklist) { this.checklist = checklist; }

    public String getTecnicoId() { return tecnicoId; }
    public void setTecnicoId(String tecnicoId) { this.tecnicoId = tecnicoId; }

    public String getTecnicoNombre() { return tecnicoNombre; }
    public void setTecnicoNombre(String tecnicoNombre) { this.tecnicoNombre = tecnicoNombre; }

    public String getTecnicoFirmaUrl() { return tecnicoFirmaUrl; }
    public void setTecnicoFirmaUrl(String tecnicoFirmaUrl) { this.tecnicoFirmaUrl = tecnicoFirmaUrl; }

    public boolean isAprobadoPorGerencia() { return aprobadoPorGerencia; }
    public void setAprobadoPorGerencia(boolean aprobadoPorGerencia) { this.aprobadoPorGerencia = aprobadoPorGerencia; }

    public long getFechaAprobacion() { return fechaAprobacion; }
    public void setFechaAprobacion(long fechaAprobacion) { this.fechaAprobacion = fechaAprobacion; }

    public String getAprobadoPorNombre() { return aprobadoPorNombre; }
    public void setAprobadoPorNombre(String aprobadoPorNombre) { this.aprobadoPorNombre = aprobadoPorNombre; }
}