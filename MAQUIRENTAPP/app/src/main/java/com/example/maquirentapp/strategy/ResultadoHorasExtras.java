package com.example.maquirentapp.strategy;

import java.io.Serializable;

public class ResultadoHorasExtras implements Serializable {
    private final double horasTrabajadas;
    private final double horasPermitidas;
    private final double horasExtras;
    private final double precioTotalHorasExtras;

    public ResultadoHorasExtras(double horasTrabajadas, double horasPermitidas, double horasExtras, double precioTotalHorasExtras) {
        this.horasTrabajadas = horasTrabajadas;
        this.horasPermitidas = horasPermitidas;
        this.horasExtras = horasExtras;
        this.precioTotalHorasExtras = precioTotalHorasExtras;
    }

    public double getHorasTrabajadas() {
        return horasTrabajadas;
    }

    public double getHorasPermitidas() {
        return horasPermitidas;
    }

    public double getHorasExtras() {
        return horasExtras;
    }

    public double getPrecioTotalHorasExtras() {
        return precioTotalHorasExtras;
    }
}
