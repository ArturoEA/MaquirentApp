package com.example.maquirentapp.Model;

public class DetalleMes {
    private String id;
    private String idAlquilerMensual;
    private String tituloPeriodo;
    private String fechaInicio;
    private String fechaFin;
    private double horometro;
    private double horasExtras;
    private double precioHorasExtras;
    private boolean pagoMesConfirmado;
    private boolean pagoHEConfirmado;
    private int numeroMes;
    private boolean expandido;

    public DetalleMes() {
        this.pagoMesConfirmado = false;
        this.pagoHEConfirmado = false;
        this.expandido = false;
        this.horometro = 0;
        this.horasExtras = 0;
        this.precioHorasExtras = 0;
    }

    // Getters y Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getIdAlquilerMensual() {
        return idAlquilerMensual;
    }

    public void setIdAlquilerMensual(String idAlquilerMensual) {
        this.idAlquilerMensual = idAlquilerMensual;
    }

    public String getTituloPeriodo() {
        return tituloPeriodo;
    }

    public void setTituloPeriodo(String tituloPeriodo) {
        this.tituloPeriodo = tituloPeriodo;
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

    public double getHorometro() {
        return horometro;
    }

    public void setHorometro(double horometro) {
        this.horometro = horometro;
    }

    public double getHorasExtras() {
        return horasExtras;
    }

    public void setHorasExtras(double horasExtras) {
        this.horasExtras = horasExtras;
    }

    public double getPrecioHorasExtras() {
        return precioHorasExtras;
    }

    public void setPrecioHorasExtras(double precioHorasExtras) {
        this.precioHorasExtras = precioHorasExtras;
    }

    public boolean isPagoMesConfirmado() {
        return pagoMesConfirmado;
    }

    public void setPagoMesConfirmado(boolean pagoMesConfirmado) {
        this.pagoMesConfirmado = pagoMesConfirmado;
    }

    public boolean isPagoHEConfirmado() {
        return pagoHEConfirmado;
    }

    public void setPagoHEConfirmado(boolean pagoHEConfirmado) {
        this.pagoHEConfirmado = pagoHEConfirmado;
    }

    public int getNumeroMes() {
        return numeroMes;
    }

    public void setNumeroMes(int numeroMes) {
        this.numeroMes = numeroMes;
    }

    public boolean isExpandido() {
        return expandido;
    }

    public void setExpandido(boolean expandido) {
        this.expandido = expandido;
    }
}