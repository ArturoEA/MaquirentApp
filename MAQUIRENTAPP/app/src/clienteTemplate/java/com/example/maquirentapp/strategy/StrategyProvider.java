package com.example.maquirentapp.strategy;

public class StrategyProvider {
    private static final PdfGeneratorStrategy pdfStrategy = new ClienteTemplatePdfStrategy();
    private static final CalculadoraAlquilerStrategy calculadoraStrategy = new ClienteTemplateCalculadoraStrategy();
    private static final CorrelativoStrategy correlativoStrategy = new ClienteTemplateCorrelativoStrategy();

    public static PdfGeneratorStrategy getPdfStrategy() {
        return pdfStrategy;
    }

    public static CalculadoraAlquilerStrategy getCalculadoraStrategy() {
        return calculadoraStrategy;
    }

    public static CorrelativoStrategy getCorrelativoStrategy() {
        return correlativoStrategy;
    }
}
