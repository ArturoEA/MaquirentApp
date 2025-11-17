package com.example.maquirentapp.View;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.maquirentapp.Access.AccesorioSeleccionAdapter;
import com.example.maquirentapp.Model.Accesorio;
import com.example.maquirentapp.Model.AlquilerDia;
import com.example.maquirentapp.Model.GrupoElectrogeno;
import com.example.maquirentapp.Network.FirebaseServicio;
import com.example.maquirentapp.R;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class NuevoAlquilerDiaFragment extends Fragment {

    private String idGrupo, alquilerId;
    private boolean modoEdicion = false;
    private boolean vieneDeHome = false;

    private TextInputEditText inputCliente, inputLugar, inputFechaInicial, inputFechaFinal,
            inputHorometroInicial, inputHorometroFinal, inputPrecio, inputHorasMaximas;
    private Spinner spinnerMoneda, spinnerGrupo;
    private TextView tvSimboloMoneda;
    private LinearLayout layoutSpinnerGrupo;
    private Button btnFinalizar;
    private RecyclerView recyclerAccesorios;

    private AccesorioSeleccionAdapter adapterAccesorios;
    private FirebaseServicio firebaseServicio;
    private FirebaseAuth firebaseAuth;
    private AlquilerDia alquilerActual;
    private List<GrupoElectrogeno> listaGrupos = new ArrayList<>(); // Para el spinner

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        firebaseServicio = new FirebaseServicio();
        firebaseAuth = FirebaseAuth.getInstance();
        if (getArguments() != null) {
            idGrupo = getArguments().getString("idGrupo");
            alquilerId = getArguments().getString("alquilerId");
            modoEdicion = (alquilerId != null);
            vieneDeHome = (idGrupo == null && !modoEdicion);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_nuevo_alquiler_dia, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        inicializarVistas(view);
        configurarDatePickers();
        configurarSpinnerMoneda();
        configurarListeners();

        cargarAccesoriosDiarios();

        if (modoEdicion) {
            layoutSpinnerGrupo.setVisibility(View.GONE);
            cargarDatosAlquiler();
        } else if (vieneDeHome) {
            layoutSpinnerGrupo.setVisibility(View.VISIBLE);
            cargarSpinnerGrupos();
        } else {
            layoutSpinnerGrupo.setVisibility(View.GONE);
        }
    }

    private void inicializarVistas(View view) {
        layoutSpinnerGrupo = view.findViewById(R.id.layoutSpinnerGrupo);
        spinnerGrupo = view.findViewById(R.id.spinnerGrupo);
        inputCliente = view.findViewById(R.id.clienteEditText);
        inputLugar = view.findViewById(R.id.ubicacionEditText);
        spinnerMoneda = view.findViewById(R.id.spinnerMoneda);
        tvSimboloMoneda = view.findViewById(R.id.tvSimboloMoneda);
        inputFechaInicial = view.findViewById(R.id.fechaInicialEditText);
        inputFechaFinal = view.findViewById(R.id.fechaFinalEditText);
        inputHorometroInicial = view.findViewById(R.id.horometroInicialEditText);
        inputHorometroFinal = view.findViewById(R.id.horometroFinalEditText);
        inputPrecio = view.findViewById(R.id.inputPrecioAlquiler);
        inputHorasMaximas = view.findViewById(R.id.horasMaxDia);
        btnFinalizar = view.findViewById(R.id.btnFinalizarAlquilerDiario);

        recyclerAccesorios = view.findViewById(R.id.recyclerAccesorios);
        recyclerAccesorios.setLayoutManager(new LinearLayoutManager(getContext()));
        adapterAccesorios = new AccesorioSeleccionAdapter();
        recyclerAccesorios.setAdapter(adapterAccesorios);

        inputHorasMaximas.setText("10");
    }

    private void configurarSpinnerMoneda() {
        List<String> monedas = new ArrayList<>();
        monedas.add("SOL");
        monedas.add("USD");
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, monedas);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMoneda.setAdapter(adapter);
        spinnerMoneda.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                tvSimboloMoneda.setText(monedas.get(position).equals("USD") ? "$" : "S/.");
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void configurarDatePickers() {
        // Lógica para inputFechaInicial
        inputFechaInicial.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            DatePickerDialog dpd = new DatePickerDialog(getContext(), (picker, year, month, day) -> {
                inputFechaInicial.setText(String.format(Locale.US, "%02d/%02d/%d", day, month + 1, year));
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
            dpd.show();
        });

        // Lógica para inputFechaFinal
        inputFechaFinal.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            DatePickerDialog dpd = new DatePickerDialog(getContext(), (picker, year, month, day) -> {
                inputFechaFinal.setText(String.format(Locale.US, "%02d/%02d/%d", day, month + 1, year));
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
            dpd.show();
        });
    }

    private void configurarListeners() {
        btnFinalizar.setOnClickListener(v -> mostrarDialogoFinalizar());
    }

    private void cargarSpinnerGrupos() {
        firebaseServicio.getGruposParaSpinner(new FirebaseServicio.OnGruposLoadedListener() {
            @Override
            public void onSuccess(List<GrupoElectrogeno> grupos) {
                listaGrupos = grupos;
                List<String> nombresGrupos = new ArrayList<>();
                nombresGrupos.add("Seleccione un grupo..."); // Hint
                for (GrupoElectrogeno g : grupos) {
                    nombresGrupos.add(g.getCodigo());
                }
                ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
                        android.R.layout.simple_spinner_item, nombresGrupos);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerGrupo.setAdapter(adapter);
            }
            @Override
            public void onError(Exception e) {
                Toast.makeText(getContext(), "Error al cargar grupos", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void cargarAccesoriosDiarios() {
        firebaseServicio.getAccesorios("diario", new FirebaseServicio.OnAccesoriosLoadedListener() {
            @Override
            public void onSuccess(List<Accesorio> accesorios) {
                adapterAccesorios.setItems(accesorios);
                // Si estamos editando, seleccionamos los guardados
                if (modoEdicion && alquilerActual != null && alquilerActual.getAccesoriosIds() != null) {
                    adapterAccesorios.setAccesoriosSeleccionados(alquilerActual.getAccesoriosIds());
                }
            }
            @Override
            public void onError(Exception e) {
                Toast.makeText(getContext(), "Error al cargar accesorios: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void cargarDatosAlquiler() {
        firebaseServicio.getAlquilerDiaPorId(alquilerId, new FirebaseServicio.OnAlquilerDiaLoadedListener() {
            @Override
            public void onSuccess(AlquilerDia alquiler) {
                alquilerActual = alquiler;
                idGrupo = alquiler.getIdGrupo();
                inputCliente.setText(alquiler.getNombreCliente());
                inputLugar.setText(alquiler.getUbicacion());
                inputFechaInicial.setText(alquiler.getFechaInicial());
                inputFechaFinal.setText(alquiler.getFechaFinal());
                inputHorometroInicial.setText(String.valueOf(alquiler.getHorometroInicial()));
                inputHorometroFinal.setText(String.valueOf(alquiler.getHorometroFinal()));
                inputPrecio.setText(String.valueOf(alquiler.getPrecioTotal()));
                inputHorasMaximas.setText(String.valueOf(alquiler.getHorasMaximas()));

                spinnerMoneda.setSelection("USD".equals(alquiler.getMoneda()) ? 1 : 0);
                adapterAccesorios.setAccesoriosSeleccionados(alquiler.getAccesoriosIds());

                if (alquiler.isFinalizado()) {
                    deshabilitarCampos();
                }
            }
            @Override
            public void onError(Exception e) {
                Toast.makeText(getContext(), "Error al cargar alquiler", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean validarCampos() {
        if (inputCliente.getText().toString().isEmpty() ||
                inputLugar.getText().toString().isEmpty() ||
                inputFechaInicial.getText().toString().isEmpty() ||
                inputHorometroInicial.getText().toString().isEmpty() ||
                inputPrecio.getText().toString().isEmpty()) {
            Toast.makeText(getContext(), "Campos obligatorios: Cliente, Lugar, Fecha Inicio, H. Inicio y Precio", Toast.LENGTH_LONG).show();
            return false;
        }
        if (vieneDeHome && spinnerGrupo.getSelectedItemPosition() == 0) {
            Toast.makeText(getContext(), "Debe seleccionar un grupo electrógeno", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void guardarAlquilerDiario(boolean finalizar) {
        if (!validarCampos()) {
            if (finalizar) btnFinalizar.setEnabled(true);
            return;
        }

        if (alquilerActual == null) {
            alquilerActual = new AlquilerDia();
        }

        // Obtener el idGrupo
        if (vieneDeHome) {
            int pos = spinnerGrupo.getSelectedItemPosition() - 1;
            idGrupo = listaGrupos.get(pos).getId();
        }
        alquilerActual.setIdGrupo(idGrupo);

        alquilerActual.setNombreCliente(inputCliente.getText().toString().trim());
        alquilerActual.setUbicacion(inputLugar.getText().toString().trim());
        alquilerActual.setFechaInicial(inputFechaInicial.getText().toString().trim());
        alquilerActual.setFechaFinal(inputFechaFinal.getText().toString().trim());

        try {
            alquilerActual.setHorometroInicial(Double.parseDouble(inputHorometroInicial.getText().toString().trim()));
            String hFinalStr = inputHorometroFinal.getText().toString().trim();
            alquilerActual.setHorometroFinal(hFinalStr.isEmpty() ? 0 : Double.parseDouble(hFinalStr));

            alquilerActual.setPrecioTotal(Double.parseDouble(inputPrecio.getText().toString().trim()));

            String horasMax = inputHorasMaximas.getText().toString().trim();
            alquilerActual.setHorasMaximas(horasMax.isEmpty() ? 10 : Double.parseDouble(horasMax));

        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "Revise los campos numéricos", Toast.LENGTH_SHORT).show();
            if (finalizar) btnFinalizar.setEnabled(true);
            return;
        }

        alquilerActual.setMoneda(spinnerMoneda.getSelectedItem().toString());
        alquilerActual.setAccesoriosIds(adapterAccesorios.getAccesoriosSeleccionados());

        if (!modoEdicion) {
            alquilerActual.setAdminUid(firebaseAuth.getUid());
        }

        if (modoEdicion) {
            firebaseServicio.actualizarAlquilerDia(alquilerActual, new FirebaseServicio.OnSimpleCallback() {
                @Override public void onSuccess() {
                    Toast.makeText(getContext(), "Alquiler actualizado", Toast.LENGTH_SHORT).show();
                    if (!finalizar) Navigation.findNavController(getView()).popBackStack();
                }
                @Override public void onError(Exception e) {
                    Toast.makeText(getContext(), "Error al actualizar", Toast.LENGTH_SHORT).show();
                    if (finalizar) btnFinalizar.setEnabled(true);
                }
            });
        } else {
            firebaseServicio.crearAlquilerDia(alquilerActual, new FirebaseServicio.OnAlquilerDiaCreadoListener() {
                @Override public void onSuccess(AlquilerDia alquiler) {
                    Toast.makeText(getContext(), "Alquiler creado", Toast.LENGTH_SHORT).show();
                    if (!finalizar) Navigation.findNavController(getView()).popBackStack();
                    else alquilerActual = alquiler;
                }
                @Override public void onError(Exception e) {
                    Toast.makeText(getContext(), "Error al crear", Toast.LENGTH_SHORT).show();
                    if (finalizar) btnFinalizar.setEnabled(true);
                }
            });
        }
    }

    private void mostrarDialogoFinalizar() {
        if (inputFechaFinal.getText().toString().isEmpty()) {
            Toast.makeText(getContext(), "Debe llenar la Fecha Final para poder finalizar", Toast.LENGTH_LONG).show();
            return;
        }

        new AlertDialog.Builder(getContext())
                .setTitle("Finalizar Alquiler")
                .setMessage("¿Estás seguro de finalizar este alquiler? Se registrará el ingreso y ya no se podrá modificar.")
                .setPositiveButton("Sí, finalizar", (dialog, which) -> {
                    btnFinalizar.setText("Finalizando...");
                    btnFinalizar.setEnabled(false);

                    guardarAlquilerDiario(true);

                    new android.os.Handler().postDelayed(() -> {
                        if (alquilerActual.getId() == null) { // Falló la creación
                            btnFinalizar.setText("Finalizar Alquiler");
                            btnFinalizar.setEnabled(true);
                            return;
                        }

                        firebaseServicio.finalizarAlquilerDiario(alquilerActual, new FirebaseServicio.OnSimpleCallback() {
                            @Override public void onSuccess() {
                                Toast.makeText(getContext(), "Alquiler finalizado e ingresos registrados", Toast.LENGTH_SHORT).show();
                                deshabilitarCampos();
                                Navigation.findNavController(getView()).popBackStack(); // Volver
                            }
                            @Override public void onError(Exception e) {
                                Toast.makeText(getContext(), "Error al finalizar: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                btnFinalizar.setText("Finalizar Alquiler");
                                btnFinalizar.setEnabled(true);
                            }
                        });
                    }, 1500);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void deshabilitarCampos() {
        btnFinalizar.setText("Alquiler Finalizado");
        btnFinalizar.setEnabled(false);
        inputCliente.setEnabled(false);
        inputLugar.setEnabled(false);
        inputFechaInicial.setEnabled(false);
        inputFechaFinal.setEnabled(false);
        inputHorometroInicial.setEnabled(false);
        inputHorometroFinal.setEnabled(false);
        inputPrecio.setEnabled(false);
        inputHorasMaximas.setEnabled(false);
        spinnerMoneda.setEnabled(false);
        spinnerGrupo.setEnabled(false);
        adapterAccesorios.setClickEnabled(false);
    }
    private void configurarFabGlobal() {
        ExtendedFloatingActionButton fab = getActivity().findViewById(R.id.btnGlobal);
        if (fab != null) {
            fab.setText("Guardar");
            fab.setIconResource(R.drawable.icon_guardar_blanco);
            fab.setVisibility(View.VISIBLE);
            fab.setOnClickListener(v -> guardarAlquilerDiario(false)); // false = solo guardar
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (alquilerActual == null || !alquilerActual.isFinalizado()) {
            configurarFabGlobal();
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