package com.example.maquirentapp.View;

import android.app.Dialog;
import android.os.Bundle;
import android.net.Uri;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import com.example.maquirentapp.Access.GrupoElectrogenoAdapter;
import com.example.maquirentapp.Model.GrupoElectrogeno;
import com.example.maquirentapp.Network.ApiServicio;
import com.example.maquirentapp.Network.FirebaseServicio;
import com.example.maquirentapp.Network.RetrofitCliente;
import com.example.maquirentapp.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class CGEFragment extends Fragment {
    private RecyclerView recyclerView;
    private GrupoElectrogenoAdapter adapter;
    private FirebaseServicio firebaseServicio;
    private ActivityResultLauncher<String> imagePickerLauncher;
    private Uri selectedImageUri;
    private ImageView dialogImagePreview;

    public CGEFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_cge, container, false);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Configurar launcher para seleccionar imágenes
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        selectedImageUri = uri;
                        if (dialogImagePreview != null) {
                            dialogImagePreview.setImageURI(uri);
                        }
                    }
                });
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        NavController navController = Navigation.findNavController(view);
        firebaseServicio = new FirebaseServicio();

        recyclerView = view.findViewById(R.id.recycler_grupos_electrogenos);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new GrupoElectrogenoAdapter(grupo -> {
            Bundle args = new Bundle();
            args.putString("codigo", grupo.getCodigo());
            navController.navigate(
                    R.id.action_cge_to_grupoElectrogeno,
                    args
            );
        });
        recyclerView.setAdapter(adapter);

        // Configurar botón de agregar grupo
        CardView btnAgregar = view.findViewById(R.id.btnAgregarGrupo);
        btnAgregar.setOnClickListener(v -> mostrarDialogoNuevoGrupo());

        fetchGruposElectrogenos();

    }
    private void mostrarDialogoNuevoGrupo() {
        Dialog dialog = new Dialog(requireContext());
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_nuevo_grupo, null);
        dialog.setContentView(dialogView);

        // Referencias a las vistas del diálogo
        dialogImagePreview = dialogView.findViewById(R.id.imgPreview);
        MaterialButton btnSeleccionarFoto = dialogView.findViewById(R.id.btnSeleccionarFoto);
        TextInputEditText inputCodigo = dialogView.findViewById(R.id.inputCodigo);
        MaterialButton btnCancelar = dialogView.findViewById(R.id.btnCancelar);
        MaterialButton btnGuardar = dialogView.findViewById(R.id.btnGuardar);

        // Configurar listeners
        btnSeleccionarFoto.setOnClickListener(v -> {
            imagePickerLauncher.launch("image/*");
        });

        btnCancelar.setOnClickListener(v -> {
            selectedImageUri = null;
            dialog.dismiss();
        });

        btnGuardar.setOnClickListener(v -> {
            String codigo = inputCodigo.getText().toString().trim();
            if (codigo.isEmpty()) {
                Toast.makeText(requireContext(), "Ingresa un código", Toast.LENGTH_SHORT).show();
                return;
            }

            // Deshabilitar botón mientras se guarda
            btnGuardar.setEnabled(false);
            btnGuardar.setText("Guardando");

            firebaseServicio.crearGrupoConImagen(codigo, selectedImageUri,
                    new FirebaseServicio.OnGrupoCreatedListener() {
                        @Override
                        public void onSuccess(GrupoElectrogeno grupo) {
                            Toast.makeText(requireContext(), "Grupo creado exitosamente", Toast.LENGTH_SHORT).show();
                            selectedImageUri = null;
                            dialog.dismiss();
                            fetchGruposElectrogenos(); // Recargar lista
                        }

                        @Override
                        public void onError(Exception e) {
                            Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            btnGuardar.setEnabled(true);
                            btnGuardar.setText("Guardar");
                        }
                    });
        });

        dialog.show();
    }

    private void fetchGruposElectrogenos() {
        firebaseServicio.getGruposElectrogenos(new FirebaseServicio.OnGruposLoadedListener() {
            @Override
            public void onSuccess(List<GrupoElectrogeno> grupos) {
                adapter.setItems(grupos);
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(getContext(),
                        "Error al cargar grupos: " + e.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }
}
