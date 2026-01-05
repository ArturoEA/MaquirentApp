package com.example.maquirentapp.Access;

import android.content.Context;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.example.maquirentapp.Model.GrupoElectrogeno;
import com.example.maquirentapp.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GruposElectrogenosConfiguracionAdapter extends RecyclerView.Adapter<GruposElectrogenosConfiguracionAdapter.GrupoViewHolder> {
    private static final String TAG = "ADAPTER_Grupos";
    private List<GrupoElectrogeno> gruposList = new ArrayList<>();
    private Context context;
    private OnGrupoActionListener listener;
    private final OnStartDragListener dragListener;
    public interface OnStartDragListener {
        void onRequestDrag(RecyclerView.ViewHolder viewHolder);
    }
    public interface OnGrupoActionListener {
        void onEditarClick(GrupoElectrogeno grupo);
        void onEliminarClick(GrupoElectrogeno grupo);
    }

    public GruposElectrogenosConfiguracionAdapter(List<GrupoElectrogeno> gruposList, Context context, OnGrupoActionListener listener, OnStartDragListener dragListener) {
        this.gruposList = gruposList != null ? gruposList : new ArrayList<>();
        this.context = context;
        this.listener = listener;
        this.dragListener = dragListener;
    }
    public List<GrupoElectrogeno> getListaActual() {
        return gruposList;
    }
    public void onItemMove(int fromPosition, int toPosition) {
        if (fromPosition < toPosition) {
            for (int i = fromPosition; i < toPosition; i++) {
                Collections.swap(gruposList, i, i + 1);
            }
        } else {
            for (int i = fromPosition; i > toPosition; i--) {
                Collections.swap(gruposList, i, i - 1);
            }
        }
        notifyItemMoved(fromPosition, toPosition);
    }
    @NonNull
    @Override
    public GrupoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_grupo_electrogeno_configuracion, parent, false);
        return new GrupoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GrupoViewHolder holder, int position) {
        if (position < 0 || position >= gruposList.size()) {
            return;
        }
        GrupoElectrogeno grupo = gruposList.get(position);
        holder.bind(grupo);
    }

    @Override
    public int getItemCount() {
        return gruposList == null ? 0 : gruposList.size();
    }

    public class GrupoViewHolder extends RecyclerView.ViewHolder {
        private ImageView imgGrupo, btnEditar, btnEliminar, btnOrdenar;
        private TextView tvCodigoGrupo;

        public GrupoViewHolder(@NonNull View itemView) {
            super(itemView);
            imgGrupo = itemView.findViewById(R.id.imgGrupo);
            tvCodigoGrupo = itemView.findViewById(R.id.tvCodigoGrupo);
            btnEditar = itemView.findViewById(R.id.btnEditar);
            btnEliminar = itemView.findViewById(R.id.btnEliminar);
            btnOrdenar = itemView.findViewById(R.id.btnOrdenar);
        }

        public void bind(GrupoElectrogeno grupo) {
            String codigo = grupo != null && grupo.getCodigo() != null ? grupo.getCodigo() : "(sin código)";
            tvCodigoGrupo.setText(codigo);

            // carga segura de imagen (uso del contexto del ImageView)
            try {
                String foto = grupo != null ? grupo.getFoto() : null;
                if (foto != null && !foto.isEmpty()) {
                    Glide.with(imgGrupo.getContext())
                            .load(foto)
                            .placeholder(R.drawable.icon_generador)
                            .error(R.drawable.icon_generador)
                            .transform(new CenterCrop(), new RoundedCorners(25))
                            .into(imgGrupo);
                } else {
                    imgGrupo.setImageResource(R.drawable.icon_generador);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error cargando imagen Glide: " + e.getMessage(), e);
                imgGrupo.setImageResource(R.drawable.icon_generador);
            }

            // listeners
            btnEditar.setOnClickListener(v -> {
                if (listener != null) listener.onEditarClick(grupo);
            });

            btnEliminar.setOnClickListener(v -> {
                if (listener != null) listener.onEliminarClick(grupo);
            });
            btnOrdenar.setOnTouchListener((v, event) -> {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    if (dragListener != null) {
                        dragListener.onRequestDrag(this);
                    }
                }
                return false;
            });
        }
    }
    public void actualizarLista(final List<GrupoElectrogeno> nuevaLista) {
        if (nuevaLista == null) {
            return;
        }

        if (Looper.myLooper() != Looper.getMainLooper()) {
            new android.os.Handler(Looper.getMainLooper()).post(() -> {
                gruposList.clear();
                gruposList.addAll(nuevaLista);
                notifyDataSetChanged();
            });
        } else {
            gruposList.clear();
            gruposList.addAll(nuevaLista);
            notifyDataSetChanged();
        }
    }
}
