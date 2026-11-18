package com.example.maquirentapp.View;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.maquirentapp.Model.Mantenimiento;
import com.example.maquirentapp.Model.MantenimientoConfiguracion;
import com.example.maquirentapp.R;
import com.example.maquirentapp.Utils.MantenimientoSaludManager;
import com.example.maquirentapp.Utils.NetworkUtils;
import com.example.maquirentapp.adapters.MantenimientosAdapter;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.Source;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import it.xabaras.android.recyclerview.swipedecorator.RecyclerViewSwipeDecorator;

public class MantenimientosFragment extends Fragment {
    private static final String TAG = "MantenimientosFragment";

    private String codigo;
    private RecyclerView recyclerView;
    private LinearLayout emptyState;
    private MantenimientosAdapter adapter;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private Calendar calendarioActual;
    private TextView tvMes;
    private ImageButton btnAnterior, btnSiguiente;
    private List<Mantenimiento> todosMantenimientosGrupo = new ArrayList<>();
    private List<Mantenimiento> mantenimientosList = new ArrayList<>();
    private List<MantenimientoConfiguracion> itemsConfigList = new ArrayList<>();
    private ImageButton btnAbrirBuscador, btnCerrarBuscador;
    private View layoutHeader;
    private View layoutBuscador;
    private TextInputEditText etBuscador;
    private ProgressBar progressBarBusqueda;
    private ShimmerFrameLayout shimmerMantenimientos;
    private boolean isSearchMode = false;

    public MantenimientosFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            codigo = getArguments().getString("codigo");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_mantenimientos, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeFirebase();
        initViews(view);
        setupRecyclerView();
        cargarItemsConfiguracion();
    }

    private void initializeFirebase() {
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
    }

    private void initViews(View view) {
        isSearchMode = false;
        if (mantenimientosList != null) mantenimientosList.clear();

        recyclerView = view.findViewById(R.id.recyclerViewMantenimientos);
        emptyState = view.findViewById(R.id.emptyStateMantenimientos);

        tvMes = view.findViewById(R.id.tvMes);
        btnAnterior = view.findViewById(R.id.btnAnterior);
        btnSiguiente = view.findViewById(R.id.btnSiguiente);

        ImageButton btnMetricasSalud = view.findViewById(R.id.btnMetricasSalud);
        if (btnMetricasSalud != null) {
            btnMetricasSalud.setOnClickListener(v -> mostrarDialogoMetricasSalud());
        }

        btnAbrirBuscador = view.findViewById(R.id.btnAbrirBuscador);
        progressBarBusqueda = view.findViewById(R.id.progressBarBusqueda);

        layoutHeader = view.findViewById(R.id.layoutHeader);
        layoutBuscador = view.findViewById(R.id.layoutBuscador);
        etBuscador = view.findViewById(R.id.etBuscador);
        btnCerrarBuscador = view.findViewById(R.id.btnCerrarBuscador);
        progressBarBusqueda = view.findViewById(R.id.progressBarBusqueda);
        shimmerMantenimientos = view.findViewById(R.id.shimmerMantenimientos);

        calendarioActual = Calendar.getInstance();
        actualizarTextoMes();

        cargarMantenimientos(false);

        btnAnterior.setOnClickListener(v -> {
            if (!isSearchMode) {
                calendarioActual.add(Calendar.MONTH, -1);
                actualizarTextoMes();
                aplicarFiltroPorMes(calendarioActual.get(Calendar.YEAR), calendarioActual.get(Calendar.MONTH));
            }
        });
        btnSiguiente.setOnClickListener(v -> {
            if (!isSearchMode) {
                calendarioActual.add(Calendar.MONTH, 1);
                actualizarTextoMes();
                aplicarFiltroPorMes(calendarioActual.get(Calendar.YEAR), calendarioActual.get(Calendar.MONTH));
            }
        });
        tvMes.setOnClickListener(v -> {
            if (!isSearchMode) mostrarSelectorDeMes();
        });

        btnAbrirBuscador.setOnClickListener(v -> activarModoBusqueda());

        btnCerrarBuscador.setOnClickListener(v -> {
            etBuscador.setText("");
            desactivarModoBusqueda();
        });

        etBuscador.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (adapter != null) {
                    adapter.getFilter().filter(s.toString());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new MantenimientosAdapter(getContext(), mantenimientosList, itemsConfigList,
                mantenimiento -> abrirDetalleMantenimiento(mantenimiento));

        recyclerView.setAdapter(adapter);

        // Configurar Swipe to Delete
        setupSwipeToDelete();
    }

    private void setupSwipeToDelete() {

        ItemTouchHelper.SimpleCallback swipeCallback = new ItemTouchHelper.SimpleCallback(
                0,
                ItemTouchHelper.LEFT
        ) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                Mantenimiento mantenimiento = adapter.getMantenimientoAt(position);

                new MaterialAlertDialogBuilder(requireContext(), R.style.DialogoConFuenteAnta)
                        .setTitle("Eliminar mantenimiento")
                        .setMessage("¿Estás seguro de que deseas eliminar este mantenimiento?")
                        .setCancelable(false)
                        .setPositiveButton("Eliminar", (dialog, which) -> {
                            adapter.removerMantenimiento(position);
                            todosMantenimientosGrupo.remove(mantenimiento);
                            eliminarMantenimientoFirestore(mantenimiento);
                            Context ctx = getContext();
                            if (ctx != null && mantenimiento.getId() != null) {
                                com.example.maquirentapp.Utils.MantenimientoOfflineManager.eliminarPendiente(ctx, mantenimiento.getId());
                            }

                            Toast.makeText(requireContext(),
                                    "Mantenimiento eliminado",
                                    Toast.LENGTH_SHORT).show();

                            actualizarUI();
                        })
                        .setNegativeButton("Cancelar", (dialog, which) -> {
                            adapter.notifyItemChanged(position);
                        })
                        .show();
            }

            @Override
            public void onChildDraw(@NonNull Canvas c,
                                    @NonNull RecyclerView recyclerView,
                                    @NonNull RecyclerView.ViewHolder viewHolder,
                                    float dX, float dY,
                                    int actionState,
                                    boolean isCurrentlyActive) {

                new RecyclerViewSwipeDecorator.Builder(
                        c,
                        recyclerView,
                        viewHolder,
                        dX,
                        dY,
                        actionState,
                        isCurrentlyActive
                )
                        .addBackgroundColor(ContextCompat.getColor(requireContext(), R.color.red_accent))
                        .addActionIcon(R.drawable.icon_eliminar_rojo)
                        .setActionIconTint(R.color.white)
                        .addCornerRadius(TypedValue.COMPLEX_UNIT_DIP, 30)
                        .create()
                        .decorate();

                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }
        };

        new ItemTouchHelper(swipeCallback).attachToRecyclerView(recyclerView);
    }


    private void activarModoBusqueda() {
        isSearchMode = true;

        layoutHeader.setVisibility(View.GONE);
        layoutBuscador.setVisibility(View.VISIBLE);

        etBuscador.requestFocus();
        InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(etBuscador, InputMethodManager.SHOW_IMPLICIT);
        }

        mantenimientosList.clear();
        mantenimientosList.addAll(todosMantenimientosGrupo);
        actualizarUI();
    }

    private void desactivarModoBusqueda() {
        isSearchMode = false;

        InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(etBuscador.getWindowToken(), 0);
        }

        layoutBuscador.setVisibility(View.GONE);
        layoutHeader.setVisibility(View.VISIBLE);

        aplicarFiltroPorMes(calendarioActual.get(Calendar.YEAR), calendarioActual.get(Calendar.MONTH));
    }

    private void actualizarTextoMes() {
        SimpleDateFormat formatoMes = new SimpleDateFormat("MMMM", new Locale("es", "ES"));
        SimpleDateFormat formatoAño = new SimpleDateFormat("yyyy", new Locale("es", "ES"));

        String mes = formatoMes.format(calendarioActual.getTime());
        String año = formatoAño.format(calendarioActual.getTime());
        mes = mes.substring(0, 1).toUpperCase() + mes.substring(1);
        tvMes.setText(mes + "\n" + año);
    }

    private void mostrarSelectorDeMes() {
        final String[] meses = {"Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
                "Julio", "Agosto", "Setiembre", "Octubre", "Noviembre", "Diciembre"};

        Calendar calendario = Calendar.getInstance();
        int añoActual = calendario.get(Calendar.YEAR);
        int mesActual = calendario.get(Calendar.MONTH);

        NumberPicker yearPicker = new NumberPicker(requireContext());
        yearPicker.setMinValue(añoActual - 30);
        yearPicker.setMaxValue(añoActual + 30);
        yearPicker.setValue(calendarioActual.get(Calendar.YEAR));

        NumberPicker monthPicker = new NumberPicker(requireContext());
        monthPicker.setMinValue(0);
        monthPicker.setMaxValue(meses.length - 1);
        monthPicker.setDisplayedValues(meses);
        monthPicker.setValue(calendarioActual.get(Calendar.MONTH));

        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.addView(yearPicker);
        layout.addView(monthPicker);
        layout.setPadding(250, 30, 100, 10);

        new MaterialAlertDialogBuilder(requireContext(), R.style.DialogoConFuenteAnta)
                .setTitle("Seleccionar mes y año")
                .setView(layout)
                .setPositiveButton("Aceptar", (dialog, which) -> {
                    calendarioActual.set(Calendar.YEAR, yearPicker.getValue());
                    calendarioActual.set(Calendar.MONTH, monthPicker.getValue());
                    actualizarTextoMes();
                    aplicarFiltroPorMes(calendarioActual.get(Calendar.YEAR), calendarioActual.get(Calendar.MONTH));
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void aplicarFiltroPorMes(int añoSeleccionado, int mesSeleccionado) {
        mantenimientosList.clear();
        for (Mantenimiento m : todosMantenimientosGrupo) {
            if (m.perteneceAlMes(añoSeleccionado, mesSeleccionado)) {
                mantenimientosList.add(m);
            }
        }
        actualizarUI();
    }

    private void cargarMantenimientos(boolean forzarRecarga) {
        if (!forzarRecarga && !todosMantenimientosGrupo.isEmpty()) {
            aplicarFiltroPorMes(calendarioActual.get(Calendar.YEAR), calendarioActual.get(Calendar.MONTH));
            return;
        }

        if (shimmerMantenimientos != null) {
            shimmerMantenimientos.setVisibility(View.VISIBLE);
            shimmerMantenimientos.startShimmer();
        }
        recyclerView.setVisibility(View.GONE);
        emptyState.setVisibility(View.GONE);

        boolean online = NetworkUtils.isNetworkAvailable(getContext());
        Source source = online ? Source.DEFAULT : Source.CACHE;

        db.collection("mantenimientos")
                .whereEqualTo("codigoGrupo", codigo)
                .get(source)
                .addOnSuccessListener(querySnapshot -> {
                    if (shimmerMantenimientos != null) {
                        shimmerMantenimientos.stopShimmer();
                        shimmerMantenimientos.setVisibility(View.GONE);
                    }
                    todosMantenimientosGrupo.clear();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Mantenimiento m = doc.toObject(Mantenimiento.class);
                        m.setId(doc.getId());
                        todosMantenimientosGrupo.add(m);
                    }

                    // Integrar mantenimientos guardados localmente en modo offline
                    Context ctx = getContext();
                    if (ctx != null) {
                        List<Mantenimiento> pendientes = com.example.maquirentapp.Utils.MantenimientoOfflineManager.obtenerPendientesPorGrupo(ctx, codigo);
                        for (Mantenimiento p : pendientes) {
                            boolean yaExiste = false;
                            for (int i = 0; i < todosMantenimientosGrupo.size(); i++) {
                                if (todosMantenimientosGrupo.get(i).getId() != null && todosMantenimientosGrupo.get(i).getId().equals(p.getId())) {
                                    todosMantenimientosGrupo.set(i, p);
                                    yaExiste = true;
                                    break;
                                }
                            }
                            if (!yaExiste) {
                                todosMantenimientosGrupo.add(p);
                            }
                        }
                    }

                    // Ordenar todo el historial por fecha de mantenimiento descendente
                    Collections.sort(todosMantenimientosGrupo, (a, b) -> {
                        int comp = Long.compare(b.getFechaMantenimientoMillis(), a.getFechaMantenimientoMillis());
                        if (comp != 0) return comp;
                        return Long.compare(b.getFechaCreacion(), a.getFechaCreacion());
                    });

                    if (isSearchMode) {
                        mantenimientosList.clear();
                        mantenimientosList.addAll(todosMantenimientosGrupo);
                        actualizarUI();
                    } else {
                        aplicarFiltroPorMes(calendarioActual.get(Calendar.YEAR), calendarioActual.get(Calendar.MONTH));
                    }
                })
                .addOnFailureListener(e -> {
                    if (shimmerMantenimientos != null) {
                        shimmerMantenimientos.stopShimmer();
                        shimmerMantenimientos.setVisibility(View.GONE);
                    }
                    Log.e(TAG, "Error cargando mantenimientos", e);

                    // Si falla Firestore, cargar de pendientes offline
                    Context ctx = getContext();
                    if (ctx != null) {
                        todosMantenimientosGrupo.clear();
                        List<Mantenimiento> pendientes = com.example.maquirentapp.Utils.MantenimientoOfflineManager.obtenerPendientesPorGrupo(ctx, codigo);
                        todosMantenimientosGrupo.addAll(pendientes);
                        Collections.sort(todosMantenimientosGrupo, (a, b) -> Long.compare(b.getFechaMantenimientoMillis(), a.getFechaMantenimientoMillis()));
                    }

                    if (isSearchMode) {
                        mantenimientosList.clear();
                        mantenimientosList.addAll(todosMantenimientosGrupo);
                        actualizarUI();
                    } else {
                        aplicarFiltroPorMes(calendarioActual.get(Calendar.YEAR), calendarioActual.get(Calendar.MONTH));
                    }
                });
    }

    private void cargarItemsConfiguracion() {
        Context ctx = getContext();
        if (ctx != null) {
            List<MantenimientoConfiguracion> backup = com.example.maquirentapp.Utils.MantenimientoOfflineManager.obtenerConfiguracionBackup(ctx);
            if (!backup.isEmpty()) {
                itemsConfigList.clear();
                itemsConfigList.addAll(backup);
                if (adapter != null) adapter.actualizarItemsConfig(itemsConfigList);
            }
        }

        boolean online = NetworkUtils.isNetworkAvailable(getContext());
        Source source = online ? Source.DEFAULT : Source.CACHE;

        db.collection("mantenimientos_configuracion")
                .get(source)
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    itemsConfigList.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        MantenimientoConfiguracion item = doc.toObject(MantenimientoConfiguracion.class);
                        item.setId(doc.getId());
                        itemsConfigList.add(item);
                    }
                    Collections.sort(itemsConfigList, (a, b) -> Long.compare(a.getFechaCreacion(), b.getFechaCreacion()));
                    if (adapter != null) adapter.actualizarItemsConfig(itemsConfigList);
                    if (ctx != null) com.example.maquirentapp.Utils.MantenimientoOfflineManager.guardarConfiguracionBackup(ctx, itemsConfigList);
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error cargando items configuración", e));
    }

    private void actualizarUI() {
        if (mantenimientosList.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyState.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            adapter.actualizarLista(mantenimientosList);
        }
    }

    private void abrirDetalleMantenimiento(Mantenimiento mantenimiento) {
        Bundle args = new Bundle();
        if (codigo != null) args.putString("codigo", codigo);
        if (mantenimiento != null && mantenimiento.getId() != null) {
            args.putString("mantenimiento_id", mantenimiento.getId());
        }

        if (getView() != null) {
            try {
                androidx.navigation.Navigation.findNavController(getView())
                        .navigate(R.id.action_mantenimientos_to_nuevoMantenimiento, args);
                return;
            } catch (Exception ignored) {}
            try {
                androidx.navigation.Navigation.findNavController(getView())
                        .navigate(R.id.nuevoMantenimientoFragment, args);
                return;
            } catch (Exception ignored) {}
        }

        NuevoMantenimientoFragment fragment = NuevoMantenimientoFragment.newInstance(codigo, mantenimiento != null ? mantenimiento.getId() : null);
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.nav_host_fragment, fragment)
                .addToBackStack(null)
                .commit();
    }

    private void eliminarMantenimientoFirestore(Mantenimiento mantenimiento) {
        if (mantenimiento.getFotos() != null && !mantenimiento.getFotos().isEmpty()) {
            for (String fotoUrl : mantenimiento.getFotos()) {
                try {
                    StorageReference fotoRef = storage.getReferenceFromUrl(fotoUrl);
                    fotoRef.delete()
                            .addOnSuccessListener(aVoid -> Log.d(TAG, "Foto eliminada"))
                            .addOnFailureListener(e -> Log.e(TAG, "Error eliminando foto", e));
                } catch (Exception e) {
                    Log.e(TAG, "Error procesando URL foto", e);
                }
            }
        }

        db.collection("mantenimientos").document(mantenimiento.getId())
                .delete()
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Mantenimiento eliminado de Firestore"))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error eliminando de Firestore", e);
                    Toast.makeText(getContext(), "Error al eliminar", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onResume() {
        super.onResume();

        if (getActivity() instanceof com.example.maquirentapp.MainActivity) {
            com.example.maquirentapp.MainActivity main = (com.example.maquirentapp.MainActivity) getActivity();
            main.updateHeaderTitle("Mantenimientos\n"+codigo);
            main.updateHeaderIcon(R.drawable.icon_mantenimiento_blanco);
        }

        configureGlobalFab();
        cargarMantenimientos(true);

        Context ctx = getContext();
        if (ctx != null && NetworkUtils.isNetworkAvailable(ctx)) {
            com.example.maquirentapp.Utils.MantenimientoOfflineManager.sincronizarPendientes(ctx, new com.example.maquirentapp.Repository.Callback<Integer>() {
                @Override
                public void onSuccess(Integer count) {
                    if (count > 0 && isAdded()) {
                        cargarMantenimientos(true);
                        Toast.makeText(getContext(), "Se sincronizaron " + count + " mantenimientos pendientes con la nube", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onError(Exception e) {
                    Log.w(TAG, "Error en sincronización automática", e);
                }
            });
        }
    }

    private void configureGlobalFab() {
        if (getActivity() instanceof com.example.maquirentapp.MainActivity) {
            com.example.maquirentapp.MainActivity main = (com.example.maquirentapp.MainActivity) getActivity();
            main.showGlobalFab("Añadir", R.drawable.icon_nuevo_blanco, v -> abrirNuevoMantenimiento());
        }
    }

    private void hideGlobalFab() {
        if (getActivity() instanceof com.example.maquirentapp.MainActivity) {
            ((com.example.maquirentapp.MainActivity) getActivity()).hideGlobalFab();
        }
    }

    private void abrirNuevoMantenimiento() {
        abrirDetalleMantenimiento(null);
    }

    private void mostrarDialogoMetricasSalud() {
        if (getContext() == null || codigo == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_metricas_salud_mantenimiento, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextView tvTitulo = dialogView.findViewById(R.id.tvTituloMetricas);
        TextView tvSubtitulo = dialogView.findViewById(R.id.tvSubtituloMetricas);
        TextView tvValorPuntualidad = dialogView.findViewById(R.id.tvValorPuntualidad);
        TextView tvValorIntervaloPromedio = dialogView.findViewById(R.id.tvValorIntervaloPromedio);
        TextView tvDiagnostico = dialogView.findViewById(R.id.tvDiagnosticoSalud);
        LinearLayout layoutBarras = dialogView.findViewById(R.id.layoutBarrasIntervalos);
        Button btnCerrar = dialogView.findViewById(R.id.btnCerrarMetricas);

        tvTitulo.setText("Salud & Disciplina: " + codigo);
        tvSubtitulo.setText("Analizando historial de mantenimientos...");

        btnCerrar.setOnClickListener(v -> dialog.dismiss());
        dialog.show();

        db.collection("mantenimientos")
                .whereEqualTo("codigoGrupo", codigo)
                .get()
                .addOnSuccessListener(query -> {
                    List<Mantenimiento> todosM = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : query) {
                        Mantenimiento m = doc.toObject(Mantenimiento.class);
                        m.setId(doc.getId());
                        todosM.add(m);
                    }

                    MantenimientoSaludManager.MetricasPuntualidad metricas =
                            MantenimientoSaludManager.calcularMetricasPuntualidad(todosM);

                    tvSubtitulo.setText(String.format(Locale.US, "Historial de %d mantenimientos evaluados", todosM.size()));
                    tvValorPuntualidad.setText(String.format(Locale.US, "%.0f%%", metricas.indicePuntualidad));

                    if (metricas.indicePuntualidad >= 85) {
                        tvValorPuntualidad.setTextColor(Color.parseColor("#4CAF50"));
                    } else if (metricas.indicePuntualidad >= 60) {
                        tvValorPuntualidad.setTextColor(Color.parseColor("#FFC107"));
                    } else {
                        tvValorPuntualidad.setTextColor(Color.parseColor("#F44336"));
                    }

                    tvValorIntervaloPromedio.setText(String.format(Locale.US, "%.1f h", metricas.intervaloPromedio));
                    tvDiagnostico.setText(metricas.diagnostico);

                    layoutBarras.removeAllViews();
                    if (metricas.intervalos.isEmpty()) {
                        TextView tvVacio = new TextView(getContext());
                        tvVacio.setText("Aún no hay suficientes mantenimientos para calcular intervalos.");
                        tvVacio.setTextColor(Color.GRAY);
                        tvVacio.setTextSize(12);
                        layoutBarras.addView(tvVacio);
                    } else {
                        for (int i = 0; i < metricas.intervalos.size(); i++) {
                            MantenimientoSaludManager.PuntoHistorialIntervalo p = metricas.intervalos.get(i);
                            LinearLayout itemFila = new LinearLayout(getContext());
                            itemFila.setOrientation(LinearLayout.VERTICAL);
                            itemFila.setPadding(0, 10, 0, 10);

                            TextView tvCabecera = new TextView(getContext());
                            String estadoStr = p.esATiempo ? "[A TIEMPO]" : String.format(Locale.US, "[+%.0f h exceso]", p.deltaHoras - 250);
                            tvCabecera.setText(String.format(Locale.US, "Servicio #%d al horómetro %.1f h  %s", i + 1, p.horometro, estadoStr));
                            tvCabecera.setTextColor(Color.WHITE);
                            tvCabecera.setTextSize(12);
                            tvCabecera.setTypeface(null, android.graphics.Typeface.BOLD);

                            TextView tvSub = new TextView(getContext());
                            tvSub.setText(String.format(Locale.US, "Fecha: %s | Intervalo: %.1f horas", p.fecha, p.deltaHoras));
                            tvSub.setTextColor(Color.parseColor("#B0BEC5"));
                            tvSub.setTextSize(11);

                            ProgressBar barra = new ProgressBar(getContext(), null, android.R.attr.progressBarStyleHorizontal);
                            barra.setMax(400);
                            barra.setProgress((int) Math.min(400, p.deltaHoras));
                            barra.getProgressDrawable().setColorFilter(
                                    Color.parseColor(p.esATiempo ? "#4CAF50" : "#F44336"),
                                    android.graphics.PorterDuff.Mode.SRC_IN
                            );

                            itemFila.addView(tvCabecera);
                            itemFila.addView(tvSub);
                            itemFila.addView(barra);
                            layoutBarras.addView(itemFila);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    tvSubtitulo.setText("Error al cargar historial: " + e.getMessage());
                });
    }
}
