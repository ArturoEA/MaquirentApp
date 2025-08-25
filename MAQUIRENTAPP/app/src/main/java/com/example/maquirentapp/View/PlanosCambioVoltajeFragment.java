package com.example.maquirentapp.View;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.maquirentapp.Model.Plano;
import com.example.maquirentapp.R;
import com.example.maquirentapp.ViewModel.PlanosViewModel;
import com.example.maquirentapp.adaptadores.PlanoAdapter;


import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.List;
import java.util.UUID;

public class PlanosCambioVoltajeFragment extends Fragment implements PlanoAdapter.OnPlanoClickListener {

    private PlanosViewModel viewModel;
    private PlanoAdapter adapter;
    private final ActivityResultLauncher<Intent> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    if (imageUri != null) {
                        String uniqueID = UUID.randomUUID().toString();
                        Plano nuevoPlano = new Plano(uniqueID, imageUri.toString(), "Nuevo Plano");
                        viewModel.addPlano(nuevoPlano);
                    }
                }
            });

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(PlanosViewModel.class);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_planos_cambio_voltaje, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView recyclerView = view.findViewById(R.id.recyclerPlanos);
        FloatingActionButton btnAdd = view.findViewById(R.id.btnAddPlano);

        adapter = new PlanoAdapter(null, this);
        recyclerView.setAdapter(adapter);

        viewModel.getPlanos().observe(getViewLifecycleOwner(), planos -> {
            if (planos != null) {
                adapter.updateList(planos);
            }
        });

        btnAdd.setOnClickListener(v -> openImagePicker());
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

    @Override
    public void onPlanoClick(Plano plano) {
        // Acción al hacer clic en un plano (puedes implementar vista ampliada)
        showFullScreenPreview(plano.getImageUrl());
    }

    @Override
    public void onDeleteClick(Plano plano) {
        viewModel.removePlano(plano);
    }
    private void showFullScreenPreview(String imageUrl) {
        Dialog dialog = new Dialog(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.setContentView(R.layout.dialog_fullscreen_image);

        ImageView fullImageView = dialog.findViewById(R.id.fullImageView);
        ImageView btnClose = dialog.findViewById(R.id.btnClose);

        // Cargar la imagen en tamaño completo
        if (imageUrl.startsWith("drawable/")) {
            int resourceId = requireContext().getResources()
                    .getIdentifier(imageUrl.replace("drawable/", ""),
                            "drawable", requireContext().getPackageName());
            Glide.with(this).load(resourceId).into(fullImageView);
        } else {
            Glide.with(this).load(Uri.parse(imageUrl)).into(fullImageView);
        }

        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }
}