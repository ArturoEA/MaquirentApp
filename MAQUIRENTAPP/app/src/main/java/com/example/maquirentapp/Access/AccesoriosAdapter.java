package com.example.maquirentapp.Access;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.maquirentapp.Model.Accesorio;
import com.example.maquirentapp.R;

import java.util.ArrayList;
import java.util.List;

public class AccesoriosAdapter extends RecyclerView.Adapter<AccesoriosAdapter.AccesorioViewHolder> {
    private List<Accesorio> accesoriosList;
    private Context context;
    private OnAccesorioActionListener listener;

    public interface OnAccesorioActionListener {
        void onEditarClick(Accesorio accesorio);
        void onEliminarClick(Accesorio accesorio);
    }

    public AccesoriosAdapter(List<Accesorio> accesoriosList, Context context, OnAccesorioActionListener listener) {
        this.accesoriosList = accesoriosList;
        this.context = context;
        this.listener = listener;
    }

    @NonNull
    @Override
    public AccesorioViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_accesorio, parent, false);
        return new AccesorioViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AccesorioViewHolder holder, int position) {
        Accesorio accesorio = accesoriosList.get(position);
        holder.bind(accesorio);
    }

    @Override
    public int getItemCount() {
        return accesoriosList.size();
    }

    public class AccesorioViewHolder extends RecyclerView.ViewHolder {
        private ImageView imgIcono, btnEditar, btnEliminar;
        private TextView tvNombre;

        public AccesorioViewHolder(@NonNull View itemView) {
            super(itemView);
            imgIcono = itemView.findViewById(R.id.imgIconoAccesorio);
            tvNombre = itemView.findViewById(R.id.tvNombreAccesorio);
            btnEditar = itemView.findViewById(R.id.btnEditarAccesorio);
            btnEliminar = itemView.findViewById(R.id.btnEliminarAccesorio);
        }

        public void bind(Accesorio accesorio) {
            tvNombre.setText(accesorio.getNombre());

            // Cargar ícono
            if (accesorio.getIcono() != null && !accesorio.getIcono().isEmpty()) {
                Glide.with(context)
                        .load(accesorio.getIcono())
                        .placeholder(R.drawable.icon_kit_blanco)
                        .error(R.drawable.icon_kit_blanco)
                        .into(imgIcono);
            } else {
                imgIcono.setImageResource(R.drawable.icon_kit_blanco);
            }

            // Click en editar
            btnEditar.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEditarClick(accesorio);
                }
            });

            // Click en eliminar
            btnEliminar.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEliminarClick(accesorio);
                }
            });
        }
    }

    public void actualizarLista(List<Accesorio> nuevaLista) {
        this.accesoriosList = new ArrayList<>(nuevaLista);
        notifyDataSetChanged();
    }
}