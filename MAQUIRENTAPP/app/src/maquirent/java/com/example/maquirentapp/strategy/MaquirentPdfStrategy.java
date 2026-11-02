package com.example.maquirentapp.strategy;

import android.content.Context;
import com.example.maquirentapp.Model.Cotizacion;

import java.util.ArrayList;
import java.util.List;

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
    public List<String> obtenerCondicionesPorDefecto(String tipoAlquiler, int horasMinimas, int diasAlquiler, int horasPorDia) {
        List<String> condiciones = new ArrayList<>();
        if (Cotizacion.TIPO_DIARIO.equalsIgnoreCase(tipoAlquiler)) {
            condiciones.add("LOS EQUIPOS SE ALQUILAN SIN COMBUSTIBLE NI OPERADOR.");
            condiciones.add("EL PAGO DEL ALQUILER SE REALIZARÁ AL CONTADO O PREVIO ACUERDO COMERCIAL ANTES DE LA ENTREGA.");
            condiciones.add("TARIFA SE APLICA POR JORNADA DE HASTA " + (horasPorDia > 0 ? horasPorDia : 8) + " HORAS DIARIAS POR " + (diasAlquiler > 0 ? diasAlquiler : 1) + " DÍA(S), EXCESOS SE FACTURARÁN SEGÚN HORÓMETRO.");
            condiciones.add("LOS EQUIPOS DEBEN SER DEVUELTOS EN LAS MISMAS CONDICIONES EN QUE FUERON ENTREGADOS SIN MÁS DETERIORO DEL QUE PUEDE PRODUCIRSE POR EL USO NORMAL. EN CASO DE PRESENTARSE CUALQUIER DAÑO, EL CLIENTE SE HARÁ RESPONSABLE DE LAS REPARACIONES, SEGÚN LOS CARGOS POR DAÑOS VIGENTES A LA DEVOLUCIÓN.");
            condiciones.add("EN CASO DE PÉRDIDA DE LOS EQUIPOS, EL CLIENTE SE HARÁ RESPONSABLE DE LA DEVOLUCIÓN DEL MISMO, YA SEA COMO EQUIPO O EN EFECTIVO, TENIENDO EN CUENTA LA FACTURA DE COMPRA.");
            condiciones.add("LA INSPECCIÓN DIARIA ES RESPONSABILIDAD DEL CLIENTE (AGUA Y ACEITE DE MOTOR).");
            condiciones.add("LOS ADITAMENTOS DEL EQUIPO INCLUIDOS SON BANDEJA Y KIT ANTIDERRAME, EXTINTOR, BARRA PUESTA A TIERRA.");
            condiciones.add("EL EQUIPO SE ENTREGA CON ACEITE DE MOTOR Y FILTROS NUEVOS.");
            condiciones.add("LA PRESENTE COTIZACIÓN TIENE UNA VALIDEZ DE 15 DÍAS DESDE SU CREACIÓN.");
        } else {
            condiciones.add("LOS EQUIPOS SE ALQUILAN SIN COMBUSTIBLE NI OPERADOR.");
            condiciones.add("EL PAGO DEL ALQUILER SE REALIZARÁ A 30 DÍAS DE LA FACTURACIÓN.");
            condiciones.add("TARIFA SE APLICA CON HORAS MÍNIMAS DE " + (horasMinimas > 0 ? horasMinimas : 200) + " HORAS MENSUALES, EXCESOS SE FACTURARÁN SEGÚN HORÓMETRO.");
            condiciones.add("LOS EQUIPOS DEBEN SER DEVUELTOS EN LAS MISMAS CONDICIONES EN QUE FUERON ENTREGADOS SIN MÁS DETERIORO DEL QUE PUEDE PRODUCIRSE POR EL USO NORMAL. EN CASO DE PRESENTARSE CUALQUIER DAÑO, EL CLIENTE SE HARÁ RESPONSABLE DE LAS REPARACIONES, SEGÚN LOS CARGOS POR DAÑOS VIGENTES A LA DEVOLUCIÓN.");
            condiciones.add("EN CASO DE PÉRDIDA DE LOS EQUIPOS, EL CLIENTE SE HARÁ RESPONSABLE DE LA DEVOLUCIÓN DEL MISMO, YA SEA COMO EQUIPO O EN EFECTIVO, TENIENDO EN CUENTA LA FACTURA DE COMPRA.");
            condiciones.add("LA INSPECCIÓN DIARIA ES RESPONSABILIDAD DEL CLIENTE (AGUA Y ACEITE DE MOTOR).");
            condiciones.add("LOS ADITAMENTOS DEL EQUIPO INCLUIDOS SON BANDEJA Y KIT ANTIDERRAME, EXTINTOR, BARRA PUESTA A TIERRA.");
            condiciones.add("EL EQUIPO SE ENTREGA CON ACEITE DE MOTOR Y FILTROS NUEVOS.");
            condiciones.add("PARA LOS MANTENIMIENTOS DE LOS GENERADORES EN MINA, SE PUEDE CAPACITAR A UN PERSONAL DE LA EMPRESA CONTRATISTA Y SE LE OTORGARÁ TODOS LOS INSUMOS PARA QUE PUEDA REALIZAR EL MANTENIMIENTO PREVENTIVO CADA 250 HORAS.");
            condiciones.add("LA PRESENTE COTIZACIÓN TIENE UNA VALIDEZ DE 15 DÍAS DESDE SU CREACIÓN.");
        }
        return condiciones;
    }
}
