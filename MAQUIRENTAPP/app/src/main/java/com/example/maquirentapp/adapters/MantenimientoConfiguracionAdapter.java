package com.example.maquirentapp.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.maquirentapp.Model.MantenimientoConfiguracion;
import com.example.maquirentapp.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Adaptador para la pantalla de Configuración > Mantenimientos,
 * organizado en secciones desplegables (acordeón colapsable) por cada subsistema.
 */
public class MantenimientoConfiguracionAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static final int TYPE_HEADER = 0;
    public static final int TYPE_ITEM = 1;

    public static class FilaVisual {
        public int tipo;
        public String subsistema;
        public boolean expandido = false;
        public int totalTareas = 0;
        public MantenimientoConfiguracion tarea;

        public static FilaVisual crearHeader(String subsistema, boolean expandido, int total) {
            FilaVisual f = new FilaVisual();
            f.tipo = TYPE_HEADER;
            f.subsistema = subsistema;
            f.expandido = expandido;
            f.totalTareas = total;
            return f;
        }

        public static FilaVisual crearTarea(MantenimientoConfiguracion tarea) {
            FilaVisual f = new FilaVisual();
            f.tipo = TYPE_ITEM;
            f.tarea = tarea;
            f.subsistema = tarea != null ? tarea.getSubsistema() : "";
            return f;
        }
    }

    private List<MantenimientoConfiguracion> mantenimientosConfiguracionList;
    private final List<FilaVisual> filasVisibles = new ArrayList<>();
    private final Map<String, Boolean> estadoExpandidoSubsistema = new HashMap<>();
    private final Context context;
    private final OnMantenimientoConfiguracionActionListener listener;

    public interface OnMantenimientoConfiguracionActionListener {
        void onEditarClick(MantenimientoConfiguracion mantenimientoConfiguracion);
        void onEliminarClick(MantenimientoConfiguracion mantenimientoConfiguracion);
    }

    public MantenimientoConfiguracionAdapter(
            List<MantenimientoConfiguracion> mantenimientosConfiguracionList,
            Context context,
            OnMantenimientoConfiguracionActionListener listener) {
        this.mantenimientosConfiguracionList = mantenimientosConfiguracionList != null
                ? new ArrayList<>(mantenimientosConfiguracionList) : new ArrayList<>();
        this.context = context;
        this.listener = listener;
        reconstruirFilas();
    }

    private int obtenerPrioridadSubsistema(String subsistema) {
        if (subsistema == null) return 99;
        String s = subsistema.toLowerCase(Locale.ROOT).trim();
        if (s.contains("lubricaci")) return 1;
        if (s.contains("combustible")) return 2;
        if (s.contains("admis") || s.contains("escape")) return 3;
        if (s.contains("eléctric") || s.contains("electric") || s.contains("generac") || s.contains("control") || s.contains("compres")) return 4;
        if (s.contains("estructural") || s.contains("carreta") || s.contains("chasis")) return 5;
        return 6;
    }

    private void reconstruirFilas() {
        filasVisibles.clear();

        List<MantenimientoConfiguracion> ordenados = new ArrayList<>(mantenimientosConfiguracionList);
        Collections.sort(ordenados, (a, b) -> {
            int pA = obtenerPrioridadSubsistema(a.getSubsistema());
            int pB = obtenerPrioridadSubsistema(b.getSubsistema());
            if (pA != pB) return Integer.compare(pA, pB);
            int compH = Integer.compare(a.getIntervaloHoras(), b.getIntervaloHoras());
            if (compH != 0) return compH;
            return Integer.compare(a.getFrecuenciaRetornos(), b.getFrecuenciaRetornos());
        });

        Map<String, List<MantenimientoConfiguracion>> grupos = new LinkedHashMap<>();
        for (MantenimientoConfiguracion item : ordenados) {
            String sub = item.getSubsistema() != null && !item.getSubsistema().trim().isEmpty()
                    ? item.getSubsistema().trim() : "General";
            if (!grupos.containsKey(sub)) {
                grupos.put(sub, new ArrayList<>());
            }
            grupos.get(sub).add(item);
        }

        for (Map.Entry<String, List<MantenimientoConfiguracion>> entrada : grupos.entrySet()) {
            String subNombre = entrada.getKey();
            List<MantenimientoConfiguracion> tareasGrupo = entrada.getValue();

            // Colapsado por defecto para facilitar la revisión rápida por subsistema
            boolean expandido = estadoExpandidoSubsistema.containsKey(subNombre)
                    && Boolean.TRUE.equals(estadoExpandidoSubsistema.get(subNombre));

            filasVisibles.add(FilaVisual.crearHeader(subNombre, expandido, tareasGrupo.size()));

            if (expandido) {
                for (MantenimientoConfiguracion t : tareasGrupo) {
                    filasVisibles.add(FilaVisual.crearTarea(t));
                }
            }
        }

        notifyDataSetChanged();
    }

    private void toggleSubsistema(String subsistema) {
        boolean actual = estadoExpandidoSubsistema.containsKey(subsistema)
                && Boolean.TRUE.equals(estadoExpandidoSubsistema.get(subsistema));
        estadoExpandidoSubsistema.put(subsistema, !actual);
        reconstruirFilas();
    }

    @Override
    public int getItemViewType(int position) {
        return filasVisibles.get(position).tipo;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_header_subsistema_seleccionable, parent, false);
            return new HeaderViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_mantenimiento_configuracion, parent, false);
            return new MantenimientoConfiguracionViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        FilaVisual fila = filasVisibles.get(position);
        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).bind(fila);
        } else if (holder instanceof MantenimientoConfiguracionViewHolder) {
            ((MantenimientoConfiguracionViewHolder) holder).bind(fila.tarea);
        }
    }

    @Override
    public int getItemCount() {
        return filasVisibles.size();
    }

    public void actualizarLista(List<MantenimientoConfiguracion> nuevaLista) {
        this.mantenimientosConfiguracionList = nuevaLista != null ? new ArrayList<>(nuevaLista) : new ArrayList<>();
        reconstruirFilas();
    }

    public void expandirSubsistema(String subsistema) {
        if (subsistema != null && !subsistema.trim().isEmpty()) {
            estadoExpandidoSubsistema.put(subsistema.trim(), true);
        }
    }

    // =========================================================================
    // VIEWHOLDER ENCABEZADO DE SUBSISTEMA DESPLEGABLE
    // =========================================================================

    public class HeaderViewHolder extends RecyclerView.ViewHolder {
        private final CardView cardHeader;
        private final TextView tvTitulo;
        private final TextView tvContador;
        private final ImageView ivChevron;

        public HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            cardHeader = itemView.findViewById(R.id.cardHeaderSubsistema);
            tvTitulo = itemView.findViewById(R.id.tvTituloSubsistemaHeader);
            tvContador = itemView.findViewById(R.id.tvContadorSubsistemaHeader);
            ivChevron = itemView.findViewById(R.id.ivChevronHeader);
        }

        public void bind(FilaVisual fila) {
            String titulo = fila.subsistema != null ? fila.subsistema.toUpperCase(Locale.ROOT) : "GENERAL";
            tvTitulo.setText(titulo);

            String contadorTexto = fila.totalTareas == 1
                    ? "1 tarea"
                    : String.format(Locale.getDefault(), "%d tareas", fila.totalTareas);
            tvContador.setText(contadorTexto);
            tvContador.setTextColor(Color.parseColor("#90CAF9"));

            ivChevron.setRotation(fila.expandido ? 180f : 0f);
            cardHeader.setOnClickListener(v -> toggleSubsistema(fila.subsistema));
        }
    }

    // =========================================================================
    // VIEWHOLDER TAREA DE CONFIGURACIÓN
    // =========================================================================

    public class MantenimientoConfiguracionViewHolder extends RecyclerView.ViewHolder {
        private final ImageView btnEditar, btnEliminar;
        private final TextView tvNombre, tvDetalleIntervalo;

        public MantenimientoConfiguracionViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNombre = itemView.findViewById(R.id.tvNombreMantenimiento);
            tvDetalleIntervalo = itemView.findViewById(R.id.tvDetalleIntervalo);
            btnEditar = itemView.findViewById(R.id.btnEditarMantenimiento);
            btnEliminar = itemView.findViewById(R.id.btnEliminarMantenimiento);
        }

        public void bind(MantenimientoConfiguracion mantenimientoConfiguracion) {
            if (mantenimientoConfiguracion == null) return;

            tvNombre.setText(mantenimientoConfiguracion.getNombre());

            if (tvDetalleIntervalo != null) {
                if (mantenimientoConfiguracion.isEsInspeccionRetorno()) {
                    int frec = mantenimientoConfiguracion.getFrecuenciaRetornos() > 0 ? mantenimientoConfiguracion.getFrecuenciaRetornos() : 1;
                    String txt = frec == 1
                            ? "Al retorno de alquiler (Cada 1 retorno)"
                            : "Al retorno de alquiler (Cada " + frec + " retornos)";
                    tvDetalleIntervalo.setText(txt);
                } else {
                    int h = mantenimientoConfiguracion.getIntervaloHoras();
                    int m = mantenimientoConfiguracion.getMesesMaximos();
                    if (m > 0) {
                        tvDetalleIntervalo.setText(String.format(Locale.US, "Cada %d h (o máx. %d meses)", h, m));
                    } else {
                        tvDetalleIntervalo.setText(String.format(Locale.US, "Cada %d h", h));
                    }
                }
            }

            btnEditar.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEditarClick(mantenimientoConfiguracion);
                }
            });

            btnEliminar.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEliminarClick(mantenimientoConfiguracion);
                }
            });
        }
    }
}
