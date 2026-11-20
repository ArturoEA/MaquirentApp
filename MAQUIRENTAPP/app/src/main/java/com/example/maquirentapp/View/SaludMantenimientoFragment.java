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
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.maquirentapp.R;
import com.example.maquirentapp.Repository.Callback;
import com.example.maquirentapp.Utils.MantenimientoSaludManager;
import com.example.maquirentapp.Utils.MantenimientoSaludManager.EstadoSalud;
import com.example.maquirentapp.Utils.MantenimientoSaludManager.EstadoSaludTarea;
import com.example.maquirentapp.Utils.MantenimientoSaludManager.ReporteSaludEquipo;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.HashMap;
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
    private final Map<String, Boolean> subsistemasExpandidos = new HashMap<>();

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

        tvTituloEquipoSalud.setText(rep.codigoGrupo + " · " + rep.tipoEquipo);
        tvBadgeGeneralSalud.setText(rep.badgeGeneral);

        try {
            int colorBadge = Color.parseColor(rep.colorGeneralHex);
            tvBadgeGeneralSalud.setBackgroundTintList(ColorStateList.valueOf(colorBadge));
        } catch (Exception ignored) {}

        tvHorometroActualGrande.setText(rep.horometroActual > 0
                ? String.format(Locale.US, "%,.0f h", rep.horometroActual)
                : "-");
        tvIndiceSaludPorcentaje.setText(rep.indiceSaludPorcentaje + "%");

        if (rep.indiceSaludPorcentaje >= 85) {
            tvIndiceSaludPorcentaje.setTextColor(Color.parseColor("#66BB6A"));
        } else if (rep.indiceSaludPorcentaje >= 60) {
            tvIndiceSaludPorcentaje.setTextColor(Color.parseColor("#FFA726"));
        } else {
            tvIndiceSaludPorcentaje.setTextColor(Color.parseColor("#EF5350"));
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
            tvValorPuntualidad.setTextColor(Color.parseColor("#66BB6A"));
        } else if (m.indicePuntualidad >= 60) {
            tvValorPuntualidad.setTextColor(Color.parseColor("#FFA726"));
        } else {
            tvValorPuntualidad.setTextColor(Color.parseColor("#EF5350"));
        }

        tvValorIntervaloPromedio.setText(m.intervaloPromedio > 0
                ? String.format(Locale.US, "%.0f h", m.intervaloPromedio)
                : "-");
        tvDiagnosticoDisciplina.setText(m.diagnostico);
    }

    private void renderizarTareas() {
        layoutSubsistemasTareas.removeAllViews();
        if (ultimoReporte == null || ultimoReporte.tareasPorSubsistema == null) return;

        LayoutInflater inflater = LayoutInflater.from(requireContext());
        int totalTareasMostradas = 0;

        for (Map.Entry<String, List<EstadoSaludTarea>> entry : ultimoReporte.tareasPorSubsistema.entrySet()) {
            String subsistemaNombre = entry.getKey();
            List<EstadoSaludTarea> lista = entry.getValue();

            List<EstadoSaludTarea> listaFiltrada = new ArrayList<>();
            boolean tienePendientes = false;
            for (EstadoSaludTarea t : lista) {
                if (coincideConFiltro(t)) {
                    listaFiltrada.add(t);
                    if (t.isVencido() || t.estado == EstadoSalud.PROXIMO) {
                        tienePendientes = true;
                    }
                }
            }

            if (listaFiltrada.isEmpty()) continue;
            totalTareasMostradas += listaFiltrada.size();

            // En vista TODAS, expandir por defecto solo subsistemas con tareas vencidas o próximas
            boolean expandido;
            if (filtroActual != FiltroEstado.TODAS) {
                expandido = true;
            } else if (subsistemasExpandidos.containsKey(subsistemaNombre)) {
                expandido = Boolean.TRUE.equals(subsistemasExpandidos.get(subsistemaNombre));
            } else {
                expandido = tienePendientes;
                subsistemasExpandidos.put(subsistemaNombre, expandido);
            }

            LinearLayout contenedorTareasSub = new LinearLayout(requireContext());
            contenedorTareasSub.setOrientation(LinearLayout.VERTICAL);
            contenedorTareasSub.setVisibility(expandido ? View.VISIBLE : View.GONE);

            View headerSubsistema = crearHeaderSubsistema(subsistemaNombre, listaFiltrada.size(), tienePendientes, expandido, contenedorTareasSub);
            layoutSubsistemasTareas.addView(headerSubsistema);

            for (EstadoSaludTarea tarea : listaFiltrada) {
                View cardTarea = inflarCardTarea(inflater, contenedorTareasSub, tarea);
                contenedorTareasSub.addView(cardTarea);
            }
            layoutSubsistemasTareas.addView(contenedorTareasSub);
        }

        if (totalTareasMostradas == 0) {
            TextView tvVacio = new TextView(requireContext());
            tvVacio.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
            tvVacio.setGravity(Gravity.CENTER);
            tvVacio.setPadding(24, 40, 24, 40);
            tvVacio.setText("No hay tareas en esta categoría.");
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

    private View crearHeaderSubsistema(String nombreSubsistema, int cantidadTareas, boolean tienePendientes,
                                       boolean expandidoInicial, LinearLayout contenedorHijo) {
        LinearLayout layoutHeader = new LinearLayout(requireContext());
        layoutHeader.setOrientation(LinearLayout.HORIZONTAL);
        layoutHeader.setGravity(Gravity.CENTER_VERTICAL);
        layoutHeader.setPadding(4, 18, 4, 8);

        TextView tvNombre = new TextView(requireContext());
        tvNombre.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
        tvNombre.setText(nombreSubsistema.toUpperCase(Locale.ROOT));
        tvNombre.setTextColor(tienePendientes ? Color.WHITE : Color.parseColor("#B0BEC5"));
        tvNombre.setTextSize(12.5f);
        if (fontAnta != null) tvNombre.setTypeface(fontAnta, Typeface.BOLD);

        TextView tvContador = new TextView(requireContext());
        tvContador.setText(cantidadTareas + (expandidoInicial ? " ▲" : " ▼"));
        tvContador.setTextColor(Color.parseColor("#90CAF9"));
        tvContador.setTextSize(11);
        tvContador.setPadding(18, 5, 18, 5);
        tvContador.setBackgroundResource(R.drawable.bg_chip_anio_unselected);
        if (fontAnta != null) tvContador.setTypeface(fontAnta);

        layoutHeader.setOnClickListener(v -> {
            boolean nuevoEstado = contenedorHijo.getVisibility() != View.VISIBLE;
            contenedorHijo.setVisibility(nuevoEstado ? View.VISIBLE : View.GONE);
            subsistemasExpandidos.put(nombreSubsistema, nuevoEstado);
            tvContador.setText(cantidadTareas + (nuevoEstado ? " ▲" : " ▼"));
        });

        layoutHeader.addView(tvNombre);
        layoutHeader.addView(tvContador);
        return layoutHeader;
    }

    private View inflarCardTarea(LayoutInflater inflater, ViewGroup parent, EstadoSaludTarea tarea) {
        View card = inflater.inflate(R.layout.item_tarea_salud_status, parent, false);

        TextView tvNombre = card.findViewById(R.id.tvNombreTareaSalud);
        TextView tvBadge = card.findViewById(R.id.tvBadgeEstadoTarea);
        TextView tvFrecuencia = card.findViewById(R.id.tvFrecuenciaTarea);
        ProgressBar pb = card.findViewById(R.id.pbDesgasteTarea);
        TextView tvHorasAcumuladas = card.findViewById(R.id.tvHorasAcumuladasDetalle);

        tvNombre.setText(tarea.nombre);
        tvBadge.setText(tarea.badgeTexto);

        try {
            int colorEstado = Color.parseColor(tarea.colorHex);
            tvBadge.setBackgroundTintList(ColorStateList.valueOf(colorEstado));
            pb.setProgressTintList(ColorStateList.valueOf(colorEstado));
        } catch (Exception ignored) {}

        // Línea compacta: Frecuencia · Último servicio
        String freqStr;
        if (tarea.esInspeccionRetorno) {
            freqStr = tarea.frecuenciaRetornos > 1 ? "Cada " + tarea.frecuenciaRetornos + " retornos" : "Cada retorno";
        } else if (tarea.intervaloHoras > 0) {
            freqStr = "Cada " + tarea.intervaloHoras + " h";
        } else {
            freqStr = "Rutina";
        }

        String ultimoStr;
        if (tarea.esInspeccionRetorno) {
            ultimoStr = tarea.ultimoRetorno > 0 ? "Último: #" + tarea.ultimoRetorno : "Sin registro";
        } else if (tarea.ultimoHorometro > 0) {
            ultimoStr = String.format(Locale.US, "Último: %,.0f h", tarea.ultimoHorometro);
        } else {
            ultimoStr = "Sin registro";
        }
        tvFrecuencia.setText(freqStr + " · " + ultimoStr);

        // Progreso y uso corto sin decimales innecesarios
        int progreso = Math.min(100, Math.max(0, tarea.porcentajeUso));
        pb.setProgress(progreso);

        if (tarea.esInspeccionRetorno) {
            tvHorasAcumuladas.setText(tarea.retornosTranscurridos + " / " + tarea.frecuenciaRetornos + " ret.");
        } else if (tarea.intervaloHoras > 0) {
            tvHorasAcumuladas.setText(String.format(Locale.US, "%.0f / %d h", Math.max(0, tarea.horasTranscurridas), tarea.intervaloHoras));
        } else {
            tvHorasAcumuladas.setText("-");
        }

        return card;
    }
}
