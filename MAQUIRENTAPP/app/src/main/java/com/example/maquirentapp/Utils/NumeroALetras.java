package com.example.maquirentapp.Utils;

import java.util.Locale;

public class NumeroALetras {

    private static final String[] UNIDADES = {
            "", "un", "dos", "tres", "cuatro", "cinco", "seis", "siete", "ocho", "nueve",
            "diez", "once", "doce", "trece", "catorce", "quince", "dieciséis", "diecisiete", "dieciocho", "diecinueve", "veinte"
    };

    private static final String[] DECENAS = {
            "", "", "", "treinta", "cuarenta", "cincuenta", "sesenta", "setenta", "ochenta", "noventa"
    };

    private static final String[] CENTENAS = {
            "", "ciento", "doscientos", "trescientos", "cuatrocientos", "quinientos", "seiscientos", "setecientos", "ochocientos", "novecientos"
    };

    public static String convertir(double monto, String moneda) {
        long parteEntera = (long) Math.floor(monto);
        long parteDecimal = Math.round((monto - parteEntera) * 100.0);
        if (parteDecimal >= 100) {
            parteEntera += 1;
            parteDecimal = 0;
        }

        String textoEntero = convertirNumero(parteEntera);
        if (textoEntero.isEmpty()) {
            textoEntero = "cero";
        }

        // Capitalizar la primera letra
        textoEntero = Character.toUpperCase(textoEntero.charAt(0)) + textoEntero.substring(1);

        String nombreMoneda = "soles";
        if (moneda != null) {
            String m = moneda.toUpperCase(Locale.ROOT);
            if (m.contains("USD") || m.contains("DOL")) {
                nombreMoneda = "dólares americanos";
            }
        }

        return String.format(Locale.US, "Son: %s y %02d/100 %s", textoEntero, parteDecimal, nombreMoneda);
    }

    private static String convertirNumero(long n) {
        if (n == 0) return "";
        if (n == 100) return "cien";

        if (n < 20) {
            return UNIDADES[(int) n];
        }

        if (n < 30) {
            switch ((int) n) {
                case 20: return "veinte";
                case 21: return "veintiuno";
                case 22: return "veintidós";
                case 23: return "veintitrés";
                case 24: return "veinticuatro";
                case 25: return "veinticinco";
                case 26: return "veintiséis";
                case 27: return "veintisiete";
                case 28: return "veintiocho";
                case 29: return "veintinueve";
            }
        }

        if (n < 100) {
            int d = (int) (n / 10);
            int u = (int) (n % 10);
            if (u == 0) return DECENAS[d];
            return DECENAS[d] + " y " + UNIDADES[u];
        }

        if (n < 1000) {
            int c = (int) (n / 100);
            long resto = n % 100;
            if (resto == 0) {
                if (c == 1) return "cien";
                return CENTENAS[c];
            }
            return CENTENAS[c] + " " + convertirNumero(resto);
        }

        if (n < 1000000) {
            long miles = n / 1000;
            long resto = n % 1000;
            String prefijoMiles = (miles == 1) ? "mil" : (convertirNumero(miles) + " mil");
            if (resto == 0) return prefijoMiles;
            return prefijoMiles + " " + convertirNumero(resto);
        }

        if (n < 1000000000L) {
            long millones = n / 1000000;
            long resto = n % 1000000;
            String prefijoMillones = (millones == 1) ? "un millón" : (convertirNumero(millones) + " millones");
            if (resto == 0) return prefijoMillones;
            return prefijoMillones + " " + convertirNumero(resto);
        }

        return String.valueOf(n);
    }
}
