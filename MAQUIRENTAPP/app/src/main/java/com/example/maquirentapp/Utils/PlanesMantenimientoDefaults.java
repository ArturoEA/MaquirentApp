package com.example.maquirentapp.Utils;

import android.util.Log;

import com.example.maquirentapp.Model.MantenimientoConfiguracion;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Proveedor de plantillas oficiales por defecto para planes de mantenimiento
 * según especificaciones técnicas de fabricantes (Perkins, Stamford, Compresores).
 * Permite inicializar o sincronizar la base de datos de Firestore.
 */
public class PlanesMantenimientoDefaults {

    private static final String TAG = "PlanesDefaults";
    public static final String TIPO_GRUPO_ELECTROGENO = "Grupo electrógeno";
    public static final String TIPO_COMPRESORA_AIRE = "Compresora de aire";

    public static final String SUBSISTEMA_LUBRICACION = "Lubricación";
    public static final String SUBSISTEMA_COMBUSTIBLE = "Combustible";
    public static final String SUBSISTEMA_ADMISION = "Admisión y escape";
    public static final String SUBSISTEMA_ELECTRICO = "Eléctrico, generación y control";
    public static final String SUBSISTEMA_COMPRESION = "Compresión";
    public static final String SUBSISTEMA_ESTRUCTURAL = "Estructural y carreta";

    /**
     * Plan maestro para Grupos Electrógenos basado en los manuales oficiales de:
     * - Perkins: Programa de intervalos de mantenimiento de motor
     * - Stamford: Calendario de servicio del alternador (Cummins Generator Technologies)
     * - Carreta y chasis: Normativa Maquirent para equipos móviles
     */
    public static List<MantenimientoConfiguracion> obtenerPlanGruposElectrogenos() {
        List<MantenimientoConfiguracion> lista = new ArrayList<>();

        // =========================================================================
        // 1. SUBSISTEMA LUBRICACIÓN (Perkins)
        // =========================================================================
        lista.add(new MantenimientoConfiguracion(
                "Comprobación de nivel de aceite de motor", "",
                TIPO_GRUPO_ELECTROGENO, SUBSISTEMA_LUBRICACION,
                0, 0, "aceite", true, 1
        ));
        lista.add(new MantenimientoConfiguracion(
                "Cambio de aceite de motor (15W-40) y filtro de aceite", "",
                TIPO_GRUPO_ELECTROGENO, SUBSISTEMA_LUBRICACION,
                250, 6, "aceite", false, 0
        ));
        lista.add(new MantenimientoConfiguracion(
                "Inspección y ajuste de luz de válvulas de motor", "",
                TIPO_GRUPO_ELECTROGENO, SUBSISTEMA_LUBRICACION,
                1000, 12, "valvulas", false, 0
        ));

        // =========================================================================
        // 2. SUBSISTEMA COMBUSTIBLE (Perkins)
        // =========================================================================
        lista.add(new MantenimientoConfiguracion(
                "Drenaje de agua y sedimentos en filtro trampa Racor", "",
                TIPO_GRUPO_ELECTROGENO, SUBSISTEMA_COMBUSTIBLE,
                0, 0, "filtro_petroleo", true, 1
        ));
        lista.add(new MantenimientoConfiguracion(
                "Drenaje de agua y sedimentos del tanque de combustible", "",
                TIPO_GRUPO_ELECTROGENO, SUBSISTEMA_COMBUSTIBLE,
                50, 1, "filtro_petroleo", false, 0
        ));
        lista.add(new MantenimientoConfiguracion(
                "Reemplazo de filtro primario de combustible (separador Racor)", "",
                TIPO_GRUPO_ELECTROGENO, SUBSISTEMA_COMBUSTIBLE,
                500, 12, "filtro_petroleo", false, 0
        ));
        lista.add(new MantenimientoConfiguracion(
                "Reemplazo de filtro secundario de combustible", "",
                TIPO_GRUPO_ELECTROGENO, SUBSISTEMA_COMBUSTIBLE,
                500, 12, "filtro_petroleo", false, 0
        ));
        lista.add(new MantenimientoConfiguracion(
                "Prueba y calibración de inyectores de combustible", "",
                TIPO_GRUPO_ELECTROGENO, SUBSISTEMA_COMBUSTIBLE,
                3000, 24, "filtro_petroleo", false, 0
        ));

        // =========================================================================
        // 3. SUBSISTEMA ADMISIÓN Y ESCAPE (Perkins)
        // =========================================================================
        lista.add(new MantenimientoConfiguracion(
                "Inspección de indicador de servicio del filtro de aire", "",
                TIPO_GRUPO_ELECTROGENO, SUBSISTEMA_ADMISION,
                0, 0, "filtro_aire", true, 1
        ));
        lista.add(new MantenimientoConfiguracion(
                "Comprobación de nivel de refrigerante en radiador", "",
                TIPO_GRUPO_ELECTROGENO, SUBSISTEMA_ADMISION,
                0, 0, "refrigerante", true, 1
        ));
        lista.add(new MantenimientoConfiguracion(
                "Limpieza / Reemplazo de elemento de filtro de aire del motor", "",
                TIPO_GRUPO_ELECTROGENO, SUBSISTEMA_ADMISION,
                500, 12, "filtro_aire", false, 0
        ));
        lista.add(new MantenimientoConfiguracion(
                "Inspección de mangueras, tuberías y abrazaderas de enfriamiento", "",
                TIPO_GRUPO_ELECTROGENO, SUBSISTEMA_ADMISION,
                500, 12, "mangueras", false, 0
        ));
        lista.add(new MantenimientoConfiguracion(
                "Limpieza externa de aletas del radiador e inspección", "",
                TIPO_GRUPO_ELECTROGENO, SUBSISTEMA_ADMISION,
                500, 12, "radiador", false, 0
        ));
        lista.add(new MantenimientoConfiguracion(
                "Inspección de turbocompresor y holgura de eje", "",
                TIPO_GRUPO_ELECTROGENO, SUBSISTEMA_ADMISION,
                2000, 24, "turbo", false, 0
        ));
        lista.add(new MantenimientoConfiguracion(
                "Inspección de bomba de agua y sello mecánico", "",
                TIPO_GRUPO_ELECTROGENO, SUBSISTEMA_ADMISION,
                2000, 24, "bomba_agua", false, 0
        ));
        lista.add(new MantenimientoConfiguracion(
                "Inspección de núcleo del posenfriador (aftercooler)", "",
                TIPO_GRUPO_ELECTROGENO, SUBSISTEMA_ADMISION,
                2000, 24, "aftercooler", false, 0
        ));
        lista.add(new MantenimientoConfiguracion(
                "Reemplazo de refrigerante de larga duración (ELC)", "",
                TIPO_GRUPO_ELECTROGENO, SUBSISTEMA_ADMISION,
                3000, 24, "refrigerante", false, 0
        ));

        // =========================================================================
        // 4. SUBSISTEMA ELÉCTRICO, GENERACIÓN Y CONTROL (Perkins + Stamford)
        // =========================================================================
        lista.add(new MantenimientoConfiguracion(
                "Inspección y ajuste de correas de alternador y ventilador", "",
                TIPO_GRUPO_ELECTROGENO, SUBSISTEMA_ELECTRICO,
                0, 0, "faja", true, 1
        ));
        lista.add(new MantenimientoConfiguracion(
                "Comprobación de nivel de electrólito y estado de batería", "",
                TIPO_GRUPO_ELECTROGENO, SUBSISTEMA_ELECTRICO,
                500, 12, "bateria", false, 0
        ));
        lista.add(new MantenimientoConfiguracion(
                "Inspección y limpieza de conexión a tierra del motor", "",
                TIPO_GRUPO_ELECTROGENO, SUBSISTEMA_ELECTRICO,
                500, 12, "tierra", false, 0
        ));
        lista.add(new MantenimientoConfiguracion(
                "Inspección de condiciones ambientales y limpieza de alternador (Stamford)", "",
                TIPO_GRUPO_ELECTROGENO, SUBSISTEMA_ELECTRICO,
                250, 6, "generador", false, 0
        ));
        lista.add(new MantenimientoConfiguracion(
                "Inspección de máquina completa: piezas sueltas y conexiones a tierra del alternador", "",
                TIPO_GRUPO_ELECTROGENO, SUBSISTEMA_ELECTRICO,
                250, 6, "generador", false, 0
        ));
        lista.add(new MantenimientoConfiguracion(
                "Comprobación de parámetros eléctricos, voltaje y excitación en ejecución", "",
                TIPO_GRUPO_ELECTROGENO, SUBSISTEMA_ELECTRICO,
                250, 6, "generador", false, 0
        ));
        lista.add(new MantenimientoConfiguracion(
                "Verificación de nivel de vibración del alternador en ejecución", "",
                TIPO_GRUPO_ELECTROGENO, SUBSISTEMA_ELECTRICO,
                250, 6, "generador", false, 0
        ));
        lista.add(new MantenimientoConfiguracion(
                "Inspección visual del estado de devanados y aislamiento del alternador", "",
                TIPO_GRUPO_ELECTROGENO, SUBSISTEMA_ELECTRICO,
                250, 6, "generador", false, 0
        ));
        lista.add(new MantenimientoConfiguracion(
                "Inspección de conexiones de caja de bornes del alternador y cableado", "",
                TIPO_GRUPO_ELECTROGENO, SUBSISTEMA_ELECTRICO,
                250, 6, "generador", false, 0
        ));
        lista.add(new MantenimientoConfiguracion(
                "Verificación y ajuste de regulador de voltaje AVR y circuitos auxiliares", "",
                TIPO_GRUPO_ELECTROGENO, SUBSISTEMA_ELECTRICO,
                250, 6, "generador", false, 0
        ));
        lista.add(new MantenimientoConfiguracion(
                "Inspección de puente rectificador: diodos y varistores", "",
                TIPO_GRUPO_ELECTROGENO, SUBSISTEMA_ELECTRICO,
                250, 6, "generador", false, 0
        ));
        lista.add(new MantenimientoConfiguracion(
                "Inspección de flujo de aire, ventilador y temperatura de entrada", "",
                TIPO_GRUPO_ELECTROGENO, SUBSISTEMA_ELECTRICO,
                250, 6, "generador", false, 0
        ));
        lista.add(new MantenimientoConfiguracion(
                "Medición de resistencia de aislamiento de devanados (Megger / índice polaridad)", "",
                TIPO_GRUPO_ELECTROGENO, SUBSISTEMA_ELECTRICO,
                1000, 12, "generador", false, 0
        ));
        lista.add(new MantenimientoConfiguracion(
                "Inspección de cojinetes y rodamientos sellados del alternador", "",
                TIPO_GRUPO_ELECTROGENO, SUBSISTEMA_ELECTRICO,
                1000, 12, "generador", false, 0
        ));
        lista.add(new MantenimientoConfiguracion(
                "Inspección y prueba de motor de arranque y alternador de carga", "",
                TIPO_GRUPO_ELECTROGENO, SUBSISTEMA_ELECTRICO,
                2000, 24, "arranque", false, 0
        ));

        // =========================================================================
        // 5. SUBSISTEMA ESTRUCTURAL Y CARRETA (Carreta / Chasis / Retornos)
        // =========================================================================
        lista.add(new MantenimientoConfiguracion(
                "Inspección de chasis, carrocería y verificación de fugas", "",
                TIPO_GRUPO_ELECTROGENO, SUBSISTEMA_ESTRUCTURAL,
                0, 0, "carreta", true, 1
        ));
        lista.add(new MantenimientoConfiguracion(
                "Ajuste y torque de pernos de carreta y muelles de suspensión", "",
                TIPO_GRUPO_ELECTROGENO, SUBSISTEMA_ESTRUCTURAL,
                0, 0, "carreta", true, 1
        ));
        lista.add(new MantenimientoConfiguracion(
                "Inspección de lanza de tiro, perno rey y acople de remolque", "",
                TIPO_GRUPO_ELECTROGENO, SUBSISTEMA_ESTRUCTURAL,
                0, 0, "carreta", true, 1
        ));
        lista.add(new MantenimientoConfiguracion(
                "Revisión de presión y desgaste de neumáticos de carreta", "",
                TIPO_GRUPO_ELECTROGENO, SUBSISTEMA_ESTRUCTURAL,
                0, 0, "carreta", true, 1
        ));
        lista.add(new MantenimientoConfiguracion(
                "Ajuste de pernos de soportes antivibratorios motor y alternador", "",
                TIPO_GRUPO_ELECTROGENO, SUBSISTEMA_ESTRUCTURAL,
                0, 0, "carreta", true, 2
        ));
        lista.add(new MantenimientoConfiguracion(
                "Engrase de bocinas, rodamientos de ruedas y seguros de carreta", "",
                TIPO_GRUPO_ELECTROGENO, SUBSISTEMA_ESTRUCTURAL,
                0, 0, "carreta", true, 3
        ));
        lista.add(new MantenimientoConfiguracion(
                "Inspección estructural de soportes y tacos del motor", "",
                TIPO_GRUPO_ELECTROGENO, SUBSISTEMA_ESTRUCTURAL,
                2000, 24, "carreta", false, 0
        ));

        return lista;
    }

    /**
     * Plan maestro para Compresoras de Aire portátiles.
     */
    public static List<MantenimientoConfiguracion> obtenerPlanCompresoras() {
        List<MantenimientoConfiguracion> lista = new ArrayList<>();

        // 1. Lubricación motor
        lista.add(new MantenimientoConfiguracion(
                "Comprobación de nivel de aceite de motor", "",
                TIPO_COMPRESORA_AIRE, SUBSISTEMA_LUBRICACION,
                0, 0, "aceite", true, 1
        ));
        lista.add(new MantenimientoConfiguracion(
                "Cambio de aceite de motor diésel (15W-40) y filtro de aceite", "",
                TIPO_COMPRESORA_AIRE, SUBSISTEMA_LUBRICACION,
                250, 6, "aceite", false, 0
        ));

        // 2. Combustible
        lista.add(new MantenimientoConfiguracion(
                "Drenaje de agua y sedimentos en trampa de combustible Racor", "",
                TIPO_COMPRESORA_AIRE, SUBSISTEMA_COMBUSTIBLE,
                0, 0, "filtro_petroleo", true, 1
        ));
        lista.add(new MantenimientoConfiguracion(
                "Drenaje de agua y sedimentos del tanque de combustible", "",
                TIPO_COMPRESORA_AIRE, SUBSISTEMA_COMBUSTIBLE,
                50, 1, "filtro_petroleo", false, 0
        ));
        lista.add(new MantenimientoConfiguracion(
                "Cambio de filtros de combustible de motor (primario y secundario)", "",
                TIPO_COMPRESORA_AIRE, SUBSISTEMA_COMBUSTIBLE,
                500, 12, "filtro_petroleo", false, 0
        ));

        // 3. Admisión y escape
        lista.add(new MantenimientoConfiguracion(
                "Inspección de indicador de restricción de filtros de aire", "",
                TIPO_COMPRESORA_AIRE, SUBSISTEMA_ADMISION,
                0, 0, "filtro_aire", true, 1
        ));
        lista.add(new MantenimientoConfiguracion(
                "Limpieza / Reemplazo de filtro de aire de motor", "",
                TIPO_COMPRESORA_AIRE, SUBSISTEMA_ADMISION,
                500, 12, "filtro_aire", false, 0
        ));
        lista.add(new MantenimientoConfiguracion(
                "Limpieza / Reemplazo de filtro de aire de unidad compresora", "",
                TIPO_COMPRESORA_AIRE, SUBSISTEMA_ADMISION,
                500, 12, "filtro_aire", false, 0
        ));
        lista.add(new MantenimientoConfiguracion(
                "Inspección de radiadores, mangueras y tuberías de aire comprimido", "",
                TIPO_COMPRESORA_AIRE, SUBSISTEMA_ADMISION,
                500, 12, "radiador", false, 0
        ));

        // 4. Compresión
        lista.add(new MantenimientoConfiguracion(
                "Comprobación de nivel de aceite de unidad compresora de tornillo", "",
                TIPO_COMPRESORA_AIRE, SUBSISTEMA_COMPRESION,
                0, 0, "tornillo_compresor", true, 1
        ));
        lista.add(new MantenimientoConfiguracion(
                "Reemplazo de filtro de aceite de tornillo compresor", "",
                TIPO_COMPRESORA_AIRE, SUBSISTEMA_COMPRESION,
                500, 12, "tornillo_compresor", false, 0
        ));
        lista.add(new MantenimientoConfiguracion(
                "Cambio de aceite sintético de unidad compresora de tornillo", "",
                TIPO_COMPRESORA_AIRE, SUBSISTEMA_COMPRESION,
                1000, 12, "tornillo_compresor", false, 0
        ));
        lista.add(new MantenimientoConfiguracion(
                "Reemplazo de elemento separador de aire y aceite (coalescente)", "",
                TIPO_COMPRESORA_AIRE, SUBSISTEMA_COMPRESION,
                1000, 12, "tornillo_compresor", false, 0
        ));
        lista.add(new MantenimientoConfiguracion(
                "Inspección y calibración de válvula de admisión y presión mínima", "",
                TIPO_COMPRESORA_AIRE, SUBSISTEMA_COMPRESION,
                1000, 12, "valvula_presion", false, 0
        ));
        lista.add(new MantenimientoConfiguracion(
                "Limpieza externa de radiador enfriador de aceite de compresor", "",
                TIPO_COMPRESORA_AIRE, SUBSISTEMA_COMPRESION,
                500, 12, "radiador", false, 0
        ));

        // 5. Estructural y carreta
        lista.add(new MantenimientoConfiguracion(
                "Ajuste de pernos de carreta, muelles y lanza de remolque", "",
                TIPO_COMPRESORA_AIRE, SUBSISTEMA_ESTRUCTURAL,
                0, 0, "carreta", true, 1
        ));
        lista.add(new MantenimientoConfiguracion(
                "Revisión de presión de neumáticos y estado de suspensiones", "",
                TIPO_COMPRESORA_AIRE, SUBSISTEMA_ESTRUCTURAL,
                0, 0, "carreta", true, 1
        ));
        lista.add(new MantenimientoConfiguracion(
                "Ajuste de pernos de soportes antivibratorios de motor y unidad compresora", "",
                TIPO_COMPRESORA_AIRE, SUBSISTEMA_ESTRUCTURAL,
                0, 0, "carreta", true, 2
        ));
        lista.add(new MantenimientoConfiguracion(
                "Engrase de rodamientos de ruedas y lanza de remolque", "",
                TIPO_COMPRESORA_AIRE, SUBSISTEMA_ESTRUCTURAL,
                0, 0, "carreta", true, 3
        ));
        lista.add(new MantenimientoConfiguracion(
                "Inspección de acople elástico entre motor y tornillo compresor", "",
                TIPO_COMPRESORA_AIRE, SUBSISTEMA_ESTRUCTURAL,
                1000, 12, "acople", false, 0
        ));

        return lista;
    }

    public static void sembrarPlanesSiEstaVacio(Runnable onComplete) {
        sembrarPlanesSiEstaVacio(FirebaseFirestore.getInstance(), onComplete);
    }

    public static void sembrarPlanesSiEstaVacio(FirebaseFirestore db, Runnable onComplete) {
        db.collection("mantenimientos_configuracion").get()
                .addOnSuccessListener(snap -> {
                    if (snap == null || snap.isEmpty()) {
                        Log.d(TAG, "Inicializando plantillas de mantenimiento completas oficiales...");
                        sincronizarPlantillasCompletas(db, onComplete);
                    } else if (snap.size() < 15) {
                        // Si la colección tenía pocas tareas de una prueba anterior, enriquecer con todas las oficiales
                        Log.d(TAG, "Enriqueciendo plantillas con la lista completa de Perkins y Stamford...");
                        sincronizarPlantillasCompletas(db, onComplete);
                    } else {
                        if (onComplete != null) onComplete.run();
                    }
                })
                .addOnFailureListener(e -> {
                    if (onComplete != null) onComplete.run();
                });
    }

    /**
     * Sincroniza todas las tareas oficiales de Perkins y Stamford agregando
     * únicamente las que no existan aún en Firestore (sin duplicados ni sobrescrituras).
     */
    public static void sincronizarPlantillasCompletas(FirebaseFirestore db, Runnable onComplete) {
        db.collection("mantenimientos_configuracion").get()
                .addOnSuccessListener(snapExistentes -> {
                    Set<String> nombresExistentes = new HashSet<>();
                    if (snapExistentes != null) {
                        for (QueryDocumentSnapshot doc : snapExistentes) {
                            String nom = doc.getString("nombre");
                            if (nom != null) {
                                nombresExistentes.add(nom.trim().toLowerCase());
                            }
                        }
                    }

                    List<MantenimientoConfiguracion> todasLasOficiales = new ArrayList<>();
                    todasLasOficiales.addAll(obtenerPlanGruposElectrogenos());
                    todasLasOficiales.addAll(obtenerPlanCompresoras());

                    List<MantenimientoConfiguracion> aInsertar = new ArrayList<>();
                    for (MantenimientoConfiguracion item : todasLasOficiales) {
                        String k = item.getNombre() != null ? item.getNombre().trim().toLowerCase() : "";
                        if (!nombresExistentes.contains(k)) {
                            aInsertar.add(item);
                            nombresExistentes.add(k);
                        }
                    }

                    if (aInsertar.isEmpty()) {
                        if (onComplete != null) onComplete.run();
                        return;
                    }

                    WriteBatch batch = db.batch();
                    for (MantenimientoConfiguracion nuevo : aInsertar) {
                        com.google.firebase.firestore.DocumentReference ref =
                                db.collection("mantenimientos_configuracion").document();
                        nuevo.setId(ref.getId());
                        nuevo.setFechaCreacion(System.currentTimeMillis());
                        batch.set(ref, nuevo);
                    }

                    batch.commit()
                            .addOnSuccessListener(v -> {
                                Log.d(TAG, "Se sincronizaron " + aInsertar.size() + " tareas oficiales con éxito.");
                                if (onComplete != null) onComplete.run();
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error sincronizando tareas oficiales", e);
                                if (onComplete != null) onComplete.run();
                            });
                })
                .addOnFailureListener(e -> {
                    if (onComplete != null) onComplete.run();
                });
    }
}
