package com.example.maquirentapp.View;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.maquirentapp.R;
import com.example.maquirentapp.adapters.MaquinaTareaAdapter;
import com.example.maquirentapp.Repository.GeneradorRepository;
import com.example.maquirentapp.Repository.Callback;
import com.example.maquirentapp.Model.GrupoElectrogeno;
import com.example.maquirentapp.View.ChecklistDialogFragment;
import com.facebook.shimmer.ShimmerFrameLayout;

import java.util.List;

public class TareasFragment extends Fragment {

    private RecyclerView recyclerTareas;
    private ShimmerFrameLayout shimmerTareas;
    private MaquinaTareaAdapter adapter;
    private final GeneradorRepository generadorRepository = new GeneradorRepository();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_tareas, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerTareas = view.findViewById(R.id.recyclerTareas);
        shimmerTareas = view.findViewById(R.id.shimmerTareas);
        recyclerTareas.setLayoutManager(new GridLayoutManager(getContext(), 2));

        adapter = new MaquinaTareaAdapter(requireContext(), grupo -> {
            ChecklistDialogFragment dialog = new ChecklistDialogFragment();
            Bundle args = new Bundle();
            args.putString("grupoId", grupo.getId());
            dialog.setArguments(args);
            dialog.show(requireActivity().getSupportFragmentManager(), "checklist_dialog");
        });
        recyclerTareas.setAdapter(adapter);

        cargarGrupos();
    }

    private void cargarGrupos() {
        if (shimmerTareas != null) {
            shimmerTareas.setVisibility(View.VISIBLE);
            shimmerTareas.startShimmer();
        }
        generadorRepository.getGruposElectrogenos(false, new Callback<List<GrupoElectrogeno>>() {
            @Override
            public void onSuccess(List<GrupoElectrogeno> grupos) {
                if (shimmerTareas != null) {
                    shimmerTareas.stopShimmer();
                    shimmerTareas.setVisibility(View.GONE);
                }
                adapter.setMachineList(grupos);
            }

            @Override
            public void onError(Exception e) {
                if (shimmerTareas != null) {
                    shimmerTareas.stopShimmer();
                    shimmerTareas.setVisibility(View.GONE);
                }
                if (getContext() != null) {
                    android.widget.Toast.makeText(getContext(), "Error al cargar máquinas", android.widget.Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
