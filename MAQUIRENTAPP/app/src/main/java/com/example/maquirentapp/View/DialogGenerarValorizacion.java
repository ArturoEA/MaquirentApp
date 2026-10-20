package com.example.maquirentapp.View;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.DialogFragment;

import com.example.maquirentapp.Model.AlquilerMensual;
import com.example.maquirentapp.Model.ClienteValorizacion;
import com.example.maquirentapp.Model.DetalleMes;
import com.example.maquirentapp.Model.GrupoElectrogeno;
import com.example.maquirentapp.Model.InfoPlaca;
import com.example.maquirentapp.Model.ItemValorizacion;
import com.example.maquirentapp.Model.Valorizacion;
import com.example.maquirentapp.R;
import com.example.maquirentapp.Repository.Callback;
import com.example.maquirentapp.Repository.GeneradorRepository;
import com.example.maquirentapp.Repository.ValorizacionesRepository;
import com.example.maquirentapp.Utils.CalendarioTareoHelper;
import com.example.maquirentapp.Utils.NumeroALetras;
import com.example.maquirentapp.strategy.CalculadoraAlquilerStrategy;
import com.example.maquirentapp.strategy.StrategyProvider;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DialogGenerarValorizacion extends DialogFragment {

    public interface OnValorizacionGeneradaListener {
        void onValorizacionGenerada(Valorizacion valorizacion, File archivoPdf);
    }

    private static final String ARG_ALQUILER = "arg_alquiler";
    private static final String ARG_DETALLE = "arg_detalle";
    private static final String ARG_H_INICIAL = "arg_h_inicial";

    private AlquilerMensual alquiler;
    private DetalleMes detalle;
    private double horometroInicialCalculado = 0.0;

    private OnValorizacionGeneradaListener listener;

    private ValorizacionesRepository valorizacionesRepository;
    private GeneradorRepository generadorRepository;
    private CalculadoraAlquilerStrategy calculadoraStrategy;

    // Vistas
    private androidx.appcompat.widget.Toolbar toolbarDialog;
    private TextView tvCliente, tvRuc, tvEquipo, tvPeriodo, tvLugarTrabajo;
    private RadioGroup rgModalidad;
    private RadioButton rbSinHorasExtras, rbConHorasExtras;
    private View cardHorasExtras;
    private TextInputEditText inputHorometroInicial, inputHorometroFinal, inputHorasPermitidas;
    private TextView tvHorasUsadas, tvHorasExtras, tvTarifaHoraExtra, tvTotalHorasExtras;

    private TextView tvDiasValorizados, tvCostoDiario, tvCostoParcial;
    private View rowResumenHorasExtras, rowSubtotal;
    private TextView tvResumenHorasExtras, tvSubtotal, tvLabelIgv, tvIgv, tvTotalFinal, tvTotalEnLetras;
    private Button btnGenerarValorizacion;
    private ProgressBar progressGenerar;

    // Variables de cálculo
    private int diasCalculados = 30;
    private double precioMesContrato = 0.0;
    private double costoDiario = 0.0;
    private double costoParcial = 0.0;
    private double horasUsadas = 0.0;
    private double horasExtras = 0.0;
    private double tarifaHoraExtra = 0.0;
    private double totalImporteHorasExtras = 0.0;
    private double subtotal = 0.0;
    private double igv = 0.0;
    private double totalFinal = 0.0;
    private String textoTotalEnLetras = "";

    private String rucCliente = "";
    private String direccionCliente = "";
    private String marcaGrupo = "MODASA";
    private String descripcionGrupo = "";
    private String codigoGrupo = "";

    public static DialogGenerarValorizacion newInstance(AlquilerMensual alquiler, DetalleMes detalle, double horometroInicial) {
        DialogGenerarValorizacion dialog = new DialogGenerarValorizacion();
        Bundle args = new Bundle();
        args.putSerializable(ARG_ALQUILER, alquiler);
        args.putString("idDetalle", detalle != null ? detalle.getId() : "");
        args.putString("fechaInicio", detalle != null ? detalle.getFechaInicio() : (alquiler != null ? alquiler.getFechaInicial() : ""));
        args.putString("fechaFin", detalle != null ? detalle.getFechaFin() : (alquiler != null ? alquiler.getFechaFinal() : ""));
        args.putInt("numeroMes", detalle != null ? detalle.getNumeroMes() : 1);
        args.putDouble("horometroDetalle", detalle != null ? detalle.getHorometro() : 0.0);
        args.putDouble("montoMesDetalle", detalle != null ? detalle.getMontoMes() : 0.0);
        args.putDouble("horasExtrasDetalle", detalle != null ? detalle.getHorasExtras() : 0.0);
        args.putDouble(ARG_H_INICIAL, horometroInicial);
        dialog.setArguments(args);
        return dialog;
    }

    public void setOnValorizacionGeneradaListener(OnValorizacionGeneradaListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, android.R.style.Theme_Material_Light_NoActionBar_Fullscreen);
        valorizacionesRepository = new ValorizacionesRepository();
        generadorRepository = new GeneradorRepository();
        calculadoraStrategy = StrategyProvider.getCalculadoraStrategy();

        if (getArguments() != null) {
            alquiler = (AlquilerMensual) getArguments().getSerializable(ARG_ALQUILER);
            horometroInicialCalculado = getArguments().getDouble(ARG_H_INICIAL, 0.0);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_generar_valorizacion, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupToolbar();
        cargarDatosGenerales();
        setupListeners();
        calcularTodo();
    }

    private void initViews(View v) {
        toolbarDialog = v.findViewById(R.id.toolbarDialog);
        tvCliente = v.findViewById(R.id.tvCliente);
        tvRuc = v.findViewById(R.id.tvRuc);
        tvEquipo = v.findViewById(R.id.tvEquipo);
        tvPeriodo = v.findViewById(R.id.tvPeriodo);
        tvLugarTrabajo = v.findViewById(R.id.tvLugarTrabajo);

        rgModalidad = v.findViewById(R.id.rgModalidad);
        rbSinHorasExtras = v.findViewById(R.id.rbSinHorasExtras);
        rbConHorasExtras = v.findViewById(R.id.rbConHorasExtras);

        cardHorasExtras = v.findViewById(R.id.cardHorasExtras);
        inputHorometroInicial = v.findViewById(R.id.inputHorometroInicial);
        inputHorometroFinal = v.findViewById(R.id.inputHorometroFinal);
        inputHorasPermitidas = v.findViewById(R.id.inputHorasPermitidas);

        tvHorasUsadas = v.findViewById(R.id.tvHorasUsadas);
        tvHorasExtras = v.findViewById(R.id.tvHorasExtras);
        tvTarifaHoraExtra = v.findViewById(R.id.tvTarifaHoraExtra);
        tvTotalHorasExtras = v.findViewById(R.id.tvTotalHorasExtras);

        tvDiasValorizados = v.findViewById(R.id.tvDiasValorizados);
        tvCostoDiario = v.findViewById(R.id.tvCostoDiario);
        tvCostoParcial = v.findViewById(R.id.tvCostoParcial);

        rowResumenHorasExtras = v.findViewById(R.id.rowResumenHorasExtras);
        rowSubtotal = v.findViewById(R.id.rowSubtotal);
        tvResumenHorasExtras = v.findViewById(R.id.tvResumenHorasExtras);
        tvSubtotal = v.findViewById(R.id.tvSubtotal);

        tvLabelIgv = v.findViewById(R.id.tvLabelIgv);
        tvIgv = v.findViewById(R.id.tvIgv);
        tvTotalFinal = v.findViewById(R.id.tvTotalFinal);
        tvTotalEnLetras = v.findViewById(R.id.tvTotalEnLetras);

        btnGenerarValorizacion = v.findViewById(R.id.btnGenerarValorizacion);
        progressGenerar = v.findViewById(R.id.progressGenerar);

        if (calculadoraStrategy != null) {
            tvLabelIgv.setText(calculadoraStrategy.getEtiquetaImpuesto() + ":");
        }
    }

    private void setupToolbar() {
        toolbarDialog.setNavigationOnClickListener(v -> dismiss());
    }

    private void cargarDatosGenerales() {
        if (alquiler == null) return;

        Bundle args = getArguments();
        String fechaInicio = args != null ? args.getString("fechaInicio", "") : "";
        String fechaFin = args != null ? args.getString("fechaFin", "") : "";
        double montoMesDetalle = args != null ? args.getDouble("montoMesDetalle", 0.0) : 0.0;
        double horometroDetalle = args != null ? args.getDouble("horometroDetalle", 0.0) : 0.0;
        double horasExtrasDetalle = args != null ? args.getDouble("horasExtrasDetalle", 0.0) : 0.0;

        // Días entre fechas usando helper
        CalendarioTareoHelper.TareoInfo tareo = CalendarioTareoHelper.generarTareo(fechaInicio, fechaFin);
        diasCalculados = tareo.getTotalDias();
        if (diasCalculados <= 0) diasCalculados = 30;

        tvPeriodo.setText("Periodo: " + fechaInicio + " al " + fechaFin + " (" + diasCalculados + " días)");
        tvCliente.setText("Cliente: " + (alquiler.getNombreCliente() != null ? alquiler.getNombreCliente() : "-"));
        tvLugarTrabajo.setText("Lugar: " + (alquiler.getUbicacion() != null ? alquiler.getUbicacion() : "-"));

        // Precio mensual base
        if (montoMesDetalle > 0) {
            precioMesContrato = montoMesDetalle;
        } else if (alquiler.getPrecioAlquiler() > 0) {
            precioMesContrato = alquiler.getPrecioAlquiler();
        } else {
            precioMesContrato = 0.0;
        }

        // Horómetro inicial y final
        inputHorometroInicial.setText(String.format(Locale.US, "%.1f", horometroInicialCalculado));

        if (horometroDetalle > 0) {
            inputHorometroFinal.setText(String.format(Locale.US, "%.1f", horometroDetalle));
        }

        int horasMinimas = alquiler.getHorasMinimas() > 0 ? alquiler.getHorasMinimas() : 200;
        inputHorasPermitidas.setText(String.valueOf(horasMinimas));

        // Si ya tenía horas extras guardadas o el horómetro final es mayor, seleccionar modo horas extras
        if (horasExtrasDetalle > 0 || (horometroDetalle > horometroInicialCalculado && horometroInicialCalculado > 0)) {
            rbConHorasExtras.setChecked(true);
            cardHorasExtras.setVisibility(View.VISIBLE);
        } else {
            rbSinHorasExtras.setChecked(true);
            cardHorasExtras.setVisibility(View.GONE);
        }

        // Cargar código del equipo y especificaciones de placa
        if (alquiler.getIdGrupo() != null && !alquiler.getIdGrupo().isEmpty()) {
            generadorRepository.getGrupoPorId(alquiler.getIdGrupo(), new Callback<GrupoElectrogeno>() {
                @Override
                public void onSuccess(GrupoElectrogeno grupo) {
                    codigoGrupo = grupo != null && grupo.getCodigo() != null ? grupo.getCodigo() : "GEP";
                    descripcionGrupo = "Grupo electrógeno | " + codigoGrupo;
                    tvEquipo.setText("Equipo: " + descripcionGrupo);

                    generadorRepository.getInfoPlaca(alquiler.getIdGrupo(), new Callback<InfoPlaca>() {
                        @Override
                        public void onSuccess(InfoPlaca placa) {
                            if (placa != null) {
                                if (placa.getMarcaGrupo() != null && !placa.getMarcaGrupo().isEmpty()) {
                                    marcaGrupo = placa.getMarcaGrupo();
                                }
                                String pot = placa.getPotenciaStandBy() != null ? placa.getPotenciaStandBy() : "";
                                if (!pot.isEmpty()) {
                                    descripcionGrupo = "Grupo electrógeno " + pot + " | " + codigoGrupo;
                                    tvEquipo.setText("Equipo: " + descripcionGrupo);
                                }
                            }
                        }

                        @Override
                        public void onError(Exception e) { }
                    });
                }

                @Override
                public void onError(Exception e) {
                    tvEquipo.setText("Equipo: Grupo electrógeno");
                }
            });
        }
    }

    private void setupListeners() {
        rgModalidad.setOnCheckedChangeListener((group, checkedId) -> {
            boolean conExtras = (checkedId == R.id.rbConHorasExtras);
            cardHorasExtras.setVisibility(conExtras ? View.VISIBLE : View.GONE);
            rowResumenHorasExtras.setVisibility(conExtras ? View.VISIBLE : View.GONE);
            rowSubtotal.setVisibility(conExtras ? View.VISIBLE : View.GONE);
            calcularTodo();
        });

        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int count, int after) { calcularTodo(); }
            @Override public void afterTextChanged(Editable s) { }
        };

        inputHorometroFinal.addTextChangedListener(watcher);
        inputHorasPermitidas.addTextChangedListener(watcher);

        btnGenerarValorizacion.setOnClickListener(v -> validarYGenerar());
    }

    private void calcularTodo() {
        String monedaSimbolo = (alquiler != null && "USD".equalsIgnoreCase(alquiler.getMoneda())) ? "US$" : "S/.";

        // 1. Días y costo diario
        if (diasCalculados <= 0) diasCalculados = 30;
        tvDiasValorizados.setText(diasCalculados + " días");

        // Regla: si el precioMesContrato es para 30 días, costo diario = precioMesContrato / 30
        costoDiario = precioMesContrato / 30.0;
        costoParcial = costoDiario * diasCalculados;
        costoParcial = Math.round(costoParcial * 100.0) / 100.0;

        tvCostoDiario.setText(String.format(Locale.US, "%s %.2f", monedaSimbolo, costoDiario));
        tvCostoParcial.setText(String.format(Locale.US, "%s %.2f", monedaSimbolo, costoParcial));

        boolean conExtras = rbConHorasExtras.isChecked();

        if (conExtras) {
            double hFinal = 0.0;
            try {
                String strFinal = inputHorometroFinal.getText() != null ? inputHorometroFinal.getText().toString().trim() : "";
                if (!strFinal.isEmpty()) hFinal = Double.parseDouble(strFinal);
            } catch (Exception ignored) { }

            int horasPermitidas = 200;
            try {
                String strPerm = inputHorasPermitidas.getText() != null ? inputHorasPermitidas.getText().toString().trim() : "";
                if (!strPerm.isEmpty()) horasPermitidas = Integer.parseInt(strPerm);
            } catch (Exception ignored) { }

            // Horas usadas = Final - Inicial
            horasUsadas = Math.max(0.0, hFinal - horometroInicialCalculado);
            horasUsadas = Math.round(horasUsadas * 10.0) / 10.0;

            // Horas permitidas prorrateadas según días
            double horasPermitidasPeriodo;
            if (diasCalculados >= 30) {
                horasPermitidasPeriodo = horasPermitidas;
            } else {
                horasPermitidasPeriodo = (horasPermitidas / 30.0) * diasCalculados;
            }

            horasExtras = Math.max(0.0, horasUsadas - horasPermitidasPeriodo);
            horasExtras = Math.round(horasExtras * 10.0) / 10.0;

            // Tarifa de hora extra: si el contrato ya tiene una definida, o la regla del 25% desc:
            if (alquiler != null && alquiler.getPrecioHoraExtra() > 0) {
                tarifaHoraExtra = alquiler.getPrecioHoraExtra();
            } else {
                tarifaHoraExtra = calculadoraStrategy.calcularPrecioHoraExtra(precioMesContrato, horasPermitidas);
            }

            totalImporteHorasExtras = horasExtras * tarifaHoraExtra;
            totalImporteHorasExtras = Math.round(totalImporteHorasExtras * 100.0) / 100.0;

            tvHorasUsadas.setText(String.format(Locale.US, "%.1f hrs", horasUsadas));
            tvHorasExtras.setText(String.format(Locale.US, "%.1f hrs", horasExtras));
            tvTarifaHoraExtra.setText(String.format(Locale.US, "%s %.2f", monedaSimbolo, tarifaHoraExtra));
            tvTotalHorasExtras.setText(String.format(Locale.US, "%s %.2f", monedaSimbolo, totalImporteHorasExtras));

            tvResumenHorasExtras.setText(String.format(Locale.US, "%s %.2f", monedaSimbolo, totalImporteHorasExtras));
            subtotal = costoParcial + totalImporteHorasExtras;
            subtotal = Math.round(subtotal * 100.0) / 100.0;
            tvSubtotal.setText(String.format(Locale.US, "%s %.2f", monedaSimbolo, subtotal));

        } else {
            horasUsadas = 0.0;
            horasExtras = 0.0;
            totalImporteHorasExtras = 0.0;
            subtotal = costoParcial;
        }

        // Impuestos y Total
        igv = calculadoraStrategy.calcularImpuesto(subtotal);
        totalFinal = calculadoraStrategy.calcularTotalConImpuesto(subtotal);

        tvIgv.setText(String.format(Locale.US, "%s %.2f", monedaSimbolo, igv));
        tvTotalFinal.setText(String.format(Locale.US, "%s %.2f", monedaSimbolo, totalFinal));

        textoTotalEnLetras = NumeroALetras.convertir(totalFinal, alquiler != null ? alquiler.getMoneda() : "SOL");
        tvTotalEnLetras.setText(textoTotalEnLetras);
    }

    private void validarYGenerar() {
        boolean conExtras = rbConHorasExtras.isChecked();

        double hFinal = 0.0;
        if (conExtras) {
            String strFinal = inputHorometroFinal.getText() != null ? inputHorometroFinal.getText().toString().trim() : "";
            if (strFinal.isEmpty()) {
                inputHorometroFinal.setError("Ingresa el horómetro final");
                inputHorometroFinal.requestFocus();
                return;
            }
            try {
                hFinal = Double.parseDouble(strFinal);
            } catch (Exception e) {
                inputHorometroFinal.setError("Número inválido");
                return;
            }

            if (hFinal < horometroInicialCalculado) {
                inputHorometroFinal.setError("No puede ser menor que el horómetro inicial (" + horometroInicialCalculado + ")");
                return;
            }
        }

        btnGenerarValorizacion.setEnabled(false);
        btnGenerarValorizacion.setText("Generando...");
        progressGenerar.setVisibility(View.VISIBLE);

        Bundle args = getArguments();
        String idDetalle = args != null ? args.getString("idDetalle", "") : "";
        String fechaInicio = args != null ? args.getString("fechaInicio", "") : "";
        String fechaFin = args != null ? args.getString("fechaFin", "") : "";
        int numeroMes = args != null ? args.getInt("numeroMes", 1) : 1;

        // Construir modelo Valorización
        Valorizacion val = new Valorizacion();
        val.setConHorasExtras(conExtras);
        val.setIdAlquiler(alquiler != null ? alquiler.getId() : "");
        val.setIdDetalleMes(idDetalle);
        val.setNumeroMes(numeroMes);
        val.setNombreCliente(alquiler != null ? alquiler.getNombreCliente() : "");
        val.setClienteRuc(rucCliente);
        val.setClienteDireccion(direccionCliente);
        val.setUbicacionTrabajo(alquiler != null && alquiler.getUbicacion() != null ? alquiler.getUbicacion() : "-");
        val.setFechaInicio(fechaInicio);
        val.setFechaFin(fechaFin);
        val.setFechaEmision(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date()));
        val.setMoneda(alquiler != null ? alquiler.getMoneda() : "SOL");

        val.setCodigoEquipo(codigoGrupo);
        val.setNumeroValorizacion(StrategyProvider.getCorrelativoStrategy().generarCodigoValorizacion(codigoGrupo, fechaInicio));
        val.setDescripcionEquipo(descripcionGrupo);
        val.setMarcaEquipo(marcaGrupo);

        val.setDiasValorizados(diasCalculados);
        val.setCostoDiario(costoDiario);
        val.setCostoParcial(costoParcial);

        if (conExtras) {
            val.setHorometroInicial(horometroInicialCalculado);
            val.setHorometroFinal(hFinal);
            int horasPerm = 200;
            try { horasPerm = Integer.parseInt(inputHorasPermitidas.getText().toString().trim()); } catch (Exception ignored) { }
            val.setHorasPermitidas(horasPerm);
            val.setHorasUsadas(horasUsadas);
            val.setHorasExtras(horasExtras);
            val.setCostoHoraExtra(tarifaHoraExtra);
            val.setTotalHorasExtras(totalImporteHorasExtras);
        }

        val.setSubtotal(subtotal);
        val.setIgv(igv);
        val.setTotal(totalFinal);
        val.setTotalEnLetras(textoTotalEnLetras);

        // Ítem para el resumen compatible
        ItemValorizacion item = new ItemValorizacion();
        item.setIdAlquiler(val.getIdAlquiler());
        item.setDescripcionEquipo(val.getDescripcionEquipo());
        item.setFechaInicio(fechaInicio);
        item.setFechaFin(fechaFin);
        item.setHorometroInicio(val.getHorometroInicial());
        item.setHorometroFin(val.getHorometroFinal());
        item.setHorasTrabajadas(val.getHorasUsadas());
        item.setPrecioMes(val.getCostoParcial());
        item.setPrecioHorasExtras(val.getTotalHorasExtras());
        item.setTotalItem(val.getSubtotal());
        item.setMarca(marcaGrupo);
        List<ItemValorizacion> items = new ArrayList<>();
        items.add(item);
        val.setItems(items);

        // Guardar y sincronizar con Firestore
        valorizacionesRepository.crearValorizacion(val, new Callback<String>() {
            @Override
            public void onSuccess(String numeroVal) {
                val.setNumeroValorizacion(numeroVal);

                // Si tiene DetalleMes, actualizar los campos del mes en segundo plano
                if (idDetalle != null && !idDetalle.isEmpty() && conExtras) {
                    com.google.firebase.firestore.FirebaseFirestore.getInstance()
                            .collection("detallesMes").document(idDetalle)
                            .update(
                                    "horometro", val.getHorometroFinal(),
                                    "horasExtras", val.getHorasExtras(),
                                    "precioHorasExtras", val.getTotalHorasExtras()
                            );
                }

                generarPdfYFinalizar(val);
            }

            @Override
            public void onError(Exception e) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        btnGenerarValorizacion.setEnabled(true);
                        btnGenerarValorizacion.setText("📄 Generar Valorización y Guardar PDF");
                        progressGenerar.setVisibility(View.GONE);
                        Toast.makeText(getContext(), "Error al guardar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    private void generarPdfYFinalizar(Valorizacion valorizacion) {
        new Thread(() -> {
            try {
                Context ctx = requireContext().getApplicationContext();
                File archivoPdf = StrategyProvider.getPdfStrategy().generarValorizacionPdf(ctx, valorizacion);

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        btnGenerarValorizacion.setEnabled(true);
                        btnGenerarValorizacion.setText("📄 Generar Valorización y Guardar PDF");
                        progressGenerar.setVisibility(View.GONE);

                        Toast.makeText(getContext(), "Valorización " + valorizacion.getNumeroValorizacion() + " generada", Toast.LENGTH_SHORT).show();

                        if (listener != null) {
                            listener.onValorizacionGenerada(valorizacion, archivoPdf);
                        }

                        abrirOCompartirPdf(archivoPdf);
                        dismiss();
                    });
                }
            } catch (Exception e) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        btnGenerarValorizacion.setEnabled(true);
                        btnGenerarValorizacion.setText("📄 Generar Valorización y Guardar PDF");
                        progressGenerar.setVisibility(View.GONE);
                        Toast.makeText(getContext(), "Error creando PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }
            }
        }).start();
    }

    private void abrirOCompartirPdf(File archivo) {
        if (getContext() == null || archivo == null) return;
        try {
            Uri uri = FileProvider.getUriForFile(requireContext(), requireContext().getPackageName() + ".provider", archivo);

            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("application/pdf");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(Intent.createChooser(intent, "Compartir Valorización en PDF"));
        } catch (Exception e) {
            Toast.makeText(getContext(), "Error al compartir archivo: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
