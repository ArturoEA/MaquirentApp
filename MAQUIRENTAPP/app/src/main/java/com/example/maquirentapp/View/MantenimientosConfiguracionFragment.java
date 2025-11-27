package com.example.maquirentapp.View;

import android.app.Dialog;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.maquirentapp.Access.MantenimientoConfiguracionAdapter;
import com.example.maquirentapp.Model.MantenimientoConfiguracion;
import com.example.maquirentapp.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;

public class MantenimientosConfiguracionFragment extends Fragment {
    private static final String TAG = "MantenimientosConfiguracion";
    private RecyclerView recyclerView;
    private LinearLayout emptyState;
    private MantenimientoConfiguracionAdapter adapter;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private List<MantenimientoConfiguracion> mantenimientosConfiguracionList = new ArrayList<>();
    private ActivityResultLauncher<String> imagePickerLauncher;
    private Uri selectedImageUri;
    private ImageView dialogImagePreview;

    public MantenimientosConfiguracionFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Configurar launcher para seleccionar imagen
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        selectedImageUri = uri;
                        if (dialogImagePreview != null) {
                            dialogImagePreview.setImageURI(uri);
                        }
                    }
                });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_mantenimientos_configuracion, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeFirebase();
        initViews(view);
        setupRecyclerView();
        cargarMantenimientosConfiguracion();
    }

    private void initializeFirebase() {
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        hideGlobalFab();
    }

    private void configureGlobalFab() {
        if (getActivity() instanceof com.example.maquirentapp.MainActivity) {
            com.example.maquirentapp.MainActivity main = (com.example.maquirentapp.MainActivity) getActivity();
            main.showGlobalFab("Añadir", R.drawable.icon_nuevo_blanco, v -> mostrarDialogoNuevoMantenimientoConfiguracion());
        }
    }

    private void hideGlobalFab() {
        if (getActivity() instanceof com.example.maquirentapp.MainActivity) {
            ((com.example.maquirentapp.MainActivity) getActivity()).hideGlobalFab();
        }
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerViewMantenimientosConfiguracion);
        emptyState = view.findViewById(R.id.emptyState);
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new MantenimientoConfiguracionAdapter(mantenimientosConfiguracionList, getContext(),
                new MantenimientoConfiguracionAdapter.OnMantenimientoConfiguracionActionListener() {
                    @Override
                    public void onEditarClick(MantenimientoConfiguracion mantenimientoConfiguracion) {
                        mostrarDialogoEditarMantenimientoConfiguracion(mantenimientoConfiguracion);
                    }

                    @Override
                    public void onEliminarClick(MantenimientoConfiguracion mantenimientoConfiguracion) {
                        confirmarEliminacion(mantenimientoConfiguracion);
                    }
                });
        recyclerView.setAdapter(adapter);
    }

    private void cargarMantenimientosConfiguracion() {
        Log.d(TAG, "Cargando mantenimientos configuracion...");

        db.collection("mantenimientos_configuracion")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        mantenimientosConfiguracionList.clear();

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            MantenimientoConfiguracion mantenimientoConfiguracion = document.toObject(MantenimientoConfiguracion.class);
                            mantenimientoConfiguracion.setId(document.getId());
                            mantenimientosConfiguracionList.add(mantenimientoConfiguracion);
                        }

                        Log.d(TAG, "Mantenimientos configuracion cargados: " + mantenimientosConfiguracionList.size());
                        actualizarUI();
                    } else {
                        Log.e(TAG, "Error cargando mantenimientos configuracion", task.getException());
                        mostrarError("Error al cargar mantenimientos configuracion");
                    }
                });
    }

    private void actualizarUI() {
        if (recyclerView != null) {
            recyclerView.post(() -> {
                if (mantenimientosConfiguracionList.isEmpty()) {
                    emptyState.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                } else {
                    emptyState.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                    adapter.actualizarLista(mantenimientosConfiguracionList);
                }
            });
        }
    }

    // ===== CREAR NUEVO MANTENIMIENTO CONFIGURACION =====

    private void mostrarDialogoNuevoMantenimientoConfiguracion() {
        Dialog dialog = crearDialogoMantenimientoConfiguracion(null);
        dialog.show();
    }

    // ===== EDITAR MANTENIMIENTO CONFIGURACION =====

    private void mostrarDialogoEditarMantenimientoConfiguracion(MantenimientoConfiguracion mantenimientoConfiguracion) {
        Dialog dialog = crearDialogoMantenimientoConfiguracion(mantenimientoConfiguracion);
        dialog.show();
    }

    private Dialog crearDialogoMantenimientoConfiguracion(@Nullable MantenimientoConfiguracion mantenimientoConfiguracionExistente) {
        Dialog dialog = new Dialog(requireContext());
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_nuevo_mantenimiento_configuracion, null);
        dialog.setContentView(dialogView);

        TextView tvTitulo = dialogView.findViewById(R.id.tvTituloDialog);
        dialogImagePreview = dialogView.findViewById(R.id.imgIconoPreview);
        MaterialButton btnSeleccionarIcono = dialogView.findViewById(R.id.btnSeleccionarIcono);
        TextInputEditText inputNombre = dialogView.findViewById(R.id.inputNombreMantenimientoConfiguracion);
        MaterialButton btnCancelar = dialogView.findViewById(R.id.btnCancelarDialog);
        MaterialButton btnGuardar = dialogView.findViewById(R.id.btnGuardarDialog);

        boolean esEdicion = mantenimientoConfiguracionExistente != null;

        // Configurar para edición
        if (esEdicion) {
            tvTitulo.setText("Editar Item Mantenimiento");
            inputNombre.setText(mantenimientoConfiguracionExistente.getNombre());
            cargarIconoExistente(mantenimientoConfiguracionExistente.getIcono());
        } else {
            tvTitulo.setText("Nuevo Item Mantenimiento");
        }

        selectedImageUri = null;

        btnSeleccionarIcono.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));

        btnCancelar.setOnClickListener(v -> {
            selectedImageUri = null;
            dialog.dismiss();
        });

        btnGuardar.setOnClickListener(v -> {
            String nombre = inputNombre.getText() != null ?
                    inputNombre.getText().toString().trim() : "";

            if (nombre.isEmpty()) {
                mostrarError("Ingresa el nombre del mantenimiento");
                return;
            }

            deshabilitarBoton(btnGuardar, "Guardando...");

            if (esEdicion) {
                actualizarMantenimientoConfiguracion(mantenimientoConfiguracionExistente, nombre, dialog, btnGuardar);
            } else {
                crearNuevoMantenimientoConfiguracion(nombre, dialog, btnGuardar);
            }
        });

        return dialog;
    }

    private void cargarIconoExistente(String iconoUrl) {
        if (iconoUrl != null && !iconoUrl.isEmpty() && dialogImagePreview != null) {
            try {
                Glide.with(requireContext())
                        .load(iconoUrl)
                        .placeholder(R.drawable.icon_mantenimiento_blanco)
                        .error(R.drawable.icon_mantenimiento_blanco)
                        .into(dialogImagePreview);
            } catch (Exception e) {
                dialogImagePreview.setImageResource(R.drawable.icon_mantenimiento_blanco);
            }
        }
    }

    private void crearNuevoMantenimientoConfiguracion(String nombre, Dialog dialog, MaterialButton btnGuardar) {
        if (selectedImageUri == null) {
            mostrarError("Selecciona un ícono");
            habilitarBoton(btnGuardar, "Guardar");
            return;
        }

        subirIconoYCrear(nombre, dialog, btnGuardar);
    }

    private void subirIconoYCrear(String nombre, Dialog dialog, MaterialButton btnGuardar) {
        String fileName = "mantenimientos_configuracion/" + System.currentTimeMillis() + ".png";
        StorageReference storageRef = storage.getReference().child(fileName);

        storageRef.putFile(selectedImageUri)
                .addOnSuccessListener(taskSnapshot ->
                        storageRef.getDownloadUrl()
                                .addOnSuccessListener(downloadUri -> {
                                    MantenimientoConfiguracion mantenimientoConfiguracion = new MantenimientoConfiguracion(
                                            nombre,
                                            downloadUri.toString()
                                    );

                                    guardarEnFirestore(mantenimientoConfiguracion, dialog, btnGuardar);
                                })
                                .addOnFailureListener(e -> {
                                    mostrarError("Error obteniendo URL del ícono");
                                    habilitarBoton(btnGuardar, "Guardar");
                                })
                )
                .addOnFailureListener(e -> {
                    mostrarError("Error subiendo ícono: " + e.getMessage());
                    habilitarBoton(btnGuardar, "Guardar");
                });
    }

    private void guardarEnFirestore(MantenimientoConfiguracion mantenimientoConfiguracion, Dialog dialog, MaterialButton btnGuardar) {
        db.collection("mantenimientos_configuracion")
                .add(mantenimientoConfiguracion)
                .addOnSuccessListener(documentReference -> {
                    mostrarExito("Mantenimiento creado exitosamente");
                    cerrarDialogo(dialog);
                    cargarMantenimientosConfiguracion();
                })
                .addOnFailureListener(e -> {
                    mostrarError("Error al guardar: " + e.getMessage());
                    habilitarBoton(btnGuardar, "Guardar");
                });
    }

    private void actualizarMantenimientoConfiguracion(MantenimientoConfiguracion mantenimientoConfiguracion, String nuevoNombre,
                                     Dialog dialog, MaterialButton btnGuardar) {
        if (selectedImageUri != null) {
            // Si hay nuevo ícono, subirlo y actualizar
            subirIconoYActualizar(mantenimientoConfiguracion, nuevoNombre, dialog, btnGuardar);
        } else {
            // Solo actualizar nombre
            actualizarSoloNombre(mantenimientoConfiguracion, nuevoNombre, dialog, btnGuardar);
        }
    }

    private void subirIconoYActualizar(MantenimientoConfiguracion mantenimientoConfiguracion, String nuevoNombre,
                                       Dialog dialog, MaterialButton btnGuardar) {
        if (mantenimientoConfiguracion.getIcono() != null && !mantenimientoConfiguracion.getIcono().isEmpty()) {
            try {
                StorageReference oldIconRef = storage.getReferenceFromUrl(mantenimientoConfiguracion.getIcono());
                oldIconRef.delete()
                        .addOnSuccessListener(aVoid -> Log.d(TAG, "Ícono anterior eliminado correctamente"))
                        .addOnFailureListener(e -> Log.e(TAG, "Error eliminando ícono anterior", e));
            } catch (Exception e) {
                Log.e(TAG, "Error procesando URL del ícono anterior", e);
            }
        }

        String fileName = "mantenimientos_configuracion/" + System.currentTimeMillis() + ".png";
        StorageReference storageRef = storage.getReference().child(fileName);

        storageRef.putFile(selectedImageUri)
                .addOnSuccessListener(taskSnapshot ->
                        storageRef.getDownloadUrl()
                                .addOnSuccessListener(downloadUri -> {
                                    db.collection("mantenimientos_configuracion").document(mantenimientoConfiguracion.getId())
                                            .update(
                                                    "nombre", nuevoNombre,
                                                    "icono", downloadUri.toString()
                                            )
                                            .addOnSuccessListener(aVoid -> {
                                                mostrarExito("Mantenimiento actualizado");
                                                cerrarDialogo(dialog);
                                                cargarMantenimientosConfiguracion();
                                            })
                                            .addOnFailureListener(e -> {
                                                mostrarError("Error actualizando: " + e.getMessage());
                                                habilitarBoton(btnGuardar, "Guardar");
                                            });
                                })
                                .addOnFailureListener(e -> {
                                    mostrarError("Error obteniendo URL: " + e.getMessage());
                                    habilitarBoton(btnGuardar, "Guardar");
                                })
                )
                .addOnFailureListener(e -> {
                    mostrarError("Error subiendo ícono: " + e.getMessage());
                    habilitarBoton(btnGuardar, "Guardar");
                });
    }

    private void actualizarSoloNombre(MantenimientoConfiguracion mantenimientoConfiguracion, String nuevoNombre,
                                      Dialog dialog, MaterialButton btnGuardar) {
        db.collection("mantenimientos_configuracion").document(mantenimientoConfiguracion.getId())
                .update("nombre", nuevoNombre)
                .addOnSuccessListener(aVoid -> {
                    mostrarExito("Mantenimiento actualizado");
                    cerrarDialogo(dialog);
                    cargarMantenimientosConfiguracion();
                })
                .addOnFailureListener(e -> {
                    mostrarError("Error actualizando: " + e.getMessage());
                    habilitarBoton(btnGuardar, "Guardar");
                });
    }

    // ===== ELIMINAR MANTENIMIENTO CONFIGURACION =====

    private void confirmarEliminacion(MantenimientoConfiguracion mantenimientoConfiguracion) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Eliminar Mantenimiento")
                .setMessage("¿Estás seguro de que deseas eliminar \"" + mantenimientoConfiguracion.getNombre() + "\"?")
                .setPositiveButton("Eliminar", (dialog, which) -> eliminarMantenimientoConfiguracion(mantenimientoConfiguracion))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void eliminarMantenimientoConfiguracion(MantenimientoConfiguracion mantenimientoConfiguracion) {
        db.collection("mantenimientos_configuracion").document(mantenimientoConfiguracion.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    mostrarExito("Mantenimiento eliminado");

                    if (mantenimientoConfiguracion.getIcono() != null && !mantenimientoConfiguracion.getIcono().isEmpty()) {
                        eliminarIconoStorage(mantenimientoConfiguracion.getIcono());
                    }

                    cargarMantenimientosConfiguracion();
                })
                .addOnFailureListener(e -> {
                    mostrarError("Error al eliminar: " + e.getMessage());
                });
    }

    private void eliminarIconoStorage(String iconoUrl) {
        try {
            StorageReference iconoRef = storage.getReferenceFromUrl(iconoUrl);
            iconoRef.delete()
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Ícono eliminado de Storage"))
                    .addOnFailureListener(e -> Log.e(TAG, "Error eliminando ícono", e));
        } catch (Exception e) {
            Log.e(TAG, "Error procesando URL del ícono", e);
        }
    }

    // ===== HELPERS =====

    private void deshabilitarBoton(MaterialButton btn, String texto) {
        btn.setEnabled(false);
        btn.setText(texto);
    }

    private void habilitarBoton(MaterialButton btn, String texto) {
        btn.setEnabled(true);
        btn.setText(texto);
    }

    private void cerrarDialogo(Dialog dialog) {
        selectedImageUri = null;
        dialog.dismiss();
    }

    private void mostrarError(String mensaje) {
        if (getContext() != null) {
            Toast.makeText(getContext(), mensaje, Toast.LENGTH_SHORT).show();
        }
    }

    private void mostrarExito(String mensaje) {
        if (getContext() != null) {
            Toast.makeText(getContext(), mensaje, Toast.LENGTH_SHORT).show();
        }
    }
}