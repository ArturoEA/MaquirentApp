package com.example.maquirentapp.View;

import android.app.DatePickerDialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.DatePicker;
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
import com.example.maquirentapp.Access.MantenimientosAdapter;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointForward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

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
    private List<Mantenimiento> mantenimientosList = new ArrayList<>();
    private List<MantenimientoConfiguracion> itemsConfigList = new ArrayList<>();
    private ImageButton btnAbrirBuscador, btnCerrarBuscador;
    private View layoutHeader;
    private View layoutBuscador;
    private TextInputEditText etBuscador;
    private ProgressBar progressBarBusqueda;
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

        btnAbrirBuscador = view.findViewById(R.id.btnAbrirBuscador);
        progressBarBusqueda = view.findViewById(R.id.progressBarBusqueda);

        layoutHeader = view.findViewById(R.id.layoutHeader);
        layoutBuscador = view.findViewById(R.id.layoutBuscador);
        etBuscador = view.findViewById(R.id.etBuscador);
        btnAbrirBuscador = view.findViewById(R.id.btnAbrirBuscador);
        btnCerrarBuscador = view.findViewById(R.id.btnCerrarBuscador);
        progressBarBusqueda = view.findViewById(R.id.progressBarBusqueda);

        calendarioActual = Calendar.getInstance();
        actualizarTextoMes();

        cargarMantenimientosDelMes(
                calendarioActual.get(Calendar.YEAR),
                calendarioActual.get(Calendar.MONTH)
        );

        btnAnterior.setOnClickListener(v -> {
            if (!isSearchMode) {
                calendarioActual.add(Calendar.MONTH, -1);
                actualizarTextoMes();
                cargarMantenimientosDelMes(calendarioActual.get(Calendar.YEAR), calendarioActual.get(Calendar.MONTH));
            }
        });
        btnSiguiente.setOnClickListener(v -> {
            if (!isSearchMode) {
                calendarioActual.add(Calendar.MONTH, 1);
                actualizarTextoMes();
                cargarMantenimientosDelMes(calendarioActual.get(Calendar.YEAR), calendarioActual.get(Calendar.MONTH));
            }
        });
        tvMes.setOnClickListener(v -> {
            if (!isSearchMode) mostrarSelectorDeMes();
        });

        btnAbrirBuscador.setOnClickListener(v -> activarModoBusqueda());

        btnAbrirBuscador.setOnClickListener(v -> activarModoBusqueda());

        btnCerrarBuscador.setOnClickListener(v -> {
            etBuscador.setText("");
            desactivarModoBusqueda();
        });

        etBuscador.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (adapter != null) {
                    adapter.getFilter().filter(s.toString());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
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
        ItemTouchHelper.SimpleCallback swipeCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            private final ColorDrawable background = new ColorDrawable(Color.RED);
            private Drawable deleteIcon;

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                Mantenimiento mantenimiento = adapter.getMantenimientoAt(position);

                // Remover del adapter
                adapter.removerMantenimiento(position);

                // Mostrar Snackbar con opción de deshacer
                Snackbar.make(recyclerView, "Mantenimiento eliminado", Snackbar.LENGTH_LONG)
                        .setAction("DESHACER", v -> adapter.restaurarMantenimiento(mantenimiento, position))
                        .addCallback(new Snackbar.Callback() {
                            @Override
                            public void onDismissed(Snackbar transientBottomBar, int event) {
                                if (event != DISMISS_EVENT_ACTION) {
                                    // Solo eliminar de Firestore si no se presionó deshacer
                                    eliminarMantenimientoFirestore(mantenimiento);
                                }
                            }
                        })
                        .show();

                actualizarUI();
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
                                    @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY,
                                    int actionState, boolean isCurrentlyActive) {
                View itemView = viewHolder.itemView;

                if (deleteIcon == null) {
                    deleteIcon = ContextCompat.getDrawable(requireContext(), android.R.drawable.ic_menu_delete);
                    if (deleteIcon != null) {
                        deleteIcon.setTint(Color.WHITE);
                    }
                }

                int iconMargin = (itemView.getHeight() - deleteIcon.getIntrinsicHeight()) / 2;
                int iconTop = itemView.getTop() + iconMargin;
                int iconBottom = iconTop + deleteIcon.getIntrinsicHeight();

                if (dX < 0) { // Swipe izquierda
                    int iconLeft = itemView.getRight() - iconMargin - deleteIcon.getIntrinsicWidth();
                    int iconRight = itemView.getRight() - iconMargin;
                    deleteIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom);

                    background.setBounds(
                            itemView.getRight() + (int) dX,
                            itemView.getTop(),
                            itemView.getRight(),
                            itemView.getBottom()
                    );
                } else {
                    background.setBounds(0, 0, 0, 0);
                }

                background.draw(c);
                deleteIcon.draw(c);

                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(swipeCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);
    }
    private void activarModoBusqueda() {
        isSearchMode = true;

        layoutHeader.setVisibility(View.GONE);
        layoutBuscador.setVisibility(View.VISIBLE);

        etBuscador.requestFocus();
        InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(etBuscador, InputMethodManager.SHOW_IMPLICIT);

        cargarTodoElHistorial();
    }

    private void desactivarModoBusqueda() {
        isSearchMode = false;

        InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(etBuscador.getWindowToken(), 0);

        layoutBuscador.setVisibility(View.GONE);
        layoutHeader.setVisibility(View.VISIBLE);

        cargarMantenimientosDelMes(calendarioActual.get(Calendar.YEAR), calendarioActual.get(Calendar.MONTH));
    }

    private void cargarTodoElHistorial() {
        progressBarBusqueda.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.INVISIBLE);
        emptyState.setVisibility(View.GONE);

        db.collection("mantenimientos")
                .whereEqualTo("codigoGrupo", codigo)
                .orderBy("fechaCreacion", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    mantenimientosList.clear();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Mantenimiento m = doc.toObject(Mantenimiento.class);
                        m.setId(doc.getId());
                        mantenimientosList.add(m);
                    }

                    progressBarBusqueda.setVisibility(View.GONE);
                    actualizarUI();

                    Toast.makeText(getContext(), "Historial completo cargado para búsqueda", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    progressBarBusqueda.setVisibility(View.GONE);
                    Log.e(TAG, "Error cargando historial completo", e);
                    Toast.makeText(getContext(), "Error al cargar historial", Toast.LENGTH_SHORT).show();
                    desactivarModoBusqueda();
                });
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
        yearPicker.setValue(añoActual);

        NumberPicker monthPicker = new NumberPicker(requireContext());
        monthPicker.setMinValue(0);
        monthPicker.setMaxValue(meses.length - 1);
        monthPicker.setDisplayedValues(meses);
        monthPicker.setValue(mesActual);

        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.addView(yearPicker);
        layout.addView(monthPicker);
        layout.setPadding(250, 30, 100, 10);

        new AlertDialog.Builder(requireContext())
                .setTitle("Seleccionar mes y año")
                .setView(layout)
                .setPositiveButton("Aceptar", (dialog, which) -> {
                    calendarioActual.set(Calendar.YEAR, yearPicker.getValue());
                    calendarioActual.set(Calendar.MONTH, monthPicker.getValue());
                    actualizarTextoMes();
                    cargarMantenimientosDelMes(calendarioActual.get(Calendar.YEAR), calendarioActual.get(Calendar.MONTH));
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void cargarMantenimientosDelMes(int añoSeleccionado, int mesSeleccionado) {
        Calendar inicioMes = Calendar.getInstance();
        inicioMes.set(añoSeleccionado, mesSeleccionado, 1, 0, 0, 0);
        inicioMes.set(Calendar.MILLISECOND, 0);
        long inicio = inicioMes.getTimeInMillis();

        Calendar finMes = Calendar.getInstance();
        finMes.set(añoSeleccionado, mesSeleccionado, finMes.getActualMaximum(Calendar.DAY_OF_MONTH), 23, 59, 59);
        finMes.set(Calendar.MILLISECOND, 999);
        long fin = finMes.getTimeInMillis();

        db.collection("mantenimientos")
                .whereEqualTo("codigoGrupo", codigo)
                .whereGreaterThanOrEqualTo("fechaCreacion", inicio)
                .whereLessThanOrEqualTo("fechaCreacion", fin)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    mantenimientosList.clear();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Mantenimiento m = doc.toObject(Mantenimiento.class);
                        m.setId(doc.getId());
                        mantenimientosList.add(m);
                    }
                    actualizarUI();
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error cargando mantenimientos del mes", e));
    }

    private void cargarItemsConfiguracion() {
        db.collection("mantenimientos_configuracion")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    itemsConfigList.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        MantenimientoConfiguracion item = doc.toObject(MantenimientoConfiguracion.class);
                        item.setId(doc.getId());
                        itemsConfigList.add(item);
                    }
                    adapter.actualizarItemsConfig(itemsConfigList);
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
        NuevoMantenimientoFragment fragment = NuevoMantenimientoFragment.newInstance(codigo, mantenimiento.getId());
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
            main.updateHeaderTitle(codigo);
        }

        configureGlobalFab();
        cargarMantenimientosDelMes(calendarioActual.get(Calendar.YEAR), calendarioActual.get(Calendar.MONTH));
    }

    @Override
    public void onPause() {
        super.onPause();
        hideGlobalFab();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        hideGlobalFab();
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
        NuevoMantenimientoFragment fragment = NuevoMantenimientoFragment.newInstance(codigo, null);
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.nav_host_fragment, fragment)
                .addToBackStack(null)
                .commit();
    }
}