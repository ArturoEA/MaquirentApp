package com.example.maquirentapp.View;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.maquirentapp.MainActivity;
import com.example.maquirentapp.Model.GrupoElectrogeno;
import com.example.maquirentapp.R;
import com.example.maquirentapp.Repository.Callback;
import com.example.maquirentapp.Repository.GeneradorRepository;
import com.example.maquirentapp.adapters.GrupoElectrogenoAdapter;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.ArrayList;
import java.util.List;

public class CGEFragment extends Fragment {
    private RecyclerView recyclerView;
    private GrupoElectrogenoAdapter adapter;
    private List<GrupoElectrogeno> grupoList;
    private GeneradorRepository generadorRepository;
    private NavController navController;
    private com.facebook.shimmer.ShimmerFrameLayout shimmerGrupos;
    private SwitchMaterial switchEliminados;

    private int numColumnas = 1;
    private SharedPreferences prefs;
    private RecyclerView.ItemDecoration itemDecoration;

    public CGEFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_cge, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        navController = Navigation.findNavController(view);
        generadorRepository = new GeneradorRepository();

        shimmerGrupos = view.findViewById(R.id.shimmerGrupos);
        switchEliminados = view.findViewById(R.id.switchEliminados);
        recyclerView = view.findViewById(R.id.recycler_grupos_electrogenos);

        prefs = requireContext().getSharedPreferences("cge_prefs", Context.MODE_PRIVATE);
        numColumnas = prefs.getInt("num_columnas", 1);

        setupRecyclerView(view);

        switchEliminados.setOnCheckedChangeListener((buttonView, isChecked) -> {
            cargarGrupos();
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        configurarBotonCabecera();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).hideHeaderActionButton();
        }
    }

    private void configurarBotonCabecera() {
        if (getActivity() instanceof MainActivity) {
            MainActivity main = (MainActivity) getActivity();
            int iconRes = (numColumnas == 1) ? R.drawable.ic_grid_view : R.drawable.ic_list_view;
            main.setHeaderActionButton(iconRes, v -> toggleColumnas());
        }
    }

    private void setupRecyclerView(View view) {
        grupoList = new ArrayList<>();
        adapter = new GrupoElectrogenoAdapter(grupo -> {
            Bundle args = new Bundle();
            args.putString("codigo", grupo.getCodigo());
            args.putString("idGrupo", grupo.getId());
            navController.navigate(R.id.action_cge_to_grupoElectrogeno, args);
        });
        recyclerView.setAdapter(adapter);

        aplicarLayoutColumnas(numColumnas);
        cargarGrupos();
    }

    private void aplicarLayoutColumnas(int columnas) {
        this.numColumnas = columnas;
        if (prefs != null) {
            prefs.edit().putInt("num_columnas", numColumnas).apply();
        }

        if (getActivity() instanceof MainActivity) {
            int iconRes = (numColumnas == 1) ? R.drawable.ic_grid_view : R.drawable.ic_list_view;
            ((MainActivity) getActivity()).updateHeaderActionIcon(iconRes);
        }

        if (recyclerView != null) {
            GridLayoutManager layoutManager = new GridLayoutManager(getContext(), numColumnas);
            recyclerView.setLayoutManager(layoutManager);

            if (itemDecoration != null) {
                recyclerView.removeItemDecoration(itemDecoration);
            }

            int spacingPx = (int) (8 * getResources().getDisplayMetrics().density);
            itemDecoration = new RecyclerView.ItemDecoration() {
                @Override
                public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
                    if (numColumnas == 2) {
                        int pos = parent.getChildAdapterPosition(view);
                        int col = pos % 2;
                        if (col == 0) {
                            outRect.left = 0;
                            outRect.right = spacingPx / 2;
                        } else {
                            outRect.left = spacingPx / 2;
                            outRect.right = 0;
                        }
                        outRect.bottom = spacingPx;
                    } else {
                        outRect.left = 0;
                        outRect.right = 0;
                        outRect.bottom = spacingPx;
                    }
                }
            };
            recyclerView.addItemDecoration(itemDecoration);
        }
    }

    private void toggleColumnas() {
        int nuevo = (numColumnas == 1) ? 2 : 1;
        aplicarLayoutColumnas(nuevo);
        if (getContext() != null) {
            Toast.makeText(getContext(), nuevo == 1 ? "Vista de 1 columna" : "Vista de 2 columnas", Toast.LENGTH_SHORT).show();
        }
    }

    private void cargarGrupos() {
        if (shimmerGrupos != null) {
            shimmerGrupos.setVisibility(View.VISIBLE);
            shimmerGrupos.startShimmer();
        }
        if (recyclerView != null) {
            recyclerView.setVisibility(View.GONE);
        }
        boolean incluirEliminados = switchEliminados != null && switchEliminados.isChecked();

        generadorRepository.getGruposElectrogenos(incluirEliminados, new Callback<List<GrupoElectrogeno>>() {
            @Override
            public void onSuccess(List<GrupoElectrogeno> grupos) {
                grupoList.clear();
                grupoList.addAll(grupos);
                adapter.setItems(grupoList);

                if (shimmerGrupos != null) {
                    shimmerGrupos.stopShimmer();
                    shimmerGrupos.setVisibility(View.GONE);
                }
                if (recyclerView != null) {
                    recyclerView.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onError(Exception e) {
                Log.e("CGEFragment", "Error al cargar grupos", e);
                Toast.makeText(getContext(), "Error al cargar grupos", Toast.LENGTH_SHORT).show();
                if (shimmerGrupos != null) {
                    shimmerGrupos.stopShimmer();
                    shimmerGrupos.setVisibility(View.GONE);
                }
                if (recyclerView != null) {
                    recyclerView.setVisibility(View.VISIBLE);
                }
            }
        });
    }
}

