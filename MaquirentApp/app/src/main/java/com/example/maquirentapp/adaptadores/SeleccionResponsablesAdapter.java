package com.example.maquirentapp.adaptadores;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.maquirentapp.Model.Usuario;
import com.example.maquirentapp.R;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SeleccionResponsablesAdapter extends RecyclerView.Adapter<SeleccionResponsablesAdapter.ResponsableViewHolder> {

    public interface OnSeleccionChangeListener {
        void onSeleccionChange();
    }

    private final List<Usuario> usuarios = new ArrayList<>();
    private final Set<String> seleccionadosIds = new HashSet<>();
    private OnSeleccionChangeListener listener;

    public SeleccionResponsablesAdapter() {
    }

    public SeleccionResponsablesAdapter(OnSeleccionChangeListener listener) {
        this.listener = listener;
    }

    public void setOnSeleccionChangeListener(OnSeleccionChangeListener listener) {
        this.listener = listener;
    }

    public void submitList(List<Usuario> nuevosUsuarios) {
        usuarios.clear();
        seleccionadosIds.clear();
        if (nuevosUsuarios != null) {
            usuarios.addAll(nuevosUsuarios);
        }
        notifyDataSetChanged();
        if (listener != null) {
            listener.onSeleccionChange();
        }
    }

    public List<Usuario> obtenerSeleccionados() {
        List<Usuario> seleccionados = new ArrayList<>();
        for (Usuario usuario : usuarios) {
            if (usuario.getUid() != null && seleccionadosIds.contains(usuario.getUid())) {
                seleccionados.add(usuario);
            }
        }
        return seleccionados;
    }

    @NonNull
    @Override
    public ResponsableViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_responsable_selector, parent, false);
        return new ResponsableViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ResponsableViewHolder holder, int position) {
        Usuario usuario = usuarios.get(position);
        boolean seleccionado = usuario.getUid() != null && seleccionadosIds.contains(usuario.getUid());
        holder.bind(usuario, seleccionado);
    }

    @Override
    public int getItemCount() {
        return usuarios.size();
    }

    class ResponsableViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCardView cardUsuario;
        private final ShapeableImageView imagenUsuario;
        private final TextView nombreUsuario;

        ResponsableViewHolder(@NonNull View itemView) {
            super(itemView);
            cardUsuario = itemView.findViewById(R.id.cardUsuarioResponsable);
            imagenUsuario = itemView.findViewById(R.id.imageUsuarioResponsable);
            nombreUsuario = itemView.findViewById(R.id.textNombreUsuarioResponsable);
        }

        void bind(Usuario usuario, boolean seleccionado) {
            String nombre = usuario.getNombre();
            if (nombre == null || nombre.trim().isEmpty()) {
                nombre = usuario.getEmail();
            }
            if (nombre == null) {
                nombre = "";
            }
            nombreUsuario.setText(nombre);

            Glide.with(imagenUsuario.getContext())
                    .load(usuario.getFotoUrl())
                    .placeholder(R.drawable.icon_perfil_blanco)
                    .error(R.drawable.icon_perfil_blanco)
                    .centerCrop()
                    .into(imagenUsuario);

            int colorSeleccionado = ContextCompat.getColor(cardUsuario.getContext(), R.color.responsable_seleccionado);
            int colorNormal = ContextCompat.getColor(cardUsuario.getContext(), R.color.white);
            cardUsuario.setCardBackgroundColor(seleccionado ? colorSeleccionado : colorNormal);
            cardUsuario.setStrokeColor(ContextCompat.getColor(cardUsuario.getContext(), R.color.gray_light));

            cardUsuario.setOnClickListener(v -> {
                String uid = usuario.getUid();
                if (uid == null) {
                    return;
                }
                if (seleccionadosIds.contains(uid)) {
                    seleccionadosIds.remove(uid);
                } else {
                    seleccionadosIds.add(uid);
                }
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    notifyItemChanged(position);
                }
                if (listener != null) {
                    listener.onSeleccionChange();
                }
            });
        }
    }
}
