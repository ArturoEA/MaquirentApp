package com.example.maquirentapp.workers;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.maquirentapp.Model.AlquilerMensual;
import com.example.maquirentapp.Model.DetalleMes;
import com.example.maquirentapp.Network.FirebaseServicio;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AlquilerMensualCheckWorker extends Worker {
    private static final String TAG = "AlquilerMensualCheck";
    private FirebaseServicio firebaseServicio;

    public AlquilerMensualCheckWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
        firebaseServicio = new FirebaseServicio();
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "Verificando alquileres mensuales...");

        try {
            verificarYGenerarNuevosMeses();
            return Result.success();
        } catch (Exception e) {
            Log.e(TAG, "Error al verificar alquileres: " + e.getMessage());
            return Result.failure();
        }
    }

    private void verificarYGenerarNuevosMeses() {
        // Obtener todos los alquileres mensuales no finalizados
        firebaseServicio.getAlquileresMensuales(new FirebaseServicio.OnAlquileresLoadedListener() {
            @Override
            public void onSuccess(List<AlquilerMensual> alquileres) {
                for (AlquilerMensual alquiler : alquileres) {
                    if (!alquiler.isFinalizado()) {
                        verificarAlquiler(alquiler);
                    }
                }
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error al obtener alquileres: " + e.getMessage());
            }
        });
    }

    private void verificarAlquiler(AlquilerMensual alquiler) {
        // Obtener el último DetalleMes de este alquiler
        firebaseServicio.getUltimoDetalleMes(alquiler.getId(),
                new FirebaseServicio.OnDetalleMesLoadedListener() {
                    @Override
                    public void onSuccess(DetalleMes ultimoDetalle) {
                        // Verificar si ya pasaron 30 días desde la fecha fin del último mes
                        if (debeGenerarNuevoMes(ultimoDetalle)) {
                            generarSiguienteMes(alquiler, ultimoDetalle);
                        }
                    }

                    @Override
                    public void onError(Exception e) {
                        // No hay detalles, esto no debería pasar si el alquiler fue creado correctamente
                        Log.e(TAG, "No se encontró último detalle para alquiler: " + alquiler.getId());
                    }
                });
    }

    private boolean debeGenerarNuevoMes(DetalleMes ultimoDetalle) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Date fechaFin = sdf.parse(ultimoDetalle.getFechaFin());

            if (fechaFin == null) return false;

            Calendar calFechaFin = Calendar.getInstance();
            calFechaFin.setTime(fechaFin);

            Calendar hoy = Calendar.getInstance();

            // Si ya pasó la fecha fin del último mes, generar el siguiente
            return hoy.after(calFechaFin);

        } catch (ParseException e) {
            Log.e(TAG, "Error al parsear fecha: " + e.getMessage());
            return false;
        }
    }

    private void generarSiguienteMes(AlquilerMensual alquiler, DetalleMes ultimoDetalle) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Date fechaFinAnterior = sdf.parse(ultimoDetalle.getFechaFin());

            if (fechaFinAnterior == null) return;

            Calendar cal = Calendar.getInstance();
            cal.setTime(fechaFinAnterior);

            // El nuevo mes comienza al día siguiente del fin del anterior
            cal.add(Calendar.DAY_OF_MONTH, 1);
            Date fechaInicio = cal.getTime();

            // Sumar 30 días para la fecha fin
            cal.add(Calendar.DAY_OF_MONTH, 30);
            Date fechaFin = cal.getTime();

            DetalleMes nuevoMes = new DetalleMes();
            nuevoMes.setIdAlquilerMensual(alquiler.getId());
            nuevoMes.setNumeroMes(ultimoDetalle.getNumeroMes() + 1);
            nuevoMes.setFechaInicio(sdf.format(fechaInicio));
            nuevoMes.setFechaFin(sdf.format(fechaFin));

            // Crear título del período
            SimpleDateFormat monthFormat = new SimpleDateFormat("MMMM", new Locale("es", "ES"));
            String mesInicio = monthFormat.format(fechaInicio);
            String mesFin = monthFormat.format(fechaFin);
            nuevoMes.setTituloPeriodo(
                    mesInicio.substring(0, 1).toUpperCase() + mesInicio.substring(1) +
                            " - " + mesFin.substring(0, 1).toUpperCase() + mesFin.substring(1)
            );

            // Guardar en Firebase
            firebaseServicio.crearDetalleMes(nuevoMes, new FirebaseServicio.OnDetalleMesCreatedListener() {
                @Override
                public void onSuccess(DetalleMes detalle) {
                    Log.d(TAG, "Nuevo mes generado: " + detalle.getTituloPeriodo() +
                            " para alquiler " + alquiler.getId());
                }

                @Override
                public void onError(Exception e) {
                    Log.e(TAG, "Error al crear nuevo mes: " + e.getMessage());
                }
            });

        } catch (ParseException e) {
            Log.e(TAG, "Error al generar nuevo mes: " + e.getMessage());
        }
    }
}