package com.example.maquirentapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.maquirentapp.Model.Cotizacion;
import com.example.maquirentapp.Model.ItemCotizacion;
import com.example.maquirentapp.R;

import java.util.Locale;

public class CotizacionItemsAdapter extends ListAdapter<ItemCotizacion, CotizacionItemsAdapter.ViewHolder> {

    private final OnItemActionListener listener;
    private String tipoAlquiler = Cotizacion.TIPO_MENSUAL;
    private int diasAlquiler = 1;

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
                    oldItem.getPrecioUnitario() == newItem.getPrecioUnitario() &&
                    oldItem.getPrecioHoraExtra() == newItem.getPrecioHoraExtra();
        }
    };

    public CotizacionItemsAdapter(OnItemActionListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    public void setParametrosModalidad(String tipoAlquiler, int diasAlquiler) {
        this.tipoAlquiler = tipoAlquiler != null ? tipoAlquiler : Cotizacion.TIPO_MENSUAL;
        this.diasAlquiler = diasAlquiler > 0 ? diasAlquiler : 1;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_cotizacion, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position), listener, tipoAlquiler, diasAlquiler);
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

        public void bind(ItemCotizacion item, OnItemActionListener listener, String tipoAlquiler, int diasAlquiler) {
            tvEquipo.setText(item.getDescripcionEquipo());

            double subtotal = item.getSubtotal(tipoAlquiler, diasAlquiler);
            double totalConIgv = item.getTotalConIgv(tipoAlquiler, diasAlquiler);

            if (Cotizacion.TIPO_DIARIO.equalsIgnoreCase(tipoAlquiler)) {
                tvPrecio.setText(String.format(Locale.US, "Tarifa día: %.2f x %d d = %.2f", item.getPrecioUnitario(), diasAlquiler, subtotal));
            } else {
                tvPrecio.setText(String.format(Locale.US, "Tarifa mes: %.2f", item.getPrecioUnitario()));
            }

            tvTotal.setText(String.format(Locale.US, "Inc. IGV: %.2f", totalConIgv));

            btnEliminar.setOnClickListener(v -> listener.onEliminar(item));
            itemView.setOnClickListener(v -> listener.onEditar(item));
        }
    }
}
