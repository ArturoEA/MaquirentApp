package com.example.maquirentapp.Model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Valorizacion implements Serializable {
    private String id;
    private String numeroValorizacion;
    private String idClienteValorizacion;
    private String nombreCliente;
    private String fechaEmision;
    private String moneda = "SOL";
    private String clienteRuc;
    private String clienteDireccion;
    private String ubicacionTrabajo;

    // Datos del Alquiler y Equipo asociado
    private boolean conHorasExtras = false;
    private String idAlquiler = "";
    private String idDetalleMes = "";
    private int numeroMes = 1;
    private String codigoEquipo = "";
    private String descripcionEquipo = "";
    private String marcaEquipo = "";
    private String fechaInicio = "";
    private String fechaFin = "";

    // Detalle económico
    private int diasValorizados = 30;
    private double costoDiario = 0.0;
    private double costoParcial = 0.0;

    // Horómetro y horas extras (para plantilla ValorizacionHE)
    private double horometroInicial = 0.0;
    private double horometroFinal = 0.0;
    private double horasPermitidas = 200.0;
    private double horasUsadas = 0.0;
    private double horasExtras = 0.0;
    private double costoHoraExtra = 0.0;
    private double totalHorasExtras = 0.0;

    // Totales
    private double subtotal = 0.0;
    private double igv = 0.0;
    private double total = 0.0;
    private String totalEnLetras = "";

    private List<ItemValorizacion> items;
    private long timestampCreacion;

    public Valorizacion() {
        this.items = new ArrayList<>();
        this.timestampCreacion = System.currentTimeMillis();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNumeroValorizacion() {
        return numeroValorizacion;
    }

    public void setNumeroValorizacion(String numeroValorizacion) {
        this.numeroValorizacion = numeroValorizacion;
    }

    public String getIdClienteValorizacion() {
        return idClienteValorizacion;
    }

    public void setIdClienteValorizacion(String idClienteValorizacion) {
        this.idClienteValorizacion = idClienteValorizacion;
    }

    public String getNombreCliente() {
        return nombreCliente;
    }

    public void setNombreCliente(String nombreCliente) {
        this.nombreCliente = nombreCliente;
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

    public String getClienteRuc() {
        return clienteRuc;
    }

    public void setClienteRuc(String clienteRuc) {
        this.clienteRuc = clienteRuc;
    }

    public String getClienteDireccion() {
        return clienteDireccion;
    }

    public void setClienteDireccion(String clienteDireccion) {
        this.clienteDireccion = clienteDireccion;
    }

    public String getUbicacionTrabajo() {
        return ubicacionTrabajo;
    }

    public void setUbicacionTrabajo(String ubicacionTrabajo) {
        this.ubicacionTrabajo = ubicacionTrabajo;
    }

    public boolean isConHorasExtras() {
        return conHorasExtras;
    }

    public void setConHorasExtras(boolean conHorasExtras) {
        this.conHorasExtras = conHorasExtras;
    }

    public String getIdAlquiler() {
        return idAlquiler;
    }

    public void setIdAlquiler(String idAlquiler) {
        this.idAlquiler = idAlquiler;
    }

    public String getIdDetalleMes() {
        return idDetalleMes;
    }

    public void setIdDetalleMes(String idDetalleMes) {
        this.idDetalleMes = idDetalleMes;
    }

    public int getNumeroMes() {
        return numeroMes;
    }

    public void setNumeroMes(int numeroMes) {
        this.numeroMes = numeroMes;
    }

    public String getCodigoEquipo() {
        return codigoEquipo;
    }

    public void setCodigoEquipo(String codigoEquipo) {
        this.codigoEquipo = codigoEquipo;
    }

    public String getDescripcionEquipo() {
        return descripcionEquipo;
    }

    public void setDescripcionEquipo(String descripcionEquipo) {
        this.descripcionEquipo = descripcionEquipo;
    }

    public String getMarcaEquipo() {
        return marcaEquipo;
    }

    public void setMarcaEquipo(String marcaEquipo) {
        this.marcaEquipo = marcaEquipo;
    }

    public String getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(String fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public String getFechaFin() {
        return fechaFin;
    }

    public void setFechaFin(String fechaFin) {
        this.fechaFin = fechaFin;
    }

    public int getDiasValorizados() {
        return diasValorizados;
    }

    public void setDiasValorizados(int diasValorizados) {
        this.diasValorizados = diasValorizados;
    }

    public double getCostoDiario() {
        return costoDiario;
    }

    public void setCostoDiario(double costoDiario) {
        this.costoDiario = costoDiario;
    }

    public double getCostoParcial() {
        return costoParcial;
    }

    public void setCostoParcial(double costoParcial) {
        this.costoParcial = costoParcial;
    }

    public double getHorometroInicial() {
        return horometroInicial;
    }

    public void setHorometroInicial(double horometroInicial) {
        this.horometroInicial = horometroInicial;
    }

    public double getHorometroFinal() {
        return horometroFinal;
    }

    public void setHorometroFinal(double horometroFinal) {
        this.horometroFinal = horometroFinal;
    }

    public double getHorasPermitidas() {
        return horasPermitidas;
    }

    public void setHorasPermitidas(double horasPermitidas) {
        this.horasPermitidas = horasPermitidas;
    }

    public double getHorasUsadas() {
        return horasUsadas;
    }

    public void setHorasUsadas(double horasUsadas) {
        this.horasUsadas = horasUsadas;
    }

    public double getHorasExtras() {
        return horasExtras;
    }

    public void setHorasExtras(double horasExtras) {
        this.horasExtras = horasExtras;
    }

    public double getCostoHoraExtra() {
        return costoHoraExtra;
    }

    public void setCostoHoraExtra(double costoHoraExtra) {
        this.costoHoraExtra = costoHoraExtra;
    }

    public double getTotalHorasExtras() {
        return totalHorasExtras;
    }

    public void setTotalHorasExtras(double totalHorasExtras) {
        this.totalHorasExtras = totalHorasExtras;
    }

    public double getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(double subtotal) {
        this.subtotal = subtotal;
    }

    public double getIgv() {
        return igv;
    }

    public void setIgv(double igv) {
        this.igv = igv;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public String getTotalEnLetras() {
        return totalEnLetras;
    }

    public void setTotalEnLetras(String totalEnLetras) {
        this.totalEnLetras = totalEnLetras;
    }

    public List<ItemValorizacion> getItems() {
        return items;
    }

    public void setItems(List<ItemValorizacion> items) {
        this.items = items;
    }

    public long getTimestampCreacion() {
        return timestampCreacion;
    }

    public void setTimestampCreacion(long timestampCreacion) {
        this.timestampCreacion = timestampCreacion;
    }
}
