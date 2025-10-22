package com.example.maquirentapp.Access;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
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
    private final OnItemClickListener listener;
    public GrupoElectrogenoAdapter(OnItemClickListener listener) {
        this.listener = listener;
    }
    public void setItems(List<GrupoElectrogeno> nuevos) {
        items.clear();
        items.addAll(nuevos);
        notifyDataSetChanged();
    }
    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
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
        h.itemView.setOnClickListener(v -> listener.onItemClick(g));
    }
    @Override public int getItemCount() { return items.size(); }
    static class VH extends RecyclerView.ViewHolder {
        ImageView imgFoto;
        TextView txtCodigo;
        VH(View item) {
            super(item);
            imgFoto   = item.findViewById(R.id.imgGrupoElectrogeno);
            txtCodigo = item.findViewById(R.id.txtGrupoElectrogeno);
        }
    }
}
