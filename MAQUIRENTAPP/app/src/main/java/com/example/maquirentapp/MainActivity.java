package com.example.maquirentapp;

import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.NestedScrollView;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;

import com.example.maquirentapp.Model.Usuario;
import com.example.maquirentapp.Network.FirebaseServicio;
import com.example.maquirentapp.ViewModel.ScrollStateViewModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {
    private LinearLayout navHome, navRent, navPerfil;
    private TextView navHomeText, navRentText, navPerfilText;
    private TextView headerTitle;
    private ImageView headerIcon;
    private NestedScrollView contentScrollView;
    private ScrollStateViewModel scrollViewModel;
    private FirebaseServicio firebaseServicio;
    // Datos del usuario
    private String userRole;
    private String userUid;
    private String userName;

    // Para el indicador animado
    private View currentSelectedIndicator;
    private int currentSelectedIndex = 0; // 0: home, 1: cge, 2: perfil
    private int previousDestinationId = R.id.homeFragment;
    // Keys para identificar cada fragment
    private static final String HOME_FRAGMENT_KEY = "home_fragment";
    private static final String CGE_FRAGMENT_KEY = "cge_fragment";
    private static final String PERFIL_FRAGMENT_KEY = "perfil_fragment";
    private static final String NUEVO_ALQUILER_DIA_KEY = "nuevo_alquiler_dia_fragment";
    private static final String PLANOS_CAMBIO_VOLTAJE_KEY = "planos_cambio_voltaje_fragment";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        firebaseServicio = new FirebaseServicio();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("gruposElectrogenos")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d("Firebase", "Conexión exitosa! Documentos: " + task.getResult().size());
                    } else {
                        Log.e("Firebase", "Error de conexión", task.getException());
                    }
                });

        scrollViewModel = new ViewModelProvider(this).get(ScrollStateViewModel.class);

        // Configurar window insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        verificarAutenticacion();

        // Inicializar vistas
        initViews();

        // Configurar Navigation Component
        setupNavigation();

        // Configurar estado inicial
        setupInitialState();
    }
    private void verificarAutenticacion() {
        firebaseServicio.verificarEstadoUsuario(new FirebaseServicio.OnAuthListener() {
            @Override
            public void onLoginExitoso(Usuario usuario) {
                userRole = usuario.getRol();
                userUid = usuario.getUid();
                userName = usuario.getNombre();

                inicializarApp();
                navegarSegunRol();
            }

            @Override
            public void onRegistroExitoso(Usuario usuario) {
                // No se usa aquí
            }

            @Override
            public void onUsuarioPendiente() {
                mostrarAuthFragment();
            }

            @Override
            public void onUsuarioInactivo() {
                mostrarAuthFragment();
            }

            @Override
            public void onError(Exception e) {
                mostrarAuthFragment();
            }
        });
    }
    private void mostrarAuthFragment() {
        // Ocultar la navegación y mostrar solo el fragment de auth
        configurarUIParaAuth();

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment != null) {
            NavController navController = navHostFragment.getNavController();
            navController.navigate(R.id.authFragment);
        }
    }
    private void configurarUIParaAuth() {
        // Ocultar la navegación bottom y header
        findViewById(R.id.menuFlotante).setVisibility(View.GONE);
        findViewById(R.id.headerLayout).setVisibility(View.GONE);
    }
    private void inicializarApp() {
        // Mostrar la navegación y header
        findViewById(R.id.menuFlotante).setVisibility(View.VISIBLE);
        findViewById(R.id.headerLayout).setVisibility(View.VISIBLE);

        // Inicializar vistas
        initViews();
        // Configurar Navigation Component
        setupNavigation();
        // Configurar estado inicial
        setupInitialState();
    }
    private void navegarSegunRol() {
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment != null) {
            NavController navController = navHostFragment.getNavController();

            if ("empleado".equals(userRole)) {
                // Para empleados, ir directo a tareas y ocultar ciertas opciones
                setupEmpleadoUI();
                navController.navigate(R.id.tareasFragment);
            } else {
                // Para admins, mostrar todo
                setupAdminUI();
                navController.navigate(R.id.homeFragment);
            }
        }
    }
    private void setupEmpleadoUI() {
        // Ocultar el tab de CGE para empleados
        navRent.setVisibility(View.GONE);

        // Cambiar el ícono del home para mostrar "Tareas"
        navHomeText.setText("Tareas");
        // Actualizar el listener para ir a tareas
        navHome.setOnClickListener(v -> {
            NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.nav_host_fragment);
            if (navHostFragment != null) {
                NavController navController = navHostFragment.getNavController();
                navigateWithAnimation(navController, R.id.tareasFragment, 0);
            }
        });
    }
    private void setupAdminUI() {
        // Mostrar toda la navegación
        navRent.setVisibility(View.VISIBLE);
        navHomeText.setText("Inicio");
        // Mantener listeners originales
    }
    private void initViews() {
        headerTitle = findViewById(R.id.header_title);
        headerIcon = findViewById(R.id.header_icon);
        contentScrollView = findViewById(R.id.content_scroll_view);

        navHome = findViewById(R.id.nav_home);
        navRent = findViewById(R.id.nav_rent);
        navPerfil = findViewById(R.id.nav_perfil);

        navHomeText = findViewById(R.id.nav_home_text);
        navRentText = findViewById(R.id.nav_rent_text);
        navPerfilText = findViewById(R.id.nav_perfil_text);
    }

    private void setupInitialState() {
        currentSelectedIndicator = navHome;
        updateNavigationUI(0);
    }

    private void setupNavigation() {
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);

        if (navHostFragment != null) {
            NavController navController = navHostFragment.getNavController();

            // Listener para cambios de destino
            navController.addOnDestinationChangedListener((ctrl, dest, args) -> {
                saveScrollPositionForDestination(previousDestinationId);
                if (dest.getId() == R.id.authFragment) {
                    // No mostrar header ni navegación en auth
                    configurarUIParaAuth();
                } else if (dest.getId() == R.id.tareasFragment) {
                    setHeaderTitle("Lista de Tareas");
                    setHeaderIcon(R.drawable.icon_voltaje_blanco); // Necesitas este ícono
                    updateNavigationUI(0); // Para empleados será el "home"
                    restoreScrollPosition("tareas_fragment");
                } else if (dest.getId() == R.id.homeFragment) {
                    setHeaderTitle("Inicio");
                    setHeaderIcon(R.drawable.icon_home_blanco);
                    updateNavigationUI(0);
                    restoreScrollPosition(HOME_FRAGMENT_KEY);
                } else if (dest.getId() == R.id.homeFragment) {
                    setHeaderTitle("Inicio");
                    setHeaderIcon(R.drawable.icon_home_blanco);
                    updateNavigationUI(0);
                    restoreScrollPosition(HOME_FRAGMENT_KEY);
                } else if (dest.getId() == R.id.cgeFragment) {
                    setHeaderTitle("Control de grupos electrógenos");
                    setHeaderIcon(R.drawable.icon_generador);
                    updateNavigationUI(1);
                    restoreScrollPosition(CGE_FRAGMENT_KEY);
                } else if (dest.getId() == R.id.perfilFragment) {
                    setHeaderTitle("Perfil");
                    setHeaderIcon(R.drawable.icon_perfil_blanco);
                    updateNavigationUI(2);
                    restoreScrollPosition(PERFIL_FRAGMENT_KEY);
                } else if (dest.getId() == R.id.nuevoAlquilerFragment) {
                    setHeaderTitle("Nuevo alquiler por día(s)");
                    setHeaderIcon(R.drawable.icon_contrato_blanco);
                    contentScrollView.scrollTo(0, 0);
                } else if (dest.getId() == R.id.planosCambioVoltajeFragment) {
                    setHeaderTitle("Planos de cambio de voltaje");
                    setHeaderIcon(R.drawable.icon_voltaje_blanco);
                    contentScrollView.scrollTo(0, 0);
                } else if (dest.getId() == R.id.fichasTecnicasFragment) {
                    setHeaderTitle("Fichas técnicas");
                    setHeaderIcon(R.drawable.icon_ficha_tecnica_blanco);
                    contentScrollView.scrollTo(0, 0);
                } else if (dest.getId() == R.id.grupoElectrogenoFragment) {
                    String codigo = args != null
                            ? args.getString("codigo", "GEP")
                            : "GEP";
                    setHeaderTitle(codigo);
                    setHeaderIcon(R.drawable.icon_generador);
                    contentScrollView.scrollTo(0, 0);
                } else if (dest.getId() == R.id.historialAlquilerMensualFragment) {
                    String codigo = args != null
                            ? args.getString("codigo", "GEP")
                            : "GEP";
                    setHeaderIcon(R.drawable.icon_generador);
                    setHeaderTitle("Historial de alquileres\n" + codigo);
                } else if (dest.getId() == R.id.nuevoAlquilerMensualFragment) {
                    String codigo = args != null
                            ? args.getString("codigo", "GEP")
                            : "GEP";
                    setHeaderIcon(R.drawable.icon_generador);
                    setHeaderTitle("Nuevo alquiler mensual\n" + codigo);
                }

                previousDestinationId = dest.getId();
            });

            // Configurar clicks de navegación con animaciones inteligentes
            navHome.setOnClickListener(v -> {
                if ("empleado".equals(userRole)) {
                    navigateWithAnimation(navController, R.id.tareasFragment, 0);
                } else {
                    navigateWithAnimation(navController, R.id.homeFragment, 0);
                }
            });

            navRent.setOnClickListener(v -> {
                navigateWithAnimation(navController, R.id.cgeFragment, 1);
            });

            navPerfil.setOnClickListener(v -> {
                navigateWithAnimation(navController, R.id.perfilFragment, 2);
            });

            // Configurar botón de retroceso
            FloatingActionButton btnBack = findViewById(R.id.btn_back);
            btnBack.setOnClickListener(v -> {
                if (!navController.popBackStack()) {
                    finish();
                }
            });
        }
    }

    // Método público para que los fragments puedan acceder a los datos del usuario
//    public String getUserRole() { return userRole; }
//    public String getUserUid() { return userUid; }
//    public String getUserName() { return usserName; }

    private void saveScrollPositionForDestination(int destinationId) {
        if (contentScrollView != null) {
            int currentScrollY = contentScrollView.getScrollY();
            String fragmentKey = getFragmentKeyByDestinationId(destinationId);

            if (fragmentKey != null) {
                scrollViewModel.saveScrollPosition(fragmentKey, currentScrollY);
            }
        }
    }

    private void restoreScrollPosition(String fragmentKey) {
        if (contentScrollView != null) {
            int savedPosition = scrollViewModel.getScrollPosition(fragmentKey);

            // Usar post para asegurar que el contenido se ha cargado
            contentScrollView.post(() -> {
                contentScrollView.scrollTo(0, savedPosition);
            });
        }
    }

    private String getFragmentKeyByDestinationId(int destinationId) {
        if (destinationId == R.id.homeFragment) {
            return HOME_FRAGMENT_KEY;
        } else if (destinationId == R.id.cgeFragment) {
            return CGE_FRAGMENT_KEY;
        } else if (destinationId == R.id.perfilFragment) {
            return PERFIL_FRAGMENT_KEY;
        } else if (destinationId == R.id.nuevoAlquilerFragment) {
            return NUEVO_ALQUILER_DIA_KEY;
        } else if (destinationId == R.id.planosCambioVoltajeFragment) {
            return PLANOS_CAMBIO_VOLTAJE_KEY;
        } else {
            return null;
        }
    }


    private void navigateWithAnimation(NavController navController, int destinationId, int targetIndex) {
        // Solo navegar si no estamos ya en ese destino
        if (navController.getCurrentDestination() != null &&
                navController.getCurrentDestination().getId() == destinationId) {
            return;
        }

        // Crear NavOptions con animaciones personalizadas basadas en la dirección
        NavOptions.Builder navOptionsBuilder = new NavOptions.Builder();

        // Determinar la dirección de la animación basada en los índices
        if (targetIndex > currentSelectedIndex) {
            // Navegar hacia la derecha
            navOptionsBuilder
                    .setEnterAnim(R.anim.slide_in_right)
                    .setExitAnim(R.anim.slide_out_left)
                    .setPopEnterAnim(R.anim.slide_in_left)
                    .setPopExitAnim(R.anim.slide_out_right);
        } else {
            // Navegar hacia la izquierda
            navOptionsBuilder
                    .setEnterAnim(R.anim.slide_in_left)
                    .setExitAnim(R.anim.slide_out_right)
                    .setPopEnterAnim(R.anim.slide_in_right)
                    .setPopExitAnim(R.anim.slide_out_left);
        }

        NavOptions navOptions = navOptionsBuilder.build();
        navController.navigate(destinationId, null, navOptions);
    }

    private void updateNavigationUI(int selectedIndex) {
        if (selectedIndex == currentSelectedIndex) {
            return;
        }

        // Animar la transición del indicador
        animateIndicatorTransition(selectedIndex);

        // Actualizar textos de navegación
        updateNavigationText(selectedIndex);

        currentSelectedIndex = selectedIndex;
    }

    private void animateIndicatorTransition(int newIndex) {
        View newSelectedView = getNavigationViewByIndex(newIndex);

        if (currentSelectedIndicator != null && newSelectedView != null) {
            // Remover el background del elemento anterior
            currentSelectedIndicator.setBackground(null);

            // Crear y ejecutar la animación
            ObjectAnimator fadeOut = ObjectAnimator.ofFloat(currentSelectedIndicator, "alpha", 1f, 0.7f);
            fadeOut.setDuration(150);

            ObjectAnimator fadeIn = ObjectAnimator.ofFloat(newSelectedView, "alpha", 0.7f, 1f);
            fadeIn.setDuration(150);
            fadeIn.setStartDelay(150);

            fadeOut.start();
            fadeIn.start();

            // Aplicar el background al nuevo elemento
            newSelectedView.setBackgroundResource(R.drawable.selection_indicator_background);

            // Actualizar referencia
            currentSelectedIndicator = newSelectedView;
        }
    }

    private void updateNavigationText(int selectedIndex) {
        // Ocultar todos los textos primero
        navHomeText.setVisibility(View.GONE);
        navRentText.setVisibility(View.GONE);
        navPerfilText.setVisibility(View.GONE);

        // Mostrar el texto del elemento seleccionado con animación
        TextView selectedText = getNavigationTextByIndex(selectedIndex);
        if (selectedText != null) {
            selectedText.setVisibility(View.VISIBLE);
            selectedText.setAlpha(0f);
            selectedText.animate()
                    .alpha(1f)
                    .setDuration(200)
                    .setStartDelay(100)
                    .start();
        }
    }

    private View getNavigationViewByIndex(int index) {
        switch (index) {
            case 0:
                return navHome;
            case 1:
                return navRent;
            case 2:
                return navPerfil;
            default:
                return navHome;
        }
    }

    private TextView getNavigationTextByIndex(int index) {
        switch (index) {
            case 0:
                return navHomeText;
            case 1:
                return navRentText;
            case 2:
                return navPerfilText;
            default:
                return navHomeText;
        }
    }

    private void setHeaderIcon(int iconResId) {
        if (headerIcon != null) {
            headerIcon.setImageResource(iconResId);
        }
    }

    private void setHeaderTitle(String title) {
        if (headerTitle != null) {
            headerTitle.setText(title);
        }
    }
}