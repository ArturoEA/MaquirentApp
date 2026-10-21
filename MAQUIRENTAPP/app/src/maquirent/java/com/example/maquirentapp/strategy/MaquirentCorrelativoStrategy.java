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
    public String generarCodigoValorizacion(String codigoEquipo, String fechaInicio) {
        String numGrupo = "000";
        if (codigoEquipo != null && !codigoEquipo.trim().isEmpty()) {
            java.util.List<String> numeros = new java.util.ArrayList<>();
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\d+").matcher(codigoEquipo);
            while (m.find()) {
                numeros.add(m.group());
            }
            if (numeros.size() >= 2) {
                numGrupo = numeros.get(1); // El segundo número (ej. en GEP 160-101 toma 101, en GEP 105-111 toma 111)
            } else if (numeros.size() == 1) {
                numGrupo = numeros.get(0);
            }
        }

        String mesAnio = "0000";
        if (fechaInicio != null && !fechaInicio.trim().isEmpty()) {
            try {
                String[] partes = fechaInicio.split("[/\\-]");
                if (partes.length >= 3) {
                    int mesNum = 0;
                    String anioStr = "";
                    if (partes[0].length() == 4) {
                        anioStr = partes[0].trim();
                        mesNum = Integer.parseInt(partes[1].trim());
                    } else {
                        mesNum = Integer.parseInt(partes[1].trim());
                        anioStr = partes[2].trim();
                    }
                    String mes = String.format(Locale.US, "%02d", mesNum);
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
        return String.format(Locale.US, "CERT-%06d", numero);
    }
}
