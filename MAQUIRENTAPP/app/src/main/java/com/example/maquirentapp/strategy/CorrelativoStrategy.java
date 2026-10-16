package com.example.maquirentapp.strategy;

public interface CorrelativoStrategy {
    /**
     * Genera el código correlativo de cotización (ej. COT-000123).
     */
    String generarCodigoCotizacion(long numero);

    /**
     * Genera el código correlativo de valorización (ej. VAL-000123).
     */
    String generarCodigoValorizacion(long numero);

    /**
     * Genera el código correlativo de certificado de operatividad (ej. CERT-000123).
     */
    String generarCodigoCertificado(long numero);
}
