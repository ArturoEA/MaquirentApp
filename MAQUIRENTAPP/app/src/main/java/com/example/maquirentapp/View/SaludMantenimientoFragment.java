package com.example.maquirentapp.View;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.maquirentapp.R;
import com.example.maquirentapp.Repository.Callback;
import com.example.maquirentapp.Utils.MantenimientoSaludManager;
import com.example.maquirentapp.Utils.MantenimientoSaludManager.EstadoSalud;
import com.example.maquirentapp.Utils.MantenimientoSaludManager.EstadoSaludTarea;
import com.example.maquirentapp.Utils.MantenimientoSaludManager.PuntoHistorialIntervalo;
import com.example.maquirentapp.Utils.MantenimientoSaludManager.ReporteSaludEquipo;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.material.button.MaterialButton;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SaludMantenimientoFragment extends Fragment {

    private enum FiltroEstado {
        TODAS,
        VENCIDAS,
        PROXIMAS,
        AL_DIA
    }

    private String codigo = "";
    private ReporteSaludEquipo ultimoReporte = null;
    private FiltroEstado filtroActual = FiltroEstado.TODAS;

    private ShimmerFrameLayout shimmerSalud;
    private LinearLayout layoutContenidoSalud;

    // Hero Card
    private TextView tvTituloEquipoSalud;
    private TextView tvBadgeGeneralSalud;
    private TextView tvHorometroActualGrande;
    private TextView tvIndiceSaludPorcentaje;
    private TextView tvTotalAlDia;
    private TextView tvTotalProximas;
    private TextView tvTotalVencidas;
    private LinearLayout btnFiltroAlDiaBox;
    private LinearLayout btnFiltroProximasBox;
    private LinearLayout btnFiltroVencidasBox;

    // Disciplina Card
    private TextView tvValorPuntualidad;
    private TextView tvValorIntervaloPromedio;
    private TextView tvDiagnosticoDisciplina;
    private LinearLayout layoutBarrasGrafico;

    // Chips de Filtro
    private TextView chipFiltroTodas;
    private TextView chipFiltroVencidas;
    private TextView chipFiltroProximas;
    private TextView chipFiltroAlDia;

    // Tareas
    private LinearLayout layoutSubsistemasTareas;
    private MaterialButton btnNuevoMantenimientoDesdeSalud;

    private Typeface fontAnta;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            codigo = getArguments().getString("codigo", "");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_salud_mantenimiento, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        try {
            fontAnta = ResourcesCompat.getFont(requireContext(), R.font.anta_font);
        } catch (Exception ignored) {}

        inicializarVistas(view);
        configurarListeners();
        cargarDatosSalud();
    }

    private void inicializarVistas(View view) {
        shimmerSalud = view.findViewById(R.id.shimmerSalud);
        layoutContenidoSalud = view.findViewById(R.id.layoutContenidoSalud);

        tvTituloEquipoSalud = view.findViewById(R.id.tvTituloEquipoSalud);
        tvBadgeGeneralSalud = view.findViewById(R.id.tvBadgeGeneralSalud);
        tvHorometroActualGrande = view.findViewById(R.id.tvHorometroActualGrande);
        tvIndiceSaludPorcentaje = view.findViewById(R.id.tvIndiceSaludPorcentaje);

        tvTotalAlDia = view.findViewById(R.id.tvTotalAlDia);
        tvTotalProximas = view.findViewById(R.id.tvTotalProximas);
        tvTotalVencidas = view.findViewById(R.id.tvTotalVencidas);

        btnFiltroAlDiaBox = view.findViewById(R.id.btnFiltroAlDiaBox);
        btnFiltroProximasBox = view.findViewById(R.id.btnFiltroProximasBox);
        btnFiltroVencidasBox = view.findViewById(R.id.btnFiltroVencidasBox);

        tvValorPuntualidad = view.findViewById(R.id.tvValorPuntualidad);
        tvValorIntervaloPromedio = view.findViewById(R.id.tvValorIntervaloPromedio);
        tvDiagnosticoDisciplina = view.findViewById(R.id.tvDiagnosticoDisciplina);
        layoutBarrasGrafico = view.findViewById(R.id.layoutBarrasGrafico);

        chipFiltroTodas = view.findViewById(R.id.chipFiltroTodas);
        chipFiltroVencidas = view.findViewById(R.id.chipFiltroVencidas);
        chipFiltroProximas = view.findViewById(R.id.chipFiltroProximas);
        chipFiltroAlDia = view.findViewById(R.id.chipFiltroAlDia);

        layoutSubsistemasTareas = view.findViewById(R.id.layoutSubsistemasTareas);
        btnNuevoMantenimientoDesdeSalud = view.findViewById(R.id.btnNuevoMantenimientoDesdeSalud);
    }

    private void configurarListeners() {
        chipFiltroTodas.setOnClickListener(v -> cambiarFiltro(FiltroEstado.TODAS));
        chipFiltroVencidas.setOnClickListener(v -> cambiarFiltro(FiltroEstado.VENCIDAS));
        chipFiltroProximas.setOnClickListener(v -> cambiarFiltro(FiltroEstado.PROXIMAS));
        chipFiltroAlDia.setOnClickListener(v -> cambiarFiltro(FiltroEstado.AL_DIA));

        btnFiltroAlDiaBox.setOnClickListener(v -> cambiarFiltro(FiltroEstado.AL_DIA));
        btnFiltroProximasBox.setOnClickListener(v -> cambiarFiltro(FiltroEstado.PROXIMAS));
        btnFiltroVencidasBox.setOnClickListener(v -> cambiarFiltro(FiltroEstado.VENCIDAS));

        btnNuevoMantenimientoDesdeSalud.setOnClickListener(v -> {
            if (codigo != null && !codigo.isEmpty()) {
                Bundle args = new Bundle();
                args.putString("codigo", codigo);
                try {
                    Navigation.findNavController(v).navigate(R.id.action_saludMantenimiento_to_nuevoMantenimiento, args);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void cambiarFiltro(FiltroEstado nuevoFiltro) {
        filtroActual = nuevoFiltro;
        actualizarEstilosChips();
        renderizarTareas();
    }

    private void actualizarEstilosChips() {
        aplicarEstiloChip(chipFiltroTodas, filtroActual == FiltroEstado.TODAS);
        aplicarEstiloChip(chipFiltroVencidas, filtroActual == FiltroEstado.VENCIDAS);
        aplicarEstiloChip(chipFiltroProximas, filtroActual == FiltroEstado.PROXIMAS);
        aplicarEstiloChip(chipFiltroAlDia, filtroActual == FiltroEstado.AL_DIA);
    }

    private void aplicarEstiloChip(TextView chip, boolean activo) {
        if (chip == null) return;
        if (activo) {
            chip.setBackgroundResource(R.drawable.bg_chip_anio_selected);
            chip.setTextColor(Color.WHITE);
            chip.setTypeface(fontAnta, Typeface.BOLD);
        } else {
            chip.setBackgroundResource(R.drawable.bg_chip_anio_unselected);
            chip.setTextColor(Color.parseColor("#B0BEC5"));
            chip.setTypeface(fontAnta, Typeface.NORMAL);
        }
    }

    private void cargarDatosSalud() {
        if (shimmerSalud != null) {
            shimmerSalud.setVisibility(View.VISIBLE);
            shimmerSalud.startShimmer();
        }
        if (layoutContenidoSalud != null) {
            layoutContenidoSalud.setVisibility(View.GONE);
        }

        MantenimientoSaludManager.generarReporteSaludCompleto(codigo, getContext(), new Callback<ReporteSaludEquipo>() {
            @Override
            public void onSuccess(ReporteSaludEquipo reporte) {
                if (!isAdded() || getContext() == null) return;
                ultimoReporte = reporte;

                if (shimmerSalud != null) {
                    shimmerSalud.stopShimmer();
                    shimmerSalud.setVisibility(View.GONE);
                }
                if (layoutContenidoSalud != null) {
                    layoutContenidoSalud.setVisibility(View.VISIBLE);
                }

                poblarHeroCard(reporte);
                poblarDisciplinaCard(reporte);
                actualizarEstilosChips();
                renderizarTareas();
            }

            @Override
            public void onError(Exception e) {
                if (!isAdded() || getContext() == null) return;
                if (shimmerSalud != null) {
                    shimmerSalud.stopShimmer();
                    shimmerSalud.setVisibility(View.GONE);
                }
                Toast.makeText(getContext(), "Error al cargar reporte de salud: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void poblarHeroCard(ReporteSaludEquipo rep) {
        if (rep == null) return;

        tvTituloEquipoSalud.setText(rep.codigoGrupo + " - " + rep.tipoEquipo);
        tvBadgeGeneralSalud.setText(rep.badgeGeneral);

        try {
            int colorBadge = Color.parseColor(rep.colorGeneralHex);
            tvBadgeGeneralSalud.setBackgroundTintList(ColorStateList.valueOf(colorBadge));
        } catch (Exception ignored) {}

        tvHorometroActualGrande.setText(String.format(Locale.US, "%,.1f h", rep.horometroActual));
        tvIndiceSaludPorcentaje.setText(rep.indiceSaludPorcentaje + "%");

        if (rep.indiceSaludPorcentaje >= 85) {
            tvIndiceSaludPorcentaje.setTextColor(Color.parseColor("#4CAF50"));
        } else if (rep.indiceSaludPorcentaje >= 60) {
            tvIndiceSaludPorcentaje.setTextColor(Color.parseColor("#FFC107"));
        } else {
            tvIndiceSaludPorcentaje.setTextColor(Color.parseColor("#F44336"));
        }

        tvTotalAlDia.setText(String.valueOf(rep.tareasAlDia));
        tvTotalProximas.setText(String.valueOf(rep.tareasProximas));
        tvTotalVencidas.setText(String.valueOf(rep.tareasVencidas));
    }

    private void poblarDisciplinaCard(ReporteSaludEquipo rep) {
        if (rep == null || rep.metricasPuntualidad == null) return;
        MantenimientoSaludManager.MetricasPuntualidad m = rep.metricasPuntualidad;

        tvValorPuntualidad.setText(String.format(Locale.US, "%.0f%%", m.indicePuntualidad));
        if (m.indicePuntualidad >= 85) {
            tvValorPuntualidad.setTextColor(Color.parseColor("#4CAF50"));
        } else if (m.indicePuntualidad >= 60) {
            tvValorPuntualidad.setTextColor(Color.parseColor("#FFC107"));
        } else {
            tvValorPuntualidad.setTextColor(Color.parseColor("#F44336"));
        }

        tvValorIntervaloPromedio.setText(String.format(Locale.US, "%.1f h", m.intervaloPromedio));
        tvDiagnosticoDisciplina.setText(m.diagnostico);

        layoutBarrasGrafico.removeAllViews();
        if (m.intervalos == null || m.intervalos.isEmpty()) {
            TextView tvVacio = new TextView(getContext());
            tvVacio.setText("Aún no hay suficientes mantenimientos para calcular intervalos.");
            tvVacio.setTextColor(Color.parseColor("#90A4AE"));
            tvVacio.setTextSize(12);
            if (fontAnta != null) tvVacio.setTypeface(fontAnta);
            layoutBarrasGrafico.addView(tvVacio);
        } else {
            for (int i = 0; i < m.intervalos.size(); i++) {
                PuntoHistorialIntervalo p = m.intervalos.get(i);
                View vistaFila = crearFilaGraficoIntervalo(i + 1, p);
                layoutBarrasGrafico.addView(vistaFila);
            }
        }
    }

    private View crearFilaGraficoIntervalo(int numeroServicio, PuntoHistorialIntervalo p) {
        LinearLayout container = new LinearLayout(getContext());
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(0, 10, 0, 10);

        // Header fila: Servicio # y Badge
        LinearLayout headerRow = new LinearLayout(getContext());
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView tvTitulo = new TextView(getContext());
        tvTitulo.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
        tvTitulo.setText(String.format(Locale.US, "Servicio #%d (Horómetro %.1f h)", numeroServicio, p.horometro));
        tvTitulo.setTextColor(Color.WHITE);
        tvTitulo.setTextSize(12);
        if (fontAnta != null) tvTitulo.setTypeface(fontAnta, Typeface.BOLD);

        TextView tvBadge = new TextView(getContext());
        String estadoTexto = p.esATiempo ? "A tiempo" : String.format(Locale.US, "+%.0f h exceso", p.deltaHoras - 250);
        tvBadge.setText(estadoTexto);
        tvBadge.setTextSize(11);
        tvBadge.setPadding(16, 4, 16, 4);
        tvBadge.setBackgroundResource(R.drawable.dialog_rounded_white_background);
        int colorBadge = Color.parseColor(p.esATiempo ? "#2E7D32" : "#C62828");
        tvBadge.setBackgroundTintList(ColorStateList.valueOf(colorBadge));
        tvBadge.setTextColor(Color.WHITE);
        if (fontAnta != null) tvBadge.setTypeface(fontAnta, Typeface.BOLD);

        headerRow.addView(tvTitulo);
        headerRow.addView(tvBadge);

        // Subtítulo con fecha y delta
        TextView tvSub = new TextView(getContext());
        tvSub.setText(String.format(Locale.US, "Fecha: %s | Intervalo: %.1f h (Meta: 250.0 h)", p.fecha, p.deltaHoras));
        tvSub.setTextColor(Color.parseColor("#B0BEC5"));
        tvSub.setTextSize(11);
        tvSub.setPadding(0, 4, 0, 6);
        if (fontAnta != null) tvSub.setTypeface(fontAnta);

        // Barra de progreso estilizada
        ProgressBar pb = new ProgressBar(getContext(), null, android.R.attr.progressBarStyleHorizontal);
        pb.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 16));
        pb.setMax(350);
        pb.setProgress((int) Math.min(350, p.deltaHoras));
        pb.setProgressDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.bg_progress_salud, null));
        int colorBarra = Color.parseColor(p.esATiempo ? "#4CAF50" : "#F44336");
        pb.setProgressTintList(ColorStateList.valueOf(colorBarra));

        container.addView(headerRow);
        container.addView(tvSub);
        container.addView(pb);

        return container;
    }

    private void renderizarTareas() {
        layoutSubsistemasTareas.removeAllViews();
        if (ultimoReporte == null || ultimoReporte.tareasPorSubsistema == null) return;

        LayoutInflater inflater = LayoutInflater.from(requireContext());
        int totalTareasMostradas = 0;

        for (Map.Entry<String, List<EstadoSaludTarea>> entry : ultimoReporte.tareasPorSubsistema.entrySet()) {
            String subsistemaNombre = entry.getKey();
            List<EstadoSaludTarea> lista = entry.getValue();

            // Filtrar tareas que coincidan con el filtro actual
            java.util.ArrayList<EstadoSaludTarea> listaFiltrada = new java.util.ArrayList<>();
            for (EstadoSaludTarea t : lista) {
                if (coincideConFiltro(t)) {
                    listaFiltrada.add(t);
                }
            }

            if (listaFiltrada.isEmpty()) continue;

            totalTareasMostradas += listaFiltrada.size();

            // 1. Cabecera del subsistema
            View headerSubsistema = crearHeaderSubsistema(subsistemaNombre, listaFiltrada.size());
            layoutSubsistemasTareas.addView(headerSubsistema);

            // 2. Tarjetas de cada tarea
            for (EstadoSaludTarea tarea : listaFiltrada) {
                View cardTarea = inflarCardTarea(inflater, layoutSubsistemasTareas, tarea);
                layoutSubsistemasTareas.addView(cardTarea);
            }
        }

        if (totalTareasMostradas == 0) {
            TextView tvVacio = new TextView(requireContext());
            tvVacio.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
            tvVacio.setGravity(Gravity.CENTER);
            tvVacio.setPadding(24, 40, 24, 40);
            tvVacio.setText("No hay tareas para mostrar en la categoría seleccionada.");
            tvVacio.setTextColor(Color.parseColor("#B0BEC5"));
            tvVacio.setTextSize(13);
            if (fontAnta != null) tvVacio.setTypeface(fontAnta);
            layoutSubsistemasTareas.addView(tvVacio);
        }
    }

    private boolean coincideConFiltro(EstadoSaludTarea t) {
        if (filtroActual == FiltroEstado.TODAS) return true;
        if (filtroActual == FiltroEstado.VENCIDAS) return t.isVencido();
        if (filtroActual == FiltroEstado.PROXIMAS) return t.estado == EstadoSalud.PROXIMO;
        if (filtroActual == FiltroEstado.AL_DIA) return t.estado == EstadoSalud.OPTIMO;
        return true;
    }

    private View crearHeaderSubsistema(String nombreSubsistema, int cantidadTareas) {
        LinearLayout layoutHeader = new LinearLayout(requireContext());
        layoutHeader.setOrientation(LinearLayout.HORIZONTAL);
        layoutHeader.setGravity(Gravity.CENTER_VERTICAL);
        layoutHeader.setPadding(0, 16, 0, 8);

        TextView tvNombre = new TextView(requireContext());
        tvNombre.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
        tvNombre.setText(nombreSubsistema.toUpperCase(Locale.ROOT));
        tvNombre.setTextColor(Color.WHITE);
        tvNombre.setTextSize(13);
        if (fontAnta != null) tvNombre.setTypeface(fontAnta, Typeface.BOLD);

        TextView tvContador = new TextView(requireContext());
        tvContador.setText(cantidadTareas + (cantidadTareas == 1 ? " tarea" : " tareas"));
        tvContador.setTextColor(Color.parseColor("#90CAF9"));
        tvContador.setTextSize(11);
        tvContador.setPadding(16, 4, 16, 4);
        tvContador.setBackgroundResource(R.drawable.bg_chip_anio_unselected);
        if (fontAnta != null) tvContador.setTypeface(fontAnta);

        layoutHeader.addView(tvNombre);
        layoutHeader.addView(tvContador);
        return layoutHeader;
    }

    private View inflarCardTarea(LayoutInflater inflater, ViewGroup parent, EstadoSaludTarea tarea) {
        View card = inflater.inflate(R.layout.item_tarea_salud_status, parent, false);

        TextView tvNombre = card.findViewById(R.id.tvNombreTareaSalud);
        TextView tvBadge = card.findViewById(R.id.tvBadgeEstadoTarea);
        TextView tvFrecuencia = card.findViewById(R.id.tvFrecuenciaTarea);
        TextView tvUltimo = card.findViewById(R.id.tvUltimoServicioTarea);
        ProgressBar pb = card.findViewById(R.id.pbDesgasteTarea);
        TextView tvHorasAcumuladas = card.findViewById(R.id.tvHorasAcumuladasDetalle);
        TextView tvPorcentaje = card.findViewById(R.id.tvPorcentajeUso);

        tvNombre.setText(tarea.nombre);
        tvBadge.setText(tarea.badgeTexto);

        try {
            int colorEstado = Color.parseColor(tarea.colorHex);
            tvBadge.setBackgroundTintList(ColorStateList.valueOf(colorEstado));
            pb.setProgressTintList(ColorStateList.valueOf(colorEstado));
            tvPorcentaje.setTextColor(colorEstado);
        } catch (Exception ignored) {}

        // Frecuencia
        if (tarea.esInspeccionRetorno) {
            String freqStr = tarea.frecuenciaRetornos > 1
                    ? "Frecuencia: Cada " + tarea.frecuenciaRetornos + " retornos"
                    : "Frecuencia: Cada retorno de alquiler";
            tvFrecuencia.setText(freqStr);
        } else if (tarea.intervaloHoras > 0 && tarea.mesesMaximos > 0) {
            tvFrecuencia.setText(String.format(Locale.US, "Frecuencia: Cada %d h (máx. %d m)", tarea.intervaloHoras, tarea.mesesMaximos));
        } else if (tarea.intervaloHoras > 0) {
            tvFrecuencia.setText(String.format(Locale.US, "Frecuencia: Cada %d h", tarea.intervaloHoras));
        } else {
            tvFrecuencia.setText("Frecuencia: Inspección de rutina");
        }

        // Último servicio
        if (tarea.esInspeccionRetorno) {
            tvUltimo.setText(tarea.ultimoRetorno > 0 ? "Último: Retorno #" + tarea.ultimoRetorno : "Último: Sin registro");
        } else if (tarea.ultimoHorometro > 0) {
            tvUltimo.setText(String.format(Locale.US, "Último: %,.1f h", tarea.ultimoHorometro));
        } else {
            tvUltimo.setText("Último: Sin registros previos");
        }

        // Progreso y Horas
        int progreso = Math.min(100, Math.max(0, tarea.porcentajeUso));
        pb.setProgress(progreso);
        tvPorcentaje.setText(progreso + "%");

        if (tarea.esInspeccionRetorno) {
            tvHorasAcumuladas.setText(tarea.retornosTranscurridos + " retornos acumulados de " + tarea.frecuenciaRetornos);
        } else if (tarea.intervaloHoras > 0) {
            tvHorasAcumuladas.setText(String.format(Locale.US, "%,.1f h acumuladas de %d h", tarea.horasTranscurridas, tarea.intervaloHoras));
        } else {
            tvHorasAcumuladas.setText("Inspección periódica");
        }

        return card;
    }
}
