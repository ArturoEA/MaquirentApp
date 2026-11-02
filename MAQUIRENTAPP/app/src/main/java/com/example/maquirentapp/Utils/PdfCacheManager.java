package com.example.maquirentapp.Utils;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Gestor centralizado de caché persistente de archivos PDF.
 * Permite que los documentos (fichas técnicas, valorizaciones, mantenimientos)
 * se almacenen localmente y se puedan abrir en modo offline por defecto sin intervención del usuario.
 */
public class PdfCacheManager {
    private static final String TAG = "PdfCacheManager";
    private static final String DIR_PDFS_OFFLINE = "pdfs_offline";

    public static File getOfflinePdfDir(Context context) {
        File dir = new File(context.getFilesDir(), DIR_PDFS_OFFLINE);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    public static String sanitizeFilename(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            return "documento_" + System.currentTimeMillis() + ".pdf";
        }
        String clean = filename.trim().replaceAll("[^a-zA-Z0-9._-]", "_");
        if (!clean.toLowerCase().endsWith(".pdf")) {
            clean += ".pdf";
        }
        return clean;
    }

    public static File getCachedFile(Context context, String filename) {
        return new File(getOfflinePdfDir(context), sanitizeFilename(filename));
    }

    public static boolean isFileCached(Context context, String filename) {
        File f = getCachedFile(context, filename);
        return f.exists() && f.length() > 0;
    }

    /**
     * Obtiene el archivo PDF local.
     * Si ya fue descargado previamente, lo retorna inmediatamente desde el almacenamiento interno
     * sin realizar peticiones de red (100% offline).
     * Si no existe y hay conexión, lo descarga a disco permanentemente para futuras consultas offline.
     */
    public static File obtenerPdf(Context context, String urlPdf, String nombreArchivo) throws Exception {
        File cachedFile = getCachedFile(context, nombreArchivo);
        if (cachedFile.exists() && cachedFile.length() > 0) {
            return cachedFile;
        }

        if (urlPdf == null || !urlPdf.startsWith("http")) {
            if (urlPdf != null) {
                File local = new File(urlPdf);
                if (local.exists()) return local;
            }
            throw new Exception("El archivo no está disponible offline y la URL no es válida.");
        }

        File tempFile = new File(context.getCacheDir(), "dl_" + System.currentTimeMillis() + ".tmp");
        URL url = new URL(urlPdf);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(20000);
        connection.connect();

        try (InputStream is = connection.getInputStream();
             FileOutputStream fos = new FileOutputStream(tempFile)) {
            byte[] buffer = new byte[4096];
            int len;
            while ((len = is.read(buffer)) > 0) {
                fos.write(buffer, 0, len);
            }
        } finally {
            connection.disconnect();
        }

        if (tempFile.length() > 0) {
            if (cachedFile.exists()) cachedFile.delete();
            if (!tempFile.renameTo(cachedFile)) {
                // Fallback de copiado por si renameTo falla entre particiones
                try (InputStream is = new FileInputStream(tempFile);
                     FileOutputStream fos = new FileOutputStream(cachedFile)) {
                    byte[] buffer = new byte[4096];
                    int len;
                    while ((len = is.read(buffer)) > 0) {
                        fos.write(buffer, 0, len);
                    }
                }
                tempFile.delete();
            }
        }

        return cachedFile;
    }

    public static void eliminarPdfOffline(Context context, String nombreArchivo) {
        try {
            File f = getCachedFile(context, nombreArchivo);
            if (f.exists()) {
                f.delete();
            }
        } catch (Exception e) {
            Log.w(TAG, "Error eliminando PDF offline: " + nombreArchivo, e);
        }
    }

    public static void renombrarPdfOffline(Context context, String nombreViejo, String nombreNuevo) {
        try {
            File viejo = getCachedFile(context, nombreViejo);
            File nuevo = getCachedFile(context, nombreNuevo);
            if (viejo.exists()) {
                if (nuevo.exists()) {
                    nuevo.delete();
                }
                if (!viejo.renameTo(nuevo)) {
                    // Fallback con copiado
                    try (InputStream is = new FileInputStream(viejo);
                         FileOutputStream fos = new FileOutputStream(nuevo)) {
                        byte[] buffer = new byte[4096];
                        int len;
                        while ((len = is.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                    viejo.delete();
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Error renombrando PDF offline: " + nombreViejo + " -> " + nombreNuevo, e);
        }
    }
}
