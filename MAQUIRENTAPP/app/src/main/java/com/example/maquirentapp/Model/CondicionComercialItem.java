package com.example.maquirentapp.Model;

import java.io.Serializable;

public class CondicionComercialItem implements Serializable {
    private String texto;
    private boolean activo;

    public CondicionComercialItem() {
        this.activo = true;
    }

    public CondicionComercialItem(String texto, boolean activo) {
        this.texto = texto;
        this.activo = activo;
    }

    public String getTexto() {
        return texto != null ? texto : "";
    }

    public void setTexto(String texto) {
        this.texto = texto;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }
}
