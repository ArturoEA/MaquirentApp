package com.example.maquirentapp.View;

import android.os.Bundle;

import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.example.maquirentapp.Access.GrupoElectrogenoAdapter;
import com.example.maquirentapp.Model.GrupoElectrogeno;
import com.example.maquirentapp.Network.ApiServicio;
import com.example.maquirentapp.Network.RetrofitCliente;
import com.example.maquirentapp.R;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class CGEFragment extends Fragment {
    private RecyclerView recyclerView;
    private GrupoElectrogenoAdapter adapter;
    private ApiServicio api;

    public CGEFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_cge, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        NavController navController = Navigation.findNavController(view);

        recyclerView = view.findViewById(R.id.recycler_grupos_electrogenos);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new GrupoElectrogenoAdapter(grupo -> {
            Bundle args = new Bundle();
            args.putString("codigo", grupo.getCodigo());
            navController.navigate(
                    R.id.action_cge_to_grupoElectrogeno,
                    args
            );
        });
        recyclerView.setAdapter(adapter);

        fetchGruposElectrogenos();

    }

    private void fetchGruposElectrogenos() {
        api = RetrofitCliente.getCliente().create(ApiServicio.class);
        Call<List<GrupoElectrogeno>> peticion = api.GetGruposElectrogenos();
        peticion.enqueue(new Callback<List<GrupoElectrogeno>>() {
            @Override
            public void onResponse(Call<List<GrupoElectrogeno>> call,
                                   Response<List<GrupoElectrogeno>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    adapter.setItems(response.body());
                } else {
                    Toast.makeText(getContext(),
                            "Error API: " + response.message(),
                            Toast.LENGTH_LONG).show();
                }
            }
            @Override
            public void onFailure(Call<List<GrupoElectrogeno>> call, Throwable t) {
                Toast.makeText(getContext(),
                        "Conexión al API: " + t.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }
}
