package com.example.maquirentapp.View;

import android.graphics.Color;
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
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.maquirentapp.Model.Ingreso;
import com.example.maquirentapp.Model.PagoPendiente;
import com.example.maquirentapp.R;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.HorizontalBarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class DashboardFinanzasFragment extends Fragment {

    private FirebaseFirestore db;

    // Vistas
    private TextView tvTotalDeuda;
    private Spinner spinnerAnio;
    private MaterialButtonToggleGroup toggleMoneda;
    private PieChart chartOcupacion, chartComposicion;
    private BarChart chartEvolucion;
    private HorizontalBarChart chartTopClientes;

    // Datos en Memoria (Raw Data)
    private List<Ingreso> listaIngresos = new ArrayList<>();
    private List<PagoPendiente> listaPagosPendientes = new ArrayList<>();

    // Estado actual
    private String monedaActual = "SOL"; // "SOL" o "USD"
    private int anioSeleccionado;

    public DashboardFinanzasFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dashboard_finanzas, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = FirebaseFirestore.getInstance();

        initViews(view);
        setupChartsStyling();
        setupSpinner();
        setupCurrencyToggle();

        // Cargar datos operativos (Ocupación) - Se carga una vez
        cargarDatosOperativos();
    }

    private void initViews(View view) {
        tvTotalDeuda = view.findViewById(R.id.tvTotalDeuda);
        spinnerAnio = view.findViewById(R.id.spinnerAnio);
        toggleMoneda = view.findViewById(R.id.toggleMoneda);

        chartOcupacion = view.findViewById(R.id.chartOcupacion);
        chartEvolucion = view.findViewById(R.id.chartEvolucion);
        chartComposicion = view.findViewById(R.id.chartComposicion);
        chartTopClientes = view.findViewById(R.id.chartTopClientes);
    }

    private void setupChartsStyling() {
        // Estilo base para que se vean limpios
        chartEvolucion.getDescription().setEnabled(false);
        chartEvolucion.getLegend().setEnabled(false);
        chartEvolucion.setDrawGridBackground(false);
        chartEvolucion.getXAxis().setTextColor(Color.WHITE);
        chartEvolucion.getAxisLeft().setTextColor(Color.WHITE);
        chartEvolucion.getAxisRight().setTextColor(Color.WHITE);

        chartTopClientes.getDescription().setEnabled(false);
        chartTopClientes.getLegend().setEnabled(false);
        chartTopClientes.getXAxis().setTextColor(Color.WHITE);
        chartTopClientes.getAxisLeft().setTextColor(Color.WHITE);
        chartTopClientes.getAxisRight().setTextColor(Color.WHITE);

        chartComposicion.getDescription().setEnabled(false);
        chartComposicion.setHoleRadius(40f);
        chartComposicion.setTransparentCircleRadius(45f);
        chartComposicion.setCenterTextColor(Color.WHITE);
        chartComposicion.getLegend().setTextColor(Color.WHITE);

        chartOcupacion.getDescription().setEnabled(false);
        chartOcupacion.setCenterText("Flota");
        chartOcupacion.setCenterTextColor(Color.WHITE);
        chartOcupacion.setCenterTextSize(12f);
        chartOcupacion.getLegend().setTextColor(Color.WHITE);


    }

    private void setupSpinner() {
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        anioSeleccionado = currentYear;

        List<String> years = new ArrayList<>();
        for (int i = 0; i < 5; i++) years.add(String.valueOf(currentYear - i));

        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, years);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerAnio.setAdapter(adapter);

        spinnerAnio.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                anioSeleccionado = Integer.parseInt(years.get(position));
                cargarDatosFinancieros(); // Recargar al cambiar año
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void setupCurrencyToggle() {
        toggleMoneda.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.btnSoles) monedaActual = "SOL";
                else monedaActual = "USD";

                // Recalcular visuales sin volver a descargar de internet
                procesarDatosVisuales();
            }
        });
    }

    // =========================================================
    // CARGA DE DATOS (FIRESTORE)
    // =========================================================

    private void cargarDatosFinancieros() {
        // Usamos Tasks.whenAllSuccess para traer Ingresos y Deudas en paralelo
        Task<QuerySnapshot> taskIngresos = db.collection("ingresosRegistrados")
                .whereEqualTo("anio", anioSeleccionado)
                .get();

        Task<QuerySnapshot> taskDeuda = db.collection("pagosPendientes")
                .get(); // Traemos toda la deuda pendiente (no suele filtrarse por año)

        Tasks.whenAllSuccess(taskIngresos, taskDeuda).addOnSuccessListener(results -> {
            // 1. Procesar Ingresos
            QuerySnapshot snapIngresos = (QuerySnapshot) results.get(0);
            listaIngresos.clear();
            for (QueryDocumentSnapshot doc : snapIngresos) {
                listaIngresos.add(doc.toObject(Ingreso.class));
            }

            // 2. Procesar Deudas
            QuerySnapshot snapDeuda = (QuerySnapshot) results.get(1);
            listaPagosPendientes.clear();
            for (QueryDocumentSnapshot doc : snapDeuda) {
                listaPagosPendientes.add(doc.toObject(PagoPendiente.class));
            }

            // 3. Actualizar UI
            procesarDatosVisuales();

        }).addOnFailureListener(e -> {
            Log.e("Dashboard", "Error cargando finanzas", e);
            Toast.makeText(getContext(), "Error cargando datos", Toast.LENGTH_SHORT).show();
        });
    }

    private void cargarDatosOperativos() {
        // Consultar total de grupos y alquileres activos para Tasa de Ocupación
        Task<QuerySnapshot> taskGrupos = db.collection("gruposElectrogenos").whereEqualTo("eliminado", false).get();
        Task<QuerySnapshot> taskMensual = db.collection("alquileresMensuales").whereEqualTo("finalizado", false).get();
        Task<QuerySnapshot> taskDiario = db.collection("alquileresDiarios").whereEqualTo("finalizado", false).get();

        Tasks.whenAllSuccess(taskGrupos, taskMensual, taskDiario).addOnSuccessListener(results -> {
            QuerySnapshot snapGrupos = (QuerySnapshot) results.get(0);
            QuerySnapshot snapMensual = (QuerySnapshot) results.get(1);
            QuerySnapshot snapDiario = (QuerySnapshot) results.get(2);

            int totalMaquinas = snapGrupos.size();

            // Usamos un Set para contar máquinas únicas alquiladas (por si hay error de datos)
            Set<String> maquinasOcupadas = new HashSet<>();

            for (QueryDocumentSnapshot doc : snapMensual) {
                String idGrupo = doc.getString("idGrupo");
                if (idGrupo != null) maquinasOcupadas.add(idGrupo);
            }
            for (QueryDocumentSnapshot doc : snapDiario) {
                String idGrupo = doc.getString("idGrupo");
                if (idGrupo != null) maquinasOcupadas.add(idGrupo);
            }

            int ocupadas = maquinasOcupadas.size();
            int disponibles = Math.max(0, totalMaquinas - ocupadas);

            actualizarGraficoOcupacion(ocupadas, disponibles);
        });
    }

    // =========================================================
    // PROCESAMIENTO Y GRÁFICAS
    // =========================================================

    private void procesarDatosVisuales() {
        actualizarTarjetaDeuda();
        actualizarGraficoEvolucion();
        actualizarGraficoComposicion();
        actualizarGraficoTopClientes();
    }

    private boolean esMonedaActual(String monedaItem) {
        if (monedaItem == null) return false;
        if (monedaActual.equals("SOL")) {
            return monedaItem.equals("SOL") || monedaItem.equals("PEN") || monedaItem.equals("S/");
        } else {
            return monedaItem.equals("USD") || monedaItem.equals("$") || monedaItem.equals("DOLARES");
        }
    }

    private void actualizarTarjetaDeuda() {
        double totalDeuda = 0;
        for (PagoPendiente pago : listaPagosPendientes) {
            if (esMonedaActual(pago.getMoneda())) {
                totalDeuda += (pago.getMontoPendienteMes() + pago.getMontoPendienteHE());
            }
        }
        String simbolo = monedaActual.equals("SOL") ? "S/ " : "$ ";
        tvTotalDeuda.setText(simbolo + String.format(Locale.US, "%.2f", totalDeuda));
    }

    private void actualizarGraficoEvolucion() {
        float[] ingresosPorMes = new float[12]; // Ene-Dic

        for (Ingreso ing : listaIngresos) {
            if (esMonedaActual(ing.getMoneda())) {
                // ing.getMes() devuelve 1-12, restamos 1 para índice 0-11
                int indice = ing.getMes() - 1;
                if (indice >= 0 && indice < 12) {
                    ingresosPorMes[indice] += (float) ing.getMonto();
                }
            }
        }

        List<BarEntry> entries = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            entries.add(new BarEntry(i, ingresosPorMes[i]));
        }

        BarDataSet set = new BarDataSet(entries, "Ingresos");
        set.setColor(ContextCompat.getColor(requireContext(), R.color.selection_indicator));
        set.setValueTextColor(Color.WHITE);
        set.setValueTextSize(10f);

        BarData data = new BarData(set);
        data.setBarWidth(0.8f);

        chartEvolucion.setData(data);

        String[] meses = {"Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic"};
        XAxis xAxis = chartEvolucion.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(meses));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);

        chartEvolucion.animateY(1000);
        chartEvolucion.invalidate();
    }

    private void actualizarGraficoComposicion() {
        Map<String, Float> mapTipos = new HashMap<>();

        for (Ingreso ing : listaIngresos) {
            if (esMonedaActual(ing.getMoneda())) {
                String tipo = ing.getTipo() != null ? ing.getTipo() : "Otros";
                mapTipos.put(tipo, mapTipos.getOrDefault(tipo, 0f) + (float) ing.getMonto());
            }
        }

        List<PieEntry> entries = new ArrayList<>();
        for (Map.Entry<String, Float> entry : mapTipos.entrySet()) {
            if (entry.getValue() > 0) { // Solo mostrar si hay valor
                entries.add(new PieEntry(entry.getValue(), entry.getKey()));
            }
        }

        if (entries.isEmpty()) {
            chartComposicion.clear(); // Limpiar si no hay datos
            return;
        }

        PieDataSet set = new PieDataSet(entries, "");
        set.setColors(ColorTemplate.MATERIAL_COLORS);
        set.setValueTextColor(Color.WHITE);
        set.setValueTextSize(12f);
        set.setValueTextColor(Color.WHITE);

        PieData data = new PieData(set);
        chartComposicion.setData(data);
        chartComposicion.setEntryLabelColor(Color.WHITE);
        chartComposicion.setEntryLabelColor(Color.BLACK);
        chartComposicion.animateY(1000);
        chartComposicion.invalidate();
    }

    private void actualizarGraficoTopClientes() {
        Map<String, Double> ventasCliente = new HashMap<>();

        for (Ingreso ing : listaIngresos) {
            if (esMonedaActual(ing.getMoneda())) {
                String cliente = ing.getNombreCliente() != null ? ing.getNombreCliente() : "Desconocido";
                ventasCliente.put(cliente, ventasCliente.getOrDefault(cliente, 0.0) + ing.getMonto());
            }
        }

        // Ordenar
        List<Map.Entry<String, Double>> list = new ArrayList<>(ventasCliente.entrySet());
        list.sort(Map.Entry.comparingByValue(Collections.reverseOrder())); // Mayor a menor

        // Top 5
        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        int count = 0;

        // Horizontal chart dibuja de abajo hacia arriba, invertimos para que el #1 esté arriba
        for (int i = Math.min(list.size(), 5) - 1; i >= 0; i--) {
            Map.Entry<String, Double> entry = list.get(i);
            entries.add(new BarEntry(count, entry.getValue().floatValue()));
            labels.add(entry.getKey());
            count++;
        }

        BarDataSet set = new BarDataSet(entries, "Top Clientes");
        set.setColors(ColorTemplate.VORDIPLOM_COLORS);
        set.setValueTextColor(Color.WHITE);
        set.setValueTextSize(10f);

        BarData data = new BarData(set);
        data.setBarWidth(0.6f);

        chartTopClientes.setData(data);
        chartTopClientes.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        chartTopClientes.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        chartTopClientes.getXAxis().setGranularity(1f);
        chartTopClientes.animateY(1000);
        chartTopClientes.invalidate();
    }

    private void actualizarGraficoOcupacion(int ocupadas, int disponibles) {
        List<PieEntry> entries = new ArrayList<>();
        // Solo agregamos si son mayor a 0 para evitar crash visual
        if (ocupadas > 0) entries.add(new PieEntry(ocupadas, "Alquilado"));
        if (disponibles > 0) entries.add(new PieEntry(disponibles, "Disponible"));

        PieDataSet set = new PieDataSet(entries, "");
        // Verde para Ocupado, Gris para Disponible
        set.setColors(new int[]{
                ContextCompat.getColor(requireContext(), R.color.green_accent),
                Color.LTGRAY
        });
        set.setValueTextColor(Color.WHITE);
        set.setValueTextSize(14f);

        PieData data = new PieData(set);
        chartOcupacion.setData(data);
        chartOcupacion.setEntryLabelColor(Color.WHITE);
        chartOcupacion.setCenterText(ocupadas + " / " + (ocupadas + disponibles));
        chartOcupacion.animateXY(1000, 1000);
        chartOcupacion.invalidate();
    }
}