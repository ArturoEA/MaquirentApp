package com.example.maquirentapp.Model;

import java.io.Serializable;

public class ItemCotizacion implements Serializable {
    private String descripcionEquipo;
    private String potencia;
    private String modoTrabajo;
    private String marca;
    private String incluye;
    private double precioMensual;
    private double precioHoraExtra;

    public ItemCotizacion() {
        this.incluye = "Bandeja y kit antiderrame, extintor, varilla puesta a tierra y certificado de operatividad";
    }

    public ItemCotizacion(String descripcionEquipo, String potencia, String modoTrabajo, String marca, double precioMensual, double precioHoraExtra) {
        this.descripcionEquipo = descripcionEquipo;
        this.potencia = potencia;
        this.modoTrabajo = modoTrabajo;
        this.marca = marca;
        this.precioMensual = precioMensual;
        this.precioHoraExtra = precioHoraExtra;
        this.incluye = "Bandeja y kit anti derrame, extintor, varilla puesta a tierra y certificado de operatividad";
    }

    public String getDescripcionEquipo() {
        return descripcionEquipo;
    }

    public void setDescripcionEquipo(String descripcionEquipo) {
        this.descripcionEquipo = descripcionEquipo;
    }

    public String getPotencia() {
        return potencia;
    }

    public void setPotencia(String potencia) {
        this.potencia = potencia;
    }

    public String getModoTrabajo() {
        return modoTrabajo;
    }

    public void setModoTrabajo(String modoTrabajo) {
        this.modoTrabajo = modoTrabajo;
    }

    public String getMarca() {
        return marca;
    }

    public void setMarca(String marca) {
        this.marca = marca;
    }

    public String getIncluye() {
        return incluye;
    }

    public void setIncluye(String incluye) {
        this.incluye = incluye;
    }

    public double getPrecioMensual() {
        return precioMensual;
    }

    public void setPrecioMensual(double precioMensual) {
        this.precioMensual = precioMensual;
    }

    public double getPrecioHoraExtra() {
        return precioHoraExtra;
    }

    public void setPrecioHoraExtra(double precioHoraExtra) {
        this.precioHoraExtra = precioHoraExtra;
    }
    public double getIgv() {
        return precioMensual * 0.18;
    }

    public double getTotalConIgv() {
        return precioMensual * 1.18;
    }
}