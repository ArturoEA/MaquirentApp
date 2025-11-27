package com.example.maquirentapp.View;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.maquirentapp.MainActivity;
import com.example.maquirentapp.Model.ClienteValorizacion;
import com.example.maquirentapp.Repository.ValorizacionesRepository;
import com.example.maquirentapp.R;
import com.example.maquirentapp.Access.ClientesValorizacionAdapter;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Calendar;
import java.util.List;

public class ListaClientesValorizacionFragment extends Fragment {

    private RecyclerView recyclerView;
    private ClientesValorizacionAdapter adapter;
    private ValorizacionesRepository repository;
    private int anioSeleccionado;

    // Vistas filtro
    private TextView tvAnio;
    private ImageButton btnAnterior, btnSiguiente;
    private LinearLayout emptyState; // Opcional si agregas un empty state en el XML

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        repository = new ValorizacionesRepository();
        anioSeleccionado = Calendar.getInstance().get(Calendar.YEAR);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Asegúrate de que este nombre coincida con tu archivo XML
        return inflater.inflate(R.layout.fragment_lista_clientes_valorizacion, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvAnio = view.findViewById(R.id.tvAnio);
        btnAnterior = view.findViewById(R.id.btnAnterior);
        btnSiguiente = view.findViewById(R.id.btnSiguiente);
        recyclerView = view.findViewById(R.id.recyclerClientesValorizaciones);
        // emptyState = view.findViewById(R.id.emptyState); // Si lo agregas al XML

        setupRecyclerView();
        setupFilterListeners();
        actualizarTextoAnio();

        cargarClientes();
        configureFab();
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ClientesValorizacionAdapter(cliente -> {
            Bundle args = new Bundle();
            args.putSerializable("cliente", cliente);
            Navigation.findNavController(getView()).navigate(R.id.action_listaClientes_to_historialValorizacionesCliente, args);
        });
        recyclerView.setAdapter(adapter);
    }

    private void cargarClientes() {
        // Mostrar un pequeño loader si quieres, o solo cargar
        repository.getClientesPorAnio(anioSeleccionado, new ValorizacionesRepository.Callback<List<ClienteValorizacion>>() {
            @Override
            public void onSuccess(List<ClienteValorizacion> result) {
                adapter.submitList(result);

                // Manejo de Empty State (si decides agregarlo al XML)
                /*
                if (result.isEmpty()) {
                    recyclerView.setVisibility(View.GONE);
                    if (emptyState != null) emptyState.setVisibility(View.VISIBLE);
                } else {
                    recyclerView.setVisibility(View.VISIBLE);
                    if (emptyState != null) emptyState.setVisibility(View.GONE);
                }
                */
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(getContext(), "Error cargando clientes", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupFilterListeners() {
        btnAnterior.setOnClickListener(v -> {
            anioSeleccionado--;
            actualizarTextoAnio();
            cargarClientes();
        });
        btnSiguiente.setOnClickListener(v -> {
            anioSeleccionado++;
            actualizarTextoAnio();
            cargarClientes();
        });
        tvAnio.setOnClickListener(v -> mostrarSelectorAnio());
    }

    private void actualizarTextoAnio() {
        if (tvAnio != null) tvAnio.setText(String.valueOf(anioSeleccionado));
    }

    // Implementación del Selector de Año
    private void mostrarSelectorAnio() {
        int anioActual = Calendar.getInstance().get(Calendar.YEAR);

        NumberPicker picker = new NumberPicker(getContext());
        picker.setMinValue(anioActual - 10);
        picker.setMaxValue(anioActual + 10);
        picker.setValue(anioSeleccionado);
        picker.setWrapSelectorWheel(false); // Para que no de la vuelta

        new MaterialAlertDialogBuilder(getContext())
                .setTitle("Seleccionar Año Fiscal")
                .setView(picker)
                .setPositiveButton("Aceptar", (dialog, which) -> {
                    anioSeleccionado = picker.getValue();
                    actualizarTextoAnio();
                    cargarClientes();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void configureFab() {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).showGlobalFab(
                    "Añadir",
                    R.drawable.icon_nuevo_blanco,
                    v -> mostrarDialogoNuevoCliente()
            );
        }
    }

    private void mostrarDialogoNuevoCliente() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_nuevo_cliente_valorizacion, null);
        builder.setView(view);
        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        TextInputEditText inputNombre = view.findViewById(R.id.inputNombre);
        TextInputEditText inputRuc = view.findViewById(R.id.inputRuc);
        TextInputEditText inputDireccion = view.findViewById(R.id.inputDireccion);
        TextInputEditText inputUbicacion = view.findViewById(R.id.inputUbicacion);
        Button btnGuardar = view.findViewById(R.id.btnGuardarCliente);

        btnGuardar.setOnClickListener(v -> {
            String nombre = inputNombre.getText().toString().trim();
            String ruc = inputRuc.getText().toString().trim();
            String dir = inputDireccion.getText().toString().trim();
            String ubi = inputUbicacion.getText().toString().trim();

            if (nombre.isEmpty() || ruc.isEmpty() || dir.isEmpty()) {
                Toast.makeText(getContext(), "Nombre, RUC y Dirección son obligatorios", Toast.LENGTH_SHORT).show();
                return;
            }

            ClienteValorizacion cliente = new ClienteValorizacion(nombre, ruc, dir, ubi, anioSeleccionado);

            repository.crearClienteValorizacion(cliente, new ValorizacionesRepository.Callback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    Toast.makeText(getContext(), "Cliente creado para el año " + anioSeleccionado, Toast.LENGTH_SHORT).show();
                    cargarClientes(); // Refrescar lista
                    dialog.dismiss();
                }

                @Override
                public void onError(Exception e) {
                    Toast.makeText(getContext(), "Error al crear: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });

        dialog.show();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).hideGlobalFab();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        configureFab();
        cargarClientes(); // Recargar por si hubo cambios
    }
}