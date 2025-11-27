package com.example.maquirentapp.View;

import android.animation.ValueAnimator;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.maquirentapp.Model.AlquilerMensual;
import com.example.maquirentapp.Model.DetalleMes;
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
    private TextView tvTotalDeuda;
    private Spinner spinnerAnio;
    private MaterialButtonToggleGroup toggleMoneda;
    private PieChart chartOcupacion, chartComposicion;
    private BarChart chartEvolucion;
    private HorizontalBarChart chartTopClientes;

    private List<Ingreso> listaIngresos = new ArrayList<>();
    private List<PagoPendiente> listaPagosCalculados = new ArrayList<>();
    private List<PagoPendiente> listaPagosPendientes = new ArrayList<>();

    private String monedaActual = "SOL";
    private int anioSeleccionado;

    private boolean hasAnimatedOcupacion = false;
    private boolean hasAnimatedEvolucion = false;
    private boolean hasAnimatedComposicion = false;
    private boolean hasAnimatedTop = false;

    public DashboardFinanzasFragment() {}

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

        view.getViewTreeObserver().addOnScrollChangedListener(this::checkAnimateCharts);

        cargarDatosOperativos();
        cargarDatosFinancieros();
        calcularDeudaReal();
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
    private void calcularDeudaReal() {
        // 1. Traer alquileres activos
        db.collection("alquileresMensuales")
                .whereEqualTo("finalizado", false)
                .get()
                .addOnSuccessListener(alquileresSnap -> {
                    List<Task<QuerySnapshot>> tareasDetalles = new ArrayList<>();
                    List<AlquilerMensual> alquileresActivos = new ArrayList<>();

                    // 2. Para cada alquiler, pedir sus detallesMes
                    for (QueryDocumentSnapshot doc : alquileresSnap) {
                        AlquilerMensual alquiler = doc.toObject(AlquilerMensual.class);
                        alquiler.setId(doc.getId());
                        alquileresActivos.add(alquiler);

                        // Crear la tarea de consulta de detalles
                        Task<QuerySnapshot> tarea = db.collection("detallesMes")
                                .whereEqualTo("idAlquilerMensual", alquiler.getId())
                                .get();
                        tareasDetalles.add(tarea);
                    }

                    if (tareasDetalles.isEmpty()) {
                        actualizarTarjetaDeuda(); // 0 deuda
                        return;
                    }

                    // 3. Esperar a que lleguen TODOS los detalles
                    Tasks.whenAllSuccess(tareasDetalles).addOnSuccessListener(listaDeResultados -> {
                        listaPagosCalculados.clear();

                        // Recorrer resultados (uno por cada alquiler)
                        for (int i = 0; i < listaDeResultados.size(); i++) {
                            QuerySnapshot detallesSnap = (QuerySnapshot) listaDeResultados.get(i);
                            AlquilerMensual alquiler = alquileresActivos.get(i);

                            // Procesar los detalles de este alquiler
                            for (QueryDocumentSnapshot detDoc : detallesSnap) {
                                DetalleMes detalle = detDoc.toObject(DetalleMes.class);

                                boolean mesPendiente = !detalle.isPagoMesConfirmado();
                                boolean hePendiente = !detalle.isPagoHEConfirmado() && detalle.getPrecioHorasExtras() > 0;

                                if (mesPendiente || hePendiente) {
                                    PagoPendiente pago = new PagoPendiente();
                                    // Normalizar moneda del alquiler
                                    String monedaAlq = alquiler.getMoneda() != null ? alquiler.getMoneda() : "SOL";

                                    pago.setMoneda(monedaAlq);
                                    // Lógica del Home: Si es pendiente, sumamos el precio
                                    pago.setMontoPendienteMes(mesPendiente ? alquiler.getPrecioAlquiler() : 0);
                                    pago.setMontoPendienteHE(hePendiente ? detalle.getPrecioHorasExtras() : 0);

                                    listaPagosCalculados.add(pago);
                                }
                            }
                        }
                        // 4. Mostrar el total
                        actualizarTarjetaDeuda();
                    });

                }).addOnFailureListener(e -> Log.e("Dashboard", "Error deuda", e));
    }

    private void cargarIngresosHistoricos() {
        // Solo cargamos ingresos para las gráficas, ya no deuda (esa va aparte)
        db.collection("ingresosRegistrados")
                .whereEqualTo("anio", anioSeleccionado)
                .get()
                .addOnSuccessListener(snap -> {
                    listaIngresos.clear();
                    for (QueryDocumentSnapshot doc : snap) {
                        listaIngresos.add(doc.toObject(Ingreso.class));
                    }
                    procesarDatosVisuales();
                });
    }

    private void actualizarTarjetaDeuda() {
        double totalDeuda = 0;

        for (PagoPendiente pago : listaPagosCalculados) {
            if (esMonedaActual(pago.getMoneda())) {
                totalDeuda += (pago.getMontoPendienteMes() + pago.getMontoPendienteHE());
            }
        }

        ValueAnimator animator = ValueAnimator.ofFloat(0f, (float) totalDeuda);

        animator.setDuration(1200);

        animator.addUpdateListener(animation -> {
            float valorActual = (float) animation.getAnimatedValue();

            String simbolo = monedaActual.equals("SOL") ? "S/. " : "$ ";
            tvTotalDeuda.setText(simbolo + String.format(Locale.US, "%.2f", valorActual));
        });
        animator.start();
    }
    private void checkAnimateCharts() {
        if (getContext() == null) return;

        if (!hasAnimatedOcupacion && isChartVisible(chartOcupacion)) {
            chartOcupacion.animateXY(1000, 1000);
            chartOcupacion.animate().alpha(1f).setDuration(500).start();
            hasAnimatedOcupacion = true;
        }

        if (!hasAnimatedEvolucion && isChartVisible(chartEvolucion)) {
            chartEvolucion.animateY(1000);
            chartEvolucion.animate().alpha(1f).setDuration(500).start();
            hasAnimatedEvolucion = true;
        }

        if (!hasAnimatedComposicion && isChartVisible(chartComposicion)) {
            chartComposicion.animateY(1000);
            chartComposicion.animate().alpha(1f).setDuration(500).start();
            hasAnimatedComposicion = true;
        }

        if (!hasAnimatedTop && isChartVisible(chartTopClientes)) {
            chartTopClientes.animateY(1000);
            chartTopClientes.animate().alpha(1f).setDuration(500).start();
            hasAnimatedTop = true;
        }
    }

    private boolean isChartVisible(View view) {
        if (view == null || view.getVisibility() != View.VISIBLE) return false;

        if (view instanceof com.github.mikephil.charting.charts.Chart) {
            if (((com.github.mikephil.charting.charts.Chart) view).getData() == null) return false;
        }

        Rect globalVisibleRect = new Rect();
        boolean isVisible = view.getGlobalVisibleRect(globalVisibleRect);

        if (!isVisible) return false;

        int alturaTotal = view.getHeight();
        int alturaVisible = globalVisibleRect.height();

        if (alturaTotal <= 0) return false;

        float porcentajeVisible = (float) alturaVisible / alturaTotal;

        return porcentajeVisible >= 0.65f;
    }
    private void setupChartsStyling() {
        chartEvolucion.setAlpha(0f);
        chartTopClientes.setAlpha(0f);
        chartComposicion.setAlpha(0f);
        chartOcupacion.setAlpha(0f);

        chartEvolucion.getDescription().setEnabled(false);
        chartEvolucion.getLegend().setEnabled(false);
        chartEvolucion.setDrawGridBackground(false);
        chartEvolucion.getXAxis().setTextColor(Color.WHITE);
        chartEvolucion.getAxisLeft().setTextColor(Color.WHITE);
        chartEvolucion.getAxisRight().setTextColor(Color.WHITE);
        chartEvolucion.getLegend().setTextSize(12f);

        chartTopClientes.getDescription().setEnabled(false);
        chartTopClientes.getLegend().setEnabled(false);
        chartTopClientes.getXAxis().setTextColor(Color.WHITE);
        chartTopClientes.getAxisLeft().setTextColor(Color.WHITE);
        chartTopClientes.getAxisRight().setTextColor(Color.WHITE);
        chartTopClientes.getLegend().setTextSize(12f);

        chartComposicion.getDescription().setEnabled(false);
        chartComposicion.setHoleRadius(40f);
        chartComposicion.setTransparentCircleRadius(45f);
        chartComposicion.setCenterTextColor(Color.WHITE);
        chartComposicion.getLegend().setTextColor(Color.WHITE);
        chartComposicion.getLegend().setTextSize(12f);

        chartOcupacion.getDescription().setEnabled(false);
        chartOcupacion.setCenterText("Flota");
        chartOcupacion.setCenterTextColor(Color.WHITE);
        chartOcupacion.setCenterTextSize(12f);
        chartOcupacion.getLegend().setTextColor(Color.WHITE);
        chartOcupacion.getLegend().setTextSize(12f);
    }
    private void procesarDatosVisuales() {
        actualizarTarjetaDeuda();
        actualizarGraficoEvolucion();
        actualizarGraficoComposicion();
        actualizarGraficoTopClientes();

        checkAnimateCharts();
    }

    private boolean esMonedaActual(String monedaItem) {
        if (monedaItem == null) return false;
        if (monedaActual.equals("SOL")) {
            return monedaItem.equals("SOL") || monedaItem.equals("PEN") || monedaItem.equals("S/.");
        } else {
            return monedaItem.equals("USD") || monedaItem.equals("$") || monedaItem.equals("DOLARES");
        }
    }


    private void cargarDatosFinancieros() {
        Task<QuerySnapshot> taskIngresos = db.collection("ingresosRegistrados")
                .whereEqualTo("anio", anioSeleccionado)
                .get();

        Task<QuerySnapshot> taskDeuda = db.collection("pagosPendientes")
                .get();

        Tasks.whenAllSuccess(taskIngresos, taskDeuda).addOnSuccessListener(results -> {
            QuerySnapshot snapIngresos = (QuerySnapshot) results.get(0);
            listaIngresos.clear();
            for (QueryDocumentSnapshot doc : snapIngresos) {
                listaIngresos.add(doc.toObject(Ingreso.class));
            }

            QuerySnapshot snapDeuda = (QuerySnapshot) results.get(1);
            listaPagosPendientes.clear();
            for (QueryDocumentSnapshot doc : snapDeuda) {
                listaPagosPendientes.add(doc.toObject(PagoPendiente.class));
            }
            procesarDatosVisuales();

        }).addOnFailureListener(e -> {
            Log.e("Dashboard", "Error cargando finanzas", e);
            Toast.makeText(getContext(), "Error cargando datos", Toast.LENGTH_SHORT).show();
        });
    }

    private void cargarDatosOperativos() {
        Task<QuerySnapshot> taskGrupos = db.collection("gruposElectrogenos").whereEqualTo("eliminado", false).get();
        Task<QuerySnapshot> taskMensual = db.collection("alquileresMensuales").whereEqualTo("finalizado", false).get();
        Task<QuerySnapshot> taskDiario = db.collection("alquileresDiarios").whereEqualTo("finalizado", false).get();

        Tasks.whenAllSuccess(taskGrupos, taskMensual, taskDiario).addOnSuccessListener(results -> {
            QuerySnapshot snapGrupos = (QuerySnapshot) results.get(0);
            QuerySnapshot snapMensual = (QuerySnapshot) results.get(1);
            QuerySnapshot snapDiario = (QuerySnapshot) results.get(2);

            int totalMaquinas = snapGrupos.size();

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
                cargarDatosFinancieros();
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

                procesarDatosVisuales();
            }
        });
    }
    private void actualizarGraficoEvolucion() {
        float[] ingresosPorMes = new float[12];
        for (Ingreso ing : listaIngresos) {
            if (esMonedaActual(ing.getMoneda())) {
                int indice = ing.getMes() - 1;
                if (indice >= 0 && indice < 12) {
                    ingresosPorMes[indice] += (float) ing.getMonto();
                }
            }
        }

        List<BarEntry> entries = new ArrayList<>();
        for (int i = 0; i < 12; i++) entries.add(new BarEntry(i, ingresosPorMes[i]));

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
            if (entry.getValue() > 0) entries.add(new PieEntry(entry.getValue(), entry.getKey()));
        }

        if (entries.isEmpty()) {
            chartComposicion.clear();
            return;
        }

        PieDataSet set = new PieDataSet(entries, "");
        set.setColors(ColorTemplate.MATERIAL_COLORS);
        set.setValueTextColor(Color.WHITE);
        set.setValueTextSize(12f);

        PieData data = new PieData(set);
        chartComposicion.setData(data);
        chartComposicion.setEntryLabelColor(Color.WHITE);
        chartComposicion.setEntryLabelColor(Color.BLACK);

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

        List<Map.Entry<String, Double>> list = new ArrayList<>(ventasCliente.entrySet());
        list.sort(Map.Entry.comparingByValue(Collections.reverseOrder()));

        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        int count = 0;
        for (int i = Math.min(list.size(), 5) - 1; i >= 0; i--) {
            entries.add(new BarEntry(count, list.get(i).getValue().floatValue()));
            labels.add(list.get(i).getKey());
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

        chartTopClientes.invalidate();
    }

    private void actualizarGraficoOcupacion(int ocupadas, int disponibles) {
        List<PieEntry> entries = new ArrayList<>();
        if (ocupadas > 0) entries.add(new PieEntry(ocupadas, "Alquilado"));
        if (disponibles > 0) entries.add(new PieEntry(disponibles, "Disponible"));

        PieDataSet set = new PieDataSet(entries, "");
        set.setColors(new int[]{
                ContextCompat.getColor(requireContext(), R.color.selection_indicator),
                Color.parseColor("#424242")
        });
        set.setValueTextColor(Color.WHITE);
        set.setValueTextSize(16f);

        PieData data = new PieData(set);

        data.setValueFormatter(new com.github.mikephil.charting.formatter.ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.valueOf((int) value);
            }
        });

        chartOcupacion.setData(data);
        chartOcupacion.setEntryLabelColor(Color.WHITE);
        chartOcupacion.setCenterTextSize(10f);
        chartOcupacion.setHoleColor(R.color.black);
        chartOcupacion.setCenterText(ocupadas + " / " + (ocupadas + disponibles));

        chartOcupacion.invalidate();

        checkAnimateCharts();
    }
}