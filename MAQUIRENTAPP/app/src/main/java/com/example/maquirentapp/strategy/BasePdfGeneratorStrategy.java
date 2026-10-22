package com.example.maquirentapp.strategy;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.graphics.pdf.PdfRenderer;
import android.os.Environment;
import android.os.ParcelFileDescriptor;

import androidx.core.content.res.ResourcesCompat;

import com.example.maquirentapp.BuildConfig;
import com.example.maquirentapp.Model.CertificadoOperatividad;
import com.example.maquirentapp.Model.Cotizacion;
import com.example.maquirentapp.Model.InfoPlaca;
import com.example.maquirentapp.Model.ItemCotizacion;
import com.example.maquirentapp.Model.Mantenimiento;
import com.example.maquirentapp.Model.Valorizacion;
import com.example.maquirentapp.R;
import com.example.maquirentapp.Utils.CalendarioTareoHelper;
import com.example.maquirentapp.Utils.ImageUtils;
import com.example.maquirentapp.Utils.InformePdfVectorialGenerator;
import com.example.maquirentapp.Utils.PdfGenerator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public abstract class BasePdfGeneratorStrategy implements PdfGeneratorStrategy {

    public String getNombreEmpresa() {
        return BuildConfig.COMPANY_NAME;
    }

    public String getRuc() {
        return BuildConfig.COMPANY_RUC;
    }

    public String getPropietario() {
        return BuildConfig.COMPANY_OWNER;
    }

    public String getDireccion() {
        return BuildConfig.COMPANY_ADDRESS;
    }

    public String getTelefono() {
        return BuildConfig.COMPANY_PHONE;
    }

    public String getCiudad() {
        return BuildConfig.COMPANY_CITY;
    }

    public String getRubro() {
        return BuildConfig.COMPANY_BUSINESS_LINE;
    }

    public double getTasaImpuesto() {
        return BuildConfig.TAX_RATE;
    }

    public String getEtiquetaImpuesto() {
        return BuildConfig.TAX_LABEL;
    }

    public int getFirmaDrawableResId() {
        return R.drawable.firma_representante;
    }

    @Override
    public List<String> getCuentasBancarias() {
        List<String> cuentas = new ArrayList<>();
        cuentas.add("Nº DE CUENTA DE AHORROS / BCP MONEDA SOLES  245-21165261077");
        cuentas.add("Nº DE CUENTA DE DETRACCIONES  00761027077");
        cuentas.add("Nº DE CUENTA DE AHORROS / SCOTIABANK  MONEDA SOLES  722- 7753048");
        cuentas.add("Nº DE CUENTA DE AHORROS / BBVA MONEDA SOLES 0011-0248-0200277022");
        cuentas.add("Nº DE CUENTA DE AHORROS / BBVA MONEDA DÓLARES 0011-0248-0200277030");
        return cuentas;
    }

    @Override
    public String getTitularCuentas() {
        return "NOTA:  A favor de " + BuildConfig.COMPANY_OWNER;
    }

    public abstract String generarEncabezadoHtml(Context context);

    public String getTituloCondicionesComerciales() {
        return "CONDICIONES COMERCIALES";
    }

    @Override
    public List<String> obtenerCondicionesPorDefecto(String tipoAlquiler, int horasMinimas, int diasAlquiler, int horasPorDia) {
        List<String> condiciones = new ArrayList<>();
        if (Cotizacion.TIPO_DIARIO.equalsIgnoreCase(tipoAlquiler)) {
            condiciones.add("LOS EQUIPOS SE ALQUILAN SIN COMBUSTIBLE NI OPERADOR.");
            condiciones.add("FORMA DE PAGO: AL CONTADO / PREVIO ACUERDO COMERCIAL ANTES DE LA ENTREGA.");
            condiciones.add("TARIFA DIARIA POR JORNADA DE HASTA <b>" + (horasPorDia > 0 ? horasPorDia : 8) + " HORAS DIARIAS</b> POR <b>" + (diasAlquiler > 0 ? diasAlquiler : 1) + " DÍA(S)</b>. EXCESOS SE FACTURARÁN COMO HORA EXTRA.");
            condiciones.add("TRANSPORTE Y TRASLADO DE EQUIPOS A CARGO DEL CLIENTE.");
            condiciones.add("LOS EQUIPOS DEBEN SER DEVUELTOS EN LAS MISMAS CONDICIONES EN QUE FUERON ENTREGADOS SIN MÁS DETERIORO DEL QUE PUEDE PRODUCIRSE POR EL USO NORMAL.");
            condiciones.add("EN CASO DE PÉRDIDA O DAÑOS DE LOS EQUIPOS, EL CLIENTE SE HARÁ RESPONSABLE DE LA REPARACIÓN O REPOSICIÓN DEL MISMO.");
            condiciones.add("LA INSPECCIÓN DIARIA ES RESPONSABILIDAD DEL CLIENTE (NIVELES DE AGUA Y ACEITE DE MOTOR).");
            condiciones.add("LOS ADITAMENTOS DEL EQUIPO INCLUIDOS SON BANDEJA Y KIT ANTIDERRAME, EXTINTOR, BARRA PUESTA A TIERRA.");
            condiciones.add("LA PRESENTE COTIZACIÓN TIENE UNA VALIDEZ DE 15 DÍAS DESDE SU CREACIÓN.");
        } else {
            condiciones.add("LOS EQUIPOS SE ALQUILAN SIN COMBUSTIBLE NI OPERADOR.");
            condiciones.add("EL PAGO DEL ALQUILER SE REALIZARÁ A 30 DÍAS DE LA FACTURACIÓN.");
            condiciones.add("TARIFA SE APLICA CON HORAS MÍNIMAS DE <b>" + (horasMinimas > 0 ? horasMinimas : 200) + " HORAS MENSUALES</b>, EXCESOS SE FACTURARÁN SEGÚN HORÓMETRO.");
            condiciones.add("LOS EQUIPOS DEBEN SER DEVUELTOS EN LAS MISMAS CONDICIONES EN QUE FUERON ENTREGADOS SIN MÁS DETERIORO DEL QUE PUEDE PRODUCIRSE POR EL USO NORMAL. EN CASO DE PRESENTARSE CUALQUIER DAÑO, EL CLIENTE SE HARÁ RESPONSABLE DE LAS REPARACIONES, SEGÚN LOS CARGOS POR DAÑOS VIGENTES A LA DEVOLUCIÓN.");
            condiciones.add("EN CASO DE PÉRDIDA DE LOS EQUIPOS, EL CLIENTE SE HARÁ RESPONSABLE DE LA DEVOLUCIÓN DEL MISMO, YA SEA COMO EQUIPO O EN EFECTIVO, TENIENDO EN CUENTA LA FACTURA DE COMPRA.");
            condiciones.add("LA INSPECCIÓN DIARIA ES RESPONSABILIDAD DEL CLIENTE (AGUA Y ACEITE DE MOTOR).");
            condiciones.add("LOS ADITAMENTOS DEL EQUIPO INCLUIDOS SON BANDEJA Y KIT ANTIDERRAME, EXTINTOR, BARRA PUESTA A TIERRA.");
            condiciones.add("EL EQUIPO SE ENTREGA CON ACEITE DE MOTOR Y FILTROS NUEVOS.");
            condiciones.add("PARA LOS MANTENIMIENTOS DE LOS GENERADORES EN MINA, SE PUEDE CAPACITAR A UN PERSONAL DE LA EMPRESA CONTRATISTA Y SE LE OTORGARÁ TODOS LOS INSUMOS PARA QUE PUEDA REALIZAR EL MANTENIMIENTO PREVENTIVO CADA 250 HORAS.");
            condiciones.add("LA PRESENTE COTIZACIÓN TIENE UNA VALIDEZ DE 15 DÍAS DESDE SU CREACIÓN.");
        }
        return condiciones;
    }

    public String generarCondicionesComercialesHtml(Cotizacion cotizacion) {
        List<String> condiciones = cotizacion.getCondicionesComerciales();
        if (condiciones == null || condiciones.isEmpty()) {
            condiciones = obtenerCondicionesPorDefecto(
                    cotizacion.getTipoAlquiler(),
                    cotizacion.getHorasMinimas(),
                    cotizacion.getDiasAlquiler(),
                    cotizacion.getHorasPorDia()
            );
        }

        StringBuilder sb = new StringBuilder();
        sb.append("<div style='margin-top: 20px; text-align: left;'>");
        sb.append("<h3 style='text-decoration: underline; font-size: 11pt;'>").append(getTituloCondicionesComerciales()).append("</h3>");
        sb.append("<ul style='padding-left: 20px; list-style-type: disc;'>");
        for (String cond : condiciones) {
            sb.append("<li>").append(cond).append("</li>");
        }
        sb.append("</ul>");
        sb.append("</div>");
        return sb.toString();
    }

    @Override
    public String generarHtmlCotizacion(Context context, Cotizacion cotizacion) {
        String firmaBase64 = "";
        try {
            firmaBase64 = "data:image/png;base64," + ImageUtils.convertirDrawableABase64(context, getFirmaDrawableResId());
        } catch (Exception e) {
            firmaBase64 = "";
        }

        boolean esDiario = cotizacion.isDiario();
        int dias = cotizacion.getDiasAlquiler();

        StringBuilder html = new StringBuilder();
        html.append("<html><head><style>");
        html.append("@page { size: A4; margin: 20mm 15mm 20mm 15mm; }");
        html.append("body { font-family: 'Times New Roman', 'Arial', sans-serif; font-size: 10pt; color: #000; line-height: 1.3; padding: 60px;}");
        html.append("thead { display: table-header-group; }");
        html.append("tfoot { display: table-footer-group; }");
        html.append("tr { page-break-inside: avoid; }");
        html.append("table { width: 100%; border-collapse: collapse; margin-top: 10px; margin-bottom: 10px; }");
        html.append("th { background-color: #D9D9D9; font-weight: bold; text-align: center; border: 1px solid #000; padding: 4px; font-size: 9pt; }");
        html.append("td { border: 1px solid #000; padding: 4px; font-size: 9pt; vertical-align: middle; text-align: center; }");
        html.append(".td-left { text-align: left; }");
        html.append(".no-border { border: none; }");
        html.append(".titulo-maqui { color: #ff8c8c; font-weight: 900; font-size: 14pt; font-family: 'Arial Black', Arial, sans-serif; }");
        html.append(".titulo-rent { color: #80b6e0; font-weight: 900; font-size: 14pt; font-family: 'Arial Black', Arial, sans-serif; }");
        html.append(".color-encabezado { color: #818080 }");
        html.append("</style></head><body>");

        html.append("<table><tbody><tr><td class='no-border' style='width:100%; text-align:left;'>");

        html.append(generarEncabezadoHtml(context));

        html.append("<div style='display: flex; justify-content: space-between; margin-bottom: 15px;'>");
        html.append("<div><b>COTIZACIÓN:</b> ").append(cotizacion.getNumeroCotizacion()).append("</div>");
        html.append("<div><b>FECHA:</b> ").append(cotizacion.getFechaEmision() != null ? cotizacion.getFechaEmision() : "").append("</div>");
        html.append("</div>");

        html.append("<div style='margin-bottom: 15px;'>");
        html.append("<b>SEÑORES:</b> ").append(cotizacion.getClienteNombre() != null ? cotizacion.getClienteNombre() : "").append("<br>");
        html.append("<b>RUC:</b> ").append(cotizacion.getClienteRuc() != null ? cotizacion.getClienteRuc() : "").append("<br>");
        html.append("<b>LUGAR DE TRABAJO:</b> ").append(cotizacion.getLugarTrabajo() != null ? cotizacion.getLugarTrabajo() : "");
        html.append("</div>");

        html.append("<p>Estimados Señores:<br>Por medio de la presente nos es muy grato hacerles llegar nuestra cotización comercial de alquiler según el siguiente detalle:</p>");

        html.append("<table><thead><tr>");
        if (esDiario) {
            html.append("<th>ITEM</th><th>DESCRIPCIÓN</th><th>POTENCIA</th><th>TARIFA DÍA</th><th>CANT. DÍAS</th><th>HORAS/DÍA</th><th>PRECIO HE</th><th>SUBTOTAL</th>");
        } else {
            html.append("<th>ITEM</th><th>DESCRIPCIÓN</th><th>POTENCIA</th><th>TARIFA MES</th><th>HORAS MÍN.</th><th>PRECIO HE</th><th>SUBTOTAL</th>");
        }
        html.append("</tr></thead><tbody>");

        int index = 1;
        for (ItemCotizacion item : cotizacion.getItems()) {
            double subtotalItem = item.getSubtotal(cotizacion.getTipoAlquiler(), dias);
            html.append("<tr>");
            html.append("<td>").append(index++).append("</td>");
            html.append("<td class='td-left'>").append(item.getDescripcionEquipo() != null ? item.getDescripcionEquipo() : "").append("</td>");
            html.append("<td>").append(item.getPotencia() != null ? item.getPotencia() : "-").append("</td>");
            html.append("<td>").append(String.format(Locale.US, "%.2f", item.getPrecioUnitario())).append("</td>");

            if (esDiario) {
                html.append("<td>").append(dias).append("</td>");
                html.append("<td>").append(cotizacion.getHorasPorDia()).append(" hrs</td>");
            } else {
                html.append("<td>").append(cotizacion.getHorasMinimas()).append("</td>");
            }

            html.append("<td>").append(String.format(Locale.US, "%.2f", item.getPrecioHoraExtra())).append("</td>");
            html.append("<td>").append(String.format(Locale.US, "%.2f", subtotalItem)).append("</td>");
            html.append("</tr>");
        }

        int colspan = esDiario ? 7 : 6;
        html.append("<tr>");
        html.append("<td colspan='").append(colspan).append("' class='td-left no-border'><b>SUBTOTAL (").append(cotizacion.getMoneda()).append(")</b></td>");
        html.append("<td><b>").append(String.format(Locale.US, "%.2f", cotizacion.getSubtotalGlobal())).append("</b></td>");
        html.append("</tr>");
        html.append("<tr>");
        html.append("<td colspan='").append(colspan).append("' class='td-left no-border'><b>").append(getEtiquetaImpuesto()).append("</b></td>");
        html.append("<td><b>").append(String.format(Locale.US, "%.2f", cotizacion.getIgvGlobal())).append("</b></td>");
        html.append("</tr>");
        html.append("<tr>");
        html.append("<td colspan='").append(colspan).append("' class='td-left no-border'><b>TOTAL GENERAL (").append(cotizacion.getMoneda()).append(")</b></td>");
        html.append("<td><b>").append(String.format(Locale.US, "%.2f", cotizacion.getTotalGlobal())).append("</b></td>");
        html.append("</tr>");
        html.append("</tbody></table>");

        html.append(generarCondicionesComercialesHtml(cotizacion));

        if (!firmaBase64.isEmpty()) {
            html.append("<div style='margin-top: 40px; text-align: center; width: 250px;'>");
            html.append("<img src='").append(firmaBase64).append("' style='max-width: 150px; max-height: 80px;'><br>");
            html.append("<hr style='border: 0; border-top: 1px solid #000; margin: 2px 0;'>");
            html.append("<b>").append(getPropietario()).append("</b><br>");
            html.append("<b>").append(getNombreEmpresa()).append("</b>");
            html.append("</div>");
        } else {
            html.append("<div style='margin-top: 50px; text-align: left;'>");
            html.append("<p>Atentamente,</p>");
            html.append("<p><b>").append(getNombreEmpresa()).append("</b></p>");
            html.append("</div>");
        }

        html.append("</td></tr></tbody></table>");
        html.append("</body></html>");

        return html.toString();
    }

    @Override
    public String generarHtmlValorizacion(Context context, Valorizacion val) {
        CalendarioTareoHelper.TareoInfo tareo = CalendarioTareoHelper.generarTareo(val.getFechaInicio(), val.getFechaFin());
        String monedaSimbolo = (val.getMoneda() != null && val.getMoneda().contains("USD")) ? "US$" : "S/.";

        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html><head><meta charset='utf-8'>");
        sb.append("<style>");
        sb.append("@page { size: A4 portrait; margin: 10mm 12mm 10mm 12mm; }");
        sb.append("body { font-family: Arial, Helvetica, sans-serif; font-size: 8.5pt; color: #000; margin: 0; padding: 20px; line-height: 1.25; }");
        sb.append(".header-box { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 8px; border-bottom: 2px solid #000; padding-bottom: 6px; }");
        sb.append(".empresa-info { font-size: 8pt; line-height: 1.3; }");
        sb.append(".empresa-nombre { font-size: 11pt; font-weight: bold; }");
        sb.append(".titulo-doc-box { text-align: right; }");
        sb.append(".titulo-doc { font-size: 11pt; font-weight: bold; }");
        sb.append(".codigo-doc { font-size: 11pt; font-weight: bold; color: #1a365d; margin-top: 3px; }");
        sb.append(".info-table { width: 100%; border-collapse: collapse; margin-bottom: 10px; font-size: 8pt; }");
        sb.append(".info-table td { padding: 2px 4px; }");
        sb.append(".info-label { font-weight: bold; width: 15%; }");
        sb.append(".tareo-table { width: 100%; border-collapse: collapse; text-align: center; font-size: 6.5pt; margin-bottom: 10px; }");
        sb.append(".tareo-table th, .tareo-table td { border: 1px solid #000; padding: 2px 1px; }");
        sb.append(".tareo-header-mes { background-color: #E2E8F0; font-weight: bold; font-size: 7pt; }");
        sb.append(".tareo-col-label { text-align: left; padding-left: 4px; font-weight: bold; width: 120px; }");
        sb.append(".finance-box { display: flex; justify-content: space-between; align-items: flex-start; margin-top: 8px; margin-bottom: 8px; }");
        sb.append(".table-horas { width: 48%; border-collapse: collapse; font-size: 7.5pt; }");
        sb.append(".table-horas td { padding: 2px 4px; border-bottom: 1px solid #e2e8f0; }");
        sb.append(".table-totales { width: 48%; border-collapse: collapse; font-size: 8pt; margin-left: auto; }");
        sb.append(".table-totales td { padding: 2.5px 4px; }");
        sb.append(".row-total { font-weight: bold; font-size: 9pt; border-top: 1px solid #000; border-bottom: 2px solid #000; }");
        sb.append(".son-letras { font-size: 8pt; font-weight: bold; font-style: italic; margin-top: 6px; margin-bottom: 10px; }");
        sb.append(".cuentas-box { font-size: 7pt; border-top: 1px solid #000; padding-top: 6px; margin-top: 6px; }");
        sb.append(".cuentas-box div { margin-bottom: 2px; }");
        sb.append("</style></head><body>");

        // 1. Cabecera
        sb.append("<div class='header-box'>");
        sb.append("<div class='empresa-info'>");
        sb.append("<div class='empresa-nombre'>").append(getNombreEmpresa()).append("</div>");
        sb.append("<div>RUC: ").append(getRuc()).append(" | ").append(getDireccion()).append("</div>");
        sb.append("<div>").append(getRubro()).append("</div>");
        sb.append("<div>Teléfonos: ").append(getTelefono()).append("</div>");
        sb.append("</div>");
        sb.append("<div class='titulo-doc-box'>");
        sb.append("<div class='titulo-doc'>VALORIZACIÓN DE EQUIPO EN ALQUILER</div>");
        sb.append("<div class='codigo-doc'>").append(val.getNumeroValorizacion() != null ? val.getNumeroValorizacion() : "-").append("</div>");
        sb.append("</div></div>");

        // 2. Datos del Cliente y Equipo
        sb.append("<table class='info-table'>");
        sb.append("<tr>");
        sb.append("<td class='info-label'>CLIENTE:</td><td>").append(val.getNombreCliente() != null ? val.getNombreCliente() : "-").append("</td>");
        sb.append("<td class='info-label' style='width:10%;'>RUC:</td><td>").append(val.getClienteRuc() != null ? val.getClienteRuc() : "-").append("</td>");
        sb.append("</tr>");
        sb.append("<tr>");
        sb.append("<td class='info-label'>DIRECCIÓN:</td><td>").append(val.getClienteDireccion() != null ? val.getClienteDireccion() : "-").append("</td>");
        sb.append("<td class='info-label'>FECHA:</td><td>").append(val.getFechaEmision() != null ? val.getFechaEmision() : "-").append("</td>");
        sb.append("</tr>");
        sb.append("<tr>");
        sb.append("<td class='info-label'>EQUIPO:</td><td colspan='3'>").append(val.getDescripcionEquipo() != null ? val.getDescripcionEquipo() : "-").append("</td>");
        sb.append("</tr>");
        sb.append("<tr>");
        sb.append("<td class='info-label'>MARCA:</td><td colspan='3'>").append(val.getMarcaEquipo() != null && !val.getMarcaEquipo().isEmpty() ? val.getMarcaEquipo() : "MODASA").append("</td>");
        sb.append("</tr>");
        sb.append("<tr>");
        sb.append("<td class='info-label'>LUGAR DE TRABAJO:</td><td colspan='3'>").append(val.getUbicacionTrabajo() != null ? val.getUbicacionTrabajo() : "-").append("</td>");
        sb.append("</tr></table>");

        // 3. Matriz de Tareo (Calendario)
        sb.append("<table class='tareo-table'>");
        // Fila Meses
        sb.append("<tr><th class='tareo-col-label'></th>");
        for (CalendarioTareoHelper.MesSpan mes : tareo.getMeses()) {
            sb.append("<th class='tareo-header-mes' colspan='").append(mes.getCantidadDias()).append("'>").append(mes.getNombreMes()).append("</th>");
        }
        sb.append("</tr>");
        // Fila Días del mes
        sb.append("<tr><th class='tareo-col-label'>EQUIPO &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; DÍAS</th>");
        for (int dia : tareo.getDiasDelMes()) {
            sb.append("<th>").append(dia).append("</th>");
        }
        sb.append("</tr>");
        // Fila Asistencia Equipo (unos)
        sb.append("<tr><td class='tareo-col-label'>").append(val.getCodigoEquipo() != null && !val.getCodigoEquipo().isEmpty() ? val.getCodigoEquipo() : "EQUIPO").append("</td>");
        for (int i = 0; i < tareo.getTotalDias(); i++) {
            sb.append("<td>1</td>");
        }
        sb.append("</tr>");
        // Fila Acumulado
        sb.append("<tr><td class='tareo-col-label'>Días trabajados</td>");
        for (int acum : tareo.getDiasAcumulados()) {
            sb.append("<td>").append(acum).append("</td>");
        }
        sb.append("</tr></table>");

        // 4. Bloque Financiero
        sb.append("<div class='finance-container'>");

        if (val.isConHorasExtras()) {
            // Tabla de Horas y Horas Extras (Izquierda)
            sb.append("<table class='table-horas'>");
            sb.append("<tr><th colspan='2' style='text-align:left; background:#e2e8f0; padding:3px 4px;'>DETALLE DE HORAS</th><th colspan='2' style='text-align:left; background:#e2e8f0; padding:3px 4px;'>COSTO HORA EXTRA*</th></tr>");
            sb.append("<tr><td>HORÓMETRO INICIAL</td><td style='text-align:right;'><b>").append(String.format(Locale.US, "%.1f", val.getHorometroInicial())).append("</b></td><td>COSTO HORA EXTRA*</td><td style='text-align:right;'>").append(monedaSimbolo).append(" ").append(String.format(Locale.US, "%.2f", val.getCostoHoraExtra())).append("</td></tr>");
            sb.append("<tr><td>HORÓMETRO FINAL</td><td style='text-align:right;'><b>").append(String.format(Locale.US, "%.1f", val.getHorometroFinal())).append("</b></td><td>TOTAL HORAS EXTRAS</td><td style='text-align:right;'><b>").append(monedaSimbolo).append(" ").append(String.format(Locale.US, "%.2f", val.getTotalHorasExtras())).append("</b></td></tr>");
            sb.append("<tr><td>HORAS PERMITIDAS</td><td style='text-align:right;'>").append(String.format(Locale.US, "%.1f", val.getHorasPermitidas())).append("</td><td colspan='2' rowspan='3' style='font-size:6.5pt; color:#4a5568; vertical-align:top; padding:4px;'>*El precio de la hora extra se calcula aplicando un descuento del 25% sobre la tarifa de la hora base.</td></tr>");
            sb.append("<tr><td>HORAS USADAS</td><td style='text-align:right;'><b>").append(String.format(Locale.US, "%.1f", val.getHorasUsadas())).append("</b></td></tr>");
            sb.append("<tr><td>HORAS EXTRAS</td><td style='text-align:right; color:#c53030;'><b>").append(String.format(Locale.US, "%.1f", val.getHorasExtras())).append("</b></td></tr>");
            sb.append("</table>");
        } else {
            // Espacio vacío para alinear a la derecha
            sb.append("<div style='width:45%;'></div>");
        }

        // Tabla de Totales (Derecha)
        sb.append("<table class='table-totales'>");
        sb.append("<tr><th colspan='2' style='text-align:left; background:#e2e8f0; padding:3px 4px;'>").append(val.getDescripcionEquipo() != null && !val.getDescripcionEquipo().isEmpty() ? val.getDescripcionEquipo().toUpperCase() : "VALORIZACIÓN").append("</th></tr>");
        sb.append("<tr><td>DÍAS VALORIZADOS</td><td style='text-align:right; font-weight:bold;'>").append(val.getDiasValorizados()).append("</td></tr>");
        sb.append("<tr><td>COSTO DIARIO</td><td style='text-align:right;'>").append(monedaSimbolo).append(" ").append(String.format(Locale.US, "%.2f", val.getCostoDiario())).append("</td></tr>");
        sb.append("<tr><td>COSTO PARCIAL</td><td style='text-align:right;'>").append(monedaSimbolo).append(" ").append(String.format(Locale.US, "%.2f", val.getCostoParcial())).append("</td></tr>");

        if (val.isConHorasExtras()) {
            sb.append("<tr><td>TOTAL HORAS EXTRAS</td><td style='text-align:right;'>").append(monedaSimbolo).append(" ").append(String.format(Locale.US, "%.2f", val.getTotalHorasExtras())).append("</td></tr>");
            sb.append("<tr><td>SUBTOTAL</td><td style='text-align:right; font-weight:bold;'>").append(monedaSimbolo).append(" ").append(String.format(Locale.US, "%.2f", val.getSubtotal())).append("</td></tr>");
        }

        sb.append("<tr><td>").append(getEtiquetaImpuesto()).append("</td><td style='text-align:right;'>").append(monedaSimbolo).append(" ").append(String.format(Locale.US, "%.2f", val.getIgv())).append("</td></tr>");
        sb.append("<tr class='row-total'><td>TOTAL</td><td style='text-align:right;'>").append(monedaSimbolo).append(" ").append(String.format(Locale.US, "%.2f", val.getTotal())).append("</td></tr>");
        sb.append("</table>");

        sb.append("</div>");

        // 5. Total en letras
        if (val.getTotalEnLetras() != null && !val.getTotalEnLetras().isEmpty()) {
            sb.append("<div class='son-letras'>").append(val.getTotalEnLetras()).append("</div>");
        }

        // 6. Cuentas Bancarias
        sb.append("<div class='cuentas-box'>");
        for (String cuenta : getCuentasBancarias()) {
            sb.append("<div>").append(cuenta).append("</div>");
        }
        sb.append("<div style='margin-top:3px; font-weight:bold;'>").append(getTitularCuentas()).append("</div>");
        sb.append("</div>");

        sb.append("</body></html>");
        return sb.toString();
    }

    @Override
    public File generarPdfValorizacion(Context context, Valorizacion val) throws Exception {
        boolean esHE = val.isConHorasExtras() || val.getHorasExtras() > 0 || val.getHorometroFinal() > 0;

        // Correlativo exacto: VAL-{codMaquina}-{MM}{yy} (ej. VAL-111-1026 o VAL-101-1026)
        String codVal = StrategyProvider.getCorrelativoStrategy().generarCodigoValorizacion(val.getCodigoEquipo(), val.getFechaInicio());
        String numVal = val.getNumeroValorizacion();
        if (codVal != null && !codVal.equals("VAL-000-0000")) {
            numVal = codVal;
            val.setNumeroValorizacion(numVal);
        } else if (numVal == null || numVal.isEmpty() || !numVal.startsWith("VAL-")) {
            numVal = "VAL-000-0000";
            val.setNumeroValorizacion(numVal);
        }

        // Cargar tipografía Montserrat requerida
        Typeface montserratReg;
        Typeface montserratBold;
        try {
            montserratReg = ResourcesCompat.getFont(context, R.font.montserrat_regular);
            montserratBold = ResourcesCompat.getFont(context, R.font.montserrat_bold);
        } catch (Exception e) {
            montserratReg = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL);
            montserratBold = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD);
        }
        if (montserratReg == null) montserratReg = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL);
        if (montserratBold == null) montserratBold = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD);

        // Dimensiones del documento (1008 x 612 pt - Legal horizontal exacto de la plantilla)
        int pageWidth = 1008;
        int pageHeight = 612;
        String assetPath = esHE ? "templates/PlantillaValorizacionHE.pdf" : "templates/PlantillaValorizacion.pdf";

        // Renderizar plantilla oficial limpia en un Bitmap de alta resolución (2x: 2016 x 1224)
        File tempFile = new File(context.getCacheDir(), "val_bg_template_" + System.currentTimeMillis() + ".pdf");
        try (InputStream in = context.getAssets().open(assetPath);
             FileOutputStream out = new FileOutputStream(tempFile)) {
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
        }

        ParcelFileDescriptor pfd = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY);
        PdfRenderer renderer = new PdfRenderer(pfd);
        PdfRenderer.Page rendererPage = renderer.openPage(0);

        int renderW = pageWidth * 2;
        int renderH = pageHeight * 2;
        Bitmap bgBitmap = Bitmap.createBitmap(renderW, renderH, Bitmap.Config.ARGB_8888);
        Canvas bgCanvas = new Canvas(bgBitmap);
        bgCanvas.drawColor(Color.WHITE);
        rendererPage.render(bgBitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT);

        rendererPage.close();
        renderer.close();
        pfd.close();
        tempFile.delete();

        // Iniciar PdfDocument vectorial
        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        // Dibujar la plantilla oficial de fondo con resolución nativa 1:1
        canvas.drawBitmap(bgBitmap, null, new RectF(0, 0, pageWidth, pageHeight), null);
        bgBitmap.recycle();

        // Configuración de pinturas con la tipografía Montserrat exacta (-2 pt a pedido)
        // 1. Código en el banner superior derecho: tamaño 18 y en negrita
        Paint headerCodePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        headerCodePaint.setTypeface(montserratBold);
        headerCodePaint.setTextSize(16f);
        headerCodePaint.setColor(Color.WHITE);

        // 2. Tabla dinámica:
        // Textos de los meses: tamaño 8 Montserrat en negrita
        Paint tableMonthPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        tableMonthPaint.setTypeface(montserratBold);
        tableMonthPaint.setTextSize(8f);
        tableMonthPaint.setColor(Color.BLACK);

        // Números de días: fondo azul #3B5F90 (o #EE1212 para domingos), letra blanca en negrita tamaño 8
        Paint tableDayPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        tableDayPaint.setTypeface(montserratBold);
        tableDayPaint.setTextSize(8f);
        tableDayPaint.setColor(Color.WHITE);

        // Texto regular de la tabla: tamaño 8 Montserrat
        Paint tableTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        tableTextPaint.setTypeface(montserratReg);
        tableTextPaint.setTextSize(8f);
        tableTextPaint.setColor(Color.BLACK);

        // Código de máquina sobre días trabajados: fondo azul, texto blanco en negrita tamaño 8
        Paint tableBoldPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        tableBoldPaint.setTypeface(montserratBold);
        tableBoldPaint.setTextSize(8f);
        tableBoldPaint.setColor(Color.WHITE);

        // 3. Fuera de la tabla dinámica: tamaño 9 Montserrat
        Paint outsideTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        outsideTextPaint.setTypeface(montserratReg);
        outsideTextPaint.setTextSize(8f);
        outsideTextPaint.setColor(Color.BLACK);

        Paint outsideBoldPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        outsideBoldPaint.setTypeface(montserratBold);
        outsideBoldPaint.setTextSize(8f);
        outsideBoldPaint.setColor(Color.BLACK);

        Paint outsideBoldWhitePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        outsideBoldWhitePaint.setTypeface(montserratBold);
        outsideBoldWhitePaint.setTextSize(8f);
        outsideBoldWhitePaint.setColor(Color.WHITE);

        // Pinturas de fondos y bordes (#7F7F7F para las nuevas líneas)
        Paint fillNavy = new Paint(Paint.ANTI_ALIAS_FLAG);
        fillNavy.setColor(Color.parseColor("#3B5F90"));
        fillNavy.setStyle(Paint.Style.FILL);

        Paint fillSundayRed = new Paint(Paint.ANTI_ALIAS_FLAG);
        fillSundayRed.setColor(Color.parseColor("#EE1212"));
        fillSundayRed.setStyle(Paint.Style.FILL);

        Paint fillWhite = new Paint(Paint.ANTI_ALIAS_FLAG);
        fillWhite.setColor(Color.WHITE);
        fillWhite.setStyle(Paint.Style.FILL);

        Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setColor(Color.parseColor("#7F7F7F"));
        borderPaint.setStrokeWidth(0.72f);
        borderPaint.setStyle(Paint.Style.STROKE);

        // Generar tareo dinámico de fechas
        CalendarioTareoHelper.TareoInfo tareo = CalendarioTareoHelper.generarTareo(val.getFechaInicio(), val.getFechaFin());
        boolean esDolar = val.getMoneda() != null && (
                val.getMoneda().equalsIgnoreCase("USD") ||
                val.getMoneda().toUpperCase().contains("DOLAR") ||
                val.getMoneda().toUpperCase().contains("DÓLAR") ||
                val.getMoneda().contains("$")
        );
        String simboloMoneda = esDolar ? "$" : "S/.";

        if (esHE) {
            // ========================================================
            // PLANTILLA CON HORAS EXTRAS (PlantillaValorizacionHE.pdf)
            // ========================================================

            // 1. Código en el banner superior derecho: número 20, bold, blanco
            drawCenteredText(canvas, numVal, 666.91f, 66.38f, 250.82f, 24.60f, headerCodePaint);

            // 2. Datos del cliente y equipo (fuera de la tabla, tamaño 11 Montserrat)
            drawText(canvas, val.getNombreCliente(), 167.06f, 141.86f, 11.88f, outsideTextPaint, Paint.Align.LEFT);
            drawText(canvas, val.getClienteDireccion(), 167.06f, 153.74f, 11.88f, outsideTextPaint, Paint.Align.LEFT);
            String eq = (val.getDescripcionEquipo() != null && !val.getDescripcionEquipo().isEmpty())
                    ? val.getDescripcionEquipo()
                    : ((val.getCodigoEquipo() != null && !val.getCodigoEquipo().isEmpty()) ? "Equipo | " + val.getCodigoEquipo() : "Equipo");
            drawText(canvas, eq, 167.06f, 165.63f, 11.90f, outsideTextPaint, Paint.Align.LEFT);
            String marca = (val.getMarcaEquipo() != null && !val.getMarcaEquipo().isEmpty()) ? val.getMarcaEquipo() : "MODASA";
            drawText(canvas, marca, 167.06f, 177.53f, 11.88f, outsideTextPaint, Paint.Align.LEFT);
            drawText(canvas, val.getUbicacionTrabajo(), 167.06f, 189.41f, 11.88f, outsideTextPaint, Paint.Align.LEFT);

            drawText(canvas, val.getClienteRuc(), 593.47f, 141.86f, 11.88f, outsideTextPaint, Paint.Align.LEFT);
            drawText(canvas, val.getFechaEmision(), 593.47f, 153.74f, 11.88f, outsideTextPaint, Paint.Align.LEFT);

            // 3. Tabla dinámica de tareo (30 columnas de 25.08 pt cada una)
            float xStart = 165.26f;
            float colW = 25.082f;

            // Fila 1 (Meses): tamaño 10 Montserrat bold, centrado
            canvas.drawRect(xStart, 223.85f, xStart + (30 * colW), 223.85f + 12.72f, fillWhite);
            canvas.drawRect(xStart, 223.85f, xStart + (30 * colW), 223.85f + 12.72f, borderPaint);
            float curX = xStart;
            for (CalendarioTareoHelper.MesSpan mes : tareo.getMeses()) {
                float spanW = mes.getCantidadDias() * colW;
                canvas.drawRect(curX, 223.85f, curX + spanW, 223.85f + 12.72f, borderPaint);
                drawCenteredText(canvas, mes.getNombreMes(), curX, 223.85f, spanW, 12.72f, tableMonthPaint);
                curX += spanW;
            }

            // Fila 2 (Días): fondo azul #3B5F90 (rojo #EE1212 domingos), letra blanca en negrita tamaño 10
            for (int i = 0; i < 30; i++) {
                float cellX = xStart + (i * colW);
                boolean isSunday = (i < tareo.getEsDomingo().size()) && tareo.getEsDomingo().get(i);
                Paint cellBg = isSunday ? fillSundayRed : fillNavy;
                canvas.drawRect(cellX, 236.57f, cellX + colW, 236.57f + 11.88f, cellBg);
                canvas.drawRect(cellX, 236.57f, cellX + colW, 236.57f + 11.88f, borderPaint);
                int diaNum = (i < tareo.getDiasDelMes().size()) ? tareo.getDiasDelMes().get(i) : (i + 1);
                drawCenteredText(canvas, String.valueOf(diaNum), cellX, 236.57f, colW, 11.88f, tableDayPaint);
            }

            // Fila 3 (Código de Máquina y Asistencia "1"): tamaño 10 Montserrat
            String codEquipo = (val.getCodigoEquipo() != null && !val.getCodigoEquipo().isEmpty()) ? val.getCodigoEquipo() : "GEP 105-111";
            drawCenteredText(canvas, codEquipo, 70.44f, 248.45f, 94.82f, 11.88f, tableBoldPaint);
            for (int i = 0; i < 30; i++) {
                float cellX = xStart + (i * colW);
                drawCenteredText(canvas, "1", cellX, 248.45f, colW, 11.88f, tableTextPaint);
            }

            // Fila 4 (Días acumulados 1..30): tamaño 10 Montserrat
            for (int i = 0; i < 30; i++) {
                float cellX = xStart + (i * colW);
                int acum = (i < tareo.getDiasAcumulados().size()) ? tareo.getDiasAcumulados().get(i) : (i + 1);
                drawCenteredText(canvas, String.valueOf(acum), cellX, 260.33f, colW, 11.88f, tableTextPaint);
            }

            // 4. Detalle de Horas y Horómetros (fuera de la tabla dinámica, tamaño 11 Montserrat)
            String cellHeader = "EQUIPO";
            if (val.getCodigoEquipo() != null && !val.getCodigoEquipo().isEmpty()) {
                cellHeader = val.getCodigoEquipo().split("-")[0].trim().toUpperCase();
            } else if (val.getDescripcionEquipo() != null && !val.getDescripcionEquipo().isEmpty()) {
                cellHeader = val.getDescripcionEquipo().toUpperCase();
            }
            drawCenteredText(canvas, cellHeader, 466.25f, 296.33f, 200.66f, 11.88f, outsideBoldWhitePaint);

            // Horómetros (Izquierda)
            drawCenteredText(canvas, formatHorometro(val.getHorometroInicial()), 190.34f, 308.21f, 50.16f, 11.90f, outsideTextPaint);
            drawCenteredText(canvas, formatHorometro(val.getHorometroFinal()), 190.34f, 320.11f, 50.16f, 11.88f, outsideTextPaint);
            drawCenteredText(canvas, formatHorometro(val.getHorasPermitidas()), 190.34f, 331.99f, 50.16f, 11.88f, outsideTextPaint);
            drawCenteredText(canvas, formatHorometro(val.getHorasUsadas()), 190.34f, 343.87f, 50.16f, 11.88f, outsideTextPaint);
            drawCenteredText(canvas, formatHorometro(val.getHorasExtras()), 190.34f, 355.75f, 50.16f, 11.88f, outsideTextPaint);

            // Costo hora extra y total horas extras (Centro)
            drawCenteredText(canvas, simboloMoneda, 340.84f, 308.21f, 25.09f, 11.90f, outsideTextPaint);
            drawRightAlignedText(canvas, formatMonto(val.getCostoHoraExtra()), 412.0f, 308.21f, 11.90f, outsideTextPaint);

            drawCenteredText(canvas, simboloMoneda, 340.84f, 320.11f, 25.09f, 11.88f, outsideTextPaint);
            drawRightAlignedText(canvas, formatMonto(val.getTotalHorasExtras()), 412.0f, 320.11f, 11.88f, outsideTextPaint);

            // Resumen económico (Derecha)
            drawRightAlignedText(canvas, String.valueOf(val.getDiasValorizados()), 664.0f, 308.21f, 11.90f, outsideTextPaint);

            drawCenteredText(canvas, simboloMoneda, 591.67f, 320.11f, 25.08f, 11.88f, outsideTextPaint);
            drawRightAlignedText(canvas, formatMonto(val.getCostoDiario()), 664.0f, 320.11f, 11.88f, outsideTextPaint);

            drawCenteredText(canvas, simboloMoneda, 591.67f, 331.99f, 25.08f, 11.88f, outsideTextPaint);
            drawRightAlignedText(canvas, formatMonto(val.getCostoParcial()), 664.0f, 331.99f, 11.88f, outsideTextPaint);

            drawCenteredText(canvas, simboloMoneda, 591.67f, 343.87f, 25.08f, 11.88f, outsideTextPaint);
            drawRightAlignedText(canvas, formatMonto(val.getTotalHorasExtras()), 664.0f, 343.87f, 11.88f, outsideTextPaint);

            drawCenteredText(canvas, simboloMoneda, 591.67f, 355.75f, 25.08f, 11.88f, outsideTextPaint);
            drawRightAlignedText(canvas, formatMonto(val.getSubtotal()), 664.0f, 355.75f, 11.88f, outsideTextPaint);

            drawCenteredText(canvas, simboloMoneda, 591.67f, 367.63f, 25.08f, 11.88f, outsideTextPaint);
            drawRightAlignedText(canvas, formatMonto(val.getIgv()), 664.0f, 367.63f, 11.88f, outsideTextPaint);

            drawCenteredText(canvas, simboloMoneda, 591.67f, 379.51f, 25.08f, 12.24f, outsideBoldPaint);
            drawRightAlignedText(canvas, formatMonto(val.getTotal()), 664.0f, 379.51f, 12.24f, outsideBoldPaint);

            // Son en letras
            if (val.getTotalEnLetras() != null && !val.getTotalEnLetras().isEmpty()) {
                drawText(canvas, val.getTotalEnLetras(), 496.0f, 391.75f, 11.88f, outsideTextPaint, Paint.Align.LEFT);
            }

        } else {
            // ========================================================
            // PLANTILLA SIMPLE (PlantillaValorizacion.pdf)
            // ========================================================

            // 1. Código en el banner superior derecho: número 20, bold, blanco
            drawCenteredText(canvas, numVal, 701.11f, 65.30f, 265.37f, 26.04f, headerCodePaint);

            // 2. Datos del cliente y equipo (fuera de la tabla, tamaño 11 Montserrat)
            drawText(canvas, val.getNombreCliente(), 172.0f, 144.99f, 12.48f, outsideTextPaint, Paint.Align.LEFT);
            drawText(canvas, val.getClienteDireccion(), 172.0f, 157.47f, 12.50f, outsideTextPaint, Paint.Align.LEFT);
            String eq = (val.getDescripcionEquipo() != null && !val.getDescripcionEquipo().isEmpty())
                    ? val.getDescripcionEquipo()
                    : ((val.getCodigoEquipo() != null && !val.getCodigoEquipo().isEmpty()) ? "Equipo | " + val.getCodigoEquipo() : "Equipo");
            drawText(canvas, eq, 172.0f, 169.97f, 12.48f, outsideTextPaint, Paint.Align.LEFT);
            String marca = (val.getMarcaEquipo() != null && !val.getMarcaEquipo().isEmpty()) ? val.getMarcaEquipo() : "MODASA";
            drawText(canvas, marca, 172.0f, 182.45f, 12.48f, outsideTextPaint, Paint.Align.LEFT);
            drawText(canvas, val.getUbicacionTrabajo(), 172.0f, 194.93f, 12.48f, outsideTextPaint, Paint.Align.LEFT);

            drawText(canvas, val.getClienteRuc(), 623.0f, 144.99f, 12.48f, outsideTextPaint, Paint.Align.LEFT);
            drawText(canvas, val.getFechaEmision(), 623.0f, 157.47f, 12.50f, outsideTextPaint, Paint.Align.LEFT);

            // 3. Tabla dinámica de tareo (30 columnas de 26.52 pt cada una)
            float xStart = 170.30f;
            float colW = 26.52f;

            // Fila 1 (Meses): tamaño 10 Montserrat bold, centrado
            canvas.drawRect(xStart, 217.85f, xStart + (30 * colW), 217.85f + 11.76f, fillWhite);
            canvas.drawRect(xStart, 217.85f, xStart + (30 * colW), 217.85f + 11.76f, borderPaint);
            float curX = xStart;
            for (CalendarioTareoHelper.MesSpan mes : tareo.getMeses()) {
                float spanW = mes.getCantidadDias() * colW;
                canvas.drawRect(curX, 217.85f, curX + spanW, 217.85f + 11.76f, borderPaint);
                drawCenteredText(canvas, mes.getNombreMes(), curX, 217.85f, spanW, 11.76f, tableMonthPaint);
                curX += spanW;
            }

            // Fila 2 (Días): fondo azul #3B5F90 (rojo #EE1212 domingos), letra blanca en negrita tamaño 10
            for (int i = 0; i < 30; i++) {
                float cellX = xStart + (i * colW);
                boolean isSunday = (i < tareo.getEsDomingo().size()) && tareo.getEsDomingo().get(i);
                Paint cellBg = isSunday ? fillSundayRed : fillNavy;
                canvas.drawRect(cellX, 229.61f, cellX + colW, 229.61f + 13.44f, cellBg);
                canvas.drawRect(cellX, 229.61f, cellX + colW, 229.61f + 13.44f, borderPaint);
                int diaNum = (i < tareo.getDiasDelMes().size()) ? tareo.getDiasDelMes().get(i) : (i + 1);
                drawCenteredText(canvas, String.valueOf(diaNum), cellX, 229.61f, colW, 13.44f, tableDayPaint);
            }

            // Fila 3 (Código de Máquina y Asistencia "1"): tamaño 10 Montserrat
            String codEquipo = (val.getCodigoEquipo() != null && !val.getCodigoEquipo().isEmpty()) ? val.getCodigoEquipo() : "GEP 105-111";
            drawCenteredText(canvas, codEquipo, 70.44f, 243.05f, 99.86f, 12.48f, tableBoldPaint);
            for (int i = 0; i < 30; i++) {
                float cellX = xStart + (i * colW);
                drawCenteredText(canvas, "1", cellX, 243.05f, colW, 12.48f, tableTextPaint);
            }

            // Fila 4 (Días acumulados 1..30): tamaño 10 Montserrat
            for (int i = 0; i < 30; i++) {
                float cellX = xStart + (i * colW);
                int acum = (i < tareo.getDiasAcumulados().size()) ? tareo.getDiasAcumulados().get(i) : (i + 1);
                drawCenteredText(canvas, String.valueOf(acum), cellX, 255.53f, colW, 12.48f, tableTextPaint);
            }

            // 4. Detalle económico (fuera de la tabla dinámica, tamaño 11 Montserrat)
            // Fila 0: Barra azul de encabezado (y_top = 289.61, h = 12.48)
            String descEq = (val.getDescripcionEquipo() != null && !val.getDescripcionEquipo().isEmpty()) ? val.getDescripcionEquipo().toUpperCase() : "VALORIZACIÓN";
            drawCenteredText(canvas, descEq, 70.44f, 289.61f, 206.42f, 12.48f, outsideBoldWhitePaint);

            // Fila 1: DÍAS VALORIZADOS (y_top = 302.09, h = 12.50)
            drawRightAlignedText(canvas, String.valueOf(val.getDiasValorizados()), 272.0f, 302.09f, 12.50f, outsideTextPaint);

            // Fila 2: COSTO DIARIO (y_top = 314.59, h = 12.48)
            drawCenteredText(canvas, simboloMoneda, 196.82f, 314.59f, 26.52f, 12.48f, outsideTextPaint);
            drawRightAlignedText(canvas, formatMonto(val.getCostoDiario()), 272.0f, 314.59f, 12.48f, outsideTextPaint);

            // Fila 3: COSTO PARCIAL (y_top = 327.07, h = 12.48)
            drawCenteredText(canvas, simboloMoneda, 196.82f, 327.07f, 26.52f, 12.48f, outsideTextPaint);
            drawRightAlignedText(canvas, formatMonto(val.getCostoParcial()), 272.0f, 327.07f, 12.48f, outsideTextPaint);

            // Fila 4: IGV (y_top = 339.55, h = 12.48)
            drawCenteredText(canvas, simboloMoneda, 196.82f, 339.55f, 26.52f, 12.48f, outsideTextPaint);
            drawRightAlignedText(canvas, formatMonto(val.getIgv()), 272.0f, 339.55f, 12.48f, outsideTextPaint);

            // Fila 5: TOTAL (y_top = 352.03, h = 12.48)
            drawCenteredText(canvas, simboloMoneda, 196.82f, 352.03f, 26.52f, 12.48f, outsideBoldPaint);
            drawRightAlignedText(canvas, formatMonto(val.getTotal()), 272.0f, 352.03f, 12.48f, outsideBoldPaint);

            // Fila 6: Son en letras (y_top = 364.51 a 378)
            if (val.getTotalEnLetras() != null && !val.getTotalEnLetras().isEmpty()) {
                drawText(canvas, val.getTotalEnLetras(), 104.0f, 364.51f, 14.0f, outsideTextPaint, Paint.Align.LEFT);
            }
        }

        document.finishPage(page);

        // Nomenclatura obligatoria estricta: VAL-CODMAQUINA-MESAÑO.pdf
        String nombreArchivo = (numVal != null && !numVal.isEmpty() ? numVal : "VAL-000-0000") + ".pdf";
        File dir = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "Valorizaciones");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File archivo = new File(dir, nombreArchivo);
        FileOutputStream fos = new FileOutputStream(archivo);
        document.writeTo(fos);
        fos.close();
        document.close();

        return archivo;
    }

    private void drawCenteredText(Canvas canvas, String text, float x, float yTop, float width, float height, Paint paint) {
        if (text == null || text.isEmpty()) return;
        Paint p = paint;
        float textW = paint.measureText(text);
        if (textW > width - 4f) {
            p = new Paint(paint);
            float newSize = paint.getTextSize() * (width - 4f) / textW;
            p.setTextSize(Math.max(6f, newSize));
        }
        p.setTextAlign(Paint.Align.CENTER);
        float textX = x + (width / 2f);
        float textY = yTop + (height / 2f) - ((p.descent() + p.ascent()) / 2f);
        canvas.drawText(text, textX, textY, p);
    }

    private void drawRightAlignedText(Canvas canvas, String text, float rightX, float yTop, float height, Paint paint) {
        if (text == null || text.isEmpty()) return;
        paint.setTextAlign(Paint.Align.RIGHT);
        float textY = yTop + (height / 2f) - ((paint.descent() + paint.ascent()) / 2f);
        canvas.drawText(text, rightX, textY, paint);
    }

    private void drawText(Canvas canvas, String text, float leftX, float yTop, float height, Paint paint, Paint.Align align) {
        if (text == null || text.isEmpty()) return;
        paint.setTextAlign(align);
        float textY = yTop + (height / 2f) - ((paint.descent() + paint.ascent()) / 2f);
        canvas.drawText(text, leftX, textY, paint);
    }

    private String formatMonto(double monto) {
        if (monto == (long) monto) {
            return String.format(Locale.US, "%d", (long) monto);
        } else {
            return String.format(Locale.US, "%.2f", monto);
        }
    }

    private String formatHorometro(double valor) {
        if (valor == (long) valor) {
            return String.format(Locale.US, "%,d", (long) valor).replace(',', ' ');
        } else {
            return String.format(Locale.US, "%,.1f", valor).replace(',', ' ');
        }
    }

    @Override
    public File generarValorizacionPdf(Context context, Valorizacion valorizacion) throws Exception {
        return generarPdfValorizacion(context, valorizacion);
    }

    @Override
    public File generarCertificadoPdf(Context context, CertificadoOperatividad cert, String codigoGrupo) throws Exception {
        PdfGenerator pdfGen = new PdfGenerator();
        return pdfGen.generarCertificadoPdf(context, cert, codigoGrupo);
    }

    @Override
    public void generarInformeMantenimientoPdf(Context context, File archivoSalida, Mantenimiento mant, InfoPlaca placa, String codigoGrupo, Map<String, Object> datos) throws Exception {
        InformePdfVectorialGenerator.generarPdf(context, archivoSalida, mant, placa, codigoGrupo, datos);
    }
}
