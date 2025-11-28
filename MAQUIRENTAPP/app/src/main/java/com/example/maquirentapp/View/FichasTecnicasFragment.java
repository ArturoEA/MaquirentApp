package com.example.maquirentapp.View;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.maquirentapp.Model.FichaTecnica;
import com.example.maquirentapp.R;
import com.example.maquirentapp.ViewModel.FichaTecnicaViewModel;
import com.example.maquirentapp.adaptadores.FichasTecnicasAdapter;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.util.ArrayList;

public class FichasTecnicasFragment extends Fragment {
    private FichaTecnicaViewModel viewModel;
    private FichasTecnicasAdapter adapter;
    private RecyclerView recyclerView;
    private TextView tvEmptyState;
    private ProgressBar progressBar;
    private ActivityResultLauncher<Intent> pdfPickerLauncher;
    private ActivityResultLauncher<String> requestPermissionLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inicializar ViewModel
        viewModel = new ViewModelProvider(this).get(FichaTecnicaViewModel.class);

        // Configurar el lanzador de selección de PDF
        pdfPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri pdfUri = result.getData().getData();
                        if (pdfUri != null) {
                            viewModel.subirPdf(requireContext(), pdfUri);
                        }
                    }
                });

        // Configurar lanzador de permisos
        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (!isGranted) {
                        Toast.makeText(getContext(),
                                "Permiso denegado. No se podrá descargar archivos.",
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_fichas_tecnicas, container, false);

        // Inicializar vistas
        recyclerView = view.findViewById(R.id.recycler_fichas_tecnicas);
        tvEmptyState = view.findViewById(R.id.tv_empty_state);
        progressBar = view.findViewById(R.id.progress_bar);

        // Configurar RecyclerView
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));

        // Configurar Adapter
        adapter = new FichasTecnicasAdapter(new ArrayList<>(), new FichasTecnicasAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(FichaTecnica ficha) {
                viewModel.abrirPdf(requireContext(), ficha);
            }

            @Override
            public void onCompartirClick(FichaTecnica ficha) {
                viewModel.compartirPdf(requireContext(), ficha);
            }

            @Override
            public void onDescargarClick(FichaTecnica ficha) {
                verificarPermisoYDescargar(ficha);
            }

            @Override
            public void onEliminarClick(FichaTecnica ficha) {
                mostrarDialogoEliminar(ficha);
            }
        });

        recyclerView.setAdapter(adapter);

        // Observar LiveData
        setupObservers();

        // Cargar fichas
        viewModel.cargarFichasTecnicas();

        return view;
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
    private void configureGlobalFab() {
        if (getActivity() instanceof com.example.maquirentapp.MainActivity) {
            com.example.maquirentapp.MainActivity main = (com.example.maquirentapp.MainActivity) getActivity();
            main.showGlobalFab("Añadir", R.drawable.icon_nuevo_blanco, v -> abrirSelectorPdf());
        }
    }
    private void hideGlobalFab() {
        if (getActivity() instanceof com.example.maquirentapp.MainActivity) {
            ((com.example.maquirentapp.MainActivity) getActivity()).hideGlobalFab();
        }
    }
    private void setupObservers() {
        // Observar lista de fichas
        viewModel.getFichasLiveData().observe(getViewLifecycleOwner(), fichas -> {
            if (fichas != null) {
                adapter.actualizarLista(fichas);

                // Mostrar mensaje si no hay fichas
                if (fichas.isEmpty()) {
                    recyclerView.setVisibility(View.GONE);
                    tvEmptyState.setVisibility(View.VISIBLE);
                } else {
                    recyclerView.setVisibility(View.VISIBLE);
                    tvEmptyState.setVisibility(View.GONE);
                }
            }
        });

        // Observar mensajes de estado
        viewModel.getOperacionStatus().observe(getViewLifecycleOwner(), mensaje -> {
            if (mensaje != null && !mensaje.isEmpty()) {
                Toast.makeText(getContext(), mensaje, Toast.LENGTH_SHORT).show();
            }
        });

        // Observar estado de carga
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            if (isLoading != null) {
                progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            }
        });
    }

    private void abrirSelectorPdf() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/pdf");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        pdfPickerLauncher.launch(intent);
    }

    private void verificarPermisoYDescargar(FichaTecnica ficha) {
        // Android 10+ no necesita permiso para usar DIRECTORY_DOWNLOADS
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            viewModel.descargarPdf(requireContext(), ficha);
            return;
        }

        // Para versiones anteriores, verificar permiso
        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            viewModel.descargarPdf(requireContext(), ficha);
        } else {
            requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
    }

    private void mostrarDialogoEliminar(FichaTecnica ficha) {
        new MaterialAlertDialogBuilder(requireContext(), R.style.DialogoConFuenteAnta)
                .setTitle("Eliminar ficha")
                .setMessage("¿Estás seguro de eliminar '" + ficha.getNombreArchivo() + "'?")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    viewModel.eliminarPdf(ficha);
                })
                .setNegativeButton("Cancelar", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        hideGlobalFab();
        // Limpiar observadores
        viewModel.getFichasLiveData().removeObservers(getViewLifecycleOwner());
        viewModel.getOperacionStatus().removeObservers(getViewLifecycleOwner());
        viewModel.getIsLoading().removeObservers(getViewLifecycleOwner());
    }
}