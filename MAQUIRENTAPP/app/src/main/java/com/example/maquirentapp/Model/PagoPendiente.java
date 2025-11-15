package com.example.maquirentapp.Model;

public class PagoPendiente {

    private String nombreCliente;
    private String codigoGrupo;
    private String tituloPeriodo;
    private double montoPendienteMes;
    private double montoPendienteHE;
    private String moneda;
    private int estadoColor;
    private String alquilerId;
    private String detalleMesId;
    private String idGrupo;

    public PagoPendiente() {
    }
    public String getNombreCliente() {
        return nombreCliente;
    }

    public void setNombreCliente(String nombreCliente) {
        this.nombreCliente = nombreCliente;
    }

    public String getCodigoGrupo() {
        return codigoGrupo;
    }

    public void setCodigoGrupo(String codigoGrupo) {
        this.codigoGrupo = codigoGrupo;
    }

    public String getTituloPeriodo() {
        return tituloPeriodo;
    }

    public void setTituloPeriodo(String tituloPeriodo) {
        this.tituloPeriodo = tituloPeriodo;
    }

    public double getMontoPendienteMes() {
        return montoPendienteMes;
    }

    public void setMontoPendienteMes(double montoPendienteMes) {
        this.montoPendienteMes = montoPendienteMes;
    }

    public double getMontoPendienteHE() {
        return montoPendienteHE;
    }

    public void setMontoPendienteHE(double montoPendienteHE) {
        this.montoPendienteHE = montoPendienteHE;
    }

    public String getMoneda() {
        return moneda;
    }

    public void setMoneda(String moneda) {
        this.moneda = moneda;
    }

    public int getEstadoColor() {
        return estadoColor;
    }

    public void setEstadoColor(int estadoColor) {
        this.estadoColor = estadoColor;
    }

    public String getAlquilerId() {
        return alquilerId;
    }

    public void setAlquilerId(String alquilerId) {
        this.alquilerId = alquilerId;
    }

    public String getDetalleMesId() {
        return detalleMesId;
    }

    public void setDetalleMesId(String detalleMesId) {
        this.detalleMesId = detalleMesId;
    }

    public String getIdGrupo() {
        return idGrupo;
    }

    public void setIdGrupo(String idGrupo) {
        this.idGrupo = idGrupo;
    }
}