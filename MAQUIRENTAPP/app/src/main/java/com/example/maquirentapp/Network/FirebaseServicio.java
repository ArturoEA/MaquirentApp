package com.example.maquirentapp.Network;

import android.net.Uri;

import com.example.maquirentapp.Model.Accesorio;
import com.example.maquirentapp.Model.AlquilerDia;
import com.example.maquirentapp.Model.AlquilerMensual;
import com.example.maquirentapp.Model.DetalleMes;
import com.example.maquirentapp.Model.FotoEquipo;
import com.example.maquirentapp.Model.FiltroCategoria;
import com.example.maquirentapp.Model.GrupoElectrogeno;
import com.example.maquirentapp.Model.InfoPlaca;
import com.example.maquirentapp.Model.Ingreso;
import com.example.maquirentapp.Model.Plano;
import com.example.maquirentapp.Model.Tarea;
import com.example.maquirentapp.Model.Usuario;
import com.example.maquirentapp.Repository.AlquilerRepository;
import com.example.maquirentapp.Repository.GeneradorRepository;
import com.example.maquirentapp.Repository.TareaRepository;
import com.example.maquirentapp.Repository.UsuarioRepository;

import java.util.List;
import java.util.Map;

/**
 * Servicio legacy unificado bajo el patrón Strict Repository.
 * Se eliminaron las 32 interfaces de callbacks redundantes reemplazándolas por una única
 * interfaz genérica Callback<T>.
 *
 * @deprecated Utilizar los repositorios específicos del paquete {@link com.example.maquirentapp.Repository}.
 */
@Deprecated
public class FirebaseServicio {

    /**
     * Interfaz genérica única para manejar respuestas de operaciones Firestore / Storage.
     * @param <T> Tipo del dato retornado en caso de éxito.
     */
    public interface Callback<T> {
        void onSuccess(T result);
        void onError(Exception e);
    }

    private final GeneradorRepository generadorRepository;
    private final AlquilerRepository alquilerRepository;
    private final TareaRepository tareaRepository;
    private final UsuarioRepository usuarioRepository;

    public FirebaseServicio() {
        this.generadorRepository = new GeneradorRepository();
        this.alquilerRepository = new AlquilerRepository();
        this.tareaRepository = new TareaRepository();
        this.usuarioRepository = new UsuarioRepository();
    }

    private <T> com.example.maquirentapp.Repository.Callback<T> adapt(Callback<T> cb) {
        return new com.example.maquirentapp.Repository.Callback<T>() {
            @Override
            public void onSuccess(T result) {
                if (cb != null) cb.onSuccess(result);
            }

            @Override
            public void onError(Exception e) {
                if (cb != null) cb.onError(e);
            }
        };
    }

    // ==========================================
    // 1. GRUPOS ELECTRÓGENOS Y EQUIPOS
    // ==========================================

    public void getGruposElectrogenos(boolean incluirEliminados, Callback<List<GrupoElectrogeno>> callback) {
        generadorRepository.getGruposElectrogenos(incluirEliminados, adapt(callback));
    }

    public void getGruposParaSpinner(Callback<List<GrupoElectrogeno>> callback) {
        generadorRepository.getGruposParaSpinner(adapt(callback));
    }

    public void getGrupoPorId(String idGrupo, Callback<GrupoElectrogeno> callback) {
        generadorRepository.getGrupoPorId(idGrupo, adapt(callback));
    }

    public void crearGrupoConImagen(String codigo, Uri uri, Callback<GrupoElectrogeno> callback) {
        generadorRepository.crearGrupoConImagen(codigo, uri, adapt(callback));
    }

    public void crearGrupoConImagenBytes(String codigo, byte[] data, Callback<GrupoElectrogeno> callback) {
        generadorRepository.crearGrupoConImagenBytes(codigo, data, adapt(callback));
    }

    public void actualizarCodigoGrupo(String grupoId, String nuevoCodigo, Callback<Void> callback) {
        generadorRepository.actualizarCodigoGrupo(grupoId, nuevoCodigo, adapt(callback));
    }

    public void actualizarOrdenGrupos(List<GrupoElectrogeno> lista, Callback<Void> callback) {
        generadorRepository.actualizarOrdenGrupos(lista, adapt(callback));
    }

    public void eliminarGrupoSuave(String grupoId, Map<String, Object> updates, Callback<Void> callback) {
        generadorRepository.eliminarGrupoSuave(grupoId, updates, adapt(callback));
    }

    // ==========================================
    // 2. ALQUILERES MENSUALES Y DETALLES
    // ==========================================

    public void getAlquileresMensuales(Callback<List<AlquilerMensual>> callback) {
        alquilerRepository.getAlquileresMensuales(adapt(callback));
    }

    public void getAlquilerMensualPorId(String id, Callback<AlquilerMensual> callback) {
        alquilerRepository.getAlquilerMensualPorId(id, adapt(callback));
    }

    public void crearAlquilerMensual(AlquilerMensual alq, Callback<AlquilerMensual> callback) {
        alquilerRepository.crearAlquilerMensual(alq, adapt(callback));
    }

    public void actualizarAlquilerMensual(AlquilerMensual alq, Callback<Void> callback) {
        alquilerRepository.actualizarAlquilerMensual(alq, adapt(callback));
    }

    public void eliminarAlquilerMensual(String id, Callback<Void> callback) {
        alquilerRepository.eliminarAlquilerMensual(id, adapt(callback));
    }

    public void getDetallesMesPorAlquiler(String idAlquiler, Callback<List<DetalleMes>> callback) {
        alquilerRepository.getDetallesMesPorAlquiler(idAlquiler, adapt(callback));
    }

    public void crearDetalleMes(DetalleMes detalle, Callback<DetalleMes> callback) {
        alquilerRepository.crearDetalleMes(detalle, adapt(callback));
    }

    public void actualizarDetalleMes(DetalleMes detalle, Callback<Void> callback) {
        alquilerRepository.actualizarDetalleMes(detalle, adapt(callback));
    }

    public void eliminarDetalleMes(String id, Callback<Void> callback) {
        alquilerRepository.eliminarDetalleMes(id, adapt(callback));
    }

    public void solicitarCodigoFinalizacion(String alquilerId, String nombreCliente, Callback<Void> callback) {
        alquilerRepository.solicitarCodigoFinalizacion(alquilerId, nombreCliente, adapt(callback));
    }

    public void confirmarFinalizacion(String alquilerId, String codigo, double horometroFinal, String fechaFinal, Callback<Void> callback) {
        alquilerRepository.confirmarFinalizacion(alquilerId, codigo, horometroFinal, fechaFinal, adapt(callback));
    }

    // ==========================================
    // 3. ALQUILERES DIARIOS
    // ==========================================

    public void getAlquileresDiarios(Callback<List<AlquilerDia>> callback) {
        alquilerRepository.getAlquileresDiarios(adapt(callback));
    }

    public void getAlquilerDiaPorId(String id, Callback<AlquilerDia> callback) {
        alquilerRepository.getAlquilerDiaPorId(id, adapt(callback));
    }

    public void crearAlquilerDia(AlquilerDia alq, Callback<AlquilerDia> callback) {
        alquilerRepository.crearAlquilerDia(alq, adapt(callback));
    }

    public void actualizarAlquilerDia(AlquilerDia alq, Callback<Void> callback) {
        alquilerRepository.actualizarAlquilerDia(alq, adapt(callback));
    }

    public void finalizarAlquilerDiario(AlquilerDia alq, Callback<Void> callback) {
        alquilerRepository.finalizarAlquilerDiario(alq, adapt(callback));
    }

    // ==========================================
    // 4. ACCESORIOS E INGRESOS
    // ==========================================

    public void getAccesorios(String tipo, Callback<List<Accesorio>> callback) {
        alquilerRepository.getAccesorios(tipo, adapt(callback));
    }

    public void getAccesorioPorId(String id, Callback<Accesorio> callback) {
        alquilerRepository.getAccesorioPorId(id, adapt(callback));
    }

    public void registrarIngreso(Ingreso ingreso, Callback<String> callback) {
        alquilerRepository.registrarIngreso(ingreso, adapt(callback));
    }

    // ==========================================
    // 5. PLANOS Y GALERÍA
    // ==========================================

    public void getPlanosVoltaje(Callback<List<Plano>> callback) {
        generadorRepository.getPlanosVoltaje(adapt(callback));
    }

    public void subirPlanoBytes(byte[] data, Callback<Void> callback) {
        generadorRepository.subirPlanoBytes(data, adapt(callback));
    }

    public void eliminarPlano(Plano plano, Callback<Void> callback) {
        generadorRepository.eliminarPlano(plano, adapt(callback));
    }

    // ==========================================
    // 6. TAREAS
    // ==========================================

    public void getTareas(Callback<List<Tarea>> callback) {
        tareaRepository.getTareas(adapt(callback));
    }

    public void crearTarea(String titulo, Callback<Void> callback) {
        tareaRepository.crearTarea(titulo, adapt(callback));
    }

    public void completarTarea(String idTarea, List<String> participantes, Callback<Void> callback) {
        tareaRepository.completarTarea(idTarea, participantes, adapt(callback));
    }

    public void eliminarTarea(String idTarea, Callback<Void> callback) {
        tareaRepository.eliminarTarea(idTarea, adapt(callback));
    }

    // ==========================================
    // 7. USUARIOS
    // ==========================================

    public void verificarSiEsAdmin(Callback<Boolean> callback) {
        usuarioRepository.verificarSiEsAdmin(adapt(callback));
    }

    public void getUsuariosActivos(Callback<List<Usuario>> callback) {
        usuarioRepository.getUsuariosActivos(adapt(callback));
    }

    public void guardarFCMToken(String token) {
        usuarioRepository.guardarFCMToken(token);
    }
}