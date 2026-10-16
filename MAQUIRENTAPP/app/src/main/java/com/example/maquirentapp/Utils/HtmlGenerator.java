package com.example.maquirentapp.Utils;

import android.content.Context;
import com.example.maquirentapp.Model.Cotizacion;
import com.example.maquirentapp.strategy.StrategyProvider;

public class HtmlGenerator {

    public String generarHtmlCotizacion(Context context, Cotizacion cotizacion) {
        return StrategyProvider.getPdfStrategy().generarHtmlCotizacion(context, cotizacion);
    }
}