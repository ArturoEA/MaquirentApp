package com.example.maquirentapp.Utils;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class MantenimientoSaludManagerTest {
    @Test
    public void permiteRegistrarMantenimientoConHorometroMenorAlActual() {
        MantenimientoSaludManager.ResultadoValidacionHorometro resultado =
                MantenimientoSaludManager.validarHorometroMantenimiento(1200, 1150);

        assertTrue(resultado.esValido);
    }
}
