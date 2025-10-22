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
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class GestionarUsuariosFragment extends Fragment {
    private RecyclerView recyclerView;
    private TabLayout tabLayout;
    private LinearLayout emptyState;
    private UsuariosAdapter adapter;
    private FirebaseFirestore db;

    private List<Usuario> todosUsuarios = new ArrayList<>();
    private List<Usuario> usuariosPendientes = new ArrayList<>();
    private List<Usuario> usuariosActivos = new ArrayList<>();

    private String filtroActual = "todos"; // todos, pendientes, activos

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
        setupTabs();
        cargarUsuarios();
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerViewUsuarios);
        tabLayout = view.findViewById(R.id.tabLayout);
        emptyState = view.findViewById(R.id.emptyState);
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new UsuariosAdapter(todosUsuarios, getContext(), () -> {
            // Cuando se actualiza un usuario, recargar la lista
            cargarUsuarios();
        });
        recyclerView.setAdapter(adapter);
    }

    private void setupTabs() {
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int position = tab.getPosition();
                if (position == 0) {
                    filtroActual = "todos";
                    mostrarUsuarios(todosUsuarios);
                } else if (position == 1) {
                    filtroActual = "pendientes";
                    mostrarUsuarios(usuariosPendientes);
                } else if (position == 2) {
                    filtroActual = "activos";
                    mostrarUsuarios(usuariosActivos);
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void cargarUsuarios() {
        db.collection("usuarios")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        todosUsuarios.clear();
                        usuariosPendientes.clear();
                        usuariosActivos.clear();

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Usuario usuario = new Usuario();
                            usuario.setUid(document.getId());
                            usuario.setNombre(document.getString("nombre"));
                            usuario.setEmail(document.getString("email"));
                            usuario.setRol(document.getString("rol"));
                            usuario.setEstado(document.getString("estado"));

                            // Agregar a lista de todos
                            todosUsuarios.add(usuario);

                            // Agregar a listas filtradas
                            String estado = usuario.getEstado();
                            if ("pendiente".equals(estado)) {
                                usuariosPendientes.add(usuario);
                            } else if ("activo".equals(estado)) {
                                usuariosActivos.add(usuario);
                            }
                        }

                        // Mostrar usuarios según el filtro actual
                        if ("todos".equals(filtroActual)) {
                            mostrarUsuarios(todosUsuarios);
                        } else if ("pendientes".equals(filtroActual)) {
                            mostrarUsuarios(usuariosPendientes);
                        } else if ("activos".equals(filtroActual)) {
                            mostrarUsuarios(usuariosActivos);
                        }

                        // Actualizar tabs con contadores
                        actualizarContadoresTabs();

                    } else {
                        Toast.makeText(getContext(),
                                "Error al cargar usuarios",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void mostrarUsuarios(List<Usuario> usuarios) {
        if (usuarios.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyState.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            adapter.actualizarLista(usuarios);
        }
    }

    private void actualizarContadoresTabs() {
        TabLayout.Tab tabTodos = tabLayout.getTabAt(0);
        TabLayout.Tab tabPendientes = tabLayout.getTabAt(1);
        TabLayout.Tab tabActivos = tabLayout.getTabAt(2);

        if (tabTodos != null) {
            tabTodos.setText("Todos (" + todosUsuarios.size() + ")");
        }
        if (tabPendientes != null) {
            tabPendientes.setText("Pendientes (" + usuariosPendientes.size() + ")");
        }
        if (tabActivos != null) {
            tabActivos.setText("Activos (" + usuariosActivos.size() + ")");
        }
    }
}