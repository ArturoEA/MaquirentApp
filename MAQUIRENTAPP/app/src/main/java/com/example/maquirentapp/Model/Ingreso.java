package com.example.maquirentapp.Model;

import com.google.firebase.firestore.FieldValue;

public class Ingreso {
    private String id;
    private double monto;
    private String moneda;
    private String tipo; // "Alquiler Mensual", "Horas Extras", "Alquiler Diario"
    private String idGrupo;
    private String idAlquiler;
    private String nombreCliente;
    private Object fechaConfirmacion;
    private int mes;
    private int anio;
    public Ingreso() { }
    public Ingreso(double monto, String moneda, String tipo, String idGrupo, String idAlquiler, String nombreCliente, int mes, int anio) {
        this.monto = monto;
        this.moneda = moneda;
        this.tipo = tipo;
        this.idGrupo = idGrupo;
        this.idAlquiler = idAlquiler;
        this.nombreCliente = nombreCliente;
        this.mes = mes;
        this.anio = anio;
        this.fechaConfirmacion = FieldValue.serverTimestamp();
    }
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public double getMonto() { return monto; }
    public void setMonto(double monto) { this.monto = monto; }
    public String getMoneda() { return moneda; }
    public void setMoneda(String moneda) { this.moneda = moneda; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public String getIdGrupo() { return idGrupo; }
    public void setIdGrupo(String idGrupo) { this.idGrupo = idGrupo; }
    public String getIdAlquiler() { return idAlquiler; }
    public void setIdAlquiler(String idAlquiler) { this.idAlquiler = idAlquiler; }
    public String getNombreCliente() { return nombreCliente; }
    public void setNombreCliente(String nombreCliente) { this.nombreCliente = nombreCliente; }
    public Object getFechaConfirmacion() { return fechaConfirmacion; }
    public void setFechaConfirmacion(Object fechaConfirmacion) { this.fechaConfirmacion = fechaConfirmacion; }
    public int getMes() { return mes; }
    public void setMes(int mes) { this.mes = mes; }
    public int getAnio() { return anio; }
    public void setAnio(int anio) { this.anio = anio; }
}