package com.example.maquirentapp.View;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.maquirentapp.Model.AlquilerMensual;
import com.example.maquirentapp.Model.DetalleMes;
import com.example.maquirentapp.Model.GrupoElectrogeno;
import com.example.maquirentapp.Model.Ingreso;
import com.example.maquirentapp.Model.Mantenimiento;
import com.example.maquirentapp.Repository.AlquilerRepository;
import com.example.maquirentapp.Repository.Callback;
import com.example.maquirentapp.R;
import com.example.maquirentapp.Utils.MantenimientoSaludManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class GrupoElectrogenoFragment extends Fragment {
    private String codigo;
    private String idGrupo;
    private AlquilerRepository alquilerRepository;
    private FirebaseFirestore db;

    private Spinner spinnerMeses;
    private TextView tvTotalSOL, tvTotalUSD;
    private TextView tvEstadoMantenimientoSub, tvBadgeSaludMantenimiento, btnActualizarHorometroCampo;
    private GrupoElectrogeno grupoActual;
    private double horometroActualEquipo = 0.0;

    private int mesSeleccionado = Calendar.getInstance().get(Calendar.MONTH); // Mes actual (0-11)
    private int anioActual = Calendar.getInstance().get(Calendar.YEAR);
    private double acumuladoSOL = 0;
    private double acumuladoUSD = 0;
    private boolean isSpinnerInitialLoad = true;
    private int idCargaActual = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            codigo = getArguments().getString("codigo");
            idGrupo = getArguments().getString("idGrupo");
        }
        alquilerRepository = new AlquilerRepository();
        db = FirebaseFirestore.getInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_grupo_electrogeno, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final String codigoLocal = codigo != null ? codigo :
                (getArguments() != null ? getArguments().getString("codigo") : null);
        final String idGrupoLocal = idGrupo != null ? idGrupo :
                (getArguments() != null ? getArguments().getString("idGrupo") : null);

        isSpinnerInitialLoad = true;
        idCargaActual = 0;

        // Inicializar vistas
        spinnerMeses = view.findViewById(R.id.spinnerMeses);
        tvTotalSOL = view.findViewById(R.id.tvTotalSOL);
        tvTotalUSD = view.findViewById(R.id.tvTotalUSD);

        configurarSpinnerMeses();

        tvEstadoMantenimientoSub = view.findViewById(R.id.tvEstadoMantenimientoSub);
        tvBadgeSaludMantenimiento = view.findViewById(R.id.tvBadgeSaludMantenimiento);
        btnActualizarHorometroCampo = view.findViewById(R.id.btnActualizarHorometroCampo);

        if (btnActualizarHorometroCampo != null) {
            btnActualizarHorometroCampo.setOnClickListener(v -> mostrarDialogoActualizarHorometro(codigoLocal, idGrupoLocal));
        }

        cargarEstadoSaludMantenimiento(codigoLocal, idGrupoLocal);

        CardView cardMantenimientos = view.findViewById(R.id.cardMantenimientos);
        CardView cardHistorialAlquilerMensual = view.findViewById(R.id.cardHistorialAlquilerMensual);
        CardView cardInformacionGeneral = view.findViewById(R.id.cardInformacionGeneral);
        CardView cardHistorialAlquilerDiario = view.findViewById(R.id.cardHistorialAlquilerDiario);
        CardView cardFotosEquipo = view.findViewById(R.id.cardFotosEquipo);

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

        cardHistorialAlquilerDiario.setOnClickListener(v -> {
            Bundle args = new Bundle();
            if (codigoLocal != null) args.putString("codigo", codigoLocal);
            if (idGrupoLocal != null) args.putString("idGrupo", idGrupoLocal);
            Navigation.findNavController(view)
                    .navigate(R.id.action_grupoElectrogeno_to_historialAlquilerDiario, args);
        });

        cardFotosEquipo.setOnClickListener(v -> {
            Bundle args = new Bundle();
            if (idGrupoLocal != null) args.putString("idGrupo", idGrupoLocal);
            if (codigoLocal != null) args.putString("codigo", codigoLocal);
            Navigation.findNavController(view)
                    .navigate(R.id.action_grupoElectrogeno_to_fotosEquipo, args);
        });

        cardInformacionGeneral.setOnClickListener(v -> {
            Bundle args = new Bundle();
            if (idGrupoLocal != null) args.putString("idGrupo", idGrupoLocal);
            if (codigoLocal != null) args.putString("codigo", codigoLocal);

            Navigation.findNavController(view)
                    .navigate(R.id.action_grupoElectrogeno_to_informacionGeneral, args);
        });

        // Cargar totales del mes actual
        cargarTotalesMes(mesSeleccionado);
    }

    private void configurarSpinnerMeses() {
        List<String> meses = new ArrayList<>();
        String[] nombresMeses = {
                "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
                "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
        };

        for (String mes : nombresMeses) {
            meses.add(mes);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                R.layout.spinner_item_mes,
                meses
        );
        adapter.setDropDownViewResource(R.layout.spinner_item_black);
        spinnerMeses.setAdapter(adapter);

        // Seleccionar el mes actual por defecto
        spinnerMeses.setSelection(mesSeleccionado);

        spinnerMeses.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (isSpinnerInitialLoad) {
                    isSpinnerInitialLoad = false;
                    return;
                }
                mesSeleccionado = position;
                cargarTotalesMes(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }
    private void cargarTotalesMes(int mes) {
        String grupoTarget = (idGrupo != null && !idGrupo.isEmpty()) ? idGrupo : codigo;
        if (grupoTarget == null) return;
        idCargaActual++;
        final int idDeEstaCarga = idCargaActual;

        acumuladoSOL = 0;
        acumuladoUSD = 0;
        actualizarTotal(tvTotalSOL, 0, "SOL");
        actualizarTotal(tvTotalUSD, 0, "USD");

        int mesFirestore = mes + 1; // Convertir 0-11 del Spinner a 1-12 de Firestore
        alquilerRepository.getIngresosPorGrupoYMes(grupoTarget, mesFirestore, anioActual,
                new Callback<List<Ingreso>>() {
                    @Override
                    public void onSuccess(List<Ingreso> ingresos) {
                        if (idDeEstaCarga != idCargaActual) {
                            Log.d("GrupoElectrogeno", "Respuesta antigua ignorada (ID: " + idDeEstaCarga + ")");
                            return;
                        }
                        for (Ingreso ingreso : ingresos) {
                            if (ingreso.getMoneda() != null && ingreso.getMoneda().equals("USD")) {
                                acumuladoUSD += ingreso.getMonto();
                            } else {
                                acumuladoSOL += ingreso.getMonto();
                            }
                        }

                        actualizarTotal(tvTotalSOL, acumuladoSOL, "SOL");
                        actualizarTotal(tvTotalUSD, acumuladoUSD, "USD");
                    }

                    @Override
                    public void onError(Exception e) {
                        if (idDeEstaCarga != idCargaActual) {
                            return;
                        }
                        Toast.makeText(getContext(), "Error al cargar ingresos: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });

        // (Aquí iría la llamada a Alquileres Diarios, que también sumarían a 'acumuladoSOL/USD')
    }
    private void actualizarTotal(TextView textView, double total, String moneda) {
        String simbolo = moneda.equals("USD") ? "$" : "S/.";
        String totalFormateado = String.format(Locale.US, "%s %.2f", simbolo, total);
        textView.setText(totalFormateado);
    }

    private void cargarEstadoSaludMantenimiento(String codigoGrupo, String idGrupoDoc) {
        if (codigoGrupo == null && idGrupoDoc == null) return;
        String idTarget = idGrupoDoc != null && !idGrupoDoc.isEmpty() ? idGrupoDoc : codigoGrupo;

        // 1. Obtener datos del grupo electrógeno
        db.collection("gruposElectrogenos").document(idTarget).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                grupoActual = doc.toObject(GrupoElectrogeno.class);
                if (grupoActual != null) {
                    grupoActual.setId(doc.getId());
                    horometroActualEquipo = grupoActual.getHorometroActual();
                }
            }

            // 2. Obtener mantenimientos para calcular el delta exacto
            String codigoBuscado = codigoGrupo != null ? codigoGrupo : (grupoActual != null ? grupoActual.getCodigo() : idTarget);
            db.collection("mantenimientos")
                    .whereEqualTo("codigoGrupo", codigoBuscado)
                    .get()
                    .addOnSuccessListener(snapM -> {
                        List<Mantenimiento> listaM = new ArrayList<>();
                        for (QueryDocumentSnapshot mDoc : snapM) {
                            Mantenimiento m = mDoc.toObject(Mantenimiento.class);
                            m.setId(mDoc.getId());
                            listaM.add(m);
                        }

                        // Si el grupo no tenía horómetro guardado, inferirlo del historial
                        if (horometroActualEquipo <= 0) {
                            for (Mantenimiento m : listaM) {
                                double h = MantenimientoSaludManager.parseDoubleSafe(m.getHorometro());
                                if (h > horometroActualEquipo) horometroActualEquipo = h;
                            }
                            if (grupoActual != null) grupoActual.setHorometroActual(horometroActualEquipo);
                        }

                        MantenimientoSaludManager.ResultadoSalud salud =
                                MantenimientoSaludManager.evaluarSalud(grupoActual, listaM);

                        if (isAdded() && tvBadgeSaludMantenimiento != null && tvEstadoMantenimientoSub != null) {
                            tvBadgeSaludMantenimiento.setText(salud.badgeTexto);
                            try {
                                tvBadgeSaludMantenimiento.setBackgroundTintList(
                                        ColorStateList.valueOf(Color.parseColor(salud.colorHex)));
                            } catch (Exception ignored) {}

                            String textoHorometro = salud.horometroActual > 0
                                    ? String.format(Locale.US, "Últ. horómetro: %.0f h", salud.horometroActual)
                                    : "Últ. horómetro: --";
                            tvEstadoMantenimientoSub.setText(textoHorometro);
                            tvEstadoMantenimientoSub.setTextColor(Color.parseColor("#B0BEC5"));

                            if (salud.estado == MantenimientoSaludManager.EstadoSalud.VENCIDO_HORAS ||
                                    salud.estado == MantenimientoSaludManager.EstadoSalud.VENCIDO_TIEMPO) {
                                // Generar automáticamente tarea preventiva si no existe
                                MantenimientoSaludManager.crearTareaPreventivaSiNoExiste(codigoBuscado, salud.horometroActual, null, null);
                            }
                        }
                    });
        });
    }

    private void mostrarDialogoActualizarHorometro(String codigoGrupo, String idGrupoDoc) {
        if (getContext() == null) return;
        String idTarget = idGrupoDoc != null && !idGrupoDoc.isEmpty() ? idGrupoDoc : codigoGrupo;
        String codigoFinal = codigoGrupo != null ? codigoGrupo : (grupoActual != null ? grupoActual.getCodigo() : idTarget);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext(), R.style.DialogoConFuenteAnta);
        builder.setTitle("Actualizar Horómetro de Campo");

        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 10);

        TextView tvInfo = new TextView(requireContext());
        tvInfo.setText(String.format(Locale.US, "Máquina: %s\nÚltimo horómetro registrado: %.1f h", codigoFinal, horometroActualEquipo));
        tvInfo.setTextColor(Color.WHITE);
        tvInfo.setTextSize(14);
        tvInfo.setPadding(0, 0, 0, 20);
        layout.addView(tvInfo);

        final EditText input = new EditText(requireContext());
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setHint("Nuevo horómetro reportado");
        input.setTextColor(Color.WHITE);
        input.setHintTextColor(Color.GRAY);
        layout.addView(input);

        builder.setView(layout);

        builder.setPositiveButton("Guardar", (dialog, which) -> {
            String valorStr = input.getText().toString().trim();
            if (valorStr.isEmpty()) {
                Toast.makeText(getContext(), "Ingresa un horómetro válido", Toast.LENGTH_SHORT).show();
                return;
            }

            double nuevoH = MantenimientoSaludManager.parseDoubleSafe(valorStr);
            MantenimientoSaludManager.ResultadoValidacionHorometro val =
                    MantenimientoSaludManager.validarHorometro(horometroActualEquipo, nuevoH);

            if (!val.esValido) {
                new MaterialAlertDialogBuilder(requireContext(), R.style.DialogoConFuenteAnta)
                        .setTitle("Horómetro Inválido")
                        .setMessage(val.mensajeError)
                        .setPositiveButton("Entendido", null)
                        .show();
                return;
            }

            if (val.esSaltoGrande) {
                new MaterialAlertDialogBuilder(requireContext(), R.style.DialogoConFuenteAnta)
                        .setTitle("Confirmar incremento de horas")
                        .setMessage(val.mensajeError)
                        .setPositiveButton("Sí, es correcto", (d2, w2) -> guardarHorometroConfirmado(idTarget, codigoFinal, nuevoH))
                        .setNegativeButton("Corregir", (d2, w2) -> mostrarDialogoActualizarHorometro(codigoGrupo, idGrupoDoc))
                        .show();
            } else {
                guardarHorometroConfirmado(idTarget, codigoFinal, nuevoH);
            }
        });

        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }

    private void guardarHorometroConfirmado(String idDoc, String codigoGrupo, double nuevoH) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("horometroActual", nuevoH);
        updates.put("fechaActualizacionHorometro", System.currentTimeMillis());

        db.collection("gruposElectrogenos").document(idDoc).update(updates)
                .addOnSuccessListener(aVoid -> {
                    horometroActualEquipo = nuevoH;
                    if (grupoActual != null) grupoActual.setHorometroActual(nuevoH);
                    Toast.makeText(getContext(), "Horómetro actualizado correctamente", Toast.LENGTH_SHORT).show();
                    cargarEstadoSaludMantenimiento(codigoGrupo, idDoc);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error actualizando horómetro: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}