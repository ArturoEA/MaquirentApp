package com.example.maquirentapp.View;

import android.app.DatePickerDialog;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.maquirentapp.Model.Mantenimiento;
import com.example.maquirentapp.Model.MantenimientoConfiguracion;
import com.example.maquirentapp.R;
import com.example.maquirentapp.Access.ItemsMantenimientoSeleccionablesAdapter;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class NuevoMantenimientoFragment extends Fragment {
    private static final String TAG = "NuevoMantenimiento";
    private static final String ARG_CODIGO = "codigo";
    private static final String ARG_MANTENIMIENTO_ID = "mantenimiento_id";
    private static final int MAX_FOTOS = 4;

    private String codigoGrupo;
    private String mantenimientoId;
    private boolean modoLectura = true;

    // Views
    private TextInputEditText inputEmpresa, inputHorometro, inputFecha, inputComentarios;
    private RecyclerView recyclerItems;
    private LinearLayout layoutFotos;
    private ProgressBar progressBar;

    // Adapters y datos
    private ItemsMantenimientoSeleccionablesAdapter itemsAdapter;
    private List<MantenimientoConfiguracion> itemsConfigList = new ArrayList<>();
    private List<Uri> fotosUriList = new ArrayList<>();
    private List<String> fotosUrlList = new ArrayList<>();

    // Firebase
    private FirebaseFirestore db;
    private FirebaseStorage storage;

    // Launcher para seleccionar fotos
    private ActivityResultLauncher<String> imagePickerLauncher;

    // Datos del mantenimiento actual
    private Mantenimiento mantenimientoActual;

    public static NuevoMantenimientoFragment newInstance(String codigo, String mantenimientoId) {
        NuevoMantenimientoFragment fragment = new NuevoMantenimientoFragment();
        Bundle args = new Bundle();
        args.putString(ARG_CODIGO, codigo);
        args.putString(ARG_MANTENIMIENTO_ID, mantenimientoId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            codigoGrupo = getArguments().getString(ARG_CODIGO);
            mantenimientoId = getArguments().getString(ARG_MANTENIMIENTO_ID);
            modoLectura = mantenimientoId != null;
        }

        // Configurar launcher de imágenes
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null && !modoLectura) {
                        agregarFoto(uri);
                    }
                });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_nuevo_mantenimiento, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeFirebase();
        initViews(view);
        setupRecyclerItems();
        setupDatePicker();
        cargarItemsConfiguracion();

        if (mantenimientoId != null) {
            cargarMantenimiento();
        } else {
            modoLectura = false;
            aplicarModoEdicion();
        }
    }

    private void initializeFirebase() {
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
    }

    private void initViews(View view) {
        inputEmpresa = view.findViewById(R.id.inputEmpresa);
        inputHorometro = view.findViewById(R.id.inputHorometro);
        inputFecha = view.findViewById(R.id.inputFecha);
        inputComentarios = view.findViewById(R.id.inputComentarios);
        recyclerItems = view.findViewById(R.id.recyclerItemsMantenimiento);
        layoutFotos = view.findViewById(R.id.layoutFotos);
        progressBar = view.findViewById(R.id.progressBarMantenimiento);
    }

    private void setupRecyclerItems() {
        recyclerItems.setLayoutManager(new GridLayoutManager(getContext(), 1));
        itemsAdapter = new ItemsMantenimientoSeleccionablesAdapter(getContext(), itemsConfigList);
        recyclerItems.setAdapter(itemsAdapter);
    }

    private void setupDatePicker() {
        inputFecha.setFocusable(false);
        inputFecha.setOnClickListener(v -> {
            if (!modoLectura) {
                mostrarDatePicker();
            }
        });
    }

    private void mostrarDatePicker() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    calendar.set(year, month, dayOfMonth);
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    inputFecha.setText(sdf.format(calendar.getTime()));
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void cargarItemsConfiguracion() {
        db.collection("mantenimientos_configuracion")
                .orderBy("fechaCreacion")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    itemsConfigList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        MantenimientoConfiguracion item = document.toObject(MantenimientoConfiguracion.class);
                        item.setId(document.getId());
                        itemsConfigList.add(item);
                    }
                    itemsAdapter.actualizarLista(itemsConfigList);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error cargando items", e);
                    Toast.makeText(getContext(), "Error al cargar items", Toast.LENGTH_SHORT).show();
                });
    }

    private void cargarMantenimiento() {
        progressBar.setVisibility(View.VISIBLE);

        db.collection("mantenimientos").document(mantenimientoId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        mantenimientoActual = documentSnapshot.toObject(Mantenimiento.class);
                        if (mantenimientoActual != null) {
                            mantenimientoActual.setId(documentSnapshot.getId());
                            mostrarDatosMantenimiento();
                        }
                    }
                    progressBar.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error cargando mantenimiento", e);
                    Toast.makeText(getContext(), "Error al cargar datos", Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                });
    }

    private void mostrarDatosMantenimiento() {
        inputEmpresa.setText(mantenimientoActual.getEmpresa());
        inputHorometro.setText(mantenimientoActual.getHorometro());
        inputFecha.setText(mantenimientoActual.getFecha());
        inputComentarios.setText(mantenimientoActual.getComentarios());

        // Seleccionar items realizados
        if (mantenimientoActual.getItemsRealizados() != null) {
            itemsAdapter.setItemsSeleccionados(mantenimientoActual.getItemsRealizados());
        }

        // Cargar fotos existentes
        if (mantenimientoActual.getFotos() != null) {
            fotosUrlList = new ArrayList<>(mantenimientoActual.getFotos());
            mostrarFotos();
        }

        aplicarModoLectura();
    }

    private void aplicarModoLectura() {
        inputEmpresa.setEnabled(false);
        inputHorometro.setEnabled(false);
        inputFecha.setEnabled(false);
        inputComentarios.setEnabled(false);
        itemsAdapter.setModoLectura(true);
    }

    private void aplicarModoEdicion() {
        inputEmpresa.setEnabled(true);
        inputHorometro.setEnabled(true);
        inputFecha.setEnabled(true);
        inputComentarios.setEnabled(true);
        itemsAdapter.setModoLectura(false);
    }

    private void agregarFoto(Uri uri) {
        if (fotosUriList.size() + fotosUrlList.size() >= MAX_FOTOS) {
            Toast.makeText(getContext(), "Máximo " + MAX_FOTOS + " fotos", Toast.LENGTH_SHORT).show();
            return;
        }

        fotosUriList.add(uri);
        mostrarFotos();
    }

    private void mostrarFotos() {
        layoutFotos.removeAllViews();

        // Mostrar fotos existentes (URLs)
        for (int i = 0; i < fotosUrlList.size(); i++) {
            final int index = i;
            String url = fotosUrlList.get(i);
            View fotoView = crearVistaFoto(url, true);

            if (!modoLectura) {
                fotoView.findViewById(R.id.btnEliminarFoto).setOnClickListener(v -> {
                    confirmarEliminarFoto(index, true);
                });
            } else {
                fotoView.findViewById(R.id.btnEliminarFoto).setVisibility(View.GONE);
            }

            layoutFotos.addView(fotoView);
        }

        // Mostrar fotos nuevas (URIs)
        for (int i = 0; i < fotosUriList.size(); i++) {
            final int index = i;
            Uri uri = fotosUriList.get(i);
            View fotoView = crearVistaFoto(uri.toString(), false);

            if (!modoLectura) {
                fotoView.findViewById(R.id.btnEliminarFoto).setOnClickListener(v -> {
                    confirmarEliminarFoto(index, false);
                });
            }

            layoutFotos.addView(fotoView);
        }

        // Botón para agregar foto (solo si no está en modo lectura)
        if (!modoLectura && (fotosUriList.size() + fotosUrlList.size() < MAX_FOTOS)) {
            View btnAgregarFoto = crearBotonAgregarFoto();
            layoutFotos.addView(btnAgregarFoto);
        }
    }

    private View crearVistaFoto(String urlOrUri, boolean esUrl) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.item_foto_mantenimiento, layoutFotos, false);
        ImageView ivFoto = view.findViewById(R.id.ivFotoMantenimiento);

        if (esUrl) {
            Glide.with(requireContext())
                    .load(urlOrUri)
                    .centerCrop()
                    .into(ivFoto);
        } else {
            Glide.with(requireContext())
                    .load(Uri.parse(urlOrUri))
                    .centerCrop()
                    .into(ivFoto);
        }

        // Click para ver foto en grande
        ivFoto.setOnClickListener(v -> mostrarFotoGrande(urlOrUri, esUrl));

        return view;
    }

    private View crearBotonAgregarFoto() {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.item_agregar_foto, layoutFotos, false);
        view.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));
        return view;
    }

    private void mostrarFotoGrande(String urlOrUri, boolean esUrl) {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_foto_grande, null);
        ImageView ivFoto = dialogView.findViewById(R.id.ivFotoGrande);

        if (esUrl) {
            Glide.with(requireContext()).load(urlOrUri).into(ivFoto);
        } else {
            Glide.with(requireContext()).load(Uri.parse(urlOrUri)).into(ivFoto);
        }

        new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setPositiveButton("Cerrar", null)
                .show();
    }

    private void confirmarEliminarFoto(int index, boolean esUrl) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Eliminar foto")
                .setMessage("¿Deseas eliminar esta foto?")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    if (esUrl) {
                        fotosUrlList.remove(index);
                    } else {
                        fotosUriList.remove(index);
                    }
                    mostrarFotos();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void guardarMantenimiento() {
        // Validar campos
        String empresa = inputEmpresa.getText() != null ? inputEmpresa.getText().toString().trim() : "";
        String horometro = inputHorometro.getText() != null ? inputHorometro.getText().toString().trim() : "";
        String fecha = inputFecha.getText() != null ? inputFecha.getText().toString().trim() : "";
        String comentarios = inputComentarios.getText() != null ? inputComentarios.getText().toString().trim() : "";

        if (empresa.isEmpty()) {
            Toast.makeText(getContext(), "Ingresa la empresa", Toast.LENGTH_SHORT).show();
            return;
        }

        if (horometro.isEmpty()) {
            Toast.makeText(getContext(), "Ingresa el horómetro", Toast.LENGTH_SHORT).show();
            return;
        }

        if (fecha.isEmpty()) {
            Toast.makeText(getContext(), "Selecciona la fecha", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> itemsSeleccionados = itemsAdapter.getItemsSeleccionados();

        progressBar.setVisibility(View.VISIBLE);

        // Subir fotos nuevas primero
        subirFotosNuevas(fotosSubidas -> {
            // Combinar URLs existentes con nuevas
            List<String> todasLasFotos = new ArrayList<>(fotosUrlList);
            todasLasFotos.addAll(fotosSubidas);

            // Crear o actualizar mantenimiento
            Mantenimiento mantenimiento = new Mantenimiento(
                    codigoGrupo,
                    empresa,
                    horometro,
                    fecha,
                    itemsSeleccionados,
                    comentarios,
                    todasLasFotos
            );

            if (mantenimientoId != null) {
                // Actualizar
                mantenimiento.setId(mantenimientoId);
                mantenimiento.setFechaCreacion(mantenimientoActual.getFechaCreacion());
                actualizarMantenimiento(mantenimiento);
            } else {
                // Crear nuevo
                crearMantenimiento(mantenimiento);
            }
        });
    }

    private void subirFotosNuevas(OnFotosSubidasListener listener) {
        if (fotosUriList.isEmpty()) {
            listener.onFotosSubidas(new ArrayList<>());
            return;
        }

        List<String> urlsSubidas = new ArrayList<>();
        final int[] fotosSubidas = {0};

        for (Uri uri : fotosUriList) {
            String fileName = "mantenimientos/" + codigoGrupo + "/" + System.currentTimeMillis() + ".jpg";
            StorageReference storageRef = storage.getReference().child(fileName);

            storageRef.putFile(uri)
                    .addOnSuccessListener(taskSnapshot ->
                            storageRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                                urlsSubidas.add(downloadUri.toString());
                                fotosSubidas[0]++;

                                if (fotosSubidas[0] == fotosUriList.size()) {
                                    listener.onFotosSubidas(urlsSubidas);
                                }
                            }))
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error subiendo foto", e);
                        fotosSubidas[0]++;

                        if (fotosSubidas[0] == fotosUriList.size()) {
                            listener.onFotosSubidas(urlsSubidas);
                        }
                    });
        }
    }

    private void crearMantenimiento(Mantenimiento mantenimiento) {
        db.collection("mantenimientos")
                .add(mantenimiento)
                .addOnSuccessListener(documentReference -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Mantenimiento guardado", Toast.LENGTH_SHORT).show();
                    requireActivity().getSupportFragmentManager().popBackStack();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error guardando", e);
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Error al guardar", Toast.LENGTH_SHORT).show();
                });
    }

    private void actualizarMantenimiento(Mantenimiento mantenimiento) {
        db.collection("mantenimientos").document(mantenimientoId)
                .set(mantenimiento)
                .addOnSuccessListener(aVoid -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Mantenimiento actualizado", Toast.LENGTH_SHORT).show();

                    // Eliminar fotos removidas de Storage
                    eliminarFotosRemovidasDeStorage();

                    requireActivity().getSupportFragmentManager().popBackStack();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error actualizando", e);
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Error al actualizar", Toast.LENGTH_SHORT).show();
                });
    }

    private void eliminarFotosRemovidasDeStorage() {
        if (mantenimientoActual == null || mantenimientoActual.getFotos() == null) return;

        for (String urlOriginal : mantenimientoActual.getFotos()) {
            if (!fotosUrlList.contains(urlOriginal)) {
                try {
                    StorageReference fotoRef = storage.getReferenceFromUrl(urlOriginal);
                    fotoRef.delete()
                            .addOnSuccessListener(aVoid -> Log.d(TAG, "Foto eliminada"))
                            .addOnFailureListener(e -> Log.e(TAG, "Error eliminando foto", e));
                } catch (Exception e) {
                    Log.e(TAG, "Error procesando URL", e);
                }
            }
        }
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

            if (modoLectura) {
                // Modo lectura: FAB dice "Editar"
                main.showGlobalFab("Editar", R.drawable.icon_editar_blanco, v -> {
                    modoLectura = false;
                    aplicarModoEdicion();
                    mostrarFotos();
                    configureGlobalFab(); // Actualizar FAB
                });
            } else {
                // Modo edición: FAB dice "Guardar"
                main.showGlobalFab("Guardar", R.drawable.icon_guardar_blanco, v -> guardarMantenimiento());
            }
        }
    }

    private void hideGlobalFab() {
        if (getActivity() instanceof com.example.maquirentapp.MainActivity) {
            ((com.example.maquirentapp.MainActivity) getActivity()).hideGlobalFab();
        }
    }

    private interface OnFotosSubidasListener {
        void onFotosSubidas(List<String> urls);
    }
}