package com.example.maquirentapp.Model;

import java.io.Serializable;

public class ItemValorizacion implements Serializable {
    private String idAlquiler;
    private String descripcionEquipo;
    private String fechaInicio;
    private String fechaFin;
    private double horometroInicio;
    private double horometroFin;
    private double horasTrabajadas;
    private double precioMes;
    private double precioHorasExtras;
    private double totalItem;
    private String marca;
    private String modelo;
    private String serie;

    public ItemValorizacion() {
    }

    public String getIdAlquiler() {
        return idAlquiler;
    }

    public void setIdAlquiler(String idAlquiler) {
        this.idAlquiler = idAlquiler;
    }

    public String getDescripcionEquipo() {
        return descripcionEquipo;
    }

    public void setDescripcionEquipo(String descripcionEquipo) {
        this.descripcionEquipo = descripcionEquipo;
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

    public double getHorometroInicio() {
        return horometroInicio;
    }

    public void setHorometroInicio(double horometroInicio) {
        this.horometroInicio = horometroInicio;
    }

    public double getHorometroFin() {
        return horometroFin;
    }

    public void setHorometroFin(double horometroFin) {
        this.horometroFin = horometroFin;
    }

    public double getHorasTrabajadas() {
        return horasTrabajadas;
    }

    public void setHorasTrabajadas(double horasTrabajadas) {
        this.horasTrabajadas = horasTrabajadas;
    }

    public double getPrecioMes() {
        return precioMes;
    }

    public void setPrecioMes(double precioMes) {
        this.precioMes = precioMes;
    }

    public double getPrecioHorasExtras() {
        return precioHorasExtras;
    }

    public void setPrecioHorasExtras(double precioHorasExtras) {
        this.precioHorasExtras = precioHorasExtras;
    }

    public String getMarca() {
        return marca;
    }

    public void setMarca(String marca) {
        this.marca = marca;
    }

    public String getModelo() {
        return modelo;
    }

    public void setModelo(String modelo) {
        this.modelo = modelo;
    }

    public String getSerie() {
        return serie;
    }

    public void setSerie(String serie) {
        this.serie = serie;
    }

    public double getTotalItem() {
        return totalItem;
    }

    public void setTotalItem(double totalItem) {
        this.totalItem = totalItem;
    }
}