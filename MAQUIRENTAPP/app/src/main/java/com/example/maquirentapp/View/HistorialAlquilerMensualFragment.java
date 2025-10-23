package com.example.maquirentapp.View;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.maquirentapp.Model.AlquilerMensual;
import com.example.maquirentapp.Network.FirebaseServicio;
import com.example.maquirentapp.R;
import com.example.maquirentapp.adaptadores.AlquilerMensualAdapter;

import java.util.List;

import androidx.core.content.ContextCompat;

public class HistorialAlquilerMensualFragment extends Fragment {
    private RecyclerView recyclerView;
    private AlquilerMensualAdapter adapter;
    private FirebaseServicio firebaseServicio;
    private String codigo, idGrupo;

    public HistorialAlquilerMensualFragment() { }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            codigo = getArguments().getString("codigo");
            idGrupo = getArguments().getString("idGrupo");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_historial_alquiler_mensual, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        firebaseServicio = new FirebaseServicio();

        recyclerView = view.findViewById(R.id.recyclerAlquileres);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new AlquilerMensualAdapter();
        recyclerView.setAdapter(adapter);

        fetchAlquileresMensuales();
    }

    @Override
    public void onResume() {
        super.onResume();
        configureGlobalFab();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (getActivity() instanceof com.example.maquirentapp.MainActivity) {
            ((com.example.maquirentapp.MainActivity) getActivity()).hideGlobalFab();
        } else {
            View activityFab = getActivity() != null ? getActivity().findViewById(R.id.btnGlobal) : null;
            if (activityFab != null) activityFab.setVisibility(View.GONE);
        }
    }

    private void fetchAlquileresMensuales() {
        firebaseServicio.getAlquileresMensuales(new FirebaseServicio.OnAlquileresLoadedListener() {
            @Override
            public void onSuccess(List<AlquilerMensual> alquileres) {
                List<AlquilerMensual> filtrados = new java.util.ArrayList<>();
                if (idGrupo != null && !idGrupo.isEmpty()) {
                    for (AlquilerMensual a : alquileres) {
                        if (a.getIdGrupo() != null && a.getIdGrupo().equals(idGrupo)) {
                            filtrados.add(a);
                        }
                    }
                } else if (codigo != null && !codigo.isEmpty()) {
                    for (AlquilerMensual a : alquileres) {
                        if (a.getIdGrupo() != null && a.getIdGrupo().equals(codigo)) {
                            filtrados.add(a);
                        }
                    }
                }
                adapter.setItems(filtrados);
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(getContext(),
                        "Error al cargar alquileres: " + e.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void configureGlobalFab() {
        View hostView = getView();
        if (hostView == null) return;

        if (getActivity() instanceof com.example.maquirentapp.MainActivity) {
            com.example.maquirentapp.MainActivity main = (com.example.maquirentapp.MainActivity) getActivity();
            main.showGlobalFab(
                    "Añadir",
                    R.drawable.icon_nuevo_blanco,
                    v -> {
                        Bundle args = new Bundle();
                        if (codigo != null) args.putString("codigo", codigo);
                        if (idGrupo != null) args.putString("idGrupo", idGrupo);
                        Navigation.findNavController(hostView)
                                .navigate(R.id.action_historialAlquilerMensual_to_nuevoAlquilerMensual, args);
                    }
            );
        } else {
            View activityFab = getActivity() != null ? getActivity().findViewById(R.id.btnGlobal) : null;
            if (activityFab != null && activityFab instanceof com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton) {
                com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton fab =
                        (com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton) activityFab;
                fab.setText("Añadir");
                try {
                    fab.setIcon(ContextCompat.getDrawable(requireContext(), R.drawable.icon_nuevo_blanco));
                } catch (Exception ignored) { }
                fab.setOnClickListener(v -> {
                    Bundle args = new Bundle();
                    if (codigo != null) args.putString("codigo", codigo);
                    if (idGrupo != null) args.putString("idGrupo", idGrupo);
                    Navigation.findNavController(hostView)
                            .navigate(R.id.action_historialAlquilerMensual_to_nuevoAlquilerMensual, args);
                });
                fab.setVisibility(View.VISIBLE);
            }
        }
    }
}
