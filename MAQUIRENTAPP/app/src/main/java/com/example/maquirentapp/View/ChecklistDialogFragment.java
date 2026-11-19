package com.example.maquirentapp.View;

import android.app.Dialog;
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
import androidx.fragment.app.DialogFragment;

import com.example.maquirentapp.R;
import com.example.maquirentapp.Model.GrupoElectrogeno;
import com.example.maquirentapp.Repository.Callback;
import com.example.maquirentapp.Repository.GeneradorRepository;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.Map;

/**
 * Full-screen dialog that shows the complete checklist for a selected machine (GrupoElectrogeno).
 * Tasks are derived from relojesTareas map keys.
 */
public class ChecklistDialogFragment extends DialogFragment {

    private String grupoId;
    private LinearLayout llChecklistContainer;
    private final GeneradorRepository repository = new GeneradorRepository();

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

        MaterialToolbar toolbar = view.findViewById(R.id.toolbarChecklist);
        toolbar.setNavigationOnClickListener(v -> dismiss());

        if (getArguments() != null) {
            grupoId = getArguments().getString("grupoId");
        }

        if (grupoId != null) {
            cargarChecklist();
        }

        view.findViewById(R.id.btnGuardarChecklist).setOnClickListener(v -> {
            // TODO: collect checkbox states and persist via repository
            Toast.makeText(getContext(), "Checklist guardada", Toast.LENGTH_SHORT).show();
            dismiss();
        });

        return view;
    }

    /**
     * Loads the group and creates a CheckBox for each task key in relojesTareas.
     * Tasks with reloj > 0 are shown as pending (unchecked), reloj == 0 as completed (checked).
     */
    private void cargarChecklist() {
        repository.getGrupoPorId(grupoId, new Callback<GrupoElectrogeno>() {
            @Override
            public void onSuccess(GrupoElectrogeno grupo) {
                if (!isAdded()) return;

                Map<String, Double> relojes = grupo.getRelojesTareas();
                if (relojes == null || relojes.isEmpty()) {
                    TextView tvVacio = new TextView(requireContext());
                    tvVacio.setText("No hay tareas configuradas");
                    tvVacio.setTextColor(0xFF90CAF9);
                    tvVacio.setTextSize(14);
                    llChecklistContainer.addView(tvVacio);
                    return;
                }

                for (Map.Entry<String, Double> entry : relojes.entrySet()) {
                    String clave = entry.getKey();
                    Double reloj = entry.getValue();

                    CheckBox cb = new CheckBox(requireContext());
                    // Format the key to be more readable (replace underscores with spaces)
                    String label = clave.replace("_", " ");
                    if (reloj != null && reloj > 0) {
                        label += "  (+" + String.format("%.0f", reloj) + " h)";
                    }
                    cb.setText(label);
                    cb.setTextColor(0xFFFFFFFF);
                    cb.setButtonTintList(android.content.res.ColorStateList.valueOf(0xFF90CAF9));
                    // If reloj is 0 or null, task is "al día" (checked); otherwise pending (unchecked)
                    cb.setChecked(reloj == null || reloj == 0);
                    cb.setTag(clave); // store key for later persistence
                    llChecklistContainer.addView(cb);
                }
            }

            @Override
            public void onError(Exception e) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Error al cargar checklist", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
