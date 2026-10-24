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
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.maquirentapp.MainActivity;
import com.example.maquirentapp.Model.AlquilerMensual;
import com.example.maquirentapp.Model.ClienteValorizacion;
import com.example.maquirentapp.Model.DetalleMes;
import com.example.maquirentapp.Model.GrupoElectrogeno;
import com.example.maquirentapp.Model.ItemValorizacion;
import com.example.maquirentapp.R;
import com.example.maquirentapp.Repository.Callback;
import com.example.maquirentapp.Repository.GeneradorRepository;
import com.example.maquirentapp.Repository.ValorizacionesRepository;
import com.example.maquirentapp.adapters.SeleccionValorizacionAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private final Map<ItemValorizacion, AlquilerMensual> mapAlquiler = new HashMap<>();
    private final Map<ItemValorizacion, DetalleMes> mapDetalle = new HashMap<>();
    private final Map<ItemValorizacion, Double> mapHInicio = new HashMap<>();

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

        recyclerItems = view.findViewById(R.id.recyclerSeleccionItems);
        progressBar = view.findViewById(R.id.progressBar);
        btnGenerar = view.findViewById(R.id.btnGenerarValorizacion);
        tvTitulo = view.findViewById(R.id.tvTituloNuevaVal);

        if (clienteActual != null) {
            tvTitulo.setText("Nueva Valorización: " + clienteActual.getNombreEmpresa());
        }

        setupRecyclerView();
        cargarEquiposCandidatos();

        btnGenerar.setOnClickListener(v -> abrirSeleccionado());
    }

    private void setupRecyclerView() {
        recyclerItems.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new SeleccionValorizacionAdapter();
        recyclerItems.setAdapter(adapter);
    }

    private void cargarEquiposCandidatos() {
        if (clienteActual == null) return;
        progressBar.setVisibility(View.VISIBLE);
        itemsCandidatos.clear();
        mapAlquiler.clear();
        mapDetalle.clear();
        mapHInicio.clear();

        repository.getAlquileresPorCliente(clienteActual.getNombreEmpresa(), new Callback<List<AlquilerMensual>>() {
            @Override
            public void onSuccess(List<AlquilerMensual> alquileres) {
                if (alquileres.isEmpty()) {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "No hay alquileres activos para este cliente", Toast.LENGTH_LONG).show();
                    return;
                }
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
            repository.getUltimosDetallesMes(alquiler.getId(), new Callback<List<DetalleMes>>() {
                @Override
                public void onSuccess(List<DetalleMes> detalles) {
                    if (!detalles.isEmpty()) {
                        DetalleMes detalleActual = detalles.get(0);
                        double horometroInicio;

                        if (detalles.size() > 1) {
                            horometroInicio = detalles.get(1).getHorometro();
                        } else {
                            horometroInicio = alquiler.getHorometroInicial();
                        }

                        final double hInicioFinal = horometroInicio;

                        generadorRepository.getGrupoPorId(alquiler.getIdGrupo(), new Callback<GrupoElectrogeno>() {
                            @Override
                            public void onSuccess(GrupoElectrogeno grupo) {
                                crearItemYAgregar(alquiler, detalleActual, grupo, hInicioFinal);
                                checkFinalizado(counter);
                            }

                            @Override
                            public void onError(Exception e) {
                                checkFinalizado(counter);
                            }
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

        String codigo = (grupo != null && grupo.getCodigo() != null) ? grupo.getCodigo() : "EQUIPO";
        item.setDescripcionEquipo("GRUPO ELECTRÓGENO " + codigo);
        item.setFechaInicio(detalle.getFechaInicio());
        item.setFechaFin(detalle.getFechaFin());
        item.setHorometroInicio(horometroInicio);
        item.setHorometroFin(detalle.getHorometro());

        double horasTrabajadas = detalle.getHorometro() - horometroInicio;
        if (horasTrabajadas < 0) horasTrabajadas = 0;
        item.setHorasTrabajadas(horasTrabajadas);

        double precioMes = detalle.getMontoMes() > 0 ? detalle.getMontoMes() : alquiler.getPrecioAlquiler();
        item.setPrecioMes(precioMes);
        item.setPrecioHorasExtras(detalle.getPrecioHorasExtras());
        item.setTotalItem(precioMes + detalle.getPrecioHorasExtras());

        itemsCandidatos.add(item);
        mapAlquiler.put(item, alquiler);
        mapDetalle.put(item, detalle);
        mapHInicio.put(item, horometroInicio);
    }

    private void checkFinalizado(AtomicInteger counter) {
        if (counter.decrementAndGet() == 0) {
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    if (itemsCandidatos.isEmpty()) {
                        Toast.makeText(getContext(), "No se encontraron periodos activos", Toast.LENGTH_LONG).show();
                    } else {
                        adapter.setItems(itemsCandidatos);
                    }
                });
            }
        }
    }

    private void abrirSeleccionado() {
        List<ItemValorizacion> seleccionados = adapter.getSeleccionados();
        if (seleccionados.isEmpty()) {
            Toast.makeText(getContext(), "Selecciona al menos un periodo o equipo", Toast.LENGTH_SHORT).show();
            return;
        }
        abrirDialogoParaItem(seleccionados.get(0));
    }

    private void abrirDialogoParaItem(ItemValorizacion item) {
        AlquilerMensual alq = mapAlquiler.get(item);
        DetalleMes det = mapDetalle.get(item);
        Double hInicio = mapHInicio.get(item);

        if (alq == null) {
            Toast.makeText(getContext(), "No se pudieron obtener los datos del alquiler", Toast.LENGTH_SHORT).show();
            return;
        }

        DialogGenerarValorizacion dialog = DialogGenerarValorizacion.newInstance(alq, det, hInicio != null ? hInicio : 0.0);
        dialog.setOnValorizacionGeneradaListener((val, pdf) -> {
            if (getView() != null) {
                Navigation.findNavController(getView()).navigateUp();
            }
        });
        dialog.show(getChildFragmentManager(), "DialogGenerarValorizacion");
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof MainActivity) ((MainActivity) getActivity()).hideGlobalFab();
    }
}
