package com.example.maquirentapp.strategy;

import com.example.maquirentapp.BuildConfig;
import java.util.Locale;

public class VolkerCorrelativoStrategy implements CorrelativoStrategy {

    @Override
    public String generarCodigoCotizacion(long numero) {
        return String.format(Locale.US, "%s%06d", BuildConfig.PREFIX_COTIZACION, numero);
    }

    @Override
    public String generarCodigoValorizacion(long numero) {
        return String.format(Locale.US, "%s%06d", BuildConfig.PREFIX_VALORIZACION, numero);
    }

    @Override
    public String generarCodigoCertificado(long numero) {
        return String.format(Locale.US, "%s%06d", BuildConfig.PREFIX_CERTIFICADO, numero);
    }
}
