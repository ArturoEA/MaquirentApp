package com.example.maquirentapp.Utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.maquirentapp.Model.Mantenimiento;
import com.example.maquirentapp.Model.MantenimientoConfiguracion;
import com.example.maquirentapp.Repository.Callback;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Gestor centralizado del modo sin conexión (Offline) para Mantenimientos.
 * Permite guardar datos y fotografías localmente en el dispositivo de forma instantánea,
 * y sincronizarlos automáticamente con Firebase (Storage y Firestore) en segundo plano
 * cuando se restablezca la conexión a internet.
 */
public class MantenimientoOfflineManager {
    private static final String TAG = "MantenimientoOffline";

    private static final String DIR_FOTOS_OFFLINE = "mantenimientos_fotos_locales";
    private static final String DIR_PENDIENTES = "mantenimientos_pendientes";
    private static final String ARCHIVO_CONFIG_CACHE = "mantenimientos_config_cache.json";

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final AtomicBoolean isSyncing = new AtomicBoolean(false);

    public static File getFotosOfflineDir(Context context) {
        File dir = new File(context.getFilesDir(), DIR_FOTOS_OFFLINE);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    public static File getPendientesDir(Context context) {
        File dir = new File(context.getFilesDir(), DIR_PENDIENTES);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    /**
     * Guarda una fotografía en el almacenamiento interno de la app (local y permanente).
     * @return Ruta absoluta del archivo local guardado.
     */
    public static String guardarFotoLocal(Context context, Uri uri) {
        if (context == null || uri == null) return null;
        try {
            // Intentar primero con compresión optimizada
            byte[] data = ImageUtils.comprimirImagen(context, uri);

            if (data == null) {
                // Fallback: leer bytes directos del stream
                InputStream is = context.getContentResolver().openInputStream(uri);
                if (is != null) {
                    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                    byte[] temp = new byte[8192];
                    int read;
                    while ((read = is.read(temp)) != -1) {
                        buffer.write(temp, 0, read);
                    }
                    is.close();
                    data = buffer.toByteArray();
                }
            }

            if (data == null || data.length == 0) {
                Log.e(TAG, "No se pudieron obtener bytes de la imagen: " + uri);
                return null;
            }

            String filename = "foto_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8) + ".jpg";
            File destFile = new File(getFotosOfflineDir(context), filename);

            FileOutputStream fos = new FileOutputStream(destFile);
            fos.write(data);
            fos.flush();
            fos.close();

            Log.d(TAG, "Foto guardada localmente: " + destFile.getAbsolutePath() + " (" + data.length + " bytes)");
            return destFile.getAbsolutePath();

        } catch (Exception e) {
            Log.e(TAG, "Error al guardar foto localmente", e);
            return null;
        }
    }

    /**
     * Guarda un mantenimiento en la cola de pendientes offline en disco.
     * También lo inserta en la caché local de Firestore para disponibilidad inmediata.
     */
    public static boolean guardarMantenimientoOffline(Context context, Mantenimiento mantenimiento) {
        if (context == null || mantenimiento == null || mantenimiento.getId() == null) return false;

        try {
            JSONObject json = mantenimientoToJson(mantenimiento);
            File file = new File(getPendientesDir(context), mantenimiento.getId() + ".json");

            FileWriter writer = new FileWriter(file);
            writer.write(json.toString());
            writer.flush();
            writer.close();

            Log.d(TAG, "Mantenimiento guardado en cola offline: " + file.getAbsolutePath());

            // Registrar también en Firestore local (asíncrono, no bloqueante)
            try {
                FirebaseFirestore.getInstance()
                        .collection("mantenimientos")
                        .document(mantenimiento.getId())
                        .set(mantenimiento);
            } catch (Exception e) {
                Log.w(TAG, "No se pudo actualizar caché de Firestore directamente", e);
            }

            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error guardando mantenimiento offline", e);
            return false;
        }
    }

    /**
     * Retorna todos los mantenimientos guardados localmente pendientes de subida.
     */
    public static List<Mantenimiento> obtenerPendientes(Context context) {
        List<Mantenimiento> lista = new ArrayList<>();
        if (context == null) return lista;

        File dir = getPendientesDir(context);
        File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));

        if (files == null) return lista;

        for (File f : files) {
            try {
                BufferedReader reader = new BufferedReader(new FileReader(f));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                reader.close();

                JSONObject json = new JSONObject(sb.toString());
                Mantenimiento m = jsonToMantenimiento(json);
                if (m != null) {
                    lista.add(m);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error leyendo archivo offline: " + f.getName(), e);
            }
        }
        return lista;
    }

    /**
     * Retorna los mantenimientos pendientes filtrados por código de máquina.
     */
    public static List<Mantenimiento> obtenerPendientesPorGrupo(Context context, String codigoGrupo) {
        List<Mantenimiento> todos = obtenerPendientes(context);
        if (codigoGrupo == null || codigoGrupo.trim().isEmpty()) {
            return todos;
        }
        List<Mantenimiento> filtrados = new ArrayList<>();
        for (Mantenimiento m : todos) {
            if (codigoGrupo.equalsIgnoreCase(m.getCodigoGrupo())) {
                filtrados.add(m);
            }
        }
        return filtrados;
    }

    /**
     * Retorna un mantenimiento pendiente específico por su ID.
     */
    public static Mantenimiento obtenerPendientePorId(Context context, String id) {
        if (context == null || id == null) return null;
        File file = new File(getPendientesDir(context), id + ".json");
        if (!file.exists()) return null;

        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();

            JSONObject json = new JSONObject(sb.toString());
            return jsonToMantenimiento(json);
        } catch (Exception e) {
            Log.e(TAG, "Error leyendo pendiente " + id, e);
            return null;
        }
    }

    /**
     * Elimina un mantenimiento de la cola de pendientes una vez subido a la nube.
     */
    public static boolean eliminarPendiente(Context context, String id) {
        if (context == null || id == null) return false;
        File file = new File(getPendientesDir(context), id + ".json");
        if (file.exists()) {
            return file.delete();
        }
        return false;
    }

    /**
     * Sincroniza en segundo plano todos los mantenimientos pendientes con Firebase.
     * Sube las fotos locales a Firebase Storage, actualiza los URLs en Firestore
     * y elimina los registros locales pendientes una vez completado.
     */
    public static void sincronizarPendientes(Context context, Callback<Integer> callback) {
        if (context == null) {
            if (callback != null) callback.onError(new Exception("Contexto nulo"));
            return;
        }

        if (!NetworkUtils.isNetworkAvailable(context)) {
            Log.d(TAG, "Sincronización omitida: sin conexión a internet.");
            if (callback != null) callback.onSuccess(0);
            return;
        }

        if (isSyncing.getAndSet(true)) {
            Log.d(TAG, "Sincronización ya en progreso.");
            if (callback != null) callback.onSuccess(0);
            return;
        }

        executor.execute(() -> {
            int totalSincronizados = 0;
            try {
                List<Mantenimiento> pendientes = obtenerPendientes(context);
                if (pendientes.isEmpty()) {
                    isSyncing.set(false);
                    if (callback != null) {
                        new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(0));
                    }
                    return;
                }

                Log.d(TAG, "Iniciando sincronización de " + pendientes.size() + " mantenimientos pendientes...");
                FirebaseFirestore db = FirebaseFirestore.getInstance();
                FirebaseStorage storage = FirebaseStorage.getInstance();

                for (Mantenimiento m : pendientes) {
                    try {
                        List<String> fotosActualizadas = new ArrayList<>();
                        boolean falloAlgunaFoto = false;

                        if (m.getFotos() != null) {
                            for (String foto : m.getFotos()) {
                                if (foto != null && (foto.startsWith("/") || foto.startsWith("file:"))) {
                                    String localPath = foto.startsWith("file:") ? foto.substring(5) : foto;
                                    File localFile = new File(localPath);

                                    if (localFile.exists() && localFile.length() > 0) {
                                        byte[] bytes = leerBytesDeArchivo(localFile);
                                        if (bytes != null && bytes.length > 0) {
                                            String fileName = "mantenimientos/" + m.getCodigoGrupo() + "/" 
                                                    + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 6) + ".jpg";
                                            StorageReference sRef = storage.getReference().child(fileName);
                                            StorageMetadata metadata = new StorageMetadata.Builder()
                                                    .setContentType("image/jpeg")
                                                    .build();

                                            UploadTask.TaskSnapshot snap = Tasks.await(sRef.putBytes(bytes, metadata), 25, TimeUnit.SECONDS);
                                            Uri downloadUri = Tasks.await(sRef.getDownloadUrl(), 10, TimeUnit.SECONDS);

                                            fotosActualizadas.add(downloadUri.toString());
                                            Log.d(TAG, "Foto subida a Storage: " + downloadUri.toString());
                                        } else {
                                            falloAlgunaFoto = true;
                                            fotosActualizadas.add(foto);
                                        }
                                    } else {
                                        fotosActualizadas.add(foto);
                                    }
                                } else {
                                    // Ya era URL remota
                                    fotosActualizadas.add(foto);
                                }
                            }
                        }

                        m.setFotos(fotosActualizadas);

                        // Guardar en Firestore con las URLs de fotos reales
                        Tasks.await(db.collection("mantenimientos").document(m.getId()).set(m), 15, TimeUnit.SECONDS);

                        // Si todo se guardó correctamente, eliminar de la cola offline
                        eliminarPendiente(context, m.getId());
                        totalSincronizados++;
                        Log.d(TAG, "Mantenimiento " + m.getId() + " sincronizado exitosamente con la nube.");

                    } catch (Exception e) {
                        Log.e(TAG, "Error sincronizando mantenimiento id=" + m.getId(), e);
                    }
                }

            } catch (Exception e) {
                Log.e(TAG, "Error general en sincronización", e);
            } finally {
                isSyncing.set(false);
                final int finalCount = totalSincronizados;
                if (callback != null) {
                    new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(finalCount));
                }
            }
        });
    }

    private static byte[] leerBytesDeArchivo(File file) {
        try {
            FileInputStream fis = new FileInputStream(file);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] b = new byte[8192];
            int read;
            while ((read = fis.read(b)) != -1) {
                bos.write(b, 0, read);
            }
            fis.close();
            return bos.toByteArray();
        } catch (Exception e) {
            Log.e(TAG, "Error leyendo bytes de archivo: " + file.getAbsolutePath(), e);
            return null;
        }
    }

    // --- CACHÉ Y BACKUP DE ITEMS DE CONFIGURACIÓN (ACCESORIOS) ---

    public static void guardarConfiguracionBackup(Context context, List<MantenimientoConfiguracion> items) {
        if (context == null || items == null) return;
        try {
            JSONArray arr = new JSONArray();
            for (MantenimientoConfiguracion item : items) {
                JSONObject obj = new JSONObject();
                obj.put("id", item.getId());
                obj.put("nombre", item.getNombre());
                obj.put("icono", item.getIcono());
                obj.put("fechaCreacion", item.getFechaCreacion());
                arr.put(obj);
            }
            File file = new File(context.getFilesDir(), ARCHIVO_CONFIG_CACHE);
            FileWriter writer = new FileWriter(file);
            writer.write(arr.toString());
            writer.flush();
            writer.close();
        } catch (Exception e) {
            Log.w(TAG, "Error guardando backup de configuracion", e);
        }
    }

    public static List<MantenimientoConfiguracion> obtenerConfiguracionBackup(Context context) {
        List<MantenimientoConfiguracion> lista = new ArrayList<>();
        if (context == null) return lista;
        File file = new File(context.getFilesDir(), ARCHIVO_CONFIG_CACHE);
        if (!file.exists()) return lista;

        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();

            JSONArray arr = new JSONArray(sb.toString());
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                MantenimientoConfiguracion item = new MantenimientoConfiguracion();
                item.setId(obj.optString("id"));
                item.setNombre(obj.optString("nombre"));
                item.setIcono(obj.optString("icono"));
                item.setFechaCreacion(obj.optLong("fechaCreacion"));
                lista.add(item);
            }
        } catch (Exception e) {
            Log.w(TAG, "Error leyendo backup de configuracion", e);
        }
        return lista;
    }

    // --- SERIALIZADORES JSON ---

    public static JSONObject mantenimientoToJson(Mantenimiento m) {
        JSONObject json = new JSONObject();
        try {
            json.put("id", m.getId());
            json.put("codigoGrupo", m.getCodigoGrupo());
            json.put("empresa", m.getEmpresa());
            json.put("horometro", m.getHorometro());
            json.put("fecha", m.getFecha());
            json.put("comentarios", m.getComentarios());
            json.put("fechaCreacion", m.getFechaCreacion());

            JSONArray itemsArr = new JSONArray();
            if (m.getItemsRealizados() != null) {
                for (String it : m.getItemsRealizados()) {
                    itemsArr.put(it);
                }
            }
            json.put("itemsRealizados", itemsArr);

            JSONArray fotosArr = new JSONArray();
            if (m.getFotos() != null) {
                for (String f : m.getFotos()) {
                    fotosArr.put(f);
                }
            }
            json.put("fotos", fotosArr);

            // Campos opcionales de informe
            json.put("proximoHorometro", m.getProximoHorometro());
            json.put("proximaFecha", m.getProximaFecha());
            json.put("tieneBandeja", m.isTieneBandeja());
            json.put("tieneExtintor", m.isTieneExtintor());
            json.put("tieneKit", m.isTieneKit());
            json.put("tienePuestaTierra", m.isTienePuestaTierra());
            json.put("cliente", m.getCliente());
            json.put("lugar", m.getLugar());
            json.put("estadoMaquina", m.getEstadoMaquina());
            json.put("ubicacionMaquina", m.getUbicacionMaquina());
            json.put("tipoServicio", m.getTipoServicio());
            json.put("aceiteUtilizado", m.getAceiteUtilizado());
            json.put("contactoCliente", m.getContactoCliente());
            json.put("fallaEncontrada", m.getFallaEncontrada());
            json.put("causaFalla", m.getCausaFalla());
            json.put("trabajosRealizados", m.getTrabajosRealizados());
            json.put("tecnicoFirmaId", m.getTecnicoFirmaId());
            json.put("supervisorFirmaId", m.getSupervisorFirmaId());
            json.put("cantidadAceite", m.getCantidadAceite());

            if (m.getCodigosFiltrosUsados() != null) {
                JSONObject filtros = new JSONObject();
                for (Map.Entry<String, String> entry : m.getCodigosFiltrosUsados().entrySet()) {
                    filtros.put(entry.getKey(), entry.getValue());
                }
                json.put("codigosFiltrosUsados", filtros);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error serializando mantenimiento", e);
        }
        return json;
    }

    public static Mantenimiento jsonToMantenimiento(JSONObject json) {
        if (json == null) return null;
        try {
            Mantenimiento m = new Mantenimiento();
            m.setId(json.optString("id"));
            m.setCodigoGrupo(json.optString("codigoGrupo"));
            m.setEmpresa(json.optString("empresa"));
            m.setHorometro(json.optString("horometro"));
            m.setFecha(json.optString("fecha"));
            m.setComentarios(json.optString("comentarios"));
            m.setFechaCreacion(json.optLong("fechaCreacion", System.currentTimeMillis()));

            List<String> items = new ArrayList<>();
            JSONArray itemsArr = json.optJSONArray("itemsRealizados");
            if (itemsArr != null) {
                for (int i = 0; i < itemsArr.length(); i++) {
                    items.add(itemsArr.getString(i));
                }
            }
            m.setItemsRealizados(items);

            List<String> fotos = new ArrayList<>();
            JSONArray fotosArr = json.optJSONArray("fotos");
            if (fotosArr != null) {
                for (int i = 0; i < fotosArr.length(); i++) {
                    fotos.add(fotosArr.getString(i));
                }
            }
            m.setFotos(fotos);

            m.setProximoHorometro(json.optString("proximoHorometro"));
            m.setProximaFecha(json.optString("proximaFecha"));
            m.setTieneBandeja(json.optBoolean("tieneBandeja", false));
            m.setTieneExtintor(json.optBoolean("tieneExtintor", false));
            m.setTieneKit(json.optBoolean("tieneKit", false));
            m.setTienePuestaTierra(json.optBoolean("tienePuestaTierra", false));
            m.setCliente(json.optString("cliente"));
            m.setLugar(json.optString("lugar"));
            m.setEstadoMaquina(json.optString("estadoMaquina"));
            m.setUbicacionMaquina(json.optString("ubicacionMaquina"));
            m.setTipoServicio(json.optString("tipoServicio"));
            m.setAceiteUtilizado(json.optString("aceiteUtilizado"));
            m.setContactoCliente(json.optString("contactoCliente"));
            m.setFallaEncontrada(json.optString("fallaEncontrada"));
            m.setCausaFalla(json.optString("causaFalla"));
            m.setTrabajosRealizados(json.optString("trabajosRealizados"));
            m.setTecnicoFirmaId(json.optString("tecnicoFirmaId"));
            m.setSupervisorFirmaId(json.optString("supervisorFirmaId"));
            m.setCantidadAceite(json.optString("cantidadAceite"));

            JSONObject filtros = json.optJSONObject("codigosFiltrosUsados");
            if (filtros != null) {
                Map<String, String> mapFiltros = new HashMap<>();
                Iterator<String> keys = filtros.keys();
                while (keys.hasNext()) {
                    String k = keys.next();
                    mapFiltros.put(k, filtros.optString(k));
                }
                m.setCodigosFiltrosUsados(mapFiltros);
            }

            return m;
        } catch (Exception e) {
            Log.e(TAG, "Error deserializando mantenimiento", e);
            return null;
        }
    }
}
