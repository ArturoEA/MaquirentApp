package com.example.maquirentapp.Model;

public class AlquilerMensual {
    private int id;
    private int idGrupo;
    private String nombreCliente;
    private double horometroInicial;
    private String fechaInicial;
    private String ubicacion;
    private double horometroFinal;
    private String fechaFinal;
    private double precioAlquiler;
    private int horasMinimas;
    private boolean extintor9kg;
    private boolean extintor6kg;
    private boolean varillaTierra;
    private boolean bandejaAntiderrame;
    private boolean kitAntiderrame;
    private boolean cableElectrico;
    private boolean tableroDistribucion;
    private boolean carreta;

    public boolean isCarreta() {
        return carreta;
    }

    public void setCarreta(boolean carreta) {
        this.carreta = carreta;
    }

    public boolean isKitAntiderrame() {
        return kitAntiderrame;
    }

    public void setKitAntiderrame(boolean kitAntiderrame) {
        this.kitAntiderrame = kitAntiderrame;
    }

    public boolean isTableroDistribucion() {
        return tableroDistribucion;
    }

    public void setTableroDistribucion(boolean tableroDistribucion) {
        this.tableroDistribucion = tableroDistribucion;
    }

    public boolean isCableElectrico() {
        return cableElectrico;
    }

    public void setCableElectrico(boolean cableElectrico) {
        this.cableElectrico = cableElectrico;
    }

    public boolean isBandejaAntiderrame() {
        return bandejaAntiderrame;
    }

    public void setBandejaAntiderrame(boolean bandejaAntiderrame) {
        this.bandejaAntiderrame = bandejaAntiderrame;
    }

    public boolean isVarillaTierra() {
        return varillaTierra;
    }

    public void setVarillaTierra(boolean varilla) {
        this.varillaTierra = varilla;
    }

    public boolean isExtintor9kg() {
        return extintor9kg;
    }

    public void setExtintor9kg(boolean extintor9kg) {
        this.extintor9kg = extintor9kg;
    }
    public int getHorasMinimas() {
        return horasMinimas;
    }

    public void setHorasMinimas(int horasMinimas) {
        this.horasMinimas = horasMinimas;
    }

    public double getPrecioAlquiler() {
        return precioAlquiler;
    }

    public void setPrecioAlquiler(double precioAlquiler) {
        this.precioAlquiler = precioAlquiler;
    }

    public String getFechaFinal() {
        return fechaFinal;
    }

    public void setFechaFinal(String fechaFinal) {
        this.fechaFinal = fechaFinal;
    }

    public double getHorometroFinal() {
        return horometroFinal;
    }

    public void setHorometroFinal(double horometroFinal) {
        this.horometroFinal = horometroFinal;
    }

    public String getUbicacion() {
        return ubicacion;
    }

    public void setUbicacion(String ubicacion) {
        this.ubicacion = ubicacion;
    }

    public String getFechaInicial() {
        return fechaInicial;
    }

    public void setFechaInicial(String fechaInicial) {
        this.fechaInicial = fechaInicial;
    }

    public String getNombreCliente() {
        return nombreCliente;
    }

    public void setNombreCliente(String empresa) {
        this.nombreCliente = empresa;
    }

    public int getIdGrupo() {
        return idGrupo;
    }

    public void setIdGrupo(int idGrupo) {
        this.idGrupo = idGrupo;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public double getHorometroInicial() {
        return horometroInicial;
    }

    public void setHorometroInicial(double horometroInicial) {
        this.horometroInicial = horometroInicial;
    }

    public boolean isExtintor6kg() {
        return extintor6kg;
    }

    public void setExtintor6kg(boolean extintor6kg) {
        this.extintor6kg = extintor6kg;
    }
}
