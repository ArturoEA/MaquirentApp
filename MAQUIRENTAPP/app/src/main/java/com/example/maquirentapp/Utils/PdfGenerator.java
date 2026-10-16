package com.example.maquirentapp.Utils;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.maquirentapp.Model.CertificadoOperatividad;
import com.example.maquirentapp.Model.ItemValorizacion;
import com.example.maquirentapp.Model.Valorizacion;
import com.example.maquirentapp.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Locale;

public class PdfGenerator {
    private static final int A4_WIDTH_PTS = 595;
    private static final int A4_HEIGHT_PTS = 842;
    private static final int RENDER_WIDTH_PX = 1190;
    private static final float TARGET_DENSITY = 2.0f;
    public interface OnPdfGeneratedListener {
        void onPdfGenerated(File pdfFile);
        void onError(String error);
    }
    public File generarCertificadoPdf(Context context, CertificadoOperatividad cert, String codigoGrupo) throws Exception {

        // 1. CREAR CONTEXTO CON DENSIDAD CONTROLADA
        Context pdfContext = createContextWithDensity(context, TARGET_DENSITY);

        // 2. Inflar usando este contexto especial
        LayoutInflater inflater = LayoutInflater.from(pdfContext);
        View pdfView = inflater.inflate(R.layout.layout_certificado_pdf, null);

        // 3. Rellenar datos
        rellenarDatos(pdfView, cert, codigoGrupo);

        // 4. Medir la vista con el ancho fijo de alta resolución
        pdfView.measure(
                View.MeasureSpec.makeMeasureSpec(RENDER_WIDTH_PX, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        );

        int viewHeightPx = pdfView.getMeasuredHeight();

        // 5. Maquetar (Layout)
        pdfView.layout(0, 0, RENDER_WIDTH_PX, viewHeightPx);

        // 6. Crear PDF
        PdfDocument document = new PdfDocument();

        float scale = (float) A4_WIDTH_PTS / RENDER_WIDTH_PX;

        // Calcular altura de página en píxeles
        float pageHeightInPixels = A4_HEIGHT_PTS / scale;
        int numPages = (int) Math.ceil(viewHeightPx / pageHeightInPixels);

        for (int i = 0; i < numPages; i++) {
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(A4_WIDTH_PTS, A4_HEIGHT_PTS, i + 1).create();
            PdfDocument.Page page = document.startPage(pageInfo);
            Canvas canvas = page.getCanvas();

            Paint paint = new Paint();
            paint.setColor(Color.WHITE);
            canvas.drawRect(0, 0, A4_WIDTH_PTS, A4_HEIGHT_PTS, paint);

            canvas.save();
            canvas.scale(scale, scale);
            canvas.translate(0, -i * pageHeightInPixels);

            pdfView.draw(canvas);

            canvas.restore();
            document.finishPage(page);
        }

        // 7. Guardar
        String nombreArchivo = "Certificado_" + (cert.getNumeroCertificado() != null ? cert.getNumeroCertificado() : "borrador") + ".pdf";

        File archivoGenerado = guardarPdfEnDispositivo(context, document, nombreArchivo);

        document.close();
        return archivoGenerado;
    }
    private File guardarPdfEnDispositivo(Context context, PdfDocument document, String nombreArchivo) throws IOException {
        OutputStream fos;
        File archivoSalida = null;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, nombreArchivo);
            values.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS + "/MaquirentCertificados");

            Uri uri = context.getContentResolver().insert(MediaStore.Files.getContentUri("external"), values);
            if (uri != null) {
                fos = context.getContentResolver().openOutputStream(uri);
                document.writeTo(fos);
                if (fos != null) fos.close();

                archivoSalida = new File(context.getCacheDir(), nombreArchivo);
                FileOutputStream fosCache = new FileOutputStream(archivoSalida);
                document.writeTo(fosCache);
                fosCache.close();
            } else {
                throw new IOException("No se pudo crear el archivo en MediaStore");
            }
        }
        else {
            File directory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "MaquirentCertificados");
            if (!directory.exists()) directory.mkdirs();
            archivoSalida = new File(directory, nombreArchivo);
            fos = new FileOutputStream(archivoSalida);
            document.writeTo(fos);
            fos.close();
        }

        return archivoSalida;
    }
    private Context createContextWithDensity(Context context, float densityScale) {
        Configuration configuration = new Configuration(context.getResources().getConfiguration());
        configuration.densityDpi = (int) (densityScale * 160);
        configuration.fontScale = 1.0f;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            return context.createConfigurationContext(configuration);
        } else {
            return context;
        }
    }

    private void rellenarDatos(View view, CertificadoOperatividad cert, String codigo) {
        setText(view, R.id.tvPdfCliente, cert.getCliente());
        setText(view, R.id.tvPdfFecha, cert.getFechaEmision());

        TextView tvParrafo = view.findViewById(R.id.tvPdfParrafo);
        if (tvParrafo != null) {
            String texto = tvParrafo.getText().toString();
            texto = texto.replace("{{POT_SB}}", cert.getPotencia() != null ? cert.getPotencia() : "");
            texto = texto.replace("{{COD_GRUPO}}", codigo != null ? codigo : "");
            tvParrafo.setText(texto);
        }

        setText(view, R.id.tvPdfMarcaGrupo, cert.getMarcaGrupo());
        setText(view, R.id.tvPdfModGrupo, cert.getModeloGrupo());
        setText(view, R.id.tvPdfSerieGrupo, cert.getSerieGrupo());

        setText(view, R.id.tvPdfMarcaMotor, cert.getMarcaMotor());
        setText(view, R.id.tvPdfModMotor, cert.getModeloMotor());
        setText(view, R.id.tvPdfSerieMotor, cert.getSerieMotor());

        setText(view, R.id.tvPdfMarcaGen, cert.getMarcaGenerador());
        setText(view, R.id.tvPdfModGen, cert.getModeloGenerador());
        setText(view, R.id.tvPdfSerieGen, cert.getSerieGenerador());
    }

    private void setText(View parent, int id, String text) {
        TextView tv = parent.findViewById(id);
        if (tv != null) {
            tv.setText(text != null ? text : "-");
        }
    }


    public void generarPdfDesdeHtml(Context context, String htmlContent, String nombreArchivo) {
        WebView webView = new WebView(context);

        webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                crearPdf(context, view, nombreArchivo);
            }
        });
    }
    private void crearPdf(Context context, WebView webView, String nombreArchivo) {
        PrintManager printManager = (PrintManager) context.getSystemService(Context.PRINT_SERVICE);

        PrintDocumentAdapter printAdapter = webView.createPrintDocumentAdapter(nombreArchivo);

        PrintAttributes.Builder builder = new PrintAttributes.Builder();
        builder.setMediaSize(PrintAttributes.MediaSize.ISO_A4);

        printManager.print(nombreArchivo, printAdapter, builder.build());
    }
    public void generarPdfDesdeWebView(WebView webView, String htmlContent, String nombreArchivo, OnPdfGeneratedListener listener) {

        // Configuramos el WebView para que sea "A4"
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setUseWideViewPort(true);
        // Habilitar el dibujo incluso si es invisible
        webView.setDrawingCacheEnabled(true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                // Damos un pequeño respiro (300ms) para asegurar que el renderizado visual termine
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    createPdfFromVisualView(view, nombreArchivo, view.getContext(), listener);
                }, 300);
            }
        });

        // Cargamos el HTML
        webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null);
    }
    private void createPdfFromVisualView(WebView webView, String nombreArchivo, Context context, OnPdfGeneratedListener listener) {
        PdfDocument document = new PdfDocument();

        // Dimensiones A4 estándar en puntos (PDF unit)
        int anchoPaginaPdf = 595;
        int altoPaginaPdf = 842;

        try {
            // 1. Obtener las medidas REALES del contenido del WebView (en píxeles)
            // El WebView ya se dibujó en la pantalla invisible, así que usamos su ancho real.
            int anchoContenidoWebView = webView.getWidth();
            int altoContenidoWebView = webView.getContentHeight(); // Ojo: getContentHeight() es más preciso para scroll vertical

            // Si por alguna razón es 0, usamos medidas por defecto para evitar crash
            if (anchoContenidoWebView <= 0) anchoContenidoWebView = 1000;
            if (altoContenidoWebView <= 0) altoContenidoWebView = 1500;

            // 2. Calcular el Factor de Escala (Zoom Out)
            // Queremos que el ancho del WebView quepa exactamente en el ancho del PDF
            float escala = (float) anchoPaginaPdf / (float) anchoContenidoWebView;

            // Calculamos la altura de una página PDF pero en "píxeles de WebView"
            // Es decir: ¿Cuántos píxeles de la web caben en una hoja A4?
            float altoPaginaEnPixelesWeb = altoPaginaPdf / escala;

            // Calculamos cuántas páginas necesitamos
            int totalPaginas = (int) Math.ceil(altoContenidoWebView / altoPaginaEnPixelesWeb);

            for (int i = 0; i < totalPaginas; i++) {
                PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(anchoPaginaPdf, altoPaginaPdf, i + 1).create();
                PdfDocument.Page page = document.startPage(pageInfo);
                Canvas canvas = page.getCanvas();

                // Pintamos fondo blanco
                Paint paint = new Paint();
                paint.setColor(Color.WHITE);
                canvas.drawRect(0, 0, anchoPaginaPdf, altoPaginaPdf, paint);

                // --- AQUÍ ESTÁ LA MAGIA DEL ZOOM ---
                canvas.save();

                // 1. Aplicamos la escala para que el contenido gigante se encoja al tamaño A4
                canvas.scale(escala, escala);

                // 2. Trasladamos para "escanear" la parte del WebView que toca en esta página
                // Movemos hacia arriba en píxeles originales del webview
                canvas.translate(0, -(i * altoPaginaEnPixelesWeb));

                // 3. Dibujamos
                webView.draw(canvas);

                canvas.restore();
                document.finishPage(page);
            }

            // Guardar
            File pdfDir = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "Cotizaciones");
            if (!pdfDir.exists()) pdfDir.mkdirs();
            File archivoFinal = new File(pdfDir, nombreArchivo + ".pdf");

            document.writeTo(new FileOutputStream(archivoFinal));
            document.close();

            listener.onPdfGenerated(archivoFinal);

        } catch (IOException e) {
            listener.onError(e.getMessage());
            document.close();
        }
    }

    /**
     * Fusiona dos archivos PDF existentes en uno solo utilizando exclusivamente
     * APIs nativas de Android (PdfRenderer + PdfDocument), sin librerías externas.
     */
    public static void unirPdfs(File pdf1, File pdf2, File destino) throws IOException {
        PdfDocument document = new PdfDocument();
        int pageIndex = 1;
        File[] sources = new File[]{pdf1, pdf2};

        for (File source : sources) {
            if (source == null || !source.exists() || source.length() == 0) continue;

            ParcelFileDescriptor pfd = ParcelFileDescriptor.open(source, ParcelFileDescriptor.MODE_READ_ONLY);
            PdfRenderer renderer = new PdfRenderer(pfd);

            for (int i = 0; i < renderer.getPageCount(); i++) {
                PdfRenderer.Page page = renderer.openPage(i);
                int width = page.getWidth();
                int height = page.getHeight();

                PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(width, height, pageIndex++).create();
                PdfDocument.Page docPage = document.startPage(pageInfo);

                Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                bitmap.eraseColor(Color.WHITE);
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);

                docPage.getCanvas().drawBitmap(bitmap, 0, 0, null);
                document.finishPage(docPage);

                bitmap.recycle();
                page.close();
            }
            renderer.close();
            pfd.close();
        }

        if (destino.getParentFile() != null && !destino.getParentFile().exists()) {
            destino.getParentFile().mkdirs();
        }

        try (FileOutputStream fos = new FileOutputStream(destino)) {
            document.writeTo(fos);
        } finally {
            document.close();
        }
    }

    /**
     * Genera un reporte de Valorización nativo en formato PDF (A4) usando PdfDocument y Canvas.
     * Reemplaza el uso de Apache POI (Excel) por un documento PDF profesional y ligero.
     */
    public File generarValorizacionPdf(Context context, Valorizacion val) throws IOException {
        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(A4_WIDTH_PTS, A4_HEIGHT_PTS, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        // Fondo blanco
        Paint bgPaint = new Paint();
        bgPaint.setColor(Color.WHITE);
        canvas.drawRect(0, 0, A4_WIDTH_PTS, A4_HEIGHT_PTS, bgPaint);

        // Paints para textos y bordes
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

        // 1. Cabecera de la empresa
        canvas.drawText("MAQUIRENT SERVICIOS GENERALES", xStart, y, titlePaint);
        y += 14;
        canvas.drawText("RUC: 10266739414 | Hugo Alberto Esquivel Pando", xStart, y, subtitlePaint);
        y += 12;
        canvas.drawText("Alquiler de Grupos Electrógenos y Maquinaria | Cajamarca - Perú", xStart, y, subtitlePaint);
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

        // 4. Tabla de Items - Cabecera
        int rowHeight = 22;
        canvas.drawRect(xStart, y, xEnd, y + rowHeight, headerBoxPaint);

        canvas.drawText("EQUIPO", xStart + 8, y + 15, headerTextPaint);
        canvas.drawText("PERÍODO", xStart + 170, y + 15, headerTextPaint);
        canvas.drawText("HORAS", xStart + 290, y + 15, headerTextPaint);
        canvas.drawText("TARIFA", xStart + 360, y + 15, headerTextPaint);
        canvas.drawText("TOTAL", xStart + 430, y + 15, headerTextPaint);
        y += rowHeight;

        // Filas de Items
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

        // 5. Cuadro de Totales (Alineado a la derecha)
        int totalBoxX = 350;
        canvas.drawText("SUBTOTAL:", totalBoxX, y, boldPaint);
        canvas.drawText(String.format(Locale.US, "%.2f", val.getSubtotal()), totalBoxX + 90, y, textPaint);
        y += 16;

        canvas.drawText("I.G.V. (18%):", totalBoxX, y, boldPaint);
        canvas.drawText(String.format(Locale.US, "%.2f", val.getIgv()), totalBoxX + 90, y, textPaint);
        y += 16;

        canvas.drawLine(totalBoxX, y - 4, xEnd, y - 4, linePaint);
        canvas.drawText("TOTAL:", totalBoxX, y + 8, titlePaint);
        canvas.drawText(String.format(Locale.US, "%.2f", val.getTotal()), totalBoxX + 90, y + 8, titlePaint);

        // 6. Firma
        y = A4_HEIGHT_PTS - 80;
        canvas.drawLine(xStart + 150, y, xEnd - 150, y, linePaint);
        y += 15;
        Paint centerPaint = new Paint(boldPaint);
        centerPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("MAQUIRENT SERVICIOS GENERALES", (float) A4_WIDTH_PTS / 2, y, centerPaint);

        document.finishPage(page);

        // Guardar archivo
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
}