package com.example.maquirentapp.Utils;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import com.example.maquirentapp.Model.Cotizacion;
import com.example.maquirentapp.Model.ItemCotizacion;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class WordGenerator {

    private static final String TAG = "WordGenerator";

    public File generarCotizacionWord(Context context, Cotizacion cotizacion) throws IOException {
        // 1. Copiar la plantilla desde Assets a un archivo de trabajo
        InputStream inputStream = context.getAssets().open("plantilla_cotizacion.docx");
        XWPFDocument document = new XWPFDocument(inputStream);

        // 2. Preparar el mapa de datos simples
        Map<String, String> datos = new HashMap<>();
        datos.put("{{FECHA}}", cotizacion.getFechaEmision());
        datos.put("{{CLIENTE}}", cotizacion.getClienteNombre());
        datos.put("{{HORAS}}", String.valueOf(cotizacion.getHorasMinimas()));
        datos.put("{{CLIENTE_RUC}}", cotizacion.getClienteRuc());
        datos.put("{{LUGAR_TRABAJO}}", cotizacion.getLugarTrabajo());

        String simbolo = "SOL".equals(cotizacion.getMoneda()) ? "S/." : "$";
        datos.put("{{TOTAL_PARCIAL}}", simbolo + " " + String.format(Locale.US, "%.2f", cotizacion.getSubtotalGlobal()));
        datos.put("{{TOTAL_IGV}}", simbolo + " " + String.format(Locale.US, "%.2f", cotizacion.getTotalGlobal()));

        // 3. Reemplazar textos en párrafos normales
        for (XWPFParagraph p : document.getParagraphs()) {
            reemplazarEnParrafo(p, datos);
        }

        // 4. Manejar Tablas
        for (XWPFTable table : document.getTables()) {
            if (tablaContieneMarcador(table, "{{EQUIPO}}")) {
                llenarTablaItems(table, cotizacion.getItems(), simbolo);
            }
            else if (tablaContieneMarcador(table, "{{COD_GRUPO}}")) {
                llenarTablaHorasExtras(table, cotizacion.getItems(), simbolo);
            }
            else if (tablaContieneMarcador(table, "{{TOTAL_PARCIAL}}")) {
                reemplazarEnTabla(table, datos);
            }

            reemplazarEnTabla(table, datos);
        }

        // 5. Guardar el archivo generado
        String nombreArchivo = "Cotizacion_" + cotizacion.getNumeroCotizacion() + ".docx";
        File carpetaCache = new File(context.getCacheDir(), "documentos");
        if (!carpetaCache.exists()) {
            carpetaCache.mkdirs();
        }

        File archivoFinal = new File(carpetaCache, nombreArchivo);

        FileOutputStream out = new FileOutputStream(archivoFinal);
        document.write(out);
        out.close();
        document.close();

        return archivoFinal;
    }
    private void llenarTablaItems(XWPFTable table, List<ItemCotizacion> items, String simbolo) {
        int filaPlantillaIndex = -1;
        for (int i = 0; i < table.getRows().size(); i++) {
            if (filaContieneMarcador(table.getRow(i), "{{EQUIPO}}")) {
                filaPlantillaIndex = i;
                break;
            }
        }

        if (filaPlantillaIndex == -1) return;

        XWPFTableRow filaPlantilla = table.getRow(filaPlantillaIndex);

        for (int i = 0; i < items.size(); i++) {
            ItemCotizacion item = items.get(i);

            XWPFTableRow nuevaFila = table.insertNewTableRow(filaPlantillaIndex + 1 + i);
            while (nuevaFila.getTableCells().size() < filaPlantilla.getTableCells().size()) {
                nuevaFila.createCell();
            }

            Map<String, String> datosItem = new HashMap<>();
            datosItem.put("{{EQUIPO}}", item.getDescripcionEquipo());
            datosItem.put("{{POT}}", item.getPotencia());
            datosItem.put("{{MODO}}", item.getModoTrabajo());
            datosItem.put("{{MARCA}}", item.getMarca());
            datosItem.put("{{INCLUYE}}", item.getIncluye());
            datosItem.put("{{PRECIO_PARCIAL}}", simbolo + " " + String.format(Locale.US, "%.2f", item.getPrecioMensual()));
            datosItem.put("{{PRECIO_IGV}}", simbolo + " " + String.format(Locale.US, "%.2f", item.getTotalConIgv()));

            for (int j = 0; j < filaPlantilla.getTableCells().size(); j++) {
                String textoPlantilla = filaPlantilla.getCell(j).getText();
                for (Map.Entry<String, String> entry : datosItem.entrySet()) {
                    if (textoPlantilla.contains(entry.getKey())) {
                        textoPlantilla = textoPlantilla.replace(entry.getKey(), entry.getValue());
                    }
                }
                nuevaFila.getCell(j).setText(textoPlantilla);
            }
        }

        table.removeRow(filaPlantillaIndex);
        try {
            XWPFParagraph paragraph = table.getBody().insertNewParagraph(table.getCTTbl().newCursor());
            paragraph.createRun().addBreak();
        } catch (Exception e) {
        }
    }

    private void llenarTablaHorasExtras(XWPFTable table, List<ItemCotizacion> items, String simbolo) {
        int filaPlantillaIndex = -1;
        for (int i = 0; i < table.getRows().size(); i++) {
            if (filaContieneMarcador(table.getRow(i), "{{COD_GRUPO}}")) {
                filaPlantillaIndex = i;
                break;
            }
        }

        if (filaPlantillaIndex == -1) return;

        XWPFTableRow filaPlantilla = table.getRow(filaPlantillaIndex);

        for (ItemCotizacion item : items) {
            XWPFTableRow nuevaFila = table.createRow();
            while (nuevaFila.getTableCells().size() < filaPlantilla.getTableCells().size()) {
                nuevaFila.createCell();
            }

            for (int i = 0; i < filaPlantilla.getTableCells().size(); i++) {
                String texto = filaPlantilla.getCell(i).getText();
                texto = texto.replace("{{COD_GRUPO}}", item.getDescripcionEquipo());
                texto = texto.replace("{{PRECIO_HE}}", simbolo + " " + String.format(Locale.US, "%.2f", item.getPrecioHoraExtra()));
                nuevaFila.getCell(i).setText(texto);
            }
        }
        table.removeRow(filaPlantillaIndex);
    }
    private void reemplazarEnParrafo(XWPFParagraph p, Map<String, String> datos) {
        List<XWPFRun> runs = p.getRuns();
        if (runs != null) {
            for (XWPFRun r : runs) {
                String text = r.getText(0);
                if (text != null) {
                    for (Map.Entry<String, String> entry : datos.entrySet()) {
                        if (text.contains(entry.getKey())) {
                            text = text.replace(entry.getKey(), entry.getValue());
                            r.setText(text, 0);
                        }
                    }
                }
            }
        }
    }
    private void reemplazarEnTabla(XWPFTable table, Map<String, String> datos) {
        for (XWPFTableRow row : table.getRows()) {
            for (XWPFTableCell cell : row.getTableCells()) {
                for (XWPFParagraph p : cell.getParagraphs()) {
                    reemplazarEnParrafo(p, datos);
                }
            }
        }
    }

    private boolean tablaContieneMarcador(XWPFTable table, String marcador) {
        for (XWPFTableRow row : table.getRows()) {
            if (filaContieneMarcador(row, marcador)) return true;
        }
        return false;
    }

    private boolean filaContieneMarcador(XWPFTableRow row, String marcador) {
        for (XWPFTableCell cell : row.getTableCells()) {
            if (cell.getText().contains(marcador)) return true;
        }
        return false;
    }
    public static void limpiarCacheAntiguo(Context context) {
        try {
            File carpetaCache = new File(context.getCacheDir(), "documentos");
            if (carpetaCache.exists() && carpetaCache.isDirectory()) {
                File[] archivos = carpetaCache.listFiles();
                if (archivos != null) {
                    long tiempoActual = System.currentTimeMillis();
                    long tiempoMaximo = 24 * 60 * 60 * 1000;

                    for (File archivo : archivos) {
                        long diferencia = tiempoActual - archivo.lastModified();
                        if (diferencia > tiempoMaximo) {
                            boolean borrado = archivo.delete();
                            if (borrado) {
                                Log.d("CacheLimpieza", "Archivo antiguo borrado: " + archivo.getName());
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e("CacheLimpieza", "Error limpiando cache", e);
        }
    }
}