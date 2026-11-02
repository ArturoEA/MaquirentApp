package com.example.maquirentapp.Model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Cotizacion implements Serializable {
    public static final String TIPO_MENSUAL = "MENSUAL";
    public static final String TIPO_DIARIO = "DIARIO";

    private String id;
    private String numeroCotizacion;
    private String clienteNombre;
    private String clienteRuc;
    private String lugarTrabajo;
    private String fechaEmision;
    private String moneda;
    private int horasMinimas;

    // Modalidad Diario / Mensual
    private String tipoAlquiler = TIPO_MENSUAL;
    private int diasAlquiler = 1;
    private int horasPorDia = 8;

    // Condiciones comerciales dinámicas
    private List<String> condicionesComerciales;

    private List<ItemCotizacion> items;
    private double costoMovilizacion = 0.0;
    private double subtotalGlobal;
    private double igvGlobal;
    private double totalGlobal;

    private String fechaCreacionTimestamp;
    private String fichaTecnicaNombre;

    public Cotizacion() {
        this.items = new ArrayList<>();
        this.horasMinimas = 200;
        this.tipoAlquiler = TIPO_MENSUAL;
        this.diasAlquiler = 1;
        this.horasPorDia = 8;
        this.condicionesComerciales = new ArrayList<>();
    }

    public void calcularTotales() {
        this.subtotalGlobal = 0;
        boolean esDiario = isDiario();
        int dias = this.diasAlquiler > 0 ? this.diasAlquiler : 1;

        if (this.items != null) {
            for (ItemCotizacion item : items) {
                double precio = item.getPrecioUnitario();
                double subtotalItem = esDiario ? (precio * dias) : precio;
                this.subtotalGlobal += subtotalItem;
            }
        }

        if (this.costoMovilizacion > 0) {
            this.subtotalGlobal += this.costoMovilizacion;
        }

        this.igvGlobal = com.example.maquirentapp.strategy.StrategyProvider.getCalculadoraStrategy().calcularImpuesto(this.subtotalGlobal);
        this.totalGlobal = com.example.maquirentapp.strategy.StrategyProvider.getCalculadoraStrategy().calcularTotalConImpuesto(this.subtotalGlobal);
    }

    public boolean isDiario() {
        return TIPO_DIARIO.equalsIgnoreCase(this.tipoAlquiler);
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

    public String getTipoAlquiler() {
        return tipoAlquiler != null ? tipoAlquiler : TIPO_MENSUAL;
    }

    public void setTipoAlquiler(String tipoAlquiler) {
        this.tipoAlquiler = tipoAlquiler;
    }

    public int getDiasAlquiler() {
        return diasAlquiler > 0 ? diasAlquiler : 1;
    }

    public void setDiasAlquiler(int diasAlquiler) {
        this.diasAlquiler = diasAlquiler;
    }

    public int getHorasPorDia() {
        return horasPorDia > 0 ? horasPorDia : 8;
    }

    public void setHorasPorDia(int horasPorDia) {
        this.horasPorDia = horasPorDia;
    }

    public List<String> getCondicionesComerciales() {
        if (condicionesComerciales == null) {
            condicionesComerciales = new ArrayList<>();
        }
        return condicionesComerciales;
    }

    public void setCondicionesComerciales(List<String> condicionesComerciales) {
        this.condicionesComerciales = condicionesComerciales;
    }

    public List<ItemCotizacion> getItems() {
        if (items == null) {
            items = new ArrayList<>();
        }
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

    public String getFichaTecnicaNombre() {
        return fichaTecnicaNombre;
    }

    public void setFichaTecnicaNombre(String fichaTecnicaNombre) {
        this.fichaTecnicaNombre = fichaTecnicaNombre;
    }

    public double getCostoMovilizacion() {
        return costoMovilizacion;
    }

    public void setCostoMovilizacion(double costoMovilizacion) {
        this.costoMovilizacion = costoMovilizacion;
    }
}
