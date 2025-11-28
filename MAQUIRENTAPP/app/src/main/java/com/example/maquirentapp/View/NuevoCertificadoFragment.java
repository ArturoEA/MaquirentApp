package com.example.maquirentapp.View;

import android.Manifest;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.maquirentapp.Model.CertificadoOperatividad;
import com.example.maquirentapp.Model.GrupoElectrogeno;
import com.example.maquirentapp.Model.InfoPlaca;
import com.example.maquirentapp.R;
import com.example.maquirentapp.Repository.CertificadosRepository;
import com.example.maquirentapp.Utils.PdfGenerator;
import com.example.maquirentapp.databinding.FragmentNuevoCertificadoBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class NuevoCertificadoFragment extends Fragment {

    private static final int REQUEST_WRITE_STORAGE = 112;

    private FragmentNuevoCertificadoBinding binding;
    private CertificadosRepository repository;
    private String idGrupoSeleccionado;
    private String codigoGrupoSeleccionado;
    private InfoPlaca datosTecnicosCargados;
    private List<GrupoElectrogeno> listaGrupos = new ArrayList<>();
    private CertificadoOperatividad certificadoPendiente;

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

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                R.layout.spinner_item_black,
                codigos
        );
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
                    if (!camposTecnicosValidos(result)) {
                        binding.tvResumenTecnico.setText("Datos técnicos incompletos (Motor, Generador o Grupo vacíos).");
                        mostrarAlertaFaltanDatos();
                    } else {
                        if (result.getPotenciaStandBy() != null) {
                            binding.inputPotencia.setText(result.getPotenciaStandBy());
                        }

                        String resumen = "Datos técnicos listos:\n" +
                                "Equipo: " + result.getMarcaGrupo() + "\n" +
                                "Motor: " + result.getMarcaMotor() + "\n" +
                                "Generador: " + result.getMarcaGenerador();

                        binding.tvResumenTecnico.setText(resumen);
                        binding.btnGenerarCertificado.setEnabled(true);
                    }
                }
            }

            @Override
            public void onError(Exception e) {
                binding.progressBar.setVisibility(View.GONE);
                binding.tvResumenTecnico.setText("Error al obtener datos.");
            }
        });
    }
    private boolean camposTecnicosValidos(InfoPlaca info) {
        return esValido(info.getMarcaGrupo()) && esValido(info.getModeloGrupo()) && esValido(info.getSerieGrupo()) &&
                esValido(info.getMarcaMotor()) && esValido(info.getModeloMotor()) && esValido(info.getSerieMotor()) &&
                esValido(info.getMarcaGenerador()) && esValido(info.getModeloGenerador()) && esValido(info.getSerieGenerador());
    }

    private boolean esValido(String texto) {
        return texto != null && !texto.trim().isEmpty();
    }
    private void mostrarAlertaFaltanDatos() {
        new MaterialAlertDialogBuilder(getContext(), R.style.DialogoConFuenteAnta)
                .setTitle("Datos Incompletos")
                .setMessage("El grupo seleccionado no tiene los datos técnicos (Placa, Motor, Serie) registrados en el módulo de Información General.\n\nPor favor complétalos antes de emitir el certificado.")
                .setPositiveButton("Entendido", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void setupListeners() {
        binding.inputFecha.setOnClickListener(v -> mostrarDatePicker());
        binding.btnGenerarCertificado.setOnClickListener(v -> {
            if (verificarPermisos()) {
                generarCertificado();
            } else {
                solicitarPermisos();
            }
        });
    }

    private boolean verificarPermisos() {
        // Android 10+ no necesita permiso para escribir en Documents
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return true;
        }

        return ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    private void solicitarPermisos() {
        ActivityCompat.requestPermissions(requireActivity(),
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                REQUEST_WRITE_STORAGE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_WRITE_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                generarCertificado();
            } else {
                Toast.makeText(getContext(),
                        "Se necesita permiso para guardar el PDF",
                        Toast.LENGTH_SHORT).show();
            }
        }
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

        // Crear objeto certificado
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
        binding.progressBar.setVisibility(View.VISIBLE);

        // Guardar en Firebase primero
        repository.crearCertificado(cert, new CertificadosRepository.Callback<String>() {
            @Override
            public void onSuccess(String numeroCertificado) {
                // Guardar referencia para generar PDF
                certificadoPendiente = cert;
                generarDocumentoEnSegundoPlano(cert);
            }

            @Override
            public void onError(Exception e) {
                binding.progressBar.setVisibility(View.GONE);
                binding.btnGenerarCertificado.setEnabled(true);
                binding.btnGenerarCertificado.setText("Generar certificado");
                Toast.makeText(getContext(),
                        "Error al guardar: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void generarDocumentoEnSegundoPlano(CertificadoOperatividad cert) {
        // Generar PDF en hilo secundario
        new Thread(() -> {
            try {
                PdfGenerator generator = new PdfGenerator();

                // Generar PDF
                File archivo = generator.generarCertificadoPdf(
                        requireContext(),
                        cert,
                        codigoGrupoSeleccionado
                );

                // Actualizar UI en hilo principal
                requireActivity().runOnUiThread(() -> {
                    if (getContext() == null) return;

                    binding.progressBar.setVisibility(View.GONE);
                    binding.btnGenerarCertificado.setText("Certificado PDF Listo");
                    binding.btnGenerarCertificado.setEnabled(true);

                    Toast.makeText(getContext(),
                            "PDF generado: " + archivo.getName(),
                            Toast.LENGTH_LONG).show();

                    // Mostrar opciones para abrir/compartir
                    mostrarDialogoOpciones(archivo);
                });

            } catch (Exception e) {
                requireActivity().runOnUiThread(() -> {
                    if (getContext() == null) return;

                    binding.progressBar.setVisibility(View.GONE);
                    binding.btnGenerarCertificado.setEnabled(true);
                    binding.btnGenerarCertificado.setText("Reintentar");

                    Toast.makeText(getContext(),
                            "Error generando PDF: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                });
            }
        }).start();
    }

    private void mostrarDialogoOpciones(File archivo) {
        new MaterialAlertDialogBuilder(requireContext(), R.style.DialogoConFuenteAnta)
                .setTitle("Certificado PDF Generado")
                .setMessage("¿Qué deseas hacer con el certificado?")
                .setPositiveButton("Abrir", (dialog, which) -> abrirPdf(archivo))
                .setNegativeButton("Compartir", (dialog, which) -> compartirPdf(archivo))
                .setNeutralButton("Cerrar", null)
                .show();
    }

    private void abrirPdf(File archivo) {
        try {
            Uri uri = androidx.core.content.FileProvider.getUriForFile(
                    requireContext(),
                    requireContext().getPackageName() + ".provider",
                    archivo
            );

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "application/pdf");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            // Esta bandera ayuda a veces cuando se abre desde fuera de una Activity
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            // CAMBIO: Intentamos abrir directamente sin preguntar 'resolveActivity'
            startActivity(intent);

        } catch (android.content.ActivityNotFoundException e) {
            // Solo si realmente no hay ninguna app, caemos aquí
            Toast.makeText(getContext(),
                    "No tienes una aplicación instalada para abrir archivos PDF.",
                    Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(getContext(),
                    "Error al intentar abrir el archivo: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void compartirPdf(File archivo) {
        try {
            Uri uri = androidx.core.content.FileProvider.getUriForFile(
                    requireContext(),
                    requireContext().getPackageName() + ".provider",
                    archivo
            );

            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("application/pdf");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.putExtra(Intent.EXTRA_SUBJECT, "Certificado de Operatividad");
            intent.putExtra(Intent.EXTRA_TEXT,
                    "Certificado de operatividad del grupo " + codigoGrupoSeleccionado);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(Intent.createChooser(intent, "Compartir Certificado PDF"));
        } catch (Exception e) {
            Toast.makeText(getContext(),
                    "Error al compartir: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void mostrarDatePicker() {
        Calendar cal = Calendar.getInstance();
        new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    cal.set(year, month, dayOfMonth);
                    actualizarFechaInput(cal);
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    private void actualizarFechaInput(Calendar cal) {
        String fecha = String.format(Locale.getDefault(), "%d de %s del %d",
                cal.get(Calendar.DAY_OF_MONTH),
                cal.getDisplayName(Calendar.MONTH, Calendar.LONG, new Locale("es", "ES")),
                cal.get(Calendar.YEAR)
        );
        binding.inputFecha.setText(fecha);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}