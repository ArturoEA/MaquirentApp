package com.example.maquirentapp.Model;

import java.util.Date;

public class AlquilerDia {
   private int id;
   private int idGrupo;
   public String cliente;
   public String ubicacion;
    public double horometroInicial;
    public double horometroFinal;
    public Date fechaInicial;
    public Date fechaFinal;

    public AlquilerDia(int id, int idGrupo, String cliente, String ubicacion, double horometroInicial, double horometroFinal, Date fechaInicial, Date fechaFinal, double precioDia, Boolean varillaTierra, Boolean cableElectrico, Boolean tableroDistribucion, Boolean extensionCaja, Boolean bidonCombustible, Boolean llaveHexagonal, Boolean llavesPuertas, Boolean tacoMadera, Boolean manguera, Boolean embudo, Boolean fajasSogas, Boolean pinPerno, String comentarios) {
        this.id = id;
        this.idGrupo = idGrupo;
        this.cliente = cliente;
        this.ubicacion = ubicacion;
        this.horometroInicial = horometroInicial;
        this.horometroFinal = horometroFinal;
        this.fechaInicial = fechaInicial;
        this.fechaFinal = fechaFinal;
        this.precioDia = precioDia;
        this.varillaTierra = varillaTierra;
        this.cableElectrico = cableElectrico;
        this.tableroDistribucion = tableroDistribucion;
        this.extensionCaja = extensionCaja;
        this.bidonCombustible = bidonCombustible;
        this.llaveHexagonal = llaveHexagonal;
        this.llavesPuertas = llavesPuertas;
        this.tacoMadera = tacoMadera;
        this.manguera = manguera;
        this.embudo = embudo;
        this.fajasSogas = fajasSogas;
        this.pinPerno = pinPerno;
        this.comentarios = comentarios;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getIdGrupo() {
        return idGrupo;
    }

    public void setIdGrupo(int idGrupo) {
        this.idGrupo = idGrupo;
    }

    public String getCliente() {
        return cliente;
    }

    public void setCliente(String cliente) {
        this.cliente = cliente;
    }

    public String getUbicacion() {
        return ubicacion;
    }

    public void setUbicacion(String ubicacion) {
        this.ubicacion = ubicacion;
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

    public Date getFechaInicial() {
        return fechaInicial;
    }

    public void setFechaInicial(Date fechaInicial) {
        this.fechaInicial = fechaInicial;
    }

    public Date getFechaFinal() {
        return fechaFinal;
    }

    public void setFechaFinal(Date fechaFinal) {
        this.fechaFinal = fechaFinal;
    }

    public double getPrecioDia() {
        return precioDia;
    }

    public void setPrecioDia(double precioDia) {
        this.precioDia = precioDia;
    }

    public Boolean getVarillaTierra() {
        return varillaTierra;
    }

    public void setVarillaTierra(Boolean varillaTierra) {
        this.varillaTierra = varillaTierra;
    }

    public Boolean getCableElectrico() {
        return cableElectrico;
    }

    public void setCableElectrico(Boolean cableElectrico) {
        this.cableElectrico = cableElectrico;
    }

    public Boolean getTableroDistribucion() {
        return tableroDistribucion;
    }

    public void setTableroDistribucion(Boolean tableroDistribucion) {
        this.tableroDistribucion = tableroDistribucion;
    }

    public Boolean getExtensionCaja() {
        return extensionCaja;
    }

    public void setExtensionCaja(Boolean extensionCaja) {
        this.extensionCaja = extensionCaja;
    }

    public Boolean getBidonCombustible() {
        return bidonCombustible;
    }

    public void setBidonCombustible(Boolean bidonCombustible) {
        this.bidonCombustible = bidonCombustible;
    }

    public Boolean getLlaveHexagonal() {
        return llaveHexagonal;
    }

    public void setLlaveHexagonal(Boolean llaveHexagonal) {
        this.llaveHexagonal = llaveHexagonal;
    }

    public Boolean getLlavesPuertas() {
        return llavesPuertas;
    }

    public void setLlavesPuertas(Boolean llavesPuertas) {
        this.llavesPuertas = llavesPuertas;
    }

    public Boolean getTacoMadera() {
        return tacoMadera;
    }

    public void setTacoMadera(Boolean tacoMadera) {
        this.tacoMadera = tacoMadera;
    }

    public Boolean getManguera() {
        return manguera;
    }

    public void setManguera(Boolean manguera) {
        this.manguera = manguera;
    }

    public Boolean getEmbudo() {
        return embudo;
    }

    public void setEmbudo(Boolean embudo) {
        this.embudo = embudo;
    }

    public Boolean getFajasSogas() {
        return fajasSogas;
    }

    public void setFajasSogas(Boolean fajasSogas) {
        this.fajasSogas = fajasSogas;
    }

    public Boolean getPinPerno() {
        return pinPerno;
    }

    public void setPinPerno(Boolean pinPerno) {
        this.pinPerno = pinPerno;
    }

    public String getComentarios() {
        return comentarios;
    }

    public void setComentarios(String comentarios) {
        this.comentarios = comentarios;
    }

    public double precioDia;
    public Boolean varillaTierra;
    public Boolean cableElectrico;
    public Boolean tableroDistribucion;
    public Boolean extensionCaja;
    public Boolean bidonCombustible;
    public Boolean llaveHexagonal;
    public Boolean llavesPuertas;
    public Boolean tacoMadera ;
    public Boolean manguera;
    public Boolean embudo ;
    public Boolean fajasSogas;
    public Boolean pinPerno;
    public String comentarios ;
}
