package com.example.maquirentapp.Utils;

import com.example.maquirentapp.Model.Mantenimiento;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

public class InformeMantenimientoPdfGenerator {
    // ENCABEZADO
    private static String getEncabezadoHtml() {
        StringBuilder html = new StringBuilder();
        html.append("<div style='text-align:left; margin-bottom:20px;'>");
        html.append("<div><span class='titulo-maqui'>MAQUI</span><span class='titulo-rent'>RENT</span><span class='color-encabezado'> SERVICIOS GENERALES</span></div>");
        html.append("<div class='color-encabezado' style='font-size:10px; font-weight:bold; color:#555;'>");
        html.append("<div style='display: flex; justify-content: space-between; align-items: center;'>");
        html.append("<span>HUGO ALBERTO ESQUIVEL PANDO</span>");
        html.append("<span>RUC: 10266739414</span>");
        html.append("</div>");
        html.append("<div style='margin-top:2px;'><span>SERVICIOS Y ALQUILER DE GRUPOS ELECTRÓGENOS, COMPRESORAS Y EQUIPOS DE CONSTRUCCIÓN</span></div>");
        html.append("<div style='margin-top:2px;'><span>AV. ATAHUALPA 810 - CAJAMARCA | TELÉF. 976959490 - 955425015 - 969775015</span></div>");
        html.append("<hr style='border-top: 2px solid black; margin-top:5px;'>");
        html.append("</div>");
        html.append("</div>");
        return html.toString();
    }

    public static String generarInforme(
            Mantenimiento mantenimientoActual, String codigoGrupo,
            String estado, String ubicacion, String defServicio,
            String cliente, String lugar, String aceite, String cantAceite,
            String contacto, String fallas, String trabajos,
            String nombreTecnico, String urlFirmaTecnico,
            String nombreSupervisor, String urlFirmaSupervisor,
            Map<String, String> codigosFiltros) {

        StringBuilder html = new StringBuilder();

        // 1. CSS LIMPIO. A4 Standard (595x842)
        html.append("<html><head><style>");
        html.append("body { font-family: 'Helvetica', 'Arial', sans-serif; font-size: 11px; color: black; margin: 0; padding: 20px; width: 555px; background-color: #ffffff; }");
        html.append(".titulo-maqui { color: #000000; font-weight: bold; font-size: 24px; }");
        html.append(".titulo-rent { color: #FF0000; font-weight: bold; font-size: 24px; }");
        html.append(".page-break { page-break-before: always; }");
        html.append("table { width: 100%; border-collapse: collapse; margin-bottom: 10px; }");
        html.append("th, td { border: 1px solid black; padding: 4px 6px; text-align: left; font-size: 10px; }");
        html.append("th { background-color: #f2f2f2; font-weight: bold; }");
        html.append(".titulo-seccion { font-weight: bold; font-size: 12px; margin-top: 10px; margin-bottom: 5px; background-color: #ddd; padding: 4px; border: 1px solid black; }");
        html.append(".check-box { font-family: 'Courier New', monospace; font-weight: bold; font-size: 12px; }");
        html.append("</style></head><body>");

        String nombreClienteFinal = cliente.isEmpty() ? mantenimientoActual.getEmpresa() : cliente;
        String fechaHoy = new SimpleDateFormat("dd 'de' MMMM 'del' yyyy", new Locale("es", "ES")).format(new Date());
        String proxHorometro = "";
        try {
            double hActual = Double.parseDouble(mantenimientoActual.getHorometro());
            proxHorometro = String.valueOf(hActual + 250);
        } catch (Exception e) { proxHorometro = "N/A"; }

        // ================= PÁGINA 1: CARTA DE PRESENTACIÓN =================
        html.append("<div>");
        html.append(getEncabezadoHtml());

        html.append("<div style='text-align:center; font-weight:bold; font-size:14px; margin: 20px 0; text-decoration: underline;'>");
        html.append("INFORME MANTENIMIENTO GRUPO ELECTRÓGENO");
        html.append("</div>");

        html.append("<p><b>Cajamarca, ").append(fechaHoy).append(".</b></p>");
        html.append("<p><b>Cliente: </b>").append(nombreClienteFinal).append("</p>");

        html.append("<p style='text-align:justify; line-height: 1.5;'>");
        html.append("El presente documento certifica que se ha realizado el mantenimiento preventivo y limpieza ");
        html.append("del grupo electrógeno, con el fin de evitar paralizaciones no programadas; se garantiza el ");
        html.append("correcto funcionamiento para su operación del generador de potencia <b>").append(codigoGrupo).append("</b> hasta ");
        html.append("su próximo mantenimiento preventivo. A continuación, se presentan los documentos que ");
        html.append("indican los cambios de elementos que se ha realizado en campo de: filtros de aceite, de aire, ");
        html.append("separador, entre otros, además de una inspección visual general al equipo.");
        html.append("</p>");

        html.append("<p><b>El grupo electrógeno cuenta con:</b></p>");
        html.append("<table style='border: none;'><tr>");
        html.append("<td style='border: none;'>Bandeja antiderrame: [ X ]</td>");
        html.append("<td style='border: none;'>Extintor PQS: [ X ]</td>");
        html.append("</tr><tr>");
        html.append("<td style='border: none;'>Kit antiderrame: [ X ]</td>");
        html.append("<td style='border: none;'>Puesta a tierra: [ X ]</td>");
        html.append("</tr></table>");

        html.append("<br><p>Atentamente:</p>");
        html.append("<div style='margin-top: 40px;'>");
        html.append("<p>_______________________________</p>");
        html.append("<p><b>Hugo Esquivel Pando</b><br>GERENTE<br>MAQUIRENT SERVICIOS GENERALES</p>");
        html.append("</div>");
        html.append("</div>");


        // ================= PÁGINA 2: INFORME DE SERVICIO EN TALLER Y CAMPO =================
        html.append("<div class='page-break'></div>");
        html.append(getEncabezadoHtml());

        html.append("<div style='text-align:center; font-weight:bold; font-size:14px; margin-bottom: 10px;'>");
        html.append("INFORME DE SERVICIO EN TALLER Y CAMPO");
        html.append("</div>");

        html.append("<table>");
        html.append("<tr><td><b>CÓDIGO DE EQUIPO:</b></td><td>").append(codigoGrupo).append("</td><td><b>FECHA:</b></td><td>").append(mantenimientoActual.getFecha()).append("</td></tr>");
        html.append("<tr><td><b>CLIENTE:</b></td><td colspan='3'>").append(nombreClienteFinal).append("</td></tr>");
        html.append("<tr><td><b>LUGAR:</b></td><td colspan='3'>").append(lugar).append("</td></tr>");
        html.append("</table>");

        html.append("<table>");
        html.append("<tr><th>EQUIPO Y MOTOR</th><th>HORÓMETRO ACTUAL</th><th>PRÓXIMO SERVICIO</th></tr>");
        html.append("<tr><td>").append(codigoGrupo).append("</td><td>").append(mantenimientoActual.getHorometro()).append(" hrs</td><td>").append(proxHorometro).append(" hrs</td></tr>");
        html.append("</table>");

        html.append("<div class='titulo-seccion'>SERVICIO PREVENTIVO Y CAMBIO DE FILTROS</div>");
        html.append("<table>");
        html.append("<tr><td colspan='2'><b>Cambio de aceite de motor (Marca y cantidad):</b> ").append(aceite).append(" (").append(cantAceite).append(")</td></tr>");

        String checkAceite = codigosFiltros.containsKey("Filtro de aceite") ? "[ X ]" : "[   ]";
        String checkAire = codigosFiltros.containsKey("Filtro de aire") ? "[ X ]" : "[   ]";
        String checkComb = codigosFiltros.containsKey("Filtro de combustible") ? "[ X ]" : "[   ]";
        String checkSep = codigosFiltros.containsKey("Filtro separador de agua") ? "[ X ]" : "[   ]";

        html.append("<tr><td><span class='check-box'>").append(checkAceite).append("</span> De aceite: ").append(codigosFiltros.getOrDefault("Filtro de aceite", "----")).append("</td>");
        html.append("<td><span class='check-box'>").append(checkAire).append("</span> De aire: ").append(codigosFiltros.getOrDefault("Filtro de aire", "----")).append("</td></tr>");
        html.append("<tr><td><span class='check-box'>").append(checkComb).append("</span> De combustible: ").append(codigosFiltros.getOrDefault("Filtro de combustible", "----")).append("</td>");
        html.append("<td><span class='check-box'>").append(checkSep).append("</span> Separador de agua: ").append(codigosFiltros.getOrDefault("Filtro separador de agua", "----")).append("</td></tr>");
        html.append("</table>");

        html.append("<div class='titulo-seccion'>OBSERVACIONES Y DETALLES DEL TRABAJO</div>");
        html.append("<table>");
        html.append("<tr><td colspan='2'><b>¿Cómo encontró la máquina?:</b> ").append(estado).append("</td></tr>");
        html.append("<tr><td colspan='2'><b>¿Dónde encontró la máquina?:</b> ").append(ubicacion).append("</td></tr>");
        html.append("<tr><td colspan='2'><b>¿Qué problemas y/o fallas encontró?:</b><br>").append(fallas).append("</td></tr>");
        html.append("<tr><td colspan='2'><b>¿Qué trabajos realizó? ¿Cómo corrigió la falla?:</b><br>").append(trabajos).append("</td></tr>");
        html.append("<tr><td colspan='2'><b>¿Con quién trató? (Sr./Ing.):</b> ").append(contacto).append("</td></tr>");
        html.append("</table>");

        html.append("<div class='titulo-seccion'>DEFINICIÓN DEL SERVICIO</div>");
        html.append("<table style='border: none;'><tr>");
        html.append("<td style='border: none;'><span class='check-box'>").append(defServicio.equals("Mantenimiento") ? "[ X ]" : "[   ]").append("</span> Mantenimiento</td>");
        html.append("<td style='border: none;'><span class='check-box'>").append(defServicio.equals("Evaluación") ? "[ X ]" : "[   ]").append("</span> Evaluación</td>");
        html.append("<td style='border: none;'><span class='check-box'>").append(defServicio.equals("Entrega") ? "[ X ]" : "[   ]").append("</span> Entrega</td>");
        html.append("<td style='border: none;'><span class='check-box'>").append(defServicio.equals("Realizar Ajuste") ? "[ X ]" : "[   ]").append("</span> Realizar Ajuste</td>");
        html.append("</tr></table>");

        html.append("<table style='width:100%; border:none; margin-top:20px; text-align:center;'><tr>");
        html.append("<td style='border:none; width:50%;'>");
        if (!urlFirmaTecnico.isEmpty()) html.append("<img src='").append(urlFirmaTecnico).append("' style='max-height:60px; max-width:150px;'/><br>");
        else html.append("<br><br><br>");
        html.append("________________________________<br><b>").append(nombreTecnico).append("</b><br>Mecánico / Encargado</td>");

        html.append("<td style='border:none; width:50%;'>");
        if (!urlFirmaSupervisor.isEmpty()) html.append("<img src='").append(urlFirmaSupervisor).append("' style='max-height:60px; max-width:150px;'/><br>");
        else html.append("<br><br><br>");
        String supNombre = nombreSupervisor.equals("Ninguno") ? "" : nombreSupervisor;
        html.append("________________________________<br><b>").append(supNombre).append("</b><br>Supervisor</td>");
        html.append("</tr></table>");


        // ================= PÁGINA 3: PLANIFICACIÓN (+250 HORAS) =================
        html.append("<div class='page-break'></div>");
        html.append(getEncabezadoHtml());

        html.append("<div style='text-align:center; font-weight:bold; font-size:14px; margin-bottom: 15px;'>");
        html.append("RUTINA DE MANTENIMIENTO PREVENTIVO PLANIFICADO");
        html.append("</div>");

        html.append("<table>");
        html.append("<tr><th>ACTIVIDAD</th><th>FRECUENCIA</th><th>FECHA INTERVENCIÓN</th><th>HORÓMETRO INTERVENCIÓN</th><th>PRÓX. HORÓMETRO</th></tr>");
        String[] rutinas = {"Filtros de aire", "Limpieza exterior del radiador", "Cambio de aceite de motor", "Cambio de filtros de petróleo y aceite"};
        for(String rutina : rutinas) {
            html.append("<tr><td>").append(rutina).append("</td><td>250 horas</td><td>").append(mantenimientoActual.getFecha()).append("</td><td>").append(mantenimientoActual.getHorometro()).append("</td><td>").append(proxHorometro).append("</td></tr>");
        }
        html.append("</table>");


        // ================= PÁGINA 4: FOTOS =================
        if (mantenimientoActual.getFotos() != null && !mantenimientoActual.getFotos().isEmpty()) {
            html.append("<div class='page-break'></div>");
            html.append(getEncabezadoHtml());

            html.append("<div style='text-align:center; font-weight:bold; font-size:14px; margin-bottom: 20px;'>");
            html.append("ANEXO FOTOGRÁFICO DEL MANTENIMIENTO");
            html.append("</div>");

            html.append("<table style='border:none; width:100%; text-align:center;'><tr>");
            int contador = 0;
            for (String fotoUrl : mantenimientoActual.getFotos()) {
                if (contador == 2) html.append("</tr><tr>");
                html.append("<td style='border:none; padding:10px; width:50%;'>");
                html.append("<img src='").append(fotoUrl).append("' style='max-width:100%; max-height:300px; border: 1px solid #ccc; border-radius: 8px;'/>");
                html.append("</td>");
                contador++;
                if (contador >= 4) break;
            }
            html.append("</tr></table>");
        }

        html.append("</body></html>");
        return html.toString();
    }
}