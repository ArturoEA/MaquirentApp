package com.example.maquirentapp.View;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.maquirentapp.MainActivity;
import com.example.maquirentapp.Model.ClienteValorizacion;
import com.example.maquirentapp.Model.Valorizacion;
import com.example.maquirentapp.R;
import com.example.maquirentapp.Repository.Callback;
import com.example.maquirentapp.Repository.ValorizacionesRepository;
import com.example.maquirentapp.Utils.CustomDatePickerDialog;
import com.example.maquirentapp.adapters.ClientesValorizacionAdapter;
import com.example.maquirentapp.adapters.ConsolidadoValorizacionesAdapter;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ListaClientesValorizacionFragment extends Fragment {

    // Repositorio
    private ValorizacionesRepository repository;

    // Toggle de pestañas
    private MaterialButtonToggleGroup toggleVistaValorizaciones;
    private LinearLayout layoutVistaClientes;
    private LinearLayout layoutVistaConsolidado;
    private boolean enVistaConsolidado = false;

    // --- Componentes Vista 1: Clientes ---
    private RecyclerView recyclerClientes;
    private ClientesValorizacionAdapter clientesAdapter;
    private int anioSeleccionado;
    private TextView tvAnio;
    private ImageButton btnAnterior, btnSiguiente;
    private ShimmerFrameLayout shimmerClientes;

    // --- Componentes Vista 2: Consolidado ---
    private RecyclerView recyclerConsolidado;
    private ConsolidadoValorizacionesAdapter consolidadoAdapter;
    private ImageButton btnMesAnterior, btnMesSiguiente, btnFiltrarRango;
    private TextView tvPeriodoConsolidado;
    private LinearLayout layoutChipRangoActivo;
    private TextView tvTextoRangoActivo, btnLimpiarRango;
    private TextView tvTotalConsolidadoSoles, tvTotalConsolidadoDolares;
    private TextView tvCantidadValorizaciones;
    private Button btnExportarExcel;
    private ShimmerFrameLayout shimmerConsolidado;
    private TextView emptyStateConsolidado;

    // Estado del filtro consolidado
    private int mesConsolidado;   // 1 a 12
    private int anioConsolidado;  // año del período
    private Date fechaDesdePersonalizada = null;
    private Date fechaHastaPersonalizada = null;
    private final List<Valorizacion> listaTodasLasValorizaciones = new ArrayList<>();
    private final List<Valorizacion> listaFiltradaConsolidado = new ArrayList<>();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        repository = new ValorizacionesRepository();

        Calendar cal = Calendar.getInstance();
        anioSeleccionado = cal.get(Calendar.YEAR);
        mesConsolidado = cal.get(Calendar.MONTH) + 1;
        anioConsolidado = anioSeleccionado;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_lista_clientes_valorizacion, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupToggle();
        setupClientesView();
        setupConsolidadoView();

        actualizarTextoAnio();
        actualizarTextoPeriodoConsolidado();

        cargarClientes();
        configureFab();
    }

    private void initViews(View view) {
        toggleVistaValorizaciones = view.findViewById(R.id.toggleVistaValorizaciones);
        layoutVistaClientes = view.findViewById(R.id.layoutVistaClientes);
        layoutVistaConsolidado = view.findViewById(R.id.layoutVistaConsolidado);

        // Vista Clientes
        tvAnio = view.findViewById(R.id.tvAnio);
        btnAnterior = view.findViewById(R.id.btnAnterior);
        btnSiguiente = view.findViewById(R.id.btnSiguiente);
        recyclerClientes = view.findViewById(R.id.recyclerClientesValorizaciones);
        shimmerClientes = view.findViewById(R.id.shimmerClientes);

        // Vista Consolidado
        btnMesAnterior = view.findViewById(R.id.btnMesAnterior);
        btnMesSiguiente = view.findViewById(R.id.btnMesSiguiente);
        btnFiltrarRango = view.findViewById(R.id.btnFiltrarRango);
        tvPeriodoConsolidado = view.findViewById(R.id.tvPeriodoConsolidado);
        layoutChipRangoActivo = view.findViewById(R.id.layoutChipRangoActivo);
        tvTextoRangoActivo = view.findViewById(R.id.tvTextoRangoActivo);
        btnLimpiarRango = view.findViewById(R.id.btnLimpiarRango);
        tvTotalConsolidadoSoles = view.findViewById(R.id.tvTotalConsolidadoSoles);
        tvTotalConsolidadoDolares = view.findViewById(R.id.tvTotalConsolidadoDolares);
        tvCantidadValorizaciones = view.findViewById(R.id.tvCantidadValorizaciones);
        btnExportarExcel = view.findViewById(R.id.btnExportarExcel);
        shimmerConsolidado = view.findViewById(R.id.shimmerConsolidado);
        emptyStateConsolidado = view.findViewById(R.id.emptyStateConsolidado);
        recyclerConsolidado = view.findViewById(R.id.recyclerConsolidadoValorizaciones);
    }

    private void setupToggle() {
        toggleVistaValorizaciones.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.btnTabConsolidado) {
                    activarVistaConsolidado();
                } else {
                    activarVistaClientes();
                }
            }
        });
    }

    private void activarVistaClientes() {
        enVistaConsolidado = false;
        layoutVistaClientes.setVisibility(View.VISIBLE);
        layoutVistaConsolidado.setVisibility(View.GONE);
        configureFab();
    }

    private void activarVistaConsolidado() {
        enVistaConsolidado = true;
        layoutVistaClientes.setVisibility(View.GONE);
        layoutVistaConsolidado.setVisibility(View.VISIBLE);

        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).hideGlobalFab();
        }

        if (listaTodasLasValorizaciones.isEmpty()) {
            cargarTodasLasValorizaciones();
        } else {
            filtrarYAplicarConsolidado();
        }
    }

    // ==============================================================
    // SECCIÓN 1: VISTA DE CLIENTES
    // ==============================================================
    private void setupClientesView() {
        recyclerClientes.setLayoutManager(new LinearLayoutManager(getContext()));
        clientesAdapter = new ClientesValorizacionAdapter(new ClientesValorizacionAdapter.OnClienteActionListener() {
            @Override
            public void onClienteClick(ClienteValorizacion cliente) {
                if (getView() != null) {
                    Bundle args = new Bundle();
                    args.putSerializable("cliente", cliente);
                    Navigation.findNavController(getView()).navigate(R.id.action_listaClientes_to_historialValorizacionesCliente, args);
                }
            }

            @Override
            public void onEliminarClick(ClienteValorizacion cliente) {
                confirmarEliminacionCliente(cliente);
            }
        });
        recyclerClientes.setAdapter(clientesAdapter);

        btnAnterior.setOnClickListener(v -> {
            anioSeleccionado--;
            actualizarTextoAnio();
            cargarClientes();
        });

        btnSiguiente.setOnClickListener(v -> {
            anioSeleccionado++;
            actualizarTextoAnio();
            cargarClientes();
        });

        tvAnio.setOnClickListener(v -> mostrarSelectorAnio());
    }

    private void actualizarTextoAnio() {
        if (tvAnio != null) tvAnio.setText(String.valueOf(anioSeleccionado));
    }

    private void mostrarSelectorAnio() {
        int anioActual = Calendar.getInstance().get(Calendar.YEAR);

        NumberPicker picker = new NumberPicker(getContext());
        picker.setMinValue(anioActual - 10);
        picker.setMaxValue(anioActual + 10);
        picker.setValue(anioSeleccionado);
        picker.setWrapSelectorWheel(false);

        new MaterialAlertDialogBuilder(requireContext(), R.style.DialogoConFuenteAnta)
                .setTitle("Seleccionar Año Fiscal")
                .setView(picker)
                .setPositiveButton("Aceptar", (dialog, which) -> {
                    anioSeleccionado = picker.getValue();
                    actualizarTextoAnio();
                    cargarClientes();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void confirmarEliminacionCliente(ClienteValorizacion cliente) {
        if (getContext() == null || cliente == null) return;

        new MaterialAlertDialogBuilder(requireContext(), R.style.DialogoConFuenteAnta)
                .setTitle("Eliminar cliente")
                .setMessage("¿Estás seguro de eliminar a \"" + cliente.getNombreEmpresa() + "\" de la lista de valorizaciones del año " + anioSeleccionado + "?\n\nNota: Esta acción solo elimina este registro del directorio de valorizaciones. Tus alquileres y los detalles de cada mes de tus equipos se mantendrán intactos.")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    repository.eliminarClienteValorizacion(cliente.getId(), new Callback<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            Toast.makeText(getContext(), "Cliente eliminado", Toast.LENGTH_SHORT).show();
                            cargarClientes();
                        }

                        @Override
                        public void onError(Exception e) {
                            Toast.makeText(getContext(), "Error al eliminar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void cargarClientes() {
        if (shimmerClientes != null) {
            shimmerClientes.setVisibility(View.VISIBLE);
            shimmerClientes.startShimmer();
        }
        recyclerClientes.setVisibility(View.GONE);

        repository.getClientesPorAnio(anioSeleccionado, new Callback<List<ClienteValorizacion>>() {
            @Override
            public void onSuccess(List<ClienteValorizacion> result) {
                if (shimmerClientes != null) {
                    shimmerClientes.stopShimmer();
                    shimmerClientes.setVisibility(View.GONE);
                }
                recyclerClientes.setVisibility(View.VISIBLE);
                clientesAdapter.submitList(result);
            }

            @Override
            public void onError(Exception e) {
                if (shimmerClientes != null) {
                    shimmerClientes.stopShimmer();
                    shimmerClientes.setVisibility(View.GONE);
                }
                recyclerClientes.setVisibility(View.VISIBLE);
                Toast.makeText(getContext(), "Error cargando clientes", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void configureFab() {
        if (!enVistaConsolidado && getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).showGlobalFab(
                    "Añadir",
                    R.drawable.icon_nuevo_blanco,
                    v -> mostrarDialogoNuevoCliente()
            );
        }
    }

    private void mostrarDialogoNuevoCliente() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_nuevo_cliente_valorizacion, null);
        builder.setView(view);
        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        TextInputEditText inputNombre = view.findViewById(R.id.inputNombre);
        TextInputEditText inputRuc = view.findViewById(R.id.inputRuc);
        TextInputEditText inputDireccion = view.findViewById(R.id.inputDireccion);
        TextInputEditText inputUbicacion = view.findViewById(R.id.inputUbicacion);
        Button btnGuardar = view.findViewById(R.id.btnGuardarCliente);

        btnGuardar.setOnClickListener(v -> {
            String nombre = inputNombre.getText() != null ? inputNombre.getText().toString().trim() : "";
            String ruc = inputRuc.getText() != null ? inputRuc.getText().toString().trim() : "";
            String dir = inputDireccion.getText() != null ? inputDireccion.getText().toString().trim() : "";
            String ubi = inputUbicacion.getText() != null ? inputUbicacion.getText().toString().trim() : "";

            if (nombre.isEmpty() || ruc.isEmpty() || dir.isEmpty()) {
                Toast.makeText(getContext(), "Nombre, RUC y Dirección son obligatorios", Toast.LENGTH_SHORT).show();
                return;
            }

            ClienteValorizacion cliente = new ClienteValorizacion(nombre, ruc, dir, ubi, anioSeleccionado);

            repository.crearClienteValorizacion(cliente, new Callback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    Toast.makeText(getContext(), "Cliente creado para el año " + anioSeleccionado, Toast.LENGTH_SHORT).show();
                    cargarClientes();
                    dialog.dismiss();
                }

                @Override
                public void onError(Exception e) {
                    Toast.makeText(getContext(), "Error al crear: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });

        dialog.show();
    }

    // ==============================================================
    // SECCIÓN 2: VISTA CONSOLIDADO MENSUAL Y REPORTES
    // ==============================================================
    private void setupConsolidadoView() {
        recyclerConsolidado.setLayoutManager(new LinearLayoutManager(getContext()));
        consolidadoAdapter = new ConsolidadoValorizacionesAdapter(valorizacion -> abrirPdfValorizacion(valorizacion));
        recyclerConsolidado.setAdapter(consolidadoAdapter);

        btnMesAnterior.setOnClickListener(v -> {
            fechaDesdePersonalizada = null;
            fechaHastaPersonalizada = null;
            mesConsolidado--;
            if (mesConsolidado < 1) {
                mesConsolidado = 12;
                anioConsolidado--;
            }
            actualizarTextoPeriodoConsolidado();
            filtrarYAplicarConsolidado();
        });

        btnMesSiguiente.setOnClickListener(v -> {
            fechaDesdePersonalizada = null;
            fechaHastaPersonalizada = null;
            mesConsolidado++;
            if (mesConsolidado > 12) {
                mesConsolidado = 1;
                anioConsolidado++;
            }
            actualizarTextoPeriodoConsolidado();
            filtrarYAplicarConsolidado();
        });

        btnFiltrarRango.setOnClickListener(v -> mostrarDialogoSelectorRango());
        btnLimpiarRango.setOnClickListener(v -> {
            fechaDesdePersonalizada = null;
            fechaHastaPersonalizada = null;
            actualizarTextoPeriodoConsolidado();
            filtrarYAplicarConsolidado();
        });

        btnExportarExcel.setOnClickListener(v -> exportarConsolidadoExcel());
    }

    private void abrirPdfValorizacion(Valorizacion valorizacion) {
        try {
            File pdf = com.example.maquirentapp.strategy.StrategyProvider.getPdfStrategy().generarValorizacionPdf(requireContext(), valorizacion);
            Intent intent = new Intent(getContext(), PdfViewerActivity.class);
            intent.putExtra("PDF_URL", pdf.getAbsolutePath());
            intent.putExtra("NOMBRE_ARCHIVO", valorizacion.getNumeroValorizacion() != null ? valorizacion.getNumeroValorizacion() : pdf.getName());
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(getContext(), "Error abriendo PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void cargarTodasLasValorizaciones() {
        if (shimmerConsolidado != null) {
            shimmerConsolidado.setVisibility(View.VISIBLE);
            shimmerConsolidado.startShimmer();
        }
        recyclerConsolidado.setVisibility(View.GONE);
        emptyStateConsolidado.setVisibility(View.GONE);

        repository.getTodasLasValorizaciones(new Callback<List<Valorizacion>>() {
            @Override
            public void onSuccess(List<Valorizacion> result) {
                if (shimmerConsolidado != null) {
                    shimmerConsolidado.stopShimmer();
                    shimmerConsolidado.setVisibility(View.GONE);
                }
                listaTodasLasValorizaciones.clear();
                if (result != null) {
                    listaTodasLasValorizaciones.addAll(result);
                }
                filtrarYAplicarConsolidado();
            }

            @Override
            public void onError(Exception e) {
                if (shimmerConsolidado != null) {
                    shimmerConsolidado.stopShimmer();
                    shimmerConsolidado.setVisibility(View.GONE);
                }
                recyclerConsolidado.setVisibility(View.VISIBLE);
                Toast.makeText(getContext(), "Error al cargar valorizaciones: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filtrarYAplicarConsolidado() {
        listaFiltradaConsolidado.clear();
        double totalSoles = 0.0;
        double totalDolares = 0.0;

        for (Valorizacion v : listaTodasLasValorizaciones) {
            boolean coincide;
            if (fechaDesdePersonalizada != null && fechaHastaPersonalizada != null) {
                coincide = perteneceAlRango(v, fechaDesdePersonalizada, fechaHastaPersonalizada);
            } else {
                coincide = perteneceAlMesYAnio(v, mesConsolidado, anioConsolidado);
            }

            if (coincide) {
                listaFiltradaConsolidado.add(v);
                String moneda = v.getMoneda() != null ? v.getMoneda().toUpperCase() : "SOL";
                if (moneda.contains("USD") || moneda.contains("$")) {
                    totalDolares += v.getTotal();
                } else {
                    totalSoles += v.getTotal();
                }
            }
        }

        // Actualizar tarjetas de totales
        tvTotalConsolidadoSoles.setText(String.format(Locale.US, "S/. %,.2f", totalSoles));
        tvTotalConsolidadoDolares.setText(String.format(Locale.US, "$ %,.2f", totalDolares));

        int cantidad = listaFiltradaConsolidado.size();
        tvCantidadValorizaciones.setText(cantidad + (cantidad == 1 ? " valorización emitida" : " valorizaciones emitidas"));

        // Enviar a adaptador
        consolidadoAdapter.submitList(new ArrayList<>(listaFiltradaConsolidado));

        if (listaFiltradaConsolidado.isEmpty()) {
            recyclerConsolidado.setVisibility(View.GONE);
            emptyStateConsolidado.setVisibility(View.VISIBLE);
        } else {
            recyclerConsolidado.setVisibility(View.VISIBLE);
            emptyStateConsolidado.setVisibility(View.GONE);
        }
    }

    private void actualizarTextoPeriodoConsolidado() {
        if (fechaDesdePersonalizada != null && fechaHastaPersonalizada != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            tvTextoRangoActivo.setText("Rango: " + sdf.format(fechaDesdePersonalizada) + " al " + sdf.format(fechaHastaPersonalizada));
            layoutChipRangoActivo.setVisibility(View.VISIBLE);
            tvPeriodoConsolidado.setText("Filtro Personalizado");
        } else {
            layoutChipRangoActivo.setVisibility(View.GONE);
            tvPeriodoConsolidado.setText(obtenerNombreMes(mesConsolidado) + " " + anioConsolidado);
        }
    }

    private void mostrarDialogoSelectorRango() {
        Calendar cal = Calendar.getInstance();

        // 1. Selector de fecha inicial (Desde)
        new CustomDatePickerDialog(requireContext(), cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH), (y1, m1, d1) -> {
            Calendar cDesde = Calendar.getInstance();
            cDesde.set(y1, m1, d1, 0, 0, 0);

            Toast.makeText(getContext(), "Ahora selecciona la fecha final (Hasta)", Toast.LENGTH_SHORT).show();

            // 2. Selector de fecha final (Hasta)
            new CustomDatePickerDialog(requireContext(), y1, m1, d1, (y2, m2, d2) -> {
                Calendar cHasta = Calendar.getInstance();
                cHasta.set(y2, m2, d2, 23, 59, 59);

                if (cHasta.before(cDesde)) {
                    Toast.makeText(getContext(), "La fecha final debe ser posterior a la fecha inicial", Toast.LENGTH_LONG).show();
                    return;
                }

                fechaDesdePersonalizada = cDesde.getTime();
                fechaHastaPersonalizada = cHasta.getTime();

                actualizarTextoPeriodoConsolidado();
                filtrarYAplicarConsolidado();

            }).show();
        }).show();
    }

    private boolean perteneceAlMesYAnio(Valorizacion v, int mes, int anio) {
        Calendar c = obtenerFechaValorizacion(v);
        if (c == null) return false;
        int m = c.get(Calendar.MONTH) + 1;
        int y = c.get(Calendar.YEAR);
        return (m == mes && y == anio);
    }

    private boolean perteneceAlRango(Valorizacion v, Date desde, Date hasta) {
        Calendar c = obtenerFechaValorizacion(v);
        if (c == null) return false;
        Date d = c.getTime();
        return !d.before(desde) && !d.after(hasta);
    }

    private Calendar obtenerFechaValorizacion(Valorizacion v) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        if (v.getFechaEmision() != null && !v.getFechaEmision().trim().isEmpty()) {
            try {
                Date d = sdf.parse(v.getFechaEmision().trim());
                if (d != null) {
                    Calendar c = Calendar.getInstance();
                    c.setTime(d);
                    return c;
                }
            } catch (Exception ignored) {}
        }
        if (v.getFechaInicio() != null && !v.getFechaInicio().trim().isEmpty()) {
            try {
                Date d = sdf.parse(v.getFechaInicio().trim());
                if (d != null) {
                    Calendar c = Calendar.getInstance();
                    c.setTime(d);
                    return c;
                }
            } catch (Exception ignored) {}
        }
        if (v.getTimestampCreacion() > 0) {
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(v.getTimestampCreacion());
            return c;
        }
        return null;
    }

    private String obtenerNombreMes(int mes) {
        String[] meses = {"Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio", "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"};
        if (mes >= 1 && mes <= 12) return meses[mes - 1];
        return "";
    }

    private void exportarConsolidadoExcel() {
        if (listaFiltradaConsolidado.isEmpty()) {
            Toast.makeText(getContext(), "No hay valorizaciones emitidas para exportar en este período", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            StringBuilder csv = new StringBuilder();
            // UTF-8 BOM para apertura nativa en Microsoft Excel y Google Sheets con acentos correctos
            csv.append("\uFEFF");

            // Encabezados de columnas
            csv.append("N° Valorización;Cliente;RUC;Equipo;Fecha Emisión;Periodo Inicio;Periodo Fin;Días;Moneda;Tarifa Base;Horas Extras;Subtotal;IGV;Total\n");

            for (Valorizacion v : listaFiltradaConsolidado) {
                csv.append(escaparCsv(v.getNumeroValorizacion())).append(";");
                csv.append(escaparCsv(v.getNombreCliente())).append(";");
                csv.append(escaparCsv(v.getClienteRuc())).append(";");
                String eq = (v.getDescripcionEquipo() != null && !v.getDescripcionEquipo().isEmpty()) ? v.getDescripcionEquipo() : v.getCodigoEquipo();
                csv.append(escaparCsv(eq)).append(";");
                csv.append(escaparCsv(v.getFechaEmision())).append(";");
                csv.append(escaparCsv(v.getFechaInicio())).append(";");
                csv.append(escaparCsv(v.getFechaFin())).append(";");
                csv.append(v.getDiasValorizados() > 0 ? v.getDiasValorizados() : 30).append(";");
                csv.append(escaparCsv(v.getMoneda() != null ? v.getMoneda() : "SOL")).append(";");
                csv.append(String.format(Locale.US, "%.2f", v.getCostoParcial())).append(";");
                csv.append(String.format(Locale.US, "%.2f", v.getTotalHorasExtras())).append(";");
                csv.append(String.format(Locale.US, "%.2f", v.getSubtotal())).append(";");
                csv.append(String.format(Locale.US, "%.2f", v.getIgv())).append(";");
                csv.append(String.format(Locale.US, "%.2f", v.getTotal())).append("\n");
            }

            String periodoNombre = (fechaDesdePersonalizada != null) ? "Rango_Personalizado" : (obtenerNombreMes(mesConsolidado) + "_" + anioConsolidado);
            String nombreArchivo = "Consolidado_Valorizaciones_" + periodoNombre + ".csv";
            File archivo = new File(requireContext().getCacheDir(), nombreArchivo);

            FileOutputStream fos = new FileOutputStream(archivo);
            fos.write(csv.toString().getBytes(StandardCharsets.UTF_8));
            fos.flush();
            fos.close();

            Uri uri = FileProvider.getUriForFile(requireContext(), requireContext().getPackageName() + ".provider", archivo);

            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/csv");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.putExtra(Intent.EXTRA_SUBJECT, "Consolidado de Valorizaciones - " + periodoNombre);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "Compartir o abrir reporte en Excel"));

        } catch (Exception e) {
            Toast.makeText(getContext(), "Error exportando reporte: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private String escaparCsv(String valor) {
        if (valor == null) return "";
        String v = valor.replace("\"", "\"\"");
        if (v.contains(";") || v.contains("\n") || v.contains("\"")) {
            return "\"" + v + "\"";
        }
        return v;
    }

    @Override
    public void onPause() {
        super.onPause();
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).hideGlobalFab();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!enVistaConsolidado) {
            configureFab();
            cargarClientes();
        } else {
            cargarTodasLasValorizaciones();
        }
    }
}
