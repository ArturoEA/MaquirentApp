package com.example.maquirentapp.View;

import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
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
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.CircularProgressDrawable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.example.maquirentapp.MainActivity;
import com.example.maquirentapp.Model.Plano;
import com.example.maquirentapp.Network.FirebaseServicio;
import com.example.maquirentapp.R;
import com.example.maquirentapp.Utils.ImageUtils;
import com.example.maquirentapp.adaptadores.PlanoAdapter;
import com.github.chrisbanes.photoview.PhotoView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PlanosCambioVoltajeFragment extends Fragment {

    private RecyclerView recyclerPlanos;
    private ProgressBar progressBar;
    private PlanoAdapter adapter;
    private FirebaseServicio firebaseServicio;
    private List<Plano> listaPlanos = new ArrayList<>();

    private final ActivityResultLauncher<String> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    subirImagen(uri);
                }
            }
    );

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        firebaseServicio = new FirebaseServicio();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_planos_cambio_voltaje, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerPlanos = view.findViewById(R.id.recyclerPlanos);
        progressBar = view.findViewById(R.id.progressBar);

        recyclerPlanos.setLayoutManager(new GridLayoutManager(getContext(), 2));
        adapter = new PlanoAdapter(listaPlanos, this::mostrarDialogoPlano);
        recyclerPlanos.setAdapter(adapter);

        configurarFab();
        cargarPlanos();
    }

    private void configurarFab() {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).showGlobalFab(
                    "Añadir",
                    R.drawable.icon_nuevo_blanco,
                    v -> galleryLauncher.launch("image/*")
            );
        }
    }

    private void subirImagen(Uri uri) {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        Toast.makeText(getContext(), "Procesando imagen...", Toast.LENGTH_SHORT).show();
        Context safeContext = getContext();
        if (safeContext == null) return;

        new Thread(() -> {
            byte[] dataComprimida = ImageUtils.comprimirImagen(safeContext, uri);

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (getContext() == null) return;

                    if (dataComprimida != null) {
                        firebaseServicio.subirPlanoBytes(dataComprimida, new FirebaseServicio.OnSimpleCallback() {
                            @Override
                            public void onSuccess() {
                                if (getContext() == null) return;

                                Toast.makeText(getContext(), "Plano subido correctamente", Toast.LENGTH_SHORT).show();
                                cargarPlanos();
                                if (progressBar != null) progressBar.setVisibility(View.GONE);
                            }

                            @Override
                            public void onError(Exception e) {
                                if (getContext() == null) return;

                                Toast.makeText(getContext(), "Error al subir: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                if (progressBar != null) progressBar.setVisibility(View.GONE);
                            }
                        });
                    } else {
                        if (progressBar != null) progressBar.setVisibility(View.GONE);
                        Toast.makeText(getContext(), "Error al procesar la imagen", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }).start();
    }

    private void cargarPlanos() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        firebaseServicio.getPlanosVoltaje(new FirebaseServicio.OnPlanosLoadedListener() {
            @Override
            public void onSuccess(List<Plano> planos) {
                listaPlanos = planos;
                adapter.setItems(planos);
                if (progressBar != null) progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onError(Exception e) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Error al cargar planos", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void mostrarDialogoPlano(Plano plano) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_ver_foto, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        // Usar PhotoView para el Zoom
        PhotoView imgFull = dialogView.findViewById(R.id.imgFull);

        ImageView btnClose = dialogView.findViewById(R.id.btnClose);
        ImageView btnDelete = dialogView.findViewById(R.id.btnDelete);
        LinearLayout btnDownload = dialogView.findViewById(R.id.btnDownload);
        LinearLayout btnShare = dialogView.findViewById(R.id.btnShare);

        CircularProgressDrawable spinner = new CircularProgressDrawable(this.getContext());
        spinner.setStrokeWidth(5f);
        spinner.setCenterRadius(30f);
        spinner.setColorSchemeColors(Color.WHITE);
        spinner.start();

        Glide.with(this)
                .load(plano.getUrlImagen())
                .placeholder(spinner)
                .centerCrop()
                .into(imgFull);

        btnClose.setOnClickListener(v -> dialog.dismiss());

        btnDelete.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(getContext(), R.style.DialogoConFuenteAnta)
                    .setTitle("Eliminar Plano")
                    .setMessage("¿Estás seguro de eliminar esta imagen?")
                    .setPositiveButton("Eliminar", (d, w) -> {
                        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

                        firebaseServicio.eliminarPlano(plano, new FirebaseServicio.OnSimpleCallback() {
                            @Override
                            public void onSuccess() {
                                if (progressBar != null) progressBar.setVisibility(View.GONE);
                                Toast.makeText(getContext(), "Plano eliminado", Toast.LENGTH_SHORT).show();
                                cargarPlanos();
                                dialog.dismiss();
                            }

                            @Override
                            public void onError(Exception e) {
                                if (progressBar != null) progressBar.setVisibility(View.GONE);
                                Toast.makeText(getContext(), "Error al eliminar", Toast.LENGTH_SHORT).show();
                            }
                        });
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
        });

        btnDownload.setOnClickListener(v -> descargarImagen(plano.getUrlImagen(), plano.getNombreArchivo()));
        btnShare.setOnClickListener(v -> compartirImagen(plano));

        dialog.show();
    }

    private void descargarImagen(String url, String nombreArchivo) {
        try {
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE);
            request.setTitle("Descargando Plano");
            request.setDescription("Descargando " + nombreArchivo);
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, nombreArchivo);

            DownloadManager manager = (DownloadManager) requireContext().getSystemService(Context.DOWNLOAD_SERVICE);
            if (manager != null) {
                manager.enqueue(request);
                Toast.makeText(getContext(), "Descarga iniciada...", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(getContext(), "Error al descargar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void compartirImagen(Plano plano) {
        Toast.makeText(getContext(), "Preparando para compartir...", Toast.LENGTH_SHORT).show();

        Glide.with(this)
                .asBitmap()
                .load(plano.getUrlImagen())
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        // Usar requireContext() dentro del callback
                        new Thread(() -> {
                            if (isAdded() && getContext() != null) {
                                compartirBitmap(resource, "plano_compartido_" + System.currentTimeMillis() + ".jpg");
                            }
                        }).start();
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

            // Obtener URI segura usando FileProvider
            Uri contentUri = FileProvider.getUriForFile(requireContext(),
                    "com.example.maquirentapp.provider", newFile);

            if (contentUri != null) {
                Intent shareIntent = new Intent();
                shareIntent.setAction(Intent.ACTION_SEND);
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                shareIntent.setDataAndType(contentUri, requireContext().getContentResolver().getType(contentUri));
                shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
                requireActivity().runOnUiThread(() -> {
                    startActivity(Intent.createChooser(shareIntent, "Compartir plano vía"));
                });
            }
        } catch (IOException | IllegalArgumentException e) {
            e.printStackTrace();
            Log.e("Compartir", "Error: " + e.getMessage());
            Toast.makeText(getContext(), "Error al compartir. Verifique FileProvider.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).hideGlobalFab();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        configurarFab();
    }
}