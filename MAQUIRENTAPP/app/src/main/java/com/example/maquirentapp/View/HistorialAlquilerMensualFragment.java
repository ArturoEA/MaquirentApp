package com.example.maquirentapp.View;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.ItemTouchHelper;
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
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

import androidx.core.content.ContextCompat;

public class HistorialAlquilerMensualFragment extends Fragment {
    private RecyclerView recyclerView;
    private AlquilerMensualAdapter adapter;
    private FirebaseServicio firebaseServicio;
    private String codigo, idGrupo;
    private AlquilerMensual alquilerSeleccionado;

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
        adapter.setOnItemClickListener(alquiler -> {
            alquilerSeleccionado = alquiler;
            configureGlobalFab();
            mostrarDetallesAlquiler(alquiler);
        });
        recyclerView.setAdapter(adapter);

        configurarSwipeToDelete();
        fetchAlquileresMensuales();
    }

    @Override
    public void onResume() {
        super.onResume();
        alquilerSeleccionado = null; // Reset al volver
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

    private void configurarSwipeToDelete() {
        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                AlquilerMensual alquilerAEliminar = adapter.getItem(position);

                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Eliminar alquiler")
                        .setMessage("¿Estás seguro de que deseas eliminar este alquiler?")
                        .setPositiveButton("Eliminar", (dialog, which) -> {
                            firebaseServicio.eliminarAlquilerMensual(alquilerAEliminar.getId(), new FirebaseServicio.OnAlquilerDeletedListener() {
                                @Override
                                public void onSuccess() {
                                    Toast.makeText(getContext(), "Alquiler eliminado correctamente", Toast.LENGTH_SHORT).show();
                                    fetchAlquileresMensuales();
                                }

                                @Override
                                public void onError(Exception e) {
                                    Toast.makeText(getContext(), "Error al eliminar: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                    adapter.notifyItemChanged(position);
                                }
                            });
                        })
                        .setNegativeButton("Cancelar", (dialog, which) -> {
                            adapter.notifyItemChanged(position);
                        })
                        .setOnCancelListener(dialog -> adapter.notifyItemChanged(position))
                        .show();
            }
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);
    }

    private void mostrarDetallesAlquiler(AlquilerMensual alquiler) {
        // Navegar directamente a la vista de detalle/edición en modo solo lectura
        View hostView = getView();
        if (hostView == null) return;

        Bundle args = new Bundle();
        if (codigo != null) args.putString("codigo", codigo);
        if (idGrupo != null) args.putString("idGrupo", idGrupo);
        args.putString("alquilerId", alquiler.getId());
        args.putBoolean("modoSoloLectura", true);

        Navigation.findNavController(hostView)
                .navigate(R.id.action_historialAlquilerMensual_to_nuevoAlquilerMensual, args);
    }

    private void configureGlobalFab() {
        View hostView = getView();
        if (hostView == null) return;

        // Solo mostrar botón "Añadir" ya que el click en item navega directamente
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