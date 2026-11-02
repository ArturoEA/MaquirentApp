package com.example.maquirentapp.strategy;

public interface CorrelativoStrategy {
    /**
     * Genera el código correlativo de cotización por año (ej. COT-2026-001).
     */
    String generarCodigoCotizacion(long numero, int anio);

    /**
     * Genera el código correlativo de cotización (ej. COT-2026-001 usando el año actual).
     */
    String generarCodigoCotizacion(long numero);

    /**
     * Genera el código correlativo de valorización tradicional por número (ej. VAL-000123).
     */
    String generarCodigoValorizacion(long numero);

    /**
     * Genera el código correlativo de valorización con formato estándar: VAL-{grupo}-{MM}{yy}
     * Ej: VAL-111-1026 para grupo "GEP 105-111" y fechaInicio "22/10/2026".
     */
    String generarCodigoValorizacion(String codigoEquipo, String fechaInicio);

    /**
     * Genera el código correlativo de certificado de operatividad (ej. CERT-000123).
     */
    String generarCodigoCertificado(long numero);
}
