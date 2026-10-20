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
    public String generarCodigoValorizacion(String codigoEquipo, String fechaInicio) {
        String numGrupo = "000";
        if (codigoEquipo != null && !codigoEquipo.trim().isEmpty()) {
            String s = codigoEquipo.trim();
            if (s.contains("-")) {
                numGrupo = s.substring(s.lastIndexOf('-') + 1).trim();
            } else if (s.contains(" ")) {
                numGrupo = s.substring(s.lastIndexOf(' ') + 1).trim();
            } else {
                numGrupo = s;
            }
        }

        String mesAnio = "0000";
        if (fechaInicio != null && !fechaInicio.trim().isEmpty()) {
            try {
                String[] partes = fechaInicio.split("[/\\-]");
                if (partes.length >= 3) {
                    int mesNum = Integer.parseInt(partes[1].trim());
                    String mes = String.format(Locale.US, "%02d", mesNum);
                    String anioStr = partes[2].trim();
                    if (anioStr.length() >= 4) {
                        anioStr = anioStr.substring(anioStr.length() - 2);
                    } else if (anioStr.length() == 1) {
                        anioStr = "0" + anioStr;
                    }
                    mesAnio = mes + anioStr;
                }
            } catch (Exception ignored) {
            }
        }
        return "VAL-" + numGrupo + "-" + mesAnio;
    }

    @Override
    public String generarCodigoCertificado(long numero) {
        return String.format(Locale.US, "%s%06d", BuildConfig.PREFIX_CERTIFICADO, numero);
    }
}
