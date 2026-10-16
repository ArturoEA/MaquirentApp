package com.example.maquirentapp.View;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
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
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.maquirentapp.adapters.GruposElectrogenosConfiguracionAdapter;
import com.example.maquirentapp.Model.GrupoElectrogeno;
import com.example.maquirentapp.Repository.Callback;
import com.example.maquirentapp.Repository.GeneradorRepository;
import com.example.maquirentapp.Repository.UsuarioRepository;
import com.example.maquirentapp.R;
import com.example.maquirentapp.Utils.ImageUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.yalantis.ucrop.UCrop;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ListaGruposElectrogenosFragment extends Fragment implements GruposElectrogenosConfiguracionAdapter.OnStartDragListener{
    private static final String TAG = "ListaGrupos";
    private RecyclerView recyclerView;
    private LinearLayout emptyState;
    private GruposElectrogenosConfiguracionAdapter adapter;
    private GeneradorRepository generadorRepository;
    private UsuarioRepository usuarioRepository;

    private List<GrupoElectrogeno> gruposList = new ArrayList<>();

    private ActivityResultLauncher<String> imagePickerLauncher;
    private Uri selectedImageUri;
    private ImageView dialogImagePreview;
    private ActivityResultLauncher<Intent> cropImageLauncher;
    private ItemTouchHelper itemTouchHelper;

    public ListaGruposElectrogenosFragment() {
    }

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
        generadorRepository = new GeneradorRepository();
        usuarioRepository = new UsuarioRepository();
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
                },
                this
        );
        recyclerView.setAdapter(adapter);

        ItemTouchHelper.Callback callback = new ItemTouchHelper.Callback() {
            @Override
            public boolean isLongPressDragEnabled() {
                return false;
            }

            @Override
            public boolean isItemViewSwipeEnabled() {
                return false;
            }

            @Override
            public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
                int swipeFlags = 0;
                return makeMovementFlags(dragFlags, swipeFlags);
            }

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                adapter.onItemMove(viewHolder.getAdapterPosition(), target.getAdapterPosition());
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            }

            @Override
            public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerView, viewHolder);
                guardarNuevoOrdenEnFirebase();
            }
        };

        itemTouchHelper = new ItemTouchHelper(callback);
        itemTouchHelper.attachToRecyclerView(recyclerView);
    }
    public void onRequestDrag(RecyclerView.ViewHolder viewHolder) {
        if (itemTouchHelper != null) {
            itemTouchHelper.startDrag(viewHolder);
        }
    }

    private void guardarNuevoOrdenEnFirebase() {
        List<GrupoElectrogeno> listaOrdenada = adapter.getListaActual();

        if (listaOrdenada == null || listaOrdenada.isEmpty()) return;

        generadorRepository.actualizarOrdenGrupos(listaOrdenada, new Callback<Void>() {
            @Override
            public void onSuccess(Void result) {
                Log.d(TAG, "Orden actualizado correctamente en el repositorio");
            }

            @Override
            public void onError(Exception e) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Error al guardar el orden", Toast.LENGTH_SHORT).show();
                }
            }
        });
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

        generadorRepository.getGruposElectrogenos(false, new Callback<List<GrupoElectrogeno>>() {
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
            Context safeContext = getContext();
            if (safeContext == null) return;

            new Thread(() -> {
                byte[] dataImagen = ImageUtils.comprimirImagen(safeContext, selectedImageUri);

                if (getActivity() != null) {
                    requireActivity().runOnUiThread(() -> {
                        if (dataImagen != null) {
                            generadorRepository.crearGrupoConImagenBytes(codigo, dataImagen,
                                    new Callback<GrupoElectrogeno>() {
                                        @Override
                                        public void onSuccess(GrupoElectrogeno grupo) {
                                            if (getContext() == null) return;
                                            mostrarExito("Grupo creado exitosamente");
                                            cerrarDialogo(dialog);
                                            cargarGrupos();
                                        }

                                        @Override
                                        public void onError(Exception e) {
                                            if (getContext() == null) return;
                                            mostrarError("Error: " + e.getMessage());
                                            habilitarBoton(btnGuardar, "Guardar");
                                        }
                                    });
                        } else {
                            mostrarError("Error al comprimir la imagen");
                            habilitarBoton(btnGuardar, "Guardar");
                        }
                    });
                }
            }).start();
        } else {
            generadorRepository.crearGrupoConImagen(codigo, null,
                    new Callback<GrupoElectrogeno>() {
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
        Context safeContext = getContext();
        if (safeContext == null) return;

        new Thread(() -> {
            byte[] dataImagen = ImageUtils.comprimirImagen(safeContext, selectedImageUri);

            if (getActivity() != null) {
                requireActivity().runOnUiThread(() -> {
                    if (dataImagen != null) {
                        generadorRepository.actualizarGrupoConImagenBytes(grupo.getId(), nuevoCodigo, dataImagen,
                                new Callback<Void>() {
                                    @Override
                                    public void onSuccess(Void result) {
                                        if (getContext() == null) return;
                                        mostrarExito("Grupo actualizado");
                                        cerrarDialogo(dialog);
                                        cargarGrupos();
                                    }

                                    @Override
                                    public void onError(Exception e) {
                                        if (getContext() == null) return;
                                        mostrarError("Error actualizando: " + e.getMessage());
                                        habilitarBoton(btnGuardar, "Guardar");
                                    }
                                });
                    } else {
                        if (getContext() == null) return;
                        mostrarError("Error al comprimir la imagen");
                        habilitarBoton(btnGuardar, "Guardar");
                    }
                });
            }

        }).start();
    }

    private void actualizarSoloCodigo(GrupoElectrogeno grupo, String nuevoCodigo,
                                      Dialog dialog, MaterialButton btnGuardar) {
        generadorRepository.actualizarCodigoGrupo(grupo.getId(), nuevoCodigo,
                new Callback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        if (getContext() == null) return;
                        mostrarExito("Grupo actualizado");
                        cerrarDialogo(dialog);
                        cargarGrupos();
                    }

                    @Override
                    public void onError(Exception e) {
                        if (getContext() == null) return;
                        mostrarError("Error al actualizar: " + e.getMessage());
                        habilitarBoton(btnGuardar, "Guardar");
                    }
                });
    }

    private void solicitarEliminacionGrupo(GrupoElectrogeno grupo) {
        String userEmail = usuarioRepository.getCurrentUserEmail();
        if (userEmail == null) {
            mostrarError("Debes iniciar sesión antes de eliminar un grupo.");
            return;
        }

        generadorRepository.solicitarEliminacionGrupo(grupo, userEmail, new Callback<String>() {
            @Override
            public void onSuccess(String codigo) {
                if (getContext() == null) return;
                mostrarExito("Código enviado a " + userEmail);
                mostrarDialogoCodigoVerificacion(grupo);
            }

            @Override
            public void onError(Exception e) {
                if (getContext() == null) return;
                mostrarError("Error generando código: " + e.getMessage());
            }
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

        String userUid = usuarioRepository.getCurrentUserUid();
        generadorRepository.verificarCodigoEliminacion(grupo.getId(), codigoIngresado, userUid, new Callback<Void>() {
            @Override
            public void onSuccess(Void result) {
                if (getContext() == null) return;
                mostrarExito("Grupo eliminado de las vistas");
                cargarGrupos();
            }

            @Override
            public void onError(Exception e) {
                if (getContext() == null) return;
                mostrarError("Error verificando código: " + e.getMessage());
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
