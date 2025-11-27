package com.example.maquirentapp.View;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.maquirentapp.MainActivity;
import com.example.maquirentapp.Model.Cotizacion;
import com.example.maquirentapp.Repository.CotizacionesRepository;
import com.example.maquirentapp.R;
import com.example.maquirentapp.Utils.WordGenerator;
import com.example.maquirentapp.Access.CotizacionesAdapter;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import it.xabaras.android.recyclerview.swipedecorator.RecyclerViewSwipeDecorator;

public class HistorialCotizacionesFragment extends Fragment {

    private RecyclerView recyclerView;
    private CotizacionesAdapter adapter;
    private CotizacionesRepository repository;
    private List<Cotizacion> listaCompleta = new ArrayList<>();
    private TextView tvAnio;
    private int anioSeleccionado;
    private LinearLayout emptyState;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        repository = new CotizacionesRepository();
        anioSeleccionado = Calendar.getInstance().get(Calendar.YEAR);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_historial_cotizaciones, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupRecyclerView();
        cargarCotizaciones();
        configureFab();
    }

    private void initViews(View view) {
        tvAnio = view.findViewById(R.id.tvAnio);
        ImageButton btnAnterior = view.findViewById(R.id.btnAnterior);
        ImageButton btnSiguiente = view.findViewById(R.id.btnSiguiente);
        emptyState = view.findViewById(R.id.emptyStateCotizaciones);
        recyclerView = view.findViewById(R.id.recyclerCotizaciones);

        actualizarTextoAnio();

        btnAnterior.setOnClickListener(v -> {
            anioSeleccionado--;
            actualizarTextoAnio();
            filtrarPorAnio();
        });

        btnSiguiente.setOnClickListener(v -> {
            anioSeleccionado++;
            actualizarTextoAnio();
            filtrarPorAnio();
        });

        tvAnio.setOnClickListener(v -> mostrarSelectorAnio());
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new CotizacionesAdapter(this::regenerarYAbrirWord);
        recyclerView.setAdapter(adapter);
        setupSwipeToDelete();
    }

    private void actualizarTextoAnio() {
        tvAnio.setText(String.valueOf(anioSeleccionado));
    }

    private void mostrarSelectorAnio() {
        NumberPicker picker = new NumberPicker(getContext());
        picker.setMinValue(2020);
        picker.setMaxValue(2030);
        picker.setValue(anioSeleccionado);

        new MaterialAlertDialogBuilder(getContext())
                .setTitle("Seleccionar Año")
                .setView(picker)
                .setPositiveButton("OK", (d, w) -> {
                    anioSeleccionado = picker.getValue();
                    actualizarTextoAnio();
                    filtrarPorAnio();
                }).show();
    }

    private void cargarCotizaciones() {
        repository.getCotizaciones(new CotizacionesRepository.Callback<List<Cotizacion>>() {
            @Override
            public void onSuccess(List<Cotizacion> result) {
                listaCompleta = result;
                filtrarPorAnio();
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(getContext(), "Error cargando historial", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filtrarPorAnio() {
        List<Cotizacion> filtradas = new ArrayList<>();
        for (Cotizacion c : listaCompleta) {
            int anio = extraerAnio(c.getFechaEmision());
            if (anio == anioSeleccionado) {
                filtradas.add(c);
            }
        }

        adapter.submitList(filtradas);

        if (filtradas.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            if (emptyState != null) emptyState.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            if (emptyState != null) emptyState.setVisibility(View.GONE);
        }
    }

    private int extraerAnio(String fechaTexto) {
        try {
            Pattern p = Pattern.compile("(\\d{4})");
            Matcher m = p.matcher(fechaTexto);
            if (m.find()) {
                return Integer.parseInt(m.group(1));
            }
        } catch (Exception e) {
            return -1;
        }
        return -1;
    }

    private void regenerarYAbrirWord(Cotizacion cotizacion) {
        ProgressDialog progress = new ProgressDialog(getContext());
        progress.setMessage("Abriendo cotización...");
        progress.setCancelable(false);
        progress.show();

        new Thread(() -> {
            try {
                WordGenerator generator = new WordGenerator();
                File archivo = generator.generarCotizacionWord(requireContext(), cotizacion);

                requireActivity().runOnUiThread(() -> {
                    progress.dismiss();
                    abrirDocumento(archivo);
                });
            } catch (Exception e) {
                requireActivity().runOnUiThread(() -> {
                    progress.dismiss();
                    Toast.makeText(getContext(), "Error al abrir: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void abrirDocumento(File archivo) {
        try {
            Uri uri = androidx.core.content.FileProvider.getUriForFile(
                    requireContext(),
                    requireContext().getPackageName() + ".provider",
                    archivo
            );
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(Intent.createChooser(intent, "Abrir con..."));
        } catch (Exception e) {
            Toast.makeText(getContext(), "No tienes app para Word", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupSwipeToDelete() {
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(@NonNull RecyclerView r, @NonNull RecyclerView.ViewHolder v, @NonNull RecyclerView.ViewHolder t) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int pos = viewHolder.getAdapterPosition();
                Cotizacion item = adapter.getItem(pos);

                new MaterialAlertDialogBuilder(getContext())
                        .setTitle("Eliminar Cotización")
                        .setMessage("¿Eliminar " + item.getNumeroCotizacion() + "?")
                        .setPositiveButton("Eliminar", (d, w) -> {
                            repository.eliminarCotizacion(item.getId(), new CotizacionesRepository.Callback<Void>() {
                                @Override
                                public void onSuccess(Void result) {
                                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                                        listaCompleta.removeIf(c -> c.getId().equals(item.getId()));
                                    } else {
                                        for (int i = 0; i < listaCompleta.size(); i++) {
                                            if (listaCompleta.get(i).getId().equals(item.getId())) {
                                                listaCompleta.remove(i);
                                                break;
                                            }
                                        }
                                    }
                                    filtrarPorAnio();
                                    Toast.makeText(getContext(), "Eliminado", Toast.LENGTH_SHORT).show();
                                }

                                @Override
                                public void onError(Exception e) {
                                    Toast.makeText(getContext(), "Error al eliminar", Toast.LENGTH_SHORT).show();
                                    adapter.notifyItemChanged(pos);
                                }
                            });
                        })
                        .setNegativeButton("Cancelar", (d, w) -> adapter.notifyItemChanged(pos))
                        .show();
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView r, @NonNull RecyclerView.ViewHolder v, float dX, float dY, int a, boolean i) {
                new RecyclerViewSwipeDecorator.Builder(c, r, v, dX, dY, a, i)
                        .addBackgroundColor(ContextCompat.getColor(requireContext(), R.color.red_accent))
                        .addActionIcon(R.drawable.icon_eliminar_rojo)
                        .setActionIconTint(R.color.white)
                        .addCornerRadius(TypedValue.COMPLEX_UNIT_DIP, 30)
                        .create()
                        .decorate();
                super.onChildDraw(c, r, v, dX, dY, a, i);
            }
        }).attachToRecyclerView(recyclerView);
    }

    private void configureFab() {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).showGlobalFab(
                    "Nueva",
                    R.drawable.icon_nuevo_blanco,
                    v -> Navigation.findNavController(getView()).navigate(R.id.action_historialCotizaciones_to_nuevaCotizacion)
            );
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        configureFab();
        cargarCotizaciones();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).hideGlobalFab();
        }
    }
}