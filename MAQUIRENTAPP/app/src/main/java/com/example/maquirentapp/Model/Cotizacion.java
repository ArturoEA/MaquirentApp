package com.example.maquirentapp.Model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Cotizacion implements Serializable {
    private String id;
    private String numeroCotizacion;
    private String clienteNombre;
    private String clienteRuc;
    private String lugarTrabajo;
    private String fechaEmision;
    private String moneda;
    private int horasMinimas;

    private List<ItemCotizacion> items;
    private double subtotalGlobal;
    private double igvGlobal;
    private double totalGlobal;

    private String fechaCreacionTimestamp;

    public Cotizacion() {
        this.items = new ArrayList<>();
        this.horasMinimas = 200;
    }

    public void calcularTotales() {
        this.subtotalGlobal = 0;
        for (ItemCotizacion item : items) {
            this.subtotalGlobal += item.getPrecioMensual();
        }
        this.igvGlobal = this.subtotalGlobal * 0.18;
        this.totalGlobal = this.subtotalGlobal + this.igvGlobal;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNumeroCotizacion() {
        return numeroCotizacion;
    }

    public void setNumeroCotizacion(String numeroCotizacion) {
        this.numeroCotizacion = numeroCotizacion;
    }

    public String getClienteNombre() {
        return clienteNombre;
    }

    public void setClienteNombre(String clienteNombre) {
        this.clienteNombre = clienteNombre;
    }

    public String getClienteRuc() {
        return clienteRuc;
    }

    public void setClienteRuc(String clienteRuc) {
        this.clienteRuc = clienteRuc;
    }

    public String getLugarTrabajo() {
        return lugarTrabajo;
    }

    public void setLugarTrabajo(String lugarTrabajo) {
        this.lugarTrabajo = lugarTrabajo;
    }

    public String getFechaEmision() {
        return fechaEmision;
    }

    public void setFechaEmision(String fechaEmision) {
        this.fechaEmision = fechaEmision;
    }

    public String getMoneda() {
        return moneda;
    }

    public void setMoneda(String moneda) {
        this.moneda = moneda;
    }

    public int getHorasMinimas() {
        return horasMinimas;
    }

    public void setHorasMinimas(int horasMinimas) {
        this.horasMinimas = horasMinimas;
    }

    public List<ItemCotizacion> getItems() {
        return items;
    }

    public void setItems(List<ItemCotizacion> items) {
        this.items = items;
    }

    public double getSubtotalGlobal() {
        return subtotalGlobal;
    }

    public void setSubtotalGlobal(double subtotalGlobal) {
        this.subtotalGlobal = subtotalGlobal;
    }

    public double getIgvGlobal() {
        return igvGlobal;
    }

    public void setIgvGlobal(double igvGlobal) {
        this.igvGlobal = igvGlobal;
    }

    public double getTotalGlobal() {
        return totalGlobal;
    }

    public void setTotalGlobal(double totalGlobal) {
        this.totalGlobal = totalGlobal;
    }

    public String getFechaCreacionTimestamp() {
        return fechaCreacionTimestamp;
    }

    public void setFechaCreacionTimestamp(String fechaCreacionTimestamp) {
        this.fechaCreacionTimestamp = fechaCreacionTimestamp;
    }
}