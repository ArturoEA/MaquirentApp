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

import com.example.maquirentapp.Access.AlquilerDiarioAdapter; // Importar Adapter
import com.example.maquirentapp.Access.PagoPendienteAdapter;
import com.example.maquirentapp.Model.Accesorio; // Importar Modelo
import com.example.maquirentapp.Model.AlquilerDia; // Importar Modelo
import com.example.maquirentapp.Model.AlquilerMensual;
import com.example.maquirentapp.Model.DetalleMes;
import com.example.maquirentapp.Model.GrupoElectrogeno;
import com.example.maquirentapp.Model.PagoPendiente;
import com.example.maquirentapp.Network.FirebaseServicio;
import com.example.maquirentapp.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class HomeFragment extends Fragment {
    private RecyclerView recyclerPagosPendientes, recyclerAlquileresDiarios;
    private PagoPendienteAdapter pagoPendienteAdapter;
    private AlquilerDiarioAdapter alquilerDiarioAdapter;
    private FirebaseServicio firebaseServicio;

    private List<PagoPendiente> pagosPendientesList = new ArrayList<>();
    private Map<String, GrupoElectrogeno> gruposMap = new HashMap<>();
    private Map<String, String> accesoriosMap = new HashMap<>();

    private NavController navController;
    private TextView emptyStatePagosPendientes, emptyStateAlquileresDiarios;

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        firebaseServicio = new FirebaseServicio();
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
        emptyStatePagosPendientes = view.findViewById(R.id.emptyStatePagosPendientes);
        emptyStateAlquileresDiarios = view.findViewById(R.id.emptyStateAlquileresDiarios); // Inicializar

        navController = Navigation.findNavController(view);

        // Listeners de botones
        cardNuevoAlquiler.setOnClickListener(v ->
                navController.navigate(R.id.action_homeFragment_to_nuevoAlquilerFragment));
        cardPlanosCambioVoltaje.setOnClickListener(v -> navController.navigate(R.id.action_home_to_PlanosCambioVoltajeFragment));
        cardFichasTecnicas.setOnClickListener(v -> navController.navigate(R.id.action_home_to_FichasTecnicasFragment));
        cardListaTareas.setOnClickListener(v -> navController.navigate(R.id.action_home_to_TareasFragment));
        cardCotizaciones.setOnClickListener(v -> navController.navigate(R.id.action_home_to_NuevaCotizacionFragment));

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
        // 1. Cargar accesorios primero para llenar el mapa de íconos
        firebaseServicio.getAccesorios("diario", new FirebaseServicio.OnAccesoriosLoadedListener() {
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
        firebaseServicio.getAlquileresDiariosActivos(new FirebaseServicio.OnAlquileresDiariosLoadedListener() {
            @Override
            public void onSuccess(List<AlquilerDia> alquileres) {
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
            args.putBoolean("modoSoloLectura", true);

            try {
                navController.navigate(R.id.action_homeFragment_to_nuevoAlquilerMensualFragment, args);
            } catch (Exception e) {
                Log.e("HomeFragment", "Error nav pagos pendientes", e);
            }
        });
        recyclerPagosPendientes.setAdapter(pagoPendienteAdapter);
    }

    private void cargarPagosPendientes() {
        pagosPendientesList.clear();
        gruposMap.clear();
        firebaseServicio.getGruposElectrogenos(false, new FirebaseServicio.OnGruposLoadedListener() {
            @Override
            public void onSuccess(List<GrupoElectrogeno> grupos) {
                Map<String, String> mapaCodigos = new HashMap<>();
                for (GrupoElectrogeno g : grupos) {
                    if (g != null && g.getId() != null) {
                        gruposMap.put(g.getId(), g);
                        mapaCodigos.put(g.getId(), g.getCodigo());
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
        firebaseServicio.getAlquileresMensuales(new FirebaseServicio.OnAlquileresLoadedListener() {
            @Override
            public void onSuccess(List<AlquilerMensual> alquileres) {
                List<AlquilerMensual> alquileresActivos = new ArrayList<>();
                for (AlquilerMensual a : alquileres) {
                    if (a != null && !a.isFinalizado()) {
                        alquileresActivos.add(a);
                    }
                }

                if (alquileresActivos.isEmpty()) {
                    actualizarAdaptadorPagos();
                    return;
                }

                AtomicInteger counter = new AtomicInteger(alquileresActivos.size());
                for (AlquilerMensual alquiler : alquileresActivos) {
                    firebaseServicio.getDetallesMesPorAlquiler(alquiler.getId(), new FirebaseServicio.OnDetallesMesLoadedListener() {
                        @Override
                        public void onSuccess(List<DetalleMes> detalles) {
                            procesarDetallesDeAlquiler(alquiler, detalles);
                            if (counter.decrementAndGet() == 0) {
                                actualizarAdaptadorPagos();
                            }
                        }

                        @Override
                        public void onError(Exception e) {
                            if (counter.decrementAndGet() == 0) {
                                actualizarAdaptadorPagos();
                            }
                        }
                    });
                }
            }

            @Override
            public void onError(Exception e) {
                actualizarAdaptadorPagos();
            }
        });
    }

    private void procesarDetallesDeAlquiler(AlquilerMensual alquiler, List<DetalleMes> detalles) {
        for (DetalleMes detalle : detalles) {
            boolean mesPendiente = !detalle.isPagoMesConfirmado();
            boolean hePendiente = !detalle.isPagoHEConfirmado() && detalle.getPrecioHorasExtras() > 0;

            if (mesPendiente || hePendiente) {
                PagoPendiente pago = new PagoPendiente();

                GrupoElectrogeno grupo = gruposMap.get(alquiler.getIdGrupo());
                String codigo = (grupo != null) ? grupo.getCodigo() : "(ID: " + alquiler.getIdGrupo() + ")";
                String moneda = (alquiler.getMoneda() != null && alquiler.getMoneda().equals("USD")) ? "$" : "S/.";

                pago.setNombreCliente(alquiler.getNombreCliente());
                pago.setCodigoGrupo(codigo);
                pago.setTituloPeriodo(detalle.getTituloPeriodo());
                pago.setMontoPendienteMes(mesPendiente ? alquiler.getPrecioAlquiler() : 0);
                pago.setMontoPendienteHE(hePendiente ? detalle.getPrecioHorasExtras() : 0);
                pago.setMoneda(moneda);
                pago.setAlquilerId(alquiler.getId());
                pago.setDetalleMesId(detalle.getId());
                pago.setIdGrupo(alquiler.getIdGrupo());

                if (mesPendiente) {
                    pago.setEstadoColor(R.color.red_accent);
                } else {
                    pago.setEstadoColor(R.color.yellow_accent);
                }

                pagosPendientesList.add(pago);
            }
        }
    }

    private void actualizarAdaptadorPagos() {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
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