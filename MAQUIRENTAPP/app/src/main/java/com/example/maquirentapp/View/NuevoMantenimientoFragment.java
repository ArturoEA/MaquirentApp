package com.example.maquirentapp.View;

import android.app.DatePickerDialog;
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
import com.example.maquirentapp.Access.ItemsMantenimientoSeleccionablesAdapter;
import com.example.maquirentapp.Utils.ImageUtils;
import com.example.maquirentapp.Utils.InformePdfVectorialGenerator;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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

    private ItemsMantenimientoSeleccionablesAdapter itemsAdapter;
    private List<MantenimientoConfiguracion> itemsConfigList = new ArrayList<>();
    private List<Uri> fotosUriList = new ArrayList<>();
    private List<String> fotosUrlList = new ArrayList<>();

    // Firebase
    private FirebaseFirestore db;
    private FirebaseStorage storage;

    private ActivityResultLauncher<String> imagePickerLauncher;

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
            modoLectura = mantenimientoId != null;
        }

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
            mostrarFotos();
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

                            db.collection("gruposElectrogenos").whereEqualTo("codigo", codigoGrupo).get()
                                    .addOnSuccessListener(query -> {
                                        if (!query.isEmpty()) {
                                            String idEquipo = query.getDocuments().get(0).getId();
                                            db.collection("infoPlacaGrupo").whereEqualTo("idGrupo", idEquipo).get()
                                                    .addOnSuccessListener(query2 -> {
                                                        if (!query2.isEmpty()) {
                                                            infoPlacaActual = query2.getDocuments().get(0).toObject(com.example.maquirentapp.Model.InfoPlaca.class);
                                                        }
                                                    });
                                        }
                                    });
                        }
                    }
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
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext(), android.R.style.Theme_Translucent_NoTitleBar);
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_foto_grande, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();

        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#CC000000")));

        dialog.getWindow().getAttributes().windowAnimations = R.style.DialogFadeAnimation;

        ImageView ivFoto = dialogView.findViewById(R.id.ivFotoGrande);
        Button btnDescargar = dialogView.findViewById(R.id.btnDescargar);
        Button btnCompartir = dialogView.findViewById(R.id.btnCompartir);
        Button btnCerrar = dialogView.findViewById(R.id.btnCerrar);

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

    private void guardarMantenimiento() {
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

        subirFotosNuevas(fotosSubidas -> {
            List<String> todasLasFotos = new ArrayList<>(fotosUrlList);
            todasLasFotos.addAll(fotosSubidas);

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
        final int[] fotosProcesadas = {0};

        for (Uri uri : fotosUriList) {
            byte[] dataImagen = ImageUtils.comprimirImagen(requireContext(), uri);

            if (dataImagen != null) {
                String fileName = "mantenimientos/" + codigoGrupo + "/" + System.currentTimeMillis() + ".jpg";
                StorageReference storageRef = storage.getReference().child(fileName);

                storageRef.putBytes(dataImagen)
                        .addOnSuccessListener(taskSnapshot ->
                                storageRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                                    urlsSubidas.add(downloadUri.toString());
                                    verificarFinSubida(fotosUriList.size(), fotosProcesadas, urlsSubidas, listener);
                                }))
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Error subiendo foto comprimida", e);
                            verificarFinSubida(fotosUriList.size(), fotosProcesadas, urlsSubidas, listener);
                        });
            } else {
                Log.e(TAG, "Error al comprimir imagen: " + uri.toString());
                verificarFinSubida(fotosUriList.size(), fotosProcesadas, urlsSubidas, listener);
            }
        }
    }

    private void verificarFinSubida(int total, int[] procesadas, List<String> urls, OnFotosSubidasListener listener) {
        procesadas[0]++;
        if (procesadas[0] == total) {
            listener.onFotosSubidas(urls);
        }
    }

    private void crearMantenimiento(Mantenimiento mantenimiento) {
        db.collection("mantenimientos")
                .add(mantenimiento)
                .addOnSuccessListener(documentReference -> {
                    if (getContext() == null) return;
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Mantenimiento guardado", Toast.LENGTH_SHORT).show();
                    requireActivity().getSupportFragmentManager().popBackStack();
                })
                .addOnFailureListener(e -> {
                    if (getContext() == null) return;
                    Log.e(TAG, "Error guardando", e);
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Error al guardar", Toast.LENGTH_SHORT).show();
                });
    }

    private void actualizarMantenimiento(Mantenimiento mantenimiento) {
        db.collection("mantenimientos").document(mantenimientoId)
                .set(mantenimiento)
                .addOnSuccessListener(aVoid -> {
                    if (getContext() == null) return;
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Mantenimiento actualizado", Toast.LENGTH_SHORT).show();

                    eliminarFotosRemovidasDeStorage();

                    requireActivity().getSupportFragmentManager().popBackStack();
                })
                .addOnFailureListener(e -> {
                    if (getContext() == null) return;
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
            new DatePickerDialog(requireContext(), (view, year, month, dayOfMonth) -> {
                calendar.set(year, month, dayOfMonth);
                inputProxFecha.setText(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(calendar.getTime()));
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
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

                                            // 3. Enlazar el AutoCompleteTextView que está adentro del molde
                                            AutoCompleteTextView autoComplete = textInputLayout.findViewById(R.id.autoCompleteFiltro);
                                            autoComplete.setDropDownBackgroundDrawable(new ColorDrawable(Color.WHITE));
                                            autoComplete.setTextColor(Color.BLACK);

                                            // 4. Ponerle la lista de opciones (Donaldson, Perkins, etc.)
                                            ArrayAdapter<String> adapterCodigos = new ArrayAdapter<>(requireContext(), R.layout.spinner_item_black, opcionesCodigos);
                                            autoComplete.setAdapter(adapterCodigos);

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
                            // Opcional: Mostrar un mensaje si no se encuentra el equipo
                            android.util.Log.e("FILTROS", "No se encontró el equipo con código: " + codigoGrupo);
                        }
                    });
        }

        ExtendedFloatingActionButton btnGenerarPdfDialog = dialog.findViewById(R.id.btnGenerarPdfDialog);

        btnGenerarPdfDialog.setOnClickListener(item -> {
            // 1. RECOLECTAR T0DO EN UN MAPA
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
                    if (actv != null && !actv.getText().toString().isEmpty()) {
                        String nombreFiltro = til.getHint().toString().replace("Código para ", "");
                        codigosIngresados.put(nombreFiltro, actv.getText().toString());
                    }
                }
            }
            mapDatos.put("codigosFiltros", codigosIngresados);

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
            db.collection("mantenimientos").document(mantenimientoId).update("datosInforme", mapDatos);

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
                String nombreArchivo = "InformeMantenimiento_" + codigoGrupo.replace(" ", "_") + ".pdf";
                File pdfDir = new File(requireContext().getCacheDir(), "InformesMantenimientosTemporales");
                if (!pdfDir.exists()) pdfDir.mkdirs();
                File archivoFinal = new File(pdfDir, nombreArchivo);

                // Llamamos a la clase generadora pasándole el Mapa limpio
                InformePdfVectorialGenerator.generarPdf(
                        requireContext(), archivoFinal, mantenimientoActual, infoPlacaActual, codigoGrupo, mapDatos
                );

                // Volvemos a la pantalla principal para abrir el visor
                requireActivity().runOnUiThread(() -> {
                    dialogCargaPdf.dismiss();
                    Intent intent = new Intent(requireContext(), PdfViewerActivity.class);
                    intent.putExtra("PDF_URL", archivoFinal.getAbsolutePath());
                    intent.putExtra("NOMBRE_ARCHIVO", nombreArchivo);
                    startActivity(intent);
                });

            } catch (Exception e) {
                Log.e(TAG, "Error PDF", e);
                requireActivity().runOnUiThread(() -> {
                    dialogCargaPdf.dismiss();
                    Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
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