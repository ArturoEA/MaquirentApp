package com.example.maquirentapp.Access;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.maquirentapp.Model.MantenimientoConfiguracion;
import com.example.maquirentapp.R;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ItemsMantenimientoSeleccionablesAdapter extends RecyclerView.Adapter<ItemsMantenimientoSeleccionablesAdapter.ViewHolder> {
    private List<MantenimientoConfiguracion> items;
    private Set<String> itemsSeleccionados;
    private Context context;
    private boolean modoLectura = false;

    public ItemsMantenimientoSeleccionablesAdapter(Context context, List<MantenimientoConfiguracion> items) {
        this.context = context;
        this.items = items != null ? items : new ArrayList<>();
        this.itemsSeleccionados = new HashSet<>();
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
        notifyDataSetChanged();
    }

    public List<String> getItemsSeleccionados() {
        return new ArrayList<>(itemsSeleccionados);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_mantenimiento_seleccionable, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MantenimientoConfiguracion item = items.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private CardView cardView;
        private ImageView ivIcono;
        private TextView tvNombre;
        private CheckBox checkBox;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardItemSeleccionable);
            ivIcono = itemView.findViewById(R.id.ivIconoItemSeleccionable);
            tvNombre = itemView.findViewById(R.id.tvNombreItemSeleccionable);
            checkBox = itemView.findViewById(R.id.checkboxAccesorio);
        }

        public void bind(MantenimientoConfiguracion item) {
            tvNombre.setText(item.getNombre());

            // Cargar ícono
            if (item.getIcono() != null && !item.getIcono().isEmpty()) {
                Glide.with(context)
                        .load(item.getIcono())
                        .placeholder(R.drawable.icon_mantenimiento_blanco)
                        .error(R.drawable.icon_mantenimiento_blanco)
                        .into(ivIcono);
            } else {
                ivIcono.setImageResource(R.drawable.icon_mantenimiento_blanco);
            }

            boolean seleccionado = itemsSeleccionados.contains(item.getId());

            checkBox.setChecked(seleccionado);

            if (seleccionado) {
                cardView.setCardBackgroundColor(context.getResources().getColor(R.color.background_dark));
                tvNombre.setTextColor(context.getResources().getColor(R.color.white));
            } else {
                cardView.setCardBackgroundColor(context.getResources().getColor(R.color.noseleccionado_accesorio));
                tvNombre.setTextColor(context.getResources().getColor(R.color.white));
            }

            if (!modoLectura) {
                itemView.setOnClickListener(v -> {
                    if (itemsSeleccionados.contains(item.getId())) {
                        itemsSeleccionados.remove(item.getId());
                    } else {
                        itemsSeleccionados.add(item.getId());
                    }
                    notifyItemChanged(getAdapterPosition());
                });

                // AÑADIDO: También manejar click en el CheckBox
                checkBox.setOnClickListener(v -> {
                    if (itemsSeleccionados.contains(item.getId())) {
                        itemsSeleccionados.remove(item.getId());
                    } else {
                        itemsSeleccionados.add(item.getId());
                    }
                    notifyItemChanged(getAdapterPosition());
                });
            } else {
                itemView.setOnClickListener(null);
                checkBox.setOnClickListener(null);

                itemView.setAlpha(0.7f);
                checkBox.setAlpha(0.7f);
            }
        }
    }

    public void actualizarLista(List<MantenimientoConfiguracion> nuevaLista) {
        this.items = nuevaLista != null ? nuevaLista : new ArrayList<>();
        notifyDataSetChanged();
    }
}