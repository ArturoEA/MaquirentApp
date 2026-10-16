package com.example.maquirentapp.strategy;

import android.content.Context;
import com.example.maquirentapp.Model.Cotizacion;

public class MaquirentPdfStrategy extends BasePdfGeneratorStrategy {

    @Override
    public String generarEncabezadoHtml(Context context) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div style='text-align:left; margin-bottom:20px;'>");
        sb.append("<div><span class='titulo-maqui'>MAQUI</span><span class='titulo-rent'>RENT</span><span class='color-encabezado'> SERVICIOS GENERALES</span></div>");
        sb.append("<div class='color-encabezado'>");
        sb.append("<div style='display: flex; justify-content: space-between; align-items: center;'>");
        sb.append("<span>HUGO ALBERTO ESQUIVEL PANDO</span>");
        sb.append("<span>RUC: 10266739414</span>");
        sb.append("</div>");
        sb.append("<div><span>SERVICIOS Y ALQUILER DE GRUPOS ELECTRÓGENOS, COMPRESORAS Y EQUIPOS DE CONSTRUCCIÓN</span></div>");
        sb.append("<div><span>AV. ATAHUALPA 810 - CAJAMARCA | TELÉF. 976959490 - 955425015 - 969775015</span></div>");
        sb.append("<hr>");
        sb.append("</div>");
        sb.append("</div>");
        return sb.toString();
    }

    @Override
    public String generarCondicionesComercialesHtml(Cotizacion cotizacion) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div style='margin-top: 20px; text-align: left;'>");
        sb.append("<h3 style='text-decoration: underline; font-size: 11pt;'>CONDICIONES COMERCIALES</h3>");
        sb.append("<ul style='padding-left: 20px; list-style-type: disc;'>");
        sb.append("<li>LOS EQUIPOS SE ALQUILAN SIN COMBUSTIBLE NI OPERADOR.</li>");
        sb.append("<li>EL PAGO DEL ALQUILER SE REALIZARÁ A 30 DÍAS DE LA FACTURACIÓN.</li>");
        sb.append("<li>TARIFA SE APLICA CON HORAS MÍNIMAS DE <b>").append(cotizacion.getHorasMinimas()).append(" HORAS MENSUALES</b>, EXCESOS SE FACTURARÁN SEGÚN HORÓMETRO.</li>");
        sb.append("<li>LOS EQUIPOS DEBEN SER DEVUELTOS EN LAS MISMAS CONDICIONES EN QUE FUERON ENTREGADOS SIN MÁS DETERIORO DEL QUE PUEDE PRODUCIRSE POR EL USO NORMAL. EN CASO DE PRESENTARSE CUALQUIER DAÑO, EL CLIENTE SE HARÁ RESPONSABLE DE LAS REPARACIONES, SEGÚN LOS CARGOS POR DAÑOS VIGENTES A LA DEVOLUCIÓN.</li>");
        sb.append("<li>EN CASO DE PÉRDIDA DE LOS EQUIPOS, EL CLIENTE SE HARÁ RESPONSABLE DE LA DEVOLUCIÓN DEL MISMO, YA SEA COMO EQUIPO O EN EFECTIVO, TENIENDO EN CUENTA LA FACTURA DE COMPRA.</li>");
        sb.append("<li>LA INSPECCIÓN DIARIA ES RESPONSABILIDAD DEL CLIENTE (AGUA Y ACEITE DE MOTOR).</li>");
        sb.append("<li>LOS ADITAMENTOS DEL EQUIPO INCLUIDOS SON BANDEJA Y KIT ANTIDERRAME, EXTINTOR, BARRA PUESTA A TIERRA.</li>");
        sb.append("<li>EL EQUIPO SE ENTREGA CON ACEITE DE MOTOR Y FILTROS NUEVOS.</li>");
        sb.append("<li>PARA LOS MANTENIMIENTOS DE LOS GENERADORES EN MINA, SE PUEDE CAPACITAR A UN PERSONAL DE LA EMPRESA CONTRATISTA Y SE LE OTORGARÁ TODOS LOS INSUMOS PARA QUE PUEDA REALIZAR EL MANTENIMIENTO PREVENTIVO CADA 250 HORAS.</li>");
        sb.append("<li>LA PRESENTE COTIZACIÓN TIENE UNA VALIDEZ DE 15 DÍAS DESDE SU CREACIÓN.</li>");
        sb.append("</ul>");
        sb.append("</div>");
        return sb.toString();
    }
}
