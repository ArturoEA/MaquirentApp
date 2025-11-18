package com.example.maquirentapp.View;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.NumberPicker;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.maquirentapp.Access.AlquilerDiarioAdapter;
import com.example.maquirentapp.Model.Accesorio;
import com.example.maquirentapp.Model.AlquilerDia;
import com.example.maquirentapp.Network.FirebaseServicio;
import com.example.maquirentapp.R;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HistorialAlquilerDiarioFragment extends Fragment {

    private String idGrupo;
    private FirebaseServicio firebaseServicio;
    private NavController navController;
    private RecyclerView recyclerView;
    private AlquilerDiarioAdapter adapter;
    private TextView tvMes, tvEmptyState;
    private ImageButton btnAnterior, btnSiguiente;
    private int mesSeleccionado;
    private int anioActual;
    private String[] nombresMeses;
    private Map<String, String> accesoriosMap = new HashMap<>();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            idGrupo = getArguments().getString("idGrupo");
        }
        firebaseServicio = new FirebaseServicio();
        nombresMeses = getResources().getStringArray(R.array.months_array);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_historial_alquiler_diario, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = Navigation.findNavController(view);

        recyclerView = view.findViewById(R.id.recyclerHistorialAlquilerDiario);
        tvMes = view.findViewById(R.id.tvMes);
        btnAnterior = view.findViewById(R.id.btnAnterior);
        btnSiguiente = view.findViewById(R.id.btnSiguiente);
        tvEmptyState = view.findViewById(R.id.tvEmptyState);

        setupRecyclerView();
        setupFiltros();
        configurarFabGlobal();

        cargarAccesorios();
    }

    @Override
    public void onResume() {
        super.onResume();
        configurarFabGlobal();
    }

    private void setupRecyclerView() {
        adapter = new AlquilerDiarioAdapter(alquiler -> {
            Bundle args = new Bundle();
            args.putString("idGrupo", alquiler.getIdGrupo());
            args.putString("alquilerId", alquiler.getId());
            args.putBoolean("modoSoloLectura", true);
            navController.navigate(R.id.action_historialAlquilerDiario_to_nuevoAlquilerDia, args);
        }, accesoriosMap);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    private void setupFiltros() {
        Calendar cal = Calendar.getInstance();
        anioActual = cal.get(Calendar.YEAR);
        mesSeleccionado = cal.get(Calendar.MONTH);

        actualizarTextViewMes();

        btnAnterior.setOnClickListener(v -> {
            mesSeleccionado--;
            if (mesSeleccionado < 0) {
                mesSeleccionado = 11;
                anioActual--;
            }
            actualizarTextViewMes();
//            cargarAlquileres();
        });

        btnSiguiente.setOnClickListener(v -> {
            mesSeleccionado++;
            if (mesSeleccionado > 11) {
                mesSeleccionado = 0;
                anioActual++;
            }
            actualizarTextViewMes();
//            cargarAlquileres();
        });

        tvMes.setOnClickListener(v -> mostrarDialogoMesAnio());
    }
    private void cargarAccesorios() {
        firebaseServicio.getAccesorios("diario", new FirebaseServicio.OnAccesoriosLoadedListener() {
            @Override
            public void onSuccess(List<Accesorio> accesorios) {
                accesoriosMap.clear();
                for (Accesorio acc : accesorios) {
                    accesoriosMap.put(acc.getId(), acc.getNombre());
                }
                cargarAlquileres();
            }

            @Override
            public void onError(Exception e) {
                Log.e("HistorialDiario", "Error al cargar accesorios", e);
                cargarAlquileres();
            }
        });
    }
    private void mostrarDialogoMesAnio() {
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_month_year_picker, null);

        NumberPicker pickerMes = dialogView.findViewById(R.id.picker_mes);
        NumberPicker pickerAnio = dialogView.findViewById(R.id.picker_anio);

        pickerMes.setMinValue(0);
        pickerMes.setMaxValue(11);
        pickerMes.setDisplayedValues(nombresMeses);
        pickerMes.setValue(mesSeleccionado);

        pickerAnio.setMinValue(2020);
        pickerAnio.setMaxValue(2050);
        pickerAnio.setValue(anioActual);

        new AlertDialog.Builder(getContext())
                .setTitle("Seleccionar Mes y Año")
                .setView(dialogView)
                .setPositiveButton("Aceptar", (dialog, which) -> {
                    mesSeleccionado = pickerMes.getValue();
                    anioActual = pickerAnio.getValue();
                    actualizarTextViewMes();
                    cargarAlquileres();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void actualizarTextViewMes() {
        String textoMes = String.format(Locale.getDefault(), "%s\n%d",
                nombresMeses[mesSeleccionado], anioActual);
        tvMes.setText(textoMes);
    }
    private void cargarAlquileres() {
        if (idGrupo == null) {
            Toast.makeText(getContext(), "Error: ID de Grupo no encontrado", Toast.LENGTH_SHORT).show();
            return;
        }
        firebaseServicio.getAlquileresDiariosPorGrupo(idGrupo, mesSeleccionado, anioActual, new FirebaseServicio.OnAlquileresDiariosLoadedListener() {
            @Override
            public void onSuccess(List<AlquilerDia> alquileres) {
                adapter.setItems(alquileres);
                if (alquileres.isEmpty()) {
                    recyclerView.setVisibility(View.GONE);
                    tvEmptyState.setVisibility(View.VISIBLE);
                } else {
                    recyclerView.setVisibility(View.VISIBLE);
                    tvEmptyState.setVisibility(View.GONE);
                }
            }
            @Override
            public void onError(Exception e) {
                Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void configurarFabGlobal() {
        if(getActivity() == null) return;
        ExtendedFloatingActionButton fab = getActivity().findViewById(R.id.btnGlobal);
        if (fab != null) {
            fab.setText("Añadir");
            fab.setIconResource(R.drawable.icon_nuevo_blanco);
            fab.setVisibility(View.VISIBLE);
            fab.setOnClickListener(v -> {
                Bundle args = new Bundle();
                args.putString("idGrupo", idGrupo);
                navController.navigate(R.id.action_historialAlquilerDiario_to_nuevoAlquilerDia, args);
            });
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if(getActivity() == null) return;
        ExtendedFloatingActionButton fab = getActivity().findViewById(R.id.btnGlobal);
        if (fab != null) {
            fab.setVisibility(View.GONE);
        }
    }
}