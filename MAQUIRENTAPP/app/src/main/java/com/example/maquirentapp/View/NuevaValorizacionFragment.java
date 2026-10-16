package com.example.maquirentapp.View;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.maquirentapp.MainActivity;
import com.example.maquirentapp.Model.AlquilerMensual;
import com.example.maquirentapp.Model.ClienteValorizacion;
import com.example.maquirentapp.Model.DetalleMes;
import com.example.maquirentapp.Model.GrupoElectrogeno;
import com.example.maquirentapp.Model.InfoPlaca;
import com.example.maquirentapp.Model.ItemValorizacion;
import com.example.maquirentapp.Model.Valorizacion;
import com.example.maquirentapp.Repository.Callback;
import com.example.maquirentapp.Repository.GeneradorRepository;
import com.example.maquirentapp.Repository.ValorizacionesRepository;
import com.example.maquirentapp.R;
import com.example.maquirentapp.adapters.SeleccionValorizacionAdapter;
import com.example.maquirentapp.Utils.PdfGenerator;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class NuevaValorizacionFragment extends Fragment {

    private ClienteValorizacion clienteActual;
    private ValorizacionesRepository repository;
    private GeneradorRepository generadorRepository;

    private RecyclerView recyclerItems;
    private SeleccionValorizacionAdapter adapter;
    private ProgressBar progressBar;
    private Button btnGenerar;
    private TextView tvTitulo;

    private List<ItemValorizacion> itemsCandidatos = new ArrayList<>();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        repository = new ValorizacionesRepository();
        generadorRepository = new GeneradorRepository();
        if (getArguments() != null) {
            clienteActual = (ClienteValorizacion) getArguments().getSerializable("cliente");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_nueva_valorizacion, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Init Views
        recyclerItems = view.findViewById(R.id.recyclerSeleccionItems);
        progressBar = view.findViewById(R.id.progressBar);
        btnGenerar = view.findViewById(R.id.btnGenerarValorizacion);
        tvTitulo = view.findViewById(R.id.tvTituloNuevaVal);

        if (clienteActual != null) {
            tvTitulo.setText("Nueva Valorización: " + clienteActual.getNombreEmpresa());
        }

        setupRecyclerView();
        cargarEquiposCandidatos();

        btnGenerar.setOnClickListener(v -> generarValorizacion());
    }

    private void setupRecyclerView() {
        recyclerItems.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new SeleccionValorizacionAdapter();
        recyclerItems.setAdapter(adapter);
    }

    // Lógica "Multi-Step" para cargar datos de varias fuentes
    private void cargarEquiposCandidatos() {
        progressBar.setVisibility(View.VISIBLE);
        itemsCandidatos.clear();

        // 1. Obtener alquileres activos de este cliente
        repository.getAlquileresPorCliente(clienteActual.getNombreEmpresa(), new Callback<List<AlquilerMensual>>() {
            @Override
            public void onSuccess(List<AlquilerMensual> alquileres) {
                if (alquileres.isEmpty()) {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "No hay alquileres activos para este cliente", Toast.LENGTH_LONG).show();
                    return;
                }

                // 2. Para cada alquiler, buscar su último mes y los datos del grupo
                procesarAlquileres(alquileres);
            }

            @Override
            public void onError(Exception e) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Error al cargar alquileres", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void procesarAlquileres(List<AlquilerMensual> alquileres) {
        AtomicInteger counter = new AtomicInteger(alquileres.size());

        for (AlquilerMensual alquiler : alquileres) {

            // 1. Buscamos los últimos 2 detalles (Actual y Anterior)
            repository.getUltimosDetallesMes(alquiler.getId(), new Callback<List<DetalleMes>>() {
                @Override
                public void onSuccess(List<DetalleMes> detalles) {
                    // La lista viene ordenada descendente: [0] = Actual, [1] = Anterior
                    if (!detalles.isEmpty()) {
                        DetalleMes detalleActual = detalles.get(0);

                        // Lógica para encontrar el Horómetro Inicial
                        double horometroInicio = 0;

                        if (detalles.size() > 1) {
                            // Si hay mes anterior, el inicio actual es el final del anterior
                            horometroInicio = detalles.get(1).getHorometro();
                        } else {
                            // Si es el primer mes, usamos el inicial del contrato
                            horometroInicio = alquiler.getHorometroInicial();
                        }

                        final double hInicioFinal = horometroInicio;

                        // 2. Buscamos datos del grupo (Solo código)
                        generadorRepository.getGrupoPorId(alquiler.getIdGrupo(), new Callback<GrupoElectrogeno>() {
                            @Override
                            public void onSuccess(GrupoElectrogeno grupo) {
                                crearItemYAgregar(alquiler, detalleActual, grupo, hInicioFinal);
                                checkFinalizado(counter);
                            }
                            @Override
                            public void onError(Exception e) { checkFinalizado(counter); }
                        });
                    } else {
                        checkFinalizado(counter);
                    }
                }

                @Override
                public void onError(Exception e) {
                    checkFinalizado(counter);
                }
            });
        }
    }

    private void crearItemYAgregar(AlquilerMensual alquiler, DetalleMes detalle, GrupoElectrogeno grupo, double horometroInicio) {
        ItemValorizacion item = new ItemValorizacion();
        item.setIdAlquiler(alquiler.getId());

        // Datos del Equipo
        String codigo = (grupo.getCodigo() != null) ? grupo.getCodigo() : "SIN CÓDIGO";
        item.setDescripcionEquipo("GRUPO ELECTRÓGENO " + codigo);

        // Campos opcionales vacíos (ya no usamos InfoPlaca aquí para optimizar)
        item.setMarca("");
        item.setModelo("");
        item.setSerie("");

        // Fechas
        item.setFechaInicio(detalle.getFechaInicio());
        item.setFechaFin(detalle.getFechaFin());

        // --- LÓGICA DE HORÓMETROS ---
        item.setHorometroInicio(horometroInicio);
        item.setHorometroFin(detalle.getHorometro());

        // Horas trabajadas = Final - Inicial
        double horasTrabajadas = detalle.getHorometro() - horometroInicio;
        // Corrección de seguridad por si es negativo (cambio de horómetro)
        if (horasTrabajadas < 0) horasTrabajadas = 0;

        item.setHorasTrabajadas(horasTrabajadas);

        // --- PRECIOS ---
        // Si el detalle tiene montoMes (prorrateo), úsalo. Si no, usa el precio del contrato.
        double precioMes = detalle.getMontoMes() > 0 ? detalle.getMontoMes() : alquiler.getPrecioAlquiler();

        item.setPrecioMes(precioMes);
        item.setPrecioHorasExtras(detalle.getPrecioHorasExtras());

        // Total = Mes + Extras
        // Aquí ya no deberías tener error de ambigüedad si limpiaste el Modelo
        item.setTotalItem(precioMes + detalle.getPrecioHorasExtras());

        itemsCandidatos.add(item);
    }
    private void checkFinalizado(AtomicInteger counter) {
        if (counter.decrementAndGet() == 0) {
            // Terminaron todos los hilos
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    if (itemsCandidatos.isEmpty()) {
                        Toast.makeText(getContext(), "No se encontraron periodos pendientes", Toast.LENGTH_LONG).show();
                    } else {
                        adapter.setItems(itemsCandidatos);
                    }
                });
            }
        }
    }

    private void generarValorizacion() {
        List<ItemValorizacion> seleccionados = adapter.getSeleccionados();
        if (seleccionados.isEmpty()) {
            Toast.makeText(getContext(), "Selecciona al menos un equipo", Toast.LENGTH_SHORT).show();
            return;
        }

        // Crear objeto Valorización
        Valorizacion val = new Valorizacion();
        val.setIdClienteValorizacion(clienteActual.getId());
        val.setNombreCliente(clienteActual.getNombreEmpresa());
        val.setClienteRuc(clienteActual.getRuc());
        val.setClienteDireccion(clienteActual.getDireccion());
        val.setUbicacionTrabajo(clienteActual.getUbicacionTrabajo());
        val.setFechaEmision("Hoy"); // O usar DatePicker

        // Asumimos moneda del primer ítem o la definimos globalmente
        // val.setMoneda(...)

        val.setItems(seleccionados);

        // Calcular Totales
        double subtotal = 0;
        for (ItemValorizacion i : seleccionados) subtotal += i.getTotalItem();

        val.setSubtotal(subtotal);
        val.setIgv(subtotal * 0.18);
        val.setTotal(subtotal * 1.18);

        btnGenerar.setEnabled(false);
        btnGenerar.setText("Guardando...");

        // Guardar en Firebase
        repository.crearValorizacion(val, new Callback<String>() {
            @Override
            public void onSuccess(String numeroVal) {
                btnGenerar.setEnabled(true);
                btnGenerar.setText("Generar Valorización");
                val.setNumeroValorizacion(numeroVal);
                generarPdfEnSegundoPlano(val);
            }

            @Override
            public void onError(Exception e) {
                btnGenerar.setEnabled(true);
                btnGenerar.setText("Generar Valorización");
                Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void generarPdfEnSegundoPlano(Valorizacion valorizacion) {
        new Thread(() -> {
            try {
                PdfGenerator generator = new PdfGenerator();
                File archivo = generator.generarValorizacionPdf(requireContext(), valorizacion);

                requireActivity().runOnUiThread(() -> {
                    if (getContext() == null) return;
                    abrirOCompartirPdf(archivo);
                });
            } catch (Exception e) {
                requireActivity().runOnUiThread(() -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Error generando PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }).start();
    }

    private void abrirOCompartirPdf(File archivo) {
        try {
            android.net.Uri uri = androidx.core.content.FileProvider.getUriForFile(
                    requireContext(), requireContext().getPackageName() + ".provider", archivo);

            android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_SEND);
            intent.setType("application/pdf");
            intent.putExtra(android.content.Intent.EXTRA_STREAM, uri);
            intent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(android.content.Intent.createChooser(intent, "Compartir Valorización en PDF"));
        } catch (Exception e) {
            Toast.makeText(getContext(), "Error al compartir archivo: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof MainActivity) ((MainActivity) getActivity()).hideGlobalFab();
    }
}
