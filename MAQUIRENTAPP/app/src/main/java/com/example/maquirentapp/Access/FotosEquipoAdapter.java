package com.example.maquirentapp.Access;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.CircularProgressDrawable;

import com.bumptech.glide.Glide;
import com.example.maquirentapp.Model.FotoEquipo;
import com.example.maquirentapp.R;
import java.util.List;

public class FotosEquipoAdapter extends RecyclerView.Adapter<FotosEquipoAdapter.ViewHolder> {

    private List<FotoEquipo> items;
    private Context context;
    private OnFotoClickListener listener;

    public interface OnFotoClickListener {
        void onFotoClick(FotoEquipo foto);
    }

    public FotosEquipoAdapter(List<FotoEquipo> items, OnFotoClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    public void setItems(List<FotoEquipo> items) {
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
        FotoEquipo foto = items.get(position);

        CircularProgressDrawable spinner = new CircularProgressDrawable(context);
        spinner.setStrokeWidth(5f);
        spinner.setCenterRadius(30f);
        spinner.setColorSchemeColors(Color.WHITE);
        spinner.start();

        if (context instanceof Activity) {
            Activity activity = (Activity) context;
            if (activity.isDestroyed() || activity.isFinishing()) {
                return;
            }
        }

        Glide.with(context)
                .load(foto.getUrlImagen())
                .placeholder(spinner)
                .error(R.drawable.icon_generador)
                .centerCrop()
                .into(holder.imgFoto);

        holder.itemView.setOnClickListener(v -> listener.onFotoClick(foto));
    }

    @Override
    public int getItemCount() { return items.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgFoto;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgFoto = itemView.findViewById(R.id.imgPlano);
        }
    }
}