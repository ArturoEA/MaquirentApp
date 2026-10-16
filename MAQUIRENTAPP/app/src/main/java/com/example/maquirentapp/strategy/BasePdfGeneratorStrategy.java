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
import com.example.maquirentapp.Model.ItemValorizacion;
import com.example.maquirentapp.Model.Mantenimiento;
import com.example.maquirentapp.Model.Valorizacion;
import com.example.maquirentapp.R;
import com.example.maquirentapp.Utils.ImageUtils;
import com.example.maquirentapp.Utils.InformePdfVectorialGenerator;
import com.example.maquirentapp.Utils.PdfGenerator;

import java.io.File;
import java.io.FileOutputStream;
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

        html.append("<table style='width:100%; border:none;'>");
        html.append("<thead><tr><td class='no-border'>");

        // Encabezado corporativo de la empresa
        html.append(generarEncabezadoHtml(context));

        html.append("<div style='text-align:center; margin-bottom:20px;'>");
        html.append("<h2 style='text-decoration: underline; margin-top: 5px; font-size: 14pt;'>COTIZACIÓN DE EQUIPOS EN ALQUILER</h2>");
        html.append("</div>");
        html.append("</td></tr></thead>");

        html.append("<tbody><tr><td class='no-border'>");
        html.append("<div style='text-align:left; font-size: 11pt; margin-bottom: 15px;'>");
        html.append("<p style='text-align: left; margin-bottom: 10px;'>").append(cotizacion.getFechaEmision()).append("</p>");
        html.append("<table style='border:none; margin:0; padding:0;'>");
        html.append("<tr><td class='no-border td-left' style='width: 60%;'><b>Srs. ").append(cotizacion.getClienteNombre()).append("</b></td>");
        html.append("<td class='no-border td-left'><b>RUC:</b> ").append(cotizacion.getClienteRuc()).append("</td></tr>");
        html.append("<tr><td class='no-border td-left' colspan='2'><b>Lugar de trabajo:</b> ").append(cotizacion.getLugarTrabajo()).append("</td></tr>");
        html.append("</table>");
        html.append("<p style='margin-top: 10px; text-align: justify;'>Por la presente me es grato saludarlo a la vez, de acuerdo a lo conversado y con la intención de colaborar con el cumplimiento de sus metas le hago llegar una proforma de alquiler de equipos, en espera de llegar a un acuerdo comercial.</p>");
        html.append("</div>");

        // Items
        html.append("<table><thead><tr>");
        html.append("<th style='width:15%'>EQUIPO</th>");
        html.append("<th style='width:10%'>POTENCIA</th>");
        html.append("<th style='width:10%'>MODO DE<br>TRABAJO</th>");
        html.append("<th style='width:10%'>MARCA</th>");
        html.append("<th style='width:25%'>INCLUYE</th>");
        html.append("<th style='width:15%'>PRECIO POR MES<br>PARCIAL (" + cotizacion.getMoneda() + ")</th>");
        html.append("<th style='width:15%'>PRECIO POR MES<br>(INCLUYE " + getEtiquetaImpuesto() + ")</th>");
        html.append("</tr></thead><tbody>");

        double factorImpuesto = 1.0 + getTasaImpuesto();
        for (ItemCotizacion item : cotizacion.getItems()) {
            double precioConImpuesto = item.getPrecioMensual() * factorImpuesto;
            html.append("<tr>");
            html.append("<td>").append(item.getDescripcionEquipo()).append("</td>");
            html.append("<td>").append(item.getPotencia()).append("</td>");
            html.append("<td>").append(item.getModoTrabajo()).append("</td>");
            html.append("<td>").append(item.getMarca()).append("</td>");
            html.append("<td style='font-size: 8pt;'>").append(item.getIncluye()).append("</td>");
            html.append("<td>").append(String.format(Locale.US, "%.2f", item.getPrecioMensual())).append("</td>");
            html.append("<td>").append(String.format(Locale.US, "%.2f", precioConImpuesto)).append("</td>");
            html.append("</tr>");
        }

        html.append("<tr>");
        html.append("<td colspan='5' style='text-align:right; border:none; font-weight:bold; padding-right: 10px;'>TOTAL:</td>");
        html.append("<td style='font-weight:bold; background-color: #F2F2F2;'>").append(String.format(Locale.US, "%.2f", cotizacion.getSubtotalGlobal())).append("</td>");
        html.append("<td style='font-weight:bold; background-color: #F2F2F2;'>").append(String.format(Locale.US, "%.2f", cotizacion.getTotalGlobal())).append("</td>");
        html.append("</tr>");
        html.append("</tbody></table>");

        // Hora Extra
        html.append("<div style='margin-top: 15px; text-align: left; '>");
        html.append("<p style='font-weight:bold; margin-bottom:5px;'>Costo de hora extra a partir de ").append(cotizacion.getHorasMinimas()).append(" horas sin impuestos:</p>");
        html.append("<table style='width:60%;'>");
        html.append("<thead><tr><th>EQUIPO / CÓDIGO</th><th>PRECIO H.E. (" + cotizacion.getMoneda() + ")</th></tr></thead><tbody>");
        for (ItemCotizacion item : cotizacion.getItems()) {
            html.append("<tr>");
            html.append("<td>").append(item.getDescripcionEquipo()).append("</td>");
            html.append("<td>").append(String.format(Locale.US, "%.2f", item.getPrecioHoraExtra())).append("</td>");
            html.append("</tr>");
        }
        html.append("</tbody></table>");
        html.append("</div>");

        // Condiciones
        html.append(generarCondicionesComercialesHtml(cotizacion));

        // Firma
        if (!firmaBase64.isEmpty()) {
            html.append("<div style='text-align: center; margin-top: 40px;'>");
            html.append("<img src='").append(firmaBase64).append("' style='width: 200px; height: auto;' />");
            html.append("</div>");
        } else {
            html.append("<div style='text-align: center; margin-top: 80px;'>");
            html.append("<p>______________________________________</p>");
            html.append("<p><b>").append(getNombreEmpresa()).append("</b></p>");
            html.append("</div>");
        }

        html.append("</td></tr></tbody></table>");
        html.append("</body></html>");

        return html.toString();
    }

    @Override
    public File generarPdfValorizacion(Context context, Valorizacion val) throws Exception {
        int A4_WIDTH_PTS = 595;
        int A4_HEIGHT_PTS = 842;

        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(A4_WIDTH_PTS, A4_HEIGHT_PTS, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        android.graphics.Canvas canvas = page.getCanvas();

        Paint bgPaint = new Paint();
        bgPaint.setColor(Color.WHITE);
        canvas.drawRect(0, 0, A4_WIDTH_PTS, A4_HEIGHT_PTS, bgPaint);

        Paint titlePaint = new Paint();
        titlePaint.setColor(Color.parseColor("#1A365D"));
        titlePaint.setTextSize(16);
        titlePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        Paint subtitlePaint = new Paint();
        subtitlePaint.setColor(Color.parseColor("#4A5568"));
        subtitlePaint.setTextSize(9);

        Paint boldPaint = new Paint();
        boldPaint.setColor(Color.BLACK);
        boldPaint.setTextSize(9);
        boldPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        Paint textPaint = new Paint();
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(9);

        Paint linePaint = new Paint();
        linePaint.setColor(Color.parseColor("#CBD5E0"));
        linePaint.setStrokeWidth(1);

        Paint headerBoxPaint = new Paint();
        headerBoxPaint.setColor(Color.parseColor("#2B6CB0"));

        Paint headerTextPaint = new Paint();
        headerTextPaint.setColor(Color.WHITE);
        headerTextPaint.setTextSize(9);
        headerTextPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        int xStart = 40;
        int xEnd = A4_WIDTH_PTS - 40;
        int y = 50;

        // 1. Cabecera dinámica de la empresa
        canvas.drawText(getNombreEmpresa(), xStart, y, titlePaint);
        y += 14;
        canvas.drawText("RUC: " + getRuc() + " | " + getPropietario(), xStart, y, subtitlePaint);
        y += 12;
        canvas.drawText(getRubro(), xStart, y, subtitlePaint);
        y += 15;
        canvas.drawLine(xStart, y, xEnd, y, linePaint);
        y += 25;

        // 2. Título del Documento
        String numVal = val.getNumeroValorizacion() != null ? val.getNumeroValorizacion() : "-";
        canvas.drawText("VALORIZACIÓN DE EQUIPOS N° " + numVal, xStart, y, titlePaint);
        y += 20;

        // 3. Datos del Cliente
        canvas.drawText("Cliente:", xStart, y, boldPaint);
        canvas.drawText(val.getNombreCliente() != null ? val.getNombreCliente() : "-", xStart + 50, y, textPaint);
        canvas.drawText("Fecha Emisión:", 380, y, boldPaint);
        canvas.drawText(val.getFechaEmision() != null ? val.getFechaEmision() : "-", 455, y, textPaint);
        y += 15;

        canvas.drawText("RUC:", xStart, y, boldPaint);
        canvas.drawText(val.getClienteRuc() != null ? val.getClienteRuc() : "-", xStart + 50, y, textPaint);
        canvas.drawText("Moneda:", 380, y, boldPaint);
        canvas.drawText(val.getMoneda() != null ? val.getMoneda() : "USD", 455, y, textPaint);
        y += 15;

        canvas.drawText("Ubicación:", xStart, y, boldPaint);
        canvas.drawText(val.getUbicacionTrabajo() != null ? val.getUbicacionTrabajo() : "-", xStart + 50, y, textPaint);
        y += 25;

        // 4. Tabla de Items
        int rowHeight = 22;
        canvas.drawRect(xStart, y, xEnd, y + rowHeight, headerBoxPaint);
        canvas.drawText("EQUIPO", xStart + 8, y + 15, headerTextPaint);
        canvas.drawText("PERÍODO", xStart + 170, y + 15, headerTextPaint);
        canvas.drawText("HORAS", xStart + 290, y + 15, headerTextPaint);
        canvas.drawText("TARIFA", xStart + 360, y + 15, headerTextPaint);
        canvas.drawText("TOTAL", xStart + 430, y + 15, headerTextPaint);
        y += rowHeight;

        List<ItemValorizacion> items = val.getItems();
        if (items != null) {
            Paint altRowPaint = new Paint();
            altRowPaint.setColor(Color.parseColor("#F7FAFC"));
            int rowIndex = 0;
            for (ItemValorizacion item : items) {
                if (rowIndex % 2 == 1) {
                    canvas.drawRect(xStart, y, xEnd, y + rowHeight, altRowPaint);
                }
                String equipo = item.getDescripcionEquipo() != null ? item.getDescripcionEquipo() : "-";
                if (equipo.length() > 25) equipo = equipo.substring(0, 22) + "...";
                String periodo = (item.getFechaInicio() != null ? item.getFechaInicio() : "") + " - " +
                        (item.getFechaFin() != null ? item.getFechaFin() : "");
                String horas = String.format(Locale.US, "%.1f hrs", item.getHorasTrabajadas());
                String tarifa = String.format(Locale.US, "%.2f", item.getPrecioMes());
                String totalItem = String.format(Locale.US, "%.2f", item.getTotalItem());

                canvas.drawText(equipo, xStart + 8, y + 15, textPaint);
                canvas.drawText(periodo, xStart + 170, y + 15, textPaint);
                canvas.drawText(horas, xStart + 290, y + 15, textPaint);
                canvas.drawText(tarifa, xStart + 360, y + 15, textPaint);
                canvas.drawText(totalItem, xStart + 430, y + 15, boldPaint);

                canvas.drawLine(xStart, y + rowHeight, xEnd, y + rowHeight, linePaint);
                y += rowHeight;
                rowIndex++;
            }
        }

        y += 20;
        int totalBoxX = 350;
        canvas.drawText("SUBTOTAL:", totalBoxX, y, boldPaint);
        canvas.drawText(String.format(Locale.US, "%.2f", val.getSubtotal()), totalBoxX + 90, y, textPaint);
        y += 16;

        canvas.drawText(getEtiquetaImpuesto() + ":", totalBoxX, y, boldPaint);
        canvas.drawText(String.format(Locale.US, "%.2f", val.getIgv()), totalBoxX + 90, y, textPaint);
        y += 16;

        canvas.drawLine(totalBoxX, y - 4, xEnd, y - 4, linePaint);
        canvas.drawText("TOTAL:", totalBoxX, y + 8, titlePaint);
        canvas.drawText(String.format(Locale.US, "%.2f", val.getTotal()), totalBoxX + 90, y + 8, titlePaint);

        // Firma
        y = A4_HEIGHT_PTS - 80;
        canvas.drawLine(xStart + 150, y, xEnd - 150, y, linePaint);
        y += 15;
        Paint centerPaint = new Paint(boldPaint);
        centerPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(getNombreEmpresa(), (float) A4_WIDTH_PTS / 2, y, centerPaint);

        document.finishPage(page);

        String nombreArchivo = "Valorizacion_" + (val.getNumeroValorizacion() != null ? val.getNumeroValorizacion() : "temp") + ".pdf";
        File dir = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "Valorizaciones");
        if (!dir.exists()) dir.mkdirs();
        File archivoFinal = new File(dir, nombreArchivo);

        try (FileOutputStream fos = new FileOutputStream(archivoFinal)) {
            document.writeTo(fos);
        } finally {
            document.close();
        }

        return archivoFinal;
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
