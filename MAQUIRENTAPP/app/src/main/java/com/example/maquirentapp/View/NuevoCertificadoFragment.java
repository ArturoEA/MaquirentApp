package com.example.maquirentapp.View;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.example.maquirentapp.Model.CertificadoOperatividad;
import com.example.maquirentapp.Model.GrupoElectrogeno;
import com.example.maquirentapp.Model.InfoPlaca;
import com.example.maquirentapp.Repository.CertificadosRepository;
import com.example.maquirentapp.Utils.WordGenerator;
import com.example.maquirentapp.databinding.FragmentNuevoCertificadoBinding;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class NuevoCertificadoFragment extends Fragment {

    private FragmentNuevoCertificadoBinding binding;
    private CertificadosRepository repository;
    private String idGrupoSeleccionado;
    private String codigoGrupoSeleccionado;
    private InfoPlaca datosTecnicosCargados;
    private List<GrupoElectrogeno> listaGrupos = new ArrayList<>();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        repository = new CertificadosRepository();

        if (getArguments() != null) {
            idGrupoSeleccionado = getArguments().getString("idGrupo");
            codigoGrupoSeleccionado = getArguments().getString("codigo");
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentNuevoCertificadoBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupListeners();
        actualizarFechaInput(Calendar.getInstance());

        cargarListaGrupos();
    }

    private void cargarListaGrupos() {
        binding.progressBar.setVisibility(View.VISIBLE);

        repository.getGruposActivos(new CertificadosRepository.Callback<List<GrupoElectrogeno>>() {
            @Override
            public void onSuccess(List<GrupoElectrogeno> result) {
                binding.progressBar.setVisibility(View.GONE);
                listaGrupos = result;
                setupDropdown();
            }

            @Override
            public void onError(Exception e) {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Error al cargar grupos", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupDropdown() {
        List<String> codigos = new ArrayList<>();
        for (GrupoElectrogeno g : listaGrupos) {
            codigos.add(g.getCodigo());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, codigos);
        binding.autoCompleteGrupo.setAdapter(adapter);

        binding.autoCompleteGrupo.setOnItemClickListener((parent, view, position, id) -> {
            String codigoSeleccionado = (String) parent.getItemAtPosition(position);

            for (GrupoElectrogeno g : listaGrupos) {
                if (g.getCodigo().equals(codigoSeleccionado)) {
                    idGrupoSeleccionado = g.getId();
                    codigoGrupoSeleccionado = g.getCodigo();
                    cargarDatosTecnicos(idGrupoSeleccionado);
                    break;
                }
            }
        });

        if (codigoGrupoSeleccionado != null) {
            binding.autoCompleteGrupo.setText(codigoGrupoSeleccionado, false);
            cargarDatosTecnicos(idGrupoSeleccionado);
        }
    }

    private void cargarDatosTecnicos(String idGrupo) {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.btnGenerarCertificado.setEnabled(false);
        binding.tvResumenTecnico.setText("Cargando datos técnicos...");

        repository.getDatosTecnicos(idGrupo, new CertificadosRepository.Callback<InfoPlaca>() {
            @Override
            public void onSuccess(InfoPlaca result) {
                binding.progressBar.setVisibility(View.GONE);
                datosTecnicosCargados = result;

                if (result == null) {
                    binding.tvResumenTecnico.setText("Este grupo no tiene Información General registrada.");
                    mostrarAlertaFaltanDatos();
                } else {
                    if (result.getPotenciaStandBy() != null) {
                        binding.inputPotencia.setText(result.getPotenciaStandBy());
                    }

                    String resumen = "Datos técnicos listos:\n" +
                            "Motor: " + (result.getMarcaMotor() != null ? result.getMarcaMotor() : "-") + "\n" +
                            "Generador: " + (result.getMarcaGenerador() != null ? result.getMarcaGenerador() : "-");

                    binding.tvResumenTecnico.setText(resumen);
                    binding.btnGenerarCertificado.setEnabled(true);
                }
            }

            @Override
            public void onError(Exception e) {
                binding.progressBar.setVisibility(View.GONE);
                binding.tvResumenTecnico.setText("Error al obtener datos.");
            }
        });
    }

    private void mostrarAlertaFaltanDatos() {
        new AlertDialog.Builder(getContext())
                .setTitle("Datos Incompletos")
                .setMessage("El grupo seleccionado no tiene los datos técnicos (Placa, Motor, Serie) registrados en el módulo de Información General.\n\nPor favor complétalos antes de emitir el certificado.")
                .setPositiveButton("Entendido", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void setupListeners() {
        binding.inputFecha.setOnClickListener(v -> mostrarDatePicker());
        binding.btnGenerarCertificado.setOnClickListener(v -> generarCertificado());
    }

    private void generarCertificado() {
        if (datosTecnicosCargados == null) {
            Toast.makeText(getContext(), "Selecciona un grupo válido primero", Toast.LENGTH_SHORT).show();
            return;
        }

        String cliente = binding.inputCliente.getText().toString().trim();
        String fecha = binding.inputFecha.getText().toString().trim();
        String potencia = binding.inputPotencia.getText().toString().trim();

        if (cliente.isEmpty() || potencia.isEmpty()) {
            Toast.makeText(getContext(), "Completa el cliente y la potencia", Toast.LENGTH_SHORT).show();
            return;
        }

        CertificadoOperatividad cert = new CertificadoOperatividad();
        cert.setIdGrupo(idGrupoSeleccionado);
        cert.setCliente(cliente);
        cert.setFechaEmision(fecha);
        cert.setPotencia(potencia);

        cert.setMarcaGrupo(datosTecnicosCargados.getMarcaGrupo());
        cert.setModeloGrupo(datosTecnicosCargados.getModeloGrupo());
        cert.setSerieGrupo(datosTecnicosCargados.getSerieGrupo());

        cert.setMarcaMotor(datosTecnicosCargados.getMarcaMotor());
        cert.setModeloMotor(datosTecnicosCargados.getModeloMotor());
        cert.setSerieMotor(datosTecnicosCargados.getSerieMotor());

        cert.setMarcaGenerador(datosTecnicosCargados.getMarcaGenerador());
        cert.setModeloGenerador(datosTecnicosCargados.getModeloGenerador());
        cert.setSerieGenerador(datosTecnicosCargados.getSerieGenerador());

        // UI Feedback
        binding.btnGenerarCertificado.setText("Generando...");
        binding.btnGenerarCertificado.setEnabled(false);

        // Guardar
        repository.crearCertificado(cert, new CertificadosRepository.Callback<String>() {
            @Override
            public void onSuccess(String numeroCertificado) {
                generarWordEnSegundoPlano(cert);
            }

            @Override
            public void onError(Exception e) {
                binding.btnGenerarCertificado.setEnabled(true);
                binding.btnGenerarCertificado.setText("Generar certificado");
                Toast.makeText(getContext(), "Error al guardar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void generarWordEnSegundoPlano(CertificadoOperatividad cert) {
        new Thread(() -> {
            try {
                WordGenerator generator = new WordGenerator();
                File archivo = generator.generarCertificadoWord(requireContext(), cert, codigoGrupoSeleccionado);

                requireActivity().runOnUiThread(() -> {
                    if (getContext() == null) return;
                    binding.btnGenerarCertificado.setText("Documento Listo");
                    binding.btnGenerarCertificado.setEnabled(true);
                    abrirOCompartirDocumento(archivo);
                });
            } catch (Exception e) {
                requireActivity().runOnUiThread(() -> {
                    if (getContext() == null) return;
                    binding.btnGenerarCertificado.setEnabled(true);
                    binding.btnGenerarCertificado.setText("Reintentar");
                    Toast.makeText(getContext(), "Error generando Word", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void abrirOCompartirDocumento(File archivo) {
        try {
            Uri uri = FileProvider.getUriForFile(
                    requireContext(), requireContext().getPackageName() + ".provider", archivo);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "Compartir Certificado"));
        } catch (Exception e) {
            Toast.makeText(getContext(), "Error al compartir", Toast.LENGTH_SHORT).show();
        }
    }

    private void mostrarDatePicker() {
        Calendar cal = Calendar.getInstance();
        new DatePickerDialog(requireContext(), (view, year, month, dayOfMonth) -> {
            cal.set(year, month, dayOfMonth);
            actualizarFechaInput(cal);
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void actualizarFechaInput(Calendar cal) {
        String fecha = String.format(Locale.getDefault(), "%d de %s del %d",
                cal.get(Calendar.DAY_OF_MONTH),
                cal.getDisplayName(Calendar.MONTH, Calendar.LONG, new Locale("es", "ES")),
                cal.get(Calendar.YEAR));
        binding.inputFecha.setText(fecha);
    }
}