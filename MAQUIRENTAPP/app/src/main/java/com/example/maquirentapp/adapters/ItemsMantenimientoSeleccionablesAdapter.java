package com.example.maquirentapp.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.maquirentapp.Model.MantenimientoConfiguracion;
import com.example.maquirentapp.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Adaptador para la selección de items de mantenimiento organizado en
 * secciones desplegables (acordeón colapsable) por cada subsistema técnico.
 */
public class ItemsMantenimientoSeleccionablesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static final int TYPE_HEADER = 0;
    public static final int TYPE_ITEM = 1;

    public static class FilaVisual {
        public int tipo;
        public String subsistema;
        public boolean expandido = true;
        public int totalTareas = 0;
        public int tareasSeleccionadas = 0;
        public MantenimientoConfiguracion tarea;

        public static FilaVisual crearHeader(String subsistema, boolean expandido, int total, int selec) {
            FilaVisual f = new FilaVisual();
            f.tipo = TYPE_HEADER;
            f.subsistema = subsistema;
            f.expandido = expandido;
            f.totalTareas = total;
            f.tareasSeleccionadas = selec;
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

    private final Context context;
    private List<MantenimientoConfiguracion> itemsOriginales = new ArrayList<>();
    private final List<FilaVisual> filasVisibles = new ArrayList<>();
    private final Set<String> itemsSeleccionados = new HashSet<>();
    private final Map<String, Boolean> estadoExpandidoSubsistema = new HashMap<>();
    private boolean modoLectura = false;

    public ItemsMantenimientoSeleccionablesAdapter(Context context, List<MantenimientoConfiguracion> items) {
        this.context = context;
        this.itemsOriginales = items != null ? items : new ArrayList<>();
        reconstruirFilas();
    }

    public void setModoLectura(boolean modoLectura) {
        this.modoLectura = modoLectura;
        notifyDataSetChanged();
    }

    public void setItemsSeleccionados(List<String> ids) {
        this.itemsSeleccionados.clear();
        if (ids != null) {
            this.itemsSeleccionados.addAll(ids);
        }
        reconstruirFilas();
    }

    public List<String> getItemsSeleccionados() {
        return new ArrayList<>(itemsSeleccionados);
    }

    public void actualizarLista(List<MantenimientoConfiguracion> nuevaLista) {
        this.itemsOriginales = nuevaLista != null ? nuevaLista : new ArrayList<>();
        reconstruirFilas();
    }

    private int obtenerPrioridadSubsistema(String subsistema) {
        if (subsistema == null) return 99;
        String s = subsistema.toLowerCase().trim();
        if (s.contains("lubricaci")) return 1;
        if (s.contains("combustible")) return 2;
        if (s.contains("admis") || s.contains("escape")) return 3;
        if (s.contains("eléctric") || s.contains("electric") || s.contains("generac") || s.contains("control") || s.contains("compres")) return 4;
        if (s.contains("estructural") || s.contains("carreta") || s.contains("chasis")) return 5;
        return 6;
    }

    private void reconstruirFilas() {
        filasVisibles.clear();

        // 1. Agrupar tareas originales por subsistema manteniendo orden de prioridad
        Map<String, List<MantenimientoConfiguracion>> grupos = new LinkedHashMap<>();

        // Ordenar primero la lista base
        List<MantenimientoConfiguracion> ordenados = new ArrayList<>(itemsOriginales);
        Collections.sort(ordenados, (a, b) -> {
            int pA = obtenerPrioridadSubsistema(a.getSubsistema());
            int pB = obtenerPrioridadSubsistema(b.getSubsistema());
            if (pA != pB) return Integer.compare(pA, pB);
            return Integer.compare(a.getIntervaloHoras(), b.getIntervaloHoras());
        });

        for (MantenimientoConfiguracion item : ordenados) {
            String sub = item.getSubsistema() != null && !item.getSubsistema().trim().isEmpty()
                    ? item.getSubsistema().trim() : "General";
            if (!grupos.containsKey(sub)) {
                grupos.put(sub, new ArrayList<>());
            }
            grupos.get(sub).add(item);
        }

        // 2. Para cada subsistema, agregar Header y (si está expandido) sus tareas
        for (Map.Entry<String, List<MantenimientoConfiguracion>> entrada : grupos.entrySet()) {
            String subNombre = entrada.getKey();
            List<MantenimientoConfiguracion> tareasGrupo = entrada.getValue();

            int total = tareasGrupo.size();
            int selec = 0;
            for (MantenimientoConfiguracion t : tareasGrupo) {
                if (t.getId() != null && itemsSeleccionados.contains(t.getId())) {
                    selec++;
                }
            }

            // Colapsado por defecto si no tiene ítems marcados aún, para mantener la lista compacta
            boolean expandido = estadoExpandidoSubsistema.containsKey(subNombre)
                    ? Boolean.TRUE.equals(estadoExpandidoSubsistema.get(subNombre))
                    : (selec > 0);

            filasVisibles.add(FilaVisual.crearHeader(subNombre, expandido, total, selec));

            if (expandido) {
                for (MantenimientoConfiguracion t : tareasGrupo) {
                    filasVisibles.add(FilaVisual.crearTarea(t));
                }
            }
        }

        notifyDataSetChanged();
    }

    private void toggleSubsistema(String subsistema, boolean estadoActual) {
        estadoExpandidoSubsistema.put(subsistema, !estadoActual);
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
                    .inflate(R.layout.item_mantenimiento_seleccionable, parent, false);
            return new TareaViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        FilaVisual fila = filasVisibles.get(position);
        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).bind(fila);
        } else if (holder instanceof TareaViewHolder) {
            ((TareaViewHolder) holder).bind(fila.tarea);
        }
    }

    @Override
    public int getItemCount() {
        return filasVisibles.size();
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

            String contadorTexto = fila.tareasSeleccionadas > 0
                    ? String.format(Locale.getDefault(), "%d de %d marcadas", fila.tareasSeleccionadas, fila.totalTareas)
                    : String.format(Locale.getDefault(), "%d tareas", fila.totalTareas);
            tvContador.setText(contadorTexto);

            // Resaltar contador si hay tareas marcadas en este subsistema
            if (fila.tareasSeleccionadas > 0) {
                tvContador.setTextColor(ContextCompat.getColor(context, R.color.selection_indicator));
            } else {
                tvContador.setTextColor(ContextCompat.getColor(context, R.color.text_secondary));
            }

            // Rotación suave del chevron: 180° cuando está expandido, 0° cuando está colapsado
            ivChevron.setRotation(fila.expandido ? 180f : 0f);

            cardHeader.setOnClickListener(v -> toggleSubsistema(fila.subsistema, fila.expandido));
        }
    }

    // =========================================================================
    // VIEWHOLDER ITEM DE TAREA SELECCIONABLE
    // =========================================================================

    public class TareaViewHolder extends RecyclerView.ViewHolder {
        private final CardView cardView;
        private final TextView tvNombre, tvIntervalo;
        private final CheckBox checkBox;

        public TareaViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardItemSeleccionable);
            tvNombre = itemView.findViewById(R.id.tvNombreItemSeleccionable);
            tvIntervalo = itemView.findViewById(R.id.tvIntervaloItemSeleccionable);
            checkBox = itemView.findViewById(R.id.checkboxAccesorio);
        }

        public void bind(MantenimientoConfiguracion item) {
            if (item == null) return;

            tvNombre.setText(item.getNombre());

            if (tvIntervalo != null) {
                if (item.isEsInspeccionRetorno()) {
                    int frec = item.getFrecuenciaRetornos() > 0 ? item.getFrecuenciaRetornos() : 1;
                    String txt = frec == 1
                            ? "Al retorno de alquiler (Cada 1 retorno)"
                            : "Al retorno de alquiler (Cada " + frec + " retornos)";
                    tvIntervalo.setText(txt);
                } else {
                    int h = item.getIntervaloHoras();
                    int m = item.getMesesMaximos();
                    if (m > 0) {
                        tvIntervalo.setText(String.format(Locale.US, "Cada %d h (o máx. %d meses)", h, m));
                    } else {
                        tvIntervalo.setText(String.format(Locale.US, "Cada %d h", h));
                    }
                }
            }

            boolean seleccionado = item.getId() != null && itemsSeleccionados.contains(item.getId());
            checkBox.setChecked(seleccionado);

            if (seleccionado) {
                cardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.background_dark));
                tvNombre.setTextColor(ContextCompat.getColor(context, R.color.white));
            } else {
                cardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.noseleccionado_accesorio));
                tvNombre.setTextColor(ContextCompat.getColor(context, R.color.white));
            }

            if (!modoLectura) {
                View.OnClickListener toggleClick = v -> {
                    if (item.getId() == null) return;
                    if (itemsSeleccionados.contains(item.getId())) {
                        itemsSeleccionados.remove(item.getId());
                    } else {
                        itemsSeleccionados.add(item.getId());
                    }
                    reconstruirFilas();
                };

                itemView.setOnClickListener(toggleClick);
                checkBox.setOnClickListener(toggleClick);
                itemView.setClickable(true);
                checkBox.setClickable(true);
            } else {
                itemView.setOnClickListener(null);
                checkBox.setOnClickListener(null);
                itemView.setClickable(false);
                checkBox.setClickable(false);
            }
        }
    }
}
