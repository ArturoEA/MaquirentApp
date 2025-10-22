package com.example.maquirentapp.adaptadores;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.maquirentapp.Model.ResponsableAsignado;
import com.example.maquirentapp.Model.Tarea;
import com.example.maquirentapp.R;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.ArrayList;
import java.util.List;

public class TareasAdapter extends RecyclerView.Adapter<TareasAdapter.TareaViewHolder> {

    public interface OnTareaInteractionListener {
        void onSolicitarCompletar(Tarea tarea);
    }

    private final List<Tarea> tareas = new ArrayList<>();
    private final OnTareaInteractionListener listener;

    public TareasAdapter(OnTareaInteractionListener listener) {
        this.listener = listener;
    }

    public void submitList(List<Tarea> nuevasTareas) {
        tareas.clear();
        if (nuevasTareas != null) {
            tareas.addAll(nuevasTareas);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TareaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_tarea, parent, false);
        return new TareaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TareaViewHolder holder, int position) {
        Tarea tarea = tareas.get(position);
        holder.bind(tarea);
    }

    @Override
    public int getItemCount() {
        return tareas.size();
    }

    class TareaViewHolder extends RecyclerView.ViewHolder {

        private final MaterialCardView cardTarea;
        private final CheckBox checkBoxTarea;
        private final TextView textViewNombreTarea;
        private final LinearLayout containerResponsables;

        TareaViewHolder(@NonNull View itemView) {
            super(itemView);
            cardTarea = itemView.findViewById(R.id.cardTarea);
            checkBoxTarea = itemView.findViewById(R.id.checkBoxTarea);
            textViewNombreTarea = itemView.findViewById(R.id.textViewNombreTarea);
            containerResponsables = itemView.findViewById(R.id.containerResponsables);
        }

        void bind(Tarea tarea) {
            textViewNombreTarea.setText(tarea.getTitulo());

            boolean completada = tarea.isCompletada();
            checkBoxTarea.setOnCheckedChangeListener(null);
            checkBoxTarea.setChecked(completada);
            checkBoxTarea.setEnabled(!completada);

            int colorCompletada = ContextCompat.getColor(itemView.getContext(), R.color.task_check_completada);
            int colorPendiente = ContextCompat.getColor(itemView.getContext(), R.color.gray_dark);
            checkBoxTarea.setButtonTintList(ColorStateList.valueOf(completada ? colorCompletada : colorPendiente));

            int backgroundColor = ContextCompat.getColor(itemView.getContext(),
                    completada ? R.color.task_completada_background : R.color.white);
            cardTarea.setCardBackgroundColor(backgroundColor);
            int strokeColor = ContextCompat.getColor(itemView.getContext(),
                    completada ? R.color.task_check_completada : R.color.gray_light);
            cardTarea.setStrokeColor(strokeColor);
            textViewNombreTarea.setTextColor(ContextCompat.getColor(itemView.getContext(),
                    completada ? R.color.task_check_completada : R.color.text_primary));

            itemView.setOnClickListener(v -> {
                if (!tarea.isCompletada() && listener != null) {
                    listener.onSolicitarCompletar(tarea);
                }
            });

            checkBoxTarea.setOnClickListener(v -> {
                if (tarea.isCompletada()) {
                    return;
                }
                checkBoxTarea.setChecked(false);
                if (listener != null) {
                    listener.onSolicitarCompletar(tarea);
                }
            });

            renderResponsables(tarea.getResponsables(), completada);
        }

        private void renderResponsables(List<ResponsableAsignado> responsables, boolean completada) {
            containerResponsables.removeAllViews();
            if (!completada || responsables == null || responsables.isEmpty()) {
                containerResponsables.setVisibility(View.GONE);
                return;
            }

            LayoutInflater inflater = LayoutInflater.from(containerResponsables.getContext());
            for (ResponsableAsignado responsable : responsables) {
                View avatarView = inflater.inflate(R.layout.item_responsable_avatar, containerResponsables, false);
                ShapeableImageView imagenAvatar = avatarView.findViewById(R.id.imageAvatarResponsable);
                TextView nombreAvatar = avatarView.findViewById(R.id.textNombreAvatarResponsable);

                Glide.with(imagenAvatar.getContext())
                        .load(responsable.getFotoUrl())
                        .placeholder(R.drawable.icon_perfil_blanco)
                        .error(R.drawable.icon_perfil_blanco)
                        .centerCrop()
                        .into(imagenAvatar);

                nombreAvatar.setText(responsable.getNombre() != null ? responsable.getNombre() : "");
                containerResponsables.addView(avatarView);
            }
            containerResponsables.setVisibility(View.VISIBLE);
        }
    }
}
