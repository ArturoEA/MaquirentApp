package com.example.maquirentapp.Utils;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.os.Build;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.maquirentapp.Model.CertificadoOperatividad;
import com.example.maquirentapp.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class PdfGenerator {

    // A4 en puntos (72 dpi)
    private static final int A4_WIDTH_PTS = 595;
    private static final int A4_HEIGHT_PTS = 842;

    // Usamos una densidad fija de 2.0 (xhdpi) para simular alta calidad pero controlada.
    // Con densidad 2.0, 1dp = 2px.
    // Ancho objetivo en px = 595 * 2 = 1190px.
    private static final int RENDER_WIDTH_PX = 1190;
    private static final float TARGET_DENSITY = 2.0f;

    public File generarCertificadoPdf(Context context, CertificadoOperatividad cert, String codigoGrupo, String urlFoto) throws Exception {

        // 1. CREAR CONTEXTO CON DENSIDAD CONTROLADA (Truco clave)
        // Esto asegura que 12sp se vea igual en un Samsung S24 que en un pixel antiguo.
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

        // Calculamos la escala inversa: Si dibujamos en 1190px y el PDF mide 595pt,
        // necesitamos escalar al 50% (0.5).
        // Como forzamos la densidad a 2.0, los textos de 12sp se renderizaron a 24px.
        // 24px * 0.5 = 12pt. ¡Tamaño perfecto de lectura!
        float scale = (float) A4_WIDTH_PTS / RENDER_WIDTH_PX;

        // Calcular altura de página en píxeles
        float pageHeightInPixels = A4_HEIGHT_PTS / scale;
        int numPages = (int) Math.ceil(viewHeightPx / pageHeightInPixels);

        for (int i = 0; i < numPages; i++) {
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(A4_WIDTH_PTS, A4_HEIGHT_PTS, i + 1).create();
            PdfDocument.Page page = document.startPage(pageInfo);
            Canvas canvas = page.getCanvas();

            // Fondo blanco
            Paint paint = new Paint();
            paint.setColor(Color.WHITE);
            canvas.drawRect(0, 0, A4_WIDTH_PTS, A4_HEIGHT_PTS, paint);

            // Escalar y Trasladar
            canvas.save();
            canvas.scale(scale, scale);
            canvas.translate(0, -i * pageHeightInPixels);

            pdfView.draw(canvas);

            canvas.restore();
            document.finishPage(page);
        }

        // 7. Guardar
        String nombreArchivo = "Certificado_" + (cert.getNumeroCertificado() != null ? cert.getNumeroCertificado() : "borrador") + ".pdf";

        // Intentar guardar en carpeta pública Documentos
        File directory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "MaquirentCertificados");
        if (!directory.exists()) directory.mkdirs();

        File archivoFinal = new File(directory, nombreArchivo);

        try {
            FileOutputStream fos = new FileOutputStream(archivoFinal);
            document.writeTo(fos);
            document.close();
            fos.close();
        } catch (IOException e) {
            // Fallback a caché si falla escritura pública
            File cacheDir = new File(context.getCacheDir(), "certificados");
            if (!cacheDir.exists()) cacheDir.mkdirs();
            archivoFinal = new File(cacheDir, nombreArchivo);
            FileOutputStream fos = new FileOutputStream(archivoFinal);
            document.writeTo(fos);
            document.close();
            fos.close();
        }

        return archivoFinal;
    }

    /**
     * Crea un contexto wrapper con la densidad y configuración forzada para impresión.
     */
    private Context createContextWithDensity(Context context, float densityScale) {
        Configuration configuration = new Configuration(context.getResources().getConfiguration());
        configuration.densityDpi = (int) (densityScale * 160); // 160 es la base (mdpi)
        configuration.fontScale = 1.0f; // Forzar escala de fuente normal (ignorar configuración de usuario de letra grande)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            return context.createConfigurationContext(configuration);
        } else {
            // Fallback para versiones muy viejas (no debería ser tu caso)
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
}