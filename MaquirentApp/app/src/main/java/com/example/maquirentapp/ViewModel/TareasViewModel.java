package com.example.maquirentapp.ViewModel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.maquirentapp.Model.ResponsableAsignado;
import com.example.maquirentapp.Model.Tarea;
import com.example.maquirentapp.Model.Usuario;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TareasViewModel extends ViewModel {

    private static final String COLLECTION_TAREAS = "tareas";
    private static final String COLLECTION_USUARIOS = "usuarios";

    private final MutableLiveData<List<Tarea>> tareas = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Usuario>> usuariosActivos = new MutableLiveData<>(new ArrayList<>());
    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseAuth.AuthStateListener authStateListener;
    private ListenerRegistration listenerRegistration;
    private ListenerRegistration listenerUsuarios;

    public TareasViewModel() {
        authStateListener = firebaseAuth -> {
            FirebaseUser currentUser = firebaseAuth.getCurrentUser();
            iniciarEscucha(currentUser);
            iniciarEscuchaUsuarios(currentUser);
        };
        auth.addAuthStateListener(authStateListener);
        FirebaseUser currentUser = auth.getCurrentUser();
        iniciarEscucha(currentUser);
        iniciarEscuchaUsuarios(currentUser);
    }

    public LiveData<List<Tarea>> obtenerTareas() {
        return tareas;
    }

    public LiveData<List<Usuario>> obtenerUsuariosActivos() {
        return usuariosActivos;
    }

    public void agregarTarea(String titulo) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            return;
        }

        Tarea nuevaTarea = new Tarea();
        nuevaTarea.setTitulo(titulo);
        nuevaTarea.setCompletada(false);
        nuevaTarea.setFechaCreacion(new Date());
        nuevaTarea.setCreadoPor(currentUser.getUid());
        String nombreCreador = currentUser.getDisplayName() != null
                ? currentUser.getDisplayName()
                : currentUser.getEmail();
        nuevaTarea.setNombreCreador(nombreCreador);
        nuevaTarea.setResponsables(new ArrayList<>());

        firestore.collection(COLLECTION_TAREAS)
                .add(nuevaTarea);
    }

    public void marcarTareaComoCompletada(Tarea tarea, List<Usuario> responsablesSeleccionados) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null || tarea.getId() == null) {
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("completada", true);
        updates.put("fechaCompletada", new Date());
        updates.put("completadaPor", currentUser.getUid());
        String nombreCompletador = currentUser.getDisplayName() != null
                ? currentUser.getDisplayName()
                : currentUser.getEmail();
        updates.put("nombreCompletador", nombreCompletador);

        List<ResponsableAsignado> responsablesAsignados = new ArrayList<>();
        if (responsablesSeleccionados != null) {
            for (Usuario usuario : responsablesSeleccionados) {
                ResponsableAsignado responsable = new ResponsableAsignado(
                        usuario.getUid(),
                        usuario.getNombre(),
                        usuario.getFotoUrl()
                );
                responsablesAsignados.add(responsable);
            }
        }
        updates.put("responsables", responsablesAsignados);

        firestore.collection(COLLECTION_TAREAS)
                .document(tarea.getId())
                .update(updates);
    }

    private void iniciarEscucha(FirebaseUser currentUser) {
        if (listenerRegistration != null) {
            listenerRegistration.remove();
            listenerRegistration = null;
        }

        if (currentUser == null) {
            tareas.setValue(new ArrayList<>());
            return;
        }

        listenerRegistration = firestore.collection(COLLECTION_TAREAS)
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) {
                        return;
                    }

                    List<Tarea> listaTareas = new ArrayList<>();
                    for (DocumentSnapshot document : value.getDocuments()) {
                        Tarea tarea = document.toObject(Tarea.class);
                        if (tarea != null) {
                            Object fechaCreacionRaw = document.get("fechaCreacion");
                            if (tarea.getFechaCreacion() == null && fechaCreacionRaw != null) {
                                tarea.setFechaCreacionRaw(fechaCreacionRaw);
                            }

                            Object fechaCompletadaRaw = document.get("fechaCompletada");
                            if (tarea.getFechaCompletada() == null && fechaCompletadaRaw != null) {
                                tarea.setFechaCompletadaRaw(fechaCompletadaRaw);
                            }

                            tarea.setId(document.getId());
                            tarea.setResponsables(tarea.getResponsables());
                            listaTareas.add(tarea);
                        }
                    }
                    Collections.sort(listaTareas, (t1, t2) -> {
                        Date fecha2 = t2.getFechaCreacion();
                        Date fecha1 = t1.getFechaCreacion();
                        long tiempo2 = fecha2 != null ? fecha2.getTime() : 0L;
                        long tiempo1 = fecha1 != null ? fecha1.getTime() : 0L;
                        return Long.compare(tiempo2, tiempo1);
                    });
                    tareas.setValue(listaTareas);
                });
    }

    private void iniciarEscuchaUsuarios(FirebaseUser currentUser) {
        if (listenerUsuarios != null) {
            listenerUsuarios.remove();
            listenerUsuarios = null;
        }

        if (currentUser == null) {
            usuariosActivos.setValue(new ArrayList<>());
            return;
        }

        listenerUsuarios = firestore.collection(COLLECTION_USUARIOS)
                .whereEqualTo("estado", "activo")
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) {
                        return;
                    }

                    List<Usuario> listaUsuarios = new ArrayList<>();
                    for (DocumentSnapshot document : value.getDocuments()) {
                        Usuario usuario = document.toObject(Usuario.class);
                        if (usuario != null) {
                            usuario.setUid(document.getId());
                            listaUsuarios.add(usuario);
                        }
                    }
                    usuariosActivos.setValue(listaUsuarios);
                });
    }

    @Override
    protected void onCleared() {
        if (listenerRegistration != null) {
            listenerRegistration.remove();
        }
        if (listenerUsuarios != null) {
            listenerUsuarios.remove();
        }
        auth.removeAuthStateListener(authStateListener);
        super.onCleared();
    }
}
