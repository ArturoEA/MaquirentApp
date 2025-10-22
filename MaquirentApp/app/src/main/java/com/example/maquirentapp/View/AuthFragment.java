package com.example.maquirentapp.View;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.maquirentapp.MainActivity;
import com.example.maquirentapp.Network.FirebaseServicio;
import com.example.maquirentapp.Model.Usuario;
import com.example.maquirentapp.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;

public class AuthFragment extends Fragment {
    private TextInputEditText inputEmail, inputPassword, inputConfirmPassword, inputNombre;
    private TextInputLayout layoutEmail, layoutPassword, layoutConfirmPassword, layoutNombre;
    private MaterialButton btnLogin, btnToggleMode;
    private TextView tvForgotPassword, tvModoActual, tvInfoNote;
    private FirebaseServicio firebaseServicio;
    private boolean isLoginMode = true;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_auth, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        firebaseServicio = new FirebaseServicio();
        initViews(view);
        setupListeners();
    }

    private void initViews(View view) {
        // TextInputLayouts
        layoutEmail = view.findViewById(R.id.layoutEmail);
        layoutPassword = view.findViewById(R.id.layoutPassword);
        layoutConfirmPassword = view.findViewById(R.id.layoutConfirmPassword);
        layoutNombre = view.findViewById(R.id.layoutNombre);

        // EditTexts
        inputEmail = view.findViewById(R.id.inputEmail);
        inputPassword = view.findViewById(R.id.inputPassword);
        inputConfirmPassword = view.findViewById(R.id.inputConfirmPassword);
        inputNombre = view.findViewById(R.id.inputNombre);

        // Botones y TextViews
        btnLogin = view.findViewById(R.id.btnLogin);
        btnToggleMode = view.findViewById(R.id.btnToggleMode);
        tvForgotPassword = view.findViewById(R.id.tvForgotPassword);
        tvModoActual = view.findViewById(R.id.tvModoActual);
        tvInfoNote = view.findViewById(R.id.tvInfoNote);


        updateUI();
    }

    private void setupListeners() {
        btnLogin.setOnClickListener(v -> {
            if (isLoginMode) {
                iniciarSesion();
            } else {
                registrarUsuario();
            }
        });

        btnToggleMode.setOnClickListener(v -> {
            isLoginMode = !isLoginMode;
            updateUI();
            clearErrors();
        });

        tvForgotPassword.setOnClickListener(v -> mostrarDialogoRecuperarPassword());

        // Limpiar errores al escribir
        inputEmail.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) layoutEmail.setError(null);
        });

        inputPassword.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) layoutPassword.setError(null);
        });

        inputConfirmPassword.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) layoutConfirmPassword.setError(null);
        });

        inputNombre.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) layoutNombre.setError(null);
        });
    }

    private void updateUI() {
        if (isLoginMode) {
            tvModoActual.setText("Iniciar sesión");
            layoutNombre.setVisibility(View.GONE);
            layoutConfirmPassword.setVisibility(View.GONE);
            tvForgotPassword.setVisibility(View.VISIBLE);
            tvInfoNote.setVisibility(View.GONE);
            btnLogin.setText("Iniciar sesión");
            btnToggleMode.setText("¿No tienes una cuenta? Regístrate");
        } else {
            tvModoActual.setText("Crear Cuenta");
            layoutNombre.setVisibility(View.VISIBLE);
            layoutConfirmPassword.setVisibility(View.VISIBLE);
            tvForgotPassword.setVisibility(View.GONE);
            tvInfoNote.setVisibility(View.VISIBLE);
            btnLogin.setText("Registrarse");
            btnToggleMode.setText("¿Ya tienes una cuenta? Inicia sesión");
        }
    }

    private void iniciarSesion() {
        String email = inputEmail.getText().toString().trim();
        String password = inputPassword.getText().toString().trim();

        // Validar campos
        if (!validarCamposLogin(email, password)) {
            return;
        }

        btnLogin.setEnabled(false);
        btnLogin.setText("Iniciando...");

        firebaseServicio.iniciarSesion(email, password, new FirebaseServicio.OnAuthListener() {
            @Override
            public void onLoginExitoso(Usuario usuario) {
                Toast.makeText(getContext(), "Bienvenido " + usuario.getNombre(), Toast.LENGTH_SHORT).show();
                reiniciarMainActivity();
            }

            @Override
            public void onRegistroExitoso(Usuario usuario) {
                // No se usa en login
            }

            @Override
            public void onUsuarioPendiente() {
                Toast.makeText(getContext(),
                        "Tu cuenta está pendiente de aprobación por un administrador",
                        Toast.LENGTH_LONG).show();
                resetButton();
            }

            @Override
            public void onUsuarioInactivo() {
                Toast.makeText(getContext(),
                        "Tu cuenta ha sido desactivada. Contacta al administrador",
                        Toast.LENGTH_LONG).show();
                resetButton();
            }

            @Override
            public void onError(Exception e) {
                // Mensaje genérico para no revelar información
                Toast.makeText(getContext(),
                        "Credenciales incorrectas. Verifica tu correo y contraseña",
                        Toast.LENGTH_LONG).show();
                resetButton();
            }
        });
    }

    private void registrarUsuario() {
        String email = inputEmail.getText().toString().trim();
        String password = inputPassword.getText().toString().trim();
        String confirmPassword = inputConfirmPassword.getText().toString().trim();
        String nombre = inputNombre.getText().toString().trim();

        // Validar campos
        if (!validarCamposRegistro(email, password, confirmPassword, nombre)) {
            return;
        }

        btnLogin.setEnabled(false);
        btnLogin.setText("Registrando...");

        firebaseServicio.registrarUsuario(email, password, nombre, new FirebaseServicio.OnAuthListener() {
            @Override
            public void onLoginExitoso(Usuario usuario) {
                // No se usa en registro
            }

            @Override
            public void onRegistroExitoso(Usuario usuario) {
                mostrarDialogoRegistroExitoso();
            }

            @Override
            public void onUsuarioPendiente() {
                // No se usa en registro
            }

            @Override
            public void onUsuarioInactivo() {
                // No se usa en registro
            }

            @Override
            public void onError(Exception e) {
                String mensaje = "Error al registrar. Intenta nuevamente";

                // Personalizar mensaje según el error
                if (e.getMessage() != null) {
                    if (e.getMessage().contains("email address is already in use")) {
                        mensaje = "Este correo ya está registrado";
                    } else if (e.getMessage().contains("network error")) {
                        mensaje = "Error de conexión. Verifica tu internet";
                    }
                }

                Toast.makeText(getContext(), mensaje, Toast.LENGTH_LONG).show();
                resetButton();
            }
        });
    }

    private boolean validarCamposLogin(String email, String password) {
        clearErrors();
        boolean isValid = true;

        // Validar email vacío
        if (TextUtils.isEmpty(email)) {
            layoutEmail.setError("Ingresa tu correo electrónico");
            isValid = false;
        }
        // Validar formato de email
        else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            layoutEmail.setError("Formato de correo inválido");
            isValid = false;
        }

        // Validar password vacío
        if (TextUtils.isEmpty(password)) {
            layoutPassword.setError("Ingresa tu contraseña");
            isValid = false;
        }

        return isValid;
    }

    private boolean validarCamposRegistro(String email, String password, String confirmPassword, String nombre) {
        clearErrors();
        boolean isValid = true;

        // Validar nombre
        if (TextUtils.isEmpty(nombre)) {
            layoutNombre.setError("Ingresa tu nombre completo");
            isValid = false;
        } else if (nombre.length() < 3) {
            layoutNombre.setError("El nombre debe tener al menos 3 caracteres");
            isValid = false;
        }

        // Validar email
        if (TextUtils.isEmpty(email)) {
            layoutEmail.setError("Ingresa tu correo electrónico");
            isValid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            layoutEmail.setError("Formato de correo inválido");
            isValid = false;
        }

        // Validar password
        if (TextUtils.isEmpty(password)) {
            layoutPassword.setError("Ingresa una contraseña");
            isValid = false;
        } else if (password.length() < 6) {
            layoutPassword.setError("La contraseña debe tener al menos 6 caracteres");
            isValid = false;
        } else if (!esPasswordSeguro(password)) {
            layoutPassword.setError("La contraseña debe incluir letras y números");
            isValid = false;
        }

        // Validar confirmación de password
        if (TextUtils.isEmpty(confirmPassword)) {
            layoutConfirmPassword.setError("Confirma tu contraseña");
            isValid = false;
        } else if (!password.equals(confirmPassword)) {
            layoutConfirmPassword.setError("Las contraseñas no coinciden");
            isValid = false;
        }

        return isValid;
    }

    private boolean esPasswordSeguro(String password) {
        // Verificar que tenga al menos una letra y un número
        boolean tieneLetra = password.matches(".*[a-zA-Z].*");
        boolean tieneNumero = password.matches(".*\\d.*");
        return tieneLetra && tieneNumero;
    }

    private void mostrarDialogoRecuperarPassword() {
        View dialogView = LayoutInflater.from(getContext()).inflate(
                R.layout.dialog_recuperar_password, null);

        TextInputLayout layoutEmailRecuperar = dialogView.findViewById(R.id.layoutEmailRecuperar);
        TextInputEditText inputEmailRecuperar = dialogView.findViewById(R.id.inputEmailRecuperar);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setPositiveButton("Enviar", null)
                .setNegativeButton("Cancelar", null)
                .create();

        dialog.setOnShowListener(dialogInterface -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String email = inputEmailRecuperar.getText().toString().trim();

                if (TextUtils.isEmpty(email)) {
                    layoutEmailRecuperar.setError("Ingresa tu correo");
                    return;
                }

                if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    layoutEmailRecuperar.setError("Formato de correo inválido");
                    return;
                }

                enviarEmailRecuperacion(email);
                dialog.dismiss();
            });
        });

        dialog.show();
    }
    private void enviarEmailRecuperacion(String email) {
        FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        mostrarDialogoExito(
                                "Correo enviado",
                                "Se han enviado las instrucciones de recuperación de contraeña a " + email +
                                        ". El enlace será válido por 15 minutos. No olvides revisar correos spam."
                        );
                    } else {
                        mostrarDialogoExito(
                                "Solicitud recibida",
                                "Si el correo está registrado, recibirás un enlace de recuperación."
                        );
                    }
                });
    }

    private void mostrarDialogoRegistroExitoso() {
        new AlertDialog.Builder(requireContext())
                .setTitle("¡Registro exitoso!")
                .setMessage("Tu cuenta ha sido creada y está pendiente de aprobación por un administrador. " +
                        "Recibirás una notificación cuando sea aprobada.")
                .setPositiveButton("Entendido", (dialog, which) -> {
                    isLoginMode = true;
                    updateUI();
                    resetButton();
                    clearFields();
                })
                .setCancelable(false)
                .show();
    }

    private void mostrarDialogoExito(String titulo, String mensaje) {
        new AlertDialog.Builder(requireContext())
                .setTitle(titulo)
                .setMessage(mensaje)
                .setPositiveButton("Entendido", null)
                .show();
    }

    private void clearErrors() {
        layoutEmail.setError(null);
        layoutPassword.setError(null);
        layoutConfirmPassword.setError(null);
        layoutNombre.setError(null);
    }

    private void resetButton() {
        btnLogin.setEnabled(true);
        btnLogin.setText(isLoginMode ? "Iniciar sesión" : "Registrarse");
    }

    private void clearFields() {
        inputEmail.setText("");
        inputPassword.setText("");
        inputConfirmPassword.setText("");
        inputNombre.setText("");
    }

    private void reiniciarMainActivity() {
        Intent intent = new Intent(getActivity(), MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);

        if (getActivity() != null) {
            getActivity().finish();
        }
    }
}