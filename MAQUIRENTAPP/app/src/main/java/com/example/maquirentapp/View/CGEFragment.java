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

import java.util.ArrayList;
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

        fetchGruposElectrogenos();

    }
    private void fetchGruposElectrogenos() {
        firebaseServicio.getGruposElectrogenos(new FirebaseServicio.OnGruposLoadedListener() {
            @Override
            public void onSuccess(List<GrupoElectrogeno> grupos) {
                List<GrupoElectrogeno> filtrados = new ArrayList<>();
                if (grupos != null) {
                    for (GrupoElectrogeno g : grupos) {
                        if (g != null && !g.isEliminado()) {
                            filtrados.add(g);
                        }
                    }
                }
                if (getActivity() != null && recyclerView != null) {
                    recyclerView.post(() -> adapter.setItems(filtrados));
                } else {
                    adapter.setItems(filtrados);
                }
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
