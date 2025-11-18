package com.example.maquirentapp.Access;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.maquirentapp.Model.AlquilerDia;
import com.example.maquirentapp.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AlquilerDiarioAdapter extends RecyclerView.Adapter<AlquilerDiarioAdapter.ViewHolder> {

    private List<AlquilerDia> items = new ArrayList<>();
    private OnAlquilerDiaClickListener listener;

    public interface OnAlquilerDiaClickListener {
        void onAlquilerClick(AlquilerDia alquiler);
    }

    public AlquilerDiarioAdapter(OnAlquilerDiaClickListener listener) {
        this.listener = listener;
    }

    public void setItems(List<AlquilerDia> nuevos) {
        this.items.clear();
        this.items.addAll(nuevos);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_alquiler_diario, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AlquilerDia alquiler = items.get(position);
        holder.bind(alquiler, listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvCliente, tvPrecio, tvEstado, txtUbicacion,
                txtHorasInicio, txtHorasFinal, txtFechaInicial, txtFechaFinal;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCliente = itemView.findViewById(R.id.tvCliente);
            tvPrecio = itemView.findViewById(R.id.tvPrecio);
            tvEstado = itemView.findViewById(R.id.tvEstado);
            txtUbicacion = itemView.findViewById(R.id.txtUbicacion);
            txtHorasInicio = itemView.findViewById(R.id.txtHorasInicio);
            txtHorasFinal = itemView.findViewById(R.id.txtHorasFinal);
            txtFechaFinal = itemView.findViewById(R.id.txtFechaFinal);
            txtFechaInicial = itemView.findViewById(R.id.txtFechaInicial);

        }

        public void bind(AlquilerDia alquiler, OnAlquilerDiaClickListener listener) {
            tvCliente.setText(alquiler.getNombreCliente());
            txtUbicacion.setText(alquiler.getUbicacion());

            txtFechaInicial.setText(alquiler.getFechaInicial());
            txtHorasInicio.setText(String.format(Locale.US, "%.1f horas", alquiler.getHorometroInicial()));

            if (alquiler.getFechaFinal() != null && !alquiler.getFechaFinal().isEmpty()) {
                txtFechaFinal.setText(alquiler.getFechaFinal());
            } else {
                txtFechaFinal.setText("---");
            }

            if (alquiler.getHorometroFinal() > 0) {
                txtHorasFinal.setText(String.format(Locale.US, "%.1f horas", alquiler.getHorometroFinal()));
            } else {
                txtHorasFinal.setText("---");
            }

            String simbolo = "USD".equals(alquiler.getMoneda()) ? "$" : "S/.";
            tvPrecio.setText(String.format(Locale.US, "Monto: %s %.2f",
                    simbolo, alquiler.getPrecioTotal()));

            // Lógica de estado
            if (alquiler.isFinalizado()) {
                tvEstado.setText("Finalizado");
                tvEstado.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.green_accent));
            } else {
                tvEstado.setText("Activo");
                tvEstado.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.yellow_accent));
            }

            itemView.setOnClickListener(v -> listener.onAlquilerClick(alquiler));
        }
    }
}