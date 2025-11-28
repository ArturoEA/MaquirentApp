package com.example.maquirentapp.View;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
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
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.maquirentapp.Access.GruposElectrogenosConfiguracionAdapter;
import com.example.maquirentapp.Model.GrupoElectrogeno;
import com.example.maquirentapp.Network.FirebaseServicio;
import com.example.maquirentapp.R;
import com.example.maquirentapp.Utils.ImageUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.yalantis.ucrop.UCrop;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class ListaGruposElectrogenosFragment extends Fragment {
    private static final String TAG = "ListaGrupos";
    private static final long CODIGO_EXPIRACION_MS = 15 * 60 * 1000L;
    private RecyclerView recyclerView;
    private LinearLayout emptyState;
    private GruposElectrogenosConfiguracionAdapter adapter;
    private FirebaseServicio firebaseServicio;
    private FirebaseAuth auth;
    private FirebaseFunctions functions;
    private FirebaseFirestore db;

    private List<GrupoElectrogeno> gruposList = new ArrayList<>();

    private ActivityResultLauncher<String> imagePickerLauncher;
    private Uri selectedImageUri;
    private ImageView dialogImagePreview;
    private ActivityResultLauncher<Intent> cropImageLauncher;

    public ListaGruposElectrogenosFragment() { }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        iniciarRecorte(uri);
                    }
                });

        cropImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        final Uri resultUri = UCrop.getOutput(result.getData());
                        if (resultUri != null) {
                            selectedImageUri = resultUri;
                            if (dialogImagePreview != null) {
                                dialogImagePreview.setImageURI(resultUri);
                            }
                        }
                    } else if (result.getResultCode() == UCrop.RESULT_ERROR) {
                        final Throwable cropError = UCrop.getError(result.getData());
                        mostrarError("Error al recortar: " + cropError.getMessage());
                    }
                });
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_lista_grupos_electrogenos, container, false);
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeFirebase();
        initViews(view);
        setupRecyclerView();
        cargarGrupos();
    }
    private void initializeFirebase() {
        firebaseServicio = new FirebaseServicio();
        auth = FirebaseAuth.getInstance();
        functions = FirebaseFunctions.getInstance();
        db = FirebaseFirestore.getInstance();
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
            main.showGlobalFab("Añadir", R.drawable.icon_nuevo_blanco, v -> mostrarDialogoNuevoGrupo());
        }
    }
    private void hideGlobalFab() {
        if (getActivity() instanceof com.example.maquirentapp.MainActivity) {
            ((com.example.maquirentapp.MainActivity) getActivity()).hideGlobalFab();
        }
    }
    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerViewGrupos);
        emptyState = view.findViewById(R.id.emptyState);
    }
    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new GruposElectrogenosConfiguracionAdapter(
                gruposList,
                getContext(),
                new GruposElectrogenosConfiguracionAdapter.OnGrupoActionListener() {
                    @Override
                    public void onEditarClick(GrupoElectrogeno grupo) {
                        mostrarDialogoEditarGrupo(grupo);
                    }

                    @Override
                    public void onEliminarClick(GrupoElectrogeno grupo) {
                        solicitarEliminacionGrupo(grupo);
                    }
                }
        );
        recyclerView.setAdapter(adapter);
    }
    private void iniciarRecorte(Uri sourceUri) {
        String destFileName = "cropped_" + System.currentTimeMillis() + ".jpg";

        Uri destinationUri = Uri.fromFile(new File(requireContext().getCacheDir(), destFileName));

        UCrop.Options options = new UCrop.Options();
        options.setCompressionQuality(80);
        options.setToolbarColor(ContextCompat.getColor(requireContext(), R.color.black));
        options.setStatusBarColor(ContextCompat.getColor(requireContext(), R.color.black));
        options.setActiveControlsWidgetColor(ContextCompat.getColor(requireContext(), R.color.selection_indicator));
        options.setToolbarTitle("Editar Foto");

        Intent intent = UCrop.of(sourceUri, destinationUri)
                .withAspectRatio(16, 9)
                .withMaxResultSize(1280, 720)
                .withOptions(options)
                .getIntent(requireContext());

        cropImageLauncher.launch(intent);
    }
    private void cargarGrupos() {
        Log.d(TAG, "Iniciando carga de grupos...");

        firebaseServicio.getGruposElectrogenos(false, new FirebaseServicio.OnGruposLoadedListener() {
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

                actualizarUI(filtrados);
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error cargando grupos", e);
                mostrarError("Error al cargar grupos: " + e.getMessage());
            }
        });
    }
    private void actualizarUI(List<GrupoElectrogeno> grupos) {
        if (recyclerView != null) {
            recyclerView.post(() -> {
                if (grupos.isEmpty()) {
                    emptyState.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                } else {
                    emptyState.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                    adapter.actualizarLista(grupos);
                }
            });
        }
    }
    private void mostrarDialogoNuevoGrupo() {
        Dialog dialog = crearDialogoGrupo(null);
        dialog.show();
    }
    private void mostrarDialogoEditarGrupo(GrupoElectrogeno grupo) {
        Dialog dialog = crearDialogoGrupo(grupo);
        dialog.show();
    }
    private Dialog crearDialogoGrupo(@Nullable GrupoElectrogeno grupoExistente) {
        Dialog dialog = new Dialog(requireContext());
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_nuevo_grupo, null);
        dialog.setContentView(dialogView);

        dialogImagePreview = dialogView.findViewById(R.id.imgPreview);
        MaterialButton btnSeleccionarFoto = dialogView.findViewById(R.id.btnSeleccionarFoto);
        TextInputEditText inputCodigo = dialogView.findViewById(R.id.inputCodigo);
        MaterialButton btnCancelar = dialogView.findViewById(R.id.btnCancelar);
        MaterialButton btnGuardar = dialogView.findViewById(R.id.btnGuardar);

        boolean esEdicion = grupoExistente != null;

        if (esEdicion) {
            inputCodigo.setText(grupoExistente.getCodigo());
            cargarImagenExistente(grupoExistente.getFoto());
        }

        selectedImageUri = null;

        btnSeleccionarFoto.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));
        btnCancelar.setOnClickListener(v -> {
            selectedImageUri = null;
            dialog.dismiss();
        });

        btnGuardar.setOnClickListener(v -> {
            String codigo = inputCodigo.getText() != null ?
                    inputCodigo.getText().toString().trim() : "";

            if (codigo.isEmpty()) {
                mostrarError("Ingresa un código");
                return;
            }

            deshabilitarBoton(btnGuardar, "Guardando...");

            if (esEdicion) {
                actualizarGrupo(grupoExistente, codigo, dialog, btnGuardar);
            } else {
                crearNuevoGrupo(codigo, dialog, btnGuardar);
            }
        });

        return dialog;
    }
    private void cargarImagenExistente(String fotoUrl) {
        if (fotoUrl != null && !fotoUrl.isEmpty() && dialogImagePreview != null) {
            try {
                Glide.with(requireContext())
                        .load(fotoUrl)
                        .placeholder(R.drawable.icon_generador)
                        .error(R.drawable.icon_generador)
                        .into(dialogImagePreview);
            } catch (Exception e) {
                dialogImagePreview.setImageResource(R.drawable.icon_generador);
            }
        }
    }
    private void crearNuevoGrupo(String codigo, Dialog dialog, MaterialButton btnGuardar) {
        if (selectedImageUri != null) {
            Toast.makeText(getContext(), "Procesando imagen...", Toast.LENGTH_SHORT).show();

            new Thread(() -> {
                byte[] dataImagen = ImageUtils.comprimirImagen(requireContext(), selectedImageUri);

                requireActivity().runOnUiThread(() -> {
                    if (dataImagen != null) {
                        firebaseServicio.crearGrupoConImagenBytes(codigo, dataImagen,
                                new FirebaseServicio.OnGrupoCreatedListener() {
                                    @Override
                                    public void onSuccess(GrupoElectrogeno grupo) {
                                        mostrarExito("Grupo creado exitosamente");
                                        cerrarDialogo(dialog);
                                        cargarGrupos();
                                    }

                                    @Override
                                    public void onError(Exception e) {
                                        mostrarError("Error: " + e.getMessage());
                                        habilitarBoton(btnGuardar, "Guardar");
                                    }
                                });
                    } else {
                        mostrarError("Error al comprimir la imagen");
                        habilitarBoton(btnGuardar, "Guardar");
                    }
                });
            }).start();
        } else {
            firebaseServicio.crearGrupoConImagen(codigo, null,
                    new FirebaseServicio.OnGrupoCreatedListener() {
                        @Override
                        public void onSuccess(GrupoElectrogeno grupo) {
                            mostrarExito("Grupo creado (sin foto)");
                            cerrarDialogo(dialog);
                            cargarGrupos();
                        }

                        @Override
                        public void onError(Exception e) {
                            mostrarError("Error: " + e.getMessage());
                            habilitarBoton(btnGuardar, "Guardar");
                        }
                    });
        }
    }
    private void actualizarGrupo(GrupoElectrogeno grupo, String nuevoCodigo,
                                 Dialog dialog, MaterialButton btnGuardar) {
        if (selectedImageUri != null) {
            subirImagenYActualizar(grupo, nuevoCodigo, dialog, btnGuardar);
        } else {
            actualizarSoloCodigo(grupo, nuevoCodigo, dialog, btnGuardar);
        }
    }
    private void subirImagenYActualizar(GrupoElectrogeno grupo, String nuevoCodigo,
                                        Dialog dialog, MaterialButton btnGuardar) {

        Toast.makeText(getContext(), "Procesando imagen...", Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            byte[] dataImagen = ImageUtils.comprimirImagen(requireContext(), selectedImageUri);

            requireActivity().runOnUiThread(() -> {
                if (dataImagen != null) {
                    String fileName = "grupos/" + nuevoCodigo + "_" + System.currentTimeMillis() + ".jpg";
                    StorageReference storageRef = FirebaseStorage.getInstance()
                            .getReference().child(fileName);

                    storageRef.putBytes(dataImagen)
                            .addOnSuccessListener(taskSnapshot ->
                                    storageRef.getDownloadUrl()
                                            .addOnSuccessListener(downloadUri -> {
                                                Map<String, Object> updates = new HashMap<>();
                                                updates.put("codigo", nuevoCodigo);
                                                updates.put("foto", downloadUri.toString());

                                                actualizarEnFirestore(grupo.getId(), updates, dialog, btnGuardar);
                                            })
                                            .addOnFailureListener(e -> {
                                                mostrarError("Error obteniendo URL: " + e.getMessage());
                                                habilitarBoton(btnGuardar, "Guardar");
                                            })
                            )
                            .addOnFailureListener(e -> {
                                mostrarError("Error subiendo imagen: " + e.getMessage());
                                habilitarBoton(btnGuardar, "Guardar");
                            });
                } else {
                    mostrarError("Error al comprimir la imagen");
                    habilitarBoton(btnGuardar, "Guardar");
                }
            });
        }).start();
    }
    private void actualizarSoloCodigo(GrupoElectrogeno grupo, String nuevoCodigo,
                                      Dialog dialog, MaterialButton btnGuardar) {
        firebaseServicio.actualizarCodigoGrupo(grupo.getId(), nuevoCodigo,
                new FirebaseServicio.OnSimpleCallback() {
                    @Override
                    public void onSuccess() {
                        mostrarExito("Grupo actualizado");
                        cerrarDialogo(dialog);
                        cargarGrupos();
                    }

                    @Override
                    public void onError(Exception e) {
                        mostrarError("Error al actualizar: " + e.getMessage());
                        habilitarBoton(btnGuardar, "Guardar");
                    }
                });
    }
    private void actualizarEnFirestore(String grupoId, Map<String, Object> updates,
                                       Dialog dialog, MaterialButton btnGuardar) {
        db.collection("gruposElectrogenos").document(grupoId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    mostrarExito("Grupo actualizado");
                    cerrarDialogo(dialog);
                    cargarGrupos();
                })
                .addOnFailureListener(e -> {
                    mostrarError("Error actualizando: " + e.getMessage());
                    habilitarBoton(btnGuardar, "Guardar");
                });
    }
    private void solicitarEliminacionGrupo(GrupoElectrogeno grupo) {
        String codigo = generarCodigoVerificacion();
        long timestamp = System.currentTimeMillis();

        // Guardar código en Firestore
        Map<String, Object> updates = new HashMap<>();
        updates.put("codigoVerificacion", codigo);
        updates.put("codigoGeneradoEn", timestamp);

        db.collection("gruposElectrogenos").document(grupo.getId())
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    FirebaseUser currentUser = auth.getCurrentUser();
                    if (currentUser == null) {
                        mostrarError("Debes iniciar sesión antes de eliminar un grupo.");
                        return;
                    }
                    // Enviar email mediante Cloud Function
                    enviarEmailVerificacion(codigo, grupo);
                    mostrarDialogoCodigoVerificacion(grupo);
                })
                .addOnFailureListener(e ->
                        mostrarError("Error generando código: " + e.getMessage())
                );
    }
    private String generarCodigoVerificacion() {
        return String.valueOf(100000 + new Random().nextInt(900000));
    }
    private void enviarEmailVerificacion(String codigo, GrupoElectrogeno grupo) {
        String userEmail = auth.getCurrentUser() != null ?
                auth.getCurrentUser().getEmail() : "";

        // Datos para la Cloud Function
        Map<String, Object> data = new HashMap<>();
        data.put("email", userEmail);
        data.put("codigo", codigo);
        data.put("grupoNombre", grupo.getCodigo());

        // Llamar a la Cloud Function
        functions.getHttpsCallable("enviarCodigoEliminacion")
                .call(data)
                .addOnSuccessListener(result -> {
                    Log.d(TAG, "Email enviado exitosamente");
                    mostrarExito("Código enviado a " + userEmail);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error enviando email", e);
                    // Mostrar código como fallback
                    Toast.makeText(getContext(),
                            "Email no disponible. Código: " + codigo,
                            Toast.LENGTH_LONG).show();
                });
    }
    private void mostrarDialogoCodigoVerificacion(GrupoElectrogeno grupo) {
        final EditText inputCodigo = new EditText(getContext());
        inputCodigo.setHint("Código de 6 dígitos");
        inputCodigo.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);

        new MaterialAlertDialogBuilder(requireContext(), R.style.DialogoConFuenteAnta)
                .setTitle("Confirmar Eliminación")
                .setMessage("Se ha enviado un código de verificación a tu correo.\n\n" +
                        "Grupo: " + grupo.getCodigo() + "\n" +
                        "El código expira en 15 minutos.")
                .setView(inputCodigo)
                .setPositiveButton("Confirmar", (dialog, which) ->
                        verificarCodigoYEliminar(grupo, inputCodigo.getText().toString().trim())
                )
                .setNegativeButton("Cancelar", null)
                .setCancelable(false)
                .show();
    }
    private void verificarCodigoYEliminar(GrupoElectrogeno grupo, String codigoIngresado) {
        if (codigoIngresado.isEmpty()) {
            mostrarError("Ingresa el código");
            return;
        }

        db.collection("gruposElectrogenos").document(grupo.getId())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        mostrarError("Documento no encontrado");
                        return;
                    }

                    String codigoGuardado = documentSnapshot.getString("codigoVerificacion");
                    Long tsGenerado = documentSnapshot.getLong("codigoGeneradoEn");

                    if (codigoGuardado == null || tsGenerado == null) {
                        mostrarError("Código no encontrado. Solicita nuevamente.");
                        return;
                    }

                    // Verificar expiración
                    long ahora = System.currentTimeMillis();
                    boolean expirado = (ahora - tsGenerado) > CODIGO_EXPIRACION_MS;

                    if (expirado) {
                        mostrarError("El código ha expirado. Solicita nuevamente.");
                        limpiarCodigoVerificacion(grupo.getId());
                    } else if (codigoGuardado.equals(codigoIngresado)) {
                        eliminarGrupoSuave(grupo);
                        limpiarCodigoVerificacion(grupo.getId());
                    } else {
                        mostrarError("Código incorrecto");
                    }
                })
                .addOnFailureListener(e ->
                        mostrarError("Error verificando código: " + e.getMessage())
                );
    }
    private void limpiarCodigoVerificacion(String grupoId) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("codigoVerificacion", null);
        updates.put("codigoGeneradoEn", null);

        db.collection("gruposElectrogenos").document(grupoId).update(updates);
    }
    private void eliminarGrupoSuave(GrupoElectrogeno grupo) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("eliminado", true);
        updates.put("fechaEliminacion", System.currentTimeMillis());
        updates.put("eliminadoPor", auth.getCurrentUser().getUid());

        firebaseServicio.eliminarGrupoSuave(grupo.getId(), updates,
                new FirebaseServicio.OnSimpleCallback() {
                    @Override
                    public void onSuccess() {
                        mostrarExito("Grupo marcado para eliminación.\n" +
                                "Se eliminará permanentemente en 30 días.");
                        cargarGrupos();
                    }

                    @Override
                    public void onError(Exception e) {
                        mostrarError("Error al eliminar grupo");
                    }
                });
    }
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