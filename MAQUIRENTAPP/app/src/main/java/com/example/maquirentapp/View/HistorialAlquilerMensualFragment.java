package com.example.maquirentapp.View;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.net.ParseException;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.maquirentapp.MainActivity;
import com.example.maquirentapp.Model.AlquilerMensual;
import com.example.maquirentapp.Network.FirebaseServicio;
import com.example.maquirentapp.R;
import com.example.maquirentapp.adaptadores.AlquilerMensualAdapter;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import androidx.core.content.ContextCompat;

public class HistorialAlquilerMensualFragment extends Fragment {
    private RecyclerView recyclerView;
    private AlquilerMensualAdapter adapter;
    private FirebaseServicio firebaseServicio;
    private String codigo, idGrupo;
    private AlquilerMensual alquilerSeleccionado;
    private TextView tvAnio;
    private ImageButton btnAnterior, btnSiguiente;
    private LinearLayout emptyState;

    private Calendar calendarioActual;
    private List<AlquilerMensual> todosLosAlquileres = new ArrayList<>();

    public HistorialAlquilerMensualFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            codigo = getArguments().getString("codigo");
            idGrupo = getArguments().getString("idGrupo");
        }
        calendarioActual = Calendar.getInstance();
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

        // Inicializar vistas
        initViews(view);
        setupRecyclerView();
    }

    private void initViews(View view) {
        tvAnio = view.findViewById(R.id.tvAnio);
        btnAnterior = view.findViewById(R.id.btnAnterior);
        btnSiguiente = view.findViewById(R.id.btnSiguiente);
        emptyState = view.findViewById(R.id.emptyStateHistorialAlquilerMensual);
        recyclerView = view.findViewById(R.id.recyclerAlquileres);

        // Configurar año actual
        actualizarTextoAnio();

        // Configurar listeners
        btnAnterior.setOnClickListener(v -> {
            calendarioActual.add(Calendar.YEAR, -1);
            actualizarTextoAnio();
            filtrarAlquileresPorAnio();
        });

        btnSiguiente.setOnClickListener(v -> {
            calendarioActual.add(Calendar.YEAR, 1);
            actualizarTextoAnio();
            filtrarAlquileresPorAnio();
        });

        tvAnio.setOnClickListener(v -> mostrarSelectorDeAnio());
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new AlquilerMensualAdapter();
        adapter.setOnItemClickListener(alquiler -> {
            alquilerSeleccionado = alquiler;
            mostrarDetallesAlquiler(alquiler);
        });
        recyclerView.setAdapter(adapter);

        configurarSwipeToDelete();
    }

    private void actualizarTextoAnio() {
        int año = calendarioActual.get(Calendar.YEAR);
        tvAnio.setText(String.valueOf(año));
    }

    private void mostrarSelectorDeAnio() {
        int añoActual = Calendar.getInstance().get(Calendar.YEAR);
        int añoMin = añoActual - 30;
        int añoMax = añoActual + 30;

        NumberPicker yearPicker = new NumberPicker(requireContext());
        yearPicker.setMinValue(añoMin);
        yearPicker.setMaxValue(añoMax);
        yearPicker.setValue(calendarioActual.get(Calendar.YEAR));

        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.addView(yearPicker);
        layout.setPadding(100, 50, 100, 50);

        new AlertDialog.Builder(requireContext())
                .setTitle("Seleccionar año")
                .setView(layout)
                .setPositiveButton("Aceptar", (dialog, which) -> {
                    calendarioActual.set(Calendar.YEAR, yearPicker.getValue());
                    actualizarTextoAnio();
                    filtrarAlquileresPorAnio();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private List<Integer> obtenerAñosUnicos() {
        List<Integer> años = new ArrayList<>();
        for (AlquilerMensual alquiler : todosLosAlquileres) {
            int año = extraerAñoDeFecha(alquiler.getFechaInicial());
            if (año != -1 && !años.contains(año)) {
                años.add(año);
            }
        }

        // Ordenar años
        if (!años.isEmpty()) {
            años.sort((a, b) -> a - b);
        } else {
            años.add(Calendar.getInstance().get(Calendar.YEAR));
        }
        return años;
    }

    private int extraerAñoDeFecha(String fecha) {
        try {
            if (fecha == null || fecha.isEmpty()) {
                return -1;
            }

            fecha = fecha.trim();

            if (fecha.contains("/")) {
                String[] partes = fecha.split("/");
                if (partes.length == 3) {
                    return Integer.parseInt(partes[2]);
                }
            } else if (fecha.contains("-")) {
                String[] partes = fecha.split("-");
                if (partes.length == 3) {
                    if (partes[0].length() == 4) {
                        return Integer.parseInt(partes[0]);
                    } else if (partes[2].length() == 4) {
                        return Integer.parseInt(partes[2]);
                    }
                }
            } else if (fecha.contains("T")) {
                String[] partes = fecha.split("-");
                if (partes.length >= 1) {
                    return Integer.parseInt(partes[0]);
                }
            }
        } catch (Exception e) {
            Log.e("FechaError", "Error al extraer año de: '" + fecha + "'", e);
        }
        return -1;
    }

    private void fetchAlquileresMensuales() {
        firebaseServicio.getAlquileresMensuales(new FirebaseServicio.OnAlquileresLoadedListener() {
            @Override
            public void onSuccess(List<AlquilerMensual> alquileres) {
                List<AlquilerMensual> filtradosPorGrupo = new ArrayList<>();
                if (idGrupo != null && !idGrupo.isEmpty()) {
                    for (AlquilerMensual a : alquileres) {
                        if (a.getIdGrupo() != null && a.getIdGrupo().equals(idGrupo)) {
                            filtradosPorGrupo.add(a);
                        }
                    }
                } else if (codigo != null && !codigo.isEmpty()) {
                    for (AlquilerMensual a : alquileres) {
                        if (a.getIdGrupo() != null && a.getIdGrupo().equals(codigo)) {
                            filtradosPorGrupo.add(a);
                        }
                    }
                }

                todosLosAlquileres = filtradosPorGrupo;
                filtrarAlquileresPorAnio();
            }

            @Override
            public void onError(Exception e) {
                Log.e("FetchAlquileres", "Error: " + e.getMessage());
                Toast.makeText(getContext(),
                        "Error al cargar alquileres: " + e.getMessage(),
                        Toast.LENGTH_LONG).show();
                actualizarUI(new ArrayList<>());
            }
        });
    }

    private void filtrarAlquileresPorAnio() {
        List<AlquilerMensual> alquileresFiltrados = new ArrayList<>();
        int añoSeleccionado = calendarioActual.get(Calendar.YEAR);

        for (AlquilerMensual alquiler : todosLosAlquileres) {
            String fecha = alquiler.getFechaInicial();
            int añoAlquiler = extraerAñoDeFecha(fecha);

            if (añoAlquiler == añoSeleccionado) {
                alquileresFiltrados.add(alquiler);
            }
        }

        actualizarUI(alquileresFiltrados);
    }

    private void actualizarUI(List<AlquilerMensual> alquileres) {
        adapter.setItems(alquileres);

        if (alquileres.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyState.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
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
        if (getActivity() instanceof MainActivity) {
            MainActivity main = (MainActivity) getActivity();
            main.showGlobalFab(
                    "Añadir",
                    R.drawable.icon_nuevo_blanco,
                    v -> {
                        View hostView = getView();
                        if (hostView == null) return;

                        Bundle args = new Bundle();
                        if (codigo != null) args.putString("codigo", codigo);
                        if (idGrupo != null) args.putString("idGrupo", idGrupo);
                        Navigation.findNavController(hostView)
                                .navigate(R.id.action_historialAlquilerMensual_to_nuevoAlquilerMensual, args);
                    }
            );
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        alquilerSeleccionado = null;
        configureGlobalFab();
        fetchAlquileresMensuales();
    }
    @Override
    public void onPause() {
        super.onPause();
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).hideGlobalFab();
        }
    }
}