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
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.maquirentapp.adapters.TareasAdapter;
import com.example.maquirentapp.adapters.UsuariosSeleccionAdapter;
import com.example.maquirentapp.MainActivity;
import com.example.maquirentapp.Model.Tarea;
import com.example.maquirentapp.Model.Usuario;
import com.example.maquirentapp.Repository.Callback;
import com.example.maquirentapp.Repository.TareaRepository;
import com.example.maquirentapp.Repository.UsuarioRepository;
import com.example.maquirentapp.R;
import com.facebook.shimmer.ShimmerFrameLayout;
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
    private ShimmerFrameLayout shimmerTareas;
    private TareasAdapter adapter;
    private TareaRepository tareaRepository;
    private UsuarioRepository usuarioRepository;
    private List<Usuario> listaUsuarios = new ArrayList<>();
    private Map<String, Usuario> mapaUsuarios = new HashMap<>();
    private boolean isAdmin = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        tareaRepository = new TareaRepository();
        usuarioRepository = new UsuarioRepository();
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
        shimmerTareas = view.findViewById(R.id.shimmerTareas);
        recyclerTareas.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new TareasAdapter(this::mostrarDialogoCompletar);
        recyclerTareas.setAdapter(adapter);

        configurarFab();

        usuarioRepository.verificarSiEsAdmin(new Callback<Boolean>() {
            @Override
            public void onSuccess(Boolean esAdmin) {
                isAdmin = Boolean.TRUE.equals(esAdmin);
            }

            @Override
            public void onError(Exception e) {
                isAdmin = false;
            }
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
                    if (getContext() == null) return;
                    Toast.makeText(getContext(), "Solo los administradores pueden eliminar tareas", Toast.LENGTH_SHORT).show();
                    adapter.notifyItemChanged(position);
                    return;
                }

                Tarea tarea = adapter.getItem(position);

                new MaterialAlertDialogBuilder(getContext(), R.style.DialogoConFuenteAnta)
                        .setTitle("Eliminar Tarea")
                        .setMessage("¿Estás seguro de eliminar '" + tarea.getTitulo() + "'?")
                        .setPositiveButton("Eliminar", (dialog, which) -> {
                            tareaRepository.eliminarTarea(tarea.getId(), new Callback<Void>() {
                                @Override
                                public void onSuccess(Void result) {
                                    if (getContext() == null) return;
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
                                    if (getContext() == null) return;
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
        if (shimmerTareas != null) {
            shimmerTareas.setVisibility(View.VISIBLE);
            shimmerTareas.startShimmer();
        }
        recyclerTareas.setVisibility(View.GONE);

        usuarioRepository.getUsuariosActivos(new Callback<List<Usuario>>() {
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
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Error al cargar usuarios", Toast.LENGTH_SHORT).show();
                }
                cargarTareas();
            }
        });
    }

    private void cargarTareas() {
        tareaRepository.getTareas(new Callback<List<Tarea>>() {
            @Override
            public void onSuccess(List<Tarea> tareas) {
                if (shimmerTareas != null) {
                    shimmerTareas.stopShimmer();
                    shimmerTareas.setVisibility(View.GONE);
                }
                recyclerTareas.setVisibility(View.VISIBLE);
                List<Tarea> nuevaLista = new ArrayList<>(tareas);
                adapter.submitList(nuevaLista);
                Log.d("DEBUG", "Tareas cargadas: " + tareas.size());
            }

            @Override
            public void onError(Exception e) {
                if (shimmerTareas != null) {
                    shimmerTareas.stopShimmer();
                    shimmerTareas.setVisibility(View.GONE);
                }
                recyclerTareas.setVisibility(View.VISIBLE);
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Error al cargar tareas", Toast.LENGTH_SHORT).show();
                }
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
                tareaRepository.crearTarea(titulo, new Callback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        if (getContext() == null) return;
                        Toast.makeText(getContext(), "Tarea creada", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        cargarTareas();
                    }

                    @Override
                    public void onError(Exception e) {
                        if (getContext() == null) return;
                        Toast.makeText(getContext(), "Error al crear tarea", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        dialog.show();
    }

    private void mostrarDialogoCompletar(Tarea tarea) {
        if (getContext() == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_seleccionar_participantes, null);
        builder.setView(view);
        AlertDialog dialog = builder.create();

        if (dialog.getWindow() != null)
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        TextView tvTitulo = view.findViewById(R.id.tvTituloDialogoTarea);
        TextView tvSubtitulo = view.findViewById(R.id.tvSubtituloResponsables);
        LinearLayout layoutChecklistContainer = view.findViewById(R.id.layoutChecklistContainer);
        LinearLayout layoutChecklistItems = view.findViewById(R.id.layoutChecklistItems);
        RecyclerView recyclerPart = view.findViewById(R.id.recyclerParticipantes);
        Button btnCompletar = view.findViewById(R.id.btnCompletarTarea);
        Button btnAprobarGerencia = view.findViewById(R.id.btnAprobarGerencia);

        if (tvTitulo != null) {
            tvTitulo.setText(tarea.getTitulo() != null ? tarea.getTitulo() : "Detalle de Tarea");
        }

        // Renderizar Checklist si la tarea cuenta con items SOP
        List<CheckBox> checkBoxes = new ArrayList<>();
        if (tarea.getChecklist() != null && !tarea.getChecklist().isEmpty()) {
            layoutChecklistContainer.setVisibility(View.VISIBLE);
            layoutChecklistItems.removeAllViews();
            for (Map<String, Object> itemMap : tarea.getChecklist()) {
                String desc = itemMap.get("item") != null ? itemMap.get("item").toString() : "";
                Object compObj = itemMap.get("completado");
                boolean checked = Boolean.TRUE.equals(compObj);

                CheckBox cb = new CheckBox(getContext());
                cb.setText(desc);
                cb.setChecked(checked);
                cb.setTextColor(ContextCompat.getColor(getContext(), R.color.black));
                if (tarea.isCompletada()) {
                    cb.setEnabled(false);
                }
                layoutChecklistItems.addView(cb);
                checkBoxes.add(cb);
            }
        } else {
            layoutChecklistContainer.setVisibility(View.GONE);
        }

        recyclerPart.setLayoutManager(new LinearLayoutManager(getContext()));
        UsuariosSeleccionAdapter userAdapter = new UsuariosSeleccionAdapter(listaUsuarios);
        recyclerPart.setAdapter(userAdapter);

        if (tarea.isCompletada()) {
            // Modo visualización de tarea ya terminada
            btnCompletar.setVisibility(View.GONE);
            if (tvSubtitulo != null) {
                tvSubtitulo.setText("Personal participante:");
            }
            if (tarea.getParticipantesIds() != null) {
                userAdapter.setSeleccionados(tarea.getParticipantesIds());
            }

            // Si es administrador/gerencia y aún no tiene V°B°, mostrar botón de aprobación
            if (isAdmin && !tarea.isAprobadoPorGerencia()) {
                btnAprobarGerencia.setVisibility(View.VISIBLE);
                btnAprobarGerencia.setOnClickListener(v -> {
                    String nombreAprobador = "Gerencia";
                    com.google.firebase.auth.FirebaseUser cu = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
                    if (cu != null && mapaUsuarios.containsKey(cu.getUid())) {
                        Usuario u = mapaUsuarios.get(cu.getUid());
                        if (u != null && u.getNombre() != null) {
                            nombreAprobador = u.getNombre();
                        }
                    }
                    tareaRepository.aprobarPorGerencia(tarea.getId(), nombreAprobador, new Callback<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            if (getContext() == null) return;
                            Toast.makeText(getContext(), "Visto Bueno de Gerencia registrado", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                            cargarTareas();
                        }

                        @Override
                        public void onError(Exception e) {
                            if (getContext() == null) return;
                            Toast.makeText(getContext(), "Error al aprobar tarea", Toast.LENGTH_SHORT).show();
                        }
                    });
                });
            } else if (tarea.isAprobadoPorGerencia()) {
                btnAprobarGerencia.setVisibility(View.VISIBLE);
                btnAprobarGerencia.setEnabled(false);
                String aprobador = tarea.getAprobadoPorNombre() != null ? tarea.getAprobadoPorNombre() : "Gerencia";
                btnAprobarGerencia.setText("✓ V°B° Aprobado por " + aprobador);
            }
        } else {
            // Modo completar tarea
            btnCompletar.setVisibility(View.VISIBLE);
            btnAprobarGerencia.setVisibility(View.GONE);

            btnCompletar.setOnClickListener(v -> {
                List<String> seleccionados = userAdapter.getSeleccionados();
                if (seleccionados.isEmpty()) {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Selecciona al menos un participante", Toast.LENGTH_SHORT).show();
                    }
                    return;
                }

                // Actualizar estado del checklist
                List<Map<String, Object>> checklistActualizado = new ArrayList<>();
                if (tarea.getChecklist() != null) {
                    for (int i = 0; i < tarea.getChecklist().size(); i++) {
                        Map<String, Object> mapOriginal = tarea.getChecklist().get(i);
                        Map<String, Object> itemMap = new HashMap<>(mapOriginal);
                        if (i < checkBoxes.size()) {
                            itemMap.put("completado", checkBoxes.get(i).isChecked());
                        }
                        checklistActualizado.add(itemMap);
                    }
                }

                tareaRepository.completarTareaConChecklist(tarea.getId(), seleccionados, checklistActualizado, new Callback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        if (getContext() == null) return;
                        Toast.makeText(getContext(), "Tarea completada exitosamente", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        cargarTareas();
                    }

                    @Override
                    public void onError(Exception e) {
                        if (getContext() == null) return;
                        Toast.makeText(getContext(), "Error al actualizar tarea", Toast.LENGTH_SHORT).show();
                    }
                });
            });
        }

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
