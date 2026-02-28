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
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.maquirentapp.Model.Cotizacion;
import com.example.maquirentapp.Model.GrupoElectrogeno;
import com.example.maquirentapp.Model.ItemCotizacion;
import com.example.maquirentapp.R;
import com.example.maquirentapp.Repository.CotizacionesRepository;
import com.example.maquirentapp.Access.CotizacionItemsAdapter;
import com.example.maquirentapp.Utils.HtmlGenerator;
import com.example.maquirentapp.Utils.PdfGenerator;
import com.example.maquirentapp.Utils.WordGenerator;
import com.example.maquirentapp.databinding.FragmentNuevaCotizacionBinding;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import com.example.maquirentapp.Model.FichaTecnica;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.tom_roush.pdfbox.multipdf.PDFMergerUtility;
import com.tom_roush.pdfbox.io.MemoryUsageSetting;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class NuevaCotizacionFragment extends Fragment {

    private FragmentNuevaCotizacionBinding binding;
    private CotizacionesRepository repository;
    private CotizacionItemsAdapter adapter;
    private Cotizacion cotizacionActual;
    private List<GrupoElectrogeno> listaGruposInventario = new ArrayList<>();
    private ArrayAdapter<String> autoCompleteAdapter;
    private boolean esEdicion = false;
    private List<FichaTecnica> listaFichas = new ArrayList<>();
    private FichaTecnica fichaSeleccionada = null;
    private ArrayAdapter<String> fichasAdapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        repository = new CotizacionesRepository();
        cotizacionActual = new Cotizacion();
        new Thread(() -> {
            if (getContext() != null) {
                WordGenerator.limpiarCacheAntiguo(requireContext());
            }
        }).start();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentNuevaCotizacionBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupRecyclerView();
        setupListeners();

        cargarFichasTecnicas();

        actualizarFechaInput(Calendar.getInstance());
        cargarGruposParaAutocompletado();
        if (getArguments() != null && getArguments().containsKey("cotizacion_a_editar")) {
            cotizacionActual = (Cotizacion) getArguments().getSerializable("cotizacion_a_editar");
            if (cotizacionActual != null) {
                esEdicion = true;
                rellenarDatosEnPantalla();
            }
        } else {
            cotizacionActual = new Cotizacion();
            actualizarFechaInput(Calendar.getInstance());
        }
    }
    private void rellenarDatosEnPantalla() {
        binding.inputCliente.setText(cotizacionActual.getClienteNombre());
        binding.inputRuc.setText(cotizacionActual.getClienteRuc());
        binding.inputLugar.setText(cotizacionActual.getLugarTrabajo());
        binding.inputFecha.setText(cotizacionActual.getFechaEmision());
        binding.inputHorasMinimas.setText(String.valueOf(cotizacionActual.getHorasMinimas()));

        if ("USD".equals(cotizacionActual.getMoneda())) {
            binding.radioUsd.setChecked(true);
        } else {
            binding.radioSol.setChecked(true);
        }

        actualizarListaYTotales();
    }

    private void cargarGruposParaAutocompletado() {
        repository.getGruposParaSeleccion(new CotizacionesRepository.Callback<List<GrupoElectrogeno>>() {
            @Override
            public void onSuccess(List<GrupoElectrogeno> grupos) {
                listaGruposInventario = grupos;
                List<String> nombresGrupos = new ArrayList<>();
                for (GrupoElectrogeno g : grupos) {
                    nombresGrupos.add(g.getCodigo());
                }

                autoCompleteAdapter = new ArrayAdapter<>(
                        requireContext(),
                        android.R.layout.simple_dropdown_item_1line,
                        nombresGrupos
                );
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(getContext(), "Error cargando inventario", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupRecyclerView() {
        binding.recyclerItems.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new CotizacionItemsAdapter(new CotizacionItemsAdapter.OnItemActionListener() {
            @Override
            public void onEliminar(ItemCotizacion item) {
                cotizacionActual.getItems().remove(item);
                actualizarListaYTotales();
            }

            @Override
            public void onEditar(ItemCotizacion item) {
                mostrarDialogoAgregarItem(item);
            }
        });

        binding.recyclerItems.setAdapter(adapter);
    }

    private void setupListeners() {
        binding.inputFecha.setOnClickListener(v -> mostrarDatePicker());
        binding.btnAgregarItem.setOnClickListener(v -> mostrarDialogoAgregarItem(null));
        binding.btnGenerarCotizacion.setOnClickListener(v -> guardarYGenerar("WORD"));
        binding.btnGenerarPdf.setOnClickListener(v -> guardarYGenerar("PDF"));

        binding.inputHorasMinimas.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(android.text.Editable s) {
                recalcularPreciosHoraExtraGlobal();
            }
        });
    }
    private void recalcularPreciosHoraExtraGlobal() {
        int horasMinimas = getHorasMinimasActuales();

        if (horasMinimas <= 0) return;

        boolean huboCambios = false;

        if (cotizacionActual != null && cotizacionActual.getItems() != null) {
            for (ItemCotizacion item : cotizacionActual.getItems()) {
                double precioMensual = item.getPrecioMensual();

                double nuevoPrecioHE = (precioMensual / 200) * 0.75;

                nuevoPrecioHE = Math.round(nuevoPrecioHE * 100.0) / 100.0;

                if (Math.abs(item.getPrecioHoraExtra() - nuevoPrecioHE) > 0.01) {
                    item.setPrecioHoraExtra(nuevoPrecioHE);
                    huboCambios = true;
                }
            }
        }
        if (huboCambios) {
            actualizarListaYTotales();
        }
    }
    private void mostrarDialogoAgregarItem(ItemCotizacion itemExistente) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_agregar_item_cotizacion, null);
        builder.setView(view);
        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null)
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        AutoCompleteTextView autoDesc = view.findViewById(R.id.autoCompleteDescripcion);
        TextInputEditText inputPotencia = view.findViewById(R.id.inputPotencia);
        TextInputEditText inputMarca = view.findViewById(R.id.inputMarca);
        TextInputEditText inputModo = view.findViewById(R.id.inputModo);
        TextInputEditText inputIncluye = view.findViewById(R.id.inputIncluye);
        TextInputEditText inputPrecio = view.findViewById(R.id.inputPrecio);
        TextInputEditText inputPrecioHE = view.findViewById(R.id.inputPrecioHE);
        Button btnGuardar = view.findViewById(R.id.btnGuardarItem);

        if (autoCompleteAdapter != null) {
            autoDesc.setAdapter(autoCompleteAdapter);

            autoDesc.setOnItemClickListener((parent, v, position, id) -> {
                String seleccion = (String) parent.getItemAtPosition(position);
                for (GrupoElectrogeno g : listaGruposInventario) {
                    if (seleccion.contains(g.getCodigo())) {
                        break;
                    }
                }
            });
        }

        inputPrecio.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(android.text.Editable s) {
                calcularHoraExtraAutomatica(inputPrecio, inputPrecioHE);
            }
        });

        if (itemExistente != null) {
            autoDesc.setText(itemExistente.getDescripcionEquipo());
            inputPotencia.setText(itemExistente.getPotencia());
            inputMarca.setText(itemExistente.getMarca());
            inputModo.setText(itemExistente.getModoTrabajo());
            inputIncluye.setText(itemExistente.getIncluye());
            inputPrecio.setText(String.valueOf(itemExistente.getPrecioMensual()));
            inputPrecioHE.setText(String.valueOf(itemExistente.getPrecioHoraExtra()));
            btnGuardar.setText("Actualizar");
        } else {
            inputIncluye.setText("Bandeja y kit anti derrame, extintor, varilla puesta a tierra y certificado de operatividad");
        }

        btnGuardar.setOnClickListener(v -> {
            if (autoDesc.getText().toString().isEmpty() || inputPrecio.getText().toString().isEmpty()) {
                Toast.makeText(getContext(), "Descripción y precio son obligatorios", Toast.LENGTH_SHORT).show();
                return;
            }

            String desc = autoDesc.getText().toString();
            String pot = inputPotencia.getText().toString();
            String marca = inputMarca.getText().toString();
            String modo = inputModo.getText().toString();
            String incluye = inputIncluye.getText().toString();
            double precio = 0;
            double precioHE = 0;

            try {
                precio = Double.parseDouble(inputPrecio.getText().toString());
                if (!inputPrecioHE.getText().toString().isEmpty()) {
                    precioHE = Double.parseDouble(inputPrecioHE.getText().toString());
                }
            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), "Precios inválidos", Toast.LENGTH_SHORT).show();
                return;
            }

            cotizacionActual.setHorasMinimas(getHorasMinimasActuales());

            if (itemExistente != null) {
                int index = cotizacionActual.getItems().indexOf(itemExistente);
                ItemCotizacion itemEditado = new ItemCotizacion(desc, pot, modo, marca, precio, precioHE);
                itemEditado.setIncluye(incluye);
                cotizacionActual.getItems().set(index, itemEditado);
            } else {
                ItemCotizacion nuevoItem = new ItemCotizacion(desc, pot, modo, marca, precio, precioHE);
                nuevoItem.setIncluye(incluye);
                cotizacionActual.getItems().add(nuevoItem);
            }

            actualizarListaYTotales();
            dialog.dismiss();
        });

        dialog.show();
    }

    private void calcularHoraExtraAutomatica(TextInputEditText inputPrecio, TextInputEditText inputHE) {
        try {
            String precioStr = inputPrecio.getText().toString().trim();

            if (precioStr.isEmpty() || precioStr.equals(".")) {
                inputHE.setText("");
                return;
            }

            double precioMensual = Double.parseDouble(precioStr);
            int horas = getHorasMinimasActuales();

            if (horas > 0) {
                // Fórmula: (Precio / Horas) * 0.75
                double precioHE = (precioMensual / 200) * 0.75;

                inputHE.setText(String.format(Locale.US, "%.2f", precioHE));
            }
        } catch (NumberFormatException e) {
            inputHE.setText("");
        } catch (Exception e) {
        }
    }

    private void actualizarListaYTotales() {
        adapter.submitList(new ArrayList<>(cotizacionActual.getItems()));

        cotizacionActual.calcularTotales();

        binding.tvSubtotalGlobal.setText(String.format(Locale.US, "%.2f", cotizacionActual.getSubtotalGlobal()));
        binding.tvIgvGlobal.setText(String.format(Locale.US, "%.2f", cotizacionActual.getIgvGlobal()));
        binding.tvTotalGlobal.setText(String.format(Locale.US, "%.2f", cotizacionActual.getTotalGlobal()));
    }
    private void guardarYGenerar(String tipoDocumento) {
        if (binding.inputCliente.getText().toString().isEmpty()) {
            binding.inputCliente.setError("Requerido");
            return;
        }
        if (cotizacionActual.getItems().isEmpty()) {
            Toast.makeText(getContext(), "Agrega al menos un equipo", Toast.LENGTH_SHORT).show();
            return;
        }

        String seleccionVisual = binding.autoCompleteFichas.getText().toString();

        if (seleccionVisual.equals("Ninguna (No adjuntar)") || seleccionVisual.isEmpty()) {
            fichaSeleccionada = null;
            cotizacionActual.setFichaTecnicaNombre(null);
        } else {
            cotizacionActual.setFichaTecnicaNombre(seleccionVisual);

            // Emparejamos el texto con el objeto real para tener su URL de descarga
            boolean fichaEncontrada = false;
            for (FichaTecnica f : listaFichas) {
                if (f.getNombreArchivo().equals(seleccionVisual)) {
                    fichaSeleccionada = f;
                    fichaEncontrada = true;
                    break;
                }
            }

            // Si hay texto pero el objeto no está, Firebase sigue descargando la lista.
            if (!fichaEncontrada) {
                Toast.makeText(getContext(), "Sincronizando ficha técnica, por favor intenta en un segundo...", Toast.LENGTH_LONG).show();
                return;
            }
        }

        cotizacionActual.setClienteNombre(binding.inputCliente.getText().toString());
        cotizacionActual.setClienteRuc(binding.inputRuc.getText().toString());
        cotizacionActual.setLugarTrabajo(binding.inputLugar.getText().toString());
        cotizacionActual.setFechaEmision(binding.inputFecha.getText().toString());
        cotizacionActual.setMoneda(binding.radioSol.isChecked() ? "SOL" : "USD");
        cotizacionActual.setHorasMinimas(getHorasMinimasActuales());

        // Guardar el nombre de la ficha seleccionada (o null si eligió "Ninguna")
        if (fichaSeleccionada != null) {
            cotizacionActual.setFichaTecnicaNombre(fichaSeleccionada.getNombreArchivo());
        } else {
            cotizacionActual.setFichaTecnicaNombre(null);
        }

        // Deshabilitar ambos botones para evitar doble envío
        binding.btnGenerarCotizacion.setEnabled(false);
        binding.btnGenerarPdf.setEnabled(false);

        if (esEdicion) {
            repository.actualizarCotizacion(cotizacionActual, new CotizacionesRepository.Callback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    Toast.makeText(getContext(), "Cotización actualizada", Toast.LENGTH_SHORT).show();
                    // Derivar a la función correcta
                    if (tipoDocumento.equals("WORD")) generarDocumentoWord();
                    else generarDocumentoPdf();
                }

                @Override
                public void onError(Exception e) {
                    habilitarBotones();
                    Toast.makeText(getContext(), "Error al actualizar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            repository.crearCotizacion(cotizacionActual, new CotizacionesRepository.Callback<String>() {
                @Override
                public void onSuccess(String numeroCotizacion) {
                    if (getContext() == null) return;
                    Toast.makeText(getContext(), "Cotización " + numeroCotizacion + " guardada.", Toast.LENGTH_SHORT).show();

                    // Derivar a la función correcta
                    if (tipoDocumento.equals("WORD")) generarDocumentoWord();
                    else generarDocumentoPdf();
                }

                @Override
                public void onError(Exception e) {
                    habilitarBotones();
                    Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
    private void habilitarBotones() {
        binding.btnGenerarCotizacion.setEnabled(true);
        binding.btnGenerarPdf.setEnabled(true);
    }
    private void generarDocumentoWord() {
        binding.btnGenerarCotizacion.setText("Generando Word...");

        new Thread(() -> {
            try {
                WordGenerator generator = new WordGenerator();
                File archivoWord = generator.generarCotizacionWord(requireContext(), cotizacionActual);

                requireActivity().runOnUiThread(() -> {
                    binding.btnGenerarCotizacion.setText(esEdicion ? "Word" : "Word");
                    habilitarBotones();
                    abrirOCompartirDocumento(archivoWord);
                });
            } catch (Exception e) {
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Error generando Word: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    binding.btnGenerarCotizacion.setText("Word");
                    habilitarBotones();
                    e.printStackTrace();
                });
            }
        }).start();
    }

    private void generarDocumentoPdf() {
        binding.btnGenerarPdf.setText("Generando PDF...");

        HtmlGenerator htmlGen = new HtmlGenerator();
        String html = htmlGen.generarHtmlCotizacion(requireContext(), cotizacionActual);

        PdfGenerator pdfGen = new PdfGenerator();
        String nombreArchivo = "Cotizacion_" + cotizacionActual.getNumeroCotizacion();

        pdfGen.generarPdfDesdeWebView(binding.webViewPdfHidden, html, nombreArchivo, new PdfGenerator.OnPdfGeneratedListener() {
            @Override
            public void onPdfGenerated(File pdfFile) {
                if (fichaSeleccionada != null) {
                    fusionarConFichaTecnica(pdfFile, fichaSeleccionada);
                } else {
                    requireActivity().runOnUiThread(() -> {
                        binding.btnGenerarPdf.setText("PDF");
                        habilitarBotones();
                        abrirEnVisorInterno(pdfFile);
                    });
                }
            }

            @Override
            public void onError(String error) {
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Error: " + error, Toast.LENGTH_SHORT).show();
                    binding.btnGenerarPdf.setText("PDF");
                    habilitarBotones();
                });
            }
        });
    }
    private void abrirEnVisorInterno(File pdfFile) {
        Intent intent = new Intent(getContext(), PdfViewerActivity.class);
        intent.putExtra("PDF_URL", pdfFile.getAbsolutePath());
        intent.putExtra("NOMBRE_ARCHIVO", "Cotizacion_" + cotizacionActual.getNumeroCotizacion());
        startActivity(intent);
    }
    private void abrirOCompartirDocumento(File archivo) {
        try {
            Uri uri = androidx.core.content.FileProvider.getUriForFile(
                    requireContext(),
                    requireContext().getPackageName() + ".provider",
                    archivo
            );

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            startActivity(Intent.createChooser(intent, "Abrir Cotización con..."));

        } catch (Exception e) {
            Toast.makeText(getContext(), "No tienes una app para abrir Word instalada.", Toast.LENGTH_LONG).show();
        }
    }
    private void cargarFichasTecnicas() {
        listaFichas.clear();

        List<String> nombresFichas = new ArrayList<>();
        nombresFichas.add("Ninguna (No adjuntar)");

        fichasAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, nombresFichas);
        binding.autoCompleteFichas.setAdapter(fichasAdapter);

        if (esEdicion && cotizacionActual.getFichaTecnicaNombre() != null) {
            binding.autoCompleteFichas.setText(cotizacionActual.getFichaTecnicaNombre(), false);
        } else {
            binding.autoCompleteFichas.setText("Ninguna (No adjuntar)", false);
        }

        // Buscar en Firebase Storage
        FirebaseStorage.getInstance().getReference().child("fichas_tecnicas/").listAll()
                .addOnSuccessListener(listResult -> {
                    for (StorageReference item : listResult.getItems()) {
                        item.getDownloadUrl().addOnSuccessListener(uri -> {
                            FichaTecnica ficha = new FichaTecnica();
                            ficha.setNombreArchivo(item.getName());
                            ficha.setUrlPdf(uri.toString());

                            listaFichas.add(ficha);
                            fichasAdapter.add(item.getName());
                            fichasAdapter.notifyDataSetChanged();
                        });
                    }
                    // Si estamos en modo edición y la cotización tenía una ficha guardada, la pre-seleccionamos
                    if (esEdicion && cotizacionActual.getFichaTecnicaNombre() != null) {
                        binding.autoCompleteFichas.setText(cotizacionActual.getFichaTecnicaNombre(), false);

                        // Buscar la ficha en la lista para asignarla a la variable global
                        for (FichaTecnica f : listaFichas) {
                            if (f.getNombreArchivo().equals(cotizacionActual.getFichaTecnicaNombre())) {
                                fichaSeleccionada = f;
                                break;
                            }
                        }
                    }

                    // Escuchar qué elige el usuario
                    binding.autoCompleteFichas.setOnItemClickListener((parent, view, position, id) -> {
                        String seleccion = fichasAdapter.getItem(position);
                        if (seleccion.equals("Ninguna (No adjuntar)")) {
                            fichaSeleccionada = null;
                        } else {
                            for (FichaTecnica f : listaFichas) {
                                if (f.getNombreArchivo().equals(seleccion)) {
                                    fichaSeleccionada = f;
                                    break;
                                }
                            }
                        }
                    });
                });
    }
    private void fusionarConFichaTecnica(File pdfCotizacion, FichaTecnica ficha) {
        requireActivity().runOnUiThread(() -> {
            binding.btnGenerarPdf.setText("Adjuntando Ficha...");
        });

        new Thread(() -> {
            try {
                // 1. Descargar el PDF de Firebase a la caché del celular
                File tempFicha = new File(requireContext().getCacheDir(), "temp_ficha_tecnica.pdf");
                URL url = new URL(ficha.getUrlPdf());
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                InputStream input = connection.getInputStream();
                FileOutputStream output = new FileOutputStream(tempFicha);

                byte[] data = new byte[1024];
                int count;
                while ((count = input.read(data)) != -1) {
                    output.write(data, 0, count);
                }
                output.flush();
                output.close();
                input.close();

                // 2. Fusionar los PDFs con PDFBox
                File mergedPdf = new File(requireContext().getCacheDir(), "Cotizacion_" + cotizacionActual.getNumeroCotizacion() + ".pdf");

                PDFMergerUtility merger = new PDFMergerUtility();
                merger.addSource(pdfCotizacion);
                merger.addSource(tempFicha);
                merger.setDestinationFileName(mergedPdf.getAbsolutePath());

                // Ejecutar la unión
                merger.mergeDocuments(MemoryUsageSetting.setupMainMemoryOnly());

                // 3. Abrir el PDF final consolidado
                requireActivity().runOnUiThread(() -> {
                    binding.btnGenerarPdf.setText("PDF");
                    habilitarBotones();
                    abrirEnVisorInterno(mergedPdf);
                });

            } catch (Exception e) {
                e.printStackTrace();
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Error al adjuntar ficha. Abriendo cotización simple.", Toast.LENGTH_LONG).show();
                    binding.btnGenerarPdf.setText("PDF");
                    habilitarBotones();
                    abrirEnVisorInterno(pdfCotizacion);
                });
            }
        }).start();
    }
    private int getHorasMinimasActuales() {
        try {
            String val = binding.inputHorasMinimas.getText().toString();
            return val.isEmpty() ? 200 : Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return 200;
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
        String fecha = String.format(Locale.getDefault(), "Cajamarca, %d de %s del %d",
                cal.get(Calendar.DAY_OF_MONTH),
                cal.getDisplayName(Calendar.MONTH, Calendar.LONG, new Locale("es", "ES")),
                cal.get(Calendar.YEAR));
        binding.inputFecha.setText(fecha);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}