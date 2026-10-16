package com.example.maquirentapp.View;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.maquirentapp.adapters.UsuariosAdapter;
import com.example.maquirentapp.Model.Usuario;
import com.example.maquirentapp.R;
import com.example.maquirentapp.Repository.Callback;
import com.example.maquirentapp.Repository.UsuarioRepository;

import java.util.ArrayList;
import java.util.List;

public class GestionarUsuariosFragment extends Fragment {
    private RecyclerView recyclerView;
    private LinearLayout emptyState;
    private UsuariosAdapter adapter;
    private UsuarioRepository usuarioRepository;
    private List<Usuario> usuariosList = new ArrayList<>();

    public GestionarUsuariosFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_gestionar_usuarios, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        usuarioRepository = new UsuarioRepository();

        initViews(view);
        setupRecyclerView();
        cargarUsuarios();
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerViewUsuarios);
        emptyState = view.findViewById(R.id.emptyState);
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new UsuariosAdapter(new ArrayList<>(), getContext(), () -> {
            cargarUsuarios();
        });
        recyclerView.setAdapter(adapter);
    }

    private void cargarUsuarios() {
        usuarioRepository.getTodosLosUsuarios(new Callback<List<Usuario>>() {
            @Override
            public void onSuccess(List<Usuario> usuarios) {
                usuariosList.clear();
                usuariosList.addAll(usuarios);
                mostrarUsuarios();
            }

            @Override
            public void onError(Exception e) {
                if (getContext() != null) {
                    Toast.makeText(getContext(),
                            "Error al cargar usuarios",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void mostrarUsuarios() {
        if (usuariosList.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyState.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            adapter.actualizarLista(usuariosList);
        }
    }
}
