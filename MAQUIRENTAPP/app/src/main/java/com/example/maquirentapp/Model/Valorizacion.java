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
    private String moneda;
    private String clienteRuc;
    private String clienteDireccion;
    private String ubicacionTrabajo;

    private List<ItemValorizacion> items;

    private double subtotal;
    private double igv;
    private double total;

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

    public List<ItemValorizacion> getItems() {
        return items;
    }

    public void setItems(List<ItemValorizacion> items) {
        this.items = items;
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

    public long getTimestampCreacion() {
        return timestampCreacion;
    }

    public void setTimestampCreacion(long timestampCreacion) {
        this.timestampCreacion = timestampCreacion;
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
}