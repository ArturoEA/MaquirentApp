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
import android.provider.MediaStore;
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
import java.io.OutputStream;

public class PdfGenerator {
    private static final int A4_WIDTH_PTS = 595;
    private static final int A4_HEIGHT_PTS = 842;
    private static final int RENDER_WIDTH_PX = 1190;
    private static final float TARGET_DENSITY = 2.0f;

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
}