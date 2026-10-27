package com.example.maquirentapp.adapters;

import android.content.Context;
import android.graphics.Color;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.maquirentapp.Model.Usuario;
import com.example.maquirentapp.R;
import com.example.maquirentapp.Repository.Callback;
import com.example.maquirentapp.Repository.UsuarioRepository;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UsuariosAdapter extends RecyclerView.Adapter<UsuariosAdapter.UsuarioViewHolder> {
    private List<Usuario> usuariosList;
    private Context context;
    private OnUsuarioActionListener listener;
    private UsuarioRepository usuarioRepository;

    public interface OnUsuarioActionListener {
        void onUsuarioActualizado();
    }

    public UsuariosAdapter(List<Usuario> usuariosList, Context context, OnUsuarioActionListener listener) {
        this.usuariosList = usuariosList;
        this.context = context;
        this.listener = listener;
        this.usuarioRepository = new UsuarioRepository();
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
        private MaterialButton btnCambiarRol, btnCambiarEstado;
        private ImageView btnEliminar;
        private ImageView iconEstado;
        private ImageView imgFotoPerfil;

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
            imgFotoPerfil = itemView.findViewById(R.id.imgFotoPerfilUsuario);
        }

        public void bind(Usuario usuario) {
            tvNombre.setText(usuario.getNombre());
            tvEmail.setText(usuario.getEmail());

            if (usuario.getFotoPerfil() != null && !usuario.getFotoPerfil().trim().isEmpty()) {
                imgFotoPerfil.setPadding(0, 0, 0, 0);
                imgFotoPerfil.setColorFilter(null);
                androidx.core.widget.ImageViewCompat.setImageTintList(imgFotoPerfil, null);
                Glide.with(context)
                        .load(usuario.getFotoPerfil())
                        .circleCrop()
                        .placeholder(R.drawable.selection_indicator_background)
                        .error(R.drawable.icon_perfil_blanco)
                        .into(imgFotoPerfil);
            } else {
                Glide.with(context).clear(imgFotoPerfil);
                int pad = (int) (8 * context.getResources().getDisplayMetrics().density);
                imgFotoPerfil.setPadding(pad, pad, pad, pad);
                imgFotoPerfil.setImageResource(R.drawable.icon_perfil_blanco);
                imgFotoPerfil.setColorFilter(ContextCompat.getColor(context, R.color.white));
                androidx.core.widget.ImageViewCompat.setImageTintList(imgFotoPerfil, null);
            }

            String rolTexto = "admin".equals(usuario.getRol()) ? "Administrador" : "Empleado";
            tvRol.setText("Rol: " + rolTexto);

            String estadoTexto = "pendiente".equals(usuario.getEstado()) ? "Pendiente" :
                    "inactivo".equals(usuario.getEstado()) ? "Inactivo" : "Activo";
            tvEstado.setText(estadoTexto);

            int colorEstado;
            if ("pendiente".equals(usuario.getEstado())) {
                colorEstado = Color.parseColor("#FFA000"); // Ámbar
            } else if ("inactivo".equals(usuario.getEstado())) {
                colorEstado = Color.parseColor("#E53935"); // Rojo
            } else {
                colorEstado = Color.parseColor("#4CAF50"); // Verde
            }
            tvEstado.setTextColor(colorEstado);
            iconEstado.setColorFilter(colorEstado);

            btnCambiarRol.setOnClickListener(v -> mostrarDialogoRol(usuario));
            btnCambiarEstado.setOnClickListener(v -> mostrarDialogoEstado(usuario));
            btnEliminar.setOnClickListener(v -> mostrarDialogoConfirmarEnvio(usuario));
        }

        private void mostrarDialogoRol(Usuario usuario) {
            String[] roles = {"Empleado", "Administrador"};
            int selectedRole = "admin".equals(usuario.getRol()) ? 1 : 0;

            new MaterialAlertDialogBuilder(context, R.style.DialogoConFuenteAnta)
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

            new MaterialAlertDialogBuilder(context, R.style.DialogoConFuenteAnta)
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
            String currentUid = usuarioRepository.getCurrentUserUid();
            if (currentUid != null && currentUid.equals(usuario.getUid())) {
                Toast.makeText(context, "No puedes eliminar tu propia cuenta.", Toast.LENGTH_SHORT).show();
                return;
            }

            new MaterialAlertDialogBuilder(context, R.style.DialogoConFuenteAnta)
                    .setTitle("Eliminar Usuario")
                    .setMessage("Se enviará un código al administrador para confirmar la eliminación de " + usuario.getNombre() + ". ¿Continuar?")
                    .setPositiveButton("Enviar código", (dialog, which) -> {
                        enviarCodigoParaEliminarUsuario(usuario);
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
        }
        private void enviarCodigoParaEliminarUsuario(Usuario usuario) {
            usuarioRepository.solicitarCodigoEliminacion(usuario.getUid(), usuario.getEmail(), new Callback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    Toast.makeText(context, "Código enviado al administrador. Revisa tu correo.", Toast.LENGTH_SHORT).show();
                    mostrarDialogoIngresarCodigo(usuario);
                }

                @Override
                public void onError(Exception e) {
                    Toast.makeText(context, "Error al enviar código: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        }
        private void mostrarDialogoIngresarCodigo(Usuario usuario) {
            final EditText input = new EditText(context);
            input.setHint("Código de 6 dígitos");
            input.setInputType(InputType.TYPE_CLASS_NUMBER);
            input.setTextColor(Color.WHITE);
            input.setHintTextColor(Color.parseColor("#888888"));
            input.setBackgroundResource(R.drawable.input_border);
            input.setPadding(40, 30, 40, 30);

            FrameLayout container = new FrameLayout(context);
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.leftMargin = 50;
            params.rightMargin = 50;
            params.topMargin = 20;
            params.bottomMargin = 10;
            input.setLayoutParams(params);
            container.addView(input);

            new MaterialAlertDialogBuilder(context, R.style.DialogoConFuenteAnta)
                    .setTitle("Ingrese código de verificación")
                    .setView(container)
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
            usuarioRepository.confirmarEliminacion(usuario.getUid(), codigoIngresado, new Callback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    int pos = getAbsoluteAdapterPosition();
                    if (pos >= 0 && pos < usuariosList.size()) {
                        usuariosList.remove(pos);
                        notifyItemRemoved(pos);
                    } else {
                        notifyDataSetChanged();
                    }
                    if (listener != null) listener.onUsuarioActualizado();
                    Toast.makeText(context, "Usuario eliminado correctamente", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onError(Exception e) {
                    Toast.makeText(context, "Error al confirmar eliminación: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        }
        private void cambiarRolUsuario(Usuario usuario, String nuevoRol) {
            usuarioRepository.actualizarRolUsuario(usuario.getUid(), nuevoRol, new Callback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    usuario.setRol(nuevoRol);
                    notifyItemChanged(getAbsoluteAdapterPosition());
                    if (listener != null) listener.onUsuarioActualizado();
                }

                @Override
                public void onError(Exception e) {
                    Toast.makeText(context, "Error al cambiar rol", Toast.LENGTH_SHORT).show();
                }
            });
        }

        private void cambiarEstadoUsuario(Usuario usuario, String nuevoEstado) {
            usuarioRepository.actualizarEstadoUsuario(usuario.getUid(), nuevoEstado, new Callback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    usuario.setEstado(nuevoEstado);
                    notifyItemChanged(getAbsoluteAdapterPosition());
                    if (listener != null) listener.onUsuarioActualizado();
                }

                @Override
                public void onError(Exception e) {
                    Toast.makeText(context, "Error al cambiar estado", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    public void actualizarLista(List<Usuario> nuevaLista) {
        usuariosList.clear();
        usuariosList.addAll(nuevaLista);
        notifyDataSetChanged();
    }

}

