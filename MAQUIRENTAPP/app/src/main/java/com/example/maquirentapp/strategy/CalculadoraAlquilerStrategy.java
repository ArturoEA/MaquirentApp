package com.example.maquirentapp.strategy;

public interface CalculadoraAlquilerStrategy {
    /**
     * Calcula la tarifa por hora extra sugerida a partir del precio de alquiler mensual y horas pactadas.
     */
    double calcularPrecioHoraExtra(double precioAlquiler, int horasMinimas);

    /**
     * Calcula las horas trabajadas, prorrateo según periodo de días, horas extras netas y el monto total a cobrar.
     */
    ResultadoHorasExtras calcularHorasExtras(double horometroActual, double horometroAnterior, int horasMinimas, String fechaInicio, String fechaFin, double precioHoraExtra);

    /**
     * Calcula el monto del impuesto (IGV / IVA) sobre un subtotal dado.
     */
    double calcularImpuesto(double subtotal);

    /**
     * Calcula el monto total con impuesto incluido (Subtotal + Impuesto).
     */
    double calcularTotalConImpuesto(double subtotal);

    /**
     * Obtiene la tasa de impuesto activa (ej. 0.18 para 18%).
     */
    double getTasaImpuesto();

    /**
     * Obtiene el texto representativo del impuesto (ej. "I.G.V. (18%)" o "IVA (19%)").
     */
    String getEtiquetaImpuesto();

    /**
     * Obtiene el intervalo de horas estándar para mantenimiento preventivo (ej. 250 hrs).
     */
    int getIntervaloMantenimientoHoras();

    /**
     * Calcula el próximo horómetro de mantenimiento a partir del actual.
     */
    double calcularProximoMantenimiento(double horometroActual);
}
