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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {
    private LinearLayout navHome, navRent, navConfiguracion;
    private TextView navHomeText, navRentText, navConfiguracionText;
    private TextView headerTitle;
    private ImageView headerIcon;
    private NestedScrollView contentScrollView;
    private ScrollStateViewModel scrollViewModel;
    private FirebaseServicio firebaseServicio;

    private NavController navController;
    // Datos del usuario
    private String userRole;
    private String userUid;
    private String userName;

    // Para el indicador animado
    private View currentSelectedIndicator;
    private int currentSelectedIndex = 0; // 0: home, 1: cge, 2: configuracion
    private int previousDestinationId = R.id.homeFragment;
    // Keys para identificar cada fragment
    private static final String HOME_FRAGMENT_KEY = "home_fragment";
    private static final String CGE_FRAGMENT_KEY = "cge_fragment";
    private static final String CONFIGURACION_FRAGMENT_KEY = "configuracion_fragment";
    private static final String NUEVO_ALQUILER_DIA_KEY = "nuevo_alquiler_dia_fragment";
    private static final String PLANOS_CAMBIO_VOLTAJE_KEY = "planos_cambio_voltaje_fragment";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        firebaseServicio = new FirebaseServicio();
        scrollViewModel = new ViewModelProvider(this).get(ScrollStateViewModel.class);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        // Configurar window insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        testFirestoreConnection();

        // Inicializar vistas
        initViews();

        // Configurar Navigation Component
        setupNavigation();
        verificarAutenticacion();
        // Configurar estado inicial
        //setupInitialState();
    }
    private void testFirestoreConnection() {
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
    }
    private void verificarAutenticacion() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            mostrarAuthFragment();
            return;
        }

        firebaseServicio.verificarEstadoUsuario(new FirebaseServicio.OnAuthListener() {
            @Override
            public void onLoginExitoso(Usuario usuario) {
                userRole = usuario.getRol();
                userUid = usuario.getUid();
                userName = usuario.getNombre();

                Log.d("MainActivity", "Usuario autenticado: " + userName + " - Rol: " + userRole);
                configurarUISegunRol();
                navegarSegunRol();
            }

            @Override
            public void onRegistroExitoso(Usuario usuario) {
                // No se usa aquí, se usa en FirebaseServicio
            }

            @Override
            public void onUsuarioPendiente() {
                Log.w("MainActivity", "Usuario pendiente de aprobación");
                FirebaseAuth.getInstance().signOut();
                mostrarAuthFragment();
            }

            @Override
            public void onUsuarioInactivo() {
                Log.w("MainActivity", "Usuario inactivo");
                FirebaseAuth.getInstance().signOut();
                mostrarAuthFragment();
            }

            @Override
            public void onError(Exception e) {
                Log.e("MainActivity", "Error al verificar usuario", e);
                FirebaseAuth.getInstance().signOut();
                mostrarAuthFragment();
            }
        });
    }
    private void mostrarAuthFragment() {
        configurarUIParaAuth();

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment != null) {
            NavController navController = navHostFragment.getNavController();
            navController.navigate(R.id.authFragment);
        }
    }
    private void configurarUIParaAuth() {
        findViewById(R.id.menuFlotante).setVisibility(View.GONE);
        findViewById(R.id.headerLayout).setVisibility(View.GONE);
        findViewById(R.id.btn_back).setVisibility(View.GONE);
    }
    private void configurarUISegunRol() {
        findViewById(R.id.menuFlotante).setVisibility(View.VISIBLE);
        findViewById(R.id.headerLayout).setVisibility(View.VISIBLE);
        findViewById(R.id.btn_back).setVisibility(View.VISIBLE);

        if ("empleado".equals(userRole)) {
            setupEmpleadoUI();
        } else {
            setupAdminUI();
        }
    }
//    private void inicializarApp() {
//        // Mostrar la navegación y header
//        findViewById(R.id.menuFlotante).setVisibility(View.VISIBLE);
//        findViewById(R.id.headerLayout).setVisibility(View.VISIBLE);
//
//        // Inicializar vistas
//        initViews();
//        // Configurar Navigation Component
//        setupNavigation();
//        // Configurar estado inicial
//        setupInitialState();
//    }
    private void navegarSegunRol() {
        if (navController != null) {
            NavOptions navOptions = new NavOptions.Builder()
                    .setPopUpTo(R.id.authFragment, true)
                    .build();

            if ("empleado".equals(userRole)) {
                navController.navigate(R.id.tareasFragment, null, navOptions);
            } else {
                navController.navigate(R.id.homeFragment, null, navOptions);
            }
        }
    }
    private void setupEmpleadoUI() {
        navRent.setVisibility(View.GONE);
        navHomeText.setText("Tareas");

        // Actualizar el listener para ir a tareas
//        navHome.setOnClickListener(v -> {
//            NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
//                    .findFragmentById(R.id.nav_host_fragment);
//            if (navHostFragment != null) {
//                NavController navController = navHostFragment.getNavController();
//                navigateWithAnimation(navController, R.id.tareasFragment, 0);
//            }
//        });
    }
    private void setupAdminUI() {
        navRent.setVisibility(View.VISIBLE);
        navHomeText.setText("Inicio");
    }
    private void initViews() {
        headerTitle = findViewById(R.id.header_title);
        headerIcon = findViewById(R.id.header_icon);
        contentScrollView = findViewById(R.id.content_scroll_view);

        navHome = findViewById(R.id.nav_home);
        navRent = findViewById(R.id.nav_rent);
        navConfiguracion = findViewById(R.id.nav_configuracion);

        navHomeText = findViewById(R.id.nav_home_text);
        navRentText = findViewById(R.id.nav_rent_text);
        navConfiguracionText = findViewById(R.id.nav_configuracion_text);

        // Configurar estado inicial
        currentSelectedIndicator = navHome;
        updateNavigationUI(0);
    }

    private void setupInitialState() {
        currentSelectedIndicator = navHome;
        updateNavigationUI(0);
    }

    private void setupNavigation() {
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);

        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();

            // Listener para cambios de destino
            navController.addOnDestinationChangedListener((ctrl, dest, args) -> {
                saveScrollPositionForDestination(previousDestinationId);
                if (dest.getId() == R.id.authFragment) {
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
                } else if (dest.getId() == R.id.configuracionFragment) {
                    setHeaderTitle("Configuración");
                    setHeaderIcon(R.drawable.icon_configuracion_blanco);
                    updateNavigationUI(2);
                    restoreScrollPosition(CONFIGURACION_FRAGMENT_KEY);
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

            navConfiguracion.setOnClickListener(v -> {
                navigateWithAnimation(navController, R.id.configuracionFragment, 2);
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
    public String getUserRole() { return userRole; }
    public String getUserUid() { return userUid; }
    public String getUserName() { return userName; }

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
            contentScrollView.post(() -> contentScrollView.scrollTo(0, savedPosition));
        }
    }

    private String getFragmentKeyByDestinationId(int destinationId) {
        if (destinationId == R.id.homeFragment) {
            return HOME_FRAGMENT_KEY;
        } else if (destinationId == R.id.cgeFragment) {
            return CGE_FRAGMENT_KEY;
        } else if (destinationId == R.id.configuracionFragment) {
            return CONFIGURACION_FRAGMENT_KEY;
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
        navConfiguracionText.setVisibility(View.GONE);

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
                return navConfiguracion;
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
                return navConfiguracionText;
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