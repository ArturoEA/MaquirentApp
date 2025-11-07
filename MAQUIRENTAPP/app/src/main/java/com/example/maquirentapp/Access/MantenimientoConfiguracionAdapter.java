package com.example.maquirentapp.Access;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.maquirentapp.Model.MantenimientoConfiguracion;
import com.example.maquirentapp.R;

import java.util.ArrayList;
import java.util.List;

public class MantenimientoConfiguracionAdapter extends RecyclerView.Adapter<MantenimientoConfiguracionAdapter.MantenimientoConfiguracionViewHolder>{
    private List<MantenimientoConfiguracion> mantenimientosConfiguracionList;
    private Context context;
    private OnMantenimientoConfiguracionActionListener listener;

    public interface OnMantenimientoConfiguracionActionListener {
        void onEditarClick(MantenimientoConfiguracion mantenimientoConfiguracion);
        void onEliminarClick(MantenimientoConfiguracion mantenimientoConfiguracion);
    }

    public MantenimientoConfiguracionAdapter(List<MantenimientoConfiguracion> mantenimientosConfiguracionList, Context context, MantenimientoConfiguracionAdapter.OnMantenimientoConfiguracionActionListener listener) {
        this.mantenimientosConfiguracionList = mantenimientosConfiguracionList;
        this.context = context;
        this.listener = listener;
    }

    @NonNull
    @Override
    public MantenimientoConfiguracionAdapter.MantenimientoConfiguracionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_mantenimiento_configuracion, parent, false);
        return new MantenimientoConfiguracionAdapter.MantenimientoConfiguracionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MantenimientoConfiguracionAdapter.MantenimientoConfiguracionViewHolder holder, int position) {
        MantenimientoConfiguracion mantenimientoConfiguracion = mantenimientosConfiguracionList.get(position);
        holder.bind(mantenimientoConfiguracion);
    }

    @Override
    public int getItemCount() {
        return mantenimientosConfiguracionList.size();
    }

    public class MantenimientoConfiguracionViewHolder extends RecyclerView.ViewHolder {
        private ImageView imgIcono, btnEditar, btnEliminar;
        private TextView tvNombre;

        public MantenimientoConfiguracionViewHolder(@NonNull View itemView) {
            super(itemView);
            imgIcono = itemView.findViewById(R.id.imgIconoMantenimiento);
            tvNombre = itemView.findViewById(R.id.tvNombreMantenimiento);
            btnEditar = itemView.findViewById(R.id.btnEditarMantenimiento);
            btnEliminar = itemView.findViewById(R.id.btnEliminarMantenimiento);
        }

        public void bind(MantenimientoConfiguracion mantenimientoConfiguracion) {
            tvNombre.setText(mantenimientoConfiguracion.getNombre());

            // Cargar ícono
            if (mantenimientoConfiguracion.getIcono() != null && !mantenimientoConfiguracion.getIcono().isEmpty()) {
                Glide.with(context)
                        .load(mantenimientoConfiguracion.getIcono())
                        .placeholder(R.drawable.icon_mantenimiento_blanco)
                        .error(R.drawable.icon_mantenimiento_blanco)
                        .into(imgIcono);
            } else {
                imgIcono.setImageResource(R.drawable.icon_mantenimiento_blanco);
            }

            // Click en editar
            btnEditar.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEditarClick(mantenimientoConfiguracion);
                }
            });

            // Click en eliminar
            btnEliminar.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEliminarClick(mantenimientoConfiguracion);
                }
            });
        }
    }
    public void actualizarLista(List<MantenimientoConfiguracion> nuevaLista) {
        this.mantenimientosConfiguracionList = new ArrayList<>(nuevaLista);
        notifyDataSetChanged();
    }
}
