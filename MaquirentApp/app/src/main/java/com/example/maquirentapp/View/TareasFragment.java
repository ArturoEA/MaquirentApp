package com.example.maquirentapp.View;

import android.os.Bundle;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.maquirentapp.Model.Tarea;
import com.example.maquirentapp.Model.Usuario;
import com.example.maquirentapp.R;

import com.example.maquirentapp.ViewModel.TareasViewModel;
import com.example.maquirentapp.adaptadores.SeleccionResponsablesAdapter;
import com.example.maquirentapp.adaptadores.TareasAdapter;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class TareasFragment extends Fragment {
    private TareasViewModel viewModel;
    private TareasAdapter tareasAdapter;
    private List<Usuario> usuariosActivos = new ArrayList<>();

    public TareasFragment() {
        // Required empty public constructor
    }
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(TareasViewModel.class);
    }
    @Nullable

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_tareas, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView recyclerView = view.findViewById(R.id.recyclerViewTareas);
        FloatingActionButton fabAgregar = view.findViewById(R.id.fabAgregarTarea);

        tareasAdapter = new TareasAdapter(this::mostrarDialogoSeleccionResponsables);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(tareasAdapter);

        viewModel.obtenerTareas().observe(getViewLifecycleOwner(), tareasAdapter::submitList);
        viewModel.obtenerUsuariosActivos().observe(getViewLifecycleOwner(), usuarios -> {
            usuariosActivos = usuarios != null ? usuarios : new ArrayList<>();
        });

        fabAgregar.setOnClickListener(v -> mostrarDialogoAgregarTarea());
    }

    private void mostrarDialogoAgregarTarea() {
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        View dialogView = inflater.inflate(R.layout.dialog_add_task, null, false);

        TextInputLayout textInputLayout = dialogView.findViewById(R.id.textInputLayoutTaskName);
        TextInputEditText editTextNombre = dialogView.findViewById(R.id.editTextNombreTarea);

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.titulo_dialogo_agregar_tarea)
                .setView(dialogView)
                .setPositiveButton(R.string.accion_guardar, null)
                .setNegativeButton(android.R.string.cancel, (dialogInterface, which) -> dialogInterface.dismiss())
                .create();

        dialog.setOnShowListener(dialogInterface -> {
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(v -> {
                String nombreTarea = editTextNombre.getText() != null
                        ? editTextNombre.getText().toString().trim()
                        : "";

                if (TextUtils.isEmpty(nombreTarea)) {
                    textInputLayout.setError(getString(R.string.mensaje_error_nombre_tarea));
                    return;
                }

                textInputLayout.setError(null);
                viewModel.agregarTarea(nombreTarea);
                dialog.dismiss();
            });
        });

        dialog.show();
    }

    private void mostrarDialogoSeleccionResponsables(Tarea tarea) {
        if (tarea == null || tarea.isCompletada()) {
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(requireContext());
        View dialogView = inflater.inflate(R.layout.dialog_responsables_tarea, null, false);

        TextView textNombreTarea = dialogView.findViewById(R.id.textNombreTareaSeleccion);
        TextView textTituloSeleccion = dialogView.findViewById(R.id.textSeleccioneResponsable);
        RecyclerView recyclerViewUsuarios = dialogView.findViewById(R.id.recyclerViewResponsables);
        TextView textEstadoVacio = dialogView.findViewById(R.id.textUsuariosVacios);
        MaterialButton botonMarcarCompletada = dialogView.findViewById(R.id.buttonMarcarCompletada);

        textNombreTarea.setText(tarea.getTitulo());
        textTituloSeleccion.setText(getString(R.string.texto_seleccione_responsable));

        SeleccionResponsablesAdapter adapter = new SeleccionResponsablesAdapter();
        adapter.setOnSeleccionChangeListener(() -> {
            List<Usuario> seleccionados = adapter.obtenerSeleccionados();
            botonMarcarCompletada.setEnabled(!seleccionados.isEmpty());
        });

        recyclerViewUsuarios.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerViewUsuarios.setAdapter(adapter);

        adapter.submitList(usuariosActivos);
        actualizarEstadoListaUsuarios(textEstadoVacio, recyclerViewUsuarios, botonMarcarCompletada);

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(dialogView)
                .setCancelable(true)
                .create();

        botonMarcarCompletada.setOnClickListener(v -> {
            List<Usuario> seleccionados = adapter.obtenerSeleccionados();
            if (seleccionados.isEmpty()) {
                Toast.makeText(requireContext(), R.string.mensaje_error_seleccione_responsable, Toast.LENGTH_SHORT).show();
                return;
            }
            viewModel.marcarTareaComoCompletada(tarea, seleccionados);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void actualizarEstadoListaUsuarios(TextView textoVacio, RecyclerView recyclerView, MaterialButton botonMarcar) {
        boolean hayUsuarios = usuariosActivos != null && !usuariosActivos.isEmpty();
        textoVacio.setVisibility(hayUsuarios ? View.GONE : View.VISIBLE);
        recyclerView.setVisibility(hayUsuarios ? View.VISIBLE : View.GONE);
        botonMarcar.setEnabled(false);
    }
}

