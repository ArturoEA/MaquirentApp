package com.example.maquirentapp.strategy;

import android.content.Context;

import com.example.maquirentapp.Model.CertificadoOperatividad;
import com.example.maquirentapp.Model.Cotizacion;
import com.example.maquirentapp.Model.InfoPlaca;
import com.example.maquirentapp.Model.Mantenimiento;
import com.example.maquirentapp.Model.Valorizacion;

import java.io.File;
import java.util.Map;

public interface PdfGeneratorStrategy {
    /**
     * Genera el HTML formateado para la cotización con el encabezado, términos y firma de la empresa.
     */
    String generarHtmlCotizacion(Context context, Cotizacion cotizacion);

    /**
     * Genera el documento PDF vectorial de la valorización con la cabecera corporativa y totales.
     */
    File generarPdfValorizacion(Context context, Valorizacion valorizacion) throws Exception;
    File generarValorizacionPdf(Context context, Valorizacion valorizacion) throws Exception;

    /**
     * Genera el PDF del Certificado de Operatividad.
     */
    File generarCertificadoPdf(Context context, CertificadoOperatividad cert, String codigoGrupo) throws Exception;

    /**
     * Genera el informe técnico de mantenimiento preventivo en formato PDF vectorial.
     */
    void generarInformeMantenimientoPdf(Context context, File archivoSalida, Mantenimiento mant, InfoPlaca placa, String codigoGrupo, Map<String, Object> datos) throws Exception;
}
