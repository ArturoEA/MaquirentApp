package com.example.maquirentapp.View;

import android.app.AlertDialog;
import android.graphics.Canvas;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.maquirentapp.Access.TareasAdapter;
import com.example.maquirentapp.Access.UsuariosSeleccionAdapter;
import com.example.maquirentapp.MainActivity;
import com.example.maquirentapp.Model.Tarea;
import com.example.maquirentapp.Model.Usuario;
import com.example.maquirentapp.Network.FirebaseServicio;
import com.example.maquirentapp.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import it.xabaras.android.recyclerview.swipedecorator.RecyclerViewSwipeDecorator;

public class TareasFragment extends Fragment {

    private RecyclerView recyclerTareas;
    private TareasAdapter adapter;
    private FirebaseServicio firebaseServicio;
    private List<Usuario> listaUsuarios = new ArrayList<>();
    private Map<String, Usuario> mapaUsuarios = new HashMap<>();
    private boolean isAdmin = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        firebaseServicio = new FirebaseServicio();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_tareas, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerTareas = view.findViewById(R.id.recyclerTareas);
        recyclerTareas.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new TareasAdapter(this::mostrarDialogoCompletar);
        recyclerTareas.setAdapter(adapter);

        configurarFab();

        firebaseServicio.verificarSiEsAdmin(esAdmin -> {
            this.isAdmin = esAdmin;
        });

        cargarUsuariosYTareas();
        setupSwipeToDelete();
    }
    private void setupSwipeToDelete() {
        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();

                if (position == RecyclerView.NO_POSITION) return;

                if (!isAdmin) {
                    Toast.makeText(getContext(), "Solo los administradores pueden eliminar tareas", Toast.LENGTH_SHORT).show();
                    adapter.notifyItemChanged(position);
                    return;
                }

                Tarea tarea = adapter.getItem(position);

                new MaterialAlertDialogBuilder(getContext(), R.style.DialogoConFuenteAnta)
                        .setTitle("Eliminar Tarea")
                        .setMessage("¿Estás seguro de eliminar '" + tarea.getTitulo() + "'?")
                        .setPositiveButton("Eliminar", (dialog, which) -> {
                            firebaseServicio.eliminarTarea(tarea.getId(), new FirebaseServicio.OnSimpleCallback() {
                                @Override
                                public void onSuccess() {
                                    Toast.makeText(getContext(), "Tarea eliminada", Toast.LENGTH_SHORT).show();

                                    List<Tarea> listaActualizada = new ArrayList<>(adapter.getCurrentList());

                                    for (int i = 0; i < listaActualizada.size(); i++) {
                                        if (listaActualizada.get(i).getId().equals(tarea.getId())) {
                                            listaActualizada.remove(i);
                                            break;
                                        }
                                    }

                                    adapter.submitList(listaActualizada);
                                }

                                @Override
                                public void onError(Exception e) {
                                    Toast.makeText(getContext(), "Error al eliminar", Toast.LENGTH_SHORT).show();
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

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                new RecyclerViewSwipeDecorator.Builder(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
                        .addBackgroundColor(ContextCompat.getColor(requireContext(), R.color.red_accent))
                        .addActionIcon(R.drawable.icon_eliminar_rojo)
                        .setActionIconTint(R.color.white)
                        .addCornerRadius(TypedValue.COMPLEX_UNIT_DIP, 30)
                        .create()
                        .decorate();
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }
        };

        new ItemTouchHelper(simpleCallback).attachToRecyclerView(recyclerTareas);
    }

    private void cargarUsuariosYTareas() {
        firebaseServicio.getUsuariosActivos(new FirebaseServicio.OnUsuariosListener() {
            @Override
            public void onSuccess(List<Usuario> usuarios) {
                listaUsuarios = usuarios;
                mapaUsuarios.clear();
                for (Usuario u : usuarios) {
                    mapaUsuarios.put(u.getUid(), u);
                }
                if (adapter != null) {
                    adapter.setUsuariosMap(mapaUsuarios);
                }
                cargarTareas();
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(getContext(), "Error al cargar usuarios", Toast.LENGTH_SHORT).show();
                cargarTareas();
            }
        });
    }

    private void cargarTareas() {
        firebaseServicio.getTareas(new FirebaseServicio.OnTareasLoadedListener() {
            @Override
            public void onSuccess(List<Tarea> tareas) {
                List<Tarea> nuevaLista = new ArrayList<>(tareas);
                adapter.submitList(nuevaLista);
                Log.d("DEBUG", "Tareas cargadas: " + tareas.size());
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(getContext(), "Error al cargar tareas", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void configurarFab() {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).showGlobalFab(
                    "Añadir",
                    R.drawable.icon_nuevo_blanco,
                    v -> mostrarDialogoNuevaTarea()
            );
        }
    }

    private void mostrarDialogoNuevaTarea() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_nueva_tarea, null);
        builder.setView(view);
        AlertDialog dialog = builder.create();

        if (dialog.getWindow() != null)
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        TextInputEditText input = view.findViewById(R.id.inputNombreTarea);
        Button btnGuardar = view.findViewById(R.id.btnGuardarTarea);

        btnGuardar.setOnClickListener(v -> {
            String titulo = input.getText().toString().trim();
            if (!titulo.isEmpty()) {
                firebaseServicio.crearTarea(titulo, new FirebaseServicio.OnSimpleCallback() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(getContext(), "Tarea creada", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    }

                    @Override
                    public void onError(Exception e) {
                        Toast.makeText(getContext(), "Error al crear tarea", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        dialog.show();
    }

    private void mostrarDialogoCompletar(Tarea tarea) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_seleccionar_participantes, null);
        builder.setView(view);
        AlertDialog dialog = builder.create();

        if (dialog.getWindow() != null)
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        RecyclerView recyclerPart = view.findViewById(R.id.recyclerParticipantes);
        Button btnCompletar = view.findViewById(R.id.btnCompletarTarea);

        recyclerPart.setLayoutManager(new LinearLayoutManager(getContext()));
        UsuariosSeleccionAdapter userAdapter = new UsuariosSeleccionAdapter(listaUsuarios);
        recyclerPart.setAdapter(userAdapter);

        btnCompletar.setOnClickListener(v -> {
            List<String> seleccionados = userAdapter.getSeleccionados();
            if (seleccionados.isEmpty()) {
                Toast.makeText(getContext(), "Selecciona al menos un participante", Toast.LENGTH_SHORT).show();
                return;
            }

            firebaseServicio.completarTarea(tarea.getId(), seleccionados, new FirebaseServicio.OnSimpleCallback() {
                @Override
                public void onSuccess() {
                    Toast.makeText(getContext(), "Tarea completada", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                }

                @Override
                public void onError(Exception e) {
                    Toast.makeText(getContext(), "Error al actualizar tarea", Toast.LENGTH_SHORT).show();
                }
            });
        });

        dialog.show();
    }

    @Override
    public void onResume() {
        super.onResume();
        configurarFab();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).hideGlobalFab();
        }
    }
}