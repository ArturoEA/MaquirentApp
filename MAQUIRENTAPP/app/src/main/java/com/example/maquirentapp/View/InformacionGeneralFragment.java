package com.example.maquirentapp.View;

import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.CircularProgressDrawable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.example.maquirentapp.Model.FiltroCategoria;
import com.example.maquirentapp.Model.FiltroItem;
import com.example.maquirentapp.Model.InfoPlaca;
import com.example.maquirentapp.Network.FirebaseServicio;
import com.example.maquirentapp.R;
import com.example.maquirentapp.Access.FiltroCategoriasAdapter;
import com.example.maquirentapp.Access.FotosSimpleAdapter;
import com.example.maquirentapp.Access.SpecsPlacaAdapter;
import com.example.maquirentapp.Utils.ImageUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InformacionGeneralFragment extends Fragment {

    private String idGrupo;
    private String codigoGrupo;
    private FirebaseServicio firebaseServicio;

    // Vistas Filtros
    private RecyclerView recyclerFiltros;
    private FiltroCategoriasAdapter filtrosAdapter;
    private List<FiltroCategoria> listaFiltros = new ArrayList<>();
    private ImageView btnAddCategoriaFiltro;

    // Vistas Placa
    private RecyclerView recyclerFotosPlaca, recyclerSpecsPlaca;
    private ImageView btnAddSpecPlaca;

    private InfoPlaca infoPlacaActual;
    private FotosSimpleAdapter fotosAdapter;
    private SpecsPlacaAdapter specsAdapter;
    private TextInputEditText inputPotenciaSB, inputPotenciaC, inputMarcaG, inputModeloG, inputSerieG;
    private TextInputEditText inputMarcaM, inputModeloM, inputSerieM;
    private TextInputEditText inputMarcaGen, inputModeloGen, inputSerieGen;
    private Button btnGuardarTecnicos;

    private final ActivityResultLauncher<String> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) subirFotoPlaca(uri);
            }
    );

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        firebaseServicio = new FirebaseServicio();
        if (getArguments() != null) {
            idGrupo = getArguments().getString("idGrupo");
            codigoGrupo = getArguments().getString("codigo");
            if (codigoGrupo == null) codigoGrupo = "General";
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_informacion_general, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerFiltros = view.findViewById(R.id.recyclerFiltros);
        btnAddCategoriaFiltro = view.findViewById(R.id.btnAddCategoriaFiltro);

        recyclerFotosPlaca = view.findViewById(R.id.recyclerFotosPlaca);
        recyclerSpecsPlaca = view.findViewById(R.id.recyclerSpecsPlaca);
        btnAddSpecPlaca = view.findViewById(R.id.btnAddSpecPlaca);

        inputPotenciaSB = view.findViewById(R.id.inputPotenciaStandBy);
        inputPotenciaC = view.findViewById(R.id.inputPotenciaContinua);
        inputMarcaG = view.findViewById(R.id.inputMarcaGrupo);
        inputModeloG = view.findViewById(R.id.inputModeloGrupo);
        inputSerieG = view.findViewById(R.id.inputSerieGrupo);

        inputMarcaM = view.findViewById(R.id.inputMarcaMotor);
        inputModeloM = view.findViewById(R.id.inputModeloMotor);
        inputSerieM = view.findViewById(R.id.inputSerieMotor);

        inputMarcaGen = view.findViewById(R.id.inputMarcaGen);
        inputModeloGen = view.findViewById(R.id.inputModeloGen);
        inputSerieGen = view.findViewById(R.id.inputSerieGen);

        btnGuardarTecnicos = view.findViewById(R.id.btnGuardarDatosTecnicos);

        btnGuardarTecnicos.setOnClickListener(v -> guardarDatosTecnicos());

        LinearLayout header = view.findViewById(R.id.llDatosTecnicosHeader);
        LinearLayout body = view.findViewById(R.id.llDatosTecnicosBody);
        ImageView chevron = view.findViewById(R.id.ivChevronDatosTecnicos);
        body.setVisibility(View.GONE);
        chevron.setRotation(0);

        header.setOnClickListener(v -> {
            if (body.getVisibility() == View.VISIBLE) {
                body.setVisibility(View.GONE);
                chevron.animate().rotation(0).setDuration(200).start();
            } else {
                body.setVisibility(View.VISIBLE);
                chevron.animate().rotation(180).setDuration(200).start();
            }
        });

        setupFiltros();
        setupPlaca();
    }

    private void setupFiltros() {
        recyclerFiltros.setLayoutManager(new LinearLayoutManager(getContext()));
        filtrosAdapter = new FiltroCategoriasAdapter(listaFiltros, new FiltroCategoriasAdapter.OnCategoriaActionListener() {
            @Override
            public void onAgregarItem(FiltroCategoria categoria) {
                mostrarDialogoAgregarItemFiltro(categoria, null, -1);
            }

            @Override
            public void onEliminarItem(FiltroCategoria categoria, FiltroItem item) {
                new MaterialAlertDialogBuilder(getContext(), R.style.DialogoConFuenteAnta)
                        .setTitle("Eliminar filtro")
                        .setMessage("¿Estás seguro de eliminar este item?")
                        .setPositiveButton("Eliminar", (d, w) -> {
                            categoria.getItems().remove(item);
                            firebaseServicio.actualizarCategoriaFiltro(categoria, new FirebaseServicio.OnSimpleCallback() {
                                @Override
                                public void onSuccess() {
                                    filtrosAdapter.notifyDataSetChanged();
                                }

                                @Override
                                public void onError(Exception e) {
                                    Toast.makeText(getContext(), "Error", Toast.LENGTH_SHORT).show();
                                }
                            });
                        })
                        .setNegativeButton("Cancelar", null)
                        .show();
            }

            @Override
            public void onEliminarCategoria(FiltroCategoria categoria) {
                new MaterialAlertDialogBuilder(getContext(), R.style.DialogoConFuenteAnta)
                        .setTitle("Eliminar " + categoria.getNombreCategoria())
                        .setMessage("¿Borrar esta categoría y sus items?")
                        .setPositiveButton("Sí", (d, w) -> {
                            firebaseServicio.eliminarCategoriaFiltro(categoria.getId(), new FirebaseServicio.OnSimpleCallback() {
                                @Override
                                public void onSuccess() {
                                    cargarFiltros();
                                }

                                @Override
                                public void onError(Exception e) {
                                }
                            });
                        }).show();
            }

            @Override
            public void onEditarItem(FiltroCategoria categoria, FiltroItem item, int position) {
                mostrarDialogoAgregarItemFiltro(categoria, item, position);
            }
        });
        recyclerFiltros.setAdapter(filtrosAdapter);
        btnAddCategoriaFiltro.setOnClickListener(v ->

                mostrarDialogoNuevaCategoria());

        cargarFiltros();
    }

    private void cargarFiltros() {
        firebaseServicio.getFiltrosPorGrupo(idGrupo, new FirebaseServicio.OnFiltrosLoadedListener() {
            @Override
            public void onSuccess(List<FiltroCategoria> categorias) {
                listaFiltros.clear();
                listaFiltros.addAll(categorias);
                filtrosAdapter.notifyDataSetChanged();
            }

            @Override
            public void onError(Exception e) {
            }
        });
    }

    private void mostrarDialogoNuevaCategoria() {
        EditText input = new EditText(getContext());
        new MaterialAlertDialogBuilder(getContext(), R.style.DialogoConFuenteAnta)
                .setTitle("Nueva categoría de filtro")
                .setView(input)
                .setPositiveButton("Crear", (d, w) -> {
                    String nombre = input.getText().toString().trim();
                    if (!nombre.isEmpty()) {
                        FiltroCategoria cat = new FiltroCategoria(null, idGrupo, nombre);
                        firebaseServicio.crearCategoriaFiltro(cat, new FirebaseServicio.OnSimpleCallback() {
                            @Override
                            public void onSuccess() {
                                cargarFiltros();
                            }

                            @Override
                            public void onError(Exception e) {
                            }
                        });
                    }
                }).show();
    }

    private void mostrarDialogoAgregarItemFiltro(FiltroCategoria categoria, FiltroItem itemExistente, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_agregar_filtro_item, null);
        builder.setView(view);
        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null)
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        TextInputEditText inputMarca = view.findViewById(R.id.inputMarca);
        TextInputEditText inputCodigo = view.findViewById(R.id.inputCodigo);
        Button btnGuardar = view.findViewById(R.id.btnGuardarItem);

        if (itemExistente != null) {
            inputMarca.setText(itemExistente.getMarca());
            inputCodigo.setText(itemExistente.getCodigo());
            btnGuardar.setText("Actualizar");
        }

        btnGuardar.setOnClickListener(v -> {
            String marca = inputMarca.getText().toString().trim();
            String codigo = inputCodigo.getText().toString().trim();
            if (!marca.isEmpty() && !codigo.isEmpty()) {

                if (itemExistente != null) {
                    itemExistente.setMarca(marca);
                    itemExistente.setCodigo(codigo);
                } else {
                    categoria.getItems().add(new FiltroItem(marca, codigo));
                }

                firebaseServicio.actualizarCategoriaFiltro(categoria, new FirebaseServicio.OnSimpleCallback() {
                    @Override
                    public void onSuccess() {
                        filtrosAdapter.notifyDataSetChanged();
                        dialog.dismiss();
                    }

                    @Override
                    public void onError(Exception e) {
                        Toast.makeText(getContext(), "Error", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
        dialog.show();
    }

    private void setupPlaca() {
        recyclerFotosPlaca.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        recyclerSpecsPlaca.setLayoutManager(new LinearLayoutManager(getContext()));

        btnAddSpecPlaca.setOnClickListener(v -> mostrarDialogoSpec(null, -1));

        cargarInfoPlaca();
    }

    private void cargarInfoPlaca() {
        firebaseServicio.getInfoPlaca(idGrupo, new FirebaseServicio.OnInfoPlacaLoadedListener() {
            @Override
            public void onSuccess(InfoPlaca info) {
                if (info == null) {
                    infoPlacaActual = new InfoPlaca();
                    infoPlacaActual.setIdGrupo(idGrupo);
                } else {
                    infoPlacaActual = info;
                }
                actualizarUIPlaca();
            }

            @Override
            public void onError(Exception e) {
            }
        });
    }

    private void actualizarUIPlaca() {
        if (infoPlacaActual == null) return;
        inputPotenciaSB.setText(infoPlacaActual.getPotenciaStandBy());
        inputPotenciaC.setText(infoPlacaActual.getPotenciaContinua());

        inputMarcaG.setText(infoPlacaActual.getMarcaGrupo());
        inputModeloG.setText(infoPlacaActual.getModeloGrupo());
        inputSerieG.setText(infoPlacaActual.getSerieGrupo());

        inputMarcaM.setText(infoPlacaActual.getMarcaMotor());
        inputModeloM.setText(infoPlacaActual.getModeloMotor());
        inputSerieM.setText(infoPlacaActual.getSerieMotor());

        inputMarcaGen.setText(infoPlacaActual.getMarcaGenerador());
        inputModeloGen.setText(infoPlacaActual.getModeloGenerador());
        inputSerieGen.setText(infoPlacaActual.getSerieGenerador());

        if (fotosAdapter == null) {
            fotosAdapter = new FotosSimpleAdapter(infoPlacaActual.getImagenesUrls(), new FotosSimpleAdapter.OnFotoActionListener() {
                @Override
                public void onVerFoto(String url) {
                    mostrarFotoGrande(url);
                }

                @Override
                public void onEliminarFoto(String url) {
                    new MaterialAlertDialogBuilder(getContext(), R.style.DialogoConFuenteAnta)
                            .setTitle("Eliminar foto")
                            .setMessage("¿Estás seguro?")
                            .setPositiveButton("Sí", (d, w) -> {
                                firebaseServicio.eliminarArchivoStorage(url, new FirebaseServicio.OnSimpleCallback() {
                                    @Override
                                    public void onSuccess() {
                                        infoPlacaActual.getImagenesUrls().remove(url);
                                        fotosAdapter.notifyDataSetChanged();
                                        guardarPlacaSinRecargar();
                                        Toast.makeText(getContext(), "Foto eliminada", Toast.LENGTH_SHORT).show();
                                    }

                                    @Override
                                    public void onError(Exception e) {
                                        infoPlacaActual.getImagenesUrls().remove(url);
                                        fotosAdapter.notifyDataSetChanged();
                                        guardarPlacaSinRecargar();
                                    }
                                });
                            }).setNegativeButton("No", null).show();
                }

                @Override
                public void onAgregarFoto() {
                    galleryLauncher.launch("image/*");
                }
            });
            recyclerFotosPlaca.setAdapter(fotosAdapter);
        } else {
            fotosAdapter.notifyDataSetChanged();
        }
        if (specsAdapter == null) {
            specsAdapter = new SpecsPlacaAdapter(infoPlacaActual.getEspecificaciones(), new SpecsPlacaAdapter.OnSpecActionListener() {
                @Override
                public void onEliminarSpec(Map<String, String> spec) {
                    new MaterialAlertDialogBuilder(getContext(), R.style.DialogoConFuenteAnta)
                            .setTitle("Eliminar dato")
                            .setMessage("¿Eliminar '" + spec.get("clave") + "'?")
                            .setPositiveButton("Sí", (d, w) -> {
                                infoPlacaActual.getEspecificaciones().remove(spec);
                                specsAdapter.notifyDataSetChanged();
                                guardarPlacaSinRecargar();
                            }).show();
                }

                @Override
                public void onEditarSpec(Map<String, String> spec, int position) {
                    mostrarDialogoSpec(spec, position);
                }
            });
            recyclerSpecsPlaca.setAdapter(specsAdapter);
        } else {
            specsAdapter.notifyDataSetChanged();
        }
    }
    private void subirFotoPlaca(Uri uri) {
        Toast.makeText(getContext(), "Procesando imagen...", Toast.LENGTH_SHORT).show();
        Context safeContext = getContext();
        if (safeContext == null) return;

        new Thread(() -> {
            byte[] dataImagen = ImageUtils.comprimirImagen(safeContext, uri);

            if (getActivity() != null){
                requireActivity().runOnUiThread(() -> {
                    if (dataImagen != null) {
                        firebaseServicio.subirFotoPlacaBytes(codigoGrupo, dataImagen, new FirebaseServicio.OnUrlUploadedListener() {
                            @Override
                            public void onSuccess(String url) {
                                infoPlacaActual.getImagenesUrls().add(url);
                                if (fotosAdapter != null) fotosAdapter.notifyDataSetChanged();

                                guardarPlacaSinRecargar();
                                Toast.makeText(getContext(), "Foto de placa subida", Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onError(Exception e) {
                                Toast.makeText(getContext(), "Error al subir", Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        Toast.makeText(getContext(), "Error al comprimir imagen", Toast.LENGTH_SHORT).show();
                    }
                });
            }

        }).start();
    }
    private void mostrarDialogoSpec(Map<String, String> specExistente, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_agregar_spec, null);
        builder.setView(view);
        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null)
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        TextInputEditText inputClave = view.findViewById(R.id.inputClave);
        TextInputEditText inputValor = view.findViewById(R.id.inputValor);
        Button btnGuardar = view.findViewById(R.id.btnGuardarSpec);

        if (specExistente != null) {
            inputClave.setText(specExistente.get("clave"));
            inputValor.setText(specExistente.get("valor"));
            btnGuardar.setText("Actualizar");
        }

        btnGuardar.setOnClickListener(v -> {
            String clave = inputClave.getText().toString().trim();
            String valor = inputValor.getText().toString().trim();

            if (clave.isEmpty() || valor.isEmpty()) {
                Toast.makeText(getContext(), "Completa ambos campos", Toast.LENGTH_SHORT).show();
                return;
            }

            if (infoPlacaActual == null) {
                infoPlacaActual = new InfoPlaca();
                infoPlacaActual.setIdGrupo(idGrupo);
            }

            if (specExistente != null) {
                specExistente.put("clave", clave);
                specExistente.put("valor", valor);
            } else {
                Map<String, String> nuevaSpec = new HashMap<>();
                nuevaSpec.put("clave", clave);
                nuevaSpec.put("valor", valor);
                infoPlacaActual.getEspecificaciones().add(nuevaSpec);
            }

            if (specsAdapter == null) {
                actualizarUIPlaca();
            } else {
                specsAdapter.notifyDataSetChanged();
            }

            guardarPlacaSinRecargar();
            dialog.dismiss();
        });

        dialog.show();
    }

    private void guardarPlacaSinRecargar() {
        firebaseServicio.guardarInfoPlaca(infoPlacaActual, new FirebaseServicio.OnSimpleCallback() {
            @Override
            public void onSuccess() {
            }

            @Override
            public void onError(Exception e) {
            }
        });
    }

    private void mostrarFotoGrande(String url) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), android.R.style.Theme_Translucent_NoTitleBar);
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_foto_grande, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#CC000000")));

        dialog.getWindow().getAttributes().windowAnimations = R.style.DialogFadeAnimation;

        ImageView ivFoto = dialogView.findViewById(R.id.ivFotoGrande);
        Button btnCerrar = dialogView.findViewById(R.id.btnCerrar);
        Button btnDescargar = dialogView.findViewById(R.id.btnDescargar);
        Button btnCompartir = dialogView.findViewById(R.id.btnCompartir);

        CircularProgressDrawable progressDrawable = new CircularProgressDrawable(requireContext());
        progressDrawable.setStrokeWidth(10f);
        progressDrawable.setCenterRadius(50f);
        progressDrawable.setColorSchemeColors(Color.WHITE);
        progressDrawable.start();

        Glide.with(this)
                .load(url)
                .placeholder(progressDrawable)
                .error(R.drawable.ilustracion_maquinaria_vacio)
                .into(ivFoto);

        btnCerrar.setOnClickListener(v -> dialog.dismiss());
        ivFoto.setOnClickListener(v -> dialog.dismiss());

        btnDescargar.setOnClickListener(v -> {
            descargarImagen(url, "placa_" + System.currentTimeMillis() + ".jpg");
            Toast.makeText(getContext(), "Descargando...", Toast.LENGTH_SHORT).show();
        });

        btnCompartir.setOnClickListener(v -> compartirImagen(url));

        dialog.show();
    }

    private void descargarImagen(String url, String nombreArchivo) {
        try {
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE);
            request.setTitle("Foto Placa");
            request.setDescription("Descargando foto...");
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, nombreArchivo);

            DownloadManager manager = (DownloadManager) requireContext().getSystemService(Context.DOWNLOAD_SERVICE);
            if (manager != null) {
                manager.enqueue(request);
            }
        } catch (Exception e) {
            Toast.makeText(getContext(), "Error al descargar", Toast.LENGTH_SHORT).show();
        }
    }

    private void compartirImagen(String url) {
        Toast.makeText(getContext(), "Preparando para compartir...", Toast.LENGTH_SHORT).show();
        Glide.with(this)
                .asBitmap()
                .load(url)
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        if (isAdded() && getContext() != null) {
                            compartirBitmap(resource, "placa_share.jpg");
                        }
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                    }
                });
    }

    private void compartirBitmap(Bitmap bitmap, String fileName) {
        try {
            File cachePath = new File(requireContext().getCacheDir(), "images");
            if (!cachePath.exists()) cachePath.mkdirs();
            File newFile = new File(cachePath, fileName);
            FileOutputStream stream = new FileOutputStream(newFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
            stream.close();

            Uri contentUri = FileProvider.getUriForFile(requireContext(),
                    "com.example.maquirentapp.provider", newFile);

            if (contentUri != null) {
                Intent shareIntent = new Intent();
                shareIntent.setAction(Intent.ACTION_SEND);
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                shareIntent.setDataAndType(contentUri, requireContext().getContentResolver().getType(contentUri));
                shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
                startActivity(Intent.createChooser(shareIntent, "Compartir vía"));
            }
        } catch (IOException e) {
            Toast.makeText(getContext(), "Error al compartir", Toast.LENGTH_SHORT).show();
        }
    }
    private void guardarDatosTecnicos() {
        if (infoPlacaActual == null) {
            infoPlacaActual = new InfoPlaca();
            infoPlacaActual.setIdGrupo(idGrupo);
        }

        infoPlacaActual.setPotenciaStandBy(inputPotenciaSB.getText().toString().trim());
        infoPlacaActual.setPotenciaContinua(inputPotenciaC.getText().toString().trim());

        infoPlacaActual.setMarcaGrupo(inputMarcaG.getText().toString().trim());
        infoPlacaActual.setModeloGrupo(inputModeloG.getText().toString().trim());
        infoPlacaActual.setSerieGrupo(inputSerieG.getText().toString().trim());

        infoPlacaActual.setMarcaMotor(inputMarcaM.getText().toString().trim());
        infoPlacaActual.setModeloMotor(inputModeloM.getText().toString().trim());
        infoPlacaActual.setSerieMotor(inputSerieM.getText().toString().trim());

        infoPlacaActual.setMarcaGenerador(inputMarcaGen.getText().toString().trim());
        infoPlacaActual.setModeloGenerador(inputModeloGen.getText().toString().trim());
        infoPlacaActual.setSerieGenerador(inputSerieGen.getText().toString().trim());

        Toast.makeText(getContext(), "Guardando...", Toast.LENGTH_SHORT).show();
        guardarPlacaSinRecargar();
    }
}