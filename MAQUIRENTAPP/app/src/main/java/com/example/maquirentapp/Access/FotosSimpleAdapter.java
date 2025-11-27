package com.example.maquirentapp.Access;

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
import com.example.maquirentapp.R;
import java.util.List;

public class FotosSimpleAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TIPO_FOTO = 1;
    private static final int TIPO_AGREGAR = 2;
    private static final int MAX_FOTOS = 4;

    private List<String> listaUrls;
    private OnFotoActionListener listener;
    private Context context;

    public interface OnFotoActionListener {
        void onVerFoto(String url);
        void onEliminarFoto(String url);
        void onAgregarFoto();
    }

    public FotosSimpleAdapter(List<String> listaUrls, OnFotoActionListener listener) {
        this.listaUrls = listaUrls;
        this.listener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        if (position == listaUrls.size()) {
            return TIPO_AGREGAR;
        }
        return TIPO_FOTO;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        if (viewType == TIPO_FOTO) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_foto_mantenimiento, parent, false);
            return new FotoViewHolder(view);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.item_agregar_foto, parent, false);
            return new AgregarViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder.getItemViewType() == TIPO_FOTO) {
            FotoViewHolder fotoHolder = (FotoViewHolder) holder;
            String url = listaUrls.get(position);

            CircularProgressDrawable spinner = new CircularProgressDrawable(holder.itemView.getContext());
            spinner.setStrokeWidth(5f);
            spinner.setCenterRadius(30f);
            spinner.setColorSchemeColors(Color.WHITE);
            spinner.start();

            Glide.with(holder.itemView.getContext())
                    .load(url)
                    .placeholder(spinner)
                    .centerCrop()
                    .into(fotoHolder.imgFoto);

            fotoHolder.itemView.setOnClickListener(v -> listener.onVerFoto(url));
            fotoHolder.btnEliminar.setOnClickListener(v -> listener.onEliminarFoto(url));

        } else {
            AgregarViewHolder agregarHolder = (AgregarViewHolder) holder;
            agregarHolder.itemView.setOnClickListener(v -> listener.onAgregarFoto());
        }
    }

    @Override
    public int getItemCount() {
        if (listaUrls.size() < MAX_FOTOS) {
            return listaUrls.size() + 1;
        }
        return listaUrls.size();
    }
    static class FotoViewHolder extends RecyclerView.ViewHolder {
        ImageView imgFoto, btnEliminar;
        public FotoViewHolder(@NonNull View itemView) {
            super(itemView);
            imgFoto = itemView.findViewById(R.id.ivFotoMantenimiento);
            btnEliminar = itemView.findViewById(R.id.btnEliminarFoto);
        }
    }
    static class AgregarViewHolder extends RecyclerView.ViewHolder {
        public AgregarViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}