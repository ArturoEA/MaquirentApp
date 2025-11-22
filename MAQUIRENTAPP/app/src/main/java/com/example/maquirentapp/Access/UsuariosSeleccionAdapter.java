package com.example.maquirentapp.Access;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.maquirentapp.Model.Usuario;
import com.example.maquirentapp.R;
import java.util.ArrayList;
import java.util.List;

public class UsuariosSeleccionAdapter extends RecyclerView.Adapter<UsuariosSeleccionAdapter.ViewHolder> {

    private List<Usuario> usuarios;
    private List<String> seleccionados = new ArrayList<>();

    public UsuariosSeleccionAdapter(List<Usuario> usuarios) {
        this.usuarios = usuarios;
    }

    public List<String> getSeleccionados() {
        return seleccionados;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_usuario_seleccion, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Usuario u = usuarios.get(position);
        holder.tvNombre.setText(u.getNombre());

        // Cargar foto redonda con Glide
        if (u.getFotoPerfil() != null && !u.getFotoPerfil().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(u.getFotoPerfil())
                    .circleCrop()
                    .into(holder.imgPerfil);
        } else {
            holder.imgPerfil.setImageResource(R.drawable.icon_perfil_blanco);
        }

        // Manejo seguro del Checkbox en RecyclerView
        holder.checkBox.setOnCheckedChangeListener(null);
        holder.checkBox.setChecked(seleccionados.contains(u.getUid()));

        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) seleccionados.add(u.getUid());
            else seleccionados.remove(u.getUid());
        });

        holder.itemView.setOnClickListener(v -> holder.checkBox.toggle());
    }

    @Override
    public int getItemCount() { return usuarios.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvNombre;
        ImageView imgPerfil;
        CheckBox checkBox;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNombre = itemView.findViewById(R.id.tvNombreUsuario);
            imgPerfil = itemView.findViewById(R.id.imgPerfilUsuario);
            checkBox = itemView.findViewById(R.id.checkboxUsuario);
        }
    }
}