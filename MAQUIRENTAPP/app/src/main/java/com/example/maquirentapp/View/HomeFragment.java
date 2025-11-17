package com.example.maquirentapp.View;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.example.maquirentapp.Access.PagoPendienteAdapter;
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
    private RecyclerView recyclerPagosPendientes;
    private PagoPendienteAdapter pagoPendienteAdapter;
    private FirebaseServicio firebaseServicio;
    private List<PagoPendiente> pagosPendientesList = new ArrayList<>();
    private Map<String, GrupoElectrogeno> gruposMap = new HashMap<>();
    private NavController navController;
    private TextView emptyStatePagosPendientes;

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
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        CardView cardNuevoAlquiler = view.findViewById(R.id.cardNuevoAlquilerDiario);
        CardView cardCotizaciones = view.findViewById(R.id.cardCotizaciones);
        CardView cardPlanosCambioVoltaje = view.findViewById(R.id.cardPlanosVoltaje);
        CardView cardFichasTecnicas = view.findViewById(R.id.cardFichasTecnicas);

        navController = Navigation.findNavController(view);

        cardNuevoAlquiler.setOnClickListener(v ->
                navController.navigate(R.id.action_homeFragment_to_nuevoAlquilerFragment));
//        cardCotizaciones.setOnClickListener(v ->
//                navController.navigate(R.id.action_homeFragment_to_cotizacionesFragment));
        cardPlanosCambioVoltaje.setOnClickListener(v -> navController.navigate(R.id.action_home_to_PlanosCambioVoltajeFragment));
        cardFichasTecnicas.setOnClickListener(v -> navController.navigate(R.id.action_home_to_FichasTecnicasFragment));

        emptyStatePagosPendientes = view.findViewById(R.id.emptyStatePagosPendientes);
        setupPagosPendientesRecyclerView(view);
        cargarPagosPendientes();
    }
    private void setupPagosPendientesRecyclerView(View view) {
        recyclerPagosPendientes = view.findViewById(R.id.recyclerPagosPendientes);
        recyclerPagosPendientes.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));

        pagoPendienteAdapter = new PagoPendienteAdapter(pagosPendientesList, pago -> {
            Bundle args = new Bundle();
            args.putString("idGrupo", pago.getIdGrupo());
            args.putString("alquilerId", pago.getAlquilerId());
            args.putBoolean("modoSoloLectura", true);;

            try {
                navController.navigate(R.id.action_homeFragment_to_nuevoAlquilerMensualFragment, args);
            } catch (Exception e) {
                Log.e("HomeFragment", "Error al navegar. Asegúrate de que la acción 'action_homeFragment_to_nuevoAlquilerMensualFragment' existe en tu nav_graph.", e);
                Toast.makeText(getContext(), "Error de navegación", Toast.LENGTH_SHORT).show();
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
                for (GrupoElectrogeno g : grupos) {
                    if (g != null && g.getId() != null) {
                        gruposMap.put(g.getId(), g);
                    }
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

                // 3. Para cada alquiler activo, cargar sus detalles de mes
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
                            Log.e("HomeFragment", "Error al cargar detalles para alquiler " + alquiler.getId(), e);
                            if (counter.decrementAndGet() == 0) {
                                actualizarAdaptadorPagos();
                            }
                        }
                    });
                }
            }

            @Override
            public void onError(Exception e) {
                Log.e("HomeFragment", "Error al cargar alquileres mensuales", e);
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

                // Lógica de color
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
