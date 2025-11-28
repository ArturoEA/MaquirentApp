package com.example.maquirentapp.View;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.CircularProgressDrawable;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.maquirentapp.MainActivity;
import com.example.maquirentapp.R;
import com.example.maquirentapp.Utils.ImageUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;

public class PerfilFragment extends Fragment {
    private ImageView imgFotoPerfil;
    private FloatingActionButton fabCambiarFoto;
    private EditText inputNombrePerfil;
    private TextView tvEmailPerfil;
    private View btnCambiarPassword, btnSignOut;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private StorageReference storageRef;

    private Uri imagenSeleccionada;
    private boolean nombreModificado = false;

    private ActivityResultLauncher<String> abrirGaleria;

    public PerfilFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Configurar activity launcher para seleccionar imagen
        abrirGaleria = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        imagenSeleccionada = uri;
                        imgFotoPerfil.setImageURI(uri);
                    }
                }
        );
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_perfil, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();

        initViews(view);
        setupListeners();
        cargarDatosUsuario();
    }

    @Override
    public void onResume() {
        super.onResume();
        configureGlobalFab();
    }

    @Override
    public void onPause() {
        super.onPause();
        hideGlobalFab();
    }

    private void configureGlobalFab() {
        if (getActivity() instanceof MainActivity) {
            MainActivity main = (MainActivity) getActivity();
            main.showGlobalFab(
                    "Guardar",
                    R.drawable.icon_guardar_blanco,
                    v -> guardarCambios()
            );
        }
    }

    private void hideGlobalFab() {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).hideGlobalFab();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (getActivity() instanceof com.example.maquirentapp.MainActivity) {
            ((com.example.maquirentapp.MainActivity) getActivity()).hideGlobalFab();
        } else {
            View activityFab = getActivity() != null ? getActivity().findViewById(R.id.btnGlobal) : null;
            if (activityFab != null) activityFab.setVisibility(View.GONE);
        }
    }

    private void initViews(View view) {
        imgFotoPerfil = view.findViewById(R.id.imgFotoPerfil);
        fabCambiarFoto = view.findViewById(R.id.fabCambiarFoto);
        inputNombrePerfil = view.findViewById(R.id.inputNombrePerfil);
        tvEmailPerfil = view.findViewById(R.id.tvEmailPerfil);
        btnSignOut = view.findViewById(R.id.btnSignOut);
        btnCambiarPassword = view.findViewById(R.id.cambiar_contraseña);
        ((TextView) btnCambiarPassword.findViewById(R.id.text_item_configuracion)).setText("Cambiar contraseña");
        ((ImageView) btnCambiarPassword.findViewById(R.id.icon_item_configuracion))
                .setImageResource(R.drawable.icon_password_negro);

        ((TextView) btnSignOut.findViewById(R.id.text_item_configuracion)).setText("Cerrar sesión");
        ((ImageView) btnSignOut.findViewById(R.id.icon_item_configuracion))
                .setImageResource(R.drawable.icon_cerrar_sesion_negro);

    }

    private void setupListeners() {
        // Cambiar foto
        fabCambiarFoto.setOnClickListener(v -> abrirGaleria.launch("image/*"));

        // Cambiar contraseña
        btnCambiarPassword.setOnClickListener(v -> mostrarDialogoCambiarPassword());

        // Cerrar sesión
        btnSignOut.setOnClickListener(v -> cerrarSesion());
    }

    private void cargarDatosUsuario() {
        FirebaseUser user = auth.getCurrentUser();

        if (user != null) {
            String userId = user.getUid();

            db.collection("usuarios").document(userId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String nombre = documentSnapshot.getString("nombre");
                            String email = documentSnapshot.getString("email");

                            // Obtener fotoPerfil de forma segura
                            String fotoPerfil = "";
                            try {
                                Object fotoObj = documentSnapshot.get("fotoPerfil");
                                if (fotoObj instanceof String) {
                                    fotoPerfil = (String) fotoObj;
                                }
                            } catch (Exception e) {
                                android.util.Log.e("PerfilFragment", "Error obteniendo foto", e);
                            }

                            tvEmailPerfil.setText(email);
                            inputNombrePerfil.setText(nombre);

                            // Cargar foto de perfil si existe
                            if (fotoPerfil != null && !fotoPerfil.isEmpty()) {

                                CircularProgressDrawable spinner = new CircularProgressDrawable(this.getContext());
                                spinner.setStrokeWidth(5f);
                                spinner.setCenterRadius(30f);
                                spinner.setColorSchemeColors(Color.WHITE);
                                spinner.start();

                                Glide.with(this)
                                        .load(fotoPerfil)
                                        .placeholder(spinner)
                                        .error(R.drawable.ico_voltaje_blanco)
                                        .centerCrop()
                                        .circleCrop()
                                        .into(imgFotoPerfil);
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        android.util.Log.e("PerfilFragment", "Error cargando perfil", e);
                        Toast.makeText(getContext(),
                                "Error al cargar perfil",
                                Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void guardarCambios() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(getContext(), "No estás autenticado", Toast.LENGTH_SHORT).show();
            return;
        }

        android.util.Log.d("PerfilFragment", "Usuario autenticado: " + user.getUid());
        android.util.Log.d("PerfilFragment", "Email: " + user.getEmail());

        String nuevoNombre = inputNombrePerfil.getText().toString().trim();

        if (nuevoNombre.isEmpty()) {
            Toast.makeText(getContext(), "El nombre no puede estar vacío", Toast.LENGTH_SHORT).show();
            return;
        }

        // Si hay foto nueva, subirla primero
        if (imagenSeleccionada != null) {
            android.util.Log.d("PerfilFragment", "Imagen seleccionada, procediendo a subir");
            subirFotoPerfil(user.getUid(), nuevoNombre);
        } else {
            android.util.Log.d("PerfilFragment", "Sin imagen, solo actualizando nombre");
            actualizarDatosUsuario(user.getUid(), nuevoNombre, null);
        }
    }

    private void subirFotoPerfil(String userId, String nuevoNombre) {
        if (imagenSeleccionada == null) {
            Toast.makeText(getContext(), "No hay imagen seleccionada", Toast.LENGTH_SHORT).show();
            return;
        }

        StorageReference fotoRef = storageRef.child("perfiles/" + userId + "/foto.jpg");

        byte[] dataImagen = ImageUtils.comprimirImagen(requireContext(), imagenSeleccionada);

        if (dataImagen != null) {
            fotoRef.putBytes(dataImagen)
                    .addOnProgressListener(snapshot -> {
                        double progress = (100.0 * snapshot.getBytesTransferred()) / snapshot.getTotalByteCount();
                        android.util.Log.d("PerfilFragment", "Progreso de subida: " + progress + "%");
                    })
                    .addOnSuccessListener(taskSnapshot -> {
                        android.util.Log.d("PerfilFragment", "Foto comprimida subida exitosamente");

                        fotoRef.getDownloadUrl()
                                .addOnSuccessListener(downloadUri -> {
                                    String fotoUrl = downloadUri.toString();
                                    actualizarDatosUsuario(userId, nuevoNombre, fotoUrl);
                                })
                                .addOnFailureListener(e -> {
                                    android.util.Log.e("PerfilFragment", "Error URL: " + e.getMessage());
                                    Toast.makeText(getContext(), "Error al obtener URL", Toast.LENGTH_SHORT).show();
                                });
                    })
                    .addOnFailureListener(e -> {
                        android.util.Log.e("PerfilFragment", "Error al subir: " + e.getMessage());
                        Toast.makeText(getContext(), "Error al subir foto: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } else {
            android.util.Log.e("PerfilFragment", "Error al comprimir la imagen");
            Toast.makeText(getContext(), "Error al procesar la imagen", Toast.LENGTH_SHORT).show();
        }
    }

    private void actualizarDatosUsuario(String userId, String nuevoNombre, String fotoUrl) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("nombre", nuevoNombre);

        // Solo actualizar fotoPerfil si hay una nueva foto
        if (fotoUrl != null && !fotoUrl.isEmpty()) {
            updates.put("fotoPerfil", fotoUrl);
        }

        db.collection("usuarios").document(userId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    if (getContext() == null) return;
                    Toast.makeText(getContext(),
                            "Cambios guardados correctamente",
                            Toast.LENGTH_SHORT).show();

                    nombreModificado = false;
                    inputNombrePerfil.setEnabled(false);
                    imagenSeleccionada = null;

                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(),
                            "Error al guardar cambios",
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void mostrarDialogoCambiarPassword() {
        View dialogView = LayoutInflater.from(getContext()).inflate(
                android.R.layout.simple_list_item_1, null);

        final TextInputEditText inputPasswordActual = new TextInputEditText(getContext());
        inputPasswordActual.setHint("Contraseña actual");
        inputPasswordActual.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);

        final TextInputEditText inputPasswordNueva = new TextInputEditText(getContext());
        inputPasswordNueva.setHint("Nueva contraseña");
        inputPasswordNueva.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);

        final TextInputEditText inputPasswordConfirmar = new TextInputEditText(getContext());
        inputPasswordConfirmar.setHint("Confirmar contraseña");
        inputPasswordConfirmar.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);

        LinearLayout container = new LinearLayout(getContext());
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(32, 32, 32, 32);
        container.addView(inputPasswordActual);
        container.addView(inputPasswordNueva);
        container.addView(inputPasswordConfirmar);

        new MaterialAlertDialogBuilder(requireContext(), R.style.DialogoConFuenteAnta)
                .setTitle("Cambiar Contraseña")
                .setView(container)
                .setPositiveButton("Cambiar", (dialog, which) -> {
                    String passwordActual = inputPasswordActual.getText().toString().trim();
                    String passwordNueva = inputPasswordNueva.getText().toString().trim();
                    String passwordConfirmar = inputPasswordConfirmar.getText().toString().trim();

                    cambiarPassword(passwordActual, passwordNueva, passwordConfirmar);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void cambiarPassword(String passwordActual, String passwordNueva, String passwordConfirmar) {
        if (passwordActual.isEmpty() || passwordNueva.isEmpty() || passwordConfirmar.isEmpty()) {
            Toast.makeText(getContext(), "Completa todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!passwordNueva.equals(passwordConfirmar)) {
            Toast.makeText(getContext(), "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show();
            return;
        }

        if (passwordNueva.length() < 6) {
            Toast.makeText(getContext(), "La nueva contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        // Reautenticar al usuario
        auth.signInWithEmailAndPassword(user.getEmail(), passwordActual)
                .addOnSuccessListener(authResult -> {
                    // Si la autenticación es exitosa, cambiar la contraseña
                    user.updatePassword(passwordNueva)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(getContext(),
                                        "Contraseña cambiada correctamente",
                                        Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(getContext(),
                                        "Error al cambiar contraseña",
                                        Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(),
                            "Contraseña actual incorrecta",
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void cerrarSesion() {
        new MaterialAlertDialogBuilder(requireContext(), R.style.DialogoConFuenteAnta)
                .setTitle("Cerrar Sesión")
                .setMessage("¿Estás seguro de que deseas cerrar sesión?")
                .setPositiveButton("Sí", (dialog, which) -> {
                    FirebaseAuth.getInstance().signOut();
                    Intent intent = new Intent(getActivity(), MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);

                    if (getActivity() != null) {
                        getActivity().finish();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
}