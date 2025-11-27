package com.example.maquirentapp.Access;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.maquirentapp.Model.Mantenimiento;
import com.example.maquirentapp.Model.MantenimientoConfiguracion;
import com.example.maquirentapp.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MantenimientosAdapter extends RecyclerView.Adapter<MantenimientosAdapter.ViewHolder> implements Filterable {
    private List<Mantenimiento> mantenimientos;
    private List<Mantenimiento> mantenimientosFull;
    private Map<String, MantenimientoConfiguracion> itemsConfigMap;
    private Context context;
    private OnMantenimientoClickListener listener;

    public interface OnMantenimientoClickListener {
        void onMantenimientoClick(Mantenimiento mantenimiento);
    }

    public MantenimientosAdapter(Context context, List<Mantenimiento> mantenimientos,
                                 List<MantenimientoConfiguracion> itemsConfig,
                                 OnMantenimientoClickListener listener) {
        this.context = context;
        this.mantenimientos = mantenimientos != null ? mantenimientos : new ArrayList<>();
        this.mantenimientosFull = new ArrayList<>(this.mantenimientos);
        this.listener = listener;

        // Crear mapa de items de configuración para búsqueda rápida
        this.itemsConfigMap = new HashMap<>();
        if (itemsConfig != null) {
            for (MantenimientoConfiguracion item : itemsConfig) {
                itemsConfigMap.put(item.getId(), item);
            }
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_mantenimiento, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Mantenimiento mantenimiento = mantenimientos.get(position);
        holder.bind(mantenimiento);
    }

    @Override
    public int getItemCount() {
        return mantenimientos.size();
    }
    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                List<Mantenimiento> filteredList = new ArrayList<>();

                if (constraint == null || constraint.length() == 0) {
                    filteredList.addAll(mantenimientosFull);
                } else {
                    String filterPattern = constraint.toString().toLowerCase().trim();

                    for (Mantenimiento item : mantenimientosFull) {
                        if (item.getComentarios() != null &&
                                item.getComentarios().toLowerCase().contains(filterPattern)) {
                            filteredList.add(item);
                        }
                        else if (item.getEmpresa() != null &&
                                item.getEmpresa().toLowerCase().contains(filterPattern)) {
                            filteredList.add(item);
                        }
                    }
                }

                FilterResults results = new FilterResults();
                results.values = filteredList;
                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                mantenimientos.clear();
                mantenimientos.addAll((List) results.values);
                notifyDataSetChanged();
            }
        };
    }

    public Mantenimiento getMantenimientoAt(int position) {
        return mantenimientos.get(position);
    }

    public void removerMantenimiento(int position) {
        mantenimientos.remove(position);
        notifyItemRemoved(position);
    }

    public void restaurarMantenimiento(Mantenimiento mantenimiento, int position) {
        mantenimientos.add(position, mantenimiento);
        notifyItemInserted(position);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private TextView tvEmpresa, tvFechaHoras, tvComentarios;
        private LinearLayout layoutIconos;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvEmpresa = itemView.findViewById(R.id.tvEmpresa);
            tvFechaHoras = itemView.findViewById(R.id.tvFechaHoras);
            tvComentarios = itemView.findViewById(R.id.tvComentarios);
            layoutIconos = itemView.findViewById(R.id.layoutIconos);
        }

        public void bind(Mantenimiento mantenimiento) {
            tvEmpresa.setText(mantenimiento.getEmpresa());
            tvFechaHoras.setText(mantenimiento.getHorometro() + " horas - " + mantenimiento.getFecha());

            // Comentarios
            if (mantenimiento.getComentarios() != null && !mantenimiento.getComentarios().isEmpty()) {
                tvComentarios.setText(mantenimiento.getComentarios());
                tvComentarios.setVisibility(View.VISIBLE);
            } else {
                tvComentarios.setVisibility(View.GONE);
            }

            // Limpiar íconos anteriores
            layoutIconos.removeAllViews();

            // Mostrar íconos de items realizados
            if (mantenimiento.getItemsRealizados() != null) {
                for (String itemId : mantenimiento.getItemsRealizados()) {
                    MantenimientoConfiguracion itemConfig = itemsConfigMap.get(itemId);
                    if (itemConfig != null) {
                        agregarIcono(itemConfig);
                    }
                }
            }

            // Click en el item
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onMantenimientoClick(mantenimiento);
                }
            });
        }

        private void agregarIcono(MantenimientoConfiguracion itemConfig) {
            ImageView imageView = new ImageView(context);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    (int) (40 * context.getResources().getDisplayMetrics().density),
                    (int) (40 * context.getResources().getDisplayMetrics().density)
            );
            params.setMargins(4, 0, 4, 0);
            imageView.setLayoutParams(params);
            imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);

            // Cargar ícono
            if (itemConfig.getIcono() != null && !itemConfig.getIcono().isEmpty()) {
                Glide.with(context)
                        .load(itemConfig.getIcono())
                        .placeholder(R.drawable.icon_mantenimiento_blanco)
                        .error(R.drawable.icon_mantenimiento_blanco)
                        .into(imageView);
            } else {
                imageView.setImageResource(R.drawable.icon_mantenimiento_blanco);
            }

            layoutIconos.addView(imageView);
        }
    }

    public void actualizarLista(List<Mantenimiento> nuevaLista) {
        this.mantenimientos = nuevaLista != null ? nuevaLista : new ArrayList<>();
        this.mantenimientosFull = new ArrayList<>(this.mantenimientos);
        notifyDataSetChanged();
    }

    public void actualizarItemsConfig(List<MantenimientoConfiguracion> itemsConfig) {
        this.itemsConfigMap.clear();
        if (itemsConfig != null) {
            for (MantenimientoConfiguracion item : itemsConfig) {
                itemsConfigMap.put(item.getId(), item);
            }
        }
        notifyDataSetChanged();
    }
}