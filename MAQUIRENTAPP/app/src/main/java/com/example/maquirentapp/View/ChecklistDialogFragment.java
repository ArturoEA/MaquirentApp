package com.example.maquirentapp.View;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.DialogFragment;

import com.example.maquirentapp.Model.GrupoElectrogeno;
import com.example.maquirentapp.Model.MantenimientoConfiguracion;
import com.example.maquirentapp.R;
import com.example.maquirentapp.Repository.Callback;
import com.example.maquirentapp.Repository.GeneradorRepository;
import com.example.maquirentapp.Utils.MantenimientoSaludManager;
import com.example.maquirentapp.Utils.MantenimientoSaludManager.EstadoSalud;
import com.example.maquirentapp.Utils.MantenimientoSaludManager.EstadoSaludTarea;
import com.example.maquirentapp.Utils.MantenimientoSaludManager.ReporteSaludEquipo;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Diálogo de checklist para una máquina seleccionada, alineado con el diseño y tipografía (Anta) de la app.
 * Muestra únicamente las actividades vencidas y próximas a vencer,
 * y permite pasarlas preseleccionadas a NuevoMantenimientoFragment (flujo B2).
 */
public class ChecklistDialogFragment extends DialogFragment {

    public interface OnRegistrarMantenimientoListener {
        void onRegistrarMantenimiento(String codigoGrupo, ArrayList<String> itemsSeleccionados);
    }

    private String grupoId;
    private String codigoGrupo;
    private LinearLayout llChecklistContainer;
    private MaterialToolbar toolbar;
    private TextView tvTipoEquipo, tvSubtitulo, tvHorometro;
    private MaterialButton btnRegistrar;
    private final GeneradorRepository repository = new GeneradorRepository();
    private final List<CheckBox> checkBoxes = new ArrayList<>();
    private OnRegistrarMantenimientoListener listener;

    public void setOnRegistrarMantenimientoListener(OnRegistrarMantenimientoListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_checklist_fullscreen, container, false);
        llChecklistContainer = view.findViewById(R.id.llChecklistContainer);
        toolbar = view.findViewById(R.id.toolbarChecklist);
        tvTipoEquipo = view.findViewById(R.id.tvChecklistTipoEquipo);
        tvSubtitulo = view.findViewById(R.id.tvChecklistSubtitulo);
        tvHorometro = view.findViewById(R.id.tvChecklistHorometro);
        btnRegistrar = view.findViewById(R.id.btnGuardarChecklist);

        toolbar.setNavigationOnClickListener(v -> dismiss());
        aplicarFuenteAntaEnToolbar();

        if (getArguments() != null) {
            grupoId = getArguments().getString("grupoId");
            codigoGrupo = getArguments().getString("codigoGrupo");
        }

        if (codigoGrupo != null) {
            toolbar.setTitle("Pendientes · " + codigoGrupo);
        }

        btnRegistrar.setOnClickListener(v -> {
            ArrayList<String> seleccionados = new ArrayList<>();
            for (CheckBox cb : checkBoxes) {
                if (cb.isChecked() && cb.getTag() instanceof String) {
                    seleccionados.add((String) cb.getTag());
                }
            }
            if (!checkBoxes.isEmpty() && seleccionados.isEmpty()) {
                Toast.makeText(getContext(), "Selecciona al menos una actividad para registrar", Toast.LENGTH_SHORT).show();
                return;
            }
            dismiss();
            if (listener != null && codigoGrupo != null) {
                listener.onRegistrarMantenimiento(codigoGrupo, seleccionados);
            }
        });

        if (grupoId != null) {
            cargarPendientes();
        }

        return view;
    }

    private void aplicarFuenteAntaEnToolbar() {
        Typeface anta = ResourcesCompat.getFont(requireContext(), R.font.anta_font);
        if (anta == null) return;
        for (int i = 0; i < toolbar.getChildCount(); i++) {
            View child = toolbar.getChildAt(i);
            if (child instanceof TextView) {
                ((TextView) child).setTypeface(anta, Typeface.BOLD);
            }
        }
    }

    private void cargarPendientes() {
        repository.getGrupoPorId(grupoId, new Callback<GrupoElectrogeno>() {
            @Override
            public void onSuccess(GrupoElectrogeno grupo) {
                if (!isAdded() || grupo == null) return;
                if (codigoGrupo == null || codigoGrupo.isEmpty()) {
                    codigoGrupo = grupo.getCodigo();
                    toolbar.setTitle("Pendientes · " + codigoGrupo);
                }

                MantenimientoSaludManager.cargarPlantillasConfiguracion(requireContext(), new Callback<List<MantenimientoConfiguracion>>() {
                    @Override
                    public void onSuccess(List<MantenimientoConfiguracion> plantillas) {
                        if (!isAdded()) return;
                        ReporteSaludEquipo reporte = MantenimientoSaludManager.evaluarSaludGrupo(grupo, plantillas);
                        mostrarActividadesPendientes(grupo, reporte);
                    }

                    @Override
                    public void onError(Exception e) {
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "Error al cargar configuración", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Error al cargar máquina", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void mostrarActividadesPendientes(GrupoElectrogeno grupo, ReporteSaludEquipo reporte) {
        llChecklistContainer.removeAllViews();
        checkBoxes.clear();

        String tipoEquipo = (reporte != null && reporte.tipoEquipo != null) ? reporte.tipoEquipo : "Grupo electrógeno";
        tvTipoEquipo.setText(tipoEquipo);
        tvHorometro.setText(MantenimientoSaludManager.formatearHorometroSinDecimal(grupo.getHorometroActual()));

        List<EstadoSaludTarea> pendientes = new ArrayList<>();
        if (reporte != null && reporte.todasLasTareas != null) {
            for (EstadoSaludTarea t : reporte.todasLasTareas) {
                if (t.isVencido() || t.estado == EstadoSalud.PROXIMO) {
                    pendientes.add(t);
                }
            }
        }

        Collections.sort(pendientes, (a, b) -> Boolean.compare(!a.isVencido(), !b.isVencido()));

        if (pendientes.isEmpty()) {
            tvSubtitulo.setText("Sin actividades pendientes");
            TextView tvVacio = new TextView(requireContext());
            Typeface anta = ResourcesCompat.getFont(requireContext(), R.font.anta_font);
            if (anta != null) tvVacio.setTypeface(anta);
            tvVacio.setText("Esta máquina está al día. No tiene actividades vencidas ni próximas a vencer.\n\nPuedes registrar un mantenimiento adicional si lo necesitas.");
            tvVacio.setTextColor(Color.parseColor("#CFD8DC"));
            tvVacio.setTextSize(14);
            tvVacio.setPadding(8, 16, 8, 16);
            llChecklistContainer.addView(tvVacio);
            btnRegistrar.setText("Registrar mantenimiento");
            return;
        }

        tvSubtitulo.setText(pendientes.size() + (pendientes.size() == 1 ? " actividad por atender" : " actividades por atender"));

        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (EstadoSaludTarea t : pendientes) {
            View itemView = inflater.inflate(R.layout.item_checklist_pendiente, llChecklistContainer, false);
            CheckBox cb = itemView.findViewById(R.id.cbActividadPendiente);
            TextView tvNombre = itemView.findViewById(R.id.tvNombreActividad);
            TextView tvSubsistema = itemView.findViewById(R.id.tvSubsistemaActividad);
            TextView tvBadge = itemView.findViewById(R.id.tvBadgeEstadoActividad);

            tvNombre.setText(t.nombre);
            tvSubsistema.setText(t.subsistema);

            String badgeTxt;
            int badgeColor;
            int tintCheck;
            if (t.estado == EstadoSalud.VENCIDO_HORAS) {
                badgeTxt = String.format(Locale.US, "VENC. +%.0fh", Math.max(0, t.horasExceso));
                badgeColor = Color.parseColor("#D32F2F");
                tintCheck = Color.parseColor("#EF9A9A");
            } else if (t.estado == EstadoSalud.VENCIDO_TIEMPO) {
                badgeTxt = "VENC. " + t.mesesTranscurridos + "m";
                badgeColor = Color.parseColor("#D32F2F");
                tintCheck = Color.parseColor("#EF9A9A");
            } else if (t.isVencido()) {
                badgeTxt = "RETORNO";
                badgeColor = Color.parseColor("#D32F2F");
                tintCheck = Color.parseColor("#EF9A9A");
            } else {
                badgeTxt = t.horasRestantes > 0
                        ? String.format(Locale.US, "EN %.0fh", t.horasRestantes)
                        : "PRÓXIMO";
                badgeColor = Color.parseColor("#F57C00");
                tintCheck = Color.parseColor("#FFE082");
            }

            tvBadge.setText(badgeTxt);
            tvBadge.setBackgroundTintList(ColorStateList.valueOf(badgeColor));
            cb.setButtonTintList(ColorStateList.valueOf(tintCheck));
            cb.setChecked(t.isVencido());
            cb.setTag(t.idTarea);

            itemView.setOnClickListener(v -> cb.setChecked(!cb.isChecked()));

            checkBoxes.add(cb);
            llChecklistContainer.addView(itemView);
        }

        btnRegistrar.setText("Registrar mantenimiento");
    }
}
