package com.example.maquirentapp.Access;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.maquirentapp.R;
import java.util.List;
import java.util.Map;

public class SpecsPlacaAdapter extends RecyclerView.Adapter<SpecsPlacaAdapter.ViewHolder> {

    private List<Map<String, String>> listaSpecs;
    private OnSpecActionListener listener;

    public interface OnSpecActionListener {
        void onEliminarSpec(Map<String, String> spec);
        void onEditarSpec(Map<String, String> spec, int position);
    }

    public SpecsPlacaAdapter(List<Map<String, String>> listaSpecs, OnSpecActionListener listener) {
        this.listaSpecs = listaSpecs;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_especificacion_placa, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map<String, String> spec = listaSpecs.get(position);

        String clave = spec.get("clave");
        String valor = spec.get("valor");

        holder.tvClave.setText(clave != null ? clave + ":" : "Dato:");
        holder.tvValor.setText(valor != null ? valor : "");

        holder.btnEliminar.setOnClickListener(v -> listener.onEliminarSpec(spec));
        holder.itemView.setOnClickListener(v -> listener.onEditarSpec(spec, position));
    }

    @Override
    public int getItemCount() {
        return listaSpecs.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvClave, tvValor;
        ImageView btnEliminar;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvClave = itemView.findViewById(R.id.tvClave);
            tvValor = itemView.findViewById(R.id.tvValor);
            btnEliminar = itemView.findViewById(R.id.btnEliminarSpec);
        }
    }
}