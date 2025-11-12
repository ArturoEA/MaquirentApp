package com.example.maquirentapp.View;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.maquirentapp.Model.AlquilerMensual;
import com.example.maquirentapp.Model.DetalleMes;
import com.example.maquirentapp.Network.FirebaseServicio;
import com.example.maquirentapp.R;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class GrupoElectrogenoFragment extends Fragment {
    private String codigo;
    private String idGrupo;
    private FirebaseServicio firebaseServicio;

    private Spinner spinnerMeses;
    private TextView tvTotalSOL, tvTotalUSD;
    private int mesSeleccionado = Calendar.getInstance().get(Calendar.MONTH); // Mes actual (0-11)
    private int anioActual = Calendar.getInstance().get(Calendar.YEAR);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            codigo = getArguments().getString("codigo");
            idGrupo = getArguments().getString("idGrupo");
        }
        firebaseServicio = new FirebaseServicio();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_grupo_electrogeno, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final String codigoLocal = codigo != null ? codigo :
                (getArguments() != null ? getArguments().getString("codigo") : null);
        final String idGrupoLocal = idGrupo != null ? idGrupo :
                (getArguments() != null ? getArguments().getString("idGrupo") : null);

        // Inicializar vistas
        spinnerMeses = view.findViewById(R.id.spinnerMeses);
        tvTotalSOL = view.findViewById(R.id.tvTotalSOL);
        tvTotalUSD = view.findViewById(R.id.tvTotalUSD);

        configurarSpinnerMeses();

        CardView cardMantenimientos = view.findViewById(R.id.cardMantenimientos);
        CardView cardHistorialAlquilerMensual = view.findViewById(R.id.cardHistorialAlquilerMensual);

        cardMantenimientos.setOnClickListener(v -> {
            Bundle args = new Bundle();
            if (codigoLocal != null) args.putString("codigo", codigoLocal);
            if (idGrupoLocal != null) args.putString("idGrupo", idGrupoLocal);
            Navigation.findNavController(view)
                    .navigate(R.id.action_grupoElectrogeno_to_mantenimientos, args);
        });

        cardHistorialAlquilerMensual.setOnClickListener(v -> {
            Bundle args = new Bundle();
            if (codigoLocal != null) args.putString("codigo", codigoLocal);
            if (idGrupoLocal != null) args.putString("idGrupo", idGrupoLocal);
            Navigation.findNavController(view)
                    .navigate(R.id.action_grupoElectrogeno_to_historialAlquilerMensual, args);
        });

        // Cargar totales del mes actual
        cargarTotalesMes(mesSeleccionado);
    }

    private void configurarSpinnerMeses() {
        List<String> meses = new ArrayList<>();
        String[] nombresMeses = {
                "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
                "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
        };

        for (String mes : nombresMeses) {
            meses.add(mes);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                meses
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMeses.setAdapter(adapter);

        // Seleccionar el mes actual por defecto
        spinnerMeses.setSelection(mesSeleccionado);

        spinnerMeses.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mesSeleccionado = position;
                cargarTotalesMes(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void cargarTotalesMes(int mes) {
        if (idGrupo == null) return;

        // Obtener todos los alquileres mensuales del grupo
        firebaseServicio.getAlquileresMensuales(new FirebaseServicio.OnAlquileresLoadedListener() {
            @Override
            public void onSuccess(List<AlquilerMensual> alquileres) {
                List<AlquilerMensual> alquileresFiltrados = new ArrayList<>();

                // Filtrar alquileres de este grupo
                for (AlquilerMensual alquiler : alquileres) {
                    if (alquiler.getIdGrupo() != null && alquiler.getIdGrupo().equals(idGrupo)) {
                        alquileresFiltrados.add(alquiler);
                    }
                }

                calcularTotalesPorMes(alquileresFiltrados, mes);
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(getContext(), "Error al cargar alquileres: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void calcularTotalesPorMes(List<AlquilerMensual> alquileres, int mes) {
        double totalSOL = 0;
        double totalUSD = 0;

        for (AlquilerMensual alquiler : alquileres) {
            // Obtener detalles de mes para este alquiler
            firebaseServicio.getDetallesMesPorAlquiler(alquiler.getId(),
                    new FirebaseServicio.OnDetallesMesLoadedListener() {
                        @Override
                        public void onSuccess(List<DetalleMes> detalles) {
                            double[] totales = calcularMontoDelMes(alquiler, detalles, mes);

                            if (alquiler.getMoneda() != null && alquiler.getMoneda().equals("USD")) {
                                actualizarTotal(tvTotalUSD, totales[0], "USD");
                            } else {
                                actualizarTotal(tvTotalSOL, totales[0], "SOL");
                            }
                        }

                        @Override
                        public void onError(Exception e) {
                            // Silenciar error si no hay detalles
                        }
                    });
        }
    }

    private double[] calcularMontoDelMes(AlquilerMensual alquiler, List<DetalleMes> detalles, int mesTarget) {
        double montoMes = 0;
        double montoHorasExtras = 0;

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

        for (DetalleMes detalle : detalles) {
            try {
                Date fechaInicio = sdf.parse(detalle.getFechaInicio());
                Date fechaFin = sdf.parse(detalle.getFechaFin());

                if (fechaInicio == null || fechaFin == null) continue;

                Calendar calInicio = Calendar.getInstance();
                calInicio.setTime(fechaInicio);

                Calendar calFin = Calendar.getInstance();
                calFin.setTime(fechaFin);

                // Verificar si el período cae en el mes objetivo
                int mesInicio = calInicio.get(Calendar.MONTH);
                int mesFin = calFin.get(Calendar.MONTH);
                int anioInicio = calInicio.get(Calendar.YEAR);
                int anioFin = calFin.get(Calendar.YEAR);

                // Si el período está completamente dentro del mes
                if (mesInicio == mesTarget && mesFin == mesTarget &&
                        anioInicio == anioActual && anioFin == anioActual) {
                    montoMes += alquiler.getPrecioAlquiler();
                    montoHorasExtras += detalle.getPrecioHorasExtras();
                }
                // Si el período cruza dos meses
                else if ((mesInicio == mesTarget && anioInicio == anioActual) ||
                        (mesFin == mesTarget && anioFin == anioActual)) {
                    // Calcular proporción de días en el mes
                    double diasEnMes = calcularDiasEnMes(fechaInicio, fechaFin, mesTarget, anioActual);
                    double proporcion = diasEnMes / 30.0; // Mes comercial de 30 días

                    montoMes += alquiler.getPrecioAlquiler() * proporcion;
                    montoHorasExtras += detalle.getPrecioHorasExtras() * proporcion;
                }

            } catch (ParseException e) {
                Log.e("GrupoElectrogeno", "Error al parsear fechas", e);
            }
        }

        return new double[]{montoMes + montoHorasExtras, montoHorasExtras};
    }

    private int calcularDiasEnMes(Date fechaInicio, Date fechaFin, int mesTarget, int anioTarget) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(fechaInicio);

        int diasEnMes = 0;

        while (!cal.getTime().after(fechaFin)) {
            if (cal.get(Calendar.MONTH) == mesTarget && cal.get(Calendar.YEAR) == anioTarget) {
                diasEnMes++;
            }
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }

        return diasEnMes;
    }

    private void actualizarTotal(TextView textView, double total, String moneda) {
        String simbolo = moneda.equals("USD") ? "$" : "S/.";
        String totalFormateado = String.format(Locale.US, "%s %.2f", simbolo, total);
        textView.setText(totalFormateado);
    }
}