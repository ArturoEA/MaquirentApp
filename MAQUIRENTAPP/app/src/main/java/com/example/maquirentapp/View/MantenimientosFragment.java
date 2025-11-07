package com.example.maquirentapp.View;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.maquirentapp.Model.Mantenimiento;
import com.example.maquirentapp.Model.MantenimientoConfiguracion;
import com.example.maquirentapp.R;
import com.example.maquirentapp.Access.MantenimientosAdapter;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;

public class MantenimientosFragment extends Fragment {
    private static final String TAG = "MantenimientosFragment";

    private String codigo;
    private RecyclerView recyclerView;
    private LinearLayout emptyState;
    private MantenimientosAdapter adapter;
    private FirebaseFirestore db;
    private FirebaseStorage storage;

    private List<Mantenimiento> mantenimientosList = new ArrayList<>();
    private List<MantenimientoConfiguracion> itemsConfigList = new ArrayList<>();

    public MantenimientosFragment() {
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
        return inflater.inflate(R.layout.fragment_mantenimientos, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeFirebase();
        initViews(view);
        setupRecyclerView();
        cargarItemsConfiguracion();
    }

    private void initializeFirebase() {
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerViewMantenimientos);
        emptyState = view.findViewById(R.id.emptyStateMantenimientos);
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new MantenimientosAdapter(getContext(), mantenimientosList, itemsConfigList,
                mantenimiento -> abrirDetalleMantenimiento(mantenimiento));

        recyclerView.setAdapter(adapter);

        // Configurar Swipe to Delete
        setupSwipeToDelete();
    }

    private void setupSwipeToDelete() {
        ItemTouchHelper.SimpleCallback swipeCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            private final ColorDrawable background = new ColorDrawable(Color.RED);
            private Drawable deleteIcon;

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                Mantenimiento mantenimiento = adapter.getMantenimientoAt(position);

                // Remover del adapter
                adapter.removerMantenimiento(position);

                // Mostrar Snackbar con opción de deshacer
                Snackbar.make(recyclerView, "Mantenimiento eliminado", Snackbar.LENGTH_LONG)
                        .setAction("DESHACER", v -> adapter.restaurarMantenimiento(mantenimiento, position))
                        .addCallback(new Snackbar.Callback() {
                            @Override
                            public void onDismissed(Snackbar transientBottomBar, int event) {
                                if (event != DISMISS_EVENT_ACTION) {
                                    // Solo eliminar de Firestore si no se presionó deshacer
                                    eliminarMantenimientoFirestore(mantenimiento);
                                }
                            }
                        })
                        .show();

                actualizarUI();
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
                                    @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY,
                                    int actionState, boolean isCurrentlyActive) {
                View itemView = viewHolder.itemView;

                if (deleteIcon == null) {
                    deleteIcon = ContextCompat.getDrawable(requireContext(), android.R.drawable.ic_menu_delete);
                    if (deleteIcon != null) {
                        deleteIcon.setTint(Color.WHITE);
                    }
                }

                int iconMargin = (itemView.getHeight() - deleteIcon.getIntrinsicHeight()) / 2;
                int iconTop = itemView.getTop() + iconMargin;
                int iconBottom = iconTop + deleteIcon.getIntrinsicHeight();

                if (dX < 0) { // Swipe izquierda
                    int iconLeft = itemView.getRight() - iconMargin - deleteIcon.getIntrinsicWidth();
                    int iconRight = itemView.getRight() - iconMargin;
                    deleteIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom);

                    background.setBounds(
                            itemView.getRight() + (int) dX,
                            itemView.getTop(),
                            itemView.getRight(),
                            itemView.getBottom()
                    );
                } else {
                    background.setBounds(0, 0, 0, 0);
                }

                background.draw(c);
                deleteIcon.draw(c);

                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(swipeCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);
    }

    private void cargarItemsConfiguracion() {
        db.collection("mantenimientos_configuracion")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    itemsConfigList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        MantenimientoConfiguracion item = document.toObject(MantenimientoConfiguracion.class);
                        item.setId(document.getId());
                        itemsConfigList.add(item);
                    }
                    adapter.actualizarItemsConfig(itemsConfigList);
                    cargarMantenimientos();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error cargando items configuración", e);
                    cargarMantenimientos(); // Cargar mantenimientos de todas formas
                });
    }

    private void cargarMantenimientos() {
        db.collection("mantenimientos")
                .whereEqualTo("codigoGrupo", codigo)
                .orderBy("fechaCreacion", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    mantenimientosList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Mantenimiento mantenimiento = document.toObject(Mantenimiento.class);
                        mantenimiento.setId(document.getId());
                        mantenimientosList.add(mantenimiento);
                    }
                    actualizarUI();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error cargando mantenimientos", e);
                    Toast.makeText(getContext(), "Error al cargar mantenimientos", Toast.LENGTH_SHORT).show();
                });
    }

    private void actualizarUI() {
        if (mantenimientosList.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyState.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            adapter.actualizarLista(mantenimientosList);
        }
    }

    private void abrirDetalleMantenimiento(Mantenimiento mantenimiento) {
        // Navegar al fragmento de detalle/edición
        NuevoMantenimientoFragment fragment = NuevoMantenimientoFragment.newInstance(codigo, mantenimiento.getId());
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.nav_host_fragment, fragment)
                .addToBackStack(null)
                .commit();
    }

    private void eliminarMantenimientoFirestore(Mantenimiento mantenimiento) {
        // Eliminar fotos de Storage
        if (mantenimiento.getFotos() != null && !mantenimiento.getFotos().isEmpty()) {
            for (String fotoUrl : mantenimiento.getFotos()) {
                try {
                    StorageReference fotoRef = storage.getReferenceFromUrl(fotoUrl);
                    fotoRef.delete()
                            .addOnSuccessListener(aVoid -> Log.d(TAG, "Foto eliminada"))
                            .addOnFailureListener(e -> Log.e(TAG, "Error eliminando foto", e));
                } catch (Exception e) {
                    Log.e(TAG, "Error procesando URL foto", e);
                }
            }
        }

        // Eliminar documento de Firestore
        db.collection("mantenimientos").document(mantenimiento.getId())
                .delete()
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Mantenimiento eliminado de Firestore"))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error eliminando de Firestore", e);
                    Toast.makeText(getContext(), "Error al eliminar", Toast.LENGTH_SHORT).show();
                    // Recargar para mostrar el estado real
                    cargarMantenimientos();
                });
    }

    @Override
    public void onResume() {
        super.onResume();
        configureGlobalFab();
        cargarMantenimientos(); // Recargar al volver
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
        if (getActivity() instanceof com.example.maquirentapp.MainActivity) {
            com.example.maquirentapp.MainActivity main = (com.example.maquirentapp.MainActivity) getActivity();
            main.showGlobalFab("Añadir", R.drawable.icon_nuevo_blanco, v -> abrirNuevoMantenimiento());
        }
    }

    private void hideGlobalFab() {
        if (getActivity() instanceof com.example.maquirentapp.MainActivity) {
            ((com.example.maquirentapp.MainActivity) getActivity()).hideGlobalFab();
        }
    }

    private void abrirNuevoMantenimiento() {
        NuevoMantenimientoFragment fragment = NuevoMantenimientoFragment.newInstance(codigo, null);
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.nav_host_fragment, fragment)
                .addToBackStack(null)
                .commit();
    }
}