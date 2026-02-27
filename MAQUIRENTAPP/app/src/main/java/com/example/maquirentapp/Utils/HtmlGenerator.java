package com.example.maquirentapp.Utils;

import android.content.Context;
import com.example.maquirentapp.Model.Cotizacion;
import com.example.maquirentapp.Model.ItemCotizacion;
import com.example.maquirentapp.R; // Asegúrate de importar R
import java.util.Locale;

public class HtmlGenerator {

    public String generarHtmlCotizacion(Context context, Cotizacion cotizacion) {
        String firmaBase64 = "";
        try {
            firmaBase64 = "data:image/png;base64," + ImageUtils.convertirDrawableABase64(context, R.drawable.firmahep);
        } catch (Exception e) {
            firmaBase64 = "";
        }

        StringBuilder html = new StringBuilder();

        html.append("<html><head><style>");
        // Configuración de página A4
        html.append("@page { size: A4; margin: 20mm 15mm 20mm 15mm; }"); // Margenes estándar de Word
        html.append("body { font-family: 'Times New Roman', 'Arial', sans-serif; font-size: 10pt; color: #000; line-height: 1.3; padding: 60px;}");

        // Configuración de Tablas para que se repita el encabezado
        html.append("thead { display: table-header-group; }");
        html.append("tfoot { display: table-footer-group; }");
        html.append("tr { page-break-inside: avoid; }"); // Evita cortar filas a la mitad

        // Estilos de tabla
        html.append("table { width: 100%; border-collapse: collapse; margin-top: 10px; margin-bottom: 10px; }");
        html.append("th { background-color: #D9D9D9; font-weight: bold; text-align: center; border: 1px solid #000; padding: 4px; font-size: 9pt; }");
        html.append("td { border: 1px solid #000; padding: 4px; font-size: 9pt; vertical-align: middle; text-align: center; }");
        html.append(".td-left { text-align: left; }");
        html.append(".no-border { border: none; }");

        // Estilos del Título (MAQUIRENT)
        html.append(".titulo-maqui { color: #ff8c8c; font-weight: 900; font-size: 14pt; font-family: 'Arial Black', Arial, sans-serif; }");
        html.append(".titulo-rent { color: #80b6e0; font-weight: 900; font-size: 14pt; font-family: 'Arial Black', Arial, sans-serif; }");
        html.append(".color-encabezado { color: #818080 }");
        html.append("</style></head><body>");

        // --- TABLA MAESTRA (Layout General) ---
        html.append("<table style='width:100%; border:none;'>");

        // 1. ENCABEZADO (Logo Texto + Título)
        html.append("<thead><tr><td class='no-border'>");
        // LOGO TEXTO: MAQUI (Rojo) RENT (Celeste)
        html.append("<div style='text-align:left; margin-bottom:20px;'>");
        html.append("<div><span class='titulo-maqui'>MAQUI</span><span class='titulo-rent'>RENT</span><span class='color-encabezado'> SERVICIOS GENERALES</span></div>");
        html.append("<div class='color-encabezado'>");
        html.append("<div style='display: flex; justify-content: space-between; align-items: center;'>");
        html.append("<span>HUGO ALBERTO ESQUIVEL PANDO</span>");
        html.append("<span>RUC: 10266739414</span>");
        html.append("</div>");
        html.append("<div><span>SERVICIOS Y ALQUILER DE GRUPOS ELECTRÓGENOS, COMPRESORAS Y EQUIPOS DE CONSTRUCCIÓN</span></div>");
        html.append("<div><span>AV. ATAHUALPA 810 - CAJAMARCA | TELÉF. 976959490 - 955425015 - 969775015</span></div>");
        html.append("<hr>");
        html.append("</div>");
        html.append("</div>");

        html.append("<div style='text-align:center; margin-bottom:20px;'>");
        html.append("<h2 style='text-decoration: underline; margin-top: 5px; font-size: 14pt;'>COTIZACIÓN DE EQUIPOS EN ALQUILER</h2>");
        html.append("</div>");
        html.append("</td></tr></thead>");

        // 2. CUERPO
        html.append("<tbody><tr><td class='no-border'>");

        // --- DATOS DEL CLIENTE ---
        html.append("<div style='text-align:left; font-size: 11pt; margin-bottom: 15px;'>");
        // Fecha alineada a la derecha (si quieres) o abajo
        html.append("<p style='text-align: left; margin-bottom: 10px;'>").append(cotizacion.getFechaEmision()).append("</p>");

        html.append("<table style='border:none; margin:0; padding:0;'>");
        html.append("<tr><td class='no-border td-left' style='width: 60%;'><b>Srs. ").append(cotizacion.getClienteNombre()).append("</b></td>");
        html.append("<td class='no-border td-left'><b>RUC:</b> ").append(cotizacion.getClienteRuc()).append("</td></tr>");
        html.append("<tr><td class='no-border td-left' colspan='2'><b>Lugar de trabajo:</b> ").append(cotizacion.getLugarTrabajo()).append("</td></tr>");
        html.append("</table>");

        html.append("<p style='margin-top: 10px; text-align: justify;'>Por la presente me es grato saludarlo a la vez, de acuerdo a lo conversado y con la intención de colaborar con el cumplimiento de sus metas le hago llegar una proforma de alquiler de equipos, en espera de llegar a un acuerdo comercial.</p>");
        html.append("</div>");

        // --- TABLA 1: ITEMS (7 Columnas exactas) ---
        html.append("<table>");
        html.append("<thead><tr>");
        html.append("<th style='width:15%'>EQUIPO</th>");
        html.append("<th style='width:10%'>POTENCIA</th>");
        html.append("<th style='width:10%'>MODO DE<br>TRABAJO</th>");
        html.append("<th style='width:10%'>MARCA</th>");
        html.append("<th style='width:25%'>INCLUYE</th>");
        html.append("<th style='width:15%'>PRECIO POR MES<br>PARCIAL (" + cotizacion.getMoneda() + ")</th>");
        html.append("<th style='width:15%'>PRECIO POR MES<br>(INCLUYE IGV)</th>");
        html.append("</tr></thead><tbody>");

        for (ItemCotizacion item : cotizacion.getItems()) {
            double precioConIgv = item.getPrecioMensual() * 1.18;
            html.append("<tr>");
            html.append("<td>").append(item.getDescripcionEquipo()).append("</td>");
            html.append("<td>").append(item.getPotencia()).append("</td>");
            html.append("<td>").append(item.getModoTrabajo()).append("</td>");
            html.append("<td>").append(item.getMarca()).append("</td>");
            html.append("<td style='font-size: 8pt;'>").append(item.getIncluye()).append("</td>");
            html.append("<td>").append(String.format(Locale.US, "%.2f", item.getPrecioMensual())).append("</td>");
            html.append("<td>").append(String.format(Locale.US, "%.2f", precioConIgv)).append("</td>");
            html.append("</tr>");
        }

        // Fila de TOTALES
        html.append("<tr>");
        html.append("<td colspan='5' style='text-align:right; border:none; font-weight:bold; padding-right: 10px;'>TOTAL:</td>");
        // TOTAL PARCIAL (Subtotal)
        html.append("<td style='font-weight:bold; background-color: #F2F2F2;'>").append(String.format(Locale.US, "%.2f", cotizacion.getSubtotalGlobal())).append("</td>");
        // TOTAL CON IGV
        html.append("<td style='font-weight:bold; background-color: #F2F2F2;'>").append(String.format(Locale.US, "%.2f", cotizacion.getTotalGlobal())).append("</td>");
        html.append("</tr>");
        html.append("</tbody></table>");

        // --- TABLA 2: HORA EXTRA ---
        html.append("<div style='margin-top: 15px; text-align: left; '>");
        html.append("<p style='font-weight:bold; margin-bottom:5px;'>Costo de hora extra a partir de ").append(cotizacion.getHorasMinimas()).append(" horas sin IGV:</p>");

        html.append("<table style='width:60%;'>"); // Tabla más angosta para HE
        html.append("<thead><tr><th>EQUIPO / CÓDIGO</th><th>PRECIO H.E. (" + cotizacion.getMoneda() + ")</th></tr></thead><tbody>");
        for (ItemCotizacion item : cotizacion.getItems()) {
            html.append("<tr>");
            // Usamos descripcion o marca + potencia para identificar
            html.append("<td>").append(item.getDescripcionEquipo()).append("</td>");
            html.append("<td>").append(String.format(Locale.US, "%.2f", item.getPrecioHoraExtra())).append("</td>");
            html.append("</tr>");
        }
        html.append("</tbody></table>");
        html.append("</div>");

        // --- CONDICIONES COMERCIALES ---
        html.append("<div style='margin-top: 20px; text-align: left;'>");
        html.append("<h3 style='text-decoration: underline; font-size: 11pt;'>CONDICIONES COMERCIALES</h3>");
        html.append("<ul style='padding-left: 20px; list-style-type: disc;'>");
        html.append("<li>LOS EQUIPOS SE ALQUILAN SIN COMBUSTIBLE NI OPERADOR.</li>");
        html.append("<li>EL PAGO DEL ALQUILER SE REALIZARÁ A 30 DÍAS DE LA FACTURACIÓN.</li>");
        html.append("<li>TARIFA SE APLICA CON HORAS MÍNIMAS DE <b>").append(cotizacion.getHorasMinimas()).append(" HORAS MENSUALES</b>, EXCESOS SE FACTURARÁN SEGÚN HORÓMETRO.</li>");
        html.append("<li>LOS EQUIPOS DEBEN SER DEVUELTOS EN LAS MISMAS CONDICIONES EN QUE FUERON ENTREGADOS SIN MÁS DETERIORO DEL QUE PUEDE PRODUCIRSE POR EL USO NORMAL. EN CASO DE PRESENTARSE CUALQUIER DAÑO, EL CLIENTE SE HARÁ RESPONSABLE DE LAS REPARACIONES, SEGÚN LOS CARGOS POR DAÑOS VIGENTES A LA DEVOLUCIÓN.</li>");
        html.append("<li>EN CASO DE PÉRDIDA DE LOS EQUIPOS, EL CLIENTE SE HARÁ RESPONSABLE DE LA DEVOLUCIÓN DEL MISMO, YA SEA COMO EQUIPO O EN EFECTIVO, TENIENDO EN CUENTA LA FACTURA DE COMPRA.</li>");
        html.append("<li>LA INSPECCIÓN DIARIA ES RESPONSABILIDAD DEL CLIENTE (AGUA Y ACEITE DE MOTOR).</li>");
        html.append("<li>LOS ADITAMENTOS DEL EQUIPO INCLUIDOS SON BANDEJA Y KIT ANTIDERRAME, EXTINTOR, BARRA PUESTA A TIERRA.</li>");
        html.append("<li>EL EQUIPO SE ENTREGA CON ACEITE DE MOTOR Y FILTROS NUEVOS.</li>");
        html.append("<li>PARA LOS MANTENIMIENTOS DE LOS GENERADORES EN MINA, SE PUEDE CAPACITAR A UN PERSONAL DE LA EMPRESA CONTRATISTA Y SE LE OTORGARÁ TODOS LOS INSUMOS PARA QUE PUEDA REALIZAR EL MANTENIMIENTO PREVENTIVO CADA 250 HORAS.</li>");
        html.append("<li>LA PRESENTE COTIZACIÓN TIENE UNA VALIDEZ DE 15 DÍAS DESDE SU CREACIÓN.</li>");
        html.append("</ul>");
        html.append("</div>");

        // --- FIRMA (Al final) ---
        if (!firmaBase64.isEmpty()) {
            html.append("<div style='text-align: center; margin-top: 40px;'>");
            html.append("<img src='").append(firmaBase64).append("' style='width: 200px; height: auto;' />");
            html.append("</div>");
        } else {
            // Espacio para firmar si no hay imagen
            html.append("<div style='text-align: center; margin-top: 80px;'>");
            html.append("<p>______________________________________</p>");
            html.append("<p><b>MAQUIRENT SERVICIOS GENERALES</b></p>");
            html.append("</div>");
        }

        html.append("</td></tr></tbody></table>"); // Fin Tabla Maestra
        html.append("</body></html>");

        return html.toString();
    }
}