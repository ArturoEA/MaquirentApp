package com.example.maquirentapp.Access;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.maquirentapp.Model.ItemCotizacion;
import com.example.maquirentapp.R;

import java.util.Locale;

public class CotizacionItemsAdapter extends ListAdapter<ItemCotizacion, CotizacionItemsAdapter.ViewHolder> {

    private final OnItemActionListener listener;

    public interface OnItemActionListener {
        void onEliminar(ItemCotizacion item);

        void onEditar(ItemCotizacion item);
    }

    private static final DiffUtil.ItemCallback<ItemCotizacion> DIFF_CALLBACK = new DiffUtil.ItemCallback<ItemCotizacion>() {
        @Override
        public boolean areItemsTheSame(@NonNull ItemCotizacion oldItem, @NonNull ItemCotizacion newItem) {
            return oldItem == newItem;
        }

        @Override
        public boolean areContentsTheSame(@NonNull ItemCotizacion oldItem, @NonNull ItemCotizacion newItem) {
            return oldItem.getDescripcionEquipo().equals(newItem.getDescripcionEquipo()) &&
                    oldItem.getPrecioMensual() == newItem.getPrecioMensual();
        }
    };

    public CotizacionItemsAdapter(OnItemActionListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_cotizacion, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position), listener);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvEquipo, tvPrecio, tvTotal;
        ImageView btnEliminar;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvEquipo = itemView.findViewById(R.id.tvEquipoDesc);
            tvPrecio = itemView.findViewById(R.id.tvPrecioParcial);
            tvTotal = itemView.findViewById(R.id.tvTotalIgv);
            btnEliminar = itemView.findViewById(R.id.btnEliminarItem);
        }

        public void bind(ItemCotizacion item, OnItemActionListener listener) {
            tvEquipo.setText(item.getDescripcionEquipo());

            tvPrecio.setText(String.format(Locale.US, "%.2f", item.getPrecioMensual()));
            tvTotal.setText(String.format(Locale.US, "Inc. IGV: %.2f", item.getTotalConIgv()));

            btnEliminar.setOnClickListener(v -> listener.onEliminar(item));
            itemView.setOnClickListener(v -> listener.onEditar(item));
        }
    }
}