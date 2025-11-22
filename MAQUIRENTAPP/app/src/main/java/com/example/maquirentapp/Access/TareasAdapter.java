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
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.maquirentapp.Model.Tarea;
import com.example.maquirentapp.Model.Usuario;
import com.example.maquirentapp.R;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TareasAdapter extends RecyclerView.Adapter<TareasAdapter.ViewHolder> {

    private List<Tarea> lista = new ArrayList<>();
    private Map<String, Usuario> usuariosMap;
    private OnTareaClickListener listener;
    private Context context;

    public interface OnTareaClickListener {
        void onTareaClick(Tarea tarea);
    }

    public TareasAdapter(OnTareaClickListener listener) {
        this.listener = listener;
    }

    public void setItems(List<Tarea> items) {
        this.lista = items;
        notifyDataSetChanged();
    }

    public void setUsuariosMap(Map<String, Usuario> map) {
        this.usuariosMap = map;
        notifyDataSetChanged();
    }
    public Tarea getItem(int position) {
        return lista.get(position);
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
        Tarea tarea = lista.get(position);
        holder.bind(tarea, listener, context, usuariosMap);
    }

    @Override
    public int getItemCount() { return lista.size(); }

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
            tvTitulo.setText(tarea.getTitulo());
            tvFecha.setText(tarea.getFechaCreacion());

            // 1. Configurar Colores y Estado
            if (tarea.isCompletada()) {
                // Verde
                ly_background_item.setBackgroundColor(ContextCompat.getColor(context, R.color.green_accent));
                ivEstado.setColorFilter(ContextCompat.getColor(context, R.color.white));
                containerParticipantes.setVisibility(View.VISIBLE);

                // 2. Llenar fotos de participantes
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
                ly_background_item.setBackgroundColor(ContextCompat.getColor(context, R.    color.red_accent));
                ivEstado.setColorFilter(ContextCompat.getColor(context, R.color.red_accent));
                containerParticipantes.setVisibility(View.GONE);
            }

            itemView.setOnClickListener(v -> listener.onTareaClick(tarea));
        }
    }
}