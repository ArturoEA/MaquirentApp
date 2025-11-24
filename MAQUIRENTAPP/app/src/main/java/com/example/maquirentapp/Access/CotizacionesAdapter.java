package com.example.maquirentapp.Access;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.example.maquirentapp.Model.Cotizacion;
import com.example.maquirentapp.R;
import java.util.Locale;

public class CotizacionesAdapter extends ListAdapter<Cotizacion, CotizacionesAdapter.ViewHolder> {

    private final OnCotizacionClickListener listener;

    public interface OnCotizacionClickListener {
        void onClick(Cotizacion cotizacion);
    }

    private static final DiffUtil.ItemCallback<Cotizacion> DIFF_CALLBACK = new DiffUtil.ItemCallback<Cotizacion>() {
        @Override
        public boolean areItemsTheSame(@NonNull Cotizacion oldItem, @NonNull Cotizacion newItem) {
            return oldItem.getId().equals(newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Cotizacion oldItem, @NonNull Cotizacion newItem) {
            return oldItem.getNumeroCotizacion().equals(newItem.getNumeroCotizacion()) &&
                    oldItem.getTotalGlobal() == newItem.getTotalGlobal();
        }
    };

    public CotizacionesAdapter(OnCotizacionClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_historial_cotizacion, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position), listener);
    }

    @Override
    public Cotizacion getItem(int position) {
        return super.getItem(position);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvNumero, tvCliente, tvFecha, tvTotal;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNumero = itemView.findViewById(R.id.tvNumeroCotizacion);
            tvCliente = itemView.findViewById(R.id.tvCliente);
            tvFecha = itemView.findViewById(R.id.tvFecha);
            tvTotal = itemView.findViewById(R.id.tvTotal);
        }

        public void bind(Cotizacion cotizacion, OnCotizacionClickListener listener) {
            tvNumero.setText(cotizacion.getNumeroCotizacion());
            tvCliente.setText(cotizacion.getClienteNombre());

            String fechaCorta = cotizacion.getFechaEmision();
            if (fechaCorta.contains(",")) {
                try { fechaCorta = fechaCorta.split(",")[1].trim(); } catch (Exception e) {}
            }
            tvFecha.setText(fechaCorta);

            String simbolo = "SOL".equals(cotizacion.getMoneda()) ? "S/." : "$";
            tvTotal.setText(String.format(Locale.US, "%s %.2f", simbolo, cotizacion.getTotalGlobal()));

            itemView.setOnClickListener(v -> listener.onClick(cotizacion));
        }
    }
}