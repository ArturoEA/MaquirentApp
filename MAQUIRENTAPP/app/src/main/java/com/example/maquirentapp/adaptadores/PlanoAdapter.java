package com.example.maquirentapp.adaptadores;

import android.content.Context;
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

public class PlanoAdapter extends RecyclerView.Adapter<PlanoAdapter.ViewHolder> {

    private List<Plano> items;
    private Context context;
    private OnPlanoClickListener listener;

    public interface OnPlanoClickListener {
        void onPlanoClick(Plano plano);
    }

    public PlanoAdapter(List<Plano> items, OnPlanoClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    public void setItems(List<Plano> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.item_plano, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Plano plano = items.get(position);
        Glide.with(context)
                .load(plano.getUrlImagen())
                .placeholder(R.drawable.ico_voltaje_blanco)
                .centerCrop()
                .into(holder.imgPlano);

        holder.itemView.setOnClickListener(v -> listener.onPlanoClick(plano));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgPlano;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgPlano = itemView.findViewById(R.id.imgPlano);
        }
    }
}