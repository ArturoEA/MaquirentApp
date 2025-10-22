package com.example.maquirentapp.View;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;

import com.example.maquirentapp.ViewModel.AlquilerDiaViewModel;
import com.example.maquirentapp.R;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class NuevoAlquilerDiaFragment extends Fragment {
    // Declaración de vistas
    private Spinner spinnerGrupo;
    private TextInputEditText fechaInicialEditText, fechaFinalEditText;
    private TextInputEditText clienteEditText, ubicacionEditText;
    private TextInputEditText horometroInicialEditText, horometroFinalEditText, precioDiaEditText;
    private Button saveButton;
    private ProgressBar progressBar;

    private AlquilerDiaViewModel viewModel;
    private final Calendar calendar = Calendar.getInstance();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflar el layout sin View Binding
        return inflater.inflate(R.layout.fragment_nuevo_alquiler_dia, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Inicializar vistas
        spinnerGrupo = view.findViewById(R.id.spinnerGrupo);
        fechaInicialEditText = view.findViewById(R.id.fechaInicialEditText);
        fechaFinalEditText = view.findViewById(R.id.fechaFinalEditText);
        clienteEditText = view.findViewById(R.id.clienteEditText);
        ubicacionEditText = view.findViewById(R.id.ubicacionEditText);
        horometroInicialEditText = view.findViewById(R.id.horometroInicialEditText);
        horometroFinalEditText = view.findViewById(R.id.horometroFinalEditText);
        precioDiaEditText = view.findViewById(R.id.precioDiaEditText);
        saveButton = view.findViewById(R.id.saveButton);
        progressBar = view.findViewById(R.id.progressBar);

        // Inicializar ViewModel
        viewModel = new ViewModelProvider(this).get(AlquilerDiaViewModel.class);

        setupUI();
        setupObservers();
    }

    private void setupUI() {
        setupSpinner();
        setupDatePickers();
        setupFormValidation();
        setupSaveButton();
    }

    private void setupSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.empresas_array,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerGrupo.setAdapter(adapter);

        spinnerGrupo.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                viewModel.getIdGrupo().setValue(position + 1);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                viewModel.getIdGrupo().setValue(null);
            }
        });
    }

    private void setupDatePickers() {
        fechaInicialEditText.setOnClickListener(v -> showDatePicker(true));
        fechaFinalEditText.setOnClickListener(v -> showDatePicker(false));
    }

    private void showDatePicker(boolean isInitialDate) {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    calendar.set(year, month, dayOfMonth);
                    updateDateField(isInitialDate, calendar.getTime());
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void updateDateField(boolean isInitialDate, Date date) {
        String formattedDate = dateFormat.format(date);
        if (isInitialDate) {
            fechaInicialEditText.setText(formattedDate);
            viewModel.getFechaInicial().setValue(date);
        } else {
            fechaFinalEditText.setText(formattedDate);
            viewModel.getFechaFinal().setValue(date);
        }
        viewModel.validateForm();
    }

    private void setupFormValidation() {
        setupTextInputListener(clienteEditText, text -> viewModel.getCliente().setValue(text));
        setupTextInputListener(ubicacionEditText, text -> viewModel.getUbicacion().setValue(text));
        setupTextInputListener(horometroInicialEditText, text -> parseAndSetDouble(text, viewModel.getHorometroInicial()));
        setupTextInputListener(horometroFinalEditText, text -> parseAndSetDouble(text, viewModel.getHorometroFinal()));
        setupTextInputListener(precioDiaEditText, text -> parseAndSetDouble(text, viewModel.getPrecioDia()));
    }

    private void parseAndSetDouble(String text, MutableLiveData<Double> liveData) {
        try {
            liveData.setValue(Double.parseDouble(text));
        } catch (NumberFormatException e) {
            liveData.setValue(null);
        }
        viewModel.validateForm();
    }

    private void setupTextInputListener(TextInputEditText editText, OnTextChangedListener listener) {
        editText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                listener.onTextChanged(editText.getText() != null ? editText.getText().toString() : "");
            }
        });
    }

    private void setupSaveButton() {
        saveButton.setOnClickListener(v -> viewModel.saveAlquiler());
    }

    private void setupObservers() {
        viewModel.getIsFormValid().observe(getViewLifecycleOwner(), isValid -> {
            saveButton.setEnabled(Boolean.TRUE.equals(isValid));
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), errorMessage -> {
            if (errorMessage != null && !errorMessage.isEmpty()) {
                showToast(errorMessage, Toast.LENGTH_LONG);
            }
        });

        viewModel.getSaveSuccess().observe(getViewLifecycleOwner(), success -> {
            if (Boolean.TRUE.equals(success)) {
                showToast("Alquiler guardado exitosamente", Toast.LENGTH_SHORT);
                clearForm();
            }
        });

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            if (isLoading != null) {
                progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
                saveButton.setEnabled(!isLoading);
            }
        });
    }

    private void showToast(String message, int duration) {
        Toast.makeText(requireContext(), message, duration).show();
    }

    private void clearForm() {
        clienteEditText.setText("");
        ubicacionEditText.setText("");
        horometroInicialEditText.setText("");
        horometroFinalEditText.setText("");
        fechaInicialEditText.setText("");
        fechaFinalEditText.setText("");
        precioDiaEditText.setText("");
        spinnerGrupo.setSelection(0);
    }

    interface OnTextChangedListener {
        void onTextChanged(String text);
    }
}