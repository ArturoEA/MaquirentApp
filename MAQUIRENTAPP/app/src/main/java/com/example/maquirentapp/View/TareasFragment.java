package com.example.maquirentapp.View;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.maquirentapp.MainActivity;
import com.example.maquirentapp.Model.GrupoElectrogeno;
import com.example.maquirentapp.Model.MantenimientoConfiguracion;
import com.example.maquirentapp.Model.Tarea;
import com.example.maquirentapp.Model.Usuario;
import com.example.maquirentapp.R;
import com.example.maquirentapp.Repository.Callback;
import com.example.maquirentapp.Repository.GeneradorRepository;
import com.example.maquirentapp.Utils.MantenimientoSaludManager;
import com.example.maquirentapp.Utils.MantenimientoSaludManager.EstadoSalud;
import com.example.maquirentapp.Utils.MantenimientoSaludManager.EstadoSaludTarea;
import com.example.maquirentapp.Utils.MantenimientoSaludManager.ReporteSaludEquipo;
import com.example.maquirentapp.adapters.MaquinaTareaAdapter;
import com.example.maquirentapp.adapters.MaquinaTareaAdapter.MaquinaConReporte;
import com.example.maquirentapp.adapters.TareasAdapter;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TareasFragment extends Fragment {

    private RecyclerView recyclerTareas;
    private RecyclerView recyclerTareasManuales;
    private TextView tvSinTareasManuales;
    private ShimmerFrameLayout shimmerTareas;

    private MaquinaTareaAdapter adapterMaquinas;
    private TareasAdapter adapterManuales;

    private final GeneradorRepository generadorRepository = new GeneradorRepository();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private ListenerRegistration tareasListener;
    private final List<GrupoElectrogeno> gruposCargados = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_tareas, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerTareas = view.findViewById(R.id.recyclerTareas);
        recyclerTareasManuales = view.findViewById(R.id.recyclerTareasManuales);
        tvSinTareasManuales = view.findViewById(R.id.tvSinTareasManuales);
        shimmerTareas = view.findViewById(R.id.shimmerTareas);

        view.findViewById(R.id.btnAgregarTareaManual).setOnClickListener(v -> mostrarDialogoNuevaTareaManual());

        recyclerTareasManuales.setLayoutManager(new LinearLayoutManager(getContext()));
        adapterManuales = new TareasAdapter(this::mostrarOpcionesTareaManual);
        recyclerTareasManuales.setAdapter(adapterManuales);

        recyclerTareas.setLayoutManager(new GridLayoutManager(getContext(), 2));
        adapterMaquinas = new MaquinaTareaAdapter(requireContext(), (grupo, reporte) -> abrirChecklistMaquina(grupo));
        recyclerTareas.setAdapter(adapterMaquinas);

        cargarUsuariosYEscucharTareasManuales();
        cargarMaquinasConSalud();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).showGlobalFab("Nueva tarea", R.drawable.icon_nuevo_blanco, v -> mostrarDialogoNuevaTareaManual());
        }
        cargarMaquinasConSalud();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).hideGlobalFab();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (tareasListener != null) {
            tareasListener.remove();
            tareasListener = null;
        }
    }

    private void abrirChecklistMaquina(GrupoElectrogeno grupo) {
        ChecklistDialogFragment dialog = new ChecklistDialogFragment();
        Bundle args = new Bundle();
        args.putString("grupoId", grupo.getId());
        args.putString("codigoGrupo", grupo.getCodigo());
        dialog.setArguments(args);
        dialog.setOnRegistrarMantenimientoListener((codigoGrupo, itemsSeleccionados) -> {
            if (!isAdded() || getView() == null) return;
            Bundle navArgs = new Bundle();
            navArgs.putString("codigo", codigoGrupo);
            navArgs.putStringArrayList(NuevoMantenimientoFragment.ARG_ITEMS_PRESELECCIONADOS, itemsSeleccionados);
            try {
                Navigation.findNavController(getView()).navigate(R.id.action_tareas_to_nuevoMantenimiento, navArgs);
            } catch (Exception e) {
                Navigation.findNavController(getView()).navigate(R.id.nuevoMantenimientoFragment, navArgs);
            }
        });
        dialog.show(requireActivity().getSupportFragmentManager(), "checklist_dialog");
    }

    private void cargarMaquinasConSalud() {
        if (shimmerTareas != null && adapterMaquinas.getItemCount() == 0) {
            shimmerTareas.setVisibility(View.VISIBLE);
            shimmerTareas.startShimmer();
        }
        generadorRepository.getGruposElectrogenos(false, new Callback<List<GrupoElectrogeno>>() {
            @Override
            public void onSuccess(List<GrupoElectrogeno> grupos) {
                if (!isAdded()) return;
                gruposCargados.clear();
                if (grupos != null) gruposCargados.addAll(grupos);

                MantenimientoSaludManager.cargarPlantillasConfiguracion(requireContext(), new Callback<List<MantenimientoConfiguracion>>() {
                    @Override
                    public void onSuccess(List<MantenimientoConfiguracion> plantillas) {
                        if (!isAdded()) return;
                        ocultarShimmer();

                        List<MaquinaConReporte> items = new ArrayList<>();
                        for (GrupoElectrogeno g : gruposCargados) {
                            ReporteSaludEquipo rep = MantenimientoSaludManager.evaluarSaludGrupo(g, plantillas);
                            items.add(new MaquinaConReporte(g, rep));
                        }

                        // Ordenar: máquinas con vencidas primero, luego próximas, luego al día
                        Collections.sort(items, (a, b) -> Integer.compare(prioridadMaquina(b.reporte), prioridadMaquina(a.reporte)));
                        adapterMaquinas.setData(items);
                    }

                    @Override
                    public void onError(Exception e) {
                        ocultarShimmer();
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                ocultarShimmer();
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Error al cargar máquinas", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private int prioridadMaquina(ReporteSaludEquipo rep) {
        if (rep == null || rep.todasLasTareas == null) return 0;
        int vencidas = 0;
        int proximas = 0;
        for (EstadoSaludTarea t : rep.todasLasTareas) {
            if (t.isVencido()) vencidas++;
            else if (t.estado == EstadoSalud.PROXIMO) proximas++;
        }
        return vencidas * 100 + proximas;
    }

    private void ocultarShimmer() {
        if (shimmerTareas != null) {
            shimmerTareas.stopShimmer();
            shimmerTareas.setVisibility(View.GONE);
        }
    }

    // =========================================================================
    // TAREAS MANUALES / TALLER
    // =========================================================================

    private void cargarUsuariosYEscucharTareasManuales() {
        db.collection("usuarios").get().addOnSuccessListener(snap -> {
            if (!isAdded()) return;
            Map<String, Usuario> map = new HashMap<>();
            if (snap != null) {
                for (QueryDocumentSnapshot d : snap) {
                    Usuario u = d.toObject(Usuario.class);
                    u.setUid(d.getId());
                    map.put(d.getId(), u);
                }
            }
            adapterManuales.setUsuariosMap(map);
        });

        if (tareasListener != null) tareasListener.remove();
        tareasListener = db.collection("tareas").addSnapshotListener((snap, error) -> {
            if (!isAdded() || error != null || snap == null) return;
            List<Tarea> lista = new ArrayList<>();
            for (QueryDocumentSnapshot doc : snap) {
                Tarea t = doc.toObject(Tarea.class);
                t.setId(doc.getId());
                // Evitar duplicar PREVENTIVO antiguo si ya usamos el cálculo en tiempo real por máquina
                if (!"PREVENTIVO".equalsIgnoreCase(t.getTipoTarea())) {
                    lista.add(t);
                }
            }
            // Pendientes primero, luego completadas
            Collections.sort(lista, (a, b) -> Boolean.compare(a.isCompletada(), b.isCompletada()));
            adapterManuales.setItems(lista);
            if (tvSinTareasManuales != null) {
                tvSinTareasManuales.setVisibility(lista.isEmpty() ? View.VISIBLE : View.GONE);
            }
        });
    }

    private void mostrarDialogoNuevaTareaManual() {
        if (!isAdded()) return;
        android.view.ContextThemeWrapper themedCtx = new android.view.ContextThemeWrapper(requireContext(), R.style.Theme_TextMain);
        android.graphics.Typeface anta = androidx.core.content.res.ResourcesCompat.getFont(requireContext(), R.font.anta_font);

        LinearLayout layout = new LinearLayout(themedCtx);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 24, 48, 8);

        TextView tvAreaLabel = new TextView(themedCtx);
        tvAreaLabel.setText("Área o máquina");
        tvAreaLabel.setTextSize(12);
        tvAreaLabel.setTextColor(android.graphics.Color.parseColor("#90CAF9"));
        if (anta != null) tvAreaLabel.setTypeface(anta);
        layout.addView(tvAreaLabel);

        Spinner spArea = new Spinner(themedCtx);
        List<String> opcionesArea = new ArrayList<>();
        opcionesArea.add("Taller");
        opcionesArea.add("General");
        for (GrupoElectrogeno g : gruposCargados) {
            if (g.getCodigo() != null && !g.getCodigo().trim().isEmpty()) {
                opcionesArea.add(g.getCodigo().trim());
            }
        }
        ArrayAdapter<String> spAdapter = new ArrayAdapter<String>(themedCtx, android.R.layout.simple_spinner_dropdown_item, opcionesArea) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View v = super.getView(position, convertView, parent);
                if (v instanceof TextView && anta != null) ((TextView) v).setTypeface(anta);
                return v;
            }

            @Override
            public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View v = super.getDropDownView(position, convertView, parent);
                if (v instanceof TextView && anta != null) ((TextView) v).setTypeface(anta);
                return v;
            }
        };
        spArea.setAdapter(spAdapter);
        layout.addView(spArea);

        EditText etTitulo = new EditText(themedCtx);
        etTitulo.setHint("Ej. Limpiar taller, ordenar repuestos...");
        if (anta != null) etTitulo.setTypeface(anta);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = 20;
        etTitulo.setLayoutParams(lp);
        layout.addView(etTitulo);

        new MaterialAlertDialogBuilder(requireContext(), R.style.DialogoConFuenteAnta)
                .setTitle("Nueva tarea")
                .setView(layout)
                .setPositiveButton("Crear", (dialog, which) -> {
                    String titulo = etTitulo.getText() != null ? etTitulo.getText().toString().trim() : "";
                    if (titulo.isEmpty()) {
                        Toast.makeText(getContext(), "Ingresa una descripción", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String area = spArea.getSelectedItem() != null ? spArea.getSelectedItem().toString() : "Taller";
                    crearTareaManual(area, titulo);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void crearTareaManual(String area, String titulo) {
        Tarea tarea = new Tarea();
        tarea.setTitulo(area + ": " + titulo);
        tarea.setTipoTarea("Taller".equalsIgnoreCase(area) ? "TALLER" : ("General".equalsIgnoreCase(area) ? "GENERAL" : "CORRECTIVO"));
        tarea.setCodigoGrupo(("Taller".equalsIgnoreCase(area) || "General".equalsIgnoreCase(area)) ? "" : area);
        tarea.setFechaCreacion(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date()));
        tarea.setCompletada(false);

        db.collection("tareas").add(tarea)
                .addOnSuccessListener(doc -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Tarea agregada", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Error al crear tarea", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void mostrarOpcionesTareaManual(Tarea tarea) {
        if (!isAdded() || tarea.getId() == null) return;
        String accionEstado = tarea.isCompletada() ? "Marcar como pendiente" : "Marcar como completada";
        String[] opciones = {accionEstado, "Eliminar tarea"};

        new MaterialAlertDialogBuilder(requireContext(), R.style.DialogoConFuenteAnta)
                .setTitle(tarea.getTitulo())
                .setItems(opciones, (dialog, which) -> {
                    if (which == 0) {
                        boolean nuevoEstado = !tarea.isCompletada();
                        Map<String, Object> up = new HashMap<>();
                        up.put("completada", nuevoEstado);
                        if (nuevoEstado && FirebaseAuth.getInstance().getCurrentUser() != null) {
                            String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                            List<String> part = tarea.getParticipantesIds() != null
                                    ? new ArrayList<>(tarea.getParticipantesIds()) : new ArrayList<>();
                            if (!part.contains(uid)) part.add(uid);
                            up.put("participantesIds", part);
                        }
                        db.collection("tareas").document(tarea.getId()).update(up);
                    } else if (which == 1) {
                        db.collection("tareas").document(tarea.getId()).delete();
                    }
                })
                .show();
    }
}
