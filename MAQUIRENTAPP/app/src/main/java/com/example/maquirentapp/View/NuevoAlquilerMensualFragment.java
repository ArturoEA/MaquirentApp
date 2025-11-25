package com.example.maquirentapp.View;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
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
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.maquirentapp.Access.AccesorioSeleccionAdapter;
import com.example.maquirentapp.Access.DetalleMesAdapter;
import com.example.maquirentapp.MainActivity; // Importar MainActivity
import com.example.maquirentapp.Model.Accesorio;
import com.example.maquirentapp.Model.AlquilerMensual;
import com.example.maquirentapp.Model.DetalleMes;
import com.example.maquirentapp.Model.GrupoElectrogeno;
import com.example.maquirentapp.Model.Ingreso;
import com.example.maquirentapp.Network.FirebaseServicio;
import com.example.maquirentapp.R;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

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
    private Button btnFinalizarAlquiler;

    private AccesorioSeleccionAdapter adapterAccesorios;
    private DetalleMesAdapter adapterDetallesMes;
    private FirebaseServicio firebaseServicio;

    private View llAccesoriosHeader;
    private ImageView ivAccChevron;
    private View accBody;
    private boolean accesoriosExpanded = true;

    private String monedaSeleccionada = "SOL";
    private AlquilerMensual alquilerActual;
    private FirebaseAuth firebaseAuth;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            idGrupo = getArguments().getString("idGrupo");
            alquilerId = getArguments().getString("alquilerId");
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

        firebaseServicio = new FirebaseServicio();
        firebaseAuth = FirebaseAuth.getInstance();

        inicializarVistas(view);
        configurarSpinnerMoneda();
        configurarDatePickers();
        configurarListeners();
        cargarAccesoriosMensuales();

        if (modoEdicion) {
            cargarDatosAlquiler();
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

        recyclerAccesorios = view.findViewById(R.id.recyclerAccesorios);
        recyclerAccesorios.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerAccesorios.setNestedScrollingEnabled(false);
        adapterAccesorios = new AccesorioSeleccionAdapter();
        recyclerAccesorios.setAdapter(adapterAccesorios);

        recyclerDetallesMes = view.findViewById(R.id.recyclerDetallesMes);
        recyclerDetallesMes.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerDetallesMes.setNestedScrollingEnabled(false);
        adapterDetallesMes = new DetalleMesAdapter();
        recyclerDetallesMes.setAdapter(adapterDetallesMes);

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
            recyclerDetallesMes.setVisibility(View.VISIBLE);
            btnFinalizarAlquiler.setVisibility(View.VISIBLE);
        }
    }

    private void configurarSpinnerMoneda() {
        List<String> monedas = new ArrayList<>();
        monedas.add("SOL");
        monedas.add("USD");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                R.layout.spinner_item_white,
                monedas
        );
        adapter.setDropDownViewResource(R.layout.spinner_item_white);
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

        adapterDetallesMes.setOnDetalleMesListener(new DetalleMesAdapter.OnDetalleMesListener() {
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
                    double precioHoraExtra = (precio / horas) * 0.75;
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
        firebaseServicio.registrarIngreso(ingreso, new FirebaseServicio.OnIngresoRegistradoListener() {
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

            double horasTrabajadas = horometroActual - horometroAnterior;

            double horasPermitidasDelPeriodo;

            long diasPeriodo = calcularDiasEntreFechas(detalle.getFechaInicio(), detalle.getFechaFin());

            if (diasPeriodo >= 30) {
                horasPermitidasDelPeriodo = alquilerActual.getHorasMinimas();
            } else {
                horasPermitidasDelPeriodo = (alquilerActual.getHorasMinimas() / 30.0) * diasPeriodo;
            }

            double horasExtras = Math.max(0, horasTrabajadas - horasPermitidasDelPeriodo);

            detalle.setHorometro(horometroActual);
            detalle.setHorasExtras(horasExtras);

            double precioHE = horasExtras * alquilerActual.getPrecioHoraExtra();
            detalle.setPrecioHorasExtras(precioHE);

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
        for (DetalleMes detalle : adapterDetallesMes.getItems()) {
            if (detalle.getNumeroMes() == numeroMesAnterior) {
                return detalle.getHorometro();
            }
        }
        return 0;
    }

    private void configurarDatePickers() {
        inputFechaInicial.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    requireContext(),
                    (view1, year, month, dayOfMonth) -> {
                        String fecha = String.format(Locale.US, "%02d/%02d/%d", dayOfMonth, month + 1, year);
                        inputFechaInicial.setText(fecha);
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );
            datePickerDialog.show();
        });

        inputFechaFinal.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    requireContext(),
                    (view1, year, month, dayOfMonth) -> {
                        String fecha = String.format(Locale.US, "%02d/%02d/%d", dayOfMonth, month + 1, year);
                        inputFechaFinal.setText(fecha);
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );
            datePickerDialog.show();
        });
    }

    private void cargarAccesoriosMensuales() {
        firebaseServicio.getAccesorios("mensual", new FirebaseServicio.OnAccesoriosLoadedListener() {
            @Override
            public void onSuccess(List<Accesorio> accesorios) {
                adapterAccesorios.setItems(accesorios);
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(getContext(), "Error al cargar accesorios: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void cargarDatosAlquiler() {
        firebaseServicio.getAlquilerMensualPorId(alquilerId, new FirebaseServicio.OnAlquilerMensualLoadedListener() {
            @Override
            public void onSuccess(AlquilerMensual alquiler) {
                alquilerActual = alquiler;
                idGrupo = alquiler.getIdGrupo();
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

                adapterDetallesMes.setPrecioMensualBase(alquiler.getPrecioAlquiler());

                cargarDetallesMes();

                if (alquiler.isFinalizado()) {
                    deshabilitarCampos();
                    hideGlobalFab();
                } else {
                    configureGlobalFab();
                }
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(getContext(), "Error al cargar alquiler", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void cargarDetallesMes() {
        if (alquilerId == null) return;
        firebaseServicio.getDetallesMesPorAlquiler(alquilerId, new FirebaseServicio.OnDetallesMesLoadedListener() {
            @Override
            public void onSuccess(List<DetalleMes> detalles) {
                adapterDetallesMes.setItems(detalles);
                adapterDetallesMes.setModoSoloLectura(modoSoloLectura && !editandoActualmente);
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(getContext(), "Error al cargar detalles", Toast.LENGTH_SHORT).show();
            }
        });
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

        if (firebaseAuth.getCurrentUser() != null) {
            alquiler.setAdminUid(firebaseAuth.getCurrentUser().getUid());
        }

        if (modoEdicion) {
            firebaseServicio.actualizarAlquilerMensual(alquiler, new FirebaseServicio.OnAlquilerUpdatedListener() {
                @Override
                public void onSuccess() {
                    Toast.makeText(getContext(), "Alquiler actualizado correctamente", Toast.LENGTH_SHORT).show();
                    requireActivity().onBackPressed();
                }

                @Override
                public void onError(Exception e) {
                    Toast.makeText(getContext(), "Error al actualizar: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        } else {
            firebaseServicio.crearAlquilerMensual(alquiler, new FirebaseServicio.OnAlquilerCreatedListener() {
                @Override
                public void onSuccess(AlquilerMensual alquilerCreado) {
                    generarPrimerDetalleMes(alquilerCreado);
                    Toast.makeText(getContext(), "Alquiler registrado correctamente", Toast.LENGTH_SHORT).show();
                    requireActivity().onBackPressed();
                }

                @Override
                public void onError(Exception e) {
                    Toast.makeText(getContext(), "Error al guardar: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    private void generarPrimerDetalleMes(AlquilerMensual alquiler) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Date fechaInicioDate = sdf.parse(alquiler.getFechaInicial());
            if (fechaInicioDate == null) return;

            Calendar calInicio = Calendar.getInstance();
            calInicio.setTime(fechaInicioDate);

            Calendar calFin = (Calendar) calInicio.clone();
            calFin.add(Calendar.DAY_OF_YEAR, 29);

            Date fechaFinDate = calFin.getTime();

            String strFechaInicio = sdf.format(fechaInicioDate);
            String strFechaFin = sdf.format(fechaFinDate);

            DetalleMes primerMes = new DetalleMes();
            primerMes.setIdAlquilerMensual(alquiler.getId());
            primerMes.setNumeroMes(1);
            primerMes.setFechaInicio(strFechaInicio);
            primerMes.setFechaFin(strFechaFin);
            // Precio full por defecto
            primerMes.setMontoMes(alquiler.getPrecioAlquiler());
            primerMes.setTituloPeriodo("Mes 1: " + strFechaInicio + " - " + strFechaFin);

            firebaseServicio.crearDetalleMes(primerMes, new FirebaseServicio.OnDetalleMesCreatedListener() {
                @Override
                public void onSuccess(DetalleMes detalle) {
                    Log.i("NuevoAlquiler", "Primer detalle de mes creado.");
                }

                @Override
                public void onError(Exception e) {
                    Toast.makeText(getContext(), "Error al crear detalle de mes: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });

        } catch (ParseException e) {
            Toast.makeText(getContext(), "Error al procesar fechas", Toast.LENGTH_SHORT).show();
        }
    }

    private void actualizarDetalleMes(DetalleMes detalle) {
        firebaseServicio.actualizarDetalleMes(detalle, new FirebaseServicio.OnDetalleMesUpdatedListener() {
            @Override
            public void onSuccess() {
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(getContext(), "Error al actualizar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void mostrarDialogoConfirmarEnvio() {
        if (alquilerActual == null) return;

        new AlertDialog.Builder(requireContext())
                .setTitle("Finalizar Alquiler")
                .setMessage("Estás a punto de finalizar este alquiler. Una vez finalizado, no podrás editar los datos.\n\nSe enviará un código a tu correo para confirmar.")
                .setPositiveButton("Enviar Código", (dialog, which) -> {
                    btnFinalizarAlquiler.setEnabled(false);
                    btnFinalizarAlquiler.setText("Enviando...");

                    firebaseServicio.solicitarCodigoFinalizacion(
                            alquilerActual.getId(),
                            alquilerActual.getNombreCliente(),
                            new FirebaseServicio.OnSimpleCallback() {
                                @Override
                                public void onSuccess() {
                                    Toast.makeText(getContext(), "Código enviado a tu correo", Toast.LENGTH_SHORT).show();
                                    btnFinalizarAlquiler.setEnabled(true);
                                    btnFinalizarAlquiler.setText("Finalizar alquiler");
                                    mostrarDialogoIngresarCodigo();
                                }

                                @Override
                                public void onError(Exception e) {
                                    Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
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

        new AlertDialog.Builder(requireContext())
                .setTitle("Ingresa el Código")
                .setMessage("Revisa tu correo e ingresa el código de 6 dígitos para finalizar el alquiler.")
                .setView(container)
                .setPositiveButton("Finalizar", (dialog, which) -> {
                    String codigo = inputCodigo.getText().toString().trim();
                    if (codigo.length() == 6) {
                        confirmarFinalizacion(codigo);
                    } else {
                        Toast.makeText(getContext(), "El código debe tener 6 dígitos", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void confirmarFinalizacion(String codigo) {
        String fechaFinalManual = inputFechaFinal.getText().toString().trim();
        String horometroFinalManualStr = inputHorometroFinal.getText().toString().trim();

        if (fechaFinalManual.isEmpty() || horometroFinalManualStr.isEmpty()) {
            Toast.makeText(getContext(), "Fecha Final y Horómetro Final son obligatorios.", Toast.LENGTH_LONG).show();
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
            Toast.makeText(getContext(), "Horómetro final manual inválido", Toast.LENGTH_SHORT).show();
            return;
        }

        if (alquilerActual == null) {
            Toast.makeText(getContext(), "Error: No se pudo encontrar el alquiler", Toast.LENGTH_SHORT).show();
            return;
        }

        alquilerActual.setHorometroFinal(horometroFinal);
        alquilerActual.setFechaFinal(fechaFinal);

        firebaseServicio.confirmarFinalizacion(
                alquilerActual.getId(),
                codigo,
                horometroFinal,
                fechaFinal,
                new FirebaseServicio.OnSimpleCallback() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(getContext(), "Alquiler Finalizado", Toast.LENGTH_LONG).show();
                        alquilerActual.setFinalizado(true);
                        deshabilitarCampos();
                        hideGlobalFab();
                    }

                    @Override
                    public void onError(Exception e) {
                        Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }
        );
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
        btnFinalizarAlquiler.setVisibility(View.GONE);
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
        }
    }
}