package com.example.maquirentapp.View;

import android.os.Bundle;
import android.util.Log;
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
    private String idGrupo;

    public GrupoElectrogenoFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            codigo = getArguments().getString("codigo");
            idGrupo = getArguments().getString("idGrupo");
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

        final String codigoLocal = codigo != null ? codigo :
                (getArguments() != null ? getArguments().getString("codigo") : null);
        final String idGrupoLocal = idGrupo != null ? idGrupo :
                (getArguments() != null ? getArguments().getString("idGrupo") : null);

        CardView cardMantenimientos = view.findViewById(R.id.cardMantenimientos);
        CardView cardHistorialAlquilerMensual = view.findViewById(R.id.cardHistorialAlquilerMensual);

        cardMantenimientos.setOnClickListener(v -> {
            Bundle args = new Bundle();
            if (codigoLocal != null) args.putString("codigo", codigoLocal);
            if (idGrupoLocal != null) args.putString("idGrupo", idGrupoLocal);
            Navigation.findNavController(view)
                    .navigate(R.id.action_grupoElectrogeno_to_mantenimientos, args);
        });

        cardHistorialAlquilerMensual.setOnClickListener(v -> {
            Bundle args = new Bundle();
            if (codigoLocal != null) args.putString("codigo", codigoLocal);
            if (idGrupoLocal != null) args.putString("idGrupo", idGrupoLocal);
            Navigation.findNavController(view)
                    .navigate(R.id.action_grupoElectrogeno_to_historialAlquilerMensual, args);
        });
    }
}
