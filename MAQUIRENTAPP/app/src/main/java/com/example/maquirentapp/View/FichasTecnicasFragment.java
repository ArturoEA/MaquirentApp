package com.example.maquirentapp.View;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.maquirentapp.Model.FichaTecnica;
import com.example.maquirentapp.R;
import com.example.maquirentapp.Repository.Callback;
import com.example.maquirentapp.ViewModel.FichaTecnicaViewModel;
import com.example.maquirentapp.adapters.FichasTecnicasAdapter;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.util.ArrayList;

public class FichasTecnicasFragment extends Fragment {
    private FichaTecnicaViewModel viewModel;
    private FichasTecnicasAdapter adapter;
    private RecyclerView recyclerView;
    private TextView tvEmptyState;
    private com.facebook.shimmer.ShimmerFrameLayout shimmerFichas;
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
                            mostrarDialogoRenombrar(pdfUri);
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
        shimmerFichas = view.findViewById(R.id.shimmerFichas);

        // Configurar RecyclerView
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));

        // Configurar Adapter
        adapter = new FichasTecnicasAdapter(new ArrayList<>(), new FichasTecnicasAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(FichaTecnica ficha) {
                abrirPdf(ficha);
            }

            @Override
            public void onCompartirClick(FichaTecnica ficha) {
                compartirPdf(ficha);
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
                if (isLoading) {
                    shimmerFichas.setVisibility(View.VISIBLE);
                    shimmerFichas.startShimmer();
                    recyclerView.setVisibility(View.GONE);
                } else {
                    shimmerFichas.stopShimmer();
                    shimmerFichas.setVisibility(View.GONE);
                }
            }
        });
    }

    private void abrirSelectorPdf() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/pdf");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        pdfPickerLauncher.launch(intent);
    }
    private void mostrarDialogoRenombrar(Uri pdfUri) {
        // Extraemos el nombre original solo para dárselo como "sugerencia" al usuario
        String nombreOriginal = "";
        try (android.database.Cursor cursor = requireContext().getContentResolver().query(pdfUri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                if (index >= 0) {
                    nombreOriginal = cursor.getString(index);
                }
            }
        }
        if (nombreOriginal == null || nombreOriginal.isEmpty()) {
            nombreOriginal = pdfUri.getLastPathSegment();
        }

        // Le quitamos el ".pdf" final para que sea más limpio de editar
        if (nombreOriginal != null && nombreOriginal.toLowerCase().endsWith(".pdf")) {
            nombreOriginal = nombreOriginal.substring(0, nombreOriginal.length() - 4);
        }

        // Crear el campo de texto para el diálogo
        TextInputEditText inputNombre = new TextInputEditText(requireContext());
        inputNombre.setText(nombreOriginal); // Pre-llenar
        inputNombre.setHint("Ej. FT 75-109");

        // Añadirle un poco de margen para que no se pegue a los bordes
        android.widget.FrameLayout container = new android.widget.FrameLayout(requireContext());
        android.widget.FrameLayout.LayoutParams params = new android.widget.FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(50, 20, 50, 0);
        inputNombre.setLayoutParams(params);
        container.addView(inputNombre);

        // Mostrar el Diálogo Material
        new MaterialAlertDialogBuilder(requireContext(), R.style.DialogoConFuenteAnta)
                .setTitle("Nombre de la Ficha Técnica")
                .setMessage("Asigna un nombre claro para encontrar este documento fácilmente.")
                .setView(container)
                .setCancelable(false)
                .setPositiveButton("Subir", (dialog, which) -> {
                    String nombreFinal = inputNombre.getText().toString().trim();
                    if (nombreFinal.isEmpty()) {
                        Toast.makeText(getContext(), "El nombre no puede estar vacío", Toast.LENGTH_SHORT).show();
                    } else {
                        viewModel.subirPdf(pdfUri, nombreFinal);
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void abrirPdf(FichaTecnica ficha) {
        if (ficha == null || ficha.getUrlPdf() == null || getContext() == null) return;
        Intent intent = com.example.maquirentapp.View.PdfViewerActivity.newIntent(
                requireContext(),
                ficha.getUrlPdf(),
                ficha.getNombreArchivo()
        );
        startActivity(intent);
    }

    private void compartirPdf(FichaTecnica ficha) {
        if (ficha == null || ficha.getUrlPdf() == null || getContext() == null) return;
        File tempFile = new File(requireContext().getCacheDir(), ficha.getNombreArchivo());
        viewModel.descargarPdfAArchivo(ficha.getUrlPdf(), tempFile, new Callback<File>() {
            @Override
            public void onSuccess(File file) {
                if (getContext() == null) return;
                Uri fileUri = FileProvider.getUriForFile(requireContext(),
                        requireContext().getPackageName() + ".provider", file);

                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("application/pdf");
                shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, ficha.getNombreArchivo());
                shareIntent.putExtra(Intent.EXTRA_TEXT, "Te comparto esta ficha técnica");
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                Intent chooser = Intent.createChooser(shareIntent, "Compartir PDF");
                chooser.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(chooser);
            }

            @Override
            public void onError(Exception e) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Error al preparar archivo: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void descargarPdf(FichaTecnica ficha) {
        if (ficha == null || ficha.getUrlPdf() == null || getContext() == null) return;
        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File destFile = new File(downloadsDir, ficha.getNombreArchivo());
        viewModel.descargarPdfAArchivo(ficha.getUrlPdf(), destFile, new Callback<File>() {
            @Override
            public void onSuccess(File file) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Descargado en: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onError(Exception e) {
                // Handled in viewModel status LiveData
            }
        });
    }

    private void verificarPermisoYDescargar(FichaTecnica ficha) {
        // Android 10+ no necesita permiso para usar DIRECTORY_DOWNLOADS
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            descargarPdf(ficha);
            return;
        }

        // Para versiones anteriores, verificar permiso
        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            descargarPdf(ficha);
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
