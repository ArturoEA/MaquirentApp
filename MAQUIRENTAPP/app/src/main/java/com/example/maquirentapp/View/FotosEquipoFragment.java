package com.example.maquirentapp.View;

import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
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

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.example.maquirentapp.MainActivity;
import com.example.maquirentapp.Model.FotoEquipo;
import com.example.maquirentapp.Network.FirebaseServicio;
import com.example.maquirentapp.R;
import com.example.maquirentapp.Access.FotosEquipoAdapter;
import com.example.maquirentapp.Utils.ImageUtils;
import com.github.chrisbanes.photoview.PhotoView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FotosEquipoFragment extends Fragment {

    private RecyclerView recyclerFotos;
    private ProgressBar progressBar;
    private FotosEquipoAdapter adapter;
    private FirebaseServicio firebaseServicio;
    private List<FotoEquipo> listaFotos = new ArrayList<>();
    private String idGrupo;

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
        if (getArguments() != null) {
            idGrupo = getArguments().getString("idGrupo");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_fotos_equipo, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerFotos = view.findViewById(R.id.recyclerFotos);
        progressBar = view.findViewById(R.id.progressBar);

        recyclerFotos.setLayoutManager(new GridLayoutManager(getContext(), 2));
        adapter = new FotosEquipoAdapter(listaFotos, this::mostrarDialogoFoto);
        recyclerFotos.setAdapter(adapter);

        configurarFab();
        cargarFotos();
    }

    private void configurarFab() {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).showGlobalFab(
                    "Añadir",
                    R.drawable.icon_nuevo_blanco,
                    v -> {
                        if (idGrupo != null) {
                            galleryLauncher.launch("image/*");
                        } else {
                            Toast.makeText(getContext(), "Error: Grupo no identificado", Toast.LENGTH_SHORT).show();
                        }
                    }
            );
        }
    }
    private void subirImagen(Uri uri) {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        Toast.makeText(getContext(), "Procesando imagen...", Toast.LENGTH_SHORT).show();
        Context safeContext = getContext();
        if (safeContext == null) return;

        new Thread(() -> {
            byte[] dataImagen = ImageUtils.comprimirImagen(safeContext, uri);

            if(getActivity() != null){
                getActivity().runOnUiThread(() -> {
                    if (dataImagen != null) {
                        firebaseServicio.subirFotoEquipoBytes(idGrupo, dataImagen, new FirebaseServicio.OnSimpleCallback() {
                            @Override
                            public void onSuccess() {
                                Toast.makeText(getContext(), "Foto subida correctamente", Toast.LENGTH_SHORT).show();
                                cargarFotos();
                                if (progressBar != null) progressBar.setVisibility(View.GONE);
                            }

                            @Override
                            public void onError(Exception e) {
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

    private void cargarFotos() {
        if (idGrupo == null) return;

        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        firebaseServicio.getFotosEquipo(idGrupo, new FirebaseServicio.OnFotosEquipoLoadedListener() {
            @Override
            public void onSuccess(List<FotoEquipo> fotos) {
                listaFotos = fotos;
                adapter.setItems(fotos);
                if (progressBar != null) progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onError(Exception e) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
            }
        });
    }

    private void mostrarDialogoFoto(FotoEquipo foto) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_ver_foto, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        PhotoView imgFull = dialogView.findViewById(R.id.imgFull);
        ImageView btnClose = dialogView.findViewById(R.id.btnClose);
        ImageView btnDelete = dialogView.findViewById(R.id.btnDelete);
        LinearLayout btnDownload = dialogView.findViewById(R.id.btnDownload);
        LinearLayout btnShare = dialogView.findViewById(R.id.btnShare);

        Glide.with(this)
                .load(foto.getUrlImagen())
                .placeholder(R.drawable.ilustracion_maquinaria_vacio)
                .into(imgFull);

        btnClose.setOnClickListener(v -> dialog.dismiss());

        btnDelete.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(getContext(), R.style.DialogoConFuenteAnta)
                    .setTitle("Eliminar Foto")
                    .setMessage("¿Estás seguro?")
                    .setPositiveButton("Eliminar", (d, w) -> {
                        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
                        firebaseServicio.eliminarFotoEquipo(foto, new FirebaseServicio.OnSimpleCallback() {
                            @Override
                            public void onSuccess() {
                                if (progressBar != null) progressBar.setVisibility(View.GONE);
                                Toast.makeText(getContext(), "Eliminado", Toast.LENGTH_SHORT).show();
                                cargarFotos();
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

        btnDownload.setOnClickListener(v -> descargarImagen(foto.getUrlImagen(), foto.getNombreArchivo()));
        btnShare.setOnClickListener(v -> compartirImagen(foto));

        dialog.show();
    }

    private void descargarImagen(String url, String nombreArchivo) {
        try {
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE);
            request.setTitle("Descargando Foto");
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, nombreArchivo);

            DownloadManager manager = (DownloadManager) requireContext().getSystemService(Context.DOWNLOAD_SERVICE);
            if (manager != null) {
                manager.enqueue(request);
                Toast.makeText(getContext(), "Descarga iniciada...", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(getContext(), "Error descarga", Toast.LENGTH_SHORT).show();
        }
    }

    private void compartirImagen(FotoEquipo foto) {
        Toast.makeText(getContext(), "Preparando...", Toast.LENGTH_SHORT).show();
        Glide.with(this)
                .asBitmap()
                .load(foto.getUrlImagen())
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        new Thread(() -> {
                            if (isAdded() && getContext() != null) {
                                compartirBitmap(resource, "foto_equipo_share.jpg");
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

            Uri contentUri = FileProvider.getUriForFile(requireContext(),
                    "com.example.maquirentapp.provider", newFile);

            if (contentUri != null) {
                Intent shareIntent = new Intent();
                shareIntent.setAction(Intent.ACTION_SEND);
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                shareIntent.setDataAndType(contentUri, requireContext().getContentResolver().getType(contentUri));
                shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
                requireActivity().runOnUiThread(() -> {
                    startActivity(Intent.createChooser(shareIntent, "Compartir vía"));
                });
            }
        } catch (IOException e) {
            Toast.makeText(getContext(), "Error al compartir", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        configurarFab();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).hideGlobalFab();
        }
    }
}