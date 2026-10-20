package com.example.maquirentapp.Utils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CalendarioTareoHelper {

    public static class MesSpan {
        private final String nombreMes;
        private final int cantidadDias;

        public MesSpan(String nombreMes, int cantidadDias) {
            this.nombreMes = nombreMes;
            this.cantidadDias = cantidadDias;
        }

        public String getNombreMes() {
            return nombreMes;
        }

        public int getCantidadDias() {
            return cantidadDias;
        }
    }

    public static class TareoInfo {
        private final List<MesSpan> meses;
        private final List<Integer> diasDelMes;
        private final List<Integer> diasAcumulados;
        private final List<Boolean> esDomingo;
        private final int totalDias;

        public TareoInfo(List<MesSpan> meses, List<Integer> diasDelMes, List<Integer> diasAcumulados, List<Boolean> esDomingo, int totalDias) {
            this.meses = meses;
            this.diasDelMes = diasDelMes;
            this.diasAcumulados = diasAcumulados;
            this.esDomingo = esDomingo != null ? esDomingo : new ArrayList<>();
            this.totalDias = totalDias;
        }

        public List<MesSpan> getMeses() {
            return meses;
        }

        public List<Integer> getDiasDelMes() {
            return diasDelMes;
        }

        public List<Integer> getDiasAcumulados() {
            return diasAcumulados;
        }

        public List<Boolean> getEsDomingo() {
            return esDomingo;
        }

        public int getTotalDias() {
            return totalDias;
        }
    }

    public static TareoInfo generarTareo(String fechaInicioStr, String fechaFinStr) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        Calendar calInicio = Calendar.getInstance();
        Calendar calFin = Calendar.getInstance();

        try {
            Date dInicio = sdf.parse(fechaInicioStr);
            Date dFin = sdf.parse(fechaFinStr);
            if (dInicio != null) calInicio.setTime(dInicio);
            if (dFin != null) calFin.setTime(dFin);
        } catch (Exception e) {
            // Fallback a 30 días genéricos
            return generarFallback30Dias();
        }

        // Si la fecha final es menor que la inicial, fallback
        if (calFin.before(calInicio)) {
            return generarFallback30Dias();
        }

        List<MesSpan> meses = new ArrayList<>();
        List<Integer> diasDelMes = new ArrayList<>();
        List<Integer> diasAcumulados = new ArrayList<>();
        List<Boolean> esDomingo = new ArrayList<>();

        Locale localeEs = new Locale("es", "ES");
        SimpleDateFormat sdfMes = new SimpleDateFormat("MMMM", localeEs);

        Calendar cursor = (Calendar) calInicio.clone();
        String mesActual = "";
        int contadorMesActual = 0;
        int acumulado = 0;

        while (!cursor.after(calFin)) {
            acumulado++;
            int dia = cursor.get(Calendar.DAY_OF_MONTH);
            boolean esDom = (cursor.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY);
            String nombreMes = sdfMes.format(cursor.getTime());
            nombreMes = Character.toUpperCase(nombreMes.charAt(0)) + nombreMes.substring(1);

            diasDelMes.add(dia);
            diasAcumulados.add(acumulado);
            esDomingo.add(esDom);

            if (mesActual.isEmpty()) {
                mesActual = nombreMes;
                contadorMesActual = 1;
            } else if (mesActual.equalsIgnoreCase(nombreMes)) {
                contadorMesActual++;
            } else {
                meses.add(new MesSpan(mesActual, contadorMesActual));
                mesActual = nombreMes;
                contadorMesActual = 1;
            }

            cursor.add(Calendar.DAY_OF_MONTH, 1);
        }

        if (contadorMesActual > 0) {
            meses.add(new MesSpan(mesActual, contadorMesActual));
        }

        return new TareoInfo(meses, diasDelMes, diasAcumulados, esDomingo, acumulado);
    }

    private static TareoInfo generarFallback30Dias() {
        List<MesSpan> meses = new ArrayList<>();
        meses.add(new MesSpan("Mes", 30));
        List<Integer> dias = new ArrayList<>();
        List<Integer> acum = new ArrayList<>();
        List<Boolean> esDom = new ArrayList<>();
        for (int i = 1; i <= 30; i++) {
            dias.add(i);
            acum.add(i);
            esDom.add(i % 7 == 0);
        }
        return new TareoInfo(meses, dias, acum, esDom, 30);
    }
}
