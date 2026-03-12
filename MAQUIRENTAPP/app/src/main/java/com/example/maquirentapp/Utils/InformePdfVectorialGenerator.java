package com.example.maquirentapp.Utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.graphics.text.LineBreaker;
import android.os.Build;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;

import com.example.maquirentapp.Model.InfoPlaca;
import com.example.maquirentapp.Model.Mantenimiento;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

public class InformePdfVectorialGenerator {

    private static final int PAGE_WIDTH = 595;
    private static final int PAGE_HEIGHT = 842;
    private static final int MARGIN = 40;

    public static void generarPdf(Context context, File archivoSalida, Mantenimiento mantenimientoActual, InfoPlaca placa, String codigoGrupo, Map<String, Object> datos) throws Exception {

        PdfDocument document = new PdfDocument();

        // PLUMAS
        Paint paintTexto = new Paint();
        paintTexto.setColor(Color.BLACK);
        paintTexto.setTextSize(10f);

        Paint paintNegrita = new Paint();
        paintNegrita.setColor(Color.BLACK);
        paintNegrita.setTextSize(10f);
        paintNegrita.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        Paint paintTitulo = new Paint();
        paintTitulo.setColor(Color.BLACK);
        paintTitulo.setTextSize(14f);
        paintTitulo.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        paintTitulo.setTextAlign(Paint.Align.CENTER);

        Paint paintLogoMaqui = new Paint();
        paintLogoMaqui.setColor(Color.parseColor("#ff8c8c"));
        paintLogoMaqui.setTextSize(22f);
        paintLogoMaqui.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        Paint paintLogoRent = new Paint();
        paintLogoRent.setColor(Color.parseColor("#80b6e0"));
        paintLogoRent.setTextSize(22f);
        paintLogoRent.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        Paint paintLineas = new Paint();
        paintLineas.setColor(Color.BLACK);
        paintLineas.setStrokeWidth(1f);
        paintLineas.setStyle(Paint.Style.STROKE);

        Paint paintFondoGris = new Paint();
        paintFondoGris.setColor(Color.parseColor("#E0E0E0"));
        paintFondoGris.setStyle(Paint.Style.FILL);

        TextPaint textPaintLargo = new TextPaint();
        textPaintLargo.setColor(Color.BLACK);
        textPaintLargo.setTextSize(10f);

        // EXTRACCIÓN DE DATOS
        String fechaHoy = new SimpleDateFormat("dd 'de' MMMM 'del' yyyy", new Locale("es", "ES")).format(new Date());
        String cliente = (String) datos.getOrDefault("cliente", "");
        if (cliente.isEmpty()) cliente = mantenimientoActual.getEmpresa() != null ? mantenimientoActual.getEmpresa() : "";

        String proxHorometro = "N/A";
        try { proxHorometro = String.valueOf(Double.parseDouble(mantenimientoActual.getHorometro()) + 250); } catch (Exception ignored){}

        String marca = placa != null && placa.getMarcaGrupo() != null ? placa.getMarcaGrupo() : "N/A";
        String modelo = placa != null && placa.getModeloGrupo() != null ? placa.getModeloGrupo() : "N/A";
        String serie = placa != null && placa.getSerieGrupo() != null ? placa.getSerieGrupo() : "N/A";

        // ================= PÁGINA 1: CARTA =================
        PdfDocument.Page page1 = document.startPage(new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create());
        Canvas canvas = page1.getCanvas();
        int y = dibujarEncabezado(canvas, paintLogoMaqui, paintLogoRent, paintTexto, paintNegrita, paintLineas);

        y += 40;
        canvas.drawText("INFORME DE MANTENIMIENTO DE EQUIPO", PAGE_WIDTH / 2f, y, paintTitulo);
        // (Línea de subrayado eliminada a petición)

        y += 40;
        canvas.drawText("Cajamarca, " + fechaHoy + ".", MARGIN, y, paintNegrita);
        y += 20;
        canvas.drawText("Cliente: ", MARGIN, y, paintNegrita);
        canvas.drawText(cliente, MARGIN + 45, y, paintTexto);

        y += 30;
        String parrafoCarta = "El presente documento certifica que se ha realizado el mantenimiento preventivo y limpieza del equipo, con el fin de evitar paralizaciones no programadas; se garantiza el correcto funcionamiento para su operación del equipo " + codigoGrupo + " hasta su próximo mantenimiento preventivo. A continuación, se presentan los documentos que indican los cambios de elementos que se ha realizado en campo de: filtros de aceite, de aire, separador, entre otros, además de una inspección visual general al equipo.";

        // INTERLINEADO MAYOR Y JUSTIFICADO
        StaticLayout slCarta;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            slCarta = StaticLayout.Builder.obtain(parrafoCarta, 0, parrafoCarta.length(), textPaintLargo, PAGE_WIDTH - (MARGIN * 2))
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .setLineSpacing(0.0f, 1.8f) // 1.8 = Mayor interlineado
                    .setJustificationMode(LineBreaker.JUSTIFICATION_MODE_INTER_WORD)
                    .build();
        } else {
            slCarta = new StaticLayout(parrafoCarta, textPaintLargo, PAGE_WIDTH - (MARGIN * 2), Layout.Alignment.ALIGN_NORMAL, 1.8f, 0.0f, false);
        }

        canvas.save();
        canvas.translate(MARGIN, y);
        slCarta.draw(canvas);
        canvas.restore();

        y += slCarta.getHeight() + 40;
        canvas.drawText("El equipo cuenta con:", MARGIN, y, paintNegrita);
        y += 20;

        boolean bBandeja = (Boolean) datos.getOrDefault("chkBandeja", true);
        boolean bExtintor = (Boolean) datos.getOrDefault("chkExtintor", true);
        boolean bKit = (Boolean) datos.getOrDefault("chkKit", true);
        boolean bTierra = (Boolean) datos.getOrDefault("chkTierra", true);

        canvas.drawText("Bandeja antiderrame: [" + (bBandeja ? "X" : " ") + "]", MARGIN, y, paintTexto);
        canvas.drawText("Extintor PQS: [" + (bExtintor ? "X" : " ") + "]", MARGIN + 250, y, paintTexto);
        y += 20;
        canvas.drawText("Kit antiderrame: [" + (bKit ? "X" : " ") + "]", MARGIN, y, paintTexto);
        canvas.drawText("Puesta a tierra: [" + (bTierra ? "X" : " ") + "]", MARGIN + 250, y, paintTexto);

        y += 60;
        canvas.drawText("Atentamente:", MARGIN, y, paintTexto);
        y += 60;
        canvas.drawText("_______________________________", MARGIN, y, paintTexto);
        y += 15;
        canvas.drawText("Hugo Esquivel Pando", MARGIN, y, paintNegrita);
        y += 15;
        canvas.drawText("GERENTE", MARGIN, y, paintTexto);
        y += 15;
        canvas.drawText("MAQUIRENT SERVICIOS GENERALES", MARGIN, y, paintTexto);

        document.finishPage(page1);

        // ================= PÁGINA 2: REPORTE TÉCNICO =================
        PdfDocument.Page page2 = document.startPage(new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 2).create());
        canvas = page2.getCanvas();
        y = dibujarEncabezado(canvas, paintLogoMaqui, paintLogoRent, paintTexto, paintNegrita, paintLineas);

        y += 30;
        canvas.drawText("INFORME DE SERVICIO EN TALLER Y CAMPO", PAGE_WIDTH / 2f, y, paintTitulo);

        y += 20;
        dibujarCeldaTabla(canvas, MARGIN, y, 100, 20, "CÓDIGO DE EQUIPO:", paintNegrita, paintLineas);
        dibujarCeldaTabla(canvas, MARGIN + 100, y, 200, 20, codigoGrupo, paintTexto, paintLineas);
        dibujarCeldaTabla(canvas, MARGIN + 300, y, 60, 20, "FECHA:", paintNegrita, paintLineas);
        dibujarCeldaTabla(canvas, MARGIN + 360, y, PAGE_WIDTH - MARGIN - (MARGIN + 360), 20, mantenimientoActual.getFecha(), paintTexto, paintLineas);
        y += 20;
        dibujarCeldaTabla(canvas, MARGIN, y, 100, 20, "CLIENTE:", paintNegrita, paintLineas);
        dibujarCeldaTabla(canvas, MARGIN + 100, y, PAGE_WIDTH - MARGIN - (MARGIN + 100), 20, cliente, paintTexto, paintLineas);
        y += 20;
        dibujarCeldaTabla(canvas, MARGIN, y, 100, 20, "LUGAR:", paintNegrita, paintLineas);
        dibujarCeldaTabla(canvas, MARGIN + 100, y, PAGE_WIDTH - MARGIN - (MARGIN + 100), 20, (String)datos.get("lugar"), paintTexto, paintLineas);

        // Nueva Tabla 5 columnas: Equipo, Modelo, Serie, Horómetro, Próx Servicio (Suma 515)
        y += 30;
        canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + 20, paintFondoGris);
        dibujarCeldaTabla(canvas, MARGIN, y, 90, 20, "EQUIPO", paintNegrita, paintLineas);
        dibujarCeldaTabla(canvas, MARGIN+90, y, 100, 20, "MODELO", paintNegrita, paintLineas);
        dibujarCeldaTabla(canvas, MARGIN+190, y, 110, 20, "N° SERIE", paintNegrita, paintLineas);
        dibujarCeldaTabla(canvas, MARGIN+300, y, 100, 20, "HORÓMETRO", paintNegrita, paintLineas);
        dibujarCeldaTabla(canvas, MARGIN+400, y, 115, 20, "PRÓX. SERVICIO", paintNegrita, paintLineas);
        y += 20;
        dibujarCeldaTabla(canvas, MARGIN, y, 90, 20, codigoGrupo, paintTexto, paintLineas);
        dibujarCeldaTabla(canvas, MARGIN+90, y, 100, 20, modelo, paintTexto, paintLineas);
        dibujarCeldaTabla(canvas, MARGIN+190, y, 110, 20, serie, paintTexto, paintLineas);
        dibujarCeldaTabla(canvas, MARGIN+300, y, 100, 20, mantenimientoActual.getHorometro(), paintTexto, paintLineas);
        dibujarCeldaTabla(canvas, MARGIN+400, y, 115, 20, proxHorometro, paintTexto, paintLineas);

        // Filtros
        y += 30;
        canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + 20, paintFondoGris);
        dibujarCeldaTabla(canvas, MARGIN, y, PAGE_WIDTH - (MARGIN*2), 20, "SERVICIO PREVENTIVO Y CAMBIO DE FILTROS", paintNegrita, paintLineas);
        y += 20;
        dibujarCeldaTabla(canvas, MARGIN, y, PAGE_WIDTH - (MARGIN*2), 20, "Cambio de aceite de motor: " + datos.get("aceite") + " (" + datos.get("cantAceite") + ")", paintTexto, paintLineas);
        y += 20;

        Map<String, String> filtros = (Map<String, String>) datos.get("codigosFiltros");
        String cAceite = filtros.containsKey("Filtro de aceite") ? "[ X ]" : "[   ]";
        String cAire = filtros.containsKey("Filtro de aire") ? "[ X ]" : "[   ]";
        String cComb = filtros.containsKey("Filtro de combustible") ? "[ X ]" : "[   ]";
        String cSep = filtros.containsKey("Filtro separador de agua") ? "[ X ]" : "[   ]";

        dibujarCeldaTabla(canvas, MARGIN, y, 257, 20, cAceite + " De aceite: " + filtros.getOrDefault("Filtro de aceite", "----"), paintTexto, paintLineas);
        dibujarCeldaTabla(canvas, MARGIN + 257, y, 258, 20, cAire + " De aire: " + filtros.getOrDefault("Filtro de aire", "----"), paintTexto, paintLineas);
        y += 20;
        dibujarCeldaTabla(canvas, MARGIN, y, 257, 20, cComb + " De combustible: " + filtros.getOrDefault("Filtro de combustible", "----"), paintTexto, paintLineas);
        dibujarCeldaTabla(canvas, MARGIN + 257, y, 258, 20, cSep + " Separador: " + filtros.getOrDefault("Filtro separador de agua", "----"), paintTexto, paintLineas);

        // Detalles
        y += 30;
        canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + 20, paintFondoGris);
        dibujarCeldaTabla(canvas, MARGIN, y, PAGE_WIDTH - (MARGIN*2), 20, "OBSERVACIONES Y DETALLES DEL TRABAJO", paintNegrita, paintLineas);
        y += 20;
        dibujarCeldaTabla(canvas, MARGIN, y, PAGE_WIDTH - (MARGIN*2), 20, "Estado: " + datos.get("estado") + "  |  Ubicación: " + datos.get("ubicacion"), paintTexto, paintLineas);
        y += 20;

        y = dibujarCeldaMultilinea(canvas, "Problemas/Fallas: " + datos.get("fallas"), MARGIN, y, PAGE_WIDTH - (MARGIN*2), textPaintLargo, paintLineas);
        y = dibujarCeldaMultilinea(canvas, "Trabajos realizados: " + datos.get("trabajos"), MARGIN, y, PAGE_WIDTH - (MARGIN*2), textPaintLargo, paintLineas);
        dibujarCeldaTabla(canvas, MARGIN, y, PAGE_WIDTH - (MARGIN*2), 20, "Contacto (Sr./Ing.): " + datos.get("contacto"), paintTexto, paintLineas);

        // Radios
        y += 40;
        String def = (String) datos.get("defServicio");
        canvas.drawText("Definición del Servicio:", MARGIN, y, paintNegrita);
        y += 20;
        canvas.drawText((def.equals("Mantenimiento") ? "[X] " : "[ ] ") + "Mantenimiento", MARGIN, y, paintTexto);
        canvas.drawText((def.equals("Evaluación") ? "[X] " : "[ ] ") + "Evaluación", MARGIN + 120, y, paintTexto);
        canvas.drawText((def.equals("Entrega") ? "[X] " : "[ ] ") + "Entrega", MARGIN + 240, y, paintTexto);
        canvas.drawText((def.equals("Realizar Ajuste") ? "[X] " : "[ ] ") + "Realizar Ajuste", MARGIN + 360, y, paintTexto);

        // Firmas HD (Sin ScaledBitmap que destruye la calidad)
        y += 40;
        String urlTec = (String) datos.get("urlFirmaTecnico");
        String urlSup = (String) datos.get("urlFirmaSupervisor");
        if (urlTec != null && !urlTec.isEmpty()) {
            Bitmap bmpTec = descargarImagen(urlTec);
            if (bmpTec != null) canvas.drawBitmap(bmpTec, null, new android.graphics.Rect(MARGIN + 40, y, MARGIN + 40 + 120, y + 60), null);
        }
        if (urlSup != null && !urlSup.isEmpty()) {
            Bitmap bmpSup = descargarImagen(urlSup);
            if (bmpSup != null) canvas.drawBitmap(bmpSup, null, new android.graphics.Rect(MARGIN + 320, y, MARGIN + 320 + 120, y + 60), null);
        }

        y += 60;
        canvas.drawText("_________________________", MARGIN + 20, y, paintTexto);
        canvas.drawText("_________________________", MARGIN + 300, y, paintTexto);
        y += 15;
        canvas.drawText((String) datos.get("tecnico"), MARGIN + 40, y, paintNegrita);
        String sup = (String) datos.get("supervisor");
        canvas.drawText(sup.equals("Ninguno") ? "" : sup, MARGIN + 320, y, paintNegrita);
        y += 15;
        canvas.drawText("Mecánico / Encargado", MARGIN + 40, y, paintTexto);
        canvas.drawText("Supervisor", MARGIN + 320, y, paintTexto);

        document.finishPage(page2);

        // ================= PÁGINA 3: PLANIFICACIÓN =================
        PdfDocument.Page page3 = document.startPage(new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 3).create());
        canvas = page3.getCanvas();
        y = dibujarEncabezado(canvas, paintLogoMaqui, paintLogoRent, paintTexto, paintNegrita, paintLineas);

        y += 30;
        canvas.drawText("RUTINA DE MANTENIMIENTO PREVENTIVO PLANIFICADO", PAGE_WIDTH / 2f, y, paintTitulo);

        y += 20;
        canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + 40, paintFondoGris);
        dibujarCeldaTabla(canvas, MARGIN, y, 70, 20, "EQUIPO:", paintNegrita, paintLineas);
        dibujarCeldaTabla(canvas, MARGIN+70, y, 100, 20, codigoGrupo, paintTexto, paintLineas);
        dibujarCeldaTabla(canvas, MARGIN+170, y, 70, 20, "MARCA:", paintNegrita, paintLineas);
        dibujarCeldaTabla(canvas, MARGIN+240, y, 100, 20, marca, paintTexto, paintLineas);
        dibujarCeldaTabla(canvas, MARGIN+340, y, 70, 20, "MODELO:", paintNegrita, paintLineas);
        dibujarCeldaTabla(canvas, MARGIN+410, y, 105, 20, modelo, paintTexto, paintLineas);
        y += 20;
        dibujarCeldaTabla(canvas, MARGIN, y, 70, 20, "SERIE:", paintNegrita, paintLineas);
        dibujarCeldaTabla(canvas, MARGIN+70, y, 100, 20, serie, paintTexto, paintLineas);
        dibujarCeldaTabla(canvas, MARGIN+170, y, 70, 20, "CLIENTE:", paintNegrita, paintLineas);
        dibujarCeldaTabla(canvas, MARGIN+240, y, 275, 20, cliente, paintTexto, paintLineas);

        y += 30;
        // FONDO VERDE CLARO PARA LA TABLA DE RUTINAS
        int alturaCabecera = 40;
        Paint paintFondoVerde = new Paint();
        paintFondoVerde.setColor(Color.parseColor("#e2efda"));
        paintFondoVerde.setStyle(Paint.Style.FILL);

        TextPaint textPaintCabecera = new TextPaint();
        textPaintCabecera.setColor(Color.BLACK);
        textPaintCabecera.setTextSize(8f);
        textPaintCabecera.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + alturaCabecera, paintFondoGris);
        dibujarCeldaMultilineaCentroVertical(canvas, "ACTIVIDAD", MARGIN, y, 135, alturaCabecera, textPaintCabecera, paintLineas);
        dibujarCeldaMultilineaCentroVertical(canvas, "FRECUENCIA", MARGIN+135, y, 60, alturaCabecera, textPaintCabecera, paintLineas);
        dibujarCeldaMultilineaCentroVertical(canvas, "FECHA DE INTERVENCIÓN", MARGIN+195, y, 70, alturaCabecera, textPaintCabecera, paintLineas);
        dibujarCeldaMultilineaCentroVertical(canvas, "HORÓMETRO DE INTERVENCIÓN", MARGIN+265, y, 70, alturaCabecera, textPaintCabecera, paintLineas);
        dibujarCeldaMultilineaCentroVertical(canvas, "HORÓMETRO PRÓXIMA INTERVENCIÓN", MARGIN+335, y, 70, alturaCabecera, textPaintCabecera, paintLineas);
        dibujarCeldaMultilineaCentroVertical(canvas, "PRÓXIMA FECHA DE INTERVENCIÓN", MARGIN+405, y, 110, alturaCabecera, textPaintCabecera, paintLineas);
        y += alturaCabecera;

        String pFecha = (String) datos.getOrDefault("proxFecha", "No definida");

        // RECORRER LA LISTA DINÁMICA GUARDADA
        java.util.List<Map<String, Object>> listaRutinas = (java.util.List<Map<String, Object>>) datos.get("rutinasList");
        if (listaRutinas != null) {
            for(Map<String, Object> rMap : listaRutinas) {
                // SI ESTÁ DESHABILITADA, NOS LA SALTAMOS (No se imprime)
                Boolean activa = (Boolean) rMap.get("activa");
                if (activa != null && !activa) continue;

                String rNombre = (String) rMap.get("nombre");

                // CÁLCULO MÁGICO MULTILÍNEA: Medimos si el texto necesita más altura que 25px
                StaticLayout sl = new StaticLayout(rNombre, textPaintLargo, 135 - 10, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
                int alturaNecesaria = Math.max(25, sl.getHeight() + 10);

                // Dibujamos el fondo verde de la fila
                canvas.drawRect(MARGIN, y, MARGIN + 135, y + alturaNecesaria, paintFondoVerde);

                // Dibujamos la celda adaptativa para la actividad
                dibujarCeldaMultilineaCentroVertical(canvas, rNombre, MARGIN, y, 135, alturaNecesaria, textPaintLargo, paintLineas);

                // Las demás celdas usan el mismo alto dinámico para cuadrar perfecto
                dibujarCeldaTablaCentro(canvas, MARGIN+135, y, 60, alturaNecesaria, "250 hrs", paintTexto, paintLineas);
                dibujarCeldaTablaCentro(canvas, MARGIN+195, y, 70, alturaNecesaria, mantenimientoActual.getFecha(), paintTexto, paintLineas);
                dibujarCeldaTablaCentro(canvas, MARGIN+265, y, 70, alturaNecesaria, mantenimientoActual.getHorometro(), paintTexto, paintLineas);
                dibujarCeldaTablaCentro(canvas, MARGIN+335, y, 70, alturaNecesaria, proxHorometro, paintTexto, paintLineas);
                dibujarCeldaTablaCentro(canvas, MARGIN+405, y, 110, alturaNecesaria, pFecha, paintTexto, paintLineas);

                y += alturaNecesaria;
            }
        }

        y += 65;
        canvas.drawText("Representante de la empresa: Hugo Esquivel Pando", MARGIN, y, paintNegrita);
        y += 25;
        canvas.drawText("Fecha:" + fechaHoy, MARGIN, y, paintNegrita);

        document.finishPage(page3);

        // ================= PÁGINA 4: FOTOS HD =================
        if (mantenimientoActual.getFotos() != null && !mantenimientoActual.getFotos().isEmpty()) {
            PdfDocument.Page page4 = document.startPage(new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 4).create());
            canvas = page4.getCanvas();
            y = dibujarEncabezado(canvas, paintLogoMaqui, paintLogoRent, paintTexto, paintNegrita, paintLineas);

            y += 30;
            canvas.drawText("ANEXO FOTOGRÁFICO", PAGE_WIDTH / 2f, y, paintTitulo);
            y += 30;

            int imgWidth = 230;
            int imgHeight = 230;
            int startX = MARGIN + 15;
            int currentX = startX;
            int currentY = y;

            for (int i = 0; i < mantenimientoActual.getFotos().size(); i++) {
                if (i == 2) {
                    currentX = startX;
                    currentY += imgHeight + 20;
                }
                Bitmap bmp = descargarImagen(mantenimientoActual.getFotos().get(i));
                if (bmp != null) {
                    android.graphics.Rect rectFoto = new android.graphics.Rect(currentX, currentY, currentX + imgWidth, currentY + imgHeight);
                    canvas.drawBitmap(bmp, null, rectFoto, null);
                    canvas.drawRect(currentX, currentY, currentX + imgWidth, currentY + imgHeight, paintLineas);
                }
                currentX += imgWidth + 25;
            }
            document.finishPage(page4);
        }

        FileOutputStream fos = new FileOutputStream(archivoSalida);
        document.writeTo(fos);
        document.close();
        fos.close();
    }

    private static int dibujarEncabezado(Canvas canvas, Paint logoM, Paint logoR, Paint texto, Paint negrita, Paint lineas) {
        int y = 40;
        float currentX = MARGIN;
        canvas.drawText("MAQUI", currentX, y, logoM);
        currentX += logoM.measureText("MAQUI");
        canvas.drawText("RENT", currentX, y, logoR);
        currentX += logoR.measureText("RENT") + 10;
        canvas.drawText("SERVICIOS GENERALES", currentX, y, logoM);

        y += 20;
        canvas.drawText("HUGO ALBERTO ESQUIVEL PANDO", MARGIN, y, negrita);
        canvas.drawText("RUC: 10266739414", PAGE_WIDTH - MARGIN - 100, y, negrita);

        y += 15;
        canvas.drawText("SERVICIOS Y ALQUILER DE GRUPOS ELECTRÓGENOS Y EQUIPOS DE CONSTRUCCIÓN", MARGIN, y, texto);
        y += 15;
        canvas.drawText("AV. ATAHUALPA 810 - CAJAMARCA | TELÉF. 976959490", MARGIN, y, texto);

        y += 15;
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, lineas);
        canvas.drawLine(MARGIN, y + 2, PAGE_WIDTH - MARGIN, y + 2, lineas);
        return y;
    }
    // Pone el texto en el centro vertical exacto, útil para filas que se estiraron
    private static void dibujarCeldaTablaCentro(Canvas canvas, int x, int y, int width, int height, String text, Paint paintText, Paint paintLines) {
        canvas.drawRect(x, y, x + width, y + height, paintLines);
        String t = text != null ? text : "";
        int len = paintText.breakText(t, true, width - 10, null);
        // Cálculo para centrar verticalmente basado en FontMetrics
        Paint.FontMetrics fm = paintText.getFontMetrics();
        float textY = y + (height / 2f) - (fm.ascent + fm.descent) / 2f;
        canvas.drawText(t.substring(0, len), x + 5, textY, paintText);
    }

    // El truco multilínea: envuelve el texto si es muy largo y lo centra verticalmente
    private static void dibujarCeldaMultilineaCentroVertical(Canvas canvas, String texto, int x, int y, int width, int cellHeight, TextPaint textPaint, Paint paintLines) {
        canvas.drawRect(x, y, x + width, y + cellHeight, paintLines);
        StaticLayout sl = new StaticLayout(texto, textPaint, width - 10, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
        float textY = y + (cellHeight - sl.getHeight()) / 2f; // Centrado Y matemático
        canvas.save();
        canvas.translate(x + 5, textY);
        sl.draw(canvas);
        canvas.restore();
    }
    private static void dibujarCeldaTabla(Canvas canvas, int x, int y, int width, int height, String text, Paint paintText, Paint paintLines) {
        canvas.drawRect(x, y, x + width, y + height, paintLines);
        // Truncar texto si es muy largo (ej. cliente)
        String t = text != null ? text : "";
        int len = paintText.breakText(t, true, width - 10, null);
        canvas.drawText(t.substring(0, len), x + 5, y + (height / 2f) + 4, paintText);
    }

    private static int dibujarCeldaMultilinea(Canvas canvas, String texto, int x, int y, int width, TextPaint textPaint, Paint paintLines) {
        StaticLayout sl = new StaticLayout(texto, textPaint, width - 10, Layout.Alignment.ALIGN_NORMAL, 1.2f, 0.0f, false);
        int height = sl.getHeight() + 10;
        if(height < 20) height = 20;
        canvas.drawRect(x, y, x + width, y + height, paintLines);
        canvas.save();
        canvas.translate(x + 5, y + 5);
        sl.draw(canvas);
        canvas.restore();
        return y + height;
    }

    private static Bitmap descargarImagen(String strUrl) {
        try {
            URL url = new URL(strUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            return BitmapFactory.decodeStream(input);
        } catch (Exception e) {
            return null;
        }
    }
}