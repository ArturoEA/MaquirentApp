package com.example.maquirentapp.Access;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.maquirentapp.Model.ItemValorizacion;
import com.example.maquirentapp.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SeleccionValorizacionAdapter extends RecyclerView.Adapter<SeleccionValorizacionAdapter.ViewHolder> {

    private List<ItemValorizacion> items = new ArrayList<>();
    private List<ItemValorizacion> seleccionados = new ArrayList<>();

    public void setItems(List<ItemValorizacion> items) {
        this.items = items;
        this.seleccionados = new ArrayList<>(items);
        notifyDataSetChanged();
    }

    public List<ItemValorizacion> getSeleccionados() {
        return seleccionados;
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

        holder.tvEquipo.setText(item.getDescripcionEquipo());
        holder.tvPeriodo.setText("Período: " + item.getFechaInicio() + " al " + item.getFechaFin());

        holder.tvMonto.setText(String.format(Locale.US, "%.2f", item.getTotalItem()));

        holder.checkBox.setOnCheckedChangeListener(null);
        holder.checkBox.setChecked(seleccionados.contains(item));

        holder.checkBox.setOnCheckedChangeListener((v, isChecked) -> {
            if (isChecked) seleccionados.add(item);
            else seleccionados.remove(item);
        });

        holder.itemView.setOnClickListener(v -> holder.checkBox.toggle());
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvEquipo, tvPeriodo, tvMonto;
        CheckBox checkBox;

        ViewHolder(View itemView) {
            super(itemView);
            tvEquipo = itemView.findViewById(R.id.tvEquipo);
            tvPeriodo = itemView.findViewById(R.id.tvPeriodo);
            tvMonto = itemView.findViewById(R.id.tvMonto);
            checkBox = itemView.findViewById(R.id.cbSeleccion);
        }
    }
}