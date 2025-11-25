package com.example.maquirentapp.Access;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.maquirentapp.Model.ItemValorizacion;
import com.example.maquirentapp.Model.Valorizacion;
import com.example.maquirentapp.R;

import java.util.Locale;

public class ValorizacionesAdapter extends ListAdapter<Valorizacion, ValorizacionesAdapter.ViewHolder> {

    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onClick(Valorizacion valorizacion);
    }

    private static final DiffUtil.ItemCallback<Valorizacion> DIFF_CALLBACK = new DiffUtil.ItemCallback<Valorizacion>() {
        @Override
        public boolean areItemsTheSame(@NonNull Valorizacion oldItem, @NonNull Valorizacion newItem) {
            return oldItem.getId().equals(newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Valorizacion oldItem, @NonNull Valorizacion newItem) {
            return oldItem.getTotal() == newItem.getTotal() &&
                    oldItem.getNumeroValorizacion().equals(newItem.getNumeroValorizacion());
        }
    };

    public ValorizacionesAdapter(OnItemClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_historial_valorizacion, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position), listener);
    }

    @Override
    public Valorizacion getItem(int position) {
        return super.getItem(position);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvNumero, tvFecha, tvEquipos, tvTotal;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNumero = itemView.findViewById(R.id.tvNumeroVal);
            tvFecha = itemView.findViewById(R.id.tvFecha);
            tvEquipos = itemView.findViewById(R.id.tvEquipos);
            tvTotal = itemView.findViewById(R.id.tvTotal);
        }

        public void bind(Valorizacion val, OnItemClickListener listener) {
            tvNumero.setText(val.getNumeroValorizacion());
            tvFecha.setText(val.getFechaEmision());

            StringBuilder equipos = new StringBuilder("Equipos: ");
            for (ItemValorizacion item : val.getItems()) {
                equipos.append(item.getDescripcionEquipo().split(" ")[0]).append(", ");
            }
            if (equipos.length() > 2) equipos.setLength(equipos.length() - 2);
            tvEquipos.setText(equipos.toString());

            String simbolo = "SOL".equals(val.getMoneda()) ? "S/." : "$";
            tvTotal.setText(String.format(Locale.US, "Total: %s %.2f", simbolo, val.getTotal()));

            itemView.setOnClickListener(v -> listener.onClick(val));
        }
    }
}