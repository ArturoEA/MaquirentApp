package com.example.maquirentapp.strategy;

import android.content.Context;
import com.example.maquirentapp.BuildConfig;
import com.example.maquirentapp.Model.Cotizacion;

import java.util.ArrayList;
import java.util.List;

public class VolkerPdfStrategy extends BasePdfGeneratorStrategy {

    @Override
    public String generarEncabezadoHtml(Context context) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div style='text-align:left; margin-bottom:20px;'>");
        sb.append("<div><span style='color: #1A2B49; font-weight: 900; font-size: 16pt; font-family: sans-serif;'>")
          .append(getNombreEmpresa())
          .append("</span></div>");
        sb.append("<div class='color-encabezado'>");
        sb.append("<div style='display: flex; justify-content: space-between; align-items: center;'>");
        sb.append("<span>").append(getPropietario()).append("</span>");
        sb.append("<span>RUC: ").append(getRuc()).append("</span>");
        sb.append("</div>");
        sb.append("<div><span>").append(getRubro()).append("</span></div>");
        sb.append("<div><span>").append(getDireccion()).append(" | TELÉF. ").append(getTelefono()).append("</span></div>");
        sb.append("<hr>");
        sb.append("</div>");
        sb.append("</div>");
        return sb.toString();
    }

    @Override
    public String getTituloCondicionesComerciales() {
        return "CONDICIONES COMERCIALES VOLKER";
    }

    @Override
    public List<String> obtenerCondicionesPorDefecto(String tipoAlquiler, int horasMinimas, int diasAlquiler, int horasPorDia) {
        List<String> condiciones = new ArrayList<>();
        if (Cotizacion.TIPO_DIARIO.equalsIgnoreCase(tipoAlquiler)) {
            condiciones.add("LOS EQUIPOS SE ALQUILAN EN CONDICIONES OPERATIVAS Y CON MANTENIMIENTO AL DÍA.");
            condiciones.add("FORMA DE PAGO: CONTADO O CRÉDITO CONFORME A EVALUACIÓN PREVIA.");
            condiciones.add("TARIFA DIARIA BASADA EN UNA JORNADA DE " + (horasPorDia > 0 ? horasPorDia : 8) + " HORAS DIARIAS POR " + (diasAlquiler > 0 ? diasAlquiler : 1) + " DÍA(S). HORAS EXCEDENTES FACTURADAS SEGÚN TARIFA HE.");
            condiciones.add("FLETE DE MOVILIZACIÓN Y DESMOVILIZACIÓN A CARGO DEL CLIENTE.");
            condiciones.add("EL CLIENTE ASUME LA INSPECCIÓN DIARIA DE NIVELES DE ACEITE, AGUA Y COMBUSTIBLE.");
            condiciones.add("VALIDEZ DE LA OFERTA: 15 DÍAS CALENDARIO.");
        } else {
            condiciones.add("LOS EQUIPOS SE ALQUILAN EN CONDICIONES OPERATIVAS Y CON MANTENIMIENTO AL DÍA.");
            condiciones.add("FORMA DE PAGO: CRÉDITO A 30 DÍAS TRAS EMISIÓN DE LA VALORIZACIÓN.");
            condiciones.add("TARIFA BASE SUJETA A HORAS MÍNIMAS PACTADAS (" + (horasMinimas > 0 ? horasMinimas : 200) + " HORAS MENSUALES). EXCESOS FACTURADOS SEGÚN REGISTRO DE HORÓMETRO.");
            condiciones.add("EL CLIENTE ASUME LA INSPECCIÓN DIARIA DE NIVELES DE ACEITE, AGUA Y COMBUSTIBLE.");
            condiciones.add("MANTENIMIENTO PREVENTIVO A CARGO DE " + getNombreEmpresa() + " CADA " + BuildConfig.DEFAULT_MAINTENANCE_HOURS + " HORAS.");
            condiciones.add("VALIDEZ DE LA OFERTA: 15 DÍAS CALENDARIO.");
        }
        return condiciones;
    }

    @Override
    public List<String> getCuentasBancarias() {
        List<String> cuentas = new ArrayList<>();
        cuentas.add("Nº DE CUENTA CORRIENTE / BCP MONEDA SOLES: 191-98765432-0-11");
        cuentas.add("Nº DE CUENTA DE DETRACCIONES: 00-000-000000");
        cuentas.add("Nº DE CUENTA INTERBANCARIA (CCI) SOLES: 002-191-009876543200-11");
        cuentas.add("Nº DE CUENTA CORRIENTE / BBVA MONEDA DÓLARES: 0011-0123-0100012345");
        return cuentas;
    }

    @Override
    public String getTitularCuentas() {
        return "NOTA: A favor de " + BuildConfig.COMPANY_OWNER;
    }
}
