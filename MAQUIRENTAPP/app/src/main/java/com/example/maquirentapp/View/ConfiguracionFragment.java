package com.example.maquirentapp.View;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.maquirentapp.MainActivity;
import com.example.maquirentapp.R;
import com.google.firebase.auth.FirebaseAuth;

public class ConfiguracionFragment extends Fragment {
    private Button btnSignOut;

    public ConfiguracionFragment() {
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
        View view = inflater.inflate(R.layout.fragment_configuracion, container, false);

        btnSignOut = view.findViewById(R.id.btnSignOut);
        btnSignOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signOut();
            }
        });
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        View itemHistorial = view.findViewById(R.id.item_historial);
        ((TextView) itemHistorial.findViewById(R.id.text_item_configuracion)).setText("Historial de ingresos");
        ((ImageView) itemHistorial.findViewById(R.id.icon_item_configuracion)).setImageResource(R.drawable.icon_blanco_historial_ingresos);

        View itemAccesoriosDiario = view.findViewById(R.id.item_accesorios_diario);
        ((TextView) itemAccesoriosDiario.findViewById(R.id.text_item_configuracion)).setText("Accesorios alquiler diario");
        ((ImageView) itemAccesoriosDiario.findViewById(R.id.icon_item_configuracion)).setImageResource(R.drawable.icon_blanco_accesorios_diario);

        View itemAccesoriosMensual = view.findViewById(R.id.item_accesorios_mensual);
        ((TextView) itemAccesoriosMensual.findViewById(R.id.text_item_configuracion)).setText("Accesorios alquiler mensual");
        ((ImageView) itemAccesoriosMensual.findViewById(R.id.icon_item_configuracion)).setImageResource(R.drawable.icon_blanco_accesorios_mensual);

        View itemMantenimientos = view.findViewById(R.id.item_mantenimientos);
        ((TextView) itemMantenimientos.findViewById(R.id.text_item_configuracion)).setText("Mantenimientos");
        ((ImageView) itemMantenimientos.findViewById(R.id.icon_item_configuracion)).setImageResource(R.drawable.icon_blanco_mantenimientos);

        View itemInformacionGeneral = view.findViewById(R.id.item_informacion_general);
        ((TextView) itemInformacionGeneral.findViewById(R.id.text_item_configuracion)).setText("Datos información general");
        ((ImageView) itemInformacionGeneral.findViewById(R.id.icon_item_configuracion)).setImageResource(R.drawable.icon_blanco_informacion_general);

        View itemListaGrupos = view.findViewById(R.id.item_lista_grupos);
        ((TextView) itemListaGrupos.findViewById(R.id.text_item_configuracion)).setText("Lista de grupos electrógenos");
        ((ImageView) itemListaGrupos.findViewById(R.id.icon_item_configuracion)).setImageResource(R.drawable.icon_generador);

        View itemGestionarUsuarios = view.findViewById(R.id.item_gestionar_usuarios);
        ((TextView) itemGestionarUsuarios.findViewById(R.id.text_item_configuracion)).setText("Gestionar Usuarios");
        ((ImageView) itemGestionarUsuarios.findViewById(R.id.icon_item_configuracion)).setImageResource(R.drawable.icon_blanco_gestionar_usuarios);

    }

    private void signOut() {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(getActivity(), MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);

        if (getActivity() != null) {
            getActivity().finish();
        }
    }
}