package com.example.maquirentapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.maquirentapp.Model.ItemValorizacion;
import com.example.maquirentapp.R;
import com.google.android.material.checkbox.MaterialCheckBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SeleccionValorizacionAdapter extends RecyclerView.Adapter<SeleccionValorizacionAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(ItemValorizacion item, int position);
    }

    private List<ItemValorizacion> items = new ArrayList<>();
    private List<ItemValorizacion> seleccionados = new ArrayList<>();
    private OnItemClickListener itemClickListener;

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.itemClickListener = listener;
    }

    public void setItems(List<ItemValorizacion> items) {
        this.items = items;
        this.seleccionados = new ArrayList<>(items);
        notifyDataSetChanged();
    }

    public List<ItemValorizacion> getSeleccionados() {
        return seleccionados;
    }

    public List<ItemValorizacion> getItems() {
        return items;
    }

    public double getTotalSeleccionado() {
        double total = 0;
        for (ItemValorizacion item : seleccionados) {
            total += item.getTotalItem();
        }
        return total;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_seleccion_valorizacion, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ItemValorizacion item = items.get(position);
        boolean isSelected = seleccionados.contains(item);

        holder.tvEquipo.setText(item.getDescripcionEquipo());
        holder.tvPeriodo.setText("Período: " + item.getFechaInicio() + " al " + item.getFechaFin());
        holder.tvMonto.setText(String.format(Locale.US, "%.2f", item.getTotalItem()));

        holder.checkBox.setChecked(isSelected);

        // Congruente con accesorios: cambiar color según selección
        if (isSelected) {
            holder.cardView.setCardBackgroundColor(holder.itemView.getContext().getColor(R.color.background_dark));
            holder.tvEquipo.setTextColor(holder.itemView.getContext().getColor(R.color.white));
            holder.tvPeriodo.setTextColor(holder.itemView.getContext().getColor(R.color.white));
            holder.tvMonto.setTextColor(holder.itemView.getContext().getColor(R.color.white));
        } else {
            holder.cardView.setCardBackgroundColor(holder.itemView.getContext().getColor(R.color.noseleccionado_accesorio));
            holder.tvEquipo.setTextColor(holder.itemView.getContext().getColor(R.color.white));
            holder.tvPeriodo.setTextColor(holder.itemView.getContext().getColor(R.color.white));
            holder.tvMonto.setTextColor(holder.itemView.getContext().getColor(R.color.white));
        }

        holder.itemView.setOnClickListener(v -> {
            if (seleccionados.contains(item)) {
                seleccionados.remove(item);
            } else {
                seleccionados.add(item);
            }
            notifyItemChanged(position);

            if (itemClickListener != null) {
                itemClickListener.onItemClick(item, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        TextView tvEquipo, tvPeriodo, tvMonto;
        MaterialCheckBox checkBox;

        ViewHolder(View itemView) {
            super(itemView);
            cardView = (CardView) itemView;
            tvEquipo = itemView.findViewById(R.id.tvEquipo);
            tvPeriodo = itemView.findViewById(R.id.tvPeriodo);
            tvMonto = itemView.findViewById(R.id.tvMonto);
            checkBox = itemView.findViewById(R.id.cbSeleccion);
        }
    }
}
