package com.example.maquirentapp.View;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.example.maquirentapp.Network.FirebaseServicio;
import com.example.maquirentapp.Model.Usuario;
import com.example.maquirentapp.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class AuthFragment extends Fragment {
    private TextInputEditText inputEmail, inputPassword, inputNombre;
    private MaterialButton btnLogin, btnToggleMode;
    private View layoutNombre;
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
        inputEmail = view.findViewById(R.id.inputEmail);
        inputPassword = view.findViewById(R.id.inputPassword);
        inputNombre = view.findViewById(R.id.inputNombre);
        layoutNombre = view.findViewById(R.id.layoutNombre);
        btnLogin = view.findViewById(R.id.btnLogin);
        btnToggleMode = view.findViewById(R.id.btnToggleMode);

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
        });
    }

    private void updateUI() {
        if (isLoginMode) {
            layoutNombre.setVisibility(View.GONE);
            btnLogin.setText("Iniciar Sesión");
            btnToggleMode.setText("¿No tienes cuenta? Registrarse");
        } else {
            layoutNombre.setVisibility(View.VISIBLE);
            btnLogin.setText("Registrarse");
            btnToggleMode.setText("¿Ya tienes cuenta? Iniciar Sesión");
        }
    }

    private void iniciarSesion() {
        String email = inputEmail.getText().toString().trim();
        String password = inputPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(getContext(), "Completa todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        btnLogin.setEnabled(false);
        btnLogin.setText("Iniciando...");

        firebaseServicio.iniciarSesion(email, password, new FirebaseServicio.OnAuthListener() {
            @Override
            public void onLoginExitoso(Usuario usuario) {
                // Pasar datos al MainActivity y navegar
                Bundle bundle = new Bundle();
                bundle.putString("usuario_rol", usuario.getRol());
                bundle.putString("usuario_uid", usuario.getUid());
                bundle.putString("usuario_nombre", usuario.getNombre());

                if ("admin".equals(usuario.getRol())) {
                    Navigation.findNavController(getView()).navigate(R.id.main, bundle);
//                    Navigation.findNavController(getView()).navigate(R.id.action_auth_to_home, bundle);
                } else {
                    Navigation.findNavController(getView()).navigate(R.id.action_auth_to_tareas, bundle);
                }
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
                Toast.makeText(getContext(),
                        "Error: " + e.getMessage(),
                        Toast.LENGTH_LONG).show();
                resetButton();
            }
        });
    }

    private void registrarUsuario() {
        String email = inputEmail.getText().toString().trim();
        String password = inputPassword.getText().toString().trim();
        String nombre = inputNombre.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty() || nombre.isEmpty()) {
            Toast.makeText(getContext(), "Completa todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(getContext(), "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show();
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
                Toast.makeText(getContext(),
                        "Registro exitoso. Tu cuenta está pendiente de aprobación",
                        Toast.LENGTH_LONG).show();

                isLoginMode = true;
                updateUI();
                resetButton();
                clearFields();
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
                Toast.makeText(getContext(),
                        "Error: " + e.getMessage(),
                        Toast.LENGTH_LONG).show();
                resetButton();
            }
        });
    }

    private void resetButton() {
        btnLogin.setEnabled(true);
        btnLogin.setText(isLoginMode ? "Iniciar Sesión" : "Registrarse");
    }

    private void clearFields() {
        inputEmail.setText("");
        inputPassword.setText("");
        inputNombre.setText("");
    }
}