package com.example.maquirentapp.Utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;

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

    private static final int PAGE_WIDTH = 595; // A4
    private static final int PAGE_HEIGHT = 842;
    private static final int MARGIN = 40;

    public static void generarPdf(Context context, File archivoSalida, Mantenimiento mantenimientoActual, String codigoGrupo,
                                  String estado, String ubicacion, String defServicio, String cliente, String lugar,
                                  String aceite, String cantAceite, String contacto, String fallas, String trabajos,
                                  String nombreTecnico, String urlFirmaTecnico, String nombreSupervisor, String urlFirmaSupervisor,
                                  Map<String, String> codigosFiltros) throws Exception {

        PdfDocument document = new PdfDocument();

        // PLUMAS (Paints) PARA DIBUJAR
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
        paintLogoMaqui.setTextSize(18f);
        paintLogoMaqui.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        Paint paintLogoRent = new Paint();
        paintLogoRent.setColor(Color.parseColor("#80b6e0"));
        paintLogoRent.setTextSize(18f);
        paintLogoRent.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        Paint paintLineas = new Paint();
        paintLineas.setColor(Color.BLACK);
        paintLineas.setStrokeWidth(1f);
        paintLineas.setStyle(Paint.Style.STROKE);

        Paint paintFondoGris = new Paint();
        paintFondoGris.setColor(Color.LTGRAY);
        paintFondoGris.setStyle(Paint.Style.FILL);

        TextPaint textPaintLargo = new TextPaint();
        textPaintLargo.setColor(Color.BLACK);
        textPaintLargo.setTextSize(10f);

        String fechaHoy = new SimpleDateFormat("dd 'de' MMMM 'del' yyyy", new Locale("es", "ES")).format(new Date());
        String nombreClienteFinal = cliente.isEmpty() ? (mantenimientoActual.getEmpresa() != null ? mantenimientoActual.getEmpresa() : "") : cliente;

        // ================= PÁGINA 1: CARTA =================
        PdfDocument.Page page1 = document.startPage(new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create());
        Canvas canvas = page1.getCanvas();
        int y = dibujarEncabezado(canvas, paintLogoMaqui, paintLogoRent, paintTexto, paintNegrita, paintLineas);

        y += 40;
        canvas.drawText("INFORME MANTENIMIENTO GRUPO ELECTRÓGENO", PAGE_WIDTH / 2f, y, paintTitulo);
        canvas.drawLine(150, y + 2, 445, y + 2, paintLineas); // Subrayado

        y += 40;
        canvas.drawText("Cajamarca, " + fechaHoy + ".", MARGIN, y, paintNegrita);
        y += 20;
        canvas.drawText("Cliente: ", MARGIN, y, paintNegrita);
        canvas.drawText(nombreClienteFinal, MARGIN + 45, y, paintTexto);

        y += 30;
        String parrafoCarta = "El presente documento certifica que se ha realizado el mantenimiento preventivo y limpieza del grupo electrógeno, con el fin de evitar paralizaciones no programadas; se garantiza el correcto funcionamiento para su operación del generador de potencia " + codigoGrupo + " hasta su próximo mantenimiento preventivo. A continuación, se presentan los documentos que indican los cambios de elementos que se ha realizado en campo de: filtros de aceite, de aire, separador, entre otros, además de una inspección visual general al equipo.";
        StaticLayout slCarta = new StaticLayout(parrafoCarta, textPaintLargo, PAGE_WIDTH - (MARGIN * 2), Layout.Alignment.ALIGN_NORMAL, 1.2f, 0.0f, false);
        canvas.save();
        canvas.translate(MARGIN, y);
        slCarta.draw(canvas);
        canvas.restore();

        y += slCarta.getHeight() + 30;
        canvas.drawText("El grupo electrógeno cuenta con:", MARGIN, y, paintNegrita);
        y += 20;
        canvas.drawText("Bandeja antiderrame: [ X ]", MARGIN, y, paintTexto);
        canvas.drawText("Extintor PQS: [ X ]", MARGIN + 250, y, paintTexto);
        y += 20;
        canvas.drawText("Kit antiderrame: [ X ]", MARGIN, y, paintTexto);
        canvas.drawText("Puesta a tierra: [ X ]", MARGIN + 250, y, paintTexto);

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

        // Tabla Cabecera
        y += 20;
        dibujarCeldaTabla(canvas, MARGIN, y, 100, 20, "CÓDIGO DE EQUIPO", paintNegrita, paintLineas);
        dibujarCeldaTabla(canvas, MARGIN + 100, y, 200, 20, codigoGrupo, paintTexto, paintLineas);
        dibujarCeldaTabla(canvas, MARGIN + 300, y, 60, 20, "FECHA", paintNegrita, paintLineas);
        dibujarCeldaTabla(canvas, MARGIN + 360, y, PAGE_WIDTH - MARGIN - (MARGIN + 360), 20, mantenimientoActual.getFecha(), paintTexto, paintLineas);
        y += 20;
        dibujarCeldaTabla(canvas, MARGIN, y, 100, 20, "CLIENTE", paintNegrita, paintLineas);
        dibujarCeldaTabla(canvas, MARGIN + 100, y, PAGE_WIDTH - MARGIN - (MARGIN + 100), 20, nombreClienteFinal, paintTexto, paintLineas);
        y += 20;
        dibujarCeldaTabla(canvas, MARGIN, y, 100, 20, "LUGAR", paintNegrita, paintLineas);
        dibujarCeldaTabla(canvas, MARGIN + 100, y, PAGE_WIDTH - MARGIN - (MARGIN + 100), 20, lugar, paintTexto, paintLineas);

        // Tabla Horómetro
        y += 30;
        String proxHorometro = "N/A";
        try {
            proxHorometro = String.valueOf(Double.parseDouble(mantenimientoActual.getHorometro()) + 250);
        } catch (Exception e) {
        }
        canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + 20, paintFondoGris);
        dibujarCeldaTabla(canvas, MARGIN, y, 170, 20, "EQUIPO", paintNegrita, paintLineas);
        dibujarCeldaTabla(canvas, MARGIN + 170, y, 170, 20, "HORÓMETRO ACTUAL", paintNegrita, paintLineas);
        dibujarCeldaTabla(canvas, MARGIN + 340, y, PAGE_WIDTH - MARGIN - (MARGIN + 340), 20, "PRÓXIMO SERVICIO", paintNegrita, paintLineas);
        y += 20;
        dibujarCeldaTabla(canvas, MARGIN, y, 170, 20, codigoGrupo, paintTexto, paintLineas);
        dibujarCeldaTabla(canvas, MARGIN + 170, y, 170, 20, mantenimientoActual.getHorometro() + " hrs", paintTexto, paintLineas);
        dibujarCeldaTabla(canvas, MARGIN + 340, y, PAGE_WIDTH - MARGIN - (MARGIN + 340), 20, proxHorometro + " hrs", paintTexto, paintLineas);

        // Filtros
        y += 30;
        canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + 20, paintFondoGris);
        dibujarCeldaTabla(canvas, MARGIN, y, PAGE_WIDTH - (MARGIN * 2), 20, "SERVICIO PREVENTIVO Y CAMBIO DE FILTROS", paintNegrita, paintLineas);
        y += 20;
        dibujarCeldaTabla(canvas, MARGIN, y, PAGE_WIDTH - (MARGIN * 2), 20, "Cambio de aceite de motor (Marca y cantidad): " + aceite + " (" + cantAceite + ")", paintTexto, paintLineas);
        y += 20;

        String cAceite = codigosFiltros.containsKey("Filtro de aceite") ? "[ X ]" : "[   ]";
        String cAire = codigosFiltros.containsKey("Filtro de aire") ? "[ X ]" : "[   ]";
        String cComb = codigosFiltros.containsKey("Filtro de combustible") ? "[ X ]" : "[   ]";
        String cSep = codigosFiltros.containsKey("Filtro separador de agua") ? "[ X ]" : "[   ]";

        dibujarCeldaTabla(canvas, MARGIN, y, 257, 20, cAceite + " De aceite: " + codigosFiltros.getOrDefault("Filtro de aceite", "----"), paintTexto, paintLineas);
        dibujarCeldaTabla(canvas, MARGIN + 257, y, 258, 20, cAire + " De aire: " + codigosFiltros.getOrDefault("Filtro de aire", "----"), paintTexto, paintLineas);
        y += 20;
        dibujarCeldaTabla(canvas, MARGIN, y, 257, 20, cComb + " De combustible: " + codigosFiltros.getOrDefault("Filtro de combustible", "----"), paintTexto, paintLineas);
        dibujarCeldaTabla(canvas, MARGIN + 257, y, 258, 20, cSep + " Separador: " + codigosFiltros.getOrDefault("Filtro separador de agua", "----"), paintTexto, paintLineas);

        // Detalles
        y += 30;
        canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + 20, paintFondoGris);
        dibujarCeldaTabla(canvas, MARGIN, y, PAGE_WIDTH - (MARGIN * 2), 20, "OBSERVACIONES Y DETALLES DEL TRABAJO", paintNegrita, paintLineas);
        y += 20;
        dibujarCeldaTabla(canvas, MARGIN, y, PAGE_WIDTH - (MARGIN * 2), 20, "Estado: " + estado + "  |  Ubicación: " + ubicacion, paintTexto, paintLineas);
        y += 20;

        // Textos largos con StaticLayout
        y = dibujarCeldaMultilinea(canvas, "Problemas/Fallas: " + fallas, MARGIN, y, PAGE_WIDTH - (MARGIN * 2), textPaintLargo, paintLineas);
        y = dibujarCeldaMultilinea(canvas, "Trabajos realizados: " + trabajos, MARGIN, y, PAGE_WIDTH - (MARGIN * 2), textPaintLargo, paintLineas);
        dibujarCeldaTabla(canvas, MARGIN, y, PAGE_WIDTH - (MARGIN * 2), 20, "Contacto (Sr./Ing.): " + contacto, paintTexto, paintLineas);

        // Radios
        y += 40;
        canvas.drawText("Definición del Servicio:", MARGIN, y, paintNegrita);
        y += 20;
        canvas.drawText((defServicio.equals("Mantenimiento") ? "[X] " : "[ ] ") + "Mantenimiento", MARGIN, y, paintTexto);
        canvas.drawText((defServicio.equals("Evaluación") ? "[X] " : "[ ] ") + "Evaluación", MARGIN + 120, y, paintTexto);
        canvas.drawText((defServicio.equals("Entrega") ? "[X] " : "[ ] ") + "Entrega", MARGIN + 240, y, paintTexto);
        canvas.drawText((defServicio.equals("Realizar Ajuste") ? "[X] " : "[ ] ") + "Realizar Ajuste", MARGIN + 360, y, paintTexto);

        // Firmas (Descarga síncrona, por eso estamos en un hilo secundario)
        y += 60;
        if (urlFirmaTecnico != null && !urlFirmaTecnico.isEmpty()) {
            Bitmap bmpTec = descargarImagen(urlFirmaTecnico);
            if (bmpTec != null) {
                Rect rectDestino = new Rect(MARGIN + 40, y, MARGIN + 40 + 120, y + 60);
                canvas.drawBitmap(bmpTec, null, rectDestino, null);
            }
        }
        if (urlFirmaSupervisor != null && !urlFirmaSupervisor.isEmpty()) {
            Bitmap bmpSup = descargarImagen(urlFirmaSupervisor);
            if (bmpSup != null) {
                Rect rectDestino = new Rect(MARGIN + 320, y, MARGIN + 320 + 120, y + 60);
                canvas.drawBitmap(bmpSup, null, rectDestino, null);
            }
        }

        y += 60;
        canvas.drawText("_________________________", MARGIN + 20, y, paintTexto);
        canvas.drawText("_________________________", MARGIN + 300, y, paintTexto);
        y += 15;
        canvas.drawText(nombreTecnico, MARGIN + 40, y, paintNegrita);
        canvas.drawText(nombreSupervisor.equals("Ninguno") ? "" : nombreSupervisor, MARGIN + 320, y, paintNegrita);
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

        y += 30;
        canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + 30, paintFondoGris);
        dibujarCeldaTabla(canvas, MARGIN, y, 160, 30, "ACTIVIDAD", paintNegrita, paintLineas);
        dibujarCeldaTabla(canvas, MARGIN + 160, y, 80, 30, "FRECUENCIA", paintNegrita, paintLineas);
        dibujarCeldaTabla(canvas, MARGIN + 240, y, 90, 30, "FECHA INTERV.", paintNegrita, paintLineas);
        dibujarCeldaTabla(canvas, MARGIN + 330, y, 95, 30, "HORÓMETRO", paintNegrita, paintLineas);
        dibujarCeldaTabla(canvas, MARGIN + 425, y, 90, 30, "PRÓX. HORÓ.", paintNegrita, paintLineas);
        y += 30;

        String[] rutinas = {"Filtros de aire", "Limpieza exterior del radiador", "Cambio de aceite de motor", "Cambio de filtros de petróleo/aceite"};
        for (String r : rutinas) {
            dibujarCeldaTabla(canvas, MARGIN, y, 160, 20, r, paintTexto, paintLineas);
            dibujarCeldaTabla(canvas, MARGIN + 160, y, 80, 20, "250 horas", paintTexto, paintLineas);
            dibujarCeldaTabla(canvas, MARGIN + 240, y, 90, 20, mantenimientoActual.getFecha(), paintTexto, paintLineas);
            dibujarCeldaTabla(canvas, MARGIN + 330, y, 95, 20, mantenimientoActual.getHorometro(), paintTexto, paintLineas);
            dibujarCeldaTabla(canvas, MARGIN + 425, y, 90, 20, proxHorometro, paintTexto, paintLineas);
            y += 20;
        }
        document.finishPage(page3);

        // ================= PÁGINA 4: FOTOS =================
        if (mantenimientoActual.getFotos() != null && !mantenimientoActual.getFotos().isEmpty()) {
            PdfDocument.Page page4 = document.startPage(new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 4).create());
            canvas = page4.getCanvas();
            y = dibujarEncabezado(canvas, paintLogoMaqui, paintLogoRent, paintTexto, paintNegrita, paintLineas);

            y += 30;
            canvas.drawText("ANEXO FOTOGRÁFICO", PAGE_WIDTH / 2f, y, paintTitulo);
            y += 30;

            int imgWidth = 220;
            int imgHeight = 220;
            int startX = MARGIN + 20;
            int currentX = startX;
            int currentY = y;

            for (int i = 0; i < mantenimientoActual.getFotos().size(); i++) {
                if (i == 2) { // Salto de fila
                    currentX = startX;
                    currentY += imgHeight + 20;
                }
                Bitmap bmp = descargarImagen(mantenimientoActual.getFotos().get(i));
                if (bmp != null) {
                    Rect rectFoto = new Rect(currentX, currentY, currentX + imgWidth, currentY + imgHeight);
                    canvas.drawBitmap(bmp, null, rectFoto, null);

                    canvas.drawRect(currentX, currentY, currentX + imgWidth, currentY + imgHeight, paintLineas); // Borde
                }
                currentX += imgWidth + 30;
            }
            document.finishPage(page4);
        }

        // GUARDAR
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
        canvas.drawLine(MARGIN, y + 2, PAGE_WIDTH - MARGIN, y + 2, lineas); // Línea doble
        return y;
    }

    private static void dibujarCeldaTabla(Canvas canvas, int x, int y, int width, int height, String text, Paint paintText, Paint paintLines) {
        canvas.drawRect(x, y, x + width, y + height, paintLines);
        // Centrar verticalmente el texto en la celda
        canvas.drawText(text != null ? text : "", x + 5, y + (height / 2f) + 4, paintText);
    }

    private static int dibujarCeldaMultilinea(Canvas canvas, String texto, int x, int y, int width, TextPaint textPaint, Paint paintLines) {
        StaticLayout sl = new StaticLayout(texto, textPaint, width - 10, Layout.Alignment.ALIGN_NORMAL, 1.2f, 0.0f, false);
        int height = sl.getHeight() + 10;
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