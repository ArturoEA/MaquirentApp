package com.example.maquirentapp.strategy;

import android.content.Context;
import com.example.maquirentapp.BuildConfig;
import com.example.maquirentapp.Model.Cotizacion;

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
    public String generarCondicionesComercialesHtml(Cotizacion cotizacion) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div style='margin-top: 20px; text-align: left;'>");
        sb.append("<h3 style='text-decoration: underline; font-size: 11pt;'>CONDICIONES COMERCIALES VOLKER</h3>");
        sb.append("<ul style='padding-left: 20px; list-style-type: disc;'>");
        sb.append("<li>LOS EQUIPOS SE ALQUILAN EN CONDICIONES OPERATIVAS Y CON MANTENIMIENTO AL DÍA.</li>");
        sb.append("<li>FORMA DE PAGO: CRÉDITO A 30 DÍAS TRAS EMISIÓN DE LA VALORIZACIÓN.</li>");
        sb.append("<li>TARIFA BASE SUJETA A HORAS MÍNIMAS PACTADAS (<b>").append(cotizacion.getHorasMinimas()).append(" HORAS MENSUALES</b>). EXCESOS FACTURADOS SEGÚN REGISTRO DE HORÓMETRO.</li>");
        sb.append("<li>EL CLIENTE ASUME LA INSPECCIÓN DIARIA DE NIVELES DE ACEITE, AGUA Y COMBUSTIBLE.</li>");
        sb.append("<li>MANTENIMIENTO PREVENTIVO A CARGO DE ").append(getNombreEmpresa()).append(" CADA ").append(BuildConfig.DEFAULT_MAINTENANCE_HOURS).append(" HORAS.</li>");
        sb.append("<li>VALIDEZ DE LA OFERTA: 15 DÍAS CALENDARIO.</li>");
        sb.append("</ul>");
        sb.append("</div>");
        return sb.toString();
    }
}
