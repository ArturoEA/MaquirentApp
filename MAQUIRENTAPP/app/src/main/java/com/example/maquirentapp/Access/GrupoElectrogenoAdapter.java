package com.example.maquirentapp.Access;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.maquirentapp.Model.GrupoElectrogeno;
import com.example.maquirentapp.R;

import java.util.ArrayList;
import java.util.List;

public class GrupoElectrogenoAdapter extends RecyclerView.Adapter<GrupoElectrogenoAdapter.VH> {
    public interface OnItemClickListener {
        void onItemClick(GrupoElectrogeno grupo);
    }
    private final List<GrupoElectrogeno> items = new ArrayList<>();
    private final List<GrupoElectrogeno> itemsOriginales = new ArrayList<>();
    private final OnItemClickListener listener;
    private Context context;
    public GrupoElectrogenoAdapter(OnItemClickListener listener) {
        this.listener = listener;
    }
    public void setItems(List<GrupoElectrogeno> nuevos) {
        items.clear();
        items.addAll(nuevos);
        itemsOriginales.clear();
        itemsOriginales.addAll(nuevos);
        notifyDataSetChanged();
    }
    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        this.context = parent.getContext();
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_generador_cge, parent, false);
        return new VH(v);

    }
    @Override public void onBindViewHolder(@NonNull VH h, int pos) {
        GrupoElectrogeno g = items.get(pos);
        h.txtCodigo.setText(g.getCodigo());
        Glide.with(h.imgFoto.getContext())
                .load(g.getFoto())
                .into(h.imgFoto);
        if (g.isEliminado()) {
            h.cardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.background_dark));
            h.txtEstado.setText("Eliminado");
            h.txtEstado.setVisibility(View.VISIBLE);
        } else {
            h.cardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.card_dark));
            h.txtEstado.setVisibility(View.GONE);
        }
        h.itemView.setOnClickListener(v -> listener.onItemClick(g));
    }
    @Override public int getItemCount() { return items.size(); }
    public void filtrar(String texto) {
        items.clear();
        if (texto.isEmpty()) {
            items.addAll(itemsOriginales);
        } else {
            texto = texto.toLowerCase().trim();
            for (GrupoElectrogeno grupo : itemsOriginales) {
                if (grupo.getCodigo().toLowerCase().contains(texto)) {
                    items.add(grupo);
                }
            }
        }
        notifyDataSetChanged();
    }
    static class VH extends RecyclerView.ViewHolder {
        ImageView imgFoto;
        TextView txtCodigo;
        TextView txtEstado;
        CardView cardView;
        VH(View item) {
            super(item);
            imgFoto   = item.findViewById(R.id.imgGrupoElectrogeno);
            txtCodigo = item.findViewById(R.id.txtGrupoElectrogeno);
            txtEstado = item.findViewById(R.id.tvEstadoGenerador);
            cardView  = item.findViewById(R.id.cardGrupoElectrogeno);
        }
    }
}
