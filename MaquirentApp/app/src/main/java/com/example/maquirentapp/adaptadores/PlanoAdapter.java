package com.example.maquirentapp.adaptadores;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.maquirentapp.Model.Plano;
import com.example.maquirentapp.R;
import java.util.List;

public class PlanoAdapter extends RecyclerView.Adapter<PlanoAdapter.PlanoViewHolder> {

    private List<Plano> planos;
    private OnPlanoClickListener listener;

    public interface OnPlanoClickListener {
        void onPlanoClick(Plano plano);
        void onDeleteClick(Plano plano);
    }

    public PlanoAdapter(List<Plano> planos, OnPlanoClickListener listener) {
        this.planos = planos;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PlanoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_plano, parent, false);
        return new PlanoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PlanoViewHolder holder, int position) {
        Plano plano = planos.get(position);
        holder.bind(plano, listener);

        if (plano.getImageUrl().startsWith("drawable/")) {
            int resourceId = holder.itemView.getContext().getResources()
                    .getIdentifier(plano.getImageUrl().replace("drawable/", ""),
                            "drawable", holder.itemView.getContext().getPackageName());
            Glide.with(holder.itemView.getContext())
                    .load(resourceId)
                    .thumbnail(0.1f)
                    .into(holder.imgPlano);
        } else {
            Glide.with(holder.itemView.getContext())
                    .load(Uri.parse(plano.getImageUrl()))
                    .thumbnail(0.1f)
                    .into(holder.imgPlano);
        }
    }

    @Override
    public int getItemCount() {
        return planos != null ? planos.size() : 0;
    }

    public void updateList(List<Plano> newList) {
        planos = newList;
        notifyDataSetChanged();
    }

    public void addPlano(Plano plano) {
        planos.add(plano);
        notifyItemInserted(planos.size() - 1);
    }

    public void removePlano(Plano plano) {
        int position = planos.indexOf(plano);
        if (position != -1) {
            planos.remove(position);
            notifyItemRemoved(position);
        }
    }

    static class PlanoViewHolder extends RecyclerView.ViewHolder {
        private final ImageView imgPlano;

        public PlanoViewHolder(@NonNull View itemView) {
            super(itemView);
            imgPlano = itemView.findViewById(R.id.imgPlano);
        }

        public void bind(final Plano plano, final OnPlanoClickListener listener) {
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPlanoClick(plano); // Dispara la vista previa
                }
            });

            itemView.setOnLongClickListener(v -> {
                if (listener != null) {
                    listener.onDeleteClick(plano);
                }
                return true;
            });
        }
    }
}