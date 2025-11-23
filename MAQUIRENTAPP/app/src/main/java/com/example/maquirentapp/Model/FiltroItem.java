package com.example.maquirentapp.Model;

public class FiltroItem {
    private String marca;
    private String codigo;

    public FiltroItem() {
    }

    public FiltroItem(String marca, String codigo) {
        this.marca = marca;
        this.codigo = codigo;
    }

    public String getMarca() {
        return marca;
    }

    public void setMarca(String marca) {
        this.marca = marca;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }
}