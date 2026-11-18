package com.example.maquirentapp.View;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.maquirentapp.MainActivity;
import com.example.maquirentapp.Model.MantenimientoConfiguracion;
import com.example.maquirentapp.R;
import com.example.maquirentapp.Utils.PlanesMantenimientoDefaults;
import com.example.maquirentapp.adapters.MantenimientoConfiguracionAdapter;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MantenimientosConfiguracionFragment extends Fragment {
    private static final String TAG = "MantenimientosConfig";

    private LinearLayout containerChipsPlanes;
    private RecyclerView recyclerView;
    private LinearLayout emptyState;
    private ShimmerFrameLayout shimmerConfiguracion;
    private MantenimientoConfiguracionAdapter adapter;
    private FirebaseFirestore db;

    private String tipoEquipoSeleccionado = PlanesMantenimientoDefaults.TIPO_GRUPO_ELECTROGENO;
    private List<MantenimientoConfiguracion> mantenimientosConfiguracionList = new ArrayList<>();

    public MantenimientosConfiguracionFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_mantenimientos_configuracion, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        initViews(view);
        setupChipsPlanes();
        setupRecyclerView();

        // Sembrar planes oficiales por defecto si la base de datos se encuentra vacía
        PlanesMantenimientoDefaults.sembrarPlanesSiEstaVacio(db, this::cargarMantenimientosConfiguracion);
    }

    @Override
    public void onResume() {
        super.onResume();
        configureGlobalFab();
    }

    @Override
    public void onPause() {
        super.onPause();
        hideGlobalFab();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        hideGlobalFab();
    }

    private void configureGlobalFab() {
        if (getActivity() instanceof MainActivity) {
            MainActivity main = (MainActivity) getActivity();
            main.showGlobalFab("Añadir", R.drawable.icon_nuevo_blanco, v -> mostrarDialogoNuevoMantenimientoConfiguracion());
        }
    }

    private void hideGlobalFab() {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).hideGlobalFab();
        }
    }

    private void initViews(View view) {
        containerChipsPlanes = view.findViewById(R.id.containerChipsPlanes);
        recyclerView = view.findViewById(R.id.recyclerViewMantenimientosConfiguracion);
        emptyState = view.findViewById(R.id.emptyState);
        shimmerConfiguracion = view.findViewById(R.id.shimmerConfiguracion);
    }

    private void setupChipsPlanes() {
        if (containerChipsPlanes == null || getContext() == null) return;
        containerChipsPlanes.removeAllViews();

        agregarChipPlan(PlanesMantenimientoDefaults.TIPO_GRUPO_ELECTROGENO, "Grupos electrógenos",
                tipoEquipoSeleccionado.equals(PlanesMantenimientoDefaults.TIPO_GRUPO_ELECTROGENO));

        agregarChipPlan(PlanesMantenimientoDefaults.TIPO_COMPRESORA_AIRE, "Compresoras de aire",
                tipoEquipoSeleccionado.equals(PlanesMantenimientoDefaults.TIPO_COMPRESORA_AIRE));
    }

    private void agregarChipPlan(String idPlan, String texto, boolean isSelected) {
        if (getContext() == null) return;
        TextView chip = new TextView(getContext());
        chip.setText(texto);
        chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        chip.setPadding(36, 18, 36, 18);

        Typeface typefaceAnta = null;
        try {
            typefaceAnta = ResourcesCompat.getFont(getContext(), R.font.anta_font);
        } catch (Exception ignored) {}
        if (typefaceAnta != null) {
            chip.setTypeface(typefaceAnta);
        }

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMarginEnd(16);
        chip.setLayoutParams(params);

        actualizarEstiloChip(chip, isSelected);

        chip.setOnClickListener(v -> {
            tipoEquipoSeleccionado = idPlan;
            for (int i = 0; i < containerChipsPlanes.getChildCount(); i++) {
                View child = containerChipsPlanes.getChildAt(i);
                if (child instanceof TextView) {
                    TextView tv = (TextView) child;
                    Object tag = tv.getTag();
                    actualizarEstiloChip(tv, idPlan.equals(tag));
                }
            }
            cargarMantenimientosConfiguracion();
        });

        chip.setTag(idPlan);
        containerChipsPlanes.addView(chip);
    }

    private void actualizarEstiloChip(TextView chip, boolean isSelected) {
        if (isSelected) {
            chip.setBackgroundResource(R.drawable.bg_chip_anio_selected);
            chip.setTextColor(Color.WHITE);
        } else {
            chip.setBackgroundResource(R.drawable.bg_chip_anio_unselected);
            chip.setTextColor(Color.parseColor("#B0B0B0"));
        }
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new MantenimientoConfiguracionAdapter(mantenimientosConfiguracionList, getContext(),
                new MantenimientoConfiguracionAdapter.OnMantenimientoConfiguracionActionListener() {
                    @Override
                    public void onEditarClick(MantenimientoConfiguracion mantenimientoConfiguracion) {
                        mostrarDialogoEditarMantenimientoConfiguracion(mantenimientoConfiguracion);
                    }

                    @Override
                    public void onEliminarClick(MantenimientoConfiguracion mantenimientoConfiguracion) {
                        confirmarEliminacion(mantenimientoConfiguracion);
                    }
                });
        recyclerView.setAdapter(adapter);
    }

    private void cargarMantenimientosConfiguracion() {
        if (shimmerConfiguracion != null) {
            shimmerConfiguracion.setVisibility(View.VISIBLE);
            shimmerConfiguracion.startShimmer();
        }
        recyclerView.setVisibility(View.GONE);
        if (emptyState != null) emptyState.setVisibility(View.GONE);

        db.collection("mantenimientos_configuracion")
                .get()
                .addOnCompleteListener(task -> {
                    if (shimmerConfiguracion != null) {
                        shimmerConfiguracion.stopShimmer();
                        shimmerConfiguracion.setVisibility(View.GONE);
                    }
                    if (task.isSuccessful() && task.getResult() != null) {
                        if (task.getResult().isEmpty() || task.getResult().size() < 20) {
                            PlanesMantenimientoDefaults.sincronizarPlantillasCompletas(db, () -> {
                                db.collection("mantenimientos_configuracion").get().addOnSuccessListener(snap2 -> {
                                    procesarDocumentosConfiguracion(snap2);
                                });
                            });
                            return;
                        }
                        procesarDocumentosConfiguracion(task.getResult());
                    } else {
                        recyclerView.setVisibility(View.VISIBLE);
                        mostrarError("Error al cargar configuración de mantenimiento");
                    }
                });
    }

    private void procesarDocumentosConfiguracion(Iterable<QueryDocumentSnapshot> docs) {
        mantenimientosConfiguracionList.clear();
        if (docs != null) {
            for (QueryDocumentSnapshot document : docs) {
                MantenimientoConfiguracion item = document.toObject(MantenimientoConfiguracion.class);
                item.setId(document.getId());

                String tipo = item.getTipoEquipo();
                if (tipo == null || tipo.isEmpty()) {
                    tipo = PlanesMantenimientoDefaults.TIPO_GRUPO_ELECTROGENO;
                }

                if (tipo.equalsIgnoreCase(tipoEquipoSeleccionado)) {
                    mantenimientosConfiguracionList.add(item);
                }
            }
        }

        // Ordenar por orden lógico de subsistema y luego por intervalo de horas
        Collections.sort(mantenimientosConfiguracionList, (a, b) -> {
            int ordenA = obtenerPrioridadSubsistema(a.getSubsistema());
            int ordenB = obtenerPrioridadSubsistema(b.getSubsistema());
            if (ordenA != ordenB) {
                return Integer.compare(ordenA, ordenB);
            }
            return Integer.compare(a.getIntervaloHoras(), b.getIntervaloHoras());
        });

        actualizarUI();
    }

    private int obtenerPrioridadSubsistema(String subsistema) {
        if (subsistema == null) return 99;
        switch (subsistema) {
            case PlanesMantenimientoDefaults.SUBSISTEMA_LUBRICACION: return 1;
            case PlanesMantenimientoDefaults.SUBSISTEMA_COMBUSTIBLE: return 2;
            case PlanesMantenimientoDefaults.SUBSISTEMA_ADMISION: return 3;
            case PlanesMantenimientoDefaults.SUBSISTEMA_ELECTRICO:
            case PlanesMantenimientoDefaults.SUBSISTEMA_COMPRESION: return 4;
            case PlanesMantenimientoDefaults.SUBSISTEMA_ESTRUCTURAL: return 5;
            default: return 6;
        }
    }

    private void actualizarUI() {
        if (recyclerView != null) {
            recyclerView.post(() -> {
                if (mantenimientosConfiguracionList.isEmpty()) {
                    emptyState.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                } else {
                    emptyState.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                    adapter.actualizarLista(mantenimientosConfiguracionList);
                }
            });
        }
    }

    // ===== DIÁLOGO NUEVO / EDITAR MANTENIMIENTO CONFIGURACIÓN =====

    private void mostrarDialogoNuevoMantenimientoConfiguracion() {
        Dialog dialog = crearDialogoMantenimientoConfiguracion(null);
        dialog.show();
        ajustarAnchoDialogo(dialog);
    }

    private void mostrarDialogoEditarMantenimientoConfiguracion(MantenimientoConfiguracion item) {
        Dialog dialog = crearDialogoMantenimientoConfiguracion(item);
        dialog.show();
        ajustarAnchoDialogo(dialog);
    }

    private void ajustarAnchoDialogo(Dialog dialog) {
        if (dialog != null && dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            int screenWidth = getResources().getDisplayMetrics().widthPixels;
            int dialogWidth = (int) (screenWidth * 0.94);
            dialog.getWindow().setLayout(dialogWidth, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    private Dialog crearDialogoMantenimientoConfiguracion(@Nullable MantenimientoConfiguracion itemExistente) {
        Dialog dialog = new Dialog(requireContext());
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_nuevo_mantenimiento_configuracion, null);
        dialog.setContentView(dialogView);

        TextView tvTitulo = dialogView.findViewById(R.id.tvTituloDialog);
        Spinner spinnerTipoEquipo = dialogView.findViewById(R.id.spinnerTipoEquipo);
        Spinner spinnerSubsistema = dialogView.findViewById(R.id.spinnerSubsistema);
        TextInputEditText inputNombre = dialogView.findViewById(R.id.inputNombreMantenimientoConfiguracion);
        Spinner spinnerTipoFrecuencia = dialogView.findViewById(R.id.spinnerTipoFrecuencia);
        LinearLayout layoutCamposHoras = dialogView.findViewById(R.id.layoutCamposHoras);
        LinearLayout layoutCamposRetorno = dialogView.findViewById(R.id.layoutCamposRetorno);
        TextInputEditText inputIntervaloHoras = dialogView.findViewById(R.id.inputIntervaloHoras);
        TextInputEditText inputMesesMaximos = dialogView.findViewById(R.id.inputMesesMaximos);
        Spinner spinnerFrecuenciaRetornos = dialogView.findViewById(R.id.spinnerFrecuenciaRetornos);
        Spinner spinnerComponenteKey = dialogView.findViewById(R.id.spinnerComponenteKey);

        MaterialButton btnCancelar = dialogView.findViewById(R.id.btnCancelarDialog);
        MaterialButton btnGuardar = dialogView.findViewById(R.id.btnGuardarDialog);

        boolean esEdicion = itemExistente != null;
        if (esEdicion) {
            tvTitulo.setText("Editar Tarea de Mantenimiento");
            if (inputNombre != null) inputNombre.setText(itemExistente.getNombre());
            if (inputIntervaloHoras != null) inputIntervaloHoras.setText(String.valueOf(itemExistente.getIntervaloHoras()));
            if (inputMesesMaximos != null) inputMesesMaximos.setText(String.valueOf(itemExistente.getMesesMaximos()));
        } else {
            tvTitulo.setText("Nueva Tarea de Mantenimiento");
            if (inputIntervaloHoras != null) inputIntervaloHoras.setText("250");
            if (inputMesesMaximos != null) inputMesesMaximos.setText("6");
        }

        // 1. Configurar Spinner Tipo de Equipo
        String[] planes = new String[]{
                PlanesMantenimientoDefaults.TIPO_GRUPO_ELECTROGENO,
                PlanesMantenimientoDefaults.TIPO_COMPRESORA_AIRE
        };
        ArrayAdapter<String> adapterPlan = new ArrayAdapter<>(requireContext(), R.layout.spinner_item_white, planes);
        adapterPlan.setDropDownViewResource(R.layout.spinner_dropdown_item_white);
        spinnerTipoEquipo.setAdapter(adapterPlan);
        if (esEdicion && itemExistente.getTipoEquipo() != null) {
            for (int i = 0; i < planes.length; i++) {
                if (planes[i].equalsIgnoreCase(itemExistente.getTipoEquipo())) {
                    spinnerTipoEquipo.setSelection(i);
                    break;
                }
            }
        } else {
            for (int i = 0; i < planes.length; i++) {
                if (planes[i].equalsIgnoreCase(tipoEquipoSeleccionado)) {
                    spinnerTipoEquipo.setSelection(i);
                    break;
                }
            }
        }

        // 2. Configurar Spinner Subsistemas
        String[] subsistemas = new String[]{
                PlanesMantenimientoDefaults.SUBSISTEMA_LUBRICACION,
                PlanesMantenimientoDefaults.SUBSISTEMA_COMBUSTIBLE,
                PlanesMantenimientoDefaults.SUBSISTEMA_ADMISION,
                PlanesMantenimientoDefaults.SUBSISTEMA_ELECTRICO,
                PlanesMantenimientoDefaults.SUBSISTEMA_COMPRESION,
                PlanesMantenimientoDefaults.SUBSISTEMA_ESTRUCTURAL
        };
        ArrayAdapter<String> adapterSubs = new ArrayAdapter<>(requireContext(), R.layout.spinner_item_white, subsistemas);
        adapterSubs.setDropDownViewResource(R.layout.spinner_dropdown_item_white);
        spinnerSubsistema.setAdapter(adapterSubs);
        if (esEdicion && itemExistente.getSubsistema() != null) {
            for (int i = 0; i < subsistemas.length; i++) {
                if (subsistemas[i].equalsIgnoreCase(itemExistente.getSubsistema())) {
                    spinnerSubsistema.setSelection(i);
                    break;
                }
            }
        }

        // 3. Configurar Spinner Tipo Frecuencia
        String[] tiposFrecuencia = new String[]{"Por horas de trabajo", "Por retorno de alquiler"};
        ArrayAdapter<String> adapterFrec = new ArrayAdapter<>(requireContext(), R.layout.spinner_item_white, tiposFrecuencia);
        adapterFrec.setDropDownViewResource(R.layout.spinner_dropdown_item_white);
        spinnerTipoFrecuencia.setAdapter(adapterFrec);

        spinnerTipoFrecuencia.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    layoutCamposHoras.setVisibility(View.VISIBLE);
                    layoutCamposRetorno.setVisibility(View.GONE);
                } else {
                    layoutCamposHoras.setVisibility(View.GONE);
                    layoutCamposRetorno.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        if (esEdicion && itemExistente.isEsInspeccionRetorno()) {
            spinnerTipoFrecuencia.setSelection(1);
            layoutCamposHoras.setVisibility(View.GONE);
            layoutCamposRetorno.setVisibility(View.VISIBLE);
        } else {
            spinnerTipoFrecuencia.setSelection(0);
            layoutCamposHoras.setVisibility(View.VISIBLE);
            layoutCamposRetorno.setVisibility(View.GONE);
        }

        // 4. Configurar Spinner Frecuencia de Retornos
        String[] opcionesRetorno = new String[]{
                "En cada retorno (Cada 1 retorno)",
                "Cada 2 retornos de alquiler",
                "Cada 3 retornos de alquiler",
                "Cada 4 retornos de alquiler",
                "Cada 5 retornos de alquiler"
        };
        ArrayAdapter<String> adapterOpcionesRetorno = new ArrayAdapter<>(requireContext(), R.layout.spinner_item_white, opcionesRetorno);
        adapterOpcionesRetorno.setDropDownViewResource(R.layout.spinner_dropdown_item_white);
        spinnerFrecuenciaRetornos.setAdapter(adapterOpcionesRetorno);
        if (esEdicion && itemExistente.getFrecuenciaRetornos() > 0) {
            int idx = Math.min(opcionesRetorno.length - 1, Math.max(0, itemExistente.getFrecuenciaRetornos() - 1));
            spinnerFrecuenciaRetornos.setSelection(idx);
        }

        // 5. Configurar Spinner Componente Clave
        String[] componentesNombres = new String[]{
                "Aceite de motor",
                "Filtro de combustible",
                "Filtro de aire",
                "Fajas y correas",
                "Calibración de válvulas",
                "Generador eléctrico",
                "Compresor de tornillo",
                "Filtro separador aire y aceite",
                "Carreta y pernos",
                "General"
        };
        final String[] componentesKeys = new String[]{
                "aceite",
                "filtro_petroleo",
                "filtro_aire",
                "faja",
                "valvulas",
                "generador",
                "compresor_aceite",
                "separador_aire_aceite",
                "carreta",
                "general"
        };
        ArrayAdapter<String> adapterComp = new ArrayAdapter<>(requireContext(), R.layout.spinner_item_white, componentesNombres);
        adapterComp.setDropDownViewResource(R.layout.spinner_dropdown_item_white);
        spinnerComponenteKey.setAdapter(adapterComp);
        if (esEdicion && itemExistente.getComponenteKey() != null) {
            for (int i = 0; i < componentesKeys.length; i++) {
                if (componentesKeys[i].equalsIgnoreCase(itemExistente.getComponenteKey())) {
                    spinnerComponenteKey.setSelection(i);
                    break;
                }
            }
        }

        btnCancelar.setOnClickListener(v -> dialog.dismiss());

        btnGuardar.setOnClickListener(v -> {
            String nombre = inputNombre.getText() != null ? inputNombre.getText().toString().trim() : "";
            if (nombre.isEmpty()) {
                mostrarError("Ingresa el nombre de la tarea");
                return;
            }

            String planElegido = spinnerTipoEquipo.getSelectedItem().toString();
            String subsistemaElegido = spinnerSubsistema.getSelectedItem().toString();
            boolean esRetorno = spinnerTipoFrecuencia.getSelectedItemPosition() == 1;

            int horas = 250;
            int meses = 6;
            int retornos = 1;

            if (esRetorno) {
                retornos = spinnerFrecuenciaRetornos.getSelectedItemPosition() + 1;
                horas = 0;
                meses = 0;
            } else {
                try {
                    String hStr = inputIntervaloHoras.getText() != null ? inputIntervaloHoras.getText().toString().trim() : "250";
                    horas = Integer.parseInt(hStr);
                } catch (NumberFormatException e) {
                    horas = 250;
                }
                try {
                    String mStr = inputMesesMaximos.getText() != null ? inputMesesMaximos.getText().toString().trim() : "6";
                    meses = Integer.parseInt(mStr);
                } catch (NumberFormatException e) {
                    meses = 6;
                }
            }

            int compIdx = spinnerComponenteKey.getSelectedItemPosition();
            String keyElegida = (compIdx >= 0 && compIdx < componentesKeys.length) ? componentesKeys[compIdx] : "general";

            btnGuardar.setEnabled(false);
            btnGuardar.setText("Guardando...");

            MantenimientoConfiguracion nuevoItem = esEdicion ? itemExistente : new MantenimientoConfiguracion();
            nuevoItem.setNombre(nombre);
            nuevoItem.setTipoEquipo(planElegido);
            nuevoItem.setSubsistema(subsistemaElegido);
            nuevoItem.setIntervaloHoras(horas);
            nuevoItem.setMesesMaximos(meses);
            nuevoItem.setEsInspeccionRetorno(esRetorno);
            nuevoItem.setFrecuenciaRetornos(retornos);
            nuevoItem.setComponenteKey(keyElegida);
            nuevoItem.setUnidadMedida("HORAS");

            if (esEdicion) {
                db.collection("mantenimientos_configuracion").document(nuevoItem.getId())
                        .set(nuevoItem)
                        .addOnSuccessListener(aVoid -> {
                            mostrarExito("Tarea actualizada");
                            if (adapter != null) adapter.expandirSubsistema(subsistemaElegido);
                            dialog.dismiss();
                            cargarMantenimientosConfiguracion();
                        })
                        .addOnFailureListener(e -> {
                            btnGuardar.setEnabled(true);
                            btnGuardar.setText("Guardar");
                            mostrarError("Error al actualizar: " + e.getMessage());
                        });
            } else {
                nuevoItem.setFechaCreacion(System.currentTimeMillis());
                db.collection("mantenimientos_configuracion")
                        .add(nuevoItem)
                        .addOnSuccessListener(docRef -> {
                            nuevoItem.setId(docRef.getId());
                            mostrarExito("Tarea registrada en el plan");
                            if (adapter != null) adapter.expandirSubsistema(subsistemaElegido);
                            dialog.dismiss();
                            cargarMantenimientosConfiguracion();
                        })
                        .addOnFailureListener(e -> {
                            btnGuardar.setEnabled(true);
                            btnGuardar.setText("Guardar");
                            mostrarError("Error al guardar: " + e.getMessage());
                        });
            }
        });

        return dialog;
    }

    // ===== ELIMINAR TAREA CONFIGURACIÓN =====

    private void confirmarEliminacion(MantenimientoConfiguracion item) {
        new MaterialAlertDialogBuilder(requireContext(), R.style.DialogoConFuenteAnta)
                .setTitle("Eliminar Tarea")
                .setMessage("¿Estás seguro de que deseas eliminar \"" + item.getNombre() + "\"?")
                .setPositiveButton("Eliminar", (dialog, which) -> eliminarTareaConfiguracion(item))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void eliminarTareaConfiguracion(MantenimientoConfiguracion item) {
        db.collection("mantenimientos_configuracion").document(item.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    mostrarExito("Tarea eliminada");
                    cargarMantenimientosConfiguracion();
                })
                .addOnFailureListener(e -> {
                    mostrarError("Error al eliminar: " + e.getMessage());
                });
    }

    private void mostrarError(String mensaje) {
        if (getContext() != null) {
            Toast.makeText(getContext(), mensaje, Toast.LENGTH_SHORT).show();
        }
    }

    private void mostrarExito(String mensaje) {
        if (getContext() != null) {
            Toast.makeText(getContext(), mensaje, Toast.LENGTH_SHORT).show();
        }
    }
}
