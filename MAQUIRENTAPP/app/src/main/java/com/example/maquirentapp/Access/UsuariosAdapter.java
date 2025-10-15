package com.example.maquirentapp.Access;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;
import com.example.maquirentapp.Model.Usuario;
import com.example.maquirentapp.R;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class UsuariosAdapter extends RecyclerView.Adapter<UsuariosAdapter.UsuarioViewHolder> {
    private List<Usuario> usuariosList;
    private Context context;
    private OnUsuarioActionListener listener;
    private FirebaseFirestore db;

    public interface OnUsuarioActionListener {
        void onUsuarioActualizado();
    }

    public UsuariosAdapter(List<Usuario> usuariosList, Context context, OnUsuarioActionListener listener) {
        this.usuariosList = usuariosList;
        this.context = context;
        this.listener = listener;
        this.db = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public UsuarioViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_usuario, parent, false);
        return new UsuarioViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UsuarioViewHolder holder, int position) {
        Usuario usuario = usuariosList.get(position);
        holder.bind(usuario);
    }

    @Override
    public int getItemCount() {
        return usuariosList.size();
    }

    public class UsuarioViewHolder extends RecyclerView.ViewHolder {
        private TextView tvNombre, tvEmail, tvRol, tvEstado;
        private MaterialButton btnCambiarRol, btnCambiarEstado, btnEliminar;
        private ImageView iconEstado;

        public UsuarioViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNombre = itemView.findViewById(R.id.tvNombreUsuario);
            tvEmail = itemView.findViewById(R.id.tvEmailUsuario);
            tvRol = itemView.findViewById(R.id.tvRolUsuario);
            tvEstado = itemView.findViewById(R.id.tvEstadoUsuario);
            btnCambiarRol = itemView.findViewById(R.id.btnCambiarRol);
            btnCambiarEstado = itemView.findViewById(R.id.btnCambiarEstado);
            btnEliminar = itemView.findViewById(R.id.btnEliminar);
            iconEstado = itemView.findViewById(R.id.iconEstado);
        }

        public void bind(Usuario usuario) {
            tvNombre.setText(usuario.getNombre());
            tvEmail.setText(usuario.getEmail());

            // Mostrar rol
            String rolTexto = "admin".equals(usuario.getRol()) ? "Administrador" : "Empleado";
            tvRol.setText("Rol: " + rolTexto);

            // Mostrar estado con color
            String estadoTexto = "pendiente".equals(usuario.getEstado()) ? "Pendiente" :
                    "inactivo".equals(usuario.getEstado()) ? "Inactivo" : "Activo";
            tvEstado.setText("Estado: " + estadoTexto);

            // Cambiar color del estado
            int colorEstado = "pendiente".equals(usuario.getEstado()) ?
                    context.getColor(android.R.color.holo_orange_dark) :
                    "inactivo".equals(usuario.getEstado()) ?
                            context.getColor(android.R.color.holo_red_dark) :
                            context.getColor(android.R.color.holo_green_dark);
            tvEstado.setTextColor(colorEstado);

            // Cambiar icono de estado
//            int iconoEstado = "pendiente".equals(usuario.getEstado()) ?
//                    R.drawable.icon_reloj :
//                    "inactivo".equals(usuario.getEstado()) ?
//                            R.drawable.icon_cancelado :
//                            R.drawable.icon_check;
            int iconoEstado = "pendiente".equals(usuario.getEstado()) ?
                    R.drawable.icon_aceite_blanco :
                    "inactivo".equals(usuario.getEstado()) ?
                            R.drawable.icon_contrato_blanco :
                            R.drawable.icon_voltaje_blanco;
            iconEstado.setImageResource(iconoEstado);

            // Cambiar rol
            btnCambiarRol.setOnClickListener(v -> mostrarDialogoRol(usuario));

            // Cambiar estado
            btnCambiarEstado.setOnClickListener(v -> mostrarDialogoEstado(usuario));

            // Eliminar usuario
            btnEliminar.setOnClickListener(v -> mostrarDialogoEliminar(usuario));
        }

        private void mostrarDialogoRol(Usuario usuario) {
            String[] roles = {"Empleado", "Administrador"};
            int selectedRole = "admin".equals(usuario.getRol()) ? 1 : 0;

            new AlertDialog.Builder(context)
                    .setTitle("Cambiar Rol - " + usuario.getNombre())
                    .setSingleChoiceItems(roles, selectedRole, (dialog, which) -> {
                        String nuevoRol = which == 0 ? "empleado" : "admin";
                        if (!nuevoRol.equals(usuario.getRol())) {
                            cambiarRolUsuario(usuario, nuevoRol);
                        }
                        dialog.dismiss();
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
        }

        private void mostrarDialogoEstado(Usuario usuario) {
            String[] estados = {"Activo", "Pendiente", "Inactivo"};
            int selectedEstado = "pendiente".equals(usuario.getEstado()) ? 1 :
                    "inactivo".equals(usuario.getEstado()) ? 2 : 0;

            new AlertDialog.Builder(context)
                    .setTitle("Cambiar Estado - " + usuario.getNombre())
                    .setSingleChoiceItems(estados, selectedEstado, (dialog, which) -> {
                        String nuevoEstado = which == 0 ? "activo" :
                                which == 1 ? "pendiente" : "inactivo";
                        if (!nuevoEstado.equals(usuario.getEstado())) {
                            cambiarEstadoUsuario(usuario, nuevoEstado);
                        }
                        dialog.dismiss();
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
        }

        private void mostrarDialogoEliminar(Usuario usuario) {
            new AlertDialog.Builder(context)
                    .setTitle("Eliminar Usuario")
                    .setMessage("¿Estás seguro de que deseas eliminar a " + usuario.getNombre() + "? Esta acción no se puede deshacer.")
                    .setPositiveButton("Eliminar", (dialog, which) -> {
                        eliminarUsuario(usuario);
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
        }

        private void cambiarRolUsuario(Usuario usuario, String nuevoRol) {
            db.collection("usuarios").document(usuario.getUid())
                    .update("rol", nuevoRol)
                    .addOnSuccessListener(aVoid -> {
                        usuario.setRol(nuevoRol);
                        notifyItemChanged(getAbsoluteAdapterPosition());
                        if (listener != null) listener.onUsuarioActualizado();
                    })
                    .addOnFailureListener(e -> {
                        android.widget.Toast.makeText(context,
                                "Error al cambiar rol",
                                android.widget.Toast.LENGTH_SHORT).show();
                    });
        }

        private void cambiarEstadoUsuario(Usuario usuario, String nuevoEstado) {
            db.collection("usuarios").document(usuario.getUid())
                    .update("estado", nuevoEstado)
                    .addOnSuccessListener(aVoid -> {
                        usuario.setEstado(nuevoEstado);
                        notifyItemChanged(getAbsoluteAdapterPosition());
                        if (listener != null) listener.onUsuarioActualizado();
                    })
                    .addOnFailureListener(e -> {
                        android.widget.Toast.makeText(context,
                                "Error al cambiar estado",
                                android.widget.Toast.LENGTH_SHORT).show();
                    });
        }

        private void eliminarUsuario(Usuario usuario) {
            // Primero eliminar de Authentication
            db.collection("usuarios").document(usuario.getUid())
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        usuariosList.remove(getAbsoluteAdapterPosition());
                        notifyItemRemoved(getAbsoluteAdapterPosition());
                        if (listener != null) listener.onUsuarioActualizado();
                        android.widget.Toast.makeText(context,
                                "Usuario eliminado correctamente",
                                android.widget.Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        android.widget.Toast.makeText(context,
                                "Error al eliminar usuario",
                                android.widget.Toast.LENGTH_SHORT).show();
                    });
        }
    }

    public void actualizarLista(List<Usuario> nuevaLista) {
        usuariosList.clear();
        usuariosList.addAll(nuevaLista);
        notifyDataSetChanged();
    }
}