package com.example.maquirentapp.View;

import android.app.DatePickerDialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.maquirentapp.Model.Accesorio;
import com.example.maquirentapp.Model.AlquilerMensual;
import com.example.maquirentapp.Network.FirebaseServicio;
import com.example.maquirentapp.R;
import com.example.maquirentapp.Access.AccesorioSeleccionAdapter;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class NuevoAlquilerMensualFragment extends Fragment {
    private String idGrupo, alquilerId;
    private boolean modoEdicion = false;
    private boolean modoSoloLectura = false;
    private boolean editandoActualmente = false;

    private TextInputEditText inputEmpresa, inputUbicacion, inputFechaInicial, inputFechaFinal,
            inputHorometroInicial, inputHorometroFinal, inputPrecioAlquiler, inputHorasMinimas, inputPrecioHoraExtra;

    private RecyclerView recyclerAccesorios;
    private AccesorioSeleccionAdapter adapterAccesorios;
    private FirebaseServicio firebaseServicio;

    public NuevoAlquilerMensualFragment() { }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            idGrupo = getArguments().getString("idGrupo");
            alquilerId = getArguments().getString("alquilerId");
            modoSoloLectura = getArguments().getBoolean("modoSoloLectura", false);
            modoEdicion = alquilerId != null;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_nuevo_alquiler_mensual, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        firebaseServicio = new FirebaseServicio();

        inputEmpresa = view.findViewById(R.id.inputEmpresa);
        inputUbicacion = view.findViewById(R.id.inputUbicacion);
        inputFechaInicial = view.findViewById(R.id.inputFechaInicial);
        inputFechaFinal = view.findViewById(R.id.inputFechaFinal);
        inputHorometroInicial = view.findViewById(R.id.inputHorometroInicial);
        inputHorometroFinal = view.findViewById(R.id.inputHorometroFinal);
        inputPrecioAlquiler = view.findViewById(R.id.inputPrecioAlquiler);
        inputHorasMinimas = view.findViewById(R.id.inputHorasMinimas);
        inputPrecioHoraExtra = view.findViewById(R.id.inputPrecioHoraExtra);

        recyclerAccesorios = view.findViewById(R.id.recyclerAccesorios);
        recyclerAccesorios.setLayoutManager(new LinearLayoutManager(getContext()));
        adapterAccesorios = new AccesorioSeleccionAdapter();
        recyclerAccesorios.setAdapter(adapterAccesorios);

        configurarDatePickers();
        cargarAccesoriosMensuales();

        if (modoEdicion) {
            cargarDatosAlquiler();
        }

        // Si está en modo solo lectura, deshabilitar todos los campos
        if (modoSoloLectura) {
            deshabilitarCampos();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        configureGlobalFab();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (getActivity() instanceof com.example.maquirentapp.MainActivity) {
            ((com.example.maquirentapp.MainActivity) getActivity()).hideGlobalFab();
        } else {
            View activityFab = getActivity() != null ? getActivity().findViewById(R.id.btnGlobal) : null;
            if (activityFab != null) activityFab.setVisibility(View.GONE);
        }
    }

    private void configurarDatePickers() {
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
    }

    private void cargarAccesoriosMensuales() {
        firebaseServicio.getAccesorios("mensual", new FirebaseServicio.OnAccesoriosLoadedListener() {
            @Override
            public void onSuccess(List<Accesorio> accesorios) {
                adapterAccesorios.setItems(accesorios);
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(getContext(), "Error al cargar accesorios: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void cargarDatosAlquiler() {
        firebaseServicio.getAlquilerMensualPorId(alquilerId, new FirebaseServicio.OnAlquilerMensualLoadedListener() {
            @Override
            public void onSuccess(AlquilerMensual alquiler) {
                inputEmpresa.setText(alquiler.getNombreCliente());
                inputUbicacion.setText(alquiler.getUbicacion());
                inputFechaInicial.setText(alquiler.getFechaInicial());
                inputFechaFinal.setText(alquiler.getFechaFinal());
                inputHorometroInicial.setText(String.valueOf(alquiler.getHorometroInicial()));
                inputHorometroFinal.setText(String.valueOf(alquiler.getHorometroFinal()));
                inputPrecioAlquiler.setText(String.valueOf(alquiler.getPrecioAlquiler()));
                inputHorasMinimas.setText(String.valueOf(alquiler.getHorasMinimas()));
                inputPrecioHoraExtra.setText(String.valueOf(alquiler.getPrecioHoraExtra()));

                if (alquiler.getAccesoriosIds() != null) {
                    adapterAccesorios.setAccesoriosSeleccionados(alquiler.getAccesoriosIds());
                }
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(getContext(), "Error al cargar datos: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deshabilitarCampos() {
        inputEmpresa.setEnabled(false);
        inputUbicacion.setEnabled(false);
        inputFechaInicial.setEnabled(false);
        inputFechaFinal.setEnabled(false);
        inputHorometroInicial.setEnabled(false);
        inputHorometroFinal.setEnabled(false);
        inputPrecioAlquiler.setEnabled(false);
        inputHorasMinimas.setEnabled(false);
        inputPrecioHoraExtra.setEnabled(false);

        // Deshabilitar el recycler de accesorios
        adapterAccesorios.setClickEnabled(false);
    }

    private void habilitarCampos() {
        inputEmpresa.setEnabled(true);
        inputUbicacion.setEnabled(true);
        inputFechaInicial.setEnabled(true);
        inputFechaFinal.setEnabled(true);
        inputHorometroInicial.setEnabled(true);
        inputHorometroFinal.setEnabled(true);
        inputPrecioAlquiler.setEnabled(true);
        inputHorasMinimas.setEnabled(true);
        inputPrecioHoraExtra.setEnabled(true);

        // Habilitar el recycler de accesorios
        adapterAccesorios.setClickEnabled(true);
    }

    private void configureGlobalFab() {
        View hostView = getView();
        if (hostView == null) return;

        // Si está en modo solo lectura, mostrar botón "Editar"
        if (modoSoloLectura && !editandoActualmente) {
            if (getActivity() instanceof com.example.maquirentapp.MainActivity) {
                com.example.maquirentapp.MainActivity main = (com.example.maquirentapp.MainActivity) getActivity();
                main.showGlobalFab(
                        "Editar",
                        R.drawable.icon_editar_blanco,
                        v -> {
                            editandoActualmente = true;
                            habilitarCampos();
                            configureGlobalFab(); // Actualizar FAB
                        }
                );
            } else {
                View activityFab = getActivity() != null ? getActivity().findViewById(R.id.btnGlobal) : null;
                if (activityFab != null && activityFab instanceof ExtendedFloatingActionButton) {
                    ExtendedFloatingActionButton fab = (ExtendedFloatingActionButton) activityFab;
                    fab.setText("Editar");
                    try {
                        fab.setIcon(ContextCompat.getDrawable(requireContext(), R.drawable.icon_editar_blanco));
                    } catch (Exception ignored) {}
                    fab.setOnClickListener(v -> {
                        editandoActualmente = true;
                        habilitarCampos();
                        configureGlobalFab(); // Actualizar FAB
                    });
                    fab.setVisibility(View.VISIBLE);
                }
            }
        } else {
            // Modo normal o editando: mostrar botón "Guardar"
            if (getActivity() instanceof com.example.maquirentapp.MainActivity) {
                com.example.maquirentapp.MainActivity main = (com.example.maquirentapp.MainActivity) getActivity();
                main.showGlobalFab(
                        "Guardar",
                        R.drawable.icon_guardar_blanco,
                        v -> guardarAlquilerMensual()
                );
            } else {
                View activityFab = getActivity() != null ? getActivity().findViewById(R.id.btnGlobal) : null;
                if (activityFab != null && activityFab instanceof ExtendedFloatingActionButton) {
                    ExtendedFloatingActionButton fab = (ExtendedFloatingActionButton) activityFab;
                    fab.setText("Guardar");
                    try {
                        fab.setIcon(ContextCompat.getDrawable(requireContext(), R.drawable.icon_guardar_blanco));
                    } catch (Exception ignored) {}
                    fab.setOnClickListener(v -> guardarAlquilerMensual());
                    fab.setVisibility(View.VISIBLE);
                }
            }
        }
    }

    private void guardarAlquilerMensual() {
        String empresa = inputEmpresa.getText().toString().trim();
        String ubicacion = inputUbicacion.getText().toString().trim();
        String fechaInicial = inputFechaInicial.getText().toString().trim();
        String fechaFinal = inputFechaFinal.getText().toString().trim();
        String hIniStr = inputHorometroInicial.getText().toString().trim();
        String hFinStr = inputHorometroFinal.getText().toString().trim();
        String precioStr = inputPrecioAlquiler.getText().toString().trim();
        String horasMinStr = inputHorasMinimas.getText().toString().trim();
        String precioHoraExtraStr = inputPrecioHoraExtra.getText().toString().trim();

        if (empresa.isEmpty() || ubicacion.isEmpty() || fechaInicial.isEmpty() || hIniStr.isEmpty() || precioStr.isEmpty() || horasMinStr.isEmpty()) {
            Toast.makeText(getContext(), "Por favor completa todos los campos obligatorios", Toast.LENGTH_SHORT).show();
            return;
        }

        double horometroInicial, horometroFinal, precioAlquiler, precioHoraExtra = 0;
        int horasMinimas;

        try {
            horometroInicial = Double.parseDouble(hIniStr);
            horometroFinal = Double.parseDouble(hFinStr);
            precioAlquiler = Double.parseDouble(precioStr);
            horasMinimas = Integer.parseInt(horasMinStr);
            if (!precioHoraExtraStr.isEmpty()) {
                precioHoraExtra = Double.parseDouble(precioHoraExtraStr);
            }
        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "Verifica que los campos numéricos sean válidos", Toast.LENGTH_SHORT).show();
            return;
        }

        AlquilerMensual alquiler = modoEdicion ? new AlquilerMensual() : new AlquilerMensual();
        if (modoEdicion) {
            alquiler.setId(alquilerId);
        }

        alquiler.setNombreCliente(empresa);
        alquiler.setUbicacion(ubicacion);
        alquiler.setFechaInicial(fechaInicial);
        alquiler.setFechaFinal(fechaFinal);
        alquiler.setHorometroInicial(horometroInicial);
        alquiler.setHorometroFinal(horometroFinal);
        alquiler.setPrecioAlquiler(precioAlquiler);
        alquiler.setHorasMinimas(horasMinimas);
        alquiler.setPrecioHoraExtra(precioHoraExtra);
        alquiler.setIdGrupo(idGrupo);
        alquiler.setAccesoriosIds(adapterAccesorios.getAccesoriosSeleccionados());

        if (modoEdicion) {
            firebaseServicio.actualizarAlquilerMensual(alquiler, new FirebaseServicio.OnAlquilerUpdatedListener() {
                @Override
                public void onSuccess() {
                    Toast.makeText(getContext(), "Alquiler actualizado correctamente", Toast.LENGTH_SHORT).show();
                    requireActivity().onBackPressed();
                }

                @Override
                public void onError(Exception e) {
                    Toast.makeText(getContext(), "Error al actualizar: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        } else {
            firebaseServicio.crearAlquilerMensual(alquiler, new FirebaseServicio.OnAlquilerCreatedListener() {
                @Override
                public void onSuccess(AlquilerMensual alquilerCreado) {
                    Toast.makeText(getContext(), "Alquiler registrado correctamente", Toast.LENGTH_SHORT).show();
                    requireActivity().onBackPressed();
                }

                @Override
                public void onError(Exception e) {
                    Toast.makeText(getContext(), "Error al guardar: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        }
    }
}