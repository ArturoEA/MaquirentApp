package com.example.maquirentapp.View;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.maquirentapp.Access.AccesorioSeleccionAdapter;
import com.example.maquirentapp.Model.Accesorio;
import com.example.maquirentapp.Model.AlquilerMensual;
import com.example.maquirentapp.Model.DetalleMes;
import com.example.maquirentapp.Network.FirebaseServicio;
import com.example.maquirentapp.R;
import com.example.maquirentapp.Access.DetalleMesAdapter;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NuevoAlquilerMensualFragment extends Fragment {
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
        adapterAccesorios = new AccesorioSeleccionAdapter();
        recyclerAccesorios.setAdapter(adapterAccesorios);

        recyclerDetallesMes = view.findViewById(R.id.recyclerDetallesMes);
        recyclerDetallesMes.setLayoutManager(new LinearLayoutManager(getContext()));
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

        // Configurar visibilidad inicial
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
                android.R.layout.simple_spinner_item,
                monedas
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMoneda.setAdapter(adapter);

        spinnerMoneda.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                monedaSeleccionada = monedas.get(position);
                tvSimboloMoneda.setText(monedaSeleccionada.equals("USD") ? "$" : "S/.");

                // Recalcular precio hora extra si es necesario
                String precioStr = inputPrecioAlquiler.getText().toString().trim();
                if (!precioStr.isEmpty()) {
                    calcularPrecioHoraExtra();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void configurarListeners() {
        inputPrecioAlquiler.addTextChangedListener(new TextWatcher() {
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
            public void onHorometroChanged(DetalleMes detalle, double nuevoHorometro) {
                calcularHorasExtras(detalle, nuevoHorometro);
                actualizarDetalleMes(detalle);
            }

            @Override
            public void onPagoMesConfirmado(DetalleMes detalle) {
                detalle.setPagoMesConfirmado(true);
                actualizarDetalleMes(detalle);
            }

            @Override
            public void onPagoHEConfirmado(DetalleMes detalle) {
                detalle.setPagoHEConfirmado(true);
                actualizarDetalleMes(detalle);
            }

            @Override
            public void onGenerarValorizacion(DetalleMes detalle) {
                Toast.makeText(getContext(), "Funcionalidad de valorización próximamente", Toast.LENGTH_SHORT).show();
            }
        });

        btnFinalizarAlquiler.setOnClickListener(v -> finalizarAlquiler());
    }

    private void calcularPrecioHoraExtra() {
        String precioStr = inputPrecioAlquiler.getText().toString().trim();
        if (!precioStr.isEmpty()) {
            try {
                double precio = Double.parseDouble(precioStr);
                double precioHoraExtra = (precio / 30.0 / 8.0) * 0.75;
                inputPrecioHoraExtra.setText(String.format(Locale.US, "%.2f", precioHoraExtra));
            } catch (NumberFormatException e) {
            }
        }
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
                inputEmpresa.setText(alquiler.getNombreCliente());
                inputUbicacion.setText(alquiler.getUbicacion());
                inputFechaInicial.setText(formatearFecha(alquiler.getFechaInicial()));
                inputFechaFinal.setText(formatearFecha(alquiler.getFechaFinal()));
                inputHorometroInicial.setText(String.valueOf(alquiler.getHorometroInicial()));
                inputHorometroFinal.setText(String.valueOf(alquiler.getHorometroFinal()));
                inputPrecioAlquiler.setText(String.valueOf(alquiler.getPrecioAlquiler()));
                inputHorasMinimas.setText(String.valueOf(alquiler.getHorasMinimas()));
                inputPrecioHoraExtra.setText(String.valueOf(alquiler.getPrecioHoraExtra()));

                // Configurar moneda
                String moneda = alquiler.getMoneda() != null ? alquiler.getMoneda() : "SOL";
                spinnerMoneda.setSelection(moneda.equals("USD") ? 1 : 0);

                if (alquiler.getAccesoriosIds() != null) {
                    adapterAccesorios.setAccesoriosSeleccionados(alquiler.getAccesoriosIds());
                }

                // Cargar detalles de mes
                cargarDetallesMes();
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(getContext(), "Error al cargar datos: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
                Toast.makeText(getContext(), "Error al cargar detalles: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
        btnFinalizarAlquiler.setEnabled(false);
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
        btnFinalizarAlquiler.setEnabled(true);
        adapterDetallesMes.setModoSoloLectura(false);
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
                    // Generar primer DetalleMes
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
            Date fechaInicio = sdf.parse(alquiler.getFechaInicial());

            Calendar cal = Calendar.getInstance();
            cal.setTime(fechaInicio);

            // Sumar 30 días para obtener fecha fin
            cal.add(Calendar.DAY_OF_MONTH, 30);
            Date fechaFin = cal.getTime();

            DetalleMes primerMes = new DetalleMes();
            primerMes.setIdAlquilerMensual(alquiler.getId());
            primerMes.setNumeroMes(1);
            primerMes.setFechaInicio(sdf.format(fechaInicio));
            primerMes.setFechaFin(sdf.format(fechaFin));

            // Crear título del período
            SimpleDateFormat monthFormat = new SimpleDateFormat("MMMM", new Locale("es", "ES"));
            String mesInicio = monthFormat.format(fechaInicio);
            String mesFin = monthFormat.format(fechaFin);
            primerMes.setTituloPeriodo(mesInicio.substring(0, 1).toUpperCase() + mesInicio.substring(1) +
                    " - " + mesFin.substring(0, 1).toUpperCase() + mesFin.substring(1));

            firebaseServicio.crearDetalleMes(primerMes, new FirebaseServicio.OnDetalleMesCreatedListener() {
                @Override
                public void onSuccess(DetalleMes detalle) {
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
                cargarDetallesMes();
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(getContext(), "Error al actualizar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void finalizarAlquiler() {
        if (alquilerActual == null || alquilerId == null) {
            Toast.makeText(getContext(), "Error: No se puede finalizar el alquiler", Toast.LENGTH_SHORT).show();
            return;
        }

        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Finalizar alquiler")
                .setMessage("¿Está seguro de que desea finalizar este alquiler? " +
                        "Se tomarán los datos del último mes registrado para completar la información.")
                .setPositiveButton("Finalizar", (dialog, which) -> {
                    ejecutarFinalizacion();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void ejecutarFinalizacion() {
        firebaseServicio.getUltimoDetalleMes(alquilerId,
                new FirebaseServicio.OnDetalleMesLoadedListener() {
                    @Override
                    public void onSuccess(DetalleMes ultimoDetalle) {
                        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

                        try {
                            alquilerActual.setFechaFinal(ultimoDetalle.getFechaFin());

                            alquilerActual.setHorometroFinal(ultimoDetalle.getHorometro());

                            alquilerActual.setFinalizado(true);

                            firebaseServicio.actualizarAlquilerMensual(alquilerActual,
                                    new FirebaseServicio.OnAlquilerUpdatedListener() {
                                        @Override
                                        public void onSuccess() {
                                            Toast.makeText(getContext(),
                                                    "Alquiler finalizado correctamente",
                                                    Toast.LENGTH_SHORT).show();

                                            inputFechaFinal.setText(ultimoDetalle.getFechaFin());
                                            inputHorometroFinal.setText(String.valueOf(ultimoDetalle.getHorometro()));
                                            btnFinalizarAlquiler.setEnabled(false);
                                            btnFinalizarAlquiler.setAlpha(0.5f);
                                            btnFinalizarAlquiler.setText("Alquiler finalizado");

                                            requireActivity().onBackPressed();
                                        }

                                        @Override
                                        public void onError(Exception e) {
                                            Toast.makeText(getContext(),
                                                    "Error al finalizar: " + e.getMessage(),
                                                    Toast.LENGTH_LONG).show();
                                        }
                                    });

                        } catch (Exception e) {
                            Toast.makeText(getContext(),
                                    "Error al procesar datos de finalización",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onError(Exception e) {
                        Toast.makeText(getContext(),
                                "No se puede finalizar: No hay meses registrados",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }
    private void calcularHorasExtras(DetalleMes detalle, double horometroActual) {
        if (alquilerActual == null) return;

        // Obtener horómetro anterior
        if (detalle.getNumeroMes() == 1) {
            // Primer mes: usar horómetro inicial del alquiler
            double horometroAnterior = alquilerActual.getHorometroInicial();
            calcularYActualizarHorasExtras(detalle, horometroAnterior, horometroActual);
        } else {
            // Mes posterior: obtener horómetro del mes anterior
            firebaseServicio.getDetalleMesPorNumero(
                    alquilerActual.getId(),
                    detalle.getNumeroMes() - 1,
                    new FirebaseServicio.OnDetalleMesLoadedListener() {
                        @Override
                        public void onSuccess(DetalleMes mesAnterior) {
                            double horometroAnterior = mesAnterior.getHorometro();
                            calcularYActualizarHorasExtras(detalle, horometroAnterior, horometroActual);
                        }

                        @Override
                        public void onError(Exception e) {
                            Toast.makeText(getContext(),
                                    "Error al obtener datos del mes anterior",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
            );
        }
    }
    private void calcularYActualizarHorasExtras(DetalleMes detalle,
                                                double horometroAnterior,
                                                double horometroActual) {
        // Calcular horas trabajadas
        double horasTrabajadas = horometroActual - horometroAnterior;

        // Calcular horas extras
        double horasMinimas = alquilerActual.getHorasMinimas();
        double horasExtras = Math.max(0, horasTrabajadas - horasMinimas);

        detalle.setHorometro(horometroActual);
        detalle.setHorasExtras(horasExtras);

        // Calcular precio de horas extras
        double precioHE = horasExtras * alquilerActual.getPrecioHoraExtra();
        detalle.setPrecioHorasExtras(precioHE);

        // Refrescar el adapter para mostrar los cambios
        cargarDetallesMes();
    }
    private void configureGlobalFab() {
        View hostView = getView();
        if (hostView == null) return;

        if (modoSoloLectura && !editandoActualmente) {
            configurarFabEditar();
        } else {
            configurarFabGuardar();
        }
    }

    private void configurarFabEditar() {
        if (getActivity() instanceof com.example.maquirentapp.MainActivity) {
            com.example.maquirentapp.MainActivity main = (com.example.maquirentapp.MainActivity) getActivity();
            main.showGlobalFab("Editar", R.drawable.icon_editar_blanco, v -> {
                editandoActualmente = true;
                habilitarCampos();
                configureGlobalFab();
            });
        } else {
            View activityFab = getActivity() != null ? getActivity().findViewById(R.id.btnGlobal) : null;
            if (activityFab instanceof ExtendedFloatingActionButton) {
                ExtendedFloatingActionButton fab = (ExtendedFloatingActionButton) activityFab;
                fab.setText("Editar");
                try {
                    fab.setIcon(ContextCompat.getDrawable(requireContext(), R.drawable.icon_editar_blanco));
                } catch (Exception ignored) {}
                fab.setOnClickListener(v -> {
                    editandoActualmente = true;
                    habilitarCampos();
                    configureGlobalFab();
                });
                fab.setVisibility(View.VISIBLE);
            }
        }
    }

    private void configurarFabGuardar() {
        if (getActivity() instanceof com.example.maquirentapp.MainActivity) {
            com.example.maquirentapp.MainActivity main = (com.example.maquirentapp.MainActivity) getActivity();
            main.showGlobalFab("Guardar", R.drawable.icon_guardar_blanco, v -> guardarAlquilerMensual());
        } else {
            View activityFab = getActivity() != null ? getActivity().findViewById(R.id.btnGlobal) : null;
            if (activityFab instanceof ExtendedFloatingActionButton) {
                ExtendedFloatingActionButton fab = (ExtendedFloatingActionButton) activityFab;
                fab.setText("Guardar");
                try {
                    fab.setIcon(ContextCompat.getDrawable(requireContext(), R.drawable.icon_guardar_blanco));
                } catch (Exception ignored) {}
                fab.setOnClickListener(v -> guardarAlquilerMensual());
                fab.setVisibility(View.VISIBLE);
            }
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
        if (getActivity() instanceof com.example.maquirentapp.MainActivity) {
            ((com.example.maquirentapp.MainActivity) getActivity()).hideGlobalFab();
        } else {
            View activityFab = getActivity() != null ? getActivity().findViewById(R.id.btnGlobal) : null;
            if (activityFab != null) activityFab.setVisibility(View.GONE);
        }
    }
}