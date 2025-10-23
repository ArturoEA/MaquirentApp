package com.example.maquirentapp.View;

import android.app.Dialog;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
import com.example.maquirentapp.Access.GruposElectrogenosConfiguracionAdapter;
import com.example.maquirentapp.Model.GrupoElectrogeno;
import com.example.maquirentapp.Network.FirebaseServicio;
import com.example.maquirentapp.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import androidx.core.content.ContextCompat;

public class ListaGruposElectrogenosFragment extends Fragment {
    private RecyclerView recyclerView;
    private LinearLayout emptyState;
    private GruposElectrogenosConfiguracionAdapter adapter;
    private FirebaseServicio firebaseServicio;
    private FirebaseAuth auth;

    private List<GrupoElectrogeno> gruposList = new ArrayList<>();
    private String codigoVerificacion;

    private ActivityResultLauncher<String> imagePickerLauncher;
    private Uri selectedImageUri;
    private ImageView dialogImagePreview;

    public ListaGruposElectrogenosFragment() { }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
        return inflater.inflate(R.layout.fragment_lista_grupos_electrogenos, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        firebaseServicio = new FirebaseServicio();
        auth = FirebaseAuth.getInstance();

        initViews(view);
        setupRecyclerView();
        cargarGrupos();
    }
    @Override
    public void onResume() {
        super.onResume();
        configureGlobalFab();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (getActivity() instanceof com.example.maquirentapp.MainActivity) {
            ((com.example.maquirentapp.MainActivity) getActivity()).hideGlobalFab();
        } else {
            View activityFab = getActivity() != null ? getActivity().findViewById(R.id.btnGlobal) : null;
            if (activityFab != null) activityFab.setVisibility(View.GONE);
        }
    }
    private void configureGlobalFab() {
        if (getActivity() instanceof com.example.maquirentapp.MainActivity) {
            com.example.maquirentapp.MainActivity main = (com.example.maquirentapp.MainActivity) getActivity();
            main.showGlobalFab(
                    "Añadir",
                    R.drawable.icon_nuevo_blanco,
                    v -> {
                        mostrarDialogoNuevoGrupo();
                    }
            );
        } else {
            View activityFab = getActivity() != null ? getActivity().findViewById(R.id.btnGlobal) : null;
            if (activityFab != null && activityFab instanceof ExtendedFloatingActionButton) {
                ExtendedFloatingActionButton fab = (ExtendedFloatingActionButton) activityFab;
                fab.setText("Añadir");
                try {
                    fab.setIcon(ContextCompat.getDrawable(requireContext(), R.drawable.icon_nuevo_blanco));
                } catch (Exception ignored) {}
                fab.setOnClickListener(v -> mostrarDialogoNuevoGrupo());
                fab.setVisibility(View.VISIBLE);
            }
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
        recyclerView = view.findViewById(R.id.recyclerViewGrupos);
        emptyState = view.findViewById(R.id.emptyState);
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new GruposElectrogenosConfiguracionAdapter(gruposList, getContext(), new GruposElectrogenosConfiguracionAdapter.OnGrupoActionListener() {
            @Override
            public void onEditarClick(GrupoElectrogeno grupo) {
                mostrarDialogoEditarGrupo(grupo);
            }

            @Override
            public void onEliminarClick(GrupoElectrogeno grupo) {
                solicitarEliminacionGrupo(grupo);
            }
        });
        recyclerView.setAdapter(adapter);
    }

    private void cargarGrupos() {
        Log.d("ListaGrupos", "Iniciando carga de grupos...");

        firebaseServicio.getGruposElectrogenos(new FirebaseServicio.OnGruposLoadedListener() {
            @Override
            public void onSuccess(List<GrupoElectrogeno> grupos) {
                List<GrupoElectrogeno> filtrados = new ArrayList<>();
                if (grupos != null) {
                    for (GrupoElectrogeno grupo : grupos) {
                        if (!grupo.isEliminado()) {
                            filtrados.add(grupo);
                        }
                    }
                }

                // Actualizar UI en el hilo principal
                recyclerView.post(() -> {
                    if (filtrados.isEmpty()) {
                        emptyState.setVisibility(View.VISIBLE);
                        recyclerView.setVisibility(View.GONE);
                    } else {
                        emptyState.setVisibility(View.GONE);
                        recyclerView.setVisibility(View.VISIBLE);
                        adapter.actualizarLista(filtrados);
                        try {
                            recyclerView.scrollToPosition(0);
                        } catch (Exception e) {
                            Log.w("ListaGrupos", "scrollToPosition error: " + e.getMessage());
                        }
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                Log.e("ListaGrupos", "Error cargando grupos", e);
                Toast.makeText(getContext(), "Error al cargar grupos: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void mostrarDialogoNuevoGrupo() {
        Dialog dialog = new Dialog(requireContext());
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_nuevo_grupo, null);
        dialog.setContentView(dialogView);

        dialogImagePreview = dialogView.findViewById(R.id.imgPreview);
        MaterialButton btnSeleccionarFoto = dialogView.findViewById(R.id.btnSeleccionarFoto);
        TextInputEditText inputCodigo = dialogView.findViewById(R.id.inputCodigo);
        MaterialButton btnCancelar = dialogView.findViewById(R.id.btnCancelar);
        MaterialButton btnGuardar = dialogView.findViewById(R.id.btnGuardar);

        btnSeleccionarFoto.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));

        btnCancelar.setOnClickListener(v -> {
            selectedImageUri = null;
            dialog.dismiss();
        });

        btnGuardar.setOnClickListener(v -> {
            String codigo = inputCodigo.getText().toString().trim();
            if (codigo.isEmpty()) {
                Toast.makeText(requireContext(), "Ingresa un código", Toast.LENGTH_SHORT).show();
                return;
            }

            btnGuardar.setEnabled(false);
            btnGuardar.setText("Guardando...");

            firebaseServicio.crearGrupoConImagen(codigo, selectedImageUri, new FirebaseServicio.OnGrupoCreatedListener() {
                @Override
                public void onSuccess(GrupoElectrogeno grupo) {
                    Toast.makeText(requireContext(), "Grupo creado exitosamente", Toast.LENGTH_SHORT).show();
                    selectedImageUri = null;
                    dialog.dismiss();
                    cargarGrupos();
                }

                @Override
                public void onError(Exception e) {
                    Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    btnGuardar.setEnabled(true);
                    btnGuardar.setText("Guardar");
                }
            });
        });

        dialog.show();
    }

    private void mostrarDialogoEditarGrupo(GrupoElectrogeno grupo) {
        Dialog dialog = new Dialog(requireContext());
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_nuevo_grupo, null);
        dialog.setContentView(dialogView);

        dialogImagePreview = dialogView.findViewById(R.id.imgPreview);
        MaterialButton btnSeleccionarFoto = dialogView.findViewById(R.id.btnSeleccionarFoto);
        final TextInputEditText inputCodigo = dialogView.findViewById(R.id.inputCodigo);
        MaterialButton btnCancelar = dialogView.findViewById(R.id.btnCancelar);
        MaterialButton btnGuardar = dialogView.findViewById(R.id.btnGuardar);

        inputCodigo.setText(grupo.getCodigo());

        if (grupo.getFoto() != null && !grupo.getFoto().isEmpty()) {
            try {
                Glide.with(requireContext())
                        .load(grupo.getFoto())
                        .placeholder(R.drawable.icon_generador)
                        .error(R.drawable.icon_generador)
                        .into(dialogImagePreview);
            } catch (Exception e) {
                dialogImagePreview.setImageResource(R.drawable.icon_generador);
            }
        } else {
            dialogImagePreview.setImageResource(R.drawable.icon_generador);
        }

        selectedImageUri = null;

        btnSeleccionarFoto.setOnClickListener(v -> {
            imagePickerLauncher.launch("image/*");
        });

        btnCancelar.setOnClickListener(v -> {
            selectedImageUri = null;
            dialog.dismiss();
        });

        btnGuardar.setOnClickListener(v -> {
            String nuevoCodigo = inputCodigo.getText() != null ? inputCodigo.getText().toString().trim() : "";
            if (nuevoCodigo.isEmpty()) {
                Toast.makeText(requireContext(), "Ingresa un código", Toast.LENGTH_SHORT).show();
                return;
            }

            btnGuardar.setEnabled(false);
            btnGuardar.setText("Guardando...");

            if (selectedImageUri != null) {
                String fileName = "grupos/" + nuevoCodigo + "_" + System.currentTimeMillis() + ".jpg";
                StorageReference storageRef = FirebaseStorage.getInstance().getReference().child(fileName);

                storageRef.putFile(selectedImageUri)
                        .addOnSuccessListener(taskSnapshot -> {
                            storageRef.getDownloadUrl()
                                    .addOnSuccessListener(downloadUri -> {
                                        String fotoUrl = downloadUri.toString();
                                        FirebaseFirestore.getInstance()
                                                .collection("gruposElectrogenos")
                                                .document(grupo.getId())
                                                .update("codigo", nuevoCodigo, "foto", fotoUrl)
                                                .addOnSuccessListener(aVoid -> {
                                                    Toast.makeText(requireContext(), "Grupo actualizado", Toast.LENGTH_SHORT).show();
                                                    // limpiar y cerrar
                                                    selectedImageUri = null;
                                                    dialog.dismiss();
                                                    cargarGrupos();
                                                })
                                                .addOnFailureListener(e -> {
                                                    Toast.makeText(requireContext(), "Error actualizando grupo: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                                    btnGuardar.setEnabled(true);
                                                    btnGuardar.setText("Guardar");
                                                });
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(requireContext(), "Error obteniendo URL: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                        btnGuardar.setEnabled(true);
                                        btnGuardar.setText("Guardar");
                                    });
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(requireContext(), "Error subiendo imagen: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            btnGuardar.setEnabled(true);
                            btnGuardar.setText("Guardar");
                        });
            } else {
                firebaseServicio.actualizarCodigoGrupo(grupo.getId(), nuevoCodigo, new FirebaseServicio.OnSimpleCallback() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(requireContext(), "Grupo actualizado", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        cargarGrupos();
                    }

                    @Override
                    public void onError(Exception e) {
                        Toast.makeText(requireContext(), "Error al actualizar: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        btnGuardar.setEnabled(true);
                        btnGuardar.setText("Guardar");
                    }
                });
            }
        });

        dialog.show();
    }

    private void solicitarEliminacionGrupo(GrupoElectrogeno grupo) {
        codigoVerificacion = generarCodigoVerificacion();

        Map<String, Object> updates = new HashMap<>();
        updates.put("codigoVerificacion", codigoVerificacion);
        updates.put("codigoGeneradoEn", System.currentTimeMillis());

        FirebaseFirestore.getInstance()
                .collection("gruposElectrogenos")
                .document(grupo.getId())
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    enviarCodigoVerificacion(codigoVerificacion, grupo);
                    mostrarDialogoCodigoVerificacion(grupo);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Error generando código: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private String generarCodigoVerificacion() {
        Random random = new Random();
        int codigo = 100000 + random.nextInt(900000);
        return String.valueOf(codigo);
    }

    private void enviarCodigoVerificacion(String codigo, GrupoElectrogeno grupo) {
        String userEmail = auth.getCurrentUser() != null ? auth.getCurrentUser().getEmail() : "";
        // Simulación:
        Toast.makeText(getContext(), "Código: " + codigo, Toast.LENGTH_LONG).show();

    }


    private void mostrarDialogoCodigoVerificacion(GrupoElectrogeno grupo) {
        final EditText inputCodigo = new EditText(getContext());
        inputCodigo.setHint("Código de 6 dígitos");
        inputCodigo.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);

        new AlertDialog.Builder(requireContext())
                .setTitle("Confirmar Eliminación")
                .setMessage("Se ha enviado un código de verificación a tu correo. Ingrésalo para confirmar la eliminación del grupo " + grupo.getCodigo())
                .setView(inputCodigo)
                .setPositiveButton("Confirmar", (dialog, which) -> {
                    String codigoIngresado = inputCodigo.getText().toString().trim();
                    if (codigoIngresado.isEmpty()) {
                        Toast.makeText(getContext(), "Ingresa el código", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    FirebaseFirestore.getInstance()
                            .collection("gruposElectrogenos")
                            .document(grupo.getId())
                            .get()
                            .addOnSuccessListener(documentSnapshot -> {
                                if (documentSnapshot.exists()) {
                                    String codigoGuardado = documentSnapshot.getString("codigoVerificacion");
                                    Long tsGenerado = documentSnapshot.getLong("codigoGeneradoEn");
                                    boolean expirado = false;
                                    if (tsGenerado != null) {
                                        long ahora = System.currentTimeMillis();
                                        long diff = ahora - tsGenerado;
                                        long ttl = 15 * 60 * 1000L;
                                        expirado = diff > ttl;
                                    }

                                    if (expirado) {
                                        Toast.makeText(getContext(), "El código ha expirado. Solicita nuevamente.", Toast.LENGTH_SHORT).show();
                                    } else if (codigoGuardado != null && codigoGuardado.equals(codigoIngresado)) {
                                        eliminarGrupoSuave(grupo);
                                        documentSnapshot.getReference().update("codigoVerificacion", null, "codigoGeneradoEn", null);
                                    } else {
                                        Toast.makeText(getContext(), "Código incorrecto", Toast.LENGTH_SHORT).show();
                                    }
                                } else {
                                    Toast.makeText(getContext(), "Documento no encontrado", Toast.LENGTH_SHORT).show();
                                }
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(getContext(), "Error verificando código: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });

                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void eliminarGrupoSuave(GrupoElectrogeno grupo) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("eliminado", true);
        updates.put("fechaEliminacion", System.currentTimeMillis());
        updates.put("eliminadoPor", auth.getCurrentUser().getUid());

        firebaseServicio.eliminarGrupoSuave(grupo.getId(), updates, new FirebaseServicio.OnSimpleCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(getContext(),
                        "Grupo marcado para eliminación. Se eliminará permanentemente en 30 días.",
                        Toast.LENGTH_LONG).show();
                cargarGrupos();
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(getContext(), "Error al eliminar grupo", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
