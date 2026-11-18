package com.example.maquirentapp.Model;

import java.util.HashMap;
import java.util.Map;

public class GrupoElectrogeno {
    private String id;
    private String codigo;
    private String foto;
    private int orden;
    private boolean eliminado = false;
    private long fechaEliminacion = 0;
    private String eliminadoPor = "";

    // Campos de seguimiento de mantenimiento y horómetro
    private double horometroActual = 0.0;
    private long fechaActualizacionHorometro = 0;
    private String tipoEquipo = "Grupo electrógeno";
    private String unidadMedida = "HORAS";
    private long contadorRetornos = 0;
    private Map<String, Double> relojesTareas = new HashMap<>();
    private Map<String, Long> fechasTareas = new HashMap<>();
    private Map<String, Long> retornosTareas = new HashMap<>();
    private Map<String, Double> relojesComponentes = new HashMap<>();
    private Map<String, Long> fechasComponentes = new HashMap<>();

    public GrupoElectrogeno() { }

    public GrupoElectrogeno(String codigo, String foto) {
        this.codigo = codigo;
        this.foto = foto;
    }

    // Getters y setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getFoto() { return foto; }
    public void setFoto(String foto) { this.foto = foto; }

    public boolean isEliminado() { return eliminado; }
    public void setEliminado(boolean eliminado) { this.eliminado = eliminado; }

    public long getFechaEliminacion() { return fechaEliminacion; }
    public void setFechaEliminacion(long fechaEliminacion) { this.fechaEliminacion = fechaEliminacion; }

    public String getEliminadoPor() { return eliminadoPor; }
    public void setEliminadoPor(String eliminadoPor) { this.eliminadoPor = eliminadoPor; }

    public int getOrden() {
        return orden;
    }

    public void setOrden(int orden) {
        this.orden = orden;
    }

    public double getHorometroActual() {
        return horometroActual;
    }

    public void setHorometroActual(double horometroActual) {
        this.horometroActual = horometroActual;
    }

    public long getFechaActualizacionHorometro() {
        return fechaActualizacionHorometro;
    }

    public void setFechaActualizacionHorometro(long fechaActualizacionHorometro) {
        this.fechaActualizacionHorometro = fechaActualizacionHorometro;
    }

    public String getTipoEquipo() {
        return tipoEquipo;
    }

    public void setTipoEquipo(String tipoEquipo) {
        this.tipoEquipo = tipoEquipo;
    }

    public String getUnidadMedida() {
        return unidadMedida;
    }

    public void setUnidadMedida(String unidadMedida) {
        this.unidadMedida = unidadMedida;
    }

    public long getContadorRetornos() {
        return contadorRetornos;
    }

    public void setContadorRetornos(long contadorRetornos) {
        this.contadorRetornos = contadorRetornos;
    }

    public Map<String, Double> getRelojesTareas() {
        if (relojesTareas == null) relojesTareas = new HashMap<>();
        return relojesTareas;
    }

    public void setRelojesTareas(Map<String, Double> relojesTareas) {
        this.relojesTareas = relojesTareas;
    }

    public Map<String, Long> getFechasTareas() {
        if (fechasTareas == null) fechasTareas = new HashMap<>();
        return fechasTareas;
    }

    public void setFechasTareas(Map<String, Long> fechasTareas) {
        this.fechasTareas = fechasTareas;
    }

    public Map<String, Long> getRetornosTareas() {
        if (retornosTareas == null) retornosTareas = new HashMap<>();
        return retornosTareas;
    }

    public void setRetornosTareas(Map<String, Long> retornosTareas) {
        this.retornosTareas = retornosTareas;
    }

    public Map<String, Double> getRelojesComponentes() {
        if (relojesComponentes == null) relojesComponentes = new HashMap<>();
        return relojesComponentes;
    }

    public void setRelojesComponentes(Map<String, Double> relojesComponentes) {
        this.relojesComponentes = relojesComponentes;
    }

    public Map<String, Long> getFechasComponentes() {
        if (fechasComponentes == null) fechasComponentes = new HashMap<>();
        return fechasComponentes;
    }

    public void setFechasComponentes(Map<String, Long> fechasComponentes) {
        this.fechasComponentes = fechasComponentes;
    }
}
