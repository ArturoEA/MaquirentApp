package com.example.maquirentapp.Model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class AlquilerDia implements Serializable {

    private String id;
    private String idGrupo;
    private String adminUid;
    private String nombreCliente;
    private String ubicacion;
    private String fechaInicial;
    private String fechaFinal;
    private double horometroInicial;
    private double horometroFinal;
    private double precioTotal;
    private String moneda;
    private double horasMaximas;
    private List<String> accesoriosIds;
    private boolean finalizado = false;

    public AlquilerDia() {
        // Constructor vacío
        this.accesoriosIds = new ArrayList<>();
        this.horasMaximas = 10; // Default
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getIdGrupo() { return idGrupo; }
    public void setIdGrupo(String idGrupo) { this.idGrupo = idGrupo; }

    public String getAdminUid() { return adminUid; }
    public void setAdminUid(String adminUid) { this.adminUid = adminUid; }

    public String getNombreCliente() { return nombreCliente; }
    public void setNombreCliente(String nombreCliente) { this.nombreCliente = nombreCliente; }

    public String getUbicacion() { return ubicacion; }
    public void setUbicacion(String ubicacion) { this.ubicacion = ubicacion; }

    public String getFechaInicial() { return fechaInicial; }
    public void setFechaInicial(String fechaInicial) { this.fechaInicial = fechaInicial; }

    public String getFechaFinal() { return fechaFinal; }
    public void setFechaFinal(String fechaFinal) { this.fechaFinal = fechaFinal; }

    public double getHorometroInicial() { return horometroInicial; }
    public void setHorometroInicial(double horometroInicial) { this.horometroInicial = horometroInicial; }

    public double getHorometroFinal() { return horometroFinal; }
    public void setHorometroFinal(double horometroFinal) { this.horometroFinal = horometroFinal; }

    public double getPrecioTotal() { return precioTotal; }
    public void setPrecioTotal(double precioTotal) { this.precioTotal = precioTotal; }

    public String getMoneda() { return moneda; }
    public void setMoneda(String moneda) { this.moneda = moneda; }

    public double getHorasMaximas() { return horasMaximas; }
    public void setHorasMaximas(double horasMaximas) { this.horasMaximas = horasMaximas; }

    public List<String> getAccesoriosIds() { return accesoriosIds; }
    public void setAccesoriosIds(List<String> accesoriosIds) { this.accesoriosIds = accesoriosIds; }

    public boolean isFinalizado() { return finalizado; }
    public void setFinalizado(boolean finalizado) { this.finalizado = finalizado; }
}