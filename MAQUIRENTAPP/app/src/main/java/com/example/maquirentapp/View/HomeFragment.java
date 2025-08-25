package com.example.maquirentapp.View;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.maquirentapp.R;
import com.example.maquirentapp.ViewModel.ScrollStateViewModel;

public class HomeFragment extends Fragment {

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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

        CardView cardNuevoAlquiler = view.findViewById(R.id.cardNuevoAlquiler);
        CardView cardCotizaciones = view.findViewById(R.id.cardCotizaciones);
        CardView cardPlanosCambioVoltaje = view.findViewById(R.id.cardPlanosVoltaje);
        CardView cardFichasTecnicas = view.findViewById(R.id.cardFichasTecnicas);

        NavController navController = Navigation.findNavController(view);

        cardNuevoAlquiler.setOnClickListener(v ->
                navController.navigate(R.id.action_homeFragment_to_nuevoAlquilerFragment));

//        cardCotizaciones.setOnClickListener(v ->
//                navController.navigate(R.id.action_homeFragment_to_cotizacionesFragment));

        cardPlanosCambioVoltaje.setOnClickListener(v -> navController.navigate(R.id.action_home_to_PlanosCambioVoltajeFragment));

        cardFichasTecnicas.setOnClickListener(v -> navController.navigate(R.id.action_home_to_FichasTecnicasFragment));


    }
}
