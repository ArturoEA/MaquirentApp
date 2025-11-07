package com.example.maquirentapp.View;

import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.example.maquirentapp.Access.VisorImagenesAdapter;
import com.example.maquirentapp.R;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class VisorImagenesFragment extends Fragment {
    private static final String ARG_IMAGENES = "imagenes";
    private static final String ARG_POSICION = "posicion";
    private static final String ARG_PERMITIR_ELIMINAR = "permitirEliminar";

    private List<String> imagenes;
    private int posicionActual;
    private boolean permitirEliminar;

    private ViewPager2 viewPager;
    private MaterialButton btnDescargar, btnCompartir, btnEliminar;

    private EliminarImagenListener eliminarListener;

    public interface EliminarImagenListener {
        void onEliminarImagen(int posicion);
    }

    public static VisorImagenesFragment newInstance(List<String> imagenes, int posicion, boolean permitirEliminar) {
        VisorImagenesFragment fragment = new VisorImagenesFragment();
        Bundle args = new Bundle();
        args.putStringArrayList(ARG_IMAGENES, new ArrayList<>(imagenes));
        args.putInt(ARG_POSICION, posicion);
        args.putBoolean(ARG_PERMITIR_ELIMINAR, permitirEliminar);
        fragment.setArguments(args);
        return fragment;
    }

    public void setEliminarListener(EliminarImagenListener listener) {
        this.eliminarListener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            imagenes = getArguments().getStringArrayList(ARG_IMAGENES);
            posicionActual = getArguments().getInt(ARG_POSICION, 0);
            permitirEliminar = getArguments().getBoolean(ARG_PERMITIR_ELIMINAR, false);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_visor_imagenes, container, false);
        initViews(view);
        setupViewPager();
        configurarBotones();
        return view;
    }

    private void initViews(View view) {
        viewPager = view.findViewById(R.id.viewPagerImagenes);
        btnDescargar = view.findViewById(R.id.btnDescargar);
        btnCompartir = view.findViewById(R.id.btnCompartir);
        btnEliminar = view.findViewById(R.id.btnEliminar);

        // Ocultar botón eliminar si no está permitido
        if (!permitirEliminar) {
            btnEliminar.setVisibility(View.GONE);
        }
    }

    private void setupViewPager() {
        VisorImagenesAdapter adapter = new VisorImagenesAdapter(imagenes);
        viewPager.setAdapter(adapter);
        viewPager.setCurrentItem(posicionActual, false);

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                posicionActual = position;
            }
        });
    }

    private void configurarBotones() {
        btnDescargar.setOnClickListener(v -> descargarImagen());
        btnCompartir.setOnClickListener(v -> compartirImagen());
        btnEliminar.setOnClickListener(v -> eliminarImagen());
    }

    private void descargarImagen() {
        String imagenUrl = imagenes.get(posicionActual);
        try {
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(imagenUrl));
            request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE);
            request.setTitle("Descargando imagen");
            request.setDescription("Descargando imagen del mantenimiento");
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "mantenimiento_" + System.currentTimeMillis() + ".jpg");

            DownloadManager downloadManager = (DownloadManager) requireContext().getSystemService(Context.DOWNLOAD_SERVICE);
            if (downloadManager != null) {
                downloadManager.enqueue(request);
                Toast.makeText(getContext(), "Descarga iniciada", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(getContext(), "Error al descargar", Toast.LENGTH_SHORT).show();
        }
    }

    private void compartirImagen() {
        String imagenUrl = imagenes.get(posicionActual);
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("image/*");
        shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(imagenUrl));
        shareIntent.putExtra(Intent.EXTRA_TEXT, "Imagen del mantenimiento");

        try {
            startActivity(Intent.createChooser(shareIntent, "Compartir imagen"));
        } catch (Exception e) {
            Toast.makeText(getContext(), "Error al compartir", Toast.LENGTH_SHORT).show();
        }
    }

    private void eliminarImagen() {
        if (permitirEliminar && eliminarListener != null) {
            eliminarListener.onEliminarImagen(posicionActual);
            requireActivity().onBackPressed();
        }
    }
}