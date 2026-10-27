package com.example.maquirentapp.View;

import android.app.AlertDialog;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import it.xabaras.android.recyclerview.swipedecorator.RecyclerViewSwipeDecorator;

import com.example.maquirentapp.adapters.AccesorioSeleccionAdapter;
import com.example.maquirentapp.adapters.DetalleMesAdapter;
import com.example.maquirentapp.MainActivity; // Importar MainActivity
import com.example.maquirentapp.Model.Accesorio;
import com.example.maquirentapp.Model.AlquilerMensual;
import com.example.maquirentapp.Model.DetalleMes;
import com.example.maquirentapp.Model.GrupoElectrogeno;
import com.example.maquirentapp.Model.Ingreso;
import com.example.maquirentapp.Repository.AlquilerRepository;
import com.example.maquirentapp.Repository.Callback;
import com.example.maquirentapp.Repository.UsuarioRepository;
import com.example.maquirentapp.Utils.CustomDatePickerDialog;
import com.example.maquirentapp.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

public class NuevoAlquilerMensualFragment extends Fragment {
    private static final String TAG = "NuevoAlquilerMensual";

    private String idGrupo, alquilerId;
    private boolean modoEdicion = false;
    private boolean modoSoloLectura = false;
    private boolean editandoActualmente = false;

    private TextInputEditText inputEmpresa, inputUbicacion, inputFechaInicial, inputFechaFinal,
            inputHorometroInicial, inputHorometroFinal, inputPrecioAlquiler, inputHorasMinimas, inputPrecioHoraExtra;
    private Spinner spinnerMoneda;
    private TextView tvSimboloMoneda;
    private RecyclerView recyclerAccesorios, recyclerDetallesMes;
    private com.facebook.shimmer.ShimmerFrameLayout shimmerDetallesMes;
    private Button btnGenerarSiguienteMesManual, btnFinalizarAlquiler;
    private LinearLayout layoutSelectorAnios;
    private LinearLayout containerChipsAnios;
    private List<DetalleMes> listaTodosDetallesMes = new ArrayList<>();
    private String anioSeleccionado = null;

    private AccesorioSeleccionAdapter adapterAccesorios;
    private DetalleMesAdapter adapterDetallesMes;
    private AlquilerRepository alquilerRepository;
    private UsuarioRepository usuarioRepository;

    private View llAccesoriosHeader;
    private ImageView ivAccChevron;
    private View accBody;
    private boolean accesoriosExpanded = true;

    private String monedaSeleccionada = "SOL";
    private AlquilerMensual alquilerActual;
    private String codigo;
    private boolean isGenerandoSiguienteMes = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            idGrupo = getArguments().getString("idGrupo");
            alquilerId = getArguments().getString("alquilerId");
            codigo = getArguments().getString("codigo");
            modoSoloLectura = getArguments().getBoolean("modoSoloLectura", false);
            modoEdicion = alquilerId != null;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_nuevo_alquiler_mensual, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        alquilerRepository = new AlquilerRepository();
        usuarioRepository = new UsuarioRepository();

        inicializarVistas(view);
        configurarSpinnerMoneda();
        configurarDatePickers();
        configurarListeners();
        cargarAccesoriosMensuales();

        if (modoEdicion) {
            cargarDatosAlquiler();
        } else{
            inputHorasMinimas.setText("200");
        }

        if (modoSoloLectura) {
            deshabilitarCampos();
        }

        configureGlobalFab();
    }

    private void inicializarVistas(View view) {
        inputEmpresa = view.findViewById(R.id.inputEmpresa);
        inputUbicacion = view.findViewById(R.id.inputUbicacion);
        inputFechaInicial = view.findViewById(R.id.inputFechaInicial);
        inputFechaFinal = view.findViewById(R.id.inputFechaFinal);
        inputHorometroInicial = view.findViewById(R.id.inputHorometroInicial);
        inputHorometroFinal = view.findViewById(R.id.inputHorometroFinal);
        inputPrecioAlquiler = view.findViewById(R.id.inputPrecioAlquiler);
        inputHorasMinimas = view.findViewById(R.id.inputHorasMinimas);
        inputPrecioHoraExtra = view.findViewById(R.id.inputPrecioHoraExtra);
        spinnerMoneda = view.findViewById(R.id.spinnerMoneda);
        tvSimboloMoneda = view.findViewById(R.id.tvSimboloMoneda);
        btnFinalizarAlquiler = view.findViewById(R.id.btnFinalizarAlquiler);
        btnGenerarSiguienteMesManual = view.findViewById(R.id.btnGenerarSiguienteMesManual);
        if (btnGenerarSiguienteMesManual != null) {
            btnGenerarSiguienteMesManual.setOnClickListener(v -> mostrarDialogoGenerarSiguienteMesManual());
        }

        recyclerAccesorios = view.findViewById(R.id.recyclerAccesorios);
        recyclerAccesorios.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerAccesorios.setNestedScrollingEnabled(false);
        adapterAccesorios = new AccesorioSeleccionAdapter();
        recyclerAccesorios.setAdapter(adapterAccesorios);

        recyclerDetallesMes = view.findViewById(R.id.recyclerDetallesMes);
        shimmerDetallesMes = view.findViewById(R.id.shimmerDetallesMes);
        layoutSelectorAnios = view.findViewById(R.id.layoutSelectorAnios);
        containerChipsAnios = view.findViewById(R.id.containerChipsAnios);
        recyclerDetallesMes.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerDetallesMes.setNestedScrollingEnabled(false);
        adapterDetallesMes = new DetalleMesAdapter();
        recyclerDetallesMes.setAdapter(adapterDetallesMes);
        configurarSwipeToDelete();

        llAccesoriosHeader = view.findViewById(R.id.llAccesoriosHeader);
        ivAccChevron = view.findViewById(R.id.ivAccChevron);
        accBody = view.findViewById(R.id.accBody);

        accesoriosExpanded = !modoEdicion;
        setAccesoriosExpanded(accesoriosExpanded, false);

        llAccesoriosHeader.setOnClickListener(v -> {
            accesoriosExpanded = !accesoriosExpanded;
            setAccesoriosExpanded(accesoriosExpanded, true);
        });

        if (modoEdicion) {
            btnFinalizarAlquiler.setVisibility(View.VISIBLE);
            if (btnGenerarSiguienteMesManual != null) {
                btnGenerarSiguienteMesManual.setVisibility(View.VISIBLE);
            }
        }
    }

    private void configurarSpinnerMoneda() {
        List<String> monedas = new ArrayList<>();
        monedas.add("SOL");
        monedas.add("USD");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                R.layout.spinner_item_moneda,
                monedas
        );
        adapter.setDropDownViewResource(R.layout.spinner_item_moneda_dropdown);
        spinnerMoneda.setAdapter(adapter);

        spinnerMoneda.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                monedaSeleccionada = monedas.get(position);
                tvSimboloMoneda.setText(monedaSeleccionada.equals("USD") ? "$" : "S/.");
                String precioStr = inputPrecioAlquiler.getText().toString().trim();
                if (!precioStr.isEmpty()) {
                    calcularPrecioHoraExtra();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void configurarListeners() {
        inputPrecioAlquiler.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.toString().trim().length() > 0) {
                    calcularPrecioHoraExtra();
                }
            }
        });

        inputHorasMinimas.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                if (s.toString().trim().length() > 0) {
                    calcularPrecioHoraExtra();
                }
            }
        });

        adapterDetallesMes.setOnDetalleMesListener(new DetalleMesAdapter.OnDetalleMesListener() {
            @Override
            public void onHorometroCalculated(DetalleMes detalle, double nuevoHorometro, int position) {
                calcularHorasExtras(detalle, nuevoHorometro);
                adapterDetallesMes.notifyItemChanged(position, "CALCULATED_FIELDS");
            }
            @Override
            public void onHorometroFinalized(DetalleMes detalle) {
                actualizarDetalleMes(detalle);
            }
            @Override
            public void onHorometroChanged(DetalleMes detalle, double nuevoHorometro) {
                calcularHorasExtras(detalle, nuevoHorometro);
                int position = adapterDetallesMes.getItems().indexOf(detalle);
                if (position != -1) {
                    adapterDetallesMes.notifyItemChanged(position, "CALCULATED_FIELDS");
                }
                actualizarDetalleMes(detalle);
            }

            @Override
            public void onPagoMesConfirmado(DetalleMes detalle, int position) {
                actualizarDetalleMes(detalle);
                registrarIngresoProrrateado(
                        // Usar el monto específico del mes (prorrateado o full)
                        detalle.getMontoMes() > 0 ? detalle.getMontoMes() : alquilerActual.getPrecioAlquiler(),
                        "Alquiler Mensual",
                        detalle
                );
                Toast.makeText(getContext(), "Pago del mes confirmado", Toast.LENGTH_SHORT).show();
                verificarYGenerarSiguienteMes(detalle);
            }

            @Override
            public void onPagoHEConfirmado(DetalleMes detalle, int position) {
                actualizarDetalleMes(detalle);
                registrarIngresoProrrateado(
                        detalle.getPrecioHorasExtras(),
                        "Horas Extras",
                        detalle
                );
                Toast.makeText(getContext(), "Pago de horas extras confirmado", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFechaFinModificada(DetalleMes detalle) {
                calcularHorasExtras(detalle, detalle.getHorometro());

                int position = adapterDetallesMes.getItems().indexOf(detalle);
                if (position != -1) {
                    adapterDetallesMes.notifyItemChanged(position, "CALCULATED_FIELDS");
                }

                actualizarDetalleMes(detalle);
            }

            @Override
            public void onGenerarValorizacion(DetalleMes detalle, int position) {
                if (alquilerActual == null) return;
                double horometroAnterior;
                if (detalle.getNumeroMes() == 1) {
                    horometroAnterior = alquilerActual.getHorometroInicial();
                } else {
                    horometroAnterior = obtenerHorometroMesAnterior(detalle.getNumeroMes() - 1);
                }

                DialogGenerarValorizacion dialog = DialogGenerarValorizacion.newInstance(alquilerActual, detalle, horometroAnterior);
                dialog.setOnValorizacionGeneradaListener((val, pdf) -> {
                    if (val.isConHorasExtras() && val.getHorometroFinal() > 0) {
                        detalle.setHorometro(val.getHorometroFinal());
                        detalle.setHorasExtras(val.getHorasExtras());
                        detalle.setPrecioHorasExtras(val.getTotalHorasExtras());
                        adapterDetallesMes.notifyItemChanged(position);
                    }
                });
                dialog.show(getChildFragmentManager(), "DialogGenerarValorizacion");
            }
        });

        btnFinalizarAlquiler.setOnClickListener(v -> mostrarDialogoConfirmarEnvio());
    }

    private void calcularPrecioHoraExtra() {
        String precioStr = inputPrecioAlquiler.getText().toString().trim();
        String horasStr = inputHorasMinimas.getText().toString().trim();

        if (!precioStr.isEmpty() && !horasStr.isEmpty()) {
            try {
                double precio = Double.parseDouble(precioStr);
                double horas = Double.parseDouble(horasStr);

                if (horas > 0) {
                    double precioHoraExtra = com.example.maquirentapp.strategy.StrategyProvider.getCalculadoraStrategy().calcularPrecioHoraExtra(precio, (int) horas);
                    inputPrecioHoraExtra.setText(String.format(Locale.US, "%.2f", precioHoraExtra));
                }
            } catch (NumberFormatException e) {
            }
        }
    }

    private void registrarIngresoProrrateado(double montoTotal, String tipo, DetalleMes detalle) {
        if (alquilerActual == null || montoTotal == 0) return;

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

        try {
            Date fechaInicio = sdf.parse(detalle.getFechaInicio());
            Date fechaFin = sdf.parse(detalle.getFechaFin());
            if (fechaInicio == null || fechaFin == null) return;

            long diff = fechaFin.getTime() - fechaInicio.getTime();
            long totalDiasPeriodo = java.util.concurrent.TimeUnit.DAYS.convert(diff, java.util.concurrent.TimeUnit.MILLISECONDS) + 1;

            double valorDiario = montoTotal / totalDiasPeriodo;

            Calendar cal = Calendar.getInstance();
            cal.setTime(fechaInicio);

            while (!cal.getTime().after(fechaFin)) {
                int mesActual = cal.get(Calendar.MONTH);
                int anioActual = cal.get(Calendar.YEAR);

                int diasEnEsteMes = 0;
                while (cal.get(Calendar.MONTH) == mesActual && !cal.getTime().after(fechaFin)) {
                    diasEnEsteMes++;
                    cal.add(Calendar.DAY_OF_YEAR, 1);
                }

                if (diasEnEsteMes > 0) {
                    double montoParaEsteMes = valorDiario * diasEnEsteMes;

                    Ingreso ingreso = new Ingreso(
                            montoParaEsteMes,
                            alquilerActual.getMoneda(),
                            tipo,
                            alquilerActual.getIdGrupo(),
                            alquilerActual.getId(),
                            alquilerActual.getNombreCliente(),
                            mesActual + 1,
                            anioActual
                    );

                    enviarIngresoAFirebase(ingreso);
                }
            }

        } catch (ParseException e) {
            Log.e("NuevoAlquiler", "Error al prorratear ingreso", e);
        }
    }

    private void enviarIngresoAFirebase(Ingreso ingreso) {
        alquilerRepository.registrarIngreso(ingreso, new Callback<String>() {
            @Override
            public void onSuccess(String id) {
                Log.i("NuevoAlquiler", "Ingreso registrado: " + id);
            }

            @Override
            public void onError(Exception e) {
                Log.e("NuevoAlquiler", "Error ingreso", e);
            }
        });
    }

    private void calcularHorasExtras(DetalleMes detalle, double horometroActual) {
        if (alquilerActual == null) return;

        try {
            double horometroAnterior;
            if (detalle.getNumeroMes() == 1) {
                horometroAnterior = alquilerActual.getHorometroInicial();
            } else {
                horometroAnterior = obtenerHorometroMesAnterior(detalle.getNumeroMes() - 1);
            }

            if (horometroActual < horometroAnterior) {
                detalle.setHorometro(horometroActual);
                detalle.setHorasExtras(0);
                detalle.setPrecioHorasExtras(0);
                return;
            }

            int horasMinimasEfectivas = detalle.getHorasMinimas() > 0 ? detalle.getHorasMinimas() : alquilerActual.getHorasMinimas();
            com.example.maquirentapp.strategy.ResultadoHorasExtras resultado = com.example.maquirentapp.strategy.StrategyProvider.getCalculadoraStrategy().calcularHorasExtras(
                    horometroActual, horometroAnterior, horasMinimasEfectivas, detalle.getFechaInicio(), detalle.getFechaFin(), alquilerActual.getPrecioHoraExtra()
            );

            detalle.setHorometro(horometroActual);
            detalle.setHorasExtras(resultado.getHorasExtras());
            detalle.setPrecioHorasExtras(resultado.getPrecioTotalHorasExtras());

        } catch (Exception e) {
            Log.e("NuevoAlquilerMensual", "Error calculando horas extras", e);
        }
    }

    private long calcularDiasEntreFechas(String inicio, String fin) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        try {
            Date d1 = sdf.parse(inicio);
            Date d2 = sdf.parse(fin);
            if (d1 == null || d2 == null) return 30;

            long diff = d2.getTime() - d1.getTime();
            long dias = java.util.concurrent.TimeUnit.DAYS.convert(diff, java.util.concurrent.TimeUnit.MILLISECONDS);

            return dias + 1;
        } catch (ParseException e) {
            return 30;
        }
    }

    private double obtenerHorometroMesAnterior(int numeroMesAnterior) {
        List<DetalleMes> fuente = !listaTodosDetallesMes.isEmpty() ? listaTodosDetallesMes : adapterDetallesMes.getItems();
        for (DetalleMes detalle : fuente) {
            if (detalle.getNumeroMes() == numeroMesAnterior) {
                return detalle.getHorometro();
            }
        }
        return 0;
    }

    private void configurarDatePickers() {
        inputFechaInicial.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.US);
            try {
                if (inputFechaInicial.getText() != null && !inputFechaInicial.getText().toString().isEmpty()) {
                    calendar.setTime(sdf.parse(inputFechaInicial.getText().toString().trim()));
                }
            } catch (Exception ignored) {}

            new CustomDatePickerDialog(
                    requireContext(),
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH),
                    (year, month, dayOfMonth) -> {
                        String fecha = String.format(Locale.US, "%02d/%02d/%d", dayOfMonth, month + 1, year);
                        inputFechaInicial.setText(fecha);
                    }
            ).show();
        });

        inputFechaFinal.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.US);
            try {
                if (inputFechaFinal.getText() != null && !inputFechaFinal.getText().toString().isEmpty()) {
                    calendar.setTime(sdf.parse(inputFechaFinal.getText().toString().trim()));
                }
            } catch (Exception ignored) {}

            new CustomDatePickerDialog(
                    requireContext(),
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH),
                    (year, month, dayOfMonth) -> {
                        String fecha = String.format(Locale.US, "%02d/%02d/%d", dayOfMonth, month + 1, year);
                        inputFechaFinal.setText(fecha);
                    }
            ).show();
        });
    }

    private void cargarAccesoriosMensuales() {
        alquilerRepository.getAccesorios("mensual", new Callback<List<Accesorio>>() {
            @Override
            public void onSuccess(List<Accesorio> accesorios) {
                adapterAccesorios.setItems(accesorios);
            }

            @Override
            public void onError(Exception e) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Error al cargar accesorios: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void cargarDatosAlquiler() {
        alquilerRepository.getAlquilerMensualPorId(alquilerId, new Callback<AlquilerMensual>() {
            @Override
            public void onSuccess(AlquilerMensual alquiler) {
                alquilerActual = alquiler;
                idGrupo = alquiler.getIdGrupo();
                if (codigo != null && !codigo.isEmpty() && getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).updateHeaderTitle("Nuevo alquiler mensual\n" + codigo);
                }
                inputEmpresa.setText(alquiler.getNombreCliente());
                inputUbicacion.setText(alquiler.getUbicacion());
                inputFechaInicial.setText(formatearFecha(alquiler.getFechaInicial()));
                inputFechaFinal.setText(formatearFecha(alquiler.getFechaFinal()));
                inputHorometroInicial.setText(String.valueOf(alquiler.getHorometroInicial()));
                inputHorometroFinal.setText(String.valueOf(alquiler.getHorometroFinal()));
                inputPrecioAlquiler.setText(String.valueOf(alquiler.getPrecioAlquiler()));
                inputHorasMinimas.setText(String.valueOf(alquiler.getHorasMinimas()));
                inputPrecioHoraExtra.setText(String.valueOf(alquiler.getPrecioHoraExtra()));

                String moneda = alquiler.getMoneda() != null ? alquiler.getMoneda() : "SOL";
                spinnerMoneda.setSelection(moneda.equals("USD") ? 1 : 0);

                if (alquiler.getAccesoriosIds() != null) {
                    adapterAccesorios.setAccesoriosSeleccionados(alquiler.getAccesoriosIds());
                }

                String simbolo = "USD".equals(moneda) ? "$" : "S/.";
                adapterDetallesMes.setSimboloMoneda(simbolo);
                adapterDetallesMes.setPrecioMensualBase(alquiler.getPrecioAlquiler());

                cargarDetallesMes();

                if (alquiler.isFinalizado()) {
                    deshabilitarCampos();
                    hideGlobalFab();
                    if (btnGenerarSiguienteMesManual != null) btnGenerarSiguienteMesManual.setVisibility(View.GONE);
                } else {
                    configureGlobalFab();
                    if (btnGenerarSiguienteMesManual != null) btnGenerarSiguienteMesManual.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onError(Exception e) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Error al cargar alquiler", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void cargarDetallesMes() {
        if (alquilerId == null) return;
        if (shimmerDetallesMes != null) {
            shimmerDetallesMes.setVisibility(View.VISIBLE);
            shimmerDetallesMes.startShimmer();
        }
        recyclerDetallesMes.setVisibility(View.GONE);
        if (layoutSelectorAnios != null) {
            layoutSelectorAnios.setVisibility(View.GONE);
        }

        alquilerRepository.getDetallesMesPorAlquiler(alquilerId, new Callback<List<DetalleMes>>() {
            @Override
            public void onSuccess(List<DetalleMes> detalles) {
                if (shimmerDetallesMes != null) {
                    shimmerDetallesMes.stopShimmer();
                    shimmerDetallesMes.setVisibility(View.GONE);
                }
                recyclerDetallesMes.setVisibility(View.VISIBLE);
                listaTodosDetallesMes = new ArrayList<>(detalles);
                configurarSelectorAnios();
            }

            @Override
            public void onError(Exception e) {
                if (shimmerDetallesMes != null) {
                    shimmerDetallesMes.stopShimmer();
                    shimmerDetallesMes.setVisibility(View.GONE);
                }
                recyclerDetallesMes.setVisibility(View.VISIBLE);
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Error al cargar detalles", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void configurarSelectorAnios() {
        if (layoutSelectorAnios == null || containerChipsAnios == null || getContext() == null) return;

        Set<String> aniosSet = new TreeSet<>();
        for (DetalleMes d : listaTodosDetallesMes) {
            String a = extraerAnio(d);
            if (!a.isEmpty()) {
                aniosSet.add(a);
            }
        }

        if (aniosSet.size() <= 1) {
            layoutSelectorAnios.setVisibility(View.GONE);
            adapterDetallesMes.setItems(listaTodosDetallesMes);
            adapterDetallesMes.setModoSoloLectura(modoSoloLectura && !editandoActualmente);
            return;
        }

        layoutSelectorAnios.setVisibility(View.VISIBLE);
        containerChipsAnios.removeAllViews();

        List<String> listaAnios = new ArrayList<>(aniosSet);
        if (anioSeleccionado == null || (!anioSeleccionado.equals("TODOS") && !aniosSet.contains(anioSeleccionado))) {
            anioSeleccionado = listaAnios.get(listaAnios.size() - 1);
        }

        for (String anio : listaAnios) {
            long count = 0;
            for (DetalleMes d : listaTodosDetallesMes) {
                if (extraerAnio(d).equals(anio)) count++;
            }
            agregarChipAnio(anio, anio + " (" + count + ")", anio.equals(anioSeleccionado));
        }

        agregarChipAnio("TODOS", "Todos (" + listaTodosDetallesMes.size() + ")", "TODOS".equals(anioSeleccionado));

        aplicarFiltroAnio(anioSeleccionado);
    }

    private String extraerAnio(DetalleMes d) {
        String f = d.getFechaFin();
        if (f == null || f.isEmpty()) f = d.getFechaInicio();
        if (f != null && f.contains("/")) {
            String[] partes = f.split("/");
            if (partes.length == 3) {
                return partes[2].trim();
            }
        }
        return "";
    }

    private void agregarChipAnio(String idAnio, String texto, boolean isSelected) {
        if (getContext() == null) return;
        TextView chip = new TextView(getContext());
        chip.setText(texto);
        chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        chip.setPadding(36, 16, 36, 16);

        Typeface typefaceAnta = null;
        try {
            typefaceAnta = ResourcesCompat.getFont(getContext(), R.font.anta_font);
        } catch (Exception ignored) {}
        if (typefaceAnta != null) {
            chip.setTypeface(typefaceAnta);
        }

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMarginEnd(16);
        chip.setLayoutParams(params);

        actualizarEstiloChip(chip, isSelected);

        chip.setOnClickListener(v -> {
            anioSeleccionado = idAnio;
            for (int i = 0; i < containerChipsAnios.getChildCount(); i++) {
                View child = containerChipsAnios.getChildAt(i);
                if (child instanceof TextView) {
                    TextView tv = (TextView) child;
                    Object tag = tv.getTag();
                    actualizarEstiloChip(tv, idAnio.equals(tag));
                }
            }
            aplicarFiltroAnio(idAnio);
        });

        chip.setTag(idAnio);
        containerChipsAnios.addView(chip);
    }

    private void actualizarEstiloChip(TextView chip, boolean isSelected) {
        if (getContext() == null) return;

        Typeface typefaceAnta = null;
        try {
            typefaceAnta = ResourcesCompat.getFont(getContext(), R.font.anta_font);
        } catch (Exception ignored) {}
        if (typefaceAnta != null) {
            chip.setTypeface(typefaceAnta);
        }

        if (isSelected) {
            chip.setBackgroundResource(R.drawable.bg_chip_anio_selected);
            chip.setTextColor(Color.WHITE);
        } else {
            chip.setBackgroundResource(R.drawable.bg_chip_anio_unselected);
            chip.setTextColor(Color.parseColor("#B0B0B0"));
        }
    }

    private void aplicarFiltroAnio(String anio) {
        List<DetalleMes> filtrados = new ArrayList<>();
        if ("TODOS".equals(anio)) {
            filtrados.addAll(listaTodosDetallesMes);
        } else {
            for (DetalleMes d : listaTodosDetallesMes) {
                if (extraerAnio(d).equals(anio)) {
                    filtrados.add(d);
                }
            }
        }
        adapterDetallesMes.setItems(filtrados);
        adapterDetallesMes.setModoSoloLectura(modoSoloLectura && !editandoActualmente);
    }

    private void configurarSwipeToDelete() {
        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override
            public int getSwipeDirs(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                if ((modoSoloLectura && !editandoActualmente) || (alquilerActual != null && alquilerActual.isFinalizado())) {
                    return 0;
                }
                return super.getSwipeDirs(recyclerView, viewHolder);
            }

            @Override
            public boolean onMove(@NonNull RecyclerView r, @NonNull RecyclerView.ViewHolder v, @NonNull RecyclerView.ViewHolder t) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                if (position == RecyclerView.NO_POSITION) return;

                if (getContext() == null) {
                    adapterDetallesMes.notifyItemChanged(position);
                    return;
                }

                DetalleMes detalleAEliminar = adapterDetallesMes.getItem(position);
                if (detalleAEliminar == null) {
                    adapterDetallesMes.notifyItemChanged(position);
                    return;
                }

                // 1. No permitir eliminar si es el único mes existente
                if (listaTodosDetallesMes.size() <= 1) {
                    Toast.makeText(getContext(),
                            "No se puede eliminar el único mes del alquiler. Si deseas cancelar el alquiler completo, hazlo desde el Historial.",
                            Toast.LENGTH_LONG).show();
                    adapterDetallesMes.notifyItemChanged(position);
                    return;
                }

                // 2. Solo se permite eliminar el último período registrado para mantener correlatividad
                DetalleMes ultimoMes = listaTodosDetallesMes.get(listaTodosDetallesMes.size() - 1);
                if (detalleAEliminar.getId() != null && !detalleAEliminar.getId().equals(ultimoMes.getId())) {
                    Toast.makeText(getContext(),
                            "Solo se puede eliminar el último período registrado (Mes " + ultimoMes.getNumeroMes() + ") para mantener la correlatividad de fechas y horómetros.",
                            Toast.LENGTH_LONG).show();
                    adapterDetallesMes.notifyItemChanged(position);
                    return;
                }

                // 3. Preparar mensaje de confirmación
                String periodoStr = "Mes " + detalleAEliminar.getNumeroMes();
                if (detalleAEliminar.getFechaInicio() != null && detalleAEliminar.getFechaFin() != null) {
                    periodoStr += " (" + detalleAEliminar.getFechaInicio() + " al " + detalleAEliminar.getFechaFin() + ")";
                }

                StringBuilder mensaje = new StringBuilder();
                if (detalleAEliminar.isPagoMesConfirmado() || detalleAEliminar.isPagoHEConfirmado()) {
                    mensaje.append("⚠️ Advertencia: Este período ya tiene pagos confirmados (Mes o Horas Extras).\n\n");
                }
                mensaje.append("¿Estás seguro de que deseas eliminar este período: ").append(periodoStr).append("?\n\nEsta acción no se puede deshacer.");

                new MaterialAlertDialogBuilder(requireContext(), R.style.DialogoConFuenteAnta)
                        .setTitle("Eliminar período")
                        .setMessage(mensaje.toString())
                        .setCancelable(false)
                        .setPositiveButton("Eliminar", (dialog, which) -> {
                            eliminarDetalleMesFirebase(detalleAEliminar, position);
                        })
                        .setNegativeButton("Cancelar", (dialog, which) -> {
                            adapterDetallesMes.notifyItemChanged(position);
                        })
                        .setOnCancelListener(dialog -> adapterDetallesMes.notifyItemChanged(position))
                        .show();
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
                                    @NonNull RecyclerView.ViewHolder viewHolder,
                                    float dX, float dY, int actionState, boolean isCurrentlyActive) {
                new RecyclerViewSwipeDecorator.Builder(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
                        .addBackgroundColor(ContextCompat.getColor(requireContext(), R.color.red_accent))
                        .addActionIcon(R.drawable.icon_eliminar_rojo)
                        .setActionIconTint(R.color.white)
                        .addCornerRadius(TypedValue.COMPLEX_UNIT_DIP, 16)
                        .create()
                        .decorate();

                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }
        };

        new ItemTouchHelper(simpleCallback).attachToRecyclerView(recyclerDetallesMes);
    }

    private void eliminarDetalleMesFirebase(DetalleMes detalle, int position) {
        if (detalle.getId() == null || detalle.getId().isEmpty()) {
            removerDetalleMesLocal(detalle);
            return;
        }

        alquilerRepository.eliminarDetalleMes(detalle.getId(), new Callback<Void>() {
            @Override
            public void onSuccess(Void result) {
                if (getContext() == null) return;
                Toast.makeText(getContext(), "Período eliminado correctamente", Toast.LENGTH_SHORT).show();
                removerDetalleMesLocal(detalle);
            }

            @Override
            public void onError(Exception e) {
                if (getContext() == null) return;
                Toast.makeText(getContext(), "Error al eliminar: " + e.getMessage(), Toast.LENGTH_LONG).show();
                adapterDetallesMes.notifyItemChanged(position);
            }
        });
    }

    private void removerDetalleMesLocal(DetalleMes detalle) {
        for (int i = 0; i < listaTodosDetallesMes.size(); i++) {
            DetalleMes d = listaTodosDetallesMes.get(i);
            if ((d.getId() != null && d.getId().equals(detalle.getId())) ||
                    (d.getNumeroMes() == detalle.getNumeroMes())) {
                listaTodosDetallesMes.remove(i);
                break;
            }
        }
        configurarSelectorAnios();
    }

    private String formatearFecha(String fechaOriginal) {
        if (fechaOriginal == null || fechaOriginal.isEmpty()) return "";
        try {
            SimpleDateFormat formatoEntrada = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            SimpleDateFormat formatoSalida = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            return formatoSalida.format(formatoEntrada.parse(fechaOriginal));
        } catch (Exception e) {
            return fechaOriginal;
        }
    }

    private void setAccesoriosExpanded(boolean expand, boolean animate) {
        if (accBody == null || ivAccChevron == null) return;

        if (expand) {
            if (animate) {
                accBody.setAlpha(0f);
                accBody.setVisibility(View.VISIBLE);
                accBody.animate().alpha(1f).setDuration(180).start();
            } else {
                accBody.setVisibility(View.VISIBLE);
            }
            if (animate) ivAccChevron.animate().rotation(180f).setDuration(180).start();
            else ivAccChevron.setRotation(180f);
        } else {
            if (animate) {
                accBody.animate().alpha(0f).setDuration(160).withEndAction(() -> accBody.setVisibility(View.GONE)).start();
            } else {
                accBody.setVisibility(View.GONE);
            }
            if (animate) ivAccChevron.animate().rotation(0f).setDuration(180).start();
            else ivAccChevron.setRotation(0f);
        }
    }

    private void configureGlobalFab() {
        if (getActivity() instanceof MainActivity) {
            MainActivity mainActivity = (MainActivity) getActivity();

            if (alquilerActual != null && alquilerActual.isFinalizado()) {
                mainActivity.hideGlobalFab();
                return;
            }

            if (modoSoloLectura && !editandoActualmente) {
                configurarFabEditar();
            } else {
                configurarFabGuardar();
            }
        }
    }

    private void hideGlobalFab() {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).hideGlobalFab();
        }
    }

    private void configurarFabEditar() {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).showGlobalFab(
                    "Editar",
                    R.drawable.icon_editar_blanco,
                    v -> {
                        editandoActualmente = true;
                        habilitarCampos();
                        configureGlobalFab();
                    }
            );
        }
    }

    private void configurarFabGuardar() {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).showGlobalFab(
                    "Guardar",
                    R.drawable.icon_guardar_blanco,
                    v -> guardarAlquilerMensual()
            );
        }
    }

    private void guardarAlquilerMensual() {
        String empresa = inputEmpresa.getText().toString().trim();
        String ubicacion = inputUbicacion.getText().toString().trim();
        String fechaInicial = inputFechaInicial.getText().toString().trim();
        String hIniStr = inputHorometroInicial.getText().toString().trim();
        String precioStr = inputPrecioAlquiler.getText().toString().trim();
        String horasMinStr = inputHorasMinimas.getText().toString().trim();

        if (empresa.isEmpty() || ubicacion.isEmpty() || fechaInicial.isEmpty() ||
                hIniStr.isEmpty() || precioStr.isEmpty() || horasMinStr.isEmpty()) {
            Toast.makeText(getContext(), "Por favor completa todos los campos obligatorios", Toast.LENGTH_SHORT).show();
            return;
        }

        double horometroInicial, horometroFinal = 0, precioAlquiler, precioHoraExtra = 0;
        int horasMinimas;

        try {
            horometroInicial = Double.parseDouble(hIniStr);
            String hFinStr = inputHorometroFinal.getText().toString().trim();
            if (!hFinStr.isEmpty()) {
                horometroFinal = Double.parseDouble(hFinStr);
            }
            precioAlquiler = Double.parseDouble(precioStr);
            horasMinimas = Integer.parseInt(horasMinStr);
            String precioHEStr = inputPrecioHoraExtra.getText().toString().trim();
            if (!precioHEStr.isEmpty()) {
                precioHoraExtra = Double.parseDouble(precioHEStr);
            }
        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "Verifica que los campos numéricos sean válidos", Toast.LENGTH_SHORT).show();
            return;
        }

        AlquilerMensual alquiler = modoEdicion ? alquilerActual : new AlquilerMensual();
        if (modoEdicion && alquiler.getId() == null) {
            alquiler.setId(alquilerId);
        }

        alquiler.setNombreCliente(empresa);
        alquiler.setUbicacion(ubicacion);
        alquiler.setFechaInicial(fechaInicial);
        alquiler.setFechaFinal(inputFechaFinal.getText().toString().trim());
        alquiler.setHorometroInicial(horometroInicial);
        alquiler.setHorometroFinal(horometroFinal);
        alquiler.setPrecioAlquiler(precioAlquiler);
        alquiler.setMoneda(monedaSeleccionada);
        alquiler.setHorasMinimas(horasMinimas);
        alquiler.setPrecioHoraExtra(precioHoraExtra);
        alquiler.setIdGrupo(idGrupo);
        alquiler.setAccesoriosIds(adapterAccesorios.getAccesoriosSeleccionados());

        String uid = usuarioRepository.getCurrentUserUid();
        if (uid != null) {
            alquiler.setAdminUid(uid);
        }

        if (modoEdicion) {
            alquilerRepository.actualizarAlquilerMensual(alquiler, new Callback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Alquiler actualizado correctamente", Toast.LENGTH_SHORT).show();
                    }
                    if (getActivity() != null) {
                        requireActivity().onBackPressed();
                    }
                }

                @Override
                public void onError(Exception e) {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Error al actualizar: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }
            });
        } else {
            DetalleMes primerMes = construirPrimerDetalleMes(alquiler);
            alquilerRepository.crearAlquilerMensualConPrimerMes(alquiler, primerMes, new Callback<AlquilerMensual>() {
                @Override
                public void onSuccess(AlquilerMensual alquilerCreado) {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Alquiler registrado correctamente", Toast.LENGTH_SHORT).show();
                    }
                    if (getActivity() != null) {
                        requireActivity().onBackPressed();
                    }
                }

                @Override
                public void onError(Exception e) {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Error al guardar: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }
            });
        }
    }

    private DetalleMes construirPrimerDetalleMes(AlquilerMensual alquiler) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Date fechaInicioDate = sdf.parse(alquiler.getFechaInicial());
            if (fechaInicioDate == null) return null;

            Calendar calInicio = Calendar.getInstance();
            calInicio.setTime(fechaInicioDate);

            Calendar calFin = (Calendar) calInicio.clone();
            calFin.add(Calendar.DAY_OF_YEAR, 29);

            Date fechaFinDate = calFin.getTime();

            String strFechaInicio = sdf.format(fechaInicioDate);
            String strFechaFin = sdf.format(fechaFinDate);

            DetalleMes primerMes = new DetalleMes();
            primerMes.setNumeroMes(1);
            primerMes.setFechaInicio(strFechaInicio);
            primerMes.setFechaFin(strFechaFin);
            primerMes.setMontoMes(alquiler.getPrecioAlquiler());

            SimpleDateFormat sdfMes = new SimpleDateFormat("MMMM", new Locale("es", "ES"));
            String m1 = sdfMes.format(fechaInicioDate);
            String m2 = sdfMes.format(fechaFinDate);
            m1 = m1.substring(0, 1).toUpperCase(new Locale("es", "ES")) + m1.substring(1).toLowerCase(new Locale("es", "ES"));
            m2 = m2.substring(0, 1).toUpperCase(new Locale("es", "ES")) + m2.substring(1).toLowerCase(new Locale("es", "ES"));
            String periodoNombre = m1.equalsIgnoreCase(m2) ? m1 : (m1 + " - " + m2);
            primerMes.setTituloPeriodo(periodoNombre);
            return primerMes;
        } catch (ParseException e) {
            Log.e("NuevoAlquiler", "Error al procesar fechas para primer mes", e);
            return null;
        }
    }

    private void actualizarDetalleMes(DetalleMes detalle) {
        alquilerRepository.actualizarDetalleMes(detalle, new Callback<Void>() {
            @Override
            public void onSuccess(Void result) {
            }

            @Override
            public void onError(Exception e) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Error al actualizar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void mostrarDialogoConfirmarEnvio() {
        if (alquilerActual == null) return;

        new MaterialAlertDialogBuilder(requireContext(), R.style.DialogoConFuenteAnta)
                .setTitle("Finalizar Alquiler")
                .setMessage("Estás a punto de finalizar este alquiler. Una vez finalizado, no podrás editar los datos.\n\nSe enviará un código a tu correo para confirmar.")
                .setPositiveButton("Enviar Código", (dialog, which) -> {
                    btnFinalizarAlquiler.setEnabled(false);
                    btnFinalizarAlquiler.setText("Enviando...");

                    alquilerRepository.solicitarCodigoFinalizacion(
                            alquilerActual.getId(),
                            alquilerActual.getNombreCliente(),
                            new Callback<Void>() {
                                @Override
                                public void onSuccess(Void result) {
                                    if (getContext() != null) {
                                        Toast.makeText(getContext(), "Código enviado a tu correo", Toast.LENGTH_SHORT).show();
                                    }
                                    btnFinalizarAlquiler.setEnabled(true);
                                    btnFinalizarAlquiler.setText("Finalizar alquiler");
                                    mostrarDialogoIngresarCodigo();
                                }

                                @Override
                                public void onError(Exception e) {
                                    if (getContext() != null) {
                                        Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                    }
                                    btnFinalizarAlquiler.setEnabled(true);
                                    btnFinalizarAlquiler.setText("Finalizar alquiler");
                                }
                            }
                    );
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void mostrarDialogoIngresarCodigo() {
        if (alquilerActual == null) return;

        final EditText inputCodigo = new EditText(getContext());
        inputCodigo.setHint("Código de 6 dígitos");
        inputCodigo.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        inputCodigo.setMaxLines(1);

        LinearLayout container = new LinearLayout(getContext());
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(48, 16, 48, 16);
        container.addView(inputCodigo);

        new MaterialAlertDialogBuilder(requireContext(), R.style.DialogoConFuenteAnta)
                .setTitle("Ingresa el Código")
                .setMessage("Revisa tu correo e ingresa el código de 6 dígitos para finalizar el alquiler.")
                .setView(container)
                .setPositiveButton("Finalizar", (dialog, which) -> {
                    String codigo = inputCodigo.getText().toString().trim();
                    if (codigo.length() == 6) {
                        confirmarFinalizacion(codigo);
                    } else {
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "El código debe tener 6 dígitos", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void confirmarFinalizacion(String codigo) {
        String fechaFinalManual = inputFechaFinal.getText().toString().trim();
        String horometroFinalManualStr = inputHorometroFinal.getText().toString().trim();

        if (fechaFinalManual.isEmpty() || horometroFinalManualStr.isEmpty()) {
            if (getContext() != null) {
                Toast.makeText(getContext(), "Fecha Final y Horómetro Final son obligatorios.", Toast.LENGTH_LONG).show();
            }
            return;
        }

        procederConFinalizacion(codigo, fechaFinalManual, horometroFinalManualStr);
    }

    private void procederConFinalizacion(String codigo, String fechaFinalManual, String horometroFinalManualStr) {
        double horometroFinal;
        String fechaFinal = fechaFinalManual;

        try {
            horometroFinal = Double.parseDouble(horometroFinalManualStr);
        } catch (NumberFormatException e) {
            if (getContext() != null) {
                Toast.makeText(getContext(), "Horómetro final manual inválido", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        if (alquilerActual == null) {
            if (getContext() != null) {
                Toast.makeText(getContext(), "Error: No se pudo encontrar el alquiler", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        alquilerActual.setHorometroFinal(horometroFinal);
        alquilerActual.setFechaFinal(fechaFinal);

        alquilerRepository.confirmarFinalizacion(
                alquilerActual.getId(),
                codigo,
                horometroFinal,
                fechaFinal,
                new Callback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "Alquiler Finalizado", Toast.LENGTH_LONG).show();
                        }
                        alquilerActual.setFinalizado(true);
                        deshabilitarCampos();
                        hideGlobalFab();
                    }

                    @Override
                    public void onError(Exception e) {
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                }
        );
    }
    private void verificarYGenerarSiguienteMes(DetalleMes detallePagado) {
        if (alquilerActual == null || alquilerActual.isFinalizado()) {
            return;
        }

        if (isGenerandoSiguienteMes) {
            Log.w(TAG, "Ya se está generando el siguiente mes. Ignorando llamada simultánea.");
            return;
        }

        List<DetalleMes> listaActual = !listaTodosDetallesMes.isEmpty() ? listaTodosDetallesMes : adapterDetallesMes.getItems();
        if (listaActual.isEmpty()) return;

        DetalleMes ultimoMes = listaActual.get(listaActual.size() - 1);

        if (!detallePagado.getId().equals(ultimoMes.getId())) {
            return;
        }

        int siguienteNumero = detallePagado.getNumeroMes() + 1;
        for (DetalleMes d : listaActual) {
            if (d.getNumeroMes() >= siguienteNumero) {
                Log.w(TAG, "El mes " + siguienteNumero + " ya existe en la lista. No se duplicará.");
                return;
            }
        }

        long diasDuracion = calcularDiasEntreFechas(detallePagado.getFechaInicio(), detallePagado.getFechaFin());
        if (diasDuracion < 30) {
            if (getContext() != null) {
                new AlertDialog.Builder(getContext())
                        .setTitle("Período con corte parcial (" + diasDuracion + " días)")
                        .setMessage("Este período duró menos de 30 días.\n\n¿El cliente continuará con el alquiler para el siguiente mes?")
                        .setPositiveButton("Sí, continuar alquiler", (dialog, which) -> {
                            generarSiguienteMesAuto(detallePagado);
                        })
                        .setNegativeButton("No, es el último período", (dialog, which) -> {
                            if (getContext() != null) {
                                Toast.makeText(getContext(), "Período guardado como último. Puede presionar 'Finalizar alquiler' cuando devuelvan el equipo.", Toast.LENGTH_LONG).show();
                            }
                        })
                        .show();
            }
            return;
        }

        generarSiguienteMesAuto(detallePagado);
    }

    private void generarSiguienteMesAuto(DetalleMes detallePagado) {
        if (alquilerActual == null || alquilerActual.isFinalizado()) return;

        int siguienteNumero = detallePagado.getNumeroMes() + 1;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Date fechaFinAnterior = sdf.parse(detallePagado.getFechaFin());
            Calendar cal = Calendar.getInstance();
            if (fechaFinAnterior != null) {
                cal.setTime(fechaFinAnterior);
            }

            cal.add(Calendar.DAY_OF_YEAR, 1);
            Date nuevaFechaInicio = cal.getTime();
            String strFechaInicio = sdf.format(nuevaFechaInicio);

            cal.add(Calendar.DAY_OF_YEAR, 29);
            Date nuevaFechaFin = cal.getTime();
            String strFechaFin = sdf.format(nuevaFechaFin);

            crearPeriodoMes(siguienteNumero, strFechaInicio, strFechaFin, alquilerActual.getPrecioAlquiler(), alquilerActual.getHorasMinimas());
        } catch (ParseException e) {
            Log.e(TAG, "Error en fechas al auto-generar mes", e);
        }
    }

    private void mostrarDialogoGenerarSiguienteMesManual() {
        if (alquilerActual == null || getContext() == null) return;

        List<DetalleMes> listaActual = !listaTodosDetallesMes.isEmpty() ? listaTodosDetallesMes : adapterDetallesMes.getItems();
        int nuevoNumero = 1;
        String fechaInicioSugerida = "";

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        Calendar cal = Calendar.getInstance();

        if (!listaActual.isEmpty()) {
            DetalleMes ultimo = listaActual.get(listaActual.size() - 1);
            nuevoNumero = ultimo.getNumeroMes() + 1;
            if (ultimo.getFechaFin() != null && !ultimo.getFechaFin().isEmpty()) {
                try {
                    Date dFin = sdf.parse(ultimo.getFechaFin());
                    if (dFin != null) {
                        cal.setTime(dFin);
                        cal.add(Calendar.DAY_OF_YEAR, 1);
                        fechaInicioSugerida = sdf.format(cal.getTime());
                    }
                } catch (Exception ignored) {}
            }
        } else {
            fechaInicioSugerida = formatearFecha(alquilerActual.getFechaInicial());
        }

        if (fechaInicioSugerida.isEmpty()) {
            fechaInicioSugerida = sdf.format(new Date());
        }

        String fechaFinSugerida = "";
        try {
            Date dIni = sdf.parse(fechaInicioSugerida);
            if (dIni != null) {
                cal.setTime(dIni);
                cal.add(Calendar.DAY_OF_YEAR, 29);
                fechaFinSugerida = sdf.format(cal.getTime());
            }
        } catch (Exception ignored) {}

        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_generar_mes_manual, null);
        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextView tvTitulo = dialogView.findViewById(R.id.tvTituloDialogoGenerar);
        TextView tvSubtitulo = dialogView.findViewById(R.id.tvSubtituloGenerar);
        TextInputEditText inputInicio = dialogView.findViewById(R.id.inputFechaInicioManual);
        TextInputEditText inputFin = dialogView.findViewById(R.id.inputFechaFinManual);
        TextView tvDias = dialogView.findViewById(R.id.tvDiasCalculadosManual);
        TextInputEditText inputMonto = dialogView.findViewById(R.id.inputMontoMesManual);
        TextInputEditText inputHorasMin = dialogView.findViewById(R.id.inputHorasMinimasManual);
        Button btnCancelar = dialogView.findViewById(R.id.btnCancelarGenerar);
        Button btnCrear = dialogView.findViewById(R.id.btnConfirmarGenerar);

        tvTitulo.setText("Generar Mes " + nuevoNumero);
        tvSubtitulo.setText("Configura el período para el nuevo mes de alquiler");
        inputInicio.setText(fechaInicioSugerida);
        inputFin.setText(fechaFinSugerida);
        inputMonto.setText(String.format(Locale.US, "%.2f", alquilerActual.getPrecioAlquiler()));
        inputHorasMin.setText(String.valueOf(alquilerActual.getHorasMinimas() > 0 ? alquilerActual.getHorasMinimas() : 200));

        final int numMesFinal = nuevoNumero;
        actualizarDiasManual(tvDias, inputInicio.getText().toString(), inputFin.getText().toString());

        inputInicio.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            try {
                Date d = sdf.parse(inputInicio.getText().toString().trim());
                if (d != null) c.setTime(d);
            } catch (Exception ignored) {}
            new CustomDatePickerDialog(getContext(), c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH),
                    (year, month, day) -> {
                        String f = String.format(Locale.US, "%02d/%02d/%d", day, month + 1, year);
                        inputInicio.setText(f);
                        actualizarDiasManual(tvDias, f, inputFin.getText().toString());
                    }).show();
        });

        inputFin.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            try {
                Date d = sdf.parse(inputFin.getText().toString().trim());
                if (d != null) c.setTime(d);
            } catch (Exception ignored) {}
            CustomDatePickerDialog dpd = new CustomDatePickerDialog(getContext(), c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH),
                    (year, month, day) -> {
                        String f = String.format(Locale.US, "%02d/%02d/%d", day, month + 1, year);
                        inputFin.setText(f);
                        actualizarDiasManual(tvDias, inputInicio.getText().toString(), f);
                    });
            try {
                Date dIni = sdf.parse(inputInicio.getText().toString().trim());
                if (dIni != null) dpd.setMinDate(dIni.getTime());
            } catch (Exception ignored) {}
            dpd.show();
        });

        btnCancelar.setOnClickListener(v -> dialog.dismiss());

        btnCrear.setOnClickListener(v -> {
            String strIni = inputInicio.getText() != null ? inputInicio.getText().toString().trim() : "";
            String strFin = inputFin.getText() != null ? inputFin.getText().toString().trim() : "";
            if (strIni.isEmpty() || strFin.isEmpty()) {
                Toast.makeText(getContext(), "Complete las fechas de inicio y fin", Toast.LENGTH_SHORT).show();
                return;
            }

            long dias = calcularDiasEntreFechas(strIni, strFin);
            if (dias <= 0) {
                Toast.makeText(getContext(), "La fecha final debe ser posterior a la de inicio", Toast.LENGTH_SHORT).show();
                return;
            }

            double monto;
            try {
                monto = Double.parseDouble(inputMonto.getText().toString().trim());
                if (monto < 0) throw new NumberFormatException();
            } catch (Exception e) {
                Toast.makeText(getContext(), "Ingrese un monto válido", Toast.LENGTH_SHORT).show();
                return;
            }

            int horasMin;
            try {
                horasMin = Integer.parseInt(inputHorasMin.getText().toString().trim());
                if (horasMin < 0) horasMin = 0;
            } catch (Exception e) {
                horasMin = 0;
            }

            dialog.dismiss();
            crearPeriodoMes(numMesFinal, strIni, strFin, monto, horasMin);
        });

        dialog.show();
    }

    private void actualizarDiasManual(TextView tvDias, String inicio, String fin) {
        if (tvDias == null) return;
        long dias = calcularDiasEntreFechas(inicio, fin);
        if (dias <= 0) {
            tvDias.setText("Duración inválida");
            tvDias.setTextColor(Color.RED);
        } else if (dias < 30) {
            tvDias.setText("Duración: " + dias + " días (corte parcial)");
            tvDias.setTextColor(Color.parseColor("#2877BC"));
        } else {
            tvDias.setText("Duración: " + dias + " días (período completo)");
            tvDias.setTextColor(Color.parseColor("#2877BC"));
        }
    }

    private void crearPeriodoMes(int numeroMes, String strFechaInicio, String strFechaFin, double monto, int horasMinimas) {
        if (alquilerActual == null || isGenerandoSiguienteMes) return;

        isGenerandoSiguienteMes = true;

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Date dInicio = sdf.parse(strFechaInicio);

            DetalleMes nuevoMes = new DetalleMes();
            nuevoMes.setIdAlquilerMensual(alquilerActual.getId());
            nuevoMes.setNumeroMes(numeroMes);
            nuevoMes.setFechaInicio(strFechaInicio);
            nuevoMes.setFechaFin(strFechaFin);
            nuevoMes.setMontoMes(monto);
            nuevoMes.setHorasMinimas(horasMinimas);

            long dias = calcularDiasEntreFechas(strFechaInicio, strFechaFin);
            SimpleDateFormat monthFormat = new SimpleDateFormat("MMMM", new Locale("es", "ES"));
            String mesNombre = dInicio != null ? monthFormat.format(dInicio) : "";
            if (!mesNombre.isEmpty()) {
                mesNombre = mesNombre.substring(0, 1).toUpperCase(new Locale("es", "ES")) + mesNombre.substring(1).toLowerCase(new Locale("es", "ES"));
            }
            Date dFin = null;
            try {
                dFin = sdf.parse(strFechaFin);
            } catch (Exception ignored) {}
            String mFin = dFin != null ? monthFormat.format(dFin) : "";
            if (!mFin.isEmpty()) {
                mFin = mFin.substring(0, 1).toUpperCase(new Locale("es", "ES")) + mFin.substring(1).toLowerCase(new Locale("es", "ES"));
            }
            String tituloPeriodo = (!mFin.isEmpty() && !mFin.equalsIgnoreCase(mesNombre)) ? (mesNombre + " - " + mFin) : mesNombre;
            nuevoMes.setTituloPeriodo(tituloPeriodo);

            if (getContext() != null) {
                Toast.makeText(getContext(), "Generando Mes " + numeroMes + "...", Toast.LENGTH_SHORT).show();
            }

            alquilerRepository.crearDetalleMes(nuevoMes, new Callback<DetalleMes>() {
                @Override
                public void onSuccess(DetalleMes detalleCreado) {
                    isGenerandoSiguienteMes = false;
                    cargarDetallesMes();
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Mes " + numeroMes + " generado exitosamente", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onError(Exception e) {
                    isGenerandoSiguienteMes = false;
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Error al generar mes: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            });
        } catch (Exception e) {
            isGenerandoSiguienteMes = false;
            Log.e(TAG, "Error parseando fechas para nuevo mes", e);
        }
    }
    @Override
    public void onResume() {
        super.onResume();
        configureGlobalFab();
    }

    @Override
    public void onPause() {
        super.onPause();
        hideGlobalFab();
    }

    private void deshabilitarCampos() {
        inputEmpresa.setEnabled(false);
        inputUbicacion.setEnabled(false);
        inputFechaInicial.setEnabled(false);
        inputFechaFinal.setEnabled(false);
        inputHorometroInicial.setEnabled(false);
        inputHorometroFinal.setEnabled(false);
        inputPrecioAlquiler.setEnabled(false);
        inputHorasMinimas.setEnabled(false);
        inputPrecioHoraExtra.setEnabled(false);
        spinnerMoneda.setEnabled(false);
        adapterAccesorios.setClickEnabled(false);
        if (alquilerActual != null && alquilerActual.isFinalizado()) {
            btnFinalizarAlquiler.setVisibility(View.GONE);
            if (btnGenerarSiguienteMesManual != null) {
                btnGenerarSiguienteMesManual.setVisibility(View.GONE);
            }
        }
    }

    private void habilitarCampos() {
        inputEmpresa.setEnabled(true);
        inputUbicacion.setEnabled(true);
        inputFechaInicial.setEnabled(true);
        inputFechaFinal.setEnabled(true);
        inputHorometroInicial.setEnabled(true);
        inputHorometroFinal.setEnabled(true);
        inputPrecioAlquiler.setEnabled(true);
        inputHorasMinimas.setEnabled(true);
        inputPrecioHoraExtra.setEnabled(true);
        spinnerMoneda.setEnabled(true);
        adapterAccesorios.setClickEnabled(true);
        adapterDetallesMes.setModoSoloLectura(false);
        if (alquilerActual == null || !alquilerActual.isFinalizado()) {
            btnFinalizarAlquiler.setVisibility(View.VISIBLE);
            btnFinalizarAlquiler.setEnabled(true);
            if (btnGenerarSiguienteMesManual != null) {
                btnGenerarSiguienteMesManual.setVisibility(View.VISIBLE);
                btnGenerarSiguienteMesManual.setEnabled(true);
            }
        }
    }
}
