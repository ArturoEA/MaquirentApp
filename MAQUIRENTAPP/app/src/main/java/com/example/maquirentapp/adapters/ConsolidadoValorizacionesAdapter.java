package com.example.maquirentapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.maquirentapp.Model.Valorizacion;
import com.example.maquirentapp.R;

import java.util.Locale;

public class ConsolidadoValorizacionesAdapter extends ListAdapter<Valorizacion, ConsolidadoValorizacionesAdapter.ViewHolder> {

    private final OnValorizacionClickListener listener;

    public interface OnValorizacionClickListener {
        void onClick(Valorizacion valorizacion);
    }

    private static final DiffUtil.ItemCallback<Valorizacion> DIFF_CALLBACK = new DiffUtil.ItemCallback<Valorizacion>() {
        @Override
        public boolean areItemsTheSame(@NonNull Valorizacion oldItem, @NonNull Valorizacion newItem) {
            return oldItem.getId() != null && oldItem.getId().equals(newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Valorizacion oldItem, @NonNull Valorizacion newItem) {
            return oldItem.getTotal() == newItem.getTotal() &&
                    (oldItem.getNumeroValorizacion() != null && oldItem.getNumeroValorizacion().equals(newItem.getNumeroValorizacion()));
        }
    };

    public ConsolidadoValorizacionesAdapter(OnValorizacionClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_consolidado_valorizacion, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position), listener);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvNumeroVal, tvFechaEmision, tvCliente, tvEquipo, tvPeriodoTareo, tvBadgeHE, tvTotal;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNumeroVal = itemView.findViewById(R.id.tvNumeroVal);
            tvFechaEmision = itemView.findViewById(R.id.tvFechaEmision);
            tvCliente = itemView.findViewById(R.id.tvCliente);
            tvEquipo = itemView.findViewById(R.id.tvEquipo);
            tvPeriodoTareo = itemView.findViewById(R.id.tvPeriodoTareo);
            tvBadgeHE = itemView.findViewById(R.id.tvBadgeHE);
            tvTotal = itemView.findViewById(R.id.tvTotal);
        }

        void bind(Valorizacion val, OnValorizacionClickListener listener) {
            tvNumeroVal.setText(val.getNumeroValorizacion() != null ? val.getNumeroValorizacion() : "-");
            tvFechaEmision.setText(val.getFechaEmision() != null ? val.getFechaEmision() : "");
            tvCliente.setText(val.getNombreCliente() != null && !val.getNombreCliente().isEmpty() ? val.getNombreCliente() : "Cliente sin nombre");

            String equipoDesc = val.getDescripcionEquipo();
            if ((equipoDesc == null || equipoDesc.isEmpty()) && val.getCodigoEquipo() != null && !val.getCodigoEquipo().isEmpty()) {
                equipoDesc = val.getCodigoEquipo();
            }
            if (equipoDesc == null || equipoDesc.isEmpty()) {
                equipoDesc = "Equipo en alquiler";
            }
            tvEquipo.setText(equipoDesc);

            String inicio = val.getFechaInicio() != null ? val.getFechaInicio() : "";
            String fin = val.getFechaFin() != null ? val.getFechaFin() : "";
            int dias = val.getDiasValorizados() > 0 ? val.getDiasValorizados() : 30;
            if (!inicio.isEmpty() && !fin.isEmpty()) {
                tvPeriodoTareo.setText("Periodo: " + inicio + " al " + fin + " (" + dias + " días)");
            } else {
                tvPeriodoTareo.setText("Días valorizados: " + dias);
            }

            if (val.isConHorasExtras() || val.getTotalHorasExtras() > 0) {
                tvBadgeHE.setVisibility(View.VISIBLE);
                if (val.getHorasExtras() > 0) {
                    tvBadgeHE.setText(String.format(Locale.US, "HE: %.1f hrs", val.getHorasExtras()));
                } else {
                    tvBadgeHE.setText("Con Horas Extras");
                }
            } else {
                tvBadgeHE.setVisibility(View.GONE);
            }

            String simbolo = (val.getMoneda() != null && (val.getMoneda().contains("USD") || val.getMoneda().contains("$"))) ? "US$ " : "S/. ";
            tvTotal.setText(String.format(Locale.US, "%s%.2f", simbolo, val.getTotal()));

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onClick(val);
                }
            });
        }
    }
}
