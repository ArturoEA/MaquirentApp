package com.example.maquirentapp.View;

import static android.view.View.GONE;

import android.app.Activity;
import com.example.maquirentapp.Utils.CustomDatePickerDialog;
import android.app.Dialog;
import android.app.DownloadManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.maquirentapp.Model.InfoPlaca;
import com.example.maquirentapp.Model.Mantenimiento;
import com.example.maquirentapp.Model.MantenimientoConfiguracion;
import com.example.maquirentapp.Model.Usuario;
import com.example.maquirentapp.R;
import com.example.maquirentapp.adapters.ItemsMantenimientoSeleccionablesAdapter;
import com.example.maquirentapp.Utils.ImageUtils;
import com.example.maquirentapp.Utils.InformePdfVectorialGenerator;
import com.example.maquirentapp.Utils.MantenimientoSaludManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.example.maquirentapp.Utils.NetworkUtils;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.Source;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class NuevoMantenimientoFragment extends Fragment {
    private static final String TAG = "NuevoMantenimiento";
    private static final String ARG_CODIGO = "codigo";
    private static final String ARG_MANTENIMIENTO_ID = "mantenimiento_id";
    public static final String ARG_ITEMS_PRESELECCIONADOS = "items_preseleccionados";
    private static final int MAX_FOTOS = 4;

    private String codigoGrupo;
    private String mantenimientoId;
    private ArrayList<String> itemsPreseleccionados;
    private boolean modoLectura = true;
    private double ultimoHorometroRegistrado = 0.0;

    // Views
    private TextInputEditText inputEmpresa, inputHorometro, inputFecha, inputComentarios;
    private RecyclerView recyclerItems;
    private ShimmerFrameLayout shimmerItemsMantenimiento;
    private LinearLayout layoutFotos;
    private ProgressBar progressBar;

    private AlertDialog dialogCargando;
    private TextView tvMensajeCargando;
    private boolean isGuardando = false;

    private ItemsMantenimientoSeleccionablesAdapter itemsAdapter;
    private List<MantenimientoConfiguracion> itemsConfigList = new ArrayList<>();
    private List<MantenimientoConfiguracion> todosLosItemsConfig = new ArrayList<>();
    private String tipoEquipoActual = "Grupo electrógeno";
    private List<Uri> fotosUriList = new ArrayList<>();
    private List<String> fotosUrlList = new ArrayList<>();

    // Firebase
    private FirebaseFirestore db;
    private FirebaseStorage storage;

    private ActivityResultLauncher<String> imagePickerLauncher;
    private ActivityResultLauncher<String> imagePickerMultipleLauncher;

    private Mantenimiento mantenimientoActual;
    private InfoPlaca infoPlacaActual = null;
    private AlertDialog dialogCargaPdf;
    private MaterialButton btnAbrirInforme;

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
            itemsPreseleccionados = getArguments().getStringArrayList(ARG_ITEMS_PRESELECCIONADOS);
            modoLectura = mantenimientoId != null;
        }

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null && !modoLectura) {
                        agregarFoto(uri);
                    }
                });

        imagePickerMultipleLauncher = registerForActivityResult(
                new ActivityResultContracts.GetMultipleContents(),
                uris -> {
                    if (uris != null && !modoLectura && !uris.isEmpty()) {
                        int agregadas = 0;
                        for (Uri uri : uris) {
                            if (fotosUriList.size() + fotosUrlList.size() < MAX_FOTOS) {
                                fotosUriList.add(uri);
                                agregadas++;
                            } else {
                                break;
                            }
                        }
                        if (uris.size() > agregadas) {
                            Toast.makeText(getContext(), "Se agregaron " + agregadas + " fotos (límite de " + MAX_FOTOS + ")", Toast.LENGTH_SHORT).show();
                        }
                        mostrarFotos();
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
            mostrarFotos();
            cargarUltimoHorometroEquipo();
        }
        
        // Ensure FAB is shown early, but with delay to avoid race conditions
        if (getActivity() instanceof com.example.maquirentapp.MainActivity) {
            if (getView() != null) {
                getView().post(() -> {
                    if (isAdded() && getView() != null) {
                        configureGlobalFab();
                    }
                });
            } else {
                configureGlobalFab();
            }
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
        shimmerItemsMantenimiento = view.findViewById(R.id.shimmerItemsMantenimiento);
        layoutFotos = view.findViewById(R.id.layoutFotos);
        progressBar = view.findViewById(R.id.progressBarMantenimiento);

        btnAbrirInforme = view.findViewById(R.id.btnAbrirInforme);
        if (mantenimientoId != null) {
            btnAbrirInforme.setVisibility(View.VISIBLE);
            btnAbrirInforme.setOnClickListener(v -> mostrarDialogoInforme());
        }
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
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        try {
            if (inputFecha.getText() != null && !inputFecha.getText().toString().isEmpty()) {
                calendar.setTime(sdf.parse(inputFecha.getText().toString().trim()));
            }
        } catch (Exception ignored) {}

        new CustomDatePickerDialog(
                requireContext(),
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH),
                (year, month, dayOfMonth) -> {
                    calendar.set(year, month, dayOfMonth);
                    inputFecha.setText(sdf.format(calendar.getTime()));
                }
        ).show();
    }

    private void mostrarShimmerItems(boolean mostrar) {
        if (shimmerItemsMantenimiento != null) {
            if (mostrar) {
                shimmerItemsMantenimiento.setVisibility(View.VISIBLE);
                shimmerItemsMantenimiento.startShimmer();
            } else {
                shimmerItemsMantenimiento.stopShimmer();
                shimmerItemsMantenimiento.setVisibility(View.GONE);
            }
        }
        if (recyclerItems != null) {
            recyclerItems.setVisibility(mostrar ? View.GONE : View.VISIBLE);
        }
    }

    private int obtenerPrioridadSubsistema(String subsistema) {
        if (subsistema == null) return 99;
        String s = subsistema.toLowerCase().trim();
        if (s.contains("lubricaci")) return 1;
        if (s.contains("combustible")) return 2;
        if (s.contains("admis") || s.contains("escape")) return 3;
        if (s.contains("eléctric") || s.contains("electric") || s.contains("generac") || s.contains("control") || s.contains("compres")) return 4;
        if (s.contains("estructural") || s.contains("carreta") || s.contains("chasis")) return 5;
        return 6;
    }

    private MantenimientoConfiguracion buscarConfigPorId(String id) {
        if (id == null) return null;
        for (MantenimientoConfiguracion c : itemsConfigList) {
            if (id.equals(c.getId())) return c;
        }
        for (MantenimientoConfiguracion c : todosLosItemsConfig) {
            if (id.equals(c.getId())) return c;
        }
        return null;
    }

    private void filtrarYAplicarItems() {
        List<MantenimientoConfiguracion> filtrados = new ArrayList<>();
        String tipoBuscado = tipoEquipoActual != null ? tipoEquipoActual.toLowerCase().trim() : "grupo electrógeno";
        boolean esCompresora = tipoBuscado.contains("compresor");

        for (MantenimientoConfiguracion item : todosLosItemsConfig) {
            String t = item.getTipoEquipo() != null ? item.getTipoEquipo().toLowerCase().trim() : "";
            if (esCompresora) {
                if (t.contains("compresor")) {
                    filtrados.add(item);
                }
            } else {
                if (t.isEmpty() || t.contains("grupo") || t.contains("electr")) {
                    filtrados.add(item);
                }
            }
        }

        Collections.sort(filtrados, (a, b) -> {
            int prioA = obtenerPrioridadSubsistema(a.getSubsistema());
            int prioB = obtenerPrioridadSubsistema(b.getSubsistema());
            if (prioA != prioB) return Integer.compare(prioA, prioB);
            return Integer.compare(a.getIntervaloHoras(), b.getIntervaloHoras());
        });

        itemsConfigList.clear();
        itemsConfigList.addAll(filtrados);
        itemsAdapter.actualizarLista(itemsConfigList);

        if (mantenimientoActual != null && mantenimientoActual.getItemsRealizados() != null) {
            itemsAdapter.setItemsSeleccionados(mantenimientoActual.getItemsRealizados());
        } else if (mantenimientoActual == null && itemsPreseleccionados != null && !itemsPreseleccionados.isEmpty()) {
            itemsAdapter.setItemsSeleccionados(itemsPreseleccionados);
        }
    }

    private void aplicarItemsConfiguracion(List<MantenimientoConfiguracion> items) {
        todosLosItemsConfig.clear();
        if (items != null) {
            todosLosItemsConfig.addAll(items);
        }
        filtrarYAplicarItems();
    }

    private void cargarItemsConfiguracion() {
        Context ctx = getContext();
        if (ctx == null) return;

        // 1. Cargar instantáneamente del backup local en disco (0ms)
        List<MantenimientoConfiguracion> backupLocal = com.example.maquirentapp.Utils.MantenimientoOfflineManager.obtenerConfiguracionBackup(ctx);
        if (!backupLocal.isEmpty()) {
            aplicarItemsConfiguracion(backupLocal);
            mostrarShimmerItems(false);
        } else {
            mostrarShimmerItems(true);
        }

        // 2. Intentar cargar desde caché de Firestore (sin orderBy para garantizar compatibilidad offline)
        db.collection("mantenimientos_configuracion")
                .get(Source.CACHE)
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                        List<MantenimientoConfiguracion> lista = new ArrayList<>();
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            MantenimientoConfiguracion item = document.toObject(MantenimientoConfiguracion.class);
                            item.setId(document.getId());
                            lista.add(item);
                        }
                        aplicarItemsConfiguracion(lista);
                        mostrarShimmerItems(false);
                        com.example.maquirentapp.Utils.MantenimientoOfflineManager.guardarConfiguracionBackup(ctx, lista);
                    }
                })
                .addOnFailureListener(e -> {
                    if (itemsConfigList.isEmpty()) {
                        mostrarShimmerItems(true);
                    }
                });

        // 3. Si hay conexión a internet, sincronizar con el servidor en segundo plano
        if (NetworkUtils.isNetworkAvailable(ctx)) {
            db.collection("mantenimientos_configuracion")
                    .get(Source.SERVER)
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        mostrarShimmerItems(false);
                        if (queryDocumentSnapshots == null || queryDocumentSnapshots.isEmpty() || queryDocumentSnapshots.size() < 20) {
                            // Si está vacía o incompleta, sincronizar todas las tareas oficiales de Perkins y Stamford
                            com.example.maquirentapp.Utils.PlanesMantenimientoDefaults.sincronizarPlantillasCompletas(db, () -> {
                                db.collection("mantenimientos_configuracion").get(Source.SERVER).addOnSuccessListener(snap2 -> {
                                    if (snap2 != null && !snap2.isEmpty()) {
                                        List<MantenimientoConfiguracion> lista = new ArrayList<>();
                                        for (QueryDocumentSnapshot document : snap2) {
                                            MantenimientoConfiguracion item = document.toObject(MantenimientoConfiguracion.class);
                                            item.setId(document.getId());
                                            lista.add(item);
                                        }
                                        aplicarItemsConfiguracion(lista);
                                        com.example.maquirentapp.Utils.MantenimientoOfflineManager.guardarConfiguracionBackup(ctx, lista);
                                    }
                                });
                            });
                        } else {
                            List<MantenimientoConfiguracion> lista = new ArrayList<>();
                            for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                                MantenimientoConfiguracion item = document.toObject(MantenimientoConfiguracion.class);
                                item.setId(document.getId());
                                lista.add(item);
                            }
                            aplicarItemsConfiguracion(lista);
                            com.example.maquirentapp.Utils.MantenimientoOfflineManager.guardarConfiguracionBackup(ctx, lista);
                        }
                    })
                    .addOnFailureListener(e -> {
                        mostrarShimmerItems(false);
                        Log.e(TAG, "Error sincronizando items", e);
                    });
        }
    }

    private void cargarMantenimiento() {
        progressBar.setVisibility(View.VISIBLE);

        Context ctx = getContext();
        if (ctx != null) {
            Mantenimiento offlineM = com.example.maquirentapp.Utils.MantenimientoOfflineManager.obtenerPendientePorId(ctx, mantenimientoId);
            if (offlineM != null) {
                progressBar.setVisibility(GONE);
                mantenimientoActual = offlineM;
                mostrarDatosMantenimiento();
                return;
            }
        }

        boolean online = NetworkUtils.isNetworkAvailable(getContext());
        Source source = online ? Source.DEFAULT : Source.CACHE;

        db.collection("mantenimientos").document(mantenimientoId)
                .get(source)
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        mantenimientoActual = documentSnapshot.toObject(Mantenimiento.class);
                        if (mantenimientoActual != null) {
                            mantenimientoActual.setId(documentSnapshot.getId());
                            mostrarDatosMantenimiento();

                            db.collection("gruposElectrogenos").whereEqualTo("codigo", codigoGrupo).get(source)
                                    .addOnSuccessListener(query -> {
                                        if (!query.isEmpty()) {
                                            DocumentSnapshot docEq = query.getDocuments().get(0);
                                            String tipo = docEq.getString("tipoEquipo");
                                            if (tipo != null && !tipo.trim().isEmpty()) {
                                                tipoEquipoActual = tipo.trim();
                                                filtrarYAplicarItems();
                                            }
                                            String idEquipo = docEq.getId();
                                            db.collection("infoPlacaGrupo").whereEqualTo("idGrupo", idEquipo).get(source)
                                                    .addOnSuccessListener(query2 -> {
                                                        if (!query2.isEmpty()) {
                                                            infoPlacaActual = query2.getDocuments().get(0).toObject(com.example.maquirentapp.Model.InfoPlaca.class);
                                                        }
                                                    });
                                        }
                                    });
                        }
                    }
                    progressBar.setVisibility(GONE);
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(GONE);
                    Log.e(TAG, "Error cargando mantenimiento", e);
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
        if (layoutFotos == null) return;
        layoutFotos.removeAllViews();

        List<View> listaVistas = new ArrayList<>();

        // 1. Mostrar fotos existentes (URLs)
        for (int i = 0; i < fotosUrlList.size(); i++) {
            final int index = i;
            String url = fotosUrlList.get(i);
            View fotoView = crearVistaFoto(url, true);

            if (!modoLectura) {
                fotoView.findViewById(R.id.btnEliminarFoto).setOnClickListener(v -> {
                    confirmarEliminarFoto(index, true);
                });
            } else {
                fotoView.findViewById(R.id.btnEliminarFoto).setVisibility(GONE);
            }

            listaVistas.add(fotoView);
        }

        // 2. Mostrar fotos nuevas (URIs)
        for (int i = 0; i < fotosUriList.size(); i++) {
            final int index = i;
            Uri uri = fotosUriList.get(i);
            View fotoView = crearVistaFoto(uri.toString(), false);

            if (!modoLectura) {
                fotoView.findViewById(R.id.btnEliminarFoto).setOnClickListener(v -> {
                    confirmarEliminarFoto(index, false);
                });
            } else {
                fotoView.findViewById(R.id.btnEliminarFoto).setVisibility(GONE);
            }

            listaVistas.add(fotoView);
        }

        // 3. Botón para agregar foto (solo si no está en modo lectura y hay menos de MAX_FOTOS)
        if (!modoLectura && (fotosUriList.size() + fotosUrlList.size() < MAX_FOTOS)) {
            View btnAgregarFoto = crearBotonAgregarFoto();
            listaVistas.add(btnAgregarFoto);
        }

        // 4. Distribuir en cuadrícula 2x2 (filas de 2 elementos)
        LinearLayout filaActual = null;
        for (int i = 0; i < listaVistas.size(); i++) {
            if (i % 2 == 0) {
                filaActual = new LinearLayout(getContext());
                filaActual.setOrientation(LinearLayout.HORIZONTAL);
                layoutFotos.addView(filaActual);
            }
            if (filaActual != null) {
                filaActual.addView(listaVistas.get(i));
            }
        }
    }

    private View crearVistaFoto(String urlOrUri, boolean esUrl) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.item_foto_mantenimiento, layoutFotos, false);
        ImageView ivFoto = view.findViewById(R.id.ivFotoMantenimiento);

        if (getContext() != null) {
            if (esUrl) {
                Glide.with(this)
                        .load(urlOrUri)
                        .centerCrop()
                        .into(ivFoto);
            } else {
                Glide.with(this)
                        .load(Uri.parse(urlOrUri))
                        .centerCrop()
                        .into(ivFoto);
            }
        }

        // Click para ver foto en grande
        ivFoto.setOnClickListener(v -> mostrarFotoGrande(urlOrUri, esUrl));

        return view;
    }

    private View crearBotonAgregarFoto() {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.item_agregar_foto, layoutFotos, false);
        view.setOnClickListener(v -> {
            if (modoLectura) return;
            try {
                imagePickerMultipleLauncher.launch("image/*");
            } catch (Exception e) {
                imagePickerLauncher.launch("image/*");
            }
        });
        return view;
    }

    private void mostrarFotoGrande(String urlOrUri, boolean esUrl) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_ver_foto, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

//        dialog.getWindow().getAttributes().windowAnimations = R.style.DialogFadeAnimation;

        ImageView ivFoto = dialogView.findViewById(R.id.imgFull);
        LinearLayout btnDescargar = dialogView.findViewById(R.id.btnDownload);
        LinearLayout btnCompartir = dialogView.findViewById(R.id.btnShare);
        ImageView btnCerrar = dialogView.findViewById(R.id.btnClose);
        ImageView btnDelete = dialogView.findViewById(R.id.btnDelete);
        btnDelete.setVisibility(GONE);

        try {
            if (esUrl) {
                Glide.with(requireContext())
                        .load(urlOrUri)
                        .error(R.drawable.icon_mantenimiento_blanco)
                        .into(ivFoto);
            } else {
                Glide.with(requireContext())
                        .load(Uri.parse(urlOrUri))
                        .error(R.drawable.icon_mantenimiento_blanco)
                        .into(ivFoto);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error cargando imagen en visor", e);
            ivFoto.setImageResource(R.drawable.icon_mantenimiento_blanco);
        }

        btnDescargar.setOnClickListener(v -> {
            descargarFoto(urlOrUri, esUrl);
            dialog.dismiss();
        });

        btnCompartir.setOnClickListener(v -> compartirFoto(urlOrUri, esUrl));
        btnCerrar.setOnClickListener(v -> dialog.dismiss());
        ivFoto.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void descargarFoto(String urlOrUri, boolean esUrl) {
        try {
            if (esUrl) {
                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(urlOrUri));
                request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE);
                request.setTitle("Foto de mantenimiento");
                request.setDescription("Descargando imagen del mantenimiento");
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,
                        "mantenimiento_" + System.currentTimeMillis() + ".jpg");

                DownloadManager downloadManager = (DownloadManager) requireContext().getSystemService(Context.DOWNLOAD_SERVICE);
                if (downloadManager != null) {
                    downloadManager.enqueue(request);
                    Toast.makeText(getContext(), "Descarga iniciada", Toast.LENGTH_SHORT).show();
                }
            } else {
                try {
                    ContentValues values = new ContentValues();
                    values.put(MediaStore.Images.Media.DISPLAY_NAME, "mantenimiento_" + System.currentTimeMillis() + ".jpg");
                    values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
                    values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES);

                    Uri uri = requireContext().getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                    if (uri != null) {
                        OutputStream outputStream = requireContext().getContentResolver().openOutputStream(uri);
                        InputStream inputStream = requireContext().getContentResolver().openInputStream(Uri.parse(urlOrUri));

                        if (inputStream != null && outputStream != null) {
                            byte[] buffer = new byte[1024];
                            int length;
                            while ((length = inputStream.read(buffer)) > 0) {
                                outputStream.write(buffer, 0, length);
                            }
                            outputStream.close();
                            inputStream.close();

                            Toast.makeText(getContext(), "Foto guardada en Galería", Toast.LENGTH_SHORT).show();
                        }
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Error guardando foto", e);
                    Toast.makeText(getContext(), "Error al guardar foto", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error en descarga", e);
            Toast.makeText(getContext(), "Error al descargar", Toast.LENGTH_SHORT).show();
        }
    }

    private void compartirFoto(String urlOrUri, boolean esUrl) {
        try {
            if (esUrl) {
                descargarTemporalmenteYCompartir(urlOrUri);
            } else {
                Uri localUri = Uri.parse(urlOrUri);
                compartirArchivoDirecto(localUri);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error compartiendo foto", e);
            Toast.makeText(getContext(), "Error al compartir foto", Toast.LENGTH_SHORT).show();
        }
    }

    private void descargarTemporalmenteYCompartir(String imageUrl) {
        Toast.makeText(getContext(), "Preparando foto para compartir...", Toast.LENGTH_SHORT).show();

        File tempFile = new File(requireContext().getCacheDir(), "temp_share_" + System.currentTimeMillis() + ".jpg");

        StorageReference storageRef = storage.getReferenceFromUrl(imageUrl);

        storageRef.getFile(tempFile)
                .addOnSuccessListener(taskSnapshot -> {
                    Uri tempUri = FileProvider.getUriForFile(
                            requireContext(),
                            requireContext().getPackageName() + ".provider",
                            tempFile
                    );
                    compartirArchivoDirecto(tempUri);

                    new Handler().postDelayed(() -> {
                        if (tempFile.exists()) {
                            tempFile.delete();
                        }
                    }, 5 * 60 * 1000); // 5 minutos
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error descargando foto para compartir", e);
                    Toast.makeText(getContext(), "Error al preparar foto", Toast.LENGTH_SHORT).show();
                });
    }

    private void compartirArchivoDirecto(Uri imageUri) {
        try {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("image/jpeg");
            shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            Intent chooser = Intent.createChooser(shareIntent, "Compartir foto del mantenimiento");

            List<ResolveInfo> resInfoList = requireContext().getPackageManager().queryIntentActivities(shareIntent, PackageManager.MATCH_DEFAULT_ONLY);
            for (ResolveInfo resolveInfo : resInfoList) {
                String packageName = resolveInfo.activityInfo.packageName;
                requireContext().grantUriPermission(packageName, imageUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }

            startActivity(chooser);

        } catch (Exception e) {
            Log.e(TAG, "Error en compartirArchivoDirecto", e);
            Toast.makeText(getContext(), "No se pudo compartir la foto", Toast.LENGTH_SHORT).show();
        }
    }

    private void confirmarEliminarFoto(int index, boolean esUrl) {
        new MaterialAlertDialogBuilder(requireContext(), R.style.DialogoConFuenteAnta)
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

    private void mostrarDialogoCargando(String mensaje) {
        if (getContext() == null) return;
        if (dialogCargando == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
            View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_cargando, null);
            tvMensajeCargando = dialogView.findViewById(R.id.tvMensajeCargando);
            builder.setView(dialogView);
            builder.setCancelable(false);
            dialogCargando = builder.create();
            if (dialogCargando.getWindow() != null) {
                dialogCargando.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            }
        }
        if (tvMensajeCargando != null) {
            tvMensajeCargando.setText(mensaje);
        }
        if (!dialogCargando.isShowing()) {
            dialogCargando.show();
        }
    }

    private void actualizarMensajeCargando(String mensaje) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                if (tvMensajeCargando != null) {
                    tvMensajeCargando.setText(mensaje);
                }
            });
        }
    }

    private void ocultarDialogoCargando() {
        if (dialogCargando != null && dialogCargando.isShowing()) {
            dialogCargando.dismiss();
        }
        if (progressBar != null) {
            progressBar.setVisibility(GONE);
        }
    }

    private void guardarMantenimiento() {
        if (isGuardando) {
            return;
        }

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

        double nuevoH = MantenimientoSaludManager.parseDoubleSafe(horometro);
        if (!modoLectura && ultimoHorometroRegistrado > 0) {
            MantenimientoSaludManager.ResultadoValidacionHorometro val =
                    MantenimientoSaludManager.validarHorometroMantenimiento(ultimoHorometroRegistrado, nuevoH);
            if (!val.esValido) {
                new MaterialAlertDialogBuilder(requireContext(), R.style.DialogoConFuenteAnta)
                        .setTitle("Horómetro Inválido")
                        .setMessage(val.mensajeError)
                        .setPositiveButton("Corregir", null)
                        .show();
                return;
            }
            if (val.esSaltoGrande) {
                new MaterialAlertDialogBuilder(requireContext(), R.style.DialogoConFuenteAnta)
                        .setTitle("Confirmar incremento de horas")
                        .setMessage(val.mensajeError)
                        .setPositiveButton("Sí, es correcto", (d, w) -> iniciarProcesoGuardado(empresa, horometro, fecha, comentarios, itemsSeleccionados))
                        .setNegativeButton("Corregir", null)
                        .show();
                return;
            }
        }

        iniciarProcesoGuardado(empresa, horometro, fecha, comentarios, itemsSeleccionados);
    }

    private void iniciarProcesoGuardado(String empresa, String horometro, String fecha, String comentarios, List<String> itemsSeleccionados) {
        boolean online = NetworkUtils.isNetworkAvailable(getContext());

        // Si no hay conexión, guardar directamente en modo offline (datos + fotos locales)
        if (!online) {
            isGuardando = true;
            hideGlobalFab();
            guardarMantenimientoOfflineDirecto(empresa, horometro, fecha, itemsSeleccionados, comentarios);
            return;
        }

        isGuardando = true;
        hideGlobalFab();
        mostrarDialogoCargando(!fotosUriList.isEmpty() ? "Subiendo fotos..." : "Guardando mantenimiento...");

        subirFotosNuevas(empresa, horometro, fecha, itemsSeleccionados, comentarios, fotosSubidas -> {
            List<String> todasLasFotos = new ArrayList<>(fotosUrlList);
            todasLasFotos.addAll(fotosSubidas);
            actualizarMensajeCargando("Guardando datos...");
            procederAGuardarMantenimiento(empresa, horometro, fecha, itemsSeleccionados, comentarios, todasLasFotos);
        });
    }

    private void guardarMantenimientoOfflineDirecto(String empresa, String horometro, String fecha,
                                                    List<String> itemsSeleccionados, String comentarios) {
        Context context = getContext();
        if (context == null) {
            isGuardando = false;
            configureGlobalFab();
            return;
        }

        mostrarDialogoCargando("Guardando localmente en el dispositivo...");

        new Thread(() -> {
            List<String> todasLasFotos = new ArrayList<>(fotosUrlList);

            // Guardar localmente cada foto seleccionada en almacenamiento seguro
            for (Uri uri : fotosUriList) {
                String localPath = com.example.maquirentapp.Utils.MantenimientoOfflineManager.guardarFotoLocal(context, uri);
                if (localPath != null) {
                    todasLasFotos.add(localPath);
                }
            }

            String id = mantenimientoId;
            if (id == null || id.trim().isEmpty()) {
                id = db.collection("mantenimientos").document().getId();
            }

            Mantenimiento m = new Mantenimiento(
                    codigoGrupo,
                    empresa,
                    horometro,
                    fecha,
                    itemsSeleccionados,
                    comentarios,
                    todasLasFotos
            );
            m.setId(id);
            long tsFecha = Mantenimiento.parseFechaToMillis(fecha);
            m.setFechaCreacion(tsFecha > 0 ? tsFecha : (mantenimientoActual != null ? mantenimientoActual.getFechaCreacion() : System.currentTimeMillis()));

            if (mantenimientoActual != null) {
                m.setProximoHorometro(mantenimientoActual.getProximoHorometro());
                m.setProximaFecha(mantenimientoActual.getProximaFecha());
                m.setTieneBandeja(mantenimientoActual.isTieneBandeja());
                m.setTieneExtintor(mantenimientoActual.isTieneExtintor());
                m.setTieneKit(mantenimientoActual.isTieneKit());
                m.setTienePuestaTierra(mantenimientoActual.isTienePuestaTierra());
                m.setCliente(mantenimientoActual.getCliente());
                m.setLugar(mantenimientoActual.getLugar());
                m.setEstadoMaquina(mantenimientoActual.getEstadoMaquina());
                m.setUbicacionMaquina(mantenimientoActual.getUbicacionMaquina());
                m.setTipoServicio(mantenimientoActual.getTipoServicio());
                m.setAceiteUtilizado(mantenimientoActual.getAceiteUtilizado());
                m.setContactoCliente(mantenimientoActual.getContactoCliente());
                m.setFallaEncontrada(mantenimientoActual.getFallaEncontrada());
                m.setCausaFalla(mantenimientoActual.getCausaFalla());
                m.setTrabajosRealizados(mantenimientoActual.getTrabajosRealizados());
                m.setTecnicoFirmaId(mantenimientoActual.getTecnicoFirmaId());
                m.setSupervisorFirmaId(mantenimientoActual.getSupervisorFirmaId());
                m.setCodigosFiltrosUsados(mantenimientoActual.getCodigosFiltrosUsados());
                m.setCantidadAceite(mantenimientoActual.getCantidadAceite());
            }

            // Guardar en almacenamiento offline
            com.example.maquirentapp.Utils.MantenimientoOfflineManager.guardarMantenimientoOffline(context, m);

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    isGuardando = false;
                    ocultarDialogoCargando();

                    int numFotos = fotosUriList.size();
                    String detalleFotos = numFotos > 0
                            ? (" y sus " + numFotos + " " + (numFotos == 1 ? "fotografía" : "fotografías") + " se han guardado localmente en tu dispositivo.")
                            : " se ha guardado localmente en tu dispositivo.";

                    new MaterialAlertDialogBuilder(requireContext(), R.style.DialogoConFuenteAnta)
                            .setTitle("Guardado sin conexión")
                            .setMessage("No tienes conexión a internet en este momento.\n\nEl mantenimiento" + detalleFotos
                                    + "\n\nSe sincronizará automáticamente con la nube cuando recuperes la conexión.")
                            .setCancelable(false)
                            .setPositiveButton("Entendido", (dialog, which) -> {
                                volverAtras();
                            })
                            .show();
                });
            }
        }).start();
    }

    private void procederAGuardarMantenimiento(String empresa, String horometro, String fecha,
                                              List<String> itemsSeleccionados, String comentarios,
                                              List<String> fotos) {
        isGuardando = true;
        hideGlobalFab();

        Mantenimiento mantenimiento = new Mantenimiento(
                codigoGrupo,
                empresa,
                horometro,
                fecha,
                itemsSeleccionados,
                comentarios,
                fotos
        );

        long tsFecha = Mantenimiento.parseFechaToMillis(fecha);
        long fechaDefinitiva = tsFecha > 0 ? tsFecha : (mantenimientoActual != null ? mantenimientoActual.getFechaCreacion() : System.currentTimeMillis());

        if (mantenimientoId != null) {
            mantenimiento.setId(mantenimientoId);
            mantenimiento.setFechaCreacion(fechaDefinitiva);
            actualizarMantenimiento(mantenimiento);
        } else {
            mantenimiento.setFechaCreacion(fechaDefinitiva);
            crearMantenimiento(mantenimiento);
        }
    }

    private void subirFotosNuevas(String empresa, String horometro, String fecha,
                                  List<String> itemsSeleccionados, String comentarios,
                                  OnFotosSubidasListener listener) {
        if (fotosUriList.isEmpty()) {
            listener.onFotosSubidas(new ArrayList<>());
            return;
        }

        Context context = getContext();
        if (context == null) {
            isGuardando = false;
            ocultarDialogoCargando();
            configureGlobalFab();
            listener.onFotosSubidas(new ArrayList<>());
            return;
        }

        List<String> urlsSubidas = Collections.synchronizedList(new ArrayList<>());
        final int[] fotosProcesadas = {0};
        final boolean[] finalizado = {false};
        final int total = fotosUriList.size();

        actualizarMensajeCargando("Subiendo foto 1 de " + total + "...");

        Handler handlerTimeout = new Handler(android.os.Looper.getMainLooper());
        Runnable timeoutRunnable = () -> {
            synchronized (finalizado) {
                if (!finalizado[0]) {
                    finalizado[0] = true;
                    ocultarDialogoCargando();
                    Log.w(TAG, "Tiempo agotado en subida online de fotos. Guardando localmente en modo offline.");
                    guardarMantenimientoOfflineDirecto(empresa, horometro, fecha, itemsSeleccionados, comentarios);
                }
            }
        };
        // 12 segundos de tiempo límite para la subida
        handlerTimeout.postDelayed(timeoutRunnable, 12000);

        for (Uri uri : fotosUriList) {
            byte[] dataImagen = ImageUtils.comprimirImagen(context, uri);

            if (dataImagen != null) {
                String fileName = "mantenimientos/" + codigoGrupo + "/" + System.currentTimeMillis() + "_" + (int)(Math.random() * 1000) + ".jpg";
                StorageReference storageRef = storage.getReference().child(fileName);

                StorageMetadata metadata = new StorageMetadata.Builder()
                        .setContentType("image/jpeg")
                        .build();

                storageRef.putBytes(dataImagen, metadata)
                        .addOnSuccessListener(taskSnapshot ->
                                storageRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                                    urlsSubidas.add(downloadUri.toString());
                                    verificarFinSubidaConTimeout(total, fotosProcesadas, urlsSubidas, finalizado, handlerTimeout, timeoutRunnable, listener);
                                }).addOnFailureListener(e -> {
                                    Log.e(TAG, "Error obteniendo URL de descarga", e);
                                    verificarFinSubidaConTimeout(total, fotosProcesadas, urlsSubidas, finalizado, handlerTimeout, timeoutRunnable, listener);
                                }))
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Error subiendo foto", e);
                            verificarFinSubidaConTimeout(total, fotosProcesadas, urlsSubidas, finalizado, handlerTimeout, timeoutRunnable, listener);
                        });
            } else {
                Log.e(TAG, "Error al comprimir imagen: " + uri.toString());
                verificarFinSubidaConTimeout(total, fotosProcesadas, urlsSubidas, finalizado, handlerTimeout, timeoutRunnable, listener);
            }
        }
    }

    private void verificarFinSubidaConTimeout(int total, int[] procesadas, List<String> urls,
                                              boolean[] finalizado, Handler handler,
                                              Runnable timeoutRunnable, OnFotosSubidasListener listener) {
        synchronized (finalizado) {
            if (finalizado[0]) return;
            procesadas[0]++;
            actualizarMensajeCargando("Subiendo foto " + Math.min(procesadas[0] + 1, total) + " de " + total + "...");
            if (procesadas[0] >= total) {
                finalizado[0] = true;
                handler.removeCallbacks(timeoutRunnable);
                listener.onFotosSubidas(urls);
            }
        }
    }

    private void crearMantenimiento(Mantenimiento mantenimiento) {
        db.collection("mantenimientos")
                .add(mantenimiento)
                .addOnSuccessListener(documentReference -> {
                    actualizarRelojesDeMaquina(mantenimiento);
                    isGuardando = false;
                    ocultarDialogoCargando();
                    if (getContext() == null) return;
                    Toast.makeText(getContext(), "Mantenimiento guardado", Toast.LENGTH_SHORT).show();
                    volverAtras();
                })
                .addOnFailureListener(e -> {
                    isGuardando = false;
                    ocultarDialogoCargando();
                    if (getContext() == null) return;
                    Log.w(TAG, "Fallo al guardar en la nube. Guardando en modo offline.", e);
                    com.example.maquirentapp.Utils.MantenimientoOfflineManager.guardarMantenimientoOffline(getContext(), mantenimiento);
                    Toast.makeText(getContext(), "Guardado localmente en tu dispositivo", Toast.LENGTH_SHORT).show();
                    volverAtras();
                });
    }

    private void actualizarMantenimiento(Mantenimiento mantenimiento) {
        db.collection("mantenimientos").document(mantenimientoId)
                .set(mantenimiento)
                .addOnSuccessListener(aVoid -> {
                    actualizarRelojesDeMaquina(mantenimiento);
                    isGuardando = false;
                    ocultarDialogoCargando();
                    if (getContext() == null) return;
                    Toast.makeText(getContext(), "Mantenimiento actualizado", Toast.LENGTH_SHORT).show();
                    eliminarFotosRemovidasDeStorage();
                    volverAtras();
                })
                .addOnFailureListener(e -> {
                    isGuardando = false;
                    ocultarDialogoCargando();
                    if (getContext() == null) return;
                    Log.w(TAG, "Fallo al actualizar en la nube. Guardando en modo offline.", e);
                    com.example.maquirentapp.Utils.MantenimientoOfflineManager.guardarMantenimientoOffline(getContext(), mantenimiento);
                    Toast.makeText(getContext(), "Actualizado localmente en tu dispositivo", Toast.LENGTH_SHORT).show();
                    volverAtras();
                });
    }

    private void volverAtras() {
        if (!isAdded()) return;
        if (getView() != null) {
            try {
                if (androidx.navigation.Navigation.findNavController(getView()).popBackStack()) {
                    return;
                }
            } catch (Exception ignored) {}
        }
        if (getActivity() != null) {
            try {
                requireActivity().getSupportFragmentManager().popBackStack();
            } catch (Exception ignored) {}
        }
    }

    private void cargarUltimoHorometroEquipo() {
        if (codigoGrupo == null) return;
        db.collection("gruposElectrogenos").whereEqualTo("codigo", codigoGrupo).get().addOnSuccessListener(query -> {
            if (query != null && !query.isEmpty()) {
                DocumentSnapshot doc = query.getDocuments().get(0);
                Double h = doc.getDouble("horometroActual");
                if (h != null && h > 0) {
                    ultimoHorometroRegistrado = h;
                }
                String tipo = doc.getString("tipoEquipo");
                if (tipo != null && !tipo.trim().isEmpty()) {
                    tipoEquipoActual = tipo.trim();
                    filtrarYAplicarItems();
                }
            }
            if (ultimoHorometroRegistrado <= 0) {
                db.collection("mantenimientos")
                        .whereEqualTo("codigoGrupo", codigoGrupo)
                        .get()
                        .addOnSuccessListener(snapM -> {
                            for (DocumentSnapshot d : snapM.getDocuments()) {
                                double h = MantenimientoSaludManager.parseDoubleSafe(d.getString("horometro"));
                                if (h > ultimoHorometroRegistrado) ultimoHorometroRegistrado = h;
                            }
                        });
            }
        });
    }

    private void actualizarRelojesDeMaquina(Mantenimiento mantenimiento) {
        if (mantenimiento == null || mantenimiento.getCodigoGrupo() == null) return;
        String codigo = mantenimiento.getCodigoGrupo();
        double horometro = MantenimientoSaludManager.parseDoubleSafe(mantenimiento.getHorometro());
        final List<String> items = mantenimiento.getItemsRealizados() != null ? mantenimiento.getItemsRealizados() : new ArrayList<>();

        db.collection("gruposElectrogenos")
                .whereEqualTo("codigo", codigo)
                .get()
                .addOnSuccessListener(query -> {
                    if (query != null && !query.isEmpty()) {
                        for (DocumentSnapshot doc : query.getDocuments()) {
                            String docId = doc.getId();
                            Map<String, Object> relojesTareas = doc.contains("relojesTareas") && doc.get("relojesTareas") instanceof Map
                                    ? (Map<String, Object>) doc.get("relojesTareas") : new HashMap<>();
                            Map<String, Object> fechasTareas = doc.contains("fechasTareas") && doc.get("fechasTareas") instanceof Map
                                    ? (Map<String, Object>) doc.get("fechasTareas") : new HashMap<>();
                            Map<String, Object> retornosTareas = doc.contains("retornosTareas") && doc.get("retornosTareas") instanceof Map
                                    ? (Map<String, Object>) doc.get("retornosTareas") : new HashMap<>();

                            Map<String, Object> relojesComponentes = doc.contains("relojesComponentes") && doc.get("relojesComponentes") instanceof Map
                                    ? (Map<String, Object>) doc.get("relojesComponentes") : new HashMap<>();
                            Map<String, Object> fechasComponentes = doc.contains("fechasComponentes") && doc.get("fechasComponentes") instanceof Map
                                    ? (Map<String, Object>) doc.get("fechasComponentes") : new HashMap<>();

                            long now = System.currentTimeMillis();
                            long contadorRetornosActual = doc.contains("contadorRetornos") && doc.getLong("contadorRetornos") != null
                                    ? doc.getLong("contadorRetornos") : 0L;
                            Double horometroActualDoc = doc.getDouble("horometroActual");

                            // Actualizar única y exclusivamente las tareas marcadas (sin retroceder relojes más recientes)
                            for (String it : items) {
                                if (it == null || it.trim().isEmpty()) continue;
                                MantenimientoConfiguracion cfg = buscarConfigPorId(it);
                                String taskId = it.trim();
                                String taskSlug = cfg != null ? cfg.getSlugNormalizado() : "";

                                // 1. Reloj de horas y fecha por ID de tarea (solo si >= al último registrado)
                                double relojPrevId = relojesTareas.get(taskId) instanceof Number
                                        ? ((Number) relojesTareas.get(taskId)).doubleValue() : -1.0;
                                if (horometro >= relojPrevId) {
                                    relojesTareas.put(taskId, horometro);
                                    fechasTareas.put(taskId, now);
                                }

                                // 2. Reloj por slug semántico para persistencia entre versiones
                                if (!taskSlug.isEmpty()) {
                                    double relojPrevSlug = relojesTareas.get(taskSlug) instanceof Number
                                            ? ((Number) relojesTareas.get(taskSlug)).doubleValue() : -1.0;
                                    if (horometro >= relojPrevSlug) {
                                        relojesTareas.put(taskSlug, horometro);
                                        fechasTareas.put(taskSlug, now);
                                    }
                                }

                                // 3. Reloj por retornos de alquiler si corresponde
                                if (cfg != null && cfg.isEsInspeccionRetorno()) {
                                    retornosTareas.put(taskId, contadorRetornosActual);
                                    if (!taskSlug.isEmpty()) {
                                        retornosTareas.put(taskSlug, contadorRetornosActual);
                                    }
                                }

                                // 4. Retrocompatibilidad para módulo de salud tradicional
                                String nombre = cfg != null && cfg.getNombre() != null ? cfg.getNombre().toLowerCase() : "";
                                if (nombre.contains("aceite") && (nombre.contains("cambio") || nombre.contains("15w-40"))) {
                                    double relojPrevAceite = relojesTareas.get("aceite") instanceof Number
                                            ? ((Number) relojesTareas.get("aceite")).doubleValue() : -1.0;
                                    if (horometro >= relojPrevAceite) {
                                        relojesTareas.put("aceite", horometro);
                                        fechasTareas.put("aceite", now);
                                        relojesComponentes.put("aceite", horometro);
                                        fechasComponentes.put("aceite", now);
                                    }
                                }
                            }

                            // Sincronizar copias en relojesComponentes para compatibilidad
                            relojesComponentes.putAll(relojesTareas);
                            fechasComponentes.putAll(fechasTareas);

                            Map<String, Object> updates = new HashMap<>();
                            // Solo actualizar horometroActual si el mantenimiento registrado supera al actual
                            if (horometro > 0 && (horometroActualDoc == null || horometro > horometroActualDoc)) {
                                updates.put("horometroActual", horometro);
                                updates.put("fechaActualizacionHorometro", now);
                            }
                            updates.put("relojesTareas", relojesTareas);
                            updates.put("fechasTareas", fechasTareas);
                            updates.put("retornosTareas", retornosTareas);
                            updates.put("relojesComponentes", relojesComponentes);
                            updates.put("fechasComponentes", fechasComponentes);

                            db.collection("gruposElectrogenos").document(docId).update(updates);
                        }
                    }
                });

        // Completar automáticamente tarea preventiva pendiente si existía
        db.collection("tareas")
                .whereEqualTo("codigoGrupo", codigo)
                .whereEqualTo("tipoTarea", "PREVENTIVO")
                .whereEqualTo("completada", false)
                .get()
                .addOnSuccessListener(snapT -> {
                    if (snapT != null && !snapT.isEmpty()) {
                        for (DocumentSnapshot tDoc : snapT.getDocuments()) {
                            db.collection("tareas").document(tDoc.getId()).update("completada", true);
                        }
                    }
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

    private void mostrarDialogoInforme() {
        Dialog dialog = new Dialog(requireContext(), android.R.style.Theme_DeviceDefault_Light_NoActionBar);
        dialog.setContentView(R.layout.dialog_informe_mantenimiento);

        Toolbar toolbar = dialog.findViewById(R.id.toolbarDialog);
        RadioGroup rgEstado = dialog.findViewById(R.id.rgEstado);
        RadioGroup rgUbicacion = dialog.findViewById(R.id.rgUbicacion);
        RadioGroup rgDefServicio = dialog.findViewById(R.id.rgDefinicionServicio);
        TextInputEditText inputCliente = dialog.findViewById(R.id.inputClienteReporte);
        TextInputEditText inputLugar = dialog.findViewById(R.id.inputLugarReporte);
        AutoCompleteTextView spinnerAceite = dialog.findViewById(R.id.spinnerAceite);
        AutoCompleteTextView spinnerCantidadAceite = dialog.findViewById(R.id.spinnerCantidadAceite);
        AutoCompleteTextView inputContacto = dialog.findViewById(R.id.inputContacto);
        TextInputEditText inputTrabajos = dialog.findViewById(R.id.inputTrabajosRealizados);
        TextInputEditText inputFallas = dialog.findViewById(R.id.inputFallas);
        AutoCompleteTextView spinnerTecnico = dialog.findViewById(R.id.spinnerTecnico);
        AutoCompleteTextView spinnerSupervisor = dialog.findViewById(R.id.spinnerSupervisor);
        LinearLayout layoutFiltros = dialog.findViewById(R.id.layoutCodigosFiltros);
        TextInputEditText inputProxFecha = dialog.findViewById(R.id.inputProxFecha);
        android.widget.CheckBox chkBandeja = dialog.findViewById(R.id.chkBandeja);
        android.widget.CheckBox chkExtintor = dialog.findViewById(R.id.chkExtintor);
        android.widget.CheckBox chkKit = dialog.findViewById(R.id.chkKit);
        android.widget.CheckBox chkTierra = dialog.findViewById(R.id.chkTierra);

        chkBandeja.setButtonTintList(null);
        chkExtintor.setButtonTintList(null);
        chkKit.setButtonTintList(null);
        chkTierra.setButtonTintList(null);

        inputProxFecha.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            try {
                if (inputProxFecha.getText() != null && !inputProxFecha.getText().toString().isEmpty()) {
                    calendar.setTime(sdf.parse(inputProxFecha.getText().toString().trim()));
                }
            } catch (Exception ignored) {}

            new CustomDatePickerDialog(
                    requireContext(),
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH),
                    (year, month, dayOfMonth) -> {
                        calendar.set(year, month, dayOfMonth);
                        inputProxFecha.setText(sdf.format(calendar.getTime()));
                    }
            ).show();
        });

        // Opciones sugeridas de Aceite
        String[] aceitesComunes = new String[]{
                "Mobil Delvac Mx Esp 15W-40",
                "Shell Rimula R4 X 15W-40",
                "Chevron Delo 400 15W-40",
                "Castrol CRB Turbomax 15W-40"
        };
        ArrayAdapter<String> adapterAceite = new ArrayAdapter<>(
                requireContext(),
                R.layout.spinner_item_black,
                aceitesComunes
        );
        spinnerAceite.setAdapter(adapterAceite);

        // Opciones sugeridas de Cantidad de Aceite
        String[] cantidadesAceite = new String[]{
                "1 galón", "2 galones", "3 galones", "4 galones", "5 galones", "6 galones"
        };
        android.widget.ArrayAdapter<String> adapterCantidad = new android.widget.ArrayAdapter<>(
                requireContext(),
                R.layout.spinner_item_black,
                cantidadesAceite
        );
        spinnerCantidadAceite.setAdapter(adapterCantidad);

        // 3. Configurar Toolbar
        toolbar.setNavigationOnClickListener(v -> dialog.dismiss());

        List<Usuario> listaUsuarios = new ArrayList<>();
        db.collection("usuarios").whereEqualTo("estado", "activo").get().addOnSuccessListener(query -> {
            List<String> nombresUsuarios = new ArrayList<>();
            nombresUsuarios.add("Ninguno");
            for (QueryDocumentSnapshot doc : query) {
                Usuario u = doc.toObject(Usuario.class);
                if (u.getFirmaUrl() != null && !u.getFirmaUrl().isEmpty()) {
                    nombresUsuarios.add(u.getNombre());
                    listaUsuarios.add(u);
                }
            }
            ArrayAdapter<String> adapterUsers = new ArrayAdapter<>(requireContext(), R.layout.spinner_item_black, nombresUsuarios);
            spinnerTecnico.setAdapter(adapterUsers);
            spinnerSupervisor.setAdapter(adapterUsers);
        });

        LinearLayout layoutListaRutinas = dialog.findViewById(R.id.layoutListaRutinas);
        Button btnAgregarRutina = dialog.findViewById(R.id.btnAgregarRutina);

        btnAgregarRutina.setOnClickListener(v -> agregarFilaRutinaVisual(layoutListaRutinas, "Nueva actividad", true));

        final Map<String, String> codigosGuardadosHolder = new HashMap<>();
        if (mantenimientoActual != null && mantenimientoActual.getCodigosFiltrosUsados() != null) {
            codigosGuardadosHolder.putAll(mantenimientoActual.getCodigosFiltrosUsados());
        }

        db.collection("mantenimientos").document(mantenimientoId).get().addOnSuccessListener(doc -> {
            if (doc.contains("datosInforme")) {
                Map<String, Object> mapGuardado = (Map<String, Object>) doc.get("datosInforme");
                if (mapGuardado != null) {
                    inputCliente.setText((String) mapGuardado.get("cliente"));
                    inputLugar.setText((String) mapGuardado.get("lugar"));
                    spinnerAceite.setText((String) mapGuardado.get("aceite"), false);
                    spinnerCantidadAceite.setText((String) mapGuardado.get("cantAceite"), false);
                    inputContacto.setText((String) mapGuardado.get("contacto"), false);
                    inputFallas.setText((String) mapGuardado.get("fallas"));
                    inputTrabajos.setText((String) mapGuardado.get("trabajos"));
                    spinnerTecnico.setText((String) mapGuardado.get("tecnico"), false);
                    spinnerSupervisor.setText((String) mapGuardado.get("supervisor"), false);
                    inputProxFecha.setText((String) mapGuardado.get("proxFecha"));
                    List<Map<String, Object>> rutinasGuardadas = (List<Map<String, Object>>) mapGuardado.get("rutinasList");
                    if (rutinasGuardadas != null && !rutinasGuardadas.isEmpty()) {
                        for (Map<String, Object> r : rutinasGuardadas) {
                            agregarFilaRutinaVisual(layoutListaRutinas, (String) r.get("nombre"), (Boolean) r.get("activa"));
                        }
                    } else {
                        cargarRutinasPorDefecto(layoutListaRutinas);
                    }

                    if (mapGuardado.containsKey("chkBandeja"))
                        chkBandeja.setChecked((Boolean) mapGuardado.get("chkBandeja"));
                    if (mapGuardado.containsKey("chkExtintor"))
                        chkExtintor.setChecked((Boolean) mapGuardado.get("chkExtintor"));
                    if (mapGuardado.containsKey("chkKit"))
                        chkKit.setChecked((Boolean) mapGuardado.get("chkKit"));
                    if (mapGuardado.containsKey("chkTierra"))
                        chkTierra.setChecked((Boolean) mapGuardado.get("chkTierra"));

                    String estadoGuardado = (String) mapGuardado.get("estado");
                    if ("Inoperativa".equals(estadoGuardado)) rgEstado.check(R.id.rbInoperativa);
                    else if ("En reparación por terceros".equals(estadoGuardado)) rgEstado.check(R.id.rbReparacionTerceros);
                    else if (estadoGuardado != null) rgEstado.check(R.id.rbOperativa);

                    String ubicacionGuardada = (String) mapGuardado.get("ubicacion");
                    if ("Campo".equals(ubicacionGuardada)) rgUbicacion.check(R.id.rbCampo);
                    else if ("Taller de terceros".equals(ubicacionGuardada)) rgUbicacion.check(R.id.rbTallerTerceros);
                    else if (ubicacionGuardada != null) rgUbicacion.check(R.id.rbTallerCliente);

                    String defServicioGuardado = (String) mapGuardado.get("defServicio");
                    if ("Evaluación".equals(defServicioGuardado)) rgDefServicio.check(R.id.rbServEvaluacion);
                    else if ("Entrega".equals(defServicioGuardado)) rgDefServicio.check(R.id.rbServEntrega);
                    else if ("Realizar Ajuste".equals(defServicioGuardado)) rgDefServicio.check(R.id.rbServAjuste);
                    else if (defServicioGuardado != null) rgDefServicio.check(R.id.rbServMantenimiento);

                    // Restaurar códigos de filtros guardados
                    if (mapGuardado.containsKey("codigosFiltros")) {
                        Object objF = mapGuardado.get("codigosFiltros");
                        if (objF instanceof Map) {
                            Map<?, ?> mapF = (Map<?, ?>) objF;
                            for (Map.Entry<?, ?> entry : mapF.entrySet()) {
                                if (entry.getKey() != null && entry.getValue() != null) {
                                    codigosGuardadosHolder.put(entry.getKey().toString(), entry.getValue().toString());
                                }
                            }
                        }
                    }
                    if (doc.contains("codigosFiltrosUsados")) {
                        Object objF2 = doc.get("codigosFiltrosUsados");
                        if (objF2 instanceof Map) {
                            Map<?, ?> mapF2 = (Map<?, ?>) objF2;
                            for (Map.Entry<?, ?> entry : mapF2.entrySet()) {
                                if (entry.getKey() != null && entry.getValue() != null) {
                                    codigosGuardadosHolder.put(entry.getKey().toString(), entry.getValue().toString());
                                }
                            }
                        }
                    }

                    // Si las vistas en layoutFiltros ya fueron añadidas, restaurar su contenido
                    restaurarCodigosEnLayoutFiltros(layoutFiltros, codigosGuardadosHolder);
                }
            } else {
                StringBuilder trabajosAuto = new StringBuilder("Mantenimiento preventivo. ");
                if (mantenimientoActual.getItemsRealizados() != null) {
                    for (String idItem : mantenimientoActual.getItemsRealizados()) {
                        for (MantenimientoConfiguracion config : itemsConfigList) {
                            if (config.getId().equals(idItem))
                                trabajosAuto.append("Cambio de ").append(config.getNombre().toLowerCase()).append(". ");
                        }
                    }
                }
                inputTrabajos.setText(trabajosAuto.toString());
                if (mantenimientoActual.getCliente() != null)
                    inputCliente.setText(mantenimientoActual.getCliente());
                if (mantenimientoActual.getLugar() != null)
                    inputLugar.setText(mantenimientoActual.getLugar());
                cargarRutinasPorDefecto(layoutListaRutinas);
            }
        });

        // -- LÓGICA DE FILTROS DINÁMICOS CORREGIDA --
        layoutFiltros.removeAllViews();

        // 1. Identificar qué filtros se cambiaron revisando los itemsRealizados
        List<String> filtrosCambiados = new ArrayList<>();
        if (mantenimientoActual.getItemsRealizados() != null) {
            for (String idItem : mantenimientoActual.getItemsRealizados()) {
                for (MantenimientoConfiguracion config : itemsConfigList) {
                    if (config.getId().equals(idItem) && config.getNombre().toLowerCase().contains("filtro")) {
                        filtrosCambiados.add(config.getNombre());
                    }
                }
            }
        }

        // 2. Si se cambió algún filtro, mostramos el contenedor y consultamos Firebase
        if (!filtrosCambiados.isEmpty()) {
            layoutFiltros.setVisibility(View.VISIBLE);

            TextView tvTituloFiltros = new TextView(requireContext());
            tvTituloFiltros.setText("Códigos de filtros utilizados:");
            Typeface typefaceAnta = ResourcesCompat.getFont(requireContext(), R.font.anta_font);
            tvTituloFiltros.setTypeface(typefaceAnta, Typeface.BOLD);
            tvTituloFiltros.setTextColor(Color.BLACK);
            tvTituloFiltros.setPadding(0, 0, 0, 16);
            layoutFiltros.addView(tvTituloFiltros);

            //Primero se obtiene el ID autogenerado por Firebase
            db.collection("gruposElectrogenos")
                    .whereEqualTo("codigo", codigoGrupo)
                    .get()
                    .addOnSuccessListener(equipoSnapshot -> {

                        if (!equipoSnapshot.isEmpty()) {
                            String idRealEquipo = equipoSnapshot.getDocuments().get(0).getId();

                            // 2. AHORA SÍ BUSCAMOS LOS FILTROS USANDO ESE ID REAL
                            db.collection("filtrosGrupo")
                                    .whereEqualTo("idGrupo", idRealEquipo)
                                    .get()
                                    .addOnSuccessListener(queryDocumentSnapshots -> {

                                        for (String nombreFiltro : filtrosCambiados) {
                                            List<String> opcionesCodigos = new ArrayList<>();
                                            String filtroMarcado = nombreFiltro.toLowerCase().trim();

                                            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                                                String nombreCategoria = doc.getString("nombreCategoria");
                                                if (nombreCategoria == null) continue;

                                                String categoriaDB = nombreCategoria.toLowerCase().trim();
                                                boolean esMatch = false;

                                                // LÓGICA DE SINÓNIMOS E INTELIGENCIA
                                                if (filtroMarcado.contains("aire") && categoriaDB.contains("aire")) {
                                                    esMatch = true;
                                                } else if (filtroMarcado.contains("aceite") && categoriaDB.contains("aceite")) {
                                                    esMatch = true;
                                                } else if ((filtroMarcado.contains("combustible") || filtroMarcado.contains("petróleo") || filtroMarcado.contains("petroleo")) &&
                                                        (categoriaDB.contains("combustible") || categoriaDB.contains("petróleo") || categoriaDB.contains("petroleo"))) {
                                                    esMatch = true;
                                                } else if ((filtroMarcado.contains("separador") || filtroMarcado.contains("agua")) &&
                                                        (categoriaDB.contains("separador") || categoriaDB.contains("agua"))) {
                                                    esMatch = true;
                                                }

                                                if (esMatch) {
                                                    try {
                                                        List<java.util.Map<String, Object>> items = (List<java.util.Map<String, Object>>) doc.get("items");
                                                        if (items != null) {
                                                            for (java.util.Map<String, Object> item : items) {
                                                                String codigo = (String) item.get("codigo");
                                                                String marca = (String) item.get("marca");
                                                                if (codigo != null && !codigo.trim().isEmpty()) {
                                                                    String opcion = codigo;
                                                                    if (marca != null && !marca.trim().isEmpty()) {
                                                                        opcion += " (" + marca + ")";
                                                                    }
                                                                    opcionesCodigos.add(opcion);
                                                                }
                                                            }
                                                        }
                                                    } catch (Exception e) {
                                                        android.util.Log.e("FILTROS", "Error extrayendo array", e);
                                                    }
                                                }
                                            }

                                            opcionesCodigos.add("");

                                            TextInputLayout textInputLayout =
                                                    (TextInputLayout) LayoutInflater.from(requireContext())
                                                            .inflate(R.layout.item_input_filtro, layoutFiltros, false);
                                            textInputLayout.setHint("Código para " + nombreFiltro);
                                            textInputLayout.setTag(nombreFiltro);

                                            // 3. Enlazar el AutoCompleteTextView que está adentro del molde
                                            AutoCompleteTextView autoComplete = textInputLayout.findViewById(R.id.autoCompleteFiltro);
                                            autoComplete.setDropDownBackgroundDrawable(new ColorDrawable(Color.WHITE));
                                            autoComplete.setTextColor(Color.BLACK);
                                            autoComplete.setTag(nombreFiltro);

                                            // 4. Ponerle la lista de opciones (Donaldson, Perkins, etc.)
                                            ArrayAdapter<String> adapterCodigos = new ArrayAdapter<>(requireContext(), R.layout.spinner_item_black, opcionesCodigos);
                                            autoComplete.setAdapter(adapterCodigos);

                                            // Restaurar selección previa si existe
                                            String codigoPrevio = obtenerCodigoParaFiltro(codigosGuardadosHolder, nombreFiltro);
                                            if (codigoPrevio != null && !codigoPrevio.isEmpty()) {
                                                autoComplete.setText(codigoPrevio, false);
                                            }

                                            // Truco para que despliegue al tocarlo
                                            autoComplete.setOnClickListener(v -> autoComplete.showDropDown());
                                            autoComplete.setOnFocusChangeListener((v, hasFocus) -> {
                                                if (hasFocus) autoComplete.showDropDown();
                                            });

                                            // 5. Agregarlo a la pantalla
                                            layoutFiltros.addView(textInputLayout);
                                        }
                                    });
                        } else {
                            // Fallback: Si no se encontró el equipo, mostrar los inputs para que el usuario pueda escribir o ver lo guardado
                            android.util.Log.e("FILTROS", "No se encontró el equipo con código: " + codigoGrupo);
                            for (String nombreFiltro : filtrosCambiados) {
                                TextInputLayout textInputLayout =
                                        (TextInputLayout) LayoutInflater.from(requireContext())
                                                .inflate(R.layout.item_input_filtro, layoutFiltros, false);
                                textInputLayout.setHint("Código para " + nombreFiltro);
                                textInputLayout.setTag(nombreFiltro);
                                AutoCompleteTextView autoComplete = textInputLayout.findViewById(R.id.autoCompleteFiltro);
                                autoComplete.setDropDownBackgroundDrawable(new ColorDrawable(Color.WHITE));
                                autoComplete.setTextColor(Color.BLACK);
                                autoComplete.setTag(nombreFiltro);

                                String codigoPrevio = obtenerCodigoParaFiltro(codigosGuardadosHolder, nombreFiltro);
                                if (codigoPrevio != null && !codigoPrevio.isEmpty()) {
                                    autoComplete.setText(codigoPrevio, false);
                                }
                                layoutFiltros.addView(textInputLayout);
                            }
                        }
                    });
        }

        ExtendedFloatingActionButton btnGenerarPdfDialog = dialog.findViewById(R.id.btnGenerarPdfDialog);

        btnGenerarPdfDialog.setOnClickListener(item -> {
            // 1. RECOLECTAR TODO EN UN MAPA
            Map<String, Object> mapDatos = new HashMap<>();

            String estado = "Operativa";
            if (rgEstado.getCheckedRadioButtonId() == R.id.rbInoperativa) estado = "Inoperativa";
            else if (rgEstado.getCheckedRadioButtonId() == R.id.rbReparacionTerceros)
                estado = "En reparación por terceros";

            String ubicacion = "Taller del cliente";
            if (rgUbicacion.getCheckedRadioButtonId() == R.id.rbCampo) ubicacion = "Campo";

            String defServicio = "Mantenimiento";
            if (rgDefServicio.getCheckedRadioButtonId() == R.id.rbServEvaluacion)
                defServicio = "Evaluación";
            else if (rgDefServicio.getCheckedRadioButtonId() == R.id.rbServEntrega)
                defServicio = "Entrega";
            else if (rgDefServicio.getCheckedRadioButtonId() == R.id.rbServAjuste)
                defServicio = "Realizar Ajuste";

            mapDatos.put("estado", estado);
            mapDatos.put("ubicacion", ubicacion);
            mapDatos.put("defServicio", defServicio);
            mapDatos.put("cliente", inputCliente.getText().toString().trim());
            mapDatos.put("lugar", inputLugar.getText().toString().trim());
            mapDatos.put("aceite", spinnerAceite.getText().toString().trim());
            mapDatos.put("cantAceite", spinnerCantidadAceite.getText().toString().trim());
            mapDatos.put("contacto", inputContacto.getText().toString().trim());
            mapDatos.put("fallas", inputFallas.getText().toString().trim());
            mapDatos.put("trabajos", inputTrabajos.getText().toString().trim());
            mapDatos.put("tecnico", spinnerTecnico.getText().toString());
            mapDatos.put("supervisor", spinnerSupervisor.getText().toString());
            mapDatos.put("proxFecha", inputProxFecha.getText().toString().trim());

            mapDatos.put("chkBandeja", chkBandeja.isChecked());
            mapDatos.put("chkExtintor", chkExtintor.isChecked());
            mapDatos.put("chkKit", chkKit.isChecked());
            mapDatos.put("chkTierra", chkTierra.isChecked());

            String urlFirmaTecnico = "";
            String urlFirmaSupervisor = "";
            for (Usuario u : listaUsuarios) {
                if (u.getNombre().equals(spinnerTecnico.getText().toString()))
                    urlFirmaTecnico = u.getFirmaUrl();
                if (u.getNombre().equals(spinnerSupervisor.getText().toString()))
                    urlFirmaSupervisor = u.getFirmaUrl();
            }
            mapDatos.put("urlFirmaTecnico", urlFirmaTecnico);
            mapDatos.put("urlFirmaSupervisor", urlFirmaSupervisor);

            Map<String, String> codigosIngresados = new HashMap<>();
            for (int i = 0; i < layoutFiltros.getChildCount(); i++) {
                View child = layoutFiltros.getChildAt(i);
                if (child instanceof TextInputLayout) {
                    TextInputLayout til = (TextInputLayout) child;
                    AutoCompleteTextView actv = (AutoCompleteTextView) til.getEditText();
                    if (actv != null && actv.getText() != null && !actv.getText().toString().trim().isEmpty()) {
                        String nombreFiltro = til.getTag() instanceof String
                                ? (String) til.getTag()
                                : (til.getHint() != null ? til.getHint().toString().replace("Código para ", "").trim() : "");
                        codigosIngresados.put(nombreFiltro, actv.getText().toString().trim());
                    }
                }
            }
            mapDatos.put("codigosFiltros", codigosIngresados);
            if (mantenimientoActual != null) {
                mantenimientoActual.setCodigosFiltrosUsados(codigosIngresados);
            }
            codigosGuardadosHolder.putAll(codigosIngresados);

            List<Map<String, Object>> listaRutinasFinal = new ArrayList<>();
            for (int i = 0; i < layoutListaRutinas.getChildCount(); i++) {
                View fila = layoutListaRutinas.getChildAt(i);
                if (fila instanceof LinearLayout) {
                    android.widget.CheckBox chk = (android.widget.CheckBox) ((LinearLayout) fila).getChildAt(0);
                    TextInputEditText txt = (TextInputEditText) ((LinearLayout) fila).getChildAt(1);

                    Map<String, Object> rutinaObj = new HashMap<>();
                    rutinaObj.put("activa", chk.isChecked());
                    rutinaObj.put("nombre", txt.getText().toString().trim());
                    listaRutinasFinal.add(rutinaObj);
                }
            }
            mapDatos.put("rutinasList", listaRutinasFinal);

            // 2. GUARDAR EN FIREBASE (Persistencia)
            Map<String, Object> updates = new HashMap<>();
            updates.put("datosInforme", mapDatos);
            updates.put("codigosFiltrosUsados", codigosIngresados);
            db.collection("mantenimientos").document(mantenimientoId).update(updates);

            Context safeCtx = getContext();
            if (safeCtx != null && mantenimientoActual != null &&
                    com.example.maquirentapp.Utils.MantenimientoOfflineManager.obtenerPendientePorId(safeCtx, mantenimientoId) != null) {
                com.example.maquirentapp.Utils.MantenimientoOfflineManager.guardarMantenimientoOffline(safeCtx, mantenimientoActual);
            }

            // 3. GENERAR PDF (Cargador Visual)
            dialog.dismiss();
            mostrarLoaderYGenerar(mapDatos);
        });

        dialog.show();
    }

    private void cargarRutinasPorDefecto(LinearLayout layout) {
        String[] rutinasBase = {"Filtros de aire", "Limpieza exterior del radiador", "Cambio de aceite de motor", "Cambio de filtro(s) petróleo", "Cambio de filtro de aceite de motor",
                "Tensión de la correa del ventilador", "Revisar las RPM del motor en vacío", "Mantenimiento al sistema de enfriamiento del motor"};
        for (String r : rutinasBase) {
            agregarFilaRutinaVisual(layout, r, true);
        }
    }

    private String obtenerCodigoParaFiltro(Map<String, String> codigosGuardados, String nombreFiltro) {
        if (codigosGuardados == null || codigosGuardados.isEmpty() || nombreFiltro == null) return null;

        // 1. Coincidencia exacta
        if (codigosGuardados.containsKey(nombreFiltro)) {
            String val = codigosGuardados.get(nombreFiltro);
            if (val != null && !val.trim().isEmpty()) return val.trim();
        }

        // 2. Coincidencia insensible a mayúsculas
        for (Map.Entry<String, String> entry : codigosGuardados.entrySet()) {
            if (entry.getKey() != null && entry.getKey().trim().equalsIgnoreCase(nombreFiltro.trim())) {
                if (entry.getValue() != null && !entry.getValue().trim().isEmpty()) {
                    return entry.getValue().trim();
                }
            }
        }

        // 3. Coincidencia por palabra clave del tipo de filtro (aire, aceite, combustible/petróleo, separador/agua)
        String normFiltro = nombreFiltro.toLowerCase().trim();
        for (Map.Entry<String, String> entry : codigosGuardados.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null || entry.getValue().trim().isEmpty()) continue;
            String keyNorm = entry.getKey().toLowerCase().trim();
            boolean match = false;
            if (normFiltro.contains("aire") && keyNorm.contains("aire")) match = true;
            else if (normFiltro.contains("aceite") && keyNorm.contains("aceite")) match = true;
            else if ((normFiltro.contains("combustible") || normFiltro.contains("petróleo") || normFiltro.contains("petroleo"))
                    && (keyNorm.contains("combustible") || keyNorm.contains("petróleo") || keyNorm.contains("petroleo"))) match = true;
            else if ((normFiltro.contains("separador") || normFiltro.contains("agua"))
                    && (keyNorm.contains("separador") || keyNorm.contains("agua"))) match = true;

            if (match) {
                return entry.getValue().trim();
            }
        }

        return null;
    }

    private void restaurarCodigosEnLayoutFiltros(LinearLayout layoutFiltros, Map<String, String> codigosGuardados) {
        if (layoutFiltros == null || codigosGuardados == null || codigosGuardados.isEmpty()) return;
        for (int i = 0; i < layoutFiltros.getChildCount(); i++) {
            View child = layoutFiltros.getChildAt(i);
            if (child instanceof TextInputLayout) {
                TextInputLayout til = (TextInputLayout) child;
                AutoCompleteTextView actv = (AutoCompleteTextView) til.getEditText();
                if (actv != null) {
                    String nombreFiltro = til.getTag() instanceof String
                            ? (String) til.getTag()
                            : (til.getHint() != null ? til.getHint().toString().replace("Código para ", "").trim() : "");
                    String guardado = obtenerCodigoParaFiltro(codigosGuardados, nombreFiltro);
                    if (guardado != null && !guardado.isEmpty()) {
                        actv.setText(guardado, false);
                    }
                }
            }
        }
    }

    // EL NUEVO MÉT0DO DE CARGA VISUAL Y LLAMADA A VECTORIAL
    private void mostrarLoaderYGenerar(Map<String, Object> mapDatos) {
        // 1. Creamos la vista del cargador ("Loader") directamente por código
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setPadding(60, 60, 60, 60);
        layout.setGravity(android.view.Gravity.CENTER_VERTICAL);

        ProgressBar loaderBar = new ProgressBar(requireContext());
        loaderBar.setIndeterminateTintList(ColorStateList.valueOf(Color.WHITE));
        layout.addView(loaderBar);

        TextView tvTexto = new TextView(requireContext());
        tvTexto.setText("Generando documento PDF");
        tvTexto.setTextColor(Color.WHITE);
        tvTexto.setTextSize(16f);
        tvTexto.setPadding(40, 0, 0, 0);

        // Le aplicamos tu fuente Anta
        try {
            Typeface typefaceAnta = ResourcesCompat.getFont(requireContext(), R.font.anta_font);
            tvTexto.setTypeface(typefaceAnta);
        } catch (Exception ignored) {
        }

        layout.addView(tvTexto);

        // 2. Construimos y mostramos el diálogo bloqueante
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext(), R.style.DialogoConFuenteAnta);
        builder.setView(layout);
        builder.setCancelable(false); // Para que el usuario no lo cierre por accidente
        dialogCargaPdf = builder.create();
        dialogCargaPdf.show();

        // 3. Ejecutamos la generación del PDF en segundo plano
        new Thread(() -> {
            try {
                Context context = getContext();
                Activity activity = getActivity();
                if (context == null || activity == null) {
                    if (dialogCargaPdf != null && dialogCargaPdf.isShowing()) {
                        dialogCargaPdf.dismiss();
                    }
                    return;
                }

                String nombreArchivo = "InformeMantenimiento_" + codigoGrupo.replace(" ", "_") + ".pdf";
                File pdfDir = new File(context.getCacheDir(), "InformesMantenimientosTemporales");
                if (!pdfDir.exists()) pdfDir.mkdirs();
                File archivoFinal = new File(pdfDir, nombreArchivo);

                // Llamamos a la clase generadora pasándole el Mapa limpio a través de la estrategia
                com.example.maquirentapp.strategy.StrategyProvider.getPdfStrategy().generarInformeMantenimientoPdf(
                        context, archivoFinal, mantenimientoActual, infoPlacaActual, codigoGrupo, mapDatos
                );

                // Volvemos a la pantalla principal para abrir el visor
                activity.runOnUiThread(() -> {
                    if (dialogCargaPdf != null && dialogCargaPdf.isShowing()) {
                        dialogCargaPdf.dismiss();
                    }
                    if (isAdded() && getContext() != null) {
                        Intent intent = new Intent(getContext(), PdfViewerActivity.class);
                        intent.putExtra("PDF_URL", archivoFinal.getAbsolutePath());
                        intent.putExtra("NOMBRE_ARCHIVO", nombreArchivo);
                        startActivity(intent);
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "Error PDF", e);
                Activity activity = getActivity();
                if (activity != null) {
                    activity.runOnUiThread(() -> {
                        if (dialogCargaPdf != null && dialogCargaPdf.isShowing()) {
                            dialogCargaPdf.dismiss();
                        }
                        if (isAdded() && getContext() != null) {
                            Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        }).start();
    }

    // Mét0do para crear visualmente una fila de rutina editable en el diálogo
    private void agregarFilaRutinaVisual(LinearLayout layoutPadre, String nombreRutina, boolean estaActiva) {
        LinearLayout fila = new LinearLayout(requireContext());
        fila.setOrientation(LinearLayout.HORIZONTAL);
        fila.setWeightSum(10);
        fila.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        fila.setPadding(0, 8, 0, 8);

        // Checkbox para Habilitar/Deshabilitar
        android.widget.CheckBox chkActiva = new android.widget.CheckBox(requireContext());
        chkActiva.setChecked(estaActiva);
        chkActiva.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.5f));
        chkActiva.setButtonDrawable(R.drawable.checbox_custom_informe_mantenimiento);
        fila.addView(chkActiva);

        // EditText para el nombre (Editable)
        TextInputEditText inputNombre = new TextInputEditText(requireContext());
        inputNombre.setText(nombreRutina);
        inputNombre.setTextColor(Color.BLACK);
        inputNombre.setTextSize(14f);
        inputNombre.setBackground(null);
        try {
            Typeface typefaceAnta = ResourcesCompat.getFont(requireContext(), R.font.anta_font);
            inputNombre.setTypeface(typefaceAnta);
        } catch (Exception ignored) {
        }
        LinearLayout.LayoutParams paramsText = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 7.5f);
        inputNombre.setLayoutParams(paramsText);
        fila.addView(inputNombre);

        // Botón Eliminar
        ImageView btnEliminar = new ImageView(requireContext());
        btnEliminar.setImageResource(R.drawable.icon_eliminar_rojo);
        btnEliminar.setPadding(0, 0, 0, 0);
        btnEliminar.setLayoutParams(new LinearLayout.LayoutParams(0, 50, 1f));
        btnEliminar.setOnClickListener(v -> layoutPadre.removeView(fila));
        fila.addView(btnEliminar);

        layoutPadre.addView(fila);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getView() != null) {
            getView().post(() -> {
                if (isAdded() && getView() != null) {
                    configureGlobalFab();
                }
            });
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ocultarDialogoCargando();
        hideGlobalFab();
    }

    private void configureGlobalFab() {
        if (getActivity() instanceof com.example.maquirentapp.MainActivity) {
            com.example.maquirentapp.MainActivity main = (com.example.maquirentapp.MainActivity) getActivity();

            if (modoLectura) {
                main.showGlobalFab("Editar", R.drawable.icon_editar_blanco, v -> {
                    modoLectura = false;
                    aplicarModoEdicion();
                    mostrarFotos();
                    configureGlobalFab();
                });
            } else {
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
