package com.example.maquirentapp.Model;

import java.util.ArrayList;
import java.util.List;

public class Mantenimiento {
    private String id;
    private String codigoGrupo;
    private String empresa;
    private String horometro;
    private String fecha;
    private List<String> itemsRealizados;
    private String comentarios;
    private List<String> fotos;
    private long fechaCreacion;

    public Mantenimiento() {
        this.itemsRealizados = new ArrayList<>();
        this.fotos = new ArrayList<>();
    }

    public Mantenimiento(String codigoGrupo, String empresa, String horometro, String fecha,
                         List<String> itemsRealizados, String comentarios, List<String> fotos) {
        this.codigoGrupo = codigoGrupo;
        this.empresa = empresa;
        this.horometro = horometro;
        this.fecha = fecha;
        this.itemsRealizados = itemsRealizados != null ? itemsRealizados : new ArrayList<>();
        this.comentarios = comentarios;
        this.fotos = fotos != null ? fotos : new ArrayList<>();
        this.fechaCreacion = System.currentTimeMillis();
    }

    // Getters y Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCodigoGrupo() {
        return codigoGrupo;
    }

    public void setCodigoGrupo(String codigoGrupo) {
        this.codigoGrupo = codigoGrupo;
    }

    public String getEmpresa() {
        return empresa;
    }

    public void setEmpresa(String empresa) {
        this.empresa = empresa;
    }

    public String getHorometro() {
        return horometro;
    }

    public void setHorometro(String horometro) {
        this.horometro = horometro;
    }

    public String getFecha() {
        return fecha;
    }

    public void setFecha(String fecha) {
        this.fecha = fecha;
    }

    public List<String> getItemsRealizados() {
        return itemsRealizados;
    }

    public void setItemsRealizados(List<String> itemsRealizados) {
        this.itemsRealizados = itemsRealizados;
    }

    public String getComentarios() {
        return comentarios;
    }

    public void setComentarios(String comentarios) {
        this.comentarios = comentarios;
    }

    public List<String> getFotos() {
        return fotos;
    }

    public void setFotos(List<String> fotos) {
        this.fotos = fotos;
    }

    public long getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(long fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public boolean tieneFotos() {
        return fotos != null && !fotos.isEmpty();
    }

    public int getCantidadFotos() {
        return fotos != null ? fotos.size() : 0;
    }
}