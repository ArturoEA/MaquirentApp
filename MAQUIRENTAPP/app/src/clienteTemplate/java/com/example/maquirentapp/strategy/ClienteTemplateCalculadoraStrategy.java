package com.example.maquirentapp.strategy;

import com.example.maquirentapp.BuildConfig;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ClienteTemplateCalculadoraStrategy implements CalculadoraAlquilerStrategy {

    private static final double DIAS_MES_ESTANDAR = 30.0;

    @Override
    public double calcularPrecioHoraExtra(double precioAlquiler, int horasMinimas) {
        if (precioAlquiler <= 0) return 0;
        int horasBase = horasMinimas > 0 ? horasMinimas : 200;
        // Tarifa por hora proporcional estándar
        double precioHE = precioAlquiler / (double) horasBase;
        return Math.round(precioHE * 100.0) / 100.0;
    }

    @Override
    public ResultadoHorasExtras calcularHorasExtras(double horometroActual, double horometroAnterior, int horasMinimas, String fechaInicio, String fechaFin, double precioHoraExtra) {
        if (horometroActual < horometroAnterior) {
            return new ResultadoHorasExtras(0, 0, 0, 0);
        }

        double horasTrabajadas = horometroActual - horometroAnterior;
        long diasPeriodo = calcularDiasEntreFechas(fechaInicio, fechaFin);

        double baseHoras = horasMinimas > 0 ? horasMinimas : 200.0;
        double horasPermitidas;
        if (diasPeriodo <= 0 || diasPeriodo >= 30) {
            horasPermitidas = baseHoras;
        } else {
            horasPermitidas = (baseHoras / DIAS_MES_ESTANDAR) * diasPeriodo;
        }

        double horasExtras = Math.max(0.0, horasTrabajadas - horasPermitidas);
        horasExtras = Math.round(horasExtras * 10.0) / 10.0;

        double precioTotalHE = horasExtras * precioHoraExtra;
        precioTotalHE = Math.round(precioTotalHE * 100.0) / 100.0;

        return new ResultadoHorasExtras(horasTrabajadas, horasPermitidas, horasExtras, precioTotalHE);
    }

    @Override
    public double calcularImpuesto(double subtotal) {
        return Math.round(subtotal * BuildConfig.TAX_RATE * 100.0) / 100.0;
    }

    @Override
    public double calcularTotalConImpuesto(double subtotal) {
        return Math.round(subtotal * (1.0 + BuildConfig.TAX_RATE) * 100.0) / 100.0;
    }

    @Override
    public double getTasaImpuesto() {
        return BuildConfig.TAX_RATE;
    }

    @Override
    public String getEtiquetaImpuesto() {
        return BuildConfig.TAX_LABEL;
    }

    @Override
    public int getIntervaloMantenimientoHoras() {
        return BuildConfig.DEFAULT_MAINTENANCE_HOURS;
    }

    @Override
    public double calcularProximoMantenimiento(double horometroActual) {
        return horometroActual + BuildConfig.DEFAULT_MAINTENANCE_HOURS;
    }

    private long calcularDiasEntreFechas(String inicio, String fin) {
        try {
            if (inicio == null || fin == null || inicio.isEmpty() || fin.isEmpty()) return 30;
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Date d1 = sdf.parse(inicio);
            Date d2 = sdf.parse(fin);
            if (d1 == null || d2 == null) return 30;
            long diff = d2.getTime() - d1.getTime();
            return (diff / (1000 * 60 * 60 * 24)) + 1;
        } catch (Exception e) {
            return 30;
        }
    }
}
