package com.example.maquirentapp.strategy;

public class StrategyProvider {
    private static final PdfGeneratorStrategy pdfStrategy = new VolkerPdfStrategy();
    private static final CalculadoraAlquilerStrategy calculadoraStrategy = new VolkerCalculadoraStrategy();
    private static final CorrelativoStrategy correlativoStrategy = new VolkerCorrelativoStrategy();

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
