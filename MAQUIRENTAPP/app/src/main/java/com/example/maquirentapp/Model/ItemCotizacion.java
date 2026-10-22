package com.example.maquirentapp.Model;

import java.io.Serializable;

public class ItemCotizacion implements Serializable {
    private String descripcionEquipo;
    private String potencia;
    private String modoTrabajo;
    private String marca;
    private String incluye;
    private double precioUnitario;
    private double precioMensual;
    private double precioHoraExtra;

    public ItemCotizacion() {
        this.incluye = "Bandeja y kit antiderrame, extintor, varilla puesta a tierra y certificado de operatividad";
    }

    public ItemCotizacion(String descripcionEquipo, String potencia, String modoTrabajo, String marca, double precioUnitario, double precioHoraExtra) {
        this.descripcionEquipo = descripcionEquipo;
        this.potencia = potencia;
        this.modoTrabajo = modoTrabajo;
        this.marca = marca;
        this.precioUnitario = precioUnitario;
        this.precioMensual = precioUnitario;
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

    public double getPrecioUnitario() {
        return precioUnitario > 0 ? precioUnitario : precioMensual;
    }

    public void setPrecioUnitario(double precioUnitario) {
        this.precioUnitario = precioUnitario;
        this.precioMensual = precioUnitario;
    }

    public double getPrecioMensual() {
        return precioMensual > 0 ? precioMensual : precioUnitario;
    }

    public void setPrecioMensual(double precioMensual) {
        this.precioMensual = precioMensual;
        if (this.precioUnitario == 0) {
            this.precioUnitario = precioMensual;
        }
    }

    public double getPrecioHoraExtra() {
        return precioHoraExtra;
    }

    public void setPrecioHoraExtra(double precioHoraExtra) {
        this.precioHoraExtra = precioHoraExtra;
    }

    public double getSubtotal(String tipoAlquiler, int diasAlquiler) {
        if (Cotizacion.TIPO_DIARIO.equalsIgnoreCase(tipoAlquiler)) {
            return getPrecioUnitario() * (diasAlquiler > 0 ? diasAlquiler : 1);
        }
        return getPrecioUnitario();
    }

    public double getIgv(String tipoAlquiler, int diasAlquiler) {
        return getSubtotal(tipoAlquiler, diasAlquiler) * 0.18;
    }

    public double getTotalConIgv(String tipoAlquiler, int diasAlquiler) {
        return getSubtotal(tipoAlquiler, diasAlquiler) * 1.18;
    }

    public double getIgv() {
        return getPrecioUnitario() * 0.18;
    }

    public double getTotalConIgv() {
        return getPrecioUnitario() * 1.18;
    }
}
