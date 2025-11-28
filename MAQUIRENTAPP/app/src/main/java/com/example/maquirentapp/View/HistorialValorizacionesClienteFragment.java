package com.example.maquirentapp.View;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.maquirentapp.MainActivity;
import com.example.maquirentapp.Model.ClienteValorizacion;
import com.example.maquirentapp.Model.Valorizacion;
import com.example.maquirentapp.Repository.ValorizacionesRepository;
import com.example.maquirentapp.R;
import com.example.maquirentapp.Access.ValorizacionesAdapter;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

import it.xabaras.android.recyclerview.swipedecorator.RecyclerViewSwipeDecorator;

public class HistorialValorizacionesClienteFragment extends Fragment {

    private ClienteValorizacion clienteActual;
    private RecyclerView recyclerView;
    private ValorizacionesAdapter adapter;
    private ValorizacionesRepository repository;
    private TextView tvTituloCliente;
    private LinearLayout emptyState;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        repository = new ValorizacionesRepository();
        if (getArguments() != null) {
            clienteActual = (ClienteValorizacion) getArguments().getSerializable("cliente");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Necesitas crear este layout: fragment_historial_valorizaciones_cliente
        // Básicamente un TextView título + RecyclerView + EmptyState
        return inflater.inflate(R.layout.fragment_historial_valorizaciones_cliente, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvTituloCliente = view.findViewById(R.id.tvTituloCliente);
        recyclerView = view.findViewById(R.id.recyclerValorizaciones);
        emptyState = view.findViewById(R.id.emptyStateVal);

        if (clienteActual != null) {
            tvTituloCliente.setText(clienteActual.getNombreEmpresa() + " (" + clienteActual.getAnio() + ")");
        }

        setupRecyclerView();
        cargarHistorial();
        configureFab();
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ValorizacionesAdapter(valorizacion -> {
            // AQUÍ GENERAREMOS EL EXCEL (Próximo paso)
            // ExcelGenerator.generar...(valorizacion)
            Toast.makeText(getContext(), "Abrir Excel: " + valorizacion.getNumeroValorizacion(), Toast.LENGTH_SHORT).show();
        });
        recyclerView.setAdapter(adapter);
        setupSwipeToDelete();
    }

    private void cargarHistorial() {
        if (clienteActual == null) return;

        repository.getValorizacionesPorCliente(clienteActual.getId(), new ValorizacionesRepository.Callback<List<Valorizacion>>() {
            @Override
            public void onSuccess(List<Valorizacion> result) {
                adapter.submitList(result);
                if (result.isEmpty()) {
                    recyclerView.setVisibility(View.GONE);
                    if (emptyState != null) emptyState.setVisibility(View.VISIBLE);
                } else {
                    recyclerView.setVisibility(View.VISIBLE);
                    if (emptyState != null) emptyState.setVisibility(View.GONE);
                }
            }
            @Override
            public void onError(Exception e) {
                Toast.makeText(getContext(), "Error cargando historial", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void configureFab() {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).showGlobalFab(
                    "Nueva Valorización",
                    R.drawable.icon_nuevo_blanco,
                    v -> {
                        Bundle args = new Bundle();
                        args.putSerializable("cliente", clienteActual);
                        Navigation.findNavController(getView()).navigate(R.id.action_historialValClien_to_NuevaValorizacionFragment, args);
                    }
            );
        }
    }

    private void setupSwipeToDelete() {
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(@NonNull RecyclerView r, @NonNull RecyclerView.ViewHolder v, @NonNull RecyclerView.ViewHolder t) { return false; }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int pos = viewHolder.getAdapterPosition();
                Valorizacion item = adapter.getItem(pos);

                new MaterialAlertDialogBuilder(getContext(), R.style.DialogoConFuenteAnta)
                        .setTitle("Eliminar")
                        .setMessage("¿Eliminar valorización " + item.getNumeroValorizacion() + "?")
                        .setPositiveButton("Eliminar", (d, w) -> {
                            repository.eliminarValorizacion(item.getId(), new ValorizacionesRepository.Callback<Void>() {
                                @Override public void onSuccess(Void res) {
                                    List<Valorizacion> lista = new ArrayList<>(adapter.getCurrentList());
                                    lista.remove(pos);
                                    adapter.submitList(lista);
                                }
                                @Override public void onError(Exception e) { adapter.notifyItemChanged(pos); }
                            });
                        })
                        .setNegativeButton("Cancelar", (d, w) -> adapter.notifyItemChanged(pos))
                        .show();
            }
            // onChildDraw con SwipeDecorator...
        }).attachToRecyclerView(recyclerView);
    }

    @Override
    public void onResume() {
        super.onResume();
        configureFab();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (getActivity() instanceof MainActivity) ((MainActivity) getActivity()).hideGlobalFab();
    }
}