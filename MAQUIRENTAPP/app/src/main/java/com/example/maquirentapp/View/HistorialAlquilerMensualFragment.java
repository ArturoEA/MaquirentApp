package com.example.maquirentapp.View;

import android.graphics.Canvas;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
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
import com.example.maquirentapp.Repository.AlquilerRepository;
import com.example.maquirentapp.Repository.Callback;
import com.example.maquirentapp.R;
import com.example.maquirentapp.adapters.AlquilerMensualAdapter;
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

import it.xabaras.android.recyclerview.swipedecorator.RecyclerViewSwipeDecorator;

public class HistorialAlquilerMensualFragment extends Fragment {
    private RecyclerView recyclerView;
    private AlquilerMensualAdapter adapter;
    private AlquilerRepository alquilerRepository;
    private String codigo, idGrupo;
    private AlquilerMensual alquilerSeleccionado;
    private TextView tvAnio;
    private ImageButton btnAnterior, btnSiguiente;
    private LinearLayout emptyState;
    private com.facebook.shimmer.ShimmerFrameLayout shimmerAlquileres;

    private Calendar calendarioActual;
    private List<AlquilerMensual> todosLosAlquileres = new ArrayList<>();
    private String[] nombresMeses;

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
        nombresMeses = getResources().getStringArray(R.array.months_array);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_historial_alquiler_mensual, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        alquilerRepository = new AlquilerRepository();

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
        shimmerAlquileres = view.findViewById(R.id.shimmerAlquileres);

        // Configurar mes y año actual
        actualizarTextoMesAnio();

        // Configurar listeners
        btnAnterior.setOnClickListener(v -> {
            calendarioActual.add(Calendar.MONTH, -1);
            actualizarTextoMesAnio();
            filtrarAlquileresPorMesYAnio();
        });

        btnSiguiente.setOnClickListener(v -> {
            calendarioActual.add(Calendar.MONTH, 1);
            actualizarTextoMesAnio();
            filtrarAlquileresPorMesYAnio();
        });

        tvAnio.setOnClickListener(v -> mostrarDialogoMesAnio());
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

    private void actualizarTextoMesAnio() {
        if (nombresMeses == null && getContext() != null) {
            nombresMeses = getResources().getStringArray(R.array.months_array);
        }
        int mes = calendarioActual.get(Calendar.MONTH);
        int año = calendarioActual.get(Calendar.YEAR);
        String nombreMes = (nombresMeses != null && mes >= 0 && mes < nombresMeses.length) ? nombresMeses[mes] : "Mes";
        String texto = String.format(Locale.getDefault(), "%s\n%d", nombreMes, año);
        tvAnio.setText(texto);
    }

    private void mostrarDialogoMesAnio() {
        if (getContext() == null) return;
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_month_year_picker, null);

        NumberPicker pickerMes = dialogView.findViewById(R.id.picker_mes);
        NumberPicker pickerAnio = dialogView.findViewById(R.id.picker_anio);

        if (nombresMeses == null) {
            nombresMeses = getResources().getStringArray(R.array.months_array);
        }

        pickerMes.setMinValue(0);
        pickerMes.setMaxValue(11);
        pickerMes.setDisplayedValues(nombresMeses);
        pickerMes.setValue(calendarioActual.get(Calendar.MONTH));

        pickerAnio.setMinValue(2020);
        pickerAnio.setMaxValue(2050);
        pickerAnio.setValue(calendarioActual.get(Calendar.YEAR));

        new MaterialAlertDialogBuilder(getContext(), R.style.DialogoConFuenteAnta)
                .setTitle("Seleccionar Mes y Año")
                .setView(dialogView)
                .setPositiveButton("Aceptar", (dialog, which) -> {
                    calendarioActual.set(Calendar.MONTH, pickerMes.getValue());
                    calendarioActual.set(Calendar.YEAR, pickerAnio.getValue());
                    actualizarTextoMesAnio();
                    filtrarAlquileresPorMesYAnio();
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
        if (fecha == null || fecha.trim().isEmpty()) {
            return -1;
        }
        try {
            Pattern pattern = Pattern.compile("\\b(19\\d{2}|20\\d{2})\\b");
            Matcher matcher = pattern.matcher(fecha.trim());
            if (matcher.find()) {
                return Integer.parseInt(matcher.group(1));
            }
        } catch (Exception e) {
            Log.e("FechaError", "Error al extraer año de: '" + fecha + "'", e);
        }
        return -1;
    }

    private void fetchAlquileresMensuales() {
        if (shimmerAlquileres != null) {
            shimmerAlquileres.setVisibility(View.VISIBLE);
            shimmerAlquileres.startShimmer();
        }
        if (recyclerView != null) {
            recyclerView.setVisibility(View.GONE);
        }
        if (emptyState != null) {
            emptyState.setVisibility(View.GONE);
        }
        alquilerRepository.getAlquileresMensuales(new Callback<List<AlquilerMensual>>() {
            @Override
            public void onSuccess(List<AlquilerMensual> alquileres) {
                List<AlquilerMensual> filtradosPorGrupo = new ArrayList<>();
                String idFiltro = (idGrupo != null) ? idGrupo.trim() : "";
                String codFiltro = (codigo != null) ? codigo.trim() : "";

                for (AlquilerMensual a : alquileres) {
                    if (a == null) continue;
                    String gId = (a.getIdGrupo() != null) ? a.getIdGrupo().trim() : "";

                    if (idFiltro.isEmpty() && codFiltro.isEmpty()) {
                        filtradosPorGrupo.add(a);
                        continue;
                    }

                    boolean coincideId = !idFiltro.isEmpty() && gId.equalsIgnoreCase(idFiltro);
                    boolean coincideCodigo = !codFiltro.isEmpty() && gId.equalsIgnoreCase(codFiltro);

                    if (coincideId || coincideCodigo) {
                        filtradosPorGrupo.add(a);
                    }
                }

                todosLosAlquileres = filtradosPorGrupo;
                filtrarAlquileresPorMesYAnio();
            }

            @Override
            public void onError(Exception e) {
                if (shimmerAlquileres != null) {
                    shimmerAlquileres.stopShimmer();
                    shimmerAlquileres.setVisibility(View.GONE);
                }
                Log.e("FetchAlquileres", "Error: " + e.getMessage());
                if (getContext() != null) {
                    Toast.makeText(getContext(),
                            "Error al cargar alquileres: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                }
                actualizarUI(new ArrayList<>());
            }
        });
    }

    private void filtrarAlquileresPorMesYAnio() {
        List<AlquilerMensual> alquileresFiltrados = new ArrayList<>();
        int mesSeleccionado = calendarioActual.get(Calendar.MONTH);
        int añoSeleccionado = calendarioActual.get(Calendar.YEAR);

        Calendar cStart = Calendar.getInstance();
        cStart.set(añoSeleccionado, mesSeleccionado, 1, 0, 0, 0);
        cStart.set(Calendar.MILLISECOND, 0);
        Date mesInicioDate = cStart.getTime();

        Calendar cEnd = Calendar.getInstance();
        cEnd.set(añoSeleccionado, mesSeleccionado, cStart.getActualMaximum(Calendar.DAY_OF_MONTH), 23, 59, 59);
        cEnd.set(Calendar.MILLISECOND, 999);
        Date mesFinDate = cEnd.getTime();

        for (AlquilerMensual alquiler : todosLosAlquileres) {
            Date dInicio = parsearFechaGeneral(alquiler.getFechaInicial());
            Date dFin = parsearFechaGeneral(alquiler.getFechaFinal());

            boolean coincide = false;
            if (dInicio != null && dFin != null) {
                if (!dInicio.after(mesFinDate) && !dFin.before(mesInicioDate)) {
                    coincide = true;
                }
            } else if (dInicio != null) {
                if (!alquiler.isFinalizado()) {
                    if (!dInicio.after(mesFinDate)) {
                        coincide = true;
                    }
                } else {
                    Calendar c = Calendar.getInstance();
                    c.setTime(dInicio);
                    if (c.get(Calendar.MONTH) == mesSeleccionado && c.get(Calendar.YEAR) == añoSeleccionado) {
                        coincide = true;
                    }
                }
            } else if (alquiler.getTimestampCreacion() > 0) {
                Calendar c = Calendar.getInstance();
                c.setTimeInMillis(alquiler.getTimestampCreacion());
                if (c.get(Calendar.MONTH) == mesSeleccionado && c.get(Calendar.YEAR) == añoSeleccionado) {
                    coincide = true;
                }
            }

            if (coincide) {
                alquileresFiltrados.add(alquiler);
            }
        }

        actualizarUI(alquileresFiltrados);
    }

    private Date parsearFechaGeneral(String fechaStr) {
        if (fechaStr == null || fechaStr.trim().isEmpty()) return null;
        String[] formatos = {
                "dd/MM/yyyy",
                "yyyy-MM-dd'T'HH:mm:ss",
                "yyyy-MM-dd",
                "dd-MM-yyyy"
        };
        for (String fmt : formatos) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(fmt, Locale.US);
                sdf.setLenient(false);
                return sdf.parse(fechaStr.trim());
            } catch (Exception ignored) {}
        }
        try {
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d{1,2})[/.-](\\d{1,2})[/.-](\\d{4})").matcher(fechaStr);
            if (m.find()) {
                int d = Integer.parseInt(m.group(1));
                int mth = Integer.parseInt(m.group(2)) - 1;
                int y = Integer.parseInt(m.group(3));
                Calendar c = Calendar.getInstance();
                c.set(y, mth, d, 0, 0, 0);
                return c.getTime();
            }
        } catch (Exception ignored) {}
        return null;
    }

    private void actualizarUI(List<AlquilerMensual> alquileres) {
        if (shimmerAlquileres != null) {
            shimmerAlquileres.stopShimmer();
            shimmerAlquileres.setVisibility(View.GONE);
        }
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
        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(
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
                AlquilerMensual alquilerAEliminar = adapter.getItem(position);

                new MaterialAlertDialogBuilder(requireContext(), R.style.DialogoConFuenteAnta)
                        .setTitle("Eliminar alquiler")
                        .setMessage("¿Estás seguro de que deseas eliminar este alquiler?")
                        .setCancelable(false)
                        .setPositiveButton("Eliminar", (dialog, which) -> {
                            alquilerRepository.eliminarAlquilerMensual(alquilerAEliminar.getId(),
                                    new Callback<Void>() {
                                        @Override
                                        public void onSuccess(Void result) {
                                            if (getContext() == null) return;
                                            Toast.makeText(getContext(),
                                                    "Alquiler eliminado correctamente",
                                                    Toast.LENGTH_SHORT).show();
                                            fetchAlquileresMensuales();
                                        }

                                        @Override
                                        public void onError(Exception e) {
                                            if (getContext() == null) return;
                                            Toast.makeText(getContext(),
                                                    "Error al eliminar: " + e.getMessage(),
                                                    Toast.LENGTH_LONG).show();
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

                super.onChildDraw(c, recyclerView, viewHolder,
                        dX, dY, actionState, isCurrentlyActive);
            }
        };

        new ItemTouchHelper(simpleCallback).attachToRecyclerView(recyclerView);
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
