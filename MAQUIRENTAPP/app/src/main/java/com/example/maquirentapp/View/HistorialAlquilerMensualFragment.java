package com.example.maquirentapp.View;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.maquirentapp.Access.GrupoElectrogenoAdapter;
import com.example.maquirentapp.Model.AlquilerMensual;
import com.example.maquirentapp.Model.GrupoElectrogeno;
import com.example.maquirentapp.Network.ApiServicio;
import com.example.maquirentapp.Network.RetrofitCliente;
import com.example.maquirentapp.R;
import com.example.maquirentapp.adaptadores.AlquilerMensualAdapter;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HistorialAlquilerMensualFragment extends Fragment {
    private RecyclerView recyclerView;
    private AlquilerMensualAdapter adapter;
    private ApiServicio api;
    private String codigo;
    public HistorialAlquilerMensualFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            codigo = getArguments().getString("codigo");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_historial_alquiler_mensual, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        String codigo = getArguments().getString("codigo");

        ExtendedFloatingActionButton btnAñadir = view.findViewById(R.id.btnAñadir);
        recyclerView = view.findViewById(R.id.recyclerAlquileres);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new AlquilerMensualAdapter();
        recyclerView.setAdapter(adapter);

        fetchAlquileresMensuales();

        btnAñadir.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putString("codigo", codigo);
            Navigation.findNavController(view)
                    .navigate(R.id.action_historialAlquilerMensual_to_nuevoAlquilerMensual, args);
        });
    }

    private void fetchAlquileresMensuales() {
        api = RetrofitCliente.getCliente().create(ApiServicio.class);
        Call<List<AlquilerMensual>> peticion = api.GetAlquileresMensuales();
        peticion.enqueue(new Callback<List<AlquilerMensual>>() {
            @Override
            public void onResponse(Call<List<AlquilerMensual>> call,
                                   Response<List<AlquilerMensual>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    adapter.setItems(response.body());
                } else {
                    Toast.makeText(getContext(),
                            "Error API: " + response.message(),
                            Toast.LENGTH_LONG).show();
                }
            }
            @Override
            public void onFailure(Call<List<AlquilerMensual>> call, Throwable t) {
                Toast.makeText(getContext(),
                        "Conexión al API: " + t.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }
}