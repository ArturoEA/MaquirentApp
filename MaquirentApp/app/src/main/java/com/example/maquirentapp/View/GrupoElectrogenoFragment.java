package com.example.maquirentapp.View;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.maquirentapp.R;


public class GrupoElectrogenoFragment extends Fragment {
    private String codigo;
    public GrupoElectrogenoFragment() {
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
        return inflater.inflate(R.layout.fragment_grupo_electrogeno, container, false);
    }
    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        String codigo = getArguments().getString("codigo");

        CardView cardMantenimientos = view.findViewById(R.id.cardMantenimientos);
        CardView cardHistorialAlquilerMensual = view.findViewById(R.id.cardHistorialAlquilerMensual);

        cardMantenimientos.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putString("codigo", codigo);
            Navigation.findNavController(view)
                    .navigate(R.id.action_grupoElectrogeno_to_mantenimientos, args);
        });

        cardHistorialAlquilerMensual.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putString("codigo", codigo);
            Navigation.findNavController(view)
                    .navigate(R.id.action_grupoElectrogeno_to_historialAlquilerMensual, args);
        });
    }

}