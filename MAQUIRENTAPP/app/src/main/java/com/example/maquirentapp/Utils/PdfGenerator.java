package com.example.maquirentapp.Utils;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
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
import com.example.maquirentapp.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

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
}