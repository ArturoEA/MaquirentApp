package com.example.maquirentapp.View;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.maquirentapp.Access.AccesoriosAdapter;
import com.example.maquirentapp.Model.Accesorio;
import com.example.maquirentapp.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;

public class AccesoriosAlquilerDiarioFragment extends Fragment {

    private static final String TAG = "AccesoriosDiario";
    private static final String TIPO_ACCESORIO = "diario";
    private RecyclerView recyclerView;
    private LinearLayout emptyState;
    private AccesoriosAdapter adapter;
    private FirebaseFirestore db;
    private FirebaseStorage storage;

    private List<Accesorio> accesoriosList = new ArrayList<>();

    private ActivityResultLauncher<String> imagePickerLauncher;
    private Uri selectedImageUri;
    private ImageView dialogImagePreview;

    public AccesoriosAlquilerDiarioFragment() {
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
        return inflater.inflate(R.layout.fragment_accesorios_alquiler_diario, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeFirebase();
        initViews(view);
        setupRecyclerView();
        cargarAccesorios();
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
            main.showGlobalFab("Añadir", R.drawable.icon_nuevo_blanco, v -> mostrarDialogoNuevoAccesorio());
        }
    }

    private void hideGlobalFab() {
        if (getActivity() instanceof com.example.maquirentapp.MainActivity) {
            ((com.example.maquirentapp.MainActivity) getActivity()).hideGlobalFab();
        }
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerViewAccesorios);
        emptyState = view.findViewById(R.id.emptyState);
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new AccesoriosAdapter(accesoriosList, getContext(),
                new AccesoriosAdapter.OnAccesorioActionListener() {
                    @Override
                    public void onEditarClick(Accesorio accesorio) {
                        mostrarDialogoEditarAccesorio(accesorio);
                    }

                    @Override
                    public void onEliminarClick(Accesorio accesorio) {
                        confirmarEliminacion(accesorio);
                    }
                });
        recyclerView.setAdapter(adapter);
    }

    private void cargarAccesorios() {
        Log.d(TAG, "Cargando accesorios...");

        db.collection("accesorios")
                .whereEqualTo("tipo", TIPO_ACCESORIO)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        accesoriosList.clear();

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Accesorio accesorio = document.toObject(Accesorio.class);
                            accesorio.setId(document.getId());
                            accesoriosList.add(accesorio);
                        }

                        Log.d(TAG, "Accesorios cargados: " + accesoriosList.size());
                        actualizarUI();
                    } else {
                        Log.e(TAG, "Error cargando accesorios", task.getException());
                        mostrarError("Error al cargar accesorios");
                    }
                });
    }

    private void actualizarUI() {
        if (recyclerView != null) {
            recyclerView.post(() -> {
                if (accesoriosList.isEmpty()) {
                    emptyState.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                } else {
                    emptyState.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                    adapter.actualizarLista(accesoriosList);
                }
            });
        }
    }

    // ===== CREAR NUEVO ACCESORIO =====

    private void mostrarDialogoNuevoAccesorio() {
        Dialog dialog = crearDialogoAccesorio(null);
        dialog.show();
    }

    // ===== EDITAR ACCESORIO =====

    private void mostrarDialogoEditarAccesorio(Accesorio accesorio) {
        Dialog dialog = crearDialogoAccesorio(accesorio);
        dialog.show();
    }

    private Dialog crearDialogoAccesorio(@Nullable Accesorio accesorioExistente) {
        Dialog dialog = new Dialog(requireContext());
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_nuevo_accesorio, null);
        dialog.setContentView(dialogView);


        if (dialog.getWindow() != null) {

            Window window = dialog.getWindow();
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

            int margen = (int) (24 * getResources().getDisplayMetrics().density); // 24dp
            window.getDecorView().setPadding(margen, margen, margen, margen);
        }

        TextView tvTitulo = dialogView.findViewById(R.id.tvTituloDialog);
        dialogImagePreview = dialogView.findViewById(R.id.imgIconoPreview);
        MaterialButton btnSeleccionarIcono = dialogView.findViewById(R.id.btnSeleccionarIcono);
        TextInputEditText inputNombre = dialogView.findViewById(R.id.inputNombreAccesorio);
        MaterialButton btnCancelar = dialogView.findViewById(R.id.btnCancelarDialog);
        MaterialButton btnGuardar = dialogView.findViewById(R.id.btnGuardarDialog);

        boolean esEdicion = accesorioExistente != null;

        // Configurar para edición
        if (esEdicion) {
            tvTitulo.setText("Editar Accesorio");
            inputNombre.setText(accesorioExistente.getNombre());
            cargarIconoExistente(accesorioExistente.getIcono());
        } else {
            tvTitulo.setText("Nuevo Accesorio");
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
                mostrarError("Ingresa el nombre del accesorio");
                return;
            }

            deshabilitarBoton(btnGuardar, "Guardando...");

            if (esEdicion) {
                actualizarAccesorio(accesorioExistente, nombre, dialog, btnGuardar);
            } else {
                crearNuevoAccesorio(nombre, dialog, btnGuardar);
            }
        });

        return dialog;
    }

    private void cargarIconoExistente(String iconoUrl) {
        if (iconoUrl != null && !iconoUrl.isEmpty() && dialogImagePreview != null) {
            try {
                Glide.with(requireContext())
                        .load(iconoUrl)
                        .placeholder(R.drawable.icon_kit_blanco)
                        .error(R.drawable.icon_kit_blanco)
                        .into(dialogImagePreview);
            } catch (Exception e) {
                dialogImagePreview.setImageResource(R.drawable.icon_kit_blanco);
            }
        }
    }

    private void crearNuevoAccesorio(String nombre, Dialog dialog, MaterialButton btnGuardar) {
        if (selectedImageUri == null) {
            mostrarError("Selecciona un ícono");
            habilitarBoton(btnGuardar, "Guardar");
            return;
        }

        subirIconoYCrear(nombre, dialog, btnGuardar);
    }

    private void subirIconoYCrear(String nombre, Dialog dialog, MaterialButton btnGuardar) {
        String fileName = "accesorios/" + TIPO_ACCESORIO + "/" + System.currentTimeMillis() + ".png";
        StorageReference storageRef = storage.getReference().child(fileName);

        storageRef.putFile(selectedImageUri)
                .addOnSuccessListener(taskSnapshot ->
                        storageRef.getDownloadUrl()
                                .addOnSuccessListener(downloadUri -> {
                                    Accesorio nuevoAccesorio = new Accesorio(
                                            nombre,
                                            downloadUri.toString(),
                                            TIPO_ACCESORIO
                                    );

                                    guardarEnFirestore(nuevoAccesorio, dialog, btnGuardar);
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

    private void guardarEnFirestore(Accesorio accesorio, Dialog dialog, MaterialButton btnGuardar) {
        db.collection("accesorios")
                .add(accesorio)
                .addOnSuccessListener(documentReference -> {
                    mostrarExito("Accesorio creado exitosamente");
                    cerrarDialogo(dialog);
                    cargarAccesorios();
                })
                .addOnFailureListener(e -> {
                    mostrarError("Error al guardar: " + e.getMessage());
                    habilitarBoton(btnGuardar, "Guardar");
                });
    }

    private void actualizarAccesorio(Accesorio accesorio, String nuevoNombre,
                                     Dialog dialog, MaterialButton btnGuardar) {
        if (selectedImageUri != null) {
            // Si hay nuevo ícono, subirlo y actualizar
            subirIconoYActualizar(accesorio, nuevoNombre, dialog, btnGuardar);
        } else {
            // Solo actualizar nombre
            actualizarSoloNombre(accesorio, nuevoNombre, dialog, btnGuardar);
        }
    }

    private void subirIconoYActualizar(Accesorio accesorio, String nuevoNombre,
                                       Dialog dialog, MaterialButton btnGuardar) {
        if (accesorio.getIcono() != null && !accesorio.getIcono().isEmpty()) {
            try {
                StorageReference oldIconRef = storage.getReferenceFromUrl(accesorio.getIcono());
                oldIconRef.delete()
                        .addOnSuccessListener(aVoid -> Log.d(TAG, "Ícono anterior eliminado correctamente"))
                        .addOnFailureListener(e -> Log.e(TAG, "Error eliminando ícono anterior", e));
            } catch (Exception e) {
                Log.e(TAG, "Error procesando URL del ícono anterior", e);
            }
        }

        String fileName = "accesorios/" + TIPO_ACCESORIO + "/" + System.currentTimeMillis() + ".png";
        StorageReference storageRef = storage.getReference().child(fileName);

        storageRef.putFile(selectedImageUri)
                .addOnSuccessListener(taskSnapshot ->
                        storageRef.getDownloadUrl()
                                .addOnSuccessListener(downloadUri -> {
                                    db.collection("accesorios").document(accesorio.getId())
                                            .update(
                                                    "nombre", nuevoNombre,
                                                    "icono", downloadUri.toString()
                                            )
                                            .addOnSuccessListener(aVoid -> {
                                                mostrarExito("Accesorio actualizado");
                                                cerrarDialogo(dialog);
                                                cargarAccesorios();
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

    private void actualizarSoloNombre(Accesorio accesorio, String nuevoNombre,
                                      Dialog dialog, MaterialButton btnGuardar) {
        db.collection("accesorios").document(accesorio.getId())
                .update("nombre", nuevoNombre)
                .addOnSuccessListener(aVoid -> {
                    mostrarExito("Accesorio actualizado");
                    cerrarDialogo(dialog);
                    cargarAccesorios();
                })
                .addOnFailureListener(e -> {
                    mostrarError("Error actualizando: " + e.getMessage());
                    habilitarBoton(btnGuardar, "Guardar");
                });
    }

    // ===== ELIMINAR ACCESORIO =====

    private void confirmarEliminacion(Accesorio accesorio) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Eliminar Accesorio")
                .setMessage("¿Estás seguro de que deseas eliminar \"" + accesorio.getNombre() + "\"?")
                .setPositiveButton("Eliminar", (dialog, which) -> eliminarAccesorio(accesorio))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void eliminarAccesorio(Accesorio accesorio) {
        db.collection("accesorios").document(accesorio.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    mostrarExito("Accesorio eliminado");

                    if (accesorio.getIcono() != null && !accesorio.getIcono().isEmpty()) {
                        eliminarIconoStorage(accesorio.getIcono());
                    }

                    cargarAccesorios();
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