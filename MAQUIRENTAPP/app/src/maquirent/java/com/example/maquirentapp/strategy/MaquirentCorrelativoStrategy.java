package com.example.maquirentapp.strategy;

import java.util.Locale;

public class MaquirentCorrelativoStrategy implements CorrelativoStrategy {

    @Override
    public String generarCodigoCotizacion(long numero) {
        return String.format(Locale.US, "COT-%06d", numero);
    }

    @Override
    public String generarCodigoValorizacion(long numero) {
        return String.format(Locale.US, "VAL-%06d", numero);
    }

    @Override
    public String generarCodigoCertificado(long numero) {
        return String.format(Locale.US, "CERT-%06d", numero);
    }
}
