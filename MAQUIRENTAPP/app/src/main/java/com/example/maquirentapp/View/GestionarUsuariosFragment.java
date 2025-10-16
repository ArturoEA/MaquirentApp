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

import com.example.maquirentapp.Access.UsuariosAdapter;
import com.example.maquirentapp.Model.Usuario;
import com.example.maquirentapp.R;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class GestionarUsuariosFragment extends Fragment {
    private RecyclerView recyclerView;
    private LinearLayout emptyState;
    private UsuariosAdapter adapter;
    private FirebaseFirestore db;
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

        db = FirebaseFirestore.getInstance();

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
        db.collection("usuarios")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        usuariosList.clear();
                        int cantidadUsuarios = task.getResult().size();

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Usuario usuario = new Usuario();
                            usuario.setUid(document.getId());
                            usuario.setNombre(document.getString("nombre"));
                            usuario.setEmail(document.getString("email"));
                            usuario.setRol(document.getString("rol"));
                            usuario.setEstado(document.getString("estado"));

                            usuariosList.add(usuario);
                        }

                        mostrarUsuarios();

                    } else {
                        Toast.makeText(getContext(),
                                "Error al cargar usuarios",
                                Toast.LENGTH_SHORT).show();
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