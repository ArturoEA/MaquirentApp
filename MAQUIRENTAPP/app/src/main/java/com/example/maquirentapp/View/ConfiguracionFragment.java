package com.example.maquirentapp.View;

import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.swiperefreshlayout.widget.CircularProgressDrawable;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.maquirentapp.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class ConfiguracionFragment extends Fragment {
    private TextView tvNombreUsuario;
    private ImageView imgFotoPerfil;

    public ConfiguracionFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_configuracion, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Cargar datos del usuario
        cargarDatosUsuario(view);

        // 2. Configurar navegación de items
        configurarItems(view);

        // 3. Verificar permisos y ocultar si es empleado
        verificarPermisosUsuario(view);
    }

    private void verificarPermisosUsuario(View view) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        FirebaseFirestore.getInstance().collection("usuarios")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String tipo = documentSnapshot.getString("rol");

                        if ("empleado".equals(tipo)) {
                            ocultarOpcionesAdmin(view);
                        }
                    }
                });
    }

    private void ocultarOpcionesAdmin(View view) {
        // Ocultar Títulos (Headers)
        if (view.findViewById(R.id.tvHeaderFinanzas) != null)
            view.findViewById(R.id.tvHeaderFinanzas).setVisibility(View.GONE);

        if (view.findViewById(R.id.tvHeaderAccesorios) != null)
            view.findViewById(R.id.tvHeaderAccesorios).setVisibility(View.GONE);

        if (view.findViewById(R.id.tvHeaderGrupos) != null)
            view.findViewById(R.id.tvHeaderGrupos).setVisibility(View.GONE);

        if (view.findViewById(R.id.tvHeaderUsuarios) != null)
            view.findViewById(R.id.tvHeaderUsuarios).setVisibility(View.GONE);

        // Ocultar Items de Menú
        view.findViewById(R.id.item_historial).setVisibility(View.GONE);
        view.findViewById(R.id.item_accesorios_diario).setVisibility(View.GONE);
        view.findViewById(R.id.item_accesorios_mensual).setVisibility(View.GONE);
        view.findViewById(R.id.item_mantenimientos).setVisibility(View.GONE);
        view.findViewById(R.id.item_lista_grupos).setVisibility(View.GONE);
        view.findViewById(R.id.item_gestionar_usuarios).setVisibility(View.GONE);
    }

    private void cargarDatosUsuario(View view) {
        TextView nombreUsuario = view.findViewById(R.id.nombreUsuario);
        imgFotoPerfil = view.findViewById(R.id.imgFotoPerfil);
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            String userId = user.getUid();
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            DocumentReference docRef = db.collection("usuarios").document(userId);

            docRef.get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    String nombre = documentSnapshot.getString("nombre");
                    if (nombre != null && !nombre.isEmpty()) {
                        nombreUsuario.setText(nombre);
                    } else {
                        nombreUsuario.setText("Usuario sin nombre");
                    }

                    String fotoPerfil = "";
                    try {
                        Object fotoObj = documentSnapshot.get("fotoPerfil");
                        if (fotoObj instanceof String) {
                            fotoPerfil = (String) fotoObj;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    if (fotoPerfil != null && !fotoPerfil.isEmpty()) {
                        CircularProgressDrawable spinner = new CircularProgressDrawable(this.getContext());
                        spinner.setStrokeWidth(5f);
                        spinner.setCenterRadius(30f);
                        spinner.setColorSchemeColors(Color.WHITE);
                        spinner.start();

                        Glide.with(this)
                                .load(fotoPerfil)
                                .placeholder(spinner)
                                .error(R.drawable.ico_voltaje_blanco)
                                .centerCrop()
                                .circleCrop()
                                .into(imgFotoPerfil);
                    }
                } else {
                    nombreUsuario.setText("Usuario no encontrado");
                }
            }).addOnFailureListener(e -> {
                nombreUsuario.setText("Error al cargar usuario");
            });
        } else {
            nombreUsuario.setText("No hay sesión activa");
        }
    }

    private void configurarItems(View view) {
        // Item Historial
        View itemHistorial = view.findViewById(R.id.item_historial);
        ((TextView) itemHistorial.findViewById(R.id.text_item_configuracion)).setText("Historial de ingresos");
        ((ImageView) itemHistorial.findViewById(R.id.icon_item_configuracion))
                .setImageResource(R.drawable.icon_blanco_historial_ingresos);

        // Item Accesorios Diario
        View itemAccesoriosDiario = view.findViewById(R.id.item_accesorios_diario);
        ((TextView) itemAccesoriosDiario.findViewById(R.id.text_item_configuracion))
                .setText("Accesorios alquiler diario");
        ((ImageView) itemAccesoriosDiario.findViewById(R.id.icon_item_configuracion))
                .setImageResource(R.drawable.icon_blanco_accesorios_diario);

        // Item Accesorios Mensual
        View itemAccesoriosMensual = view.findViewById(R.id.item_accesorios_mensual);
        ((TextView) itemAccesoriosMensual.findViewById(R.id.text_item_configuracion))
                .setText("Accesorios alquiler mensual");
        ((ImageView) itemAccesoriosMensual.findViewById(R.id.icon_item_configuracion))
                .setImageResource(R.drawable.icon_blanco_accesorios_mensual);

        // Item Mantenimientos
        View itemMantenimientos = view.findViewById(R.id.item_mantenimientos);
        ((TextView) itemMantenimientos.findViewById(R.id.text_item_configuracion))
                .setText("Mantenimientos");
        ((ImageView) itemMantenimientos.findViewById(R.id.icon_item_configuracion))
                .setImageResource(R.drawable.icon_blanco_mantenimientos);

        // Item Lista de Grupos
        View itemListaGrupos = view.findViewById(R.id.item_lista_grupos);
        ((TextView) itemListaGrupos.findViewById(R.id.text_item_configuracion))
                .setText("Lista de grupos electrógenos");
        ((ImageView) itemListaGrupos.findViewById(R.id.icon_item_configuracion))
                .setImageResource(R.drawable.icon_generador);
        ((ImageView) itemListaGrupos.findViewById(R.id.icon_item_configuracion))
                .setColorFilter(ContextCompat.getColor(requireContext(), R.color.black));

        // Item Gestionar Usuarios
        View itemGestionarUsuarios = view.findViewById(R.id.item_gestionar_usuarios);
        ((TextView) itemGestionarUsuarios.findViewById(R.id.text_item_configuracion))
                .setText("Gestionar Usuarios");
        ((ImageView) itemGestionarUsuarios.findViewById(R.id.icon_item_configuracion))
                .setImageResource(R.drawable.icon_blanco_gestionar_usuarios);

        LinearLayout itemPerfil = view.findViewById(R.id.itemPerfil);

        // Click listener para secciones
        itemPerfil.setOnClickListener(v -> Navigation.findNavController(v).navigate(R.id.action_configuracion_to_perfil));
        itemHistorial.setOnClickListener(v -> Navigation.findNavController(v).navigate(R.id.action_configuracion_to_historial_ingresos));
        itemAccesoriosDiario.setOnClickListener(v -> Navigation.findNavController(v).navigate(R.id.action_configuracion_to_accesorios_alquiler_diario));
        itemAccesoriosMensual.setOnClickListener(v -> Navigation.findNavController(v).navigate(R.id.action_configuracion_to_accesorios_alquiler_mensual));
        itemMantenimientos.setOnClickListener(v -> Navigation.findNavController(v).navigate(R.id.action_configuracion_to_mantenimientos_configuracion));
        itemListaGrupos.setOnClickListener(v -> Navigation.findNavController(v).navigate(R.id.action_configuracion_to_lista_grupos_electrogenos));
        itemGestionarUsuarios.setOnClickListener(v -> Navigation.findNavController(v).navigate(R.id.action_configuracion_to_gestionar_usuarios));
    }
}