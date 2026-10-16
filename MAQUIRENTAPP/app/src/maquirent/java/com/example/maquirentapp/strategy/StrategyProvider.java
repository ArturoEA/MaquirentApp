package com.example.maquirentapp.strategy;

public class StrategyProvider {
    private static final PdfGeneratorStrategy pdfStrategy = new MaquirentPdfStrategy();
    private static final CalculadoraAlquilerStrategy calculadoraStrategy = new MaquirentCalculadoraStrategy();
    private static final CorrelativoStrategy correlativoStrategy = new MaquirentCorrelativoStrategy();

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
