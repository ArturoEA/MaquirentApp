package com.example.maquirentapp.Access;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
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
        TextView tvCliente, tvFechas, tvPrecio, tvEstado;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCliente = itemView.findViewById(R.id.tvCliente);
            tvFechas = itemView.findViewById(R.id.tvFechas);
            tvPrecio = itemView.findViewById(R.id.tvPrecio);
            tvEstado = itemView.findViewById(R.id.tvEstado);
        }

        public void bind(AlquilerDia alquiler, OnAlquilerDiaClickListener listener) {
            tvCliente.setText(alquiler.getNombreCliente());
            tvFechas.setText(String.format("Inicio: %s - Fin: %s",
                    alquiler.getFechaInicial(), alquiler.getFechaFinal()));

            String simbolo = "USD".equals(alquiler.getMoneda()) ? "$" : "S/.";
            tvPrecio.setText(String.format(Locale.US, "Monto: %s %.2f",
                    simbolo, alquiler.getPrecioTotal()));

            tvEstado.setText(alquiler.isFinalizado() ? "Estado: Finalizado" : "Estado: Activo");

            itemView.setOnClickListener(v -> listener.onAlquilerClick(alquiler));
        }
    }
}