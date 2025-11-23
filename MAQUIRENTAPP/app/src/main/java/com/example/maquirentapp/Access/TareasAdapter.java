package com.example.maquirentapp.Access;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.maquirentapp.Model.Tarea;
import com.example.maquirentapp.Model.Usuario;
import com.example.maquirentapp.R;

import java.util.Map;
import java.util.Objects;

public class TareasAdapter extends ListAdapter<Tarea, TareasAdapter.ViewHolder> {
    private Map<String, Usuario> usuariosMap;
    private OnTareaClickListener listener;
    private Context context;

    public interface OnTareaClickListener {
        void onTareaClick(Tarea tarea);
    }

    private static final DiffUtil.ItemCallback<Tarea> DIFF_CALLBACK = new DiffUtil.ItemCallback<Tarea>() {
        @Override
        public boolean areItemsTheSame(@NonNull Tarea oldItem, @NonNull Tarea newItem) {
            return oldItem.getId().equals(newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Tarea oldItem, @NonNull Tarea newItem) {
            return oldItem.getTitulo().equals(newItem.getTitulo()) &&
                    oldItem.getFechaCreacion().equals(newItem.getFechaCreacion()) &&
                    oldItem.isCompletada() == newItem.isCompletada() &&
                    Objects.equals(oldItem.getParticipantesIds(), newItem.getParticipantesIds());
        }
    };

    public TareasAdapter(OnTareaClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    public void setUsuariosMap(Map<String, Usuario> map) {
        this.usuariosMap = map;
        notifyDataSetChanged();
    }

    public void setItems(java.util.List<Tarea> nuevosItems) {
        submitList(new java.util.ArrayList<>(nuevosItems));
    }

    @Override
    public Tarea getItem(int position) {
        return super.getItem(position);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.item_tarea, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Tarea tarea = getItem(position);
        holder.bind(tarea, listener, context, usuariosMap);
    }
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitulo, tvFecha;
        ImageView ivEstado;
        LinearLayout containerParticipantes, ly_background_item;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitulo = itemView.findViewById(R.id.tvTitulo);
            tvFecha = itemView.findViewById(R.id.tvFecha);
            ivEstado = itemView.findViewById(R.id.ivEstado);
            containerParticipantes = itemView.findViewById(R.id.containerParticipantes);
            ly_background_item = itemView.findViewById(R.id.ly_backgroudn_card);
        }

        public void bind(Tarea tarea, OnTareaClickListener listener, Context context, Map<String, Usuario> usuariosMap) {
            itemView.setTranslationX(0f);
            itemView.setAlpha(1f);

            tvTitulo.setText(tarea.getTitulo());
            tvFecha.setText(tarea.getFechaCreacion());

            if (tarea.isCompletada()) {
                ly_background_item.setBackgroundColor(ContextCompat.getColor(context, R.color.green_accent));
                ivEstado.setColorFilter(ContextCompat.getColor(context, R.color.white));
                containerParticipantes.setVisibility(View.VISIBLE);

                containerParticipantes.removeAllViews();

                if (tarea.getParticipantesIds() != null && usuariosMap != null) {
                    for (String uid : tarea.getParticipantesIds()) {
                        Usuario u = usuariosMap.get(uid);
                        if (u != null) {
                            ImageView img = new ImageView(context);
                            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(80, 80);
                            params.setMargins(0, 0, 15, 0);
                            img.setLayoutParams(params);

                            // Cargar con Glide
                            if (u.getFotoPerfil() != null && !u.getFotoPerfil().isEmpty()) {
                                Glide.with(context).load(u.getFotoPerfil()).circleCrop().into(img);
                            } else {
                                img.setImageResource(R.drawable.icon_perfil_blanco);
                                img.setColorFilter(ContextCompat.getColor(context, R.color.black));
                            }

                            containerParticipantes.addView(img);
                        }
                    }
                }

            } else {
                ly_background_item.setBackgroundColor(ContextCompat.getColor(context, R.color.red_accent));
                ivEstado.setColorFilter(ContextCompat.getColor(context, R.color.red_accent));
                containerParticipantes.setVisibility(View.GONE);
            }

            itemView.setOnClickListener(v -> listener.onTareaClick(tarea));
        }
    }
}