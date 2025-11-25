package com.example.maquirentapp.Utils;

import android.content.Context;
import android.os.Environment;

import com.example.maquirentapp.Model.ItemValorizacion;
import com.example.maquirentapp.Model.Valorizacion;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

public class ExcelGenerator {

    public File generarValorizacionExcel(Context context, Valorizacion valorizacion) throws IOException {
        // 1. Cargar Plantilla
        InputStream inputStream = context.getAssets().open("plantilla_valorizacion_vacia.xlsx");
        Workbook workbook = new XSSFWorkbook(inputStream);
        Sheet sheet = workbook.getSheetAt(0);

        // 2. Llenar Cabecera (Ajusta los índices según tu plantilla real)
        // Basado en tu CSV: Cliente está aprox en Fila 8 (Índice 7), Columna C (Índice 2)
        // OJO: En POI las filas y columnas empiezan en 0.

        escribirCelda(sheet, 7, 2, valorizacion.getNombreCliente()); // Cliente
        escribirCelda(sheet, 7, 18, valorizacion.getClienteRuc());   // RUC
        escribirCelda(sheet, 8, 2, valorizacion.getClienteDireccion()); // Dirección
        // Equipos en cabecera (opcional, si quieres listar todos)
        // escribirCelda(sheet, 9, 2, "Varios Equipos");
        escribirCelda(sheet, 10, 2, valorizacion.getUbicacionTrabajo()); // Lugar

        // 3. Configurar Estilos para la Tabla Dinámica
        CellStyle estiloBorde = crearEstiloBorde(workbook);
        CellStyle estiloMoneda = crearEstiloBorde(workbook);
        estiloMoneda.setDataFormat(workbook.createDataFormat().getFormat("#,##0.00"));

        // 4. Llenar Filas de Equipos
        // Asumimos que la tabla empieza en la fila 15 (índice 14) del Excel original
        int rowIndex = 14;
        List<ItemValorizacion> items = valorizacion.getItems();

        for (ItemValorizacion item : items) {
            // Crear nueva fila o usar existente (desplaza hacia abajo si es necesario)
            // Nota: shiftRows es pesado, si la plantilla está vacía abajo, solo createRow
            Row row = sheet.createRow(rowIndex);

            // Columna 0: Nombre del Equipo
            crearCeldaConEstilo(row, 0, item.getDescripcionEquipo(), estiloBorde);

            // Columna 1: "DÍAS" (vacío o texto fijo)
            crearCeldaConEstilo(row, 1, "", estiloBorde);

            // Columnas 2 a 32 (Días 1-31)
            // Aquí podrías marcar con "X" o las horas si tienes ese detalle diario.
            // Por ahora las dejamos vacías pero con borde.
            for (int i = 2; i <= 32; i++) {
                crearCeldaConEstilo(row, i, "", estiloBorde);
            }

            // Columna 33: Horas Totales / Días
            crearCeldaConEstilo(row, 33, String.valueOf(item.getHorasTrabajadas()), estiloBorde);

            // Columna 34: Precio Unitario
            // Convertimos a celda numérica para fórmulas
            Cell cellPrecio = row.createCell(34);
            cellPrecio.setCellValue(item.getPrecioMes());
            cellPrecio.setCellStyle(estiloMoneda);

            // Columna 35: Total Item
            Cell cellTotal = row.createCell(35);
            cellTotal.setCellValue(item.getTotalItem());
            cellTotal.setCellStyle(estiloMoneda);

            rowIndex++;
        }

        // 5. Dibujar Pie de Página (Totales)
        // Dejamos una fila vacía
        rowIndex++;

        // Fila Subtotal
        Row rowSub = sheet.createRow(rowIndex++);
        crearCeldaConEstilo(rowSub, 34, "SUBTOTAL", estiloBorde);
        crearCeldaNumerica(rowSub, 35, valorizacion.getSubtotal(), estiloMoneda);

        // Fila IGV
        Row rowIgv = sheet.createRow(rowIndex++);
        crearCeldaConEstilo(rowIgv, 34, "IGV 18%", estiloBorde);
        crearCeldaNumerica(rowIgv, 35, valorizacion.getIgv(), estiloMoneda);

        // Fila Total
        Row rowTotal = sheet.createRow(rowIndex++);
        crearCeldaConEstilo(rowTotal, 34, "TOTAL", estiloBorde);
        crearCeldaNumerica(rowTotal, 35, valorizacion.getTotal(), estiloMoneda);

        // 6. Guardar en Caché
        String nombreArchivo = "Valorizacion_" + valorizacion.getNumeroValorizacion() + ".xlsx";
        File carpetaCache = new File(context.getCacheDir(), "documentos_excel");
        if (!carpetaCache.exists()) carpetaCache.mkdirs();

        File archivoFinal = new File(carpetaCache, nombreArchivo);
        FileOutputStream fos = new FileOutputStream(archivoFinal);
        workbook.write(fos);
        workbook.close();
        fos.close();

        return archivoFinal;
    }

    // --- HELPER METHODS ---

    private void escribirCelda(Sheet sheet, int r, int c, String texto) {
        Row row = sheet.getRow(r);
        if (row == null) row = sheet.createRow(r);
        Cell cell = row.getCell(c);
        if (cell == null) cell = row.createCell(c);

        // Si había un placeholder {{...}}, lo reemplazamos respetando el estilo
        // Si está vacío, solo ponemos el texto.
        cell.setCellValue(texto != null ? texto : "");
    }

    private void crearCeldaConEstilo(Row row, int c, String texto, CellStyle estilo) {
        Cell cell = row.createCell(c);
        cell.setCellValue(texto);
        cell.setCellStyle(estilo);
    }

    private void crearCeldaNumerica(Row row, int c, double valor, CellStyle estilo) {
        Cell cell = row.createCell(c);
        cell.setCellValue(valor);
        cell.setCellStyle(estilo);
    }

    private CellStyle crearEstiloBorde(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }
}