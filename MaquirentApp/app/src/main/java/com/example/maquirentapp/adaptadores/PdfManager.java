package com.example.maquirentapp.adaptadores;

import android.content.Context;
import android.net.Uri;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class PdfManager {
    private static final String PDF_DIR = "fichas_pdf";

    // Guardar PDF localmente
    public static String guardarPdfLocal(Context context, Uri pdfUri) throws IOException {
        File dir = new File(context.getFilesDir(), PDF_DIR);
        if (!dir.exists()) dir.mkdirs();

        String fileName = "ficha_" + System.currentTimeMillis() + ".pdf";
        File pdfFile = new File(dir, fileName);

        try (InputStream in = context.getContentResolver().openInputStream(pdfUri);
             OutputStream out = new FileOutputStream(pdfFile)) {
            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        }
        return pdfFile.getAbsolutePath();
    }

    // Obtener Uri de un PDF local
    public static Uri getPdfUri(Context context, String localPath) {
        File file = new File(localPath);
        return FileProvider.getUriForFile(
                context,
                context.getApplicationContext().getPackageName() + ".provider",
                file
        );
    }

    // Eliminar PDF local
    public static boolean eliminarPdfLocal(String localPath) {
        if (localPath != null) {
            File file = new File(localPath);
            return file.delete();
        }
        return false;
    }
}