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
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.example.maquirentapp.adapters.GrupoElectrogenoAdapter;
import com.example.maquirentapp.Model.GrupoElectrogeno;
import com.example.maquirentapp.Repository.Callback;
import com.example.maquirentapp.Repository.GeneradorRepository;
import com.example.maquirentapp.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

public class CGEFragment extends Fragment {
    private RecyclerView recyclerView;
    private GrupoElectrogenoAdapter adapter;
    private List<GrupoElectrogeno> grupoList;
    private GeneradorRepository generadorRepository;
    private NavController navController;
    private ProgressBar progressBar;
    private SwitchMaterial switchEliminados;
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
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        navController = Navigation.findNavController(view);

        generadorRepository = new GeneradorRepository();

        progressBar = view.findViewById(R.id.progressBar);
        switchEliminados = view.findViewById(R.id.switchEliminados);
        recyclerView = view.findViewById(R.id.recycler_grupos_electrogenos);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        setupRecyclerView(view);
        switchEliminados.setOnCheckedChangeListener((buttonView, isChecked) -> {
            cargarGrupos();
        });

    }
    private void setupRecyclerView(View view) {
        recyclerView = view.findViewById(R.id.recycler_grupos_electrogenos);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 1));

        grupoList = new ArrayList<>();
        adapter = new GrupoElectrogenoAdapter(grupo -> {
            Bundle args = new Bundle();
            args.putString("codigo", grupo.getCodigo());
            args.putString("idGrupo", grupo.getId());
            navController.navigate(R.id.action_cge_to_grupoElectrogeno, args);
        });
        recyclerView.setAdapter(adapter);

        cargarGrupos();
    }
    private void cargarGrupos() {
        progressBar.setVisibility(View.VISIBLE);
        boolean incluirEliminados = switchEliminados.isChecked();

        generadorRepository.getGruposElectrogenos(incluirEliminados, new Callback<List<GrupoElectrogeno>>() {
            @Override
            public void onSuccess(List<GrupoElectrogeno> grupos) {
                grupoList.clear();
                grupoList.addAll(grupos);
                adapter.setItems(grupoList);

                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onError(Exception e) {
                Log.e("CGEFragment", "Error al cargar grupos", e);
                Toast.makeText(getContext(), "Error al cargar grupos", Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
            }
        });
    }
}

