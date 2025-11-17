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
import com.example.maquirentapp.Model.Ingreso;
import com.example.maquirentapp.Network.FirebaseServicio;
import com.example.maquirentapp.R;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

public class GrupoElectrogenoFragment extends Fragment {
    private String codigo;
    private String idGrupo;
    private FirebaseServicio firebaseServicio;

    private Spinner spinnerMeses;
    private TextView tvTotalSOL, tvTotalUSD;
    private int mesSeleccionado = Calendar.getInstance().get(Calendar.MONTH); // Mes actual (0-11)
    private int anioActual = Calendar.getInstance().get(Calendar.YEAR);
    private double acumuladoSOL = 0;
    private double acumuladoUSD = 0;
    private boolean isSpinnerInitialLoad = true;
    private int idCargaActual = 0;
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

        isSpinnerInitialLoad = true;
        idCargaActual = 0;

        // Inicializar vistas
        spinnerMeses = view.findViewById(R.id.spinnerMeses);
        tvTotalSOL = view.findViewById(R.id.tvTotalSOL);
        tvTotalUSD = view.findViewById(R.id.tvTotalUSD);

        configurarSpinnerMeses();

        CardView cardMantenimientos = view.findViewById(R.id.cardMantenimientos);
        CardView cardHistorialAlquilerMensual = view.findViewById(R.id.cardHistorialAlquilerMensual);
        CardView cardHistorialAlquilerDiario = view.findViewById(R.id.cardHistorialAlquilerDiario);

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

        cardHistorialAlquilerDiario.setOnClickListener(v -> {
            Bundle args = new Bundle();
            if (idGrupoLocal != null) args.putString("idGrupo", idGrupoLocal);
            Navigation.findNavController(view)
                    .navigate(R.id.action_grupoElectrogeno_to_historialAlquilerDiario, args);
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
                if (isSpinnerInitialLoad) {
                    isSpinnerInitialLoad = false;
                    return;
                }
                mesSeleccionado = position;
                cargarTotalesMes(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }
    private void cargarTotalesMes(int mes) {
        if (idGrupo == null) return;
        idCargaActual++;
        final int idDeEstaCarga = idCargaActual;

        acumuladoSOL = 0;
        acumuladoUSD = 0;
        actualizarTotal(tvTotalSOL, 0, "SOL");
        actualizarTotal(tvTotalUSD, 0, "USD");

        firebaseServicio.getIngresosPorGrupoYMes(idGrupo, mes, anioActual,
                new FirebaseServicio.OnIngresosLoadedListener() {
                    @Override
                    public void onSuccess(List<Ingreso> ingresos) {
                        if (idDeEstaCarga != idCargaActual) {
                            Log.d("GrupoElectrogeno", "Respuesta antigua ignorada (ID: " + idDeEstaCarga + ")");
                            return;
                        }
                        for (Ingreso ingreso : ingresos) {
                            if (ingreso.getMoneda() != null && ingreso.getMoneda().equals("USD")) {
                                acumuladoUSD += ingreso.getMonto();
                            } else {
                                acumuladoSOL += ingreso.getMonto();
                            }
                        }

                        actualizarTotal(tvTotalSOL, acumuladoSOL, "SOL");
                        actualizarTotal(tvTotalUSD, acumuladoUSD, "USD");
                    }

                    @Override
                    public void onError(Exception e) {
                        if (idDeEstaCarga != idCargaActual) {
                            return;
                        }
                        Toast.makeText(getContext(), "Error al cargar ingresos: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });

        // (Aquí iría la llamada a Alquileres Diarios, que también sumarían a 'acumuladoSOL/USD')
    }
    private void actualizarTotal(TextView textView, double total, String moneda) {
        String simbolo = moneda.equals("USD") ? "$" : "S/.";
        String totalFormateado = String.format(Locale.US, "%s %.2f", simbolo, total);
        textView.setText(totalFormateado);
    }
}