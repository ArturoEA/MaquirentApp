package com.example.maquirentapp.View;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.maquirentapp.Model.FichaTecnica;
import com.example.maquirentapp.R;
import com.example.maquirentapp.ViewModel.FichaTecnicaViewModel;
import com.example.maquirentapp.adaptadores.FichasTecnicasAdapter;

import java.util.ArrayList;

public class FichasTecnicasFragment extends Fragment {
    private FichaTecnicaViewModel viewModel;
    private FichasTecnicasAdapter adapter;
    private ActivityResultLauncher<Intent> pdfPickerLauncher;

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
                            viewModel.procesarPdfSeleccionado(requireContext(), pdfUri);
                        }
                    }
                });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_fichas_tecnicas, container, false);

        // Configurar RecyclerView
        RecyclerView recyclerView = view.findViewById(R.id.recycler_fichas_tecnicas);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));

        // Configurar Adapter
        adapter = new FichasTecnicasAdapter(new ArrayList<>(), new FichasTecnicasAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(FichaTecnica ficha) {
                if (ficha.tienePdf()) {
                    abrirPdf(ficha);
                } else {
                    seleccionarPdfParaFicha(ficha.getId());
                }
            }

            @Override
            public void onPdfClick(FichaTecnica ficha) {
                abrirPdf(ficha);
            }
        });

        recyclerView.setAdapter(adapter);

        // Observar cambios en las fichas
        viewModel.getFichasLiveData().observe(getViewLifecycleOwner(), fichas -> {
            if (fichas != null) {
                adapter.actualizarLista(fichas);
            }
        });

        // Observar estado de operaciones
        viewModel.getOperacionStatus().observe(getViewLifecycleOwner(), mensaje -> {
            if (mensaje != null && !mensaje.isEmpty()) {
                Toast.makeText(getContext(), mensaje, Toast.LENGTH_SHORT).show();
            }
        });

        // Cargar datos iniciales
        viewModel.cargarDatosIniciales();

        return view;
    }

    private void seleccionarPdfParaFicha(String fichaId) {
        viewModel.prepararParaSubirPdf(fichaId);
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/pdf");
        pdfPickerLauncher.launch(intent);
    }

    // Metodo para abrir PDF
    private void abrirPdf(FichaTecnica ficha) {
        if (ficha != null && ficha.tienePdf()) {
            viewModel.abrirPdf(requireContext(), ficha);
        } else {
            Toast.makeText(getContext(), "No hay PDF asociado", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Limpiar observadores
        viewModel.getFichasLiveData().removeObservers(getViewLifecycleOwner());
        viewModel.getOperacionStatus().removeObservers(getViewLifecycleOwner());
    }
}