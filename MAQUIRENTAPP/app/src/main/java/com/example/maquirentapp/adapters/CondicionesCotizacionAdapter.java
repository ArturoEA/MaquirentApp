package com.example.maquirentapp.adapters;

import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.maquirentapp.Model.CondicionComercialItem;
import com.example.maquirentapp.R;

import java.util.ArrayList;
import java.util.List;

public class CondicionesCotizacionAdapter extends RecyclerView.Adapter<CondicionesCotizacionAdapter.ViewHolder> {

    public interface OnCondicionActionListener {
        void onEditar(int position, CondicionComercialItem item);
        void onEliminar(int position, CondicionComercialItem item);
        void onToggle(int position, CondicionComercialItem item, boolean activo);
    }

    private final List<CondicionComercialItem> lista = new ArrayList<>();
    private final OnCondicionActionListener listener;

    public CondicionesCotizacionAdapter(OnCondicionActionListener listener) {
        this.listener = listener;
    }

    public void setItems(List<CondicionComercialItem> items) {
        lista.clear();
        if (items != null) {
            for (CondicionComercialItem item : items) {
                if (item != null && item.getTexto() != null) {
                    item.setTexto(item.getTexto().replace("<b>", "").replace("</b>", "").replace("<B>", "").replace("</B>", ""));
                }
                lista.add(item);
            }
        }
        notifyDataSetChanged();
    }

    public void agregarItem(String texto) {
        String textoLimpio = texto != null ? texto.replace("<b>", "").replace("</b>", "").replace("<B>", "").replace("</B>", "") : "";
        lista.add(new CondicionComercialItem(textoLimpio, true));
        notifyItemInserted(lista.size() - 1);
    }

    public void actualizarItem(int position, String nuevoTexto) {
        if (position >= 0 && position < lista.size()) {
            String textoLimpio = nuevoTexto != null ? nuevoTexto.replace("<b>", "").replace("</b>", "").replace("<B>", "").replace("</B>", "") : "";
            lista.get(position).setTexto(textoLimpio);
            notifyItemChanged(position);
        }
    }

    public void eliminarItem(int position) {
        if (position >= 0 && position < lista.size()) {
            lista.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, lista.size() - position);
        }
    }

    public List<CondicionComercialItem> getListaItems() {
        return lista;
    }

    public List<String> getCondicionesActivas() {
        List<String> activas = new ArrayList<>();
        for (CondicionComercialItem item : lista) {
            if (item.isActivo() && item.getTexto() != null && !item.getTexto().trim().isEmpty()) {
                activas.add(item.getTexto().trim());
            }
        }
        return activas;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_condicion_comercial, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(lista.get(position), position, listener);
    }

    @Override
    public int getItemCount() {
        return lista.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        CheckBox cbCondicion;
        TextView tvCondicion;
        ImageView btnEditar;
        ImageView btnEliminar;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cbCondicion = itemView.findViewById(R.id.cbCondicion);
            tvCondicion = itemView.findViewById(R.id.tvCondicion);
            btnEditar = itemView.findViewById(R.id.btnEditarCondicion);
            btnEliminar = itemView.findViewById(R.id.btnEliminarCondicion);
        }

        public void bind(CondicionComercialItem item, int position, OnCondicionActionListener listener) {
            // Desvincular listener antes de setChecked para evitar disparos no deseados durante reciclaje
            cbCondicion.setOnCheckedChangeListener(null);
            cbCondicion.setChecked(item.isActivo());

            tvCondicion.setText(item.getTexto());
            actualizarEstiloTexto(item.isActivo());

            cbCondicion.setOnCheckedChangeListener((buttonView, isChecked) -> {
                item.setActivo(isChecked);
                actualizarEstiloTexto(isChecked);
                if (listener != null) {
                    listener.onToggle(getAdapterPosition(), item, isChecked);
                }
            });

            btnEditar.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && listener != null) {
                    listener.onEditar(pos, item);
                }
            });

            btnEliminar.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && listener != null) {
                    listener.onEliminar(pos, item);
                }
            });
        }

        private void actualizarEstiloTexto(boolean activo) {
            if (activo) {
                tvCondicion.setAlpha(1.0f);
                tvCondicion.setPaintFlags(tvCondicion.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            } else {
                tvCondicion.setAlpha(0.45f);
                tvCondicion.setPaintFlags(tvCondicion.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            }
        }
    }
}
