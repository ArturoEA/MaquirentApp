package com.example.maquirentapp.strategy;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.os.Environment;

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

    public abstract String generarCondicionesComercialesHtml(Cotizacion cotizacion);

    @Override
    public String generarHtmlCotizacion(Context context, Cotizacion cotizacion) {
        String firmaBase64 = "";
        try {
            firmaBase64 = "data:image/png;base64," + ImageUtils.convertirDrawableABase64(context, getFirmaDrawableResId());
        } catch (Exception e) {
            firmaBase64 = "";
        }

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
        html.append("<th>ITEM</th><th>DESCRIPCIÓN</th><th>POTENCIA</th><th>TARIFA MES</th><th>HORAS MÍN.</th><th>PRECIO HE</th><th>SUBTOTAL</th>");
        html.append("</tr></thead><tbody>");

        int index = 1;
        for (ItemCotizacion item : cotizacion.getItems()) {
            html.append("<tr>");
            html.append("<td>").append(index++).append("</td>");
            html.append("<td class='td-left'>").append(item.getDescripcionEquipo() != null ? item.getDescripcionEquipo() : "").append("</td>");
            html.append("<td>").append(item.getPotencia() != null ? item.getPotencia() : "-").append("</td>");
            html.append("<td>").append(String.format(Locale.US, "%.2f", item.getPrecioMensual())).append("</td>");
            html.append("<td>").append(cotizacion.getHorasMinimas()).append("</td>");
            html.append("<td>").append(String.format(Locale.US, "%.2f", item.getPrecioHoraExtra())).append("</td>");
            html.append("<td>").append(String.format(Locale.US, "%.2f", item.getPrecioMensual())).append("</td>");
            html.append("</tr>");
        }

        html.append("<tr>");
        html.append("<td colspan='6' class='td-left no-border'><b>SUBTOTAL (").append(cotizacion.getMoneda()).append(")</b></td>");
        html.append("<td><b>").append(String.format(Locale.US, "%.2f", cotizacion.getSubtotalGlobal())).append("</b></td>");
        html.append("</tr>");
        html.append("<tr>");
        html.append("<td colspan='6' class='td-left no-border'><b>").append(getEtiquetaImpuesto()).append("</b></td>");
        html.append("<td><b>").append(String.format(Locale.US, "%.2f", cotizacion.getIgvGlobal())).append("</b></td>");
        html.append("</tr>");
        html.append("<tr>");
        html.append("<td colspan='6' class='td-left no-border'><b>TOTAL GENERAL (").append(cotizacion.getMoneda()).append(")</b></td>");
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
        // Formato horizontal estricto (A4 Landscape: 842 pt de ancho x 595 pt de alto)
        int A4_WIDTH_PTS = 842;
        int A4_HEIGHT_PTS = 595;

        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(A4_WIDTH_PTS, A4_HEIGHT_PTS, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        android.graphics.Canvas canvas = page.getCanvas();

        // Fondo blanco
        Paint bgPaint = new Paint();
        bgPaint.setColor(Color.WHITE);
        canvas.drawRect(0, 0, A4_WIDTH_PTS, A4_HEIGHT_PTS, bgPaint);

        // Colores idénticos a la plantilla Excel
        Paint navyBoxPaint = new Paint();
        navyBoxPaint.setColor(Color.parseColor("#3B5F90"));
        navyBoxPaint.setStyle(Paint.Style.FILL);

        Paint sundayRedPaint = new Paint();
        sundayRedPaint.setColor(Color.parseColor("#EE1212"));
        sundayRedPaint.setStyle(Paint.Style.FILL);

        Paint linePaint = new Paint();
        linePaint.setColor(Color.BLACK);
        linePaint.setStrokeWidth(0.6f);
        linePaint.setStyle(Paint.Style.STROKE);

        // Tipografías exactas
        Paint bannerTitlePaint = new Paint();
        bannerTitlePaint.setColor(Color.WHITE);
        bannerTitlePaint.setTextSize(14f);
        bannerTitlePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        Paint bannerNumPaint = new Paint();
        bannerNumPaint.setColor(Color.WHITE);
        bannerNumPaint.setTextSize(14f);
        bannerNumPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        Paint whiteBoldCenter = new Paint();
        whiteBoldCenter.setColor(Color.WHITE);
        whiteBoldCenter.setTextSize(7.5f);
        whiteBoldCenter.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        Paint whiteBoldLeft = new Paint();
        whiteBoldLeft.setColor(Color.WHITE);
        whiteBoldLeft.setTextSize(7.5f);
        whiteBoldLeft.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        Paint textPaint = new Paint();
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(7.5f);

        Paint textCenterPaint = new Paint();
        textCenterPaint.setColor(Color.BLACK);
        textCenterPaint.setTextSize(7.5f);

        Paint boldPaint = new Paint();
        boldPaint.setColor(Color.BLACK);
        boldPaint.setTextSize(7.5f);
        boldPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        Paint boldCenterPaint = new Paint();
        boldCenterPaint.setColor(Color.BLACK);
        boldCenterPaint.setTextSize(7.5f);
        boldCenterPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        Paint italicPaint = new Paint();
        italicPaint.setColor(Color.BLACK);
        italicPaint.setTextSize(6.5f);
        italicPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.ITALIC));

        float xStart = 26f;
        float y = 24f;

        // Correlativo exacto: VAL-{grupo}-{MM}{yy} (ej. VAL-111-1026)
        String numVal = val.getNumeroValorizacion();
        if (numVal == null || numVal.isEmpty() || !numVal.startsWith("VAL-")) {
            numVal = StrategyProvider.getCorrelativoStrategy().generarCodigoValorizacion(val.getCodigoEquipo(), val.getFechaInicio());
        }

        // ========================================================
        // 1. BANNER SUPERIOR (Idéntico a D2:U3 y V2:AE3 de Excel)
        // ========================================================
        drawCell(canvas, 200f, y, 396f, 32f, "VALORIZACIÓN DE EQUIPO EN ALQUILER", navyBoxPaint, linePaint, bannerTitlePaint, Paint.Align.CENTER);
        drawCell(canvas, 596f, y, 220f, 32f, numVal, navyBoxPaint, linePaint, bannerNumPaint, Paint.Align.CENTER);

        // ========================================================
        // 2. DATOS DE LA EMPRESA (Filas 4, 5 y 6)
        // ========================================================
        y = 66f;
        Paint compBold = new Paint(boldPaint);
        compBold.setTextSize(8.5f);

        canvas.drawText("RUC: " + getRuc(), xStart, y + 8f, compBold);
        canvas.drawText(getDireccion(), 178f, y + 8f, compBold);
        y += 13f;
        canvas.drawText(getRubro(), xStart, y + 8f, compBold);
        y += 13f;
        canvas.drawText("Teléfonos: " + getTelefono(), xStart, y + 8f, compBold);
        y += 18f;

        // ========================================================
        // 3. CUADRO DE DATOS CLIENTE Y EQUIPO (Filas 8 a 12)
        // ========================================================
        float hRowClient = 15f;
        float wColA = 130f;
        float wColBP = 330f;
        float wColQR = 44f;
        float wColSU = 66f;

        // Fila 8: CLIENTE y RUC
        String cliNom = val.getNombreCliente() != null ? val.getNombreCliente() : "-";
        String rucVal = val.getClienteRuc() != null ? val.getClienteRuc() : "-";
        drawCell(canvas, xStart, y, wColA, hRowClient, "CLIENTE ", navyBoxPaint, linePaint, whiteBoldLeft, Paint.Align.LEFT);
        drawCell(canvas, xStart + wColA, y, wColBP, hRowClient, cliNom, null, linePaint, textPaint, Paint.Align.LEFT);
        drawCell(canvas, xStart + wColA + wColBP, y, wColQR, hRowClient, "RUC ", navyBoxPaint, linePaint, whiteBoldCenter, Paint.Align.CENTER);
        drawCell(canvas, xStart + wColA + wColBP + wColQR, y, wColSU, hRowClient, rucVal, null, linePaint, textCenterPaint, Paint.Align.CENTER);
        y += hRowClient;

        // Fila 9: DIRECCIÓN y FECHA
        String dirVal = val.getClienteDireccion() != null ? val.getClienteDireccion() : "-";
        String fecVal = val.getFechaEmision() != null ? val.getFechaEmision() : "-";
        drawCell(canvas, xStart, y, wColA, hRowClient, "DIRECCIÓN", navyBoxPaint, linePaint, whiteBoldLeft, Paint.Align.LEFT);
        drawCell(canvas, xStart + wColA, y, wColBP, hRowClient, dirVal, null, linePaint, textPaint, Paint.Align.LEFT);
        drawCell(canvas, xStart + wColA + wColBP, y, wColQR, hRowClient, "FECHA", navyBoxPaint, linePaint, whiteBoldCenter, Paint.Align.CENTER);
        drawCell(canvas, xStart + wColA + wColBP + wColQR, y, wColSU, hRowClient, fecVal, null, linePaint, textCenterPaint, Paint.Align.CENTER);
        y += hRowClient;

        // Fila 10: EQUIPO
        String eqVal = (val.getDescripcionEquipo() != null ? val.getDescripcionEquipo() : "Grupo electrógeno") +
                (val.getCodigoEquipo() != null && !val.getCodigoEquipo().isEmpty() ? " | " + val.getCodigoEquipo() : "");
        drawCell(canvas, xStart, y, wColA, hRowClient, "EQUIPO", navyBoxPaint, linePaint, whiteBoldLeft, Paint.Align.LEFT);
        drawCell(canvas, xStart + wColA, y, wColBP, hRowClient, eqVal, null, linePaint, textPaint, Paint.Align.LEFT);
        y += hRowClient;

        // Fila 11: MARCA
        String marcaVal = (val.getMarcaEquipo() != null && !val.getMarcaEquipo().isEmpty()) ? val.getMarcaEquipo() : "MODASA";
        drawCell(canvas, xStart, y, wColA, hRowClient, "MARCA", navyBoxPaint, linePaint, whiteBoldLeft, Paint.Align.LEFT);
        drawCell(canvas, xStart + wColA, y, wColBP, hRowClient, marcaVal, null, linePaint, textPaint, Paint.Align.LEFT);
        y += hRowClient;

        // Fila 12: LUGAR DE TRABAJO
        String lugVal = val.getUbicacionTrabajo() != null ? val.getUbicacionTrabajo() : "-";
        drawCell(canvas, xStart, y, wColA, hRowClient, "LUGAR DE TRABAJO", navyBoxPaint, linePaint, whiteBoldLeft, Paint.Align.LEFT);
        drawCell(canvas, xStart + wColA, y, wColBP, hRowClient, lugVal, null, linePaint, textPaint, Paint.Align.LEFT);
        y += hRowClient + 14f;

        // ========================================================
        // 4. MATRIZ DE TAREO (30 días en columnas horizontales)
        // ========================================================
        CalendarioTareoHelper.TareoInfo tareo = CalendarioTareoHelper.generarTareo(val.getFechaInicio(), val.getFechaFin());
        int totalDias = tareo.getTotalDias();
        if (totalDias <= 0) totalDias = 30;
        float dayColW = 22f;
        float hTareo = 14f;

        // Fila Meses
        drawCell(canvas, xStart, y, wColA, hTareo, "", null, linePaint, null, Paint.Align.CENTER);
        float curX = xStart + wColA;
        for (CalendarioTareoHelper.MesSpan mes : tareo.getMeses()) {
            float spanW = mes.getCantidadDias() * dayColW;
            drawCell(canvas, curX, y, spanW, hTareo, mes.getNombreMes(), null, linePaint, boldCenterPaint, Paint.Align.CENTER);
            curX += spanW;
        }
        y += hTareo;

        // Fila EQUIPO / DÍAS
        drawCell(canvas, xStart, y, wColA, hTareo, "EQUIPO                      DÍAS", navyBoxPaint, linePaint, whiteBoldCenter, Paint.Align.CENTER);
        for (int i = 0; i < totalDias; i++) {
            boolean isSunday = (i < tareo.getEsDomingo().size()) && tareo.getEsDomingo().get(i);
            Paint fillP = isSunday ? sundayRedPaint : navyBoxPaint;
            int diaNum = (i < tareo.getDiasDelMes().size()) ? tareo.getDiasDelMes().get(i) : (i + 1);
            drawCell(canvas, xStart + wColA + (i * dayColW), y, dayColW, hTareo, String.valueOf(diaNum), fillP, linePaint, whiteBoldCenter, Paint.Align.CENTER);
        }
        y += hTareo;

        // Fila Código de Máquina y Asistencia (1s)
        String cod = val.getCodigoEquipo() != null && !val.getCodigoEquipo().isEmpty() ? val.getCodigoEquipo() : "GEP 105-111";
        drawCell(canvas, xStart, y, wColA, hTareo, cod, navyBoxPaint, linePaint, whiteBoldCenter, Paint.Align.CENTER);
        for (int i = 0; i < totalDias; i++) {
            drawCell(canvas, xStart + wColA + (i * dayColW), y, dayColW, hTareo, "1", null, linePaint, textCenterPaint, Paint.Align.CENTER);
        }
        y += hTareo;

        // Fila Días trabajados
        drawCell(canvas, xStart, y, wColA, hTareo, "Días trabajados", navyBoxPaint, linePaint, whiteBoldCenter, Paint.Align.CENTER);
        for (int i = 0; i < totalDias; i++) {
            int acum = (i < tareo.getDiasAcumulados().size()) ? tareo.getDiasAcumulados().get(i) : (i + 1);
            drawCell(canvas, xStart + wColA + (i * dayColW), y, dayColW, hTareo, String.valueOf(acum), null, linePaint, textCenterPaint, Paint.Align.CENTER);
        }
        y += hTareo + 16f;

        // ========================================================
        // 5. SECCIÓN ECONÓMICA Y HORÓMETROS
        // ========================================================
        float rowH = 15f;
        String descEq = (val.getDescripcionEquipo() != null && !val.getDescripcionEquipo().isEmpty()) ? val.getDescripcionEquipo().toUpperCase() : "GRUPO ELECTRÓGENO 105 kW";

        if (val.isConHorasExtras()) {
            // ====================================================
            // PLANTILLA CON HORAS EXTRAS (media_...9104d455.xlsx)
            // ====================================================
            float yRow = y;

            // Encabezados
            drawCell(canvas, 26f, yRow, 350f, rowH, "DETALLE DE HORAS", navyBoxPaint, linePaint, whiteBoldCenter, Paint.Align.CENTER);
            drawCell(canvas, 442f, yRow, 154f, rowH, descEq, navyBoxPaint, linePaint, whiteBoldCenter, Paint.Align.CENTER);
            yRow += rowH;

            // Fila 22
            drawCell(canvas, 26f, yRow, 84f, rowH, "HORÓMETRO INICIAL", null, linePaint, textPaint, Paint.Align.LEFT);
            drawCell(canvas, 110f, yRow, 68f, rowH, formatHorometro(val.getHorometroInicial()), null, linePaint, textCenterPaint, Paint.Align.CENTER);
            drawCell(canvas, 178f, yRow, 110f, rowH, "COSTO HORA EXTRA*", null, linePaint, textPaint, Paint.Align.LEFT);
            drawCell(canvas, 288f, yRow, 22f, rowH, "S/.", null, linePaint, textCenterPaint, Paint.Align.CENTER);
            drawCell(canvas, 310f, yRow, 66f, rowH, formatMonto(val.getCostoHoraExtra()), null, linePaint, textCenterPaint, Paint.Align.CENTER);

            drawCell(canvas, 442f, yRow, 88f, rowH, "DÍAS VALORIZADOS", null, linePaint, textPaint, Paint.Align.LEFT);
            drawCell(canvas, 530f, yRow, 66f, rowH, String.valueOf(val.getDiasValorizados()), null, linePaint, textCenterPaint, Paint.Align.CENTER);
            yRow += rowH;

            // Fila 23
            drawCell(canvas, 26f, yRow, 84f, rowH, "HORÓMETRO FINAL", null, linePaint, textPaint, Paint.Align.LEFT);
            drawCell(canvas, 110f, yRow, 68f, rowH, formatHorometro(val.getHorometroFinal()), null, linePaint, textCenterPaint, Paint.Align.CENTER);
            drawCell(canvas, 178f, yRow, 110f, rowH, "TOTAL HORAS EXTRAS", null, linePaint, textPaint, Paint.Align.LEFT);
            drawCell(canvas, 288f, yRow, 22f, rowH, "S/.", null, linePaint, textCenterPaint, Paint.Align.CENTER);
            drawCell(canvas, 310f, yRow, 66f, rowH, formatMonto(val.getTotalHorasExtras()), null, linePaint, textCenterPaint, Paint.Align.CENTER);

            drawCell(canvas, 442f, yRow, 88f, rowH, "COSTO DIARIO", null, linePaint, textPaint, Paint.Align.LEFT);
            drawCell(canvas, 530f, yRow, 22f, rowH, "S/.", null, linePaint, textCenterPaint, Paint.Align.CENTER);
            drawCell(canvas, 552f, yRow, 44f, rowH, formatMonto(val.getCostoDiario()), null, linePaint, textCenterPaint, Paint.Align.CENTER);
            yRow += rowH;

            // Fila 24
            drawCell(canvas, 26f, yRow, 84f, rowH, "HORAS PERMITIDAS", null, linePaint, textPaint, Paint.Align.LEFT);
            drawCell(canvas, 110f, yRow, 68f, rowH, formatHorometro(val.getHorasPermitidas()), null, linePaint, textCenterPaint, Paint.Align.CENTER);
            // Cuadro de nota explicativa del 25% descuento (fusionado filas 24 a 26)
            canvas.drawRect(178f, yRow, 376f, yRow + (rowH * 3f), linePaint);
            Paint noteP = new Paint(italicPaint);
            noteP.setTextSize(6.5f);
            canvas.drawText("*El precio de la hora extra se calcula", 182f, yRow + 14f, noteP);
            canvas.drawText("aplicando un descuento del 25% sobre", 182f, yRow + 24f, noteP);
            canvas.drawText("la tarifa de la hora base.", 182f, yRow + 34f, noteP);

            drawCell(canvas, 442f, yRow, 88f, rowH, "COSTO PARCIAL", null, linePaint, textPaint, Paint.Align.LEFT);
            drawCell(canvas, 530f, yRow, 22f, rowH, "S/.", null, linePaint, textCenterPaint, Paint.Align.CENTER);
            drawCell(canvas, 552f, yRow, 44f, rowH, formatMonto(val.getCostoParcial()), null, linePaint, textCenterPaint, Paint.Align.CENTER);
            yRow += rowH;

            // Fila 25
            drawCell(canvas, 26f, yRow, 84f, rowH, "HORAS USADAS", null, linePaint, textPaint, Paint.Align.LEFT);
            drawCell(canvas, 110f, yRow, 68f, rowH, formatHorometro(val.getHorasUsadas()), null, linePaint, textCenterPaint, Paint.Align.CENTER);

            drawCell(canvas, 442f, yRow, 88f, rowH, "TOTAL HORAS EXTRAS", null, linePaint, textPaint, Paint.Align.LEFT);
            drawCell(canvas, 530f, yRow, 22f, rowH, "S/.", null, linePaint, textCenterPaint, Paint.Align.CENTER);
            drawCell(canvas, 552f, yRow, 44f, rowH, formatMonto(val.getTotalHorasExtras()), null, linePaint, textCenterPaint, Paint.Align.CENTER);
            yRow += rowH;

            // Fila 26
            drawCell(canvas, 26f, yRow, 84f, rowH, "HORAS EXTRAS", null, linePaint, textPaint, Paint.Align.LEFT);
            drawCell(canvas, 110f, yRow, 68f, rowH, formatHorometro(val.getHorasExtras()), null, linePaint, textCenterPaint, Paint.Align.CENTER);

            drawCell(canvas, 442f, yRow, 88f, rowH, "SUBTOTAL", null, linePaint, textPaint, Paint.Align.LEFT);
            drawCell(canvas, 530f, yRow, 22f, rowH, "S/.", null, linePaint, textCenterPaint, Paint.Align.CENTER);
            drawCell(canvas, 552f, yRow, 44f, rowH, formatMonto(val.getSubtotal()), null, linePaint, textCenterPaint, Paint.Align.CENTER);
            yRow += rowH;

            // Fila 27
            drawCell(canvas, 442f, yRow, 88f, rowH, "IGV", null, linePaint, textPaint, Paint.Align.LEFT);
            drawCell(canvas, 530f, yRow, 22f, rowH, "S/.", null, linePaint, textCenterPaint, Paint.Align.CENTER);
            drawCell(canvas, 552f, yRow, 44f, rowH, formatMonto(val.getIgv()), null, linePaint, textCenterPaint, Paint.Align.CENTER);
            yRow += rowH;

            // Fila 28
            drawCell(canvas, 442f, yRow, 88f, rowH, "TOTAL", null, linePaint, boldCenterPaint, Paint.Align.CENTER);
            drawCell(canvas, 530f, yRow, 22f, rowH, "S/.", null, linePaint, textCenterPaint, Paint.Align.CENTER);
            drawCell(canvas, 552f, yRow, 44f, rowH, formatMonto(val.getTotal()), null, linePaint, boldCenterPaint, Paint.Align.CENTER);
            yRow += rowH;

            // Fila 29 (Son: ...)
            if (val.getTotalEnLetras() != null && !val.getTotalEnLetras().isEmpty()) {
                canvas.drawText("Son: " + val.getTotalEnLetras(), 442f, yRow + 10f, boldPaint);
            }
            y = yRow + 24f;

        } else {
            // ====================================================
            // PLANTILLA TARIFA PLANA / NORMAL (media_...81fd8b35.xlsx)
            // ====================================================
            float yRow = y;

            // Fila 20: Título del equipo
            drawCell(canvas, 26f, yRow, 218f, rowH, descEq, navyBoxPaint, linePaint, whiteBoldCenter, Paint.Align.CENTER);
            yRow += rowH;

            // Fila 21
            drawCell(canvas, 26f, yRow, 152f, rowH, "DÍAS VALORIZADOS", null, linePaint, textPaint, Paint.Align.LEFT);
            drawCell(canvas, 178f, yRow, 66f, rowH, String.valueOf(val.getDiasValorizados()), null, linePaint, textCenterPaint, Paint.Align.CENTER);
            yRow += rowH;

            // Fila 22
            drawCell(canvas, 26f, yRow, 130f, rowH, "COSTO DIARIO", null, linePaint, textPaint, Paint.Align.LEFT);
            drawCell(canvas, 156f, yRow, 22f, rowH, "S/.", null, linePaint, textCenterPaint, Paint.Align.CENTER);
            drawCell(canvas, 178f, yRow, 66f, rowH, formatMonto(val.getCostoDiario()), null, linePaint, textCenterPaint, Paint.Align.CENTER);
            yRow += rowH;

            // Fila 23
            drawCell(canvas, 26f, yRow, 130f, rowH, "COSTO PARCIAL", null, linePaint, textPaint, Paint.Align.LEFT);
            drawCell(canvas, 156f, yRow, 22f, rowH, "S/.", null, linePaint, textCenterPaint, Paint.Align.CENTER);
            drawCell(canvas, 178f, yRow, 66f, rowH, formatMonto(val.getCostoParcial()), null, linePaint, textCenterPaint, Paint.Align.CENTER);
            yRow += rowH;

            // Fila 24
            drawCell(canvas, 26f, yRow, 130f, rowH, "IGV", null, linePaint, textPaint, Paint.Align.LEFT);
            drawCell(canvas, 156f, yRow, 22f, rowH, "S/.", null, linePaint, textCenterPaint, Paint.Align.CENTER);
            drawCell(canvas, 178f, yRow, 66f, rowH, formatMonto(val.getIgv()), null, linePaint, textCenterPaint, Paint.Align.CENTER);
            yRow += rowH;

            // Fila 25
            drawCell(canvas, 26f, yRow, 130f, rowH, "TOTAL", null, linePaint, boldCenterPaint, Paint.Align.CENTER);
            drawCell(canvas, 156f, yRow, 22f, rowH, "S/.", null, linePaint, textCenterPaint, Paint.Align.CENTER);
            drawCell(canvas, 178f, yRow, 66f, rowH, formatMonto(val.getTotal()), null, linePaint, boldCenterPaint, Paint.Align.CENTER);
            yRow += rowH;

            // Fila 26 (Son: ...)
            if (val.getTotalEnLetras() != null && !val.getTotalEnLetras().isEmpty()) {
                canvas.drawText("Son: " + val.getTotalEnLetras(), 26f, yRow + 10f, boldPaint);
            }
            y = yRow + 24f;
        }

        // ========================================================
        // 6. CUENTAS BANCARIAS Y NOTA DE TITULAR (Idéntico a filas 36 a 40)
        // ========================================================
        y = Math.max(y, 445f);

        List<String> cuentas = getCuentasBancarias();
        String ctaBcp = "Nº DE CUENTA DE AHORROS / BCP MONEDA SOLES  245-21165261077";
        String ctaDetraccion = "Nº DE CUENTA DE DETRACCIONES  00761027077";
        String ctaScotia = "Nº DE CUENTA DE AHORROS / SCOTIABANK  MONEDA SOLES  722- 7753048";
        String ctaBbvaSoles = "Nº DE CUENTA DE AHORROS / BBVA MONEDA SOLES 0011-0248-0200277022";
        String ctaBbvaDolares = "Nº DE CUENTA DE AHORROS / BBVA MONEDA DÓLARES 0011-0248-0200277030";

        for (String c : cuentas) {
            if (c.contains("BCP") && c.contains("SOLES")) ctaBcp = c;
            else if (c.contains("DETRACCION")) ctaDetraccion = c;
            else if (c.contains("SCOTIABANK")) ctaScotia = c;
            else if (c.contains("BBVA") && c.contains("SOLES")) ctaBbvaSoles = c;
            else if (c.contains("BBVA") && c.contains("DOLARES")) ctaBbvaDolares = c;
        }

        Paint footText = new Paint(textPaint);
        footText.setTextSize(7.5f);

        // Fila 1 de cuentas
        canvas.drawText(ctaBcp, xStart, y + 8f, footText);
        canvas.drawText(ctaDetraccion, 442f, y + 8f, footText);
        y += 14f;

        // Fila 2 de cuentas
        canvas.drawText(ctaScotia, xStart, y + 8f, footText);
        y += 14f;

        // Fila 3 de cuentas
        canvas.drawText(ctaBbvaSoles, xStart, y + 8f, footText);
        y += 14f;

        // Fila 4 de cuentas
        canvas.drawText(ctaBbvaDolares, xStart, y + 8f, footText);
        y += 14f;

        // Fila 5: NOTA de titular
        canvas.drawText("NOTA:  A favor de " + getPropietario(), xStart, y + 8f, footText);

        document.finishPage(page);

        String nombreArchivo = "Valorizacion_" + (numVal != null ? numVal : "temp") + ".pdf";
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

    private void drawCell(android.graphics.Canvas canvas, float left, float top, float width, float height,
                          String text, Paint bgPaint, Paint borderPaint, Paint textPaint, Paint.Align align) {
        if (bgPaint != null) {
            canvas.drawRect(left, top, left + width, top + height, bgPaint);
        }
        if (borderPaint != null) {
            canvas.drawRect(left, top, left + width, top + height, borderPaint);
        }
        if (text != null && !text.isEmpty() && textPaint != null) {
            textPaint.setTextAlign(align);
            float x;
            if (align == Paint.Align.CENTER) {
                x = left + (width / 2f);
            } else if (align == Paint.Align.RIGHT) {
                x = left + width - 4f;
            } else {
                x = left + 4f;
            }
            float y = top + (height / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f);
            canvas.drawText(text, x, y, textPaint);
        }
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
