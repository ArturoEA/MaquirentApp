package com.example.maquirentapp.View;

import android.app.DatePickerDialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.Toast;

import com.example.maquirentapp.Model.AlquilerMensual;
import com.example.maquirentapp.Model.GrupoElectrogeno;
import com.example.maquirentapp.Network.ApiServicio;
import com.example.maquirentapp.Network.FirebaseServicio;
import com.example.maquirentapp.Network.RetrofitCliente;
import com.example.maquirentapp.R;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NuevoAlquilerMensualFragment extends Fragment {
    private String codigo;
    private TextInputEditText inputEmpresa, inputUbicacion, inputFechaInicial, inputFechaFinal, inputHorometroInicial, inputHorometroFinal, inputPrecioAlquiler, inputHorasMinimas;
    private CheckBox chkExtintor9kg, chkExtintor6kg, chkVarilla, chkBandeja, chkKit, chkCable, chkTablero, chkCarreta;
    private List<GrupoElectrogeno> listaDeGrupos = new ArrayList<>();
    private ExtendedFloatingActionButton btnGuardar;

    public NuevoAlquilerMensualFragment() {
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
        return inflater.inflate(R.layout.fragment_nuevo_alquiler_mensual, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        inputEmpresa = view.findViewById(R.id.inputEmpresa);
        inputUbicacion = view.findViewById(R.id.inputUbicacion);
        inputFechaInicial = view.findViewById(R.id.inputFechaInicial);
        inputFechaFinal = view.findViewById(R.id.inputFechaFinal);
        inputHorometroInicial = view.findViewById(R.id.inputHorometroInicial);
        inputHorometroFinal = view.findViewById(R.id.inputHorometroFinal);
        inputPrecioAlquiler = view.findViewById(R.id.inputPrecioAlquiler);
        inputHorasMinimas = view.findViewById(R.id.inputHorasMinimas);

        chkExtintor9kg = view.findViewById(R.id.chkExtintor9kg);
        chkExtintor6kg = view.findViewById(R.id.chkExtintor6kg);
        chkVarilla = view.findViewById(R.id.chkVarilla);
        chkBandeja = view.findViewById(R.id.chkBandeja);
        chkKit = view.findViewById(R.id.chkKit);
        chkCable = view.findViewById(R.id.chkCable);
        chkTablero = view.findViewById(R.id.chkTablero);
        chkCarreta = view.findViewById(R.id.chkCarreta);

        btnGuardar = view.findViewById(R.id.btnGuardar);

        btnGuardar.setOnClickListener(v -> guardarAlquilerMensual());


        inputFechaInicial.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    requireContext(),
                    (view1, year, month, dayOfMonth) -> {
                        Calendar chosen = Calendar.getInstance();
                        chosen.set(year, month, dayOfMonth, 0, 0, 0);

                        SimpleDateFormat isoFormat =
                                new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());

                        String fechaIso = isoFormat.format(chosen.getTime());
                        inputFechaInicial.setText(fechaIso);
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );
            datePickerDialog.show();
        });


        inputFechaFinal.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    requireContext(),
                    (view1, year, month, dayOfMonth) -> {
                        Calendar chosen2 = Calendar.getInstance();
                        chosen2.set(year, month, dayOfMonth, 0, 0, 0);

                        SimpleDateFormat isoFormat =
                                new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());

                        String fechaIso2 = isoFormat.format(chosen2.getTime());
                        inputFechaFinal.setText(fechaIso2);
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );
            datePickerDialog.show();
        });

        FirebaseServicio firebaseServicio = new FirebaseServicio();
        firebaseServicio.getGruposElectrogenos(new FirebaseServicio.OnGruposLoadedListener() {
            @Override
            public void onSuccess(List<GrupoElectrogeno> grupos) {
                listaDeGrupos.clear();
                listaDeGrupos.addAll(grupos);
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(requireContext(), "Error al cargar grupos: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

    }

    private void guardarAlquilerMensual() {
        String empresa = inputEmpresa.getText().toString();
        String ubicacion = inputUbicacion.getText().toString();
        String fechaInicial = inputFechaInicial.getText().toString();
        String fechaFinal = inputFechaFinal.getText().toString();
        double horometroInicial = Double.parseDouble(inputHorometroInicial.getText().toString());
        double horometroFinal = Double.parseDouble(inputHorometroFinal.getText().toString());
        double precioAlquiler = Double.parseDouble(inputPrecioAlquiler.getText().toString());
        int horasMinimas = Integer.parseInt(inputHorasMinimas.getText().toString());

        AlquilerMensual alquiler = new AlquilerMensual();
        alquiler.setNombreCliente(empresa);
        alquiler.setUbicacion(ubicacion);
        alquiler.setFechaInicial(fechaInicial);
        alquiler.setFechaFinal(fechaFinal);
        alquiler.setHorometroInicial(horometroInicial);
        alquiler.setHorometroFinal(horometroFinal);
        alquiler.setPrecioAlquiler(precioAlquiler);
        alquiler.setHorasMinimas(horasMinimas);

        alquiler.setExtintor9kg(chkExtintor9kg.isChecked());
        alquiler.setExtintor6kg(chkExtintor6kg.isChecked());
        alquiler.setVarillaTierra(chkVarilla.isChecked());
        alquiler.setBandejaAntiderrame(chkBandeja.isChecked());
        alquiler.setKitAntiderrame(chkKit.isChecked());
        alquiler.setCableElectrico(chkCable.isChecked());
        alquiler.setTableroDistribucion(chkTablero.isChecked());
        alquiler.setCarreta(chkCarreta.isChecked());

        String idGrupo = obtenerIdGrupoPorCodigo(codigo);
        if (idGrupo == null) {
            Toast.makeText(getContext(), "Código de grupo inválido", Toast.LENGTH_SHORT).show();
            return;
        }
        alquiler.setIdGrupo(idGrupo);

        FirebaseServicio firebaseServicio = new FirebaseServicio();
        firebaseServicio.crearAlquilerMensual(alquiler, new FirebaseServicio.OnAlquilerCreatedListener() {
            @Override
            public void onSuccess(AlquilerMensual alquilerCreado) {
                Toast.makeText(getContext(), "Alquiler registrado correctamente", Toast.LENGTH_SHORT).show();
                requireActivity().onBackPressed();
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private String obtenerIdGrupoPorCodigo(String codigoBuscado) {
        for (GrupoElectrogeno g : listaDeGrupos) {
            if (g.getCodigo().equalsIgnoreCase(codigoBuscado)) {
                return g.getId();
            }
        }
        return null;
    }



}