package com.example.maquirentapp.View;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
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
import com.example.maquirentapp.Model.AlquilerDia;
import com.example.maquirentapp.Network.FirebaseServicio;
import com.example.maquirentapp.R;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import java.util.Calendar;
import java.util.List;

public class HistorialAlquilerDiarioFragment extends Fragment {

    private String idGrupo;
    private FirebaseServicio firebaseServicio;
    private NavController navController;
    private RecyclerView recyclerView;
    private AlquilerDiarioAdapter adapter;
    private Spinner spinnerMeses;
    private TextView tvAnio;
    private int mesSeleccionado;
    private int anioActual;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            idGrupo = getArguments().getString("idGrupo");
        }
        firebaseServicio = new FirebaseServicio();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_historial_alquiler_mensual, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = Navigation.findNavController(view);

        recyclerView = view.findViewById(R.id.recyclerAlquileres);
        spinnerMeses = view.findViewById(R.id.spinnerMeses);
        tvAnio = view.findViewById(R.id.tvAnio);

        setupRecyclerView();
        setupFiltros();
        configurarFabGlobal();
    }

    @Override
    public void onResume() {
        super.onResume();
        configurarFabGlobal();
        cargarAlquileres();
    }

    private void setupRecyclerView() {
        adapter = new AlquilerDiarioAdapter(alquiler -> {
            Bundle args = new Bundle();
            args.putString("idGrupo", alquiler.getIdGrupo());
            args.putString("alquilerId", alquiler.getId());
            navController.navigate(R.id.action_historialAlquilerDiario_to_nuevoAlquilerDia, args);
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    private void setupFiltros() {
        Calendar cal = Calendar.getInstance();
        anioActual = cal.get(Calendar.YEAR);
        mesSeleccionado = cal.get(Calendar.MONTH);
        tvAnio.setText(String.valueOf(anioActual));

        ArrayAdapter<CharSequence> adapterMeses = ArrayAdapter.createFromResource(getContext(),
                R.array.months_array, android.R.layout.simple_spinner_item);
        adapterMeses.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMeses.setAdapter(adapterMeses);
        spinnerMeses.setSelection(mesSeleccionado);

        spinnerMeses.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mesSeleccionado = position;
                cargarAlquileres();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // TODO: Añadir listeners a tvAnio para cambiar el año (igual que en Mantenimientos)
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
            }
            @Override
            public void onError(Exception e) {
                Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void configurarFabGlobal() {
        ExtendedFloatingActionButton fab = getActivity().findViewById(R.id.btnGlobal);
        if (fab != null) {
            fab.setText("Añadir");
            fab.setIconResource(R.drawable.icon_nuevo_blanco);
            fab.setVisibility(View.VISIBLE);
            fab.setOnClickListener(v -> {
                Bundle args = new Bundle();
                args.putString("idGrupo", idGrupo); // Pasa el idGrupo actual
                navController.navigate(R.id.action_historialAlquilerDiario_to_nuevoAlquilerDia, args);
            });
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        ExtendedFloatingActionButton fab = getActivity().findViewById(R.id.btnGlobal);
        if (fab != null) {
            fab.setVisibility(View.GONE);
        }
    }
}