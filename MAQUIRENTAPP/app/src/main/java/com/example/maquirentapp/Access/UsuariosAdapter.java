package com.example.maquirentapp.Access;

import android.content.Context;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.example.maquirentapp.Model.Usuario;
import com.example.maquirentapp.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.HttpsCallableResult;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UsuariosAdapter extends RecyclerView.Adapter<UsuariosAdapter.UsuarioViewHolder> {
    private List<Usuario> usuariosList;
    private Context context;
    private OnUsuarioActionListener listener;
    private FirebaseFirestore db;
    private FirebaseFunctions functions;

    public interface OnUsuarioActionListener {
        void onUsuarioActualizado();
    }

    public UsuariosAdapter(List<Usuario> usuariosList, Context context, OnUsuarioActionListener listener) {
        this.usuariosList = usuariosList;
        this.context = context;
        this.listener = listener;
        this.db = FirebaseFirestore.getInstance();
        this.functions = FirebaseFunctions.getInstance();
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

            String rolTexto = "admin".equals(usuario.getRol()) ? "Administrador" : "Empleado";
            tvRol.setText("Rol: " + rolTexto);

            String estadoTexto = "pendiente".equals(usuario.getEstado()) ? "Pendiente" :
                    "inactivo".equals(usuario.getEstado()) ? "Inactivo" : "Activo";
            tvEstado.setText("Estado: " + estadoTexto);

            int colorEstado = "pendiente".equals(usuario.getEstado()) ?
                    context.getColor(android.R.color.holo_orange_dark) :
                    "inactivo".equals(usuario.getEstado()) ?
                            context.getColor(android.R.color.holo_red_dark) :
                            context.getColor(android.R.color.holo_green_dark);
            tvEstado.setTextColor(colorEstado);

            int iconoEstado = "pendiente".equals(usuario.getEstado()) ?
                    R.drawable.icon_aceite_blanco :
                    "inactivo".equals(usuario.getEstado()) ?
                            R.drawable.icon_contrato_blanco :
                            R.drawable.icon_voltaje_blanco;
            iconEstado.setImageResource(iconoEstado);

            btnCambiarRol.setOnClickListener(v -> mostrarDialogoRol(usuario));
            btnCambiarEstado.setOnClickListener(v -> mostrarDialogoEstado(usuario));
            btnEliminar.setOnClickListener(v -> mostrarDialogoConfirmarEnvio(usuario));
        }

        private void mostrarDialogoRol(Usuario usuario) {
            String[] roles = {"Empleado", "Administrador"};
            int selectedRole = "admin".equals(usuario.getRol()) ? 1 : 0;

            new MaterialAlertDialogBuilder(context)
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

            new MaterialAlertDialogBuilder(context)
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
        private void mostrarDialogoConfirmarEnvio(Usuario usuario) {
            String currentUid = FirebaseAuth.getInstance().getCurrentUser() != null ?
                    FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
            if (currentUid != null && currentUid.equals(usuario.getUid())) {
                Toast.makeText(context, "No puedes eliminar tu propia cuenta.", Toast.LENGTH_SHORT).show();
                return;
            }

            new MaterialAlertDialogBuilder(context)
                    .setTitle("Eliminar Usuario")
                    .setMessage("Se enviará un código al administrador para confirmar la eliminación de " + usuario.getNombre() + ". ¿Continuar?")
                    .setPositiveButton("Enviar código", (dialog, which) -> {
                        enviarCodigoParaEliminarUsuario(usuario);
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
        }
        private void enviarCodigoParaEliminarUsuario(Usuario usuario) {
            Map<String, Object> data = new HashMap<>();
            data.put("usuarioId", usuario.getUid());
            data.put("usuarioEmail", usuario.getEmail());

            functions.getHttpsCallable("enviarCodigoEliminacionUsuario")
                    .call(data)
                    .addOnSuccessListener((HttpsCallableResult result) -> {
                        Toast.makeText(context, "Código enviado al administrador. Revisa tu correo.", Toast.LENGTH_SHORT).show();
                        mostrarDialogoIngresarCodigo(usuario);
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(context, "Error al enviar código: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
        }
        private void mostrarDialogoIngresarCodigo(Usuario usuario) {
            final EditText input = new EditText(context);
            input.setHint("Código de 6 dígitos");
            input.setInputType(InputType.TYPE_CLASS_NUMBER);

            new MaterialAlertDialogBuilder(context)
                    .setTitle("Ingrese código de verificación")
                    .setView(input)
                    .setPositiveButton("Confirmar", (dialog, which) -> {
                        String codigoIngresado = input.getText() != null ? input.getText().toString().trim() : "";
                        if (codigoIngresado.isEmpty()) {
                            Toast.makeText(context, "Ingresa el código", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        confirmarEliminacionUsuario(usuario, codigoIngresado);
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
        }
        private void confirmarEliminacionUsuario(Usuario usuario, String codigoIngresado) {
            Map<String, Object> data = new HashMap<>();
            data.put("usuarioId", usuario.getUid());
            data.put("codigoIngresado", codigoIngresado);

            functions.getHttpsCallable("confirmarEliminacionUsuario")
                    .call(data)
                    .addOnSuccessListener((HttpsCallableResult result) -> {
                        int pos = getAbsoluteAdapterPosition();
                        if (pos >= 0 && pos < usuariosList.size()) {
                            usuariosList.remove(pos);
                            notifyItemRemoved(pos);
                        } else {
                            notifyDataSetChanged();
                        }
                        if (listener != null) listener.onUsuarioActualizado();
                        Toast.makeText(context, "Usuario eliminado correctamente", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(context, "Error al confirmar eliminación: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
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
                        Toast.makeText(context, "Error al cambiar rol", Toast.LENGTH_SHORT).show();
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
                        Toast.makeText(context, "Error al cambiar estado", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    public void actualizarLista(List<Usuario> nuevaLista) {
        usuariosList.clear();
        usuariosList.addAll(nuevaLista);
        notifyDataSetChanged();
    }

}
