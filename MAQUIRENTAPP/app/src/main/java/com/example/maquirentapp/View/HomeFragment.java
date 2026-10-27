package com.example.maquirentapp.View;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.maquirentapp.adapters.AlquilerDiarioAdapter; // Importar Adapter
import com.example.maquirentapp.adapters.PagoPendienteAdapter;
import com.example.maquirentapp.Model.Accesorio; // Importar Modelo
import com.example.maquirentapp.Model.AlquilerDia; // Importar Modelo
import com.example.maquirentapp.Model.AlquilerMensual;
import com.example.maquirentapp.Model.DetalleMes;
import com.example.maquirentapp.Model.GrupoElectrogeno;
import com.example.maquirentapp.Model.PagoPendiente;
import com.example.maquirentapp.Repository.AlquilerRepository;
import com.example.maquirentapp.Repository.Callback;
import com.example.maquirentapp.Repository.GeneradorRepository;
import com.example.maquirentapp.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class HomeFragment extends Fragment {
    private RecyclerView recyclerPagosPendientes, recyclerAlquileresDiarios;
    private PagoPendienteAdapter pagoPendienteAdapter;
    private AlquilerDiarioAdapter alquilerDiarioAdapter;
    private AlquilerRepository alquilerRepository;
    private GeneradorRepository generadorRepository;

    private List<PagoPendiente> pagosPendientesList = new ArrayList<>();
    private Map<String, GrupoElectrogeno> gruposMap = new HashMap<>();
    private Map<String, String> accesoriosMap = new HashMap<>();

    private NavController navController;
    private TextView emptyStatePagosPendientes, emptyStateAlquileresDiarios;
    private com.facebook.shimmer.ShimmerFrameLayout shimmerAlquileresDiarios;
    private com.facebook.shimmer.ShimmerFrameLayout shimmerPagosPendientes;

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        alquilerRepository = new AlquilerRepository();
        generadorRepository = new GeneradorRepository();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Inicializar vistas
        CardView cardNuevoAlquiler = view.findViewById(R.id.cardNuevoAlquilerDiario);
        CardView cardCotizaciones = view.findViewById(R.id.cardCotizaciones);
        CardView cardPlanosCambioVoltaje = view.findViewById(R.id.cardPlanosVoltaje);
        CardView cardFichasTecnicas = view.findViewById(R.id.cardFichasTecnicas);
        CardView cardListaTareas = view.findViewById(R.id.cardListaTareas);
        CardView cardValorizaciones = view.findViewById(R.id.cardValorizaciones);
        CardView cardCertificados = view.findViewById(R.id.cardCertificadosOperatividad);
        emptyStatePagosPendientes = view.findViewById(R.id.emptyStatePagosPendientes);
        emptyStateAlquileresDiarios = view.findViewById(R.id.emptyStateAlquileresDiarios);
        shimmerAlquileresDiarios = view.findViewById(R.id.shimmerAlquileresDiarios);
        shimmerPagosPendientes = view.findViewById(R.id.shimmerPagosPendientes);

        navController = Navigation.findNavController(view);

        // Listeners de botones
        cardNuevoAlquiler.setOnClickListener(v ->
                navController.navigate(R.id.action_homeFragment_to_nuevoAlquilerFragment));
        cardPlanosCambioVoltaje.setOnClickListener(v -> navController.navigate(R.id.action_home_to_PlanosCambioVoltajeFragment));
        cardFichasTecnicas.setOnClickListener(v -> navController.navigate(R.id.action_home_to_FichasTecnicasFragment));
        cardListaTareas.setOnClickListener(v -> navController.navigate(R.id.action_home_to_TareasFragment));
        cardCotizaciones.setOnClickListener(v -> navController.navigate(R.id.action_home_to_HistorialCotizacionesFragment));
        cardValorizaciones.setOnClickListener(v -> navController.navigate(R.id.action_home_to_listaClientesValorizacionFragment));
        cardCertificados.setOnClickListener(v -> navController.navigate(R.id.action_home_to_NuevoCertificadoFragment));

        // Configurar Recyclers
        setupAlquileresDiariosRecycler(view);
        setupPagosPendientesRecyclerView(view);

        cargarAccesoriosYAlquileresDiarios();
        cargarPagosPendientes();
    }
    private void setupAlquileresDiariosRecycler(View view) {
        recyclerAlquileresDiarios = view.findViewById(R.id.recyclerAlquileresDiarios);
        recyclerAlquileresDiarios.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));

        int spaceInPixels = (int) (10 * getResources().getDisplayMetrics().density);
        recyclerAlquileresDiarios.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(@NonNull android.graphics.Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
                int position = parent.getChildAdapterPosition(view);
                if (position != parent.getAdapter().getItemCount() - 1) {
                    outRect.right = spaceInPixels;
                }
                if (position == 0) {
                    outRect.left = spaceInPixels / 10;
                }
            }
        });
        alquilerDiarioAdapter = new AlquilerDiarioAdapter(alquiler -> {
            Bundle args = new Bundle();
            args.putString("idGrupo", alquiler.getIdGrupo());
            args.putString("alquilerId", alquiler.getId());
            args.putBoolean("modoSoloLectura", true);
            navController.navigate(R.id.action_homeFragment_to_nuevoAlquilerFragment, args);
        });
        alquilerDiarioAdapter.setCompactMode(true);
        recyclerAlquileresDiarios.setAdapter(alquilerDiarioAdapter);
    }
    private void cargarAccesoriosYAlquileresDiarios() {
        if (shimmerAlquileresDiarios != null) {
            shimmerAlquileresDiarios.setVisibility(View.VISIBLE);
            shimmerAlquileresDiarios.startShimmer();
        }
        if (recyclerAlquileresDiarios != null) {
            recyclerAlquileresDiarios.setVisibility(View.GONE);
        }
        // 1. Cargar accesorios primero para llenar el mapa de íconos
        alquilerRepository.getAccesorios("diario", new Callback<List<Accesorio>>() {
            @Override
            public void onSuccess(List<Accesorio> accesorios) {
                accesoriosMap.clear();
                for (Accesorio acc : accesorios) {
                    accesoriosMap.put(acc.getId(), acc.getNombre());
                }
                // Una vez tenemos los accesorios, cargamos los alquileres
                cargarAlquileresDiariosActivos();
            }

            @Override
            public void onError(Exception e) {
                Log.e("HomeFragment", "Error cargando accesorios", e);
                // Intentamos cargar alquileres de todas formas
                cargarAlquileresDiariosActivos();
            }
        });
    }

    private void cargarAlquileresDiariosActivos() {
        alquilerRepository.getAlquileresDiariosActivos(new Callback<List<AlquilerDia>>() {
            @Override
            public void onSuccess(List<AlquilerDia> alquileres) {
                if (shimmerAlquileresDiarios != null) {
                    shimmerAlquileresDiarios.stopShimmer();
                    shimmerAlquileresDiarios.setVisibility(View.GONE);
                }
                alquilerDiarioAdapter.setItems(alquileres);

                if (alquileres.isEmpty()) {
                    recyclerAlquileresDiarios.setVisibility(View.GONE);
                    emptyStateAlquileresDiarios.setVisibility(View.VISIBLE);
                } else {
                    recyclerAlquileresDiarios.setVisibility(View.VISIBLE);
                    emptyStateAlquileresDiarios.setVisibility(View.GONE);
                }
            }

            @Override
            public void onError(Exception e) {
                if (shimmerAlquileresDiarios != null) {
                    shimmerAlquileresDiarios.stopShimmer();
                    shimmerAlquileresDiarios.setVisibility(View.GONE);
                }
                Log.e("HomeFragment", "Error cargando alquileres diarios", e);
                Toast.makeText(getContext(), "Error al cargar alquileres diarios", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // --- LÓGICA PAGOS PENDIENTES (EXISTENTE) ---

    private void setupPagosPendientesRecyclerView(View view) {
        recyclerPagosPendientes = view.findViewById(R.id.recyclerPagosPendientes);
        recyclerPagosPendientes.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));

        pagoPendienteAdapter = new PagoPendienteAdapter(pagosPendientesList, pago -> {
            Bundle args = new Bundle();
            args.putString("idGrupo", pago.getIdGrupo());
            args.putString("alquilerId", pago.getAlquilerId());
            if (pago.getCodigoGrupo() != null) {
                args.putString("codigo", pago.getCodigoGrupo());
            }
            args.putBoolean("modoSoloLectura", true);

            try {
                navController.navigate(R.id.action_homeFragment_to_nuevoAlquilerMensualFragment, args);
            } catch (Exception e) {
                Log.e("HomeFragment", "Error nav pagos pendientes", e);
            }
        });
        recyclerPagosPendientes.setAdapter(pagoPendienteAdapter);
    }

    @Override
    public void onResume() {
        super.onResume();
        cargarAccesoriosYAlquileresDiarios();
        cargarPagosPendientes();
    }

    private void cargarPagosPendientes() {
        if (shimmerPagosPendientes != null) {
            shimmerPagosPendientes.setVisibility(View.VISIBLE);
            shimmerPagosPendientes.startShimmer();
        }
        if (recyclerPagosPendientes != null) {
            recyclerPagosPendientes.setVisibility(View.GONE);
        }
        pagosPendientesList.clear();
        gruposMap.clear();
        generadorRepository.getGruposElectrogenos(false, new Callback<List<GrupoElectrogeno>>() {
            @Override
            public void onSuccess(List<GrupoElectrogeno> grupos) {
                Map<String, String> mapaCodigos = new HashMap<>();
                for (GrupoElectrogeno g : grupos) {
                    if (g != null && g.getId() != null) {
                        gruposMap.put(g.getId(), g);
                        if (g.getCodigo() != null) {
                            gruposMap.put(g.getCodigo(), g);
                            mapaCodigos.put(g.getId(), g.getCodigo());
                            mapaCodigos.put(g.getCodigo(), g.getCodigo());
                        }
                    }
                }
                if (alquilerDiarioAdapter != null) {
                    alquilerDiarioAdapter.setGruposMap(mapaCodigos);
                }
                cargarAlquileresActivos();
            }

            @Override
            public void onError(Exception e) {
                Log.e("HomeFragment", "Error al cargar grupos", e);
                cargarAlquileresActivos();
            }
        });
    }

    private void cargarAlquileresActivos() {
        alquilerRepository.getAlquileresMensualesActivos(new Callback<List<AlquilerMensual>>() {
            @Override
            public void onSuccess(List<AlquilerMensual> alquileresActivos) {
                if (alquileresActivos == null || alquileresActivos.isEmpty()) {
                    pagosPendientesList.clear();
                    actualizarAdaptadorPagos();
                    return;
                }

                List<PagoPendiente> listaTemp = Collections.synchronizedList(new ArrayList<>());
                AtomicInteger counter = new AtomicInteger(alquileresActivos.size());
                for (AlquilerMensual alquiler : alquileresActivos) {
                    alquilerRepository.getDetallesMesPorAlquiler(alquiler.getId(), new Callback<List<DetalleMes>>() {
                        @Override
                        public void onSuccess(List<DetalleMes> detalles) {
                            procesarDetallesDeAlquiler(alquiler, detalles, listaTemp);
                            if (counter.decrementAndGet() == 0) {
                                pagosPendientesList.clear();
                                pagosPendientesList.addAll(listaTemp);
                                actualizarAdaptadorPagos();
                            }
                        }

                        @Override
                        public void onError(Exception e) {
                            if (counter.decrementAndGet() == 0) {
                                pagosPendientesList.clear();
                                pagosPendientesList.addAll(listaTemp);
                                actualizarAdaptadorPagos();
                            }
                        }
                    });
                }
            }

            @Override
            public void onError(Exception e) {
                pagosPendientesList.clear();
                actualizarAdaptadorPagos();
            }
        });
    }

    private void procesarDetallesDeAlquiler(AlquilerMensual alquiler, List<DetalleMes> detalles, List<PagoPendiente> destino) {
        if (detalles == null) return;
        for (DetalleMes detalle : detalles) {
            boolean mesPendiente = !detalle.isPagoMesConfirmado();
            boolean hePendiente = !detalle.isPagoHEConfirmado() && detalle.getPrecioHorasExtras() > 0;

            if (mesPendiente || hePendiente) {
                PagoPendiente pago = new PagoPendiente();

                GrupoElectrogeno grupo = gruposMap.get(alquiler.getIdGrupo());
                if (grupo == null && alquiler.getIdGrupo() != null) {
                    for (GrupoElectrogeno g : gruposMap.values()) {
                        if (g != null && g.getCodigo() != null && g.getCodigo().equalsIgnoreCase(alquiler.getIdGrupo())) {
                            grupo = g;
                            break;
                        }
                    }
                }
                String codigo = (grupo != null && grupo.getCodigo() != null) ? grupo.getCodigo() :
                        (alquiler.getIdGrupo() != null ? alquiler.getIdGrupo() : "Equipo");
                String moneda = (alquiler.getMoneda() != null && alquiler.getMoneda().equals("USD")) ? "$" : "S/.";

                pago.setNombreCliente(alquiler.getNombreCliente());
                pago.setCodigoGrupo(codigo);
                pago.setTituloPeriodo(formatearPeriodoTexto(detalle));

                double montoMes = 0;
                if (detalle.getMontoMes() > 0) {
                    montoMes = detalle.getMontoMes();
                } else if (alquiler.getPrecioAlquiler() > 0) {
                    montoMes = alquiler.getPrecioAlquiler();
                }

                pago.setMontoPendienteMes(mesPendiente ? montoMes : 0);
                pago.setMontoPendienteHE(hePendiente ? detalle.getPrecioHorasExtras() : 0);
                pago.setMoneda(moneda);
                pago.setAlquilerId(alquiler.getId());
                pago.setDetalleMesId(detalle.getId());
                pago.setIdGrupo(alquiler.getIdGrupo());
                pago.setMesPendiente(mesPendiente);
                pago.setHePendiente(hePendiente);

                if (mesPendiente) {
                    pago.setEstadoColor(R.color.red_accent);
                } else {
                    pago.setEstadoColor(R.color.yellow_accent);
                }

                destino.add(pago);
            }
        }
    }

    private String formatearPeriodoTexto(DetalleMes detalle) {
        if (detalle == null) return "Período";

        String fInicio = detalle.getFechaInicio();
        String fFin = detalle.getFechaFin();

        // 1. Si no hay fechas explícitas, intentar extraerlas de tituloPeriodo
        if ((fInicio == null || fInicio.isEmpty()) && detalle.getTituloPeriodo() != null) {
            java.util.regex.Matcher m = java.util.regex.Pattern
                    .compile("(\\d{1,2}/\\d{1,2}/\\d{4})")
                    .matcher(detalle.getTituloPeriodo());
            if (m.find()) {
                fInicio = m.group(1);
                if (m.find()) {
                    fFin = m.group(1);
                }
            }
        }

        // 2. Si tenemos fechas, convertir a meses en texto
        if (fInicio != null && !fInicio.isEmpty() && fFin != null && !fFin.isEmpty()) {
            SimpleDateFormat sdfIn = new SimpleDateFormat("dd/MM/yyyy", Locale.US);
            SimpleDateFormat sdfMes = new SimpleDateFormat("MMMM", new Locale("es", "ES"));
            try {
                Date d1 = sdfIn.parse(fInicio);
                Date d2 = sdfIn.parse(fFin);
                if (d1 != null && d2 != null) {
                    String mes1 = capitalizar(sdfMes.format(d1));
                    String mes2 = capitalizar(sdfMes.format(d2));

                    if (mes1.equalsIgnoreCase(mes2)) {
                        return mes1;
                    } else {
                        return mes1 + " - " + mes2;
                    }
                }
            } catch (Exception ignored) {}
        }

        // 3. Fallback: limpiar tituloPeriodo si ya contenía texto o números
        String titulo = detalle.getTituloPeriodo();
        if (titulo != null && !titulo.isEmpty()) {
            String limpio = titulo.replaceAll("\\(\\d{1,2}/\\d{1,2}/\\d{4}.*?\\)", "")
                    .replaceAll("\\[.*?\\]", "")
                    .replaceAll("(?i)Mes\\s*\\d+\\s*[:•-]?\\s*", "")
                    .trim();
            if (!limpio.isEmpty()) {
                return limpio;
            }
        }

        return "Período";
    }

    private String capitalizar(String str) {
        if (str == null || str.isEmpty()) return "";
        return str.substring(0, 1).toUpperCase(new Locale("es", "ES")) + str.substring(1).toLowerCase(new Locale("es", "ES"));
    }

    private void actualizarAdaptadorPagos() {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                if (shimmerPagosPendientes != null) {
                    shimmerPagosPendientes.stopShimmer();
                    shimmerPagosPendientes.setVisibility(View.GONE);
                }
                pagoPendienteAdapter.setItems(pagosPendientesList);
                if (pagosPendientesList.isEmpty()) {
                    recyclerPagosPendientes.setVisibility(View.GONE);
                    emptyStatePagosPendientes.setVisibility(View.VISIBLE);
                } else {
                    recyclerPagosPendientes.setVisibility(View.VISIBLE);
                    emptyStatePagosPendientes.setVisibility(View.GONE);
                }
            });
        }
    }
}
