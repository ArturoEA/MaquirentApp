package com.example.maquirentapp.Repository;

import android.util.Log;

import com.example.maquirentapp.Model.Cotizacion;
import com.example.maquirentapp.Model.GrupoElectrogeno;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.Transaction;
import com.google.firebase.firestore.WriteBatch;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CotizacionesRepository {

    private final FirebaseFirestore db;
    private static final String COL_COTIZACIONES = "cotizaciones";
    private static final String COL_CONFIG = "configuracion";
    private static final String DOC_SECUENCIAS = "secuencias";
    private static final String FIELD_ULTIMA_COT = "ultima_cotizacion";
    private static final String COL_GRUPOS = "gruposElectrogenos";


    public CotizacionesRepository() {
        this.db = FirebaseFirestore.getInstance();
    }
    public void crearCotizacion(Cotizacion cotizacion, Callback<String> callback) {
        final DocumentReference secuenciaRef = db.collection(COL_CONFIG).document(DOC_SECUENCIAS);
        final DocumentReference nuevaCotizacionRef = db.collection(COL_COTIZACIONES).document();

        java.util.Calendar cal = java.util.Calendar.getInstance();
        int anio = cal.get(java.util.Calendar.YEAR);
        String fieldAnio = FIELD_ULTIMA_COT + "_" + anio;

        db.runTransaction((Transaction.Function<Void>) transaction -> {
                    DocumentSnapshot snapshot = transaction.get(secuenciaRef);

                    long nuevoNumero = 1;

                    if (snapshot.exists()) {
                        Object valAnio = snapshot.get(fieldAnio);
                        if (valAnio instanceof Number) {
                            nuevoNumero = ((Number) valAnio).longValue() + 1;
                        } else {
                            Object valGlobal = snapshot.get(FIELD_ULTIMA_COT);
                            if (valGlobal instanceof Number) {
                                nuevoNumero = ((Number) valGlobal).longValue() + 1;
                            } else if (anio == 2026) {
                                nuevoNumero = 22; // Hay 21 cotizaciones registradas en 2026
                            }
                        }
                    } else if (anio == 2026) {
                        nuevoNumero = 22;
                    }

                    String codigoGenerado = com.example.maquirentapp.strategy.StrategyProvider.getCorrelativoStrategy()
                            .generarCodigoCotizacion(nuevoNumero, anio);
                    cotizacion.setNumeroCotizacion(codigoGenerado);
                    cotizacion.setId(nuevaCotizacionRef.getId());
                    if (cotizacion.getFechaCreacionTimestamp() == null || cotizacion.getFechaCreacionTimestamp().trim().isEmpty()) {
                        cotizacion.setFechaCreacionTimestamp(String.valueOf(System.currentTimeMillis()));
                    }

                    Map<String, Object> datosContador = new HashMap<>();
                    datosContador.put(fieldAnio, nuevoNumero);
                    datosContador.put(FIELD_ULTIMA_COT, nuevoNumero);

                    transaction.set(nuevaCotizacionRef, cotizacion);
                    transaction.set(secuenciaRef, datosContador, com.google.firebase.firestore.SetOptions.merge());

                    return null;
                }).addOnSuccessListener(aVoid -> callback.onSuccess(cotizacion.getNumeroCotizacion()))
                .addOnFailureListener(callback::onError);
    }
    public void getGruposParaSeleccion(Callback<List<GrupoElectrogeno>> callback) {
        db.collection(COL_GRUPOS)
                .whereEqualTo("eliminado", false)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<GrupoElectrogeno> grupos = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        GrupoElectrogeno g = doc.toObject(GrupoElectrogeno.class);
                        g.setId(doc.getId());
                        grupos.add(g);
                    }
                    callback.onSuccess(grupos);
                })
                .addOnFailureListener(callback::onError);
    }
    public void getCotizaciones(Callback<List<Cotizacion>> callback) {
        db.collection(COL_COTIZACIONES)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Cotizacion> lista = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Cotizacion c = doc.toObject(Cotizacion.class);
                        c.setId(doc.getId());
                        lista.add(c);
                    }

                    // Evaluar si existen códigos duplicados o formatos antiguos
                    // Si los hay, se reordenan cronológicamente (por fecha/hora de creación) y se renumeran de 1 a N
                    if (detectarDuplicadosONecesidadMigracion(lista)) {
                        reordenarYRenumerarCronologicamente(lista);
                    }

                    Collections.sort(lista, (c1, c2) -> {
                        int anio1 = extraerAnio(c1.getNumeroCotizacion());
                        int anio2 = extraerAnio(c2.getNumeroCotizacion());
                        if (anio1 != anio2) {
                            return Integer.compare(anio2, anio1); // Año más reciente primero
                        }
                        long sec1 = extraerNumeroSecuencia(c1.getNumeroCotizacion());
                        long sec2 = extraerNumeroSecuencia(c2.getNumeroCotizacion());
                        return Long.compare(sec2, sec1); // Secuencia más reciente primero
                    });
                    callback.onSuccess(lista);
                })
                .addOnFailureListener(callback::onError);
    }
    public void eliminarCotizacion(String idCotizacion, String numeroCotizacion, Callback<Void> callback) {
        final DocumentReference cotizacionRef = db.collection(COL_COTIZACIONES).document(idCotizacion);
        final DocumentReference secuenciaRef = db.collection(COL_CONFIG).document(DOC_SECUENCIAS);

        db.runTransaction((Transaction.Function<Void>) transaction -> {
                    DocumentSnapshot cotSnapshot = transaction.get(cotizacionRef);
                    if (!cotSnapshot.exists()) {
                        return null;
                    }

                    String numCot = numeroCotizacion;
                    if (numCot == null || numCot.trim().isEmpty()) {
                        numCot = cotSnapshot.getString("numeroCotizacion");
                    }

                    long numSecuenciaEliminar = extraerNumeroSecuencia(numCot);
                    int anioCot = extraerAnio(numCot);

                    boolean actualizarSecuencia = false;
                    Map<String, Object> updates = new HashMap<>();

                    if (numSecuenciaEliminar > 0) {
                        DocumentSnapshot secSnapshot = transaction.get(secuenciaRef);
                        if (secSnapshot.exists()) {
                            String fieldAnio = FIELD_ULTIMA_COT + "_" + anioCot;
                            Object valAnio = secSnapshot.get(fieldAnio);
                            Object valGlobal = secSnapshot.get(FIELD_ULTIMA_COT);

                            long ultimoAnio = -1;
                            if (valAnio instanceof Number) {
                                ultimoAnio = ((Number) valAnio).longValue();
                            } else if (valGlobal instanceof Number) {
                                ultimoAnio = ((Number) valGlobal).longValue();
                            }

                            // Si la cotización eliminada es exactamente la última generada para ese año
                            if (ultimoAnio == numSecuenciaEliminar) {
                                long nuevoContador = Math.max(0, ultimoAnio - 1);
                                updates.put(fieldAnio, nuevoContador);
                                if (valGlobal instanceof Number && ((Number) valGlobal).longValue() == numSecuenciaEliminar) {
                                    updates.put(FIELD_ULTIMA_COT, nuevoContador);
                                }
                                actualizarSecuencia = true;
                            }
                        }
                    }

                    if (actualizarSecuencia) {
                        transaction.set(secuenciaRef, updates, SetOptions.merge());
                    }
                    transaction.delete(cotizacionRef);
                    return null;
                }).addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(callback::onError);
    }

    public void eliminarCotizacion(String idCotizacion, Callback<Void> callback) {
        eliminarCotizacion(idCotizacion, null, callback);
    }

    public static int extraerAnio(String numeroCotizacion) {
        if (numeroCotizacion == null || numeroCotizacion.trim().isEmpty()) {
            return Calendar.getInstance().get(Calendar.YEAR);
        }
        String s = numeroCotizacion.trim();

        // 1. Formato nuevo: COT-XXX-YY (ej. COT-034-26 o COT-VK-034-26) donde YY está al final
        Matcher mNew = Pattern.compile("[-_\\s/](\\d+)[-_\\s/](\\d{2})$").matcher(s);
        if (mNew.find() && mNew.group(2) != null) {
            // Si el primer grupo tiene 4 dígitos (como COT-2026-23), entonces 2026 es el año
            if (mNew.group(1).length() == 4) {
                return Integer.parseInt(mNew.group(1));
            }
            int y2 = Integer.parseInt(mNew.group(2));
            return 2000 + y2;
        }

        // 2. Formato con año de 4 dígitos al inicio: COT-YYYY-XXX (ej. COT-2026-023)
        Matcher mOld4 = Pattern.compile("[-_\\s/](\\d{4})[-_\\s/](\\d+)$").matcher(s);
        if (mOld4.find() && mOld4.group(1) != null) {
            return Integer.parseInt(mOld4.group(1));
        }

        // 3. Formato con año de 2 dígitos y correlativo de 3+ dígitos: COT-26-023
        Matcher mOld2 = Pattern.compile("[-_\\s/](\\d{2})[-_\\s/](\\d{3,})$").matcher(s);
        if (mOld2.find() && mOld2.group(1) != null) {
            return 2000 + Integer.parseInt(mOld2.group(1));
        }

        return Calendar.getInstance().get(Calendar.YEAR);
    }

    public static long extraerNumeroSecuencia(String numeroCotizacion) {
        if (numeroCotizacion == null || numeroCotizacion.trim().isEmpty()) {
            return -1;
        }
        String s = numeroCotizacion.trim();

        // 1. Formato con año de 4 dígitos al inicio: COT-YYYY-XXX (ej. COT-2026-023 o COT-2026-23)
        Matcher mOld4 = Pattern.compile("[-_\\s/](\\d{4})[-_\\s/](\\d+)$").matcher(s);
        if (mOld4.find() && mOld4.group(2) != null) {
            try {
                return Long.parseLong(mOld4.group(2));
            } catch (Exception ignored) {}
        }

        // 2. Formato con año de 2 dígitos al inicio y correlativo de 3+ dígitos: COT-26-023
        Matcher mOld2 = Pattern.compile("[-_\\s/](\\d{2})[-_\\s/](\\d{3,})$").matcher(s);
        if (mOld2.find() && mOld2.group(2) != null) {
            try {
                return Long.parseLong(mOld2.group(2));
            } catch (Exception ignored) {}
        }

        // 3. Formato nuevo: COT-XXX-YY (ej. COT-034-26 o COT-1000-26): el número correlativo es el primer grupo
        Matcher mNew = Pattern.compile("[-_\\s/](\\d+)[-_\\s/](\\d{2})$").matcher(s);
        if (mNew.find() && mNew.group(1) != null) {
            try {
                return Long.parseLong(mNew.group(1));
            } catch (Exception ignored) {}
        }

        // 4. Formato heredado sin año: el número correlativo está al final (ej. COT-000000 o COT-000021)
        Matcher mOld = Pattern.compile("[-_\\s/](\\d+)$").matcher(s);
        if (mOld.find() && mOld.group(1) != null) {
            try {
                return Long.parseLong(mOld.group(1));
            } catch (Exception ignored) {}
        }

        return -1;
    }

    public static boolean necesitaMigracion(String numeroCotizacion) {
        if (numeroCotizacion == null || numeroCotizacion.trim().isEmpty()) {
            return false;
        }
        String s = numeroCotizacion.trim();

        // 1. Si tiene año al inicio (ej. COT-2026-023 o COT-26-023), necesita migración al nuevo formato
        Matcher mOldYearFirst = Pattern.compile("^COT(-VK)?-(\\d{2}|\\d{4})-\\d+$").matcher(s);
        if (mOldYearFirst.matches()) {
            return true;
        }

        // 2. Si ya cumple el formato nuevo: COT-XXX-YY o COT-VK-XXX-YY (secuencia de al menos 3 dígitos y año de 2 dígitos)
        Matcher mNuevo = Pattern.compile("^COT(-VK)?-\\d{3,}-\\d{2}$").matcher(s);
        return !mNuevo.matches();
    }

    public static String migrarCodigo(Cotizacion c) {
        if (c == null) return null;
        String numActual = c.getNumeroCotizacion();
        long sec = extraerNumeroSecuencia(numActual);
        if (sec < 0) {
            return null;
        }

        int anio = -1;
        // 1. Obtener año de fechaEmision (ej. 24/10/2026)
        if (c.getFechaEmision() != null && !c.getFechaEmision().trim().isEmpty()) {
            Matcher mFecha = Pattern.compile("\\b(20\\d{2})\\b").matcher(c.getFechaEmision());
            if (mFecha.find() && mFecha.group(1) != null) {
                try {
                    anio = Integer.parseInt(mFecha.group(1));
                } catch (Exception ignored) {}
            }
        }

        // 2. Obtener año de fechaCreacionTimestamp si está disponible
        if (anio <= 0 && c.getFechaCreacionTimestamp() != null && !c.getFechaCreacionTimestamp().trim().isEmpty()) {
            Matcher mTs = Pattern.compile("\\b(20\\d{2})\\b").matcher(c.getFechaCreacionTimestamp());
            if (mTs.find() && mTs.group(1) != null) {
                try {
                    anio = Integer.parseInt(mTs.group(1));
                } catch (Exception ignored) {}
            }
        }

        // 3. Obtener año del propio código anterior si lo contenía
        if (anio <= 0) {
            anio = extraerAnio(numActual);
        }

        // 4. Fallback al año actual
        if (anio <= 0) {
            anio = Calendar.getInstance().get(Calendar.YEAR);
        }

        return com.example.maquirentapp.strategy.StrategyProvider.getCorrelativoStrategy()
                .generarCodigoCotizacion(sec, anio);
    }
    public static int extraerAnioDeFecha(String fechaTexto) {
        if (fechaTexto == null || fechaTexto.trim().isEmpty()) {
            return -1;
        }
        try {
            Matcher m = Pattern.compile("\\b(20\\d{2})\\b").matcher(fechaTexto);
            if (m.find() && m.group(1) != null) {
                return Integer.parseInt(m.group(1));
            }
        } catch (Exception ignored) {}
        return -1;
    }

    public static long obtenerTimestampCronologico(Cotizacion c) {
        if (c == null) return 0L;

        // 1. Intentar fechaCreacionTimestamp si es numérico (milisegundos) o fecha ISO
        String tsStr = c.getFechaCreacionTimestamp();
        if (tsStr != null && !tsStr.trim().isEmpty()) {
            try {
                return Long.parseLong(tsStr.trim());
            } catch (NumberFormatException ignored) {}

            String[] formatosConHora = {
                    "dd/MM/yyyy HH:mm:ss",
                    "dd/MM/yyyy HH:mm",
                    "yyyy-MM-dd'T'HH:mm:ss",
                    "yyyy-MM-dd HH:mm:ss"
            };
            for (String formato : formatosConHora) {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat(formato, Locale.getDefault());
                    Date d = sdf.parse(tsStr.trim());
                    if (d != null) return d.getTime();
                } catch (Exception ignored) {}
            }
        }

        // 2. Intentar fechaEmision (ej. 24/02/2026 o 2026-02-24)
        String fechaEm = c.getFechaEmision();
        if (fechaEm != null && !fechaEm.trim().isEmpty()) {
            String[] formatosFecha = {
                    "dd/MM/yyyy",
                    "yyyy-MM-dd",
                    "dd-MM-yyyy"
            };
            for (String formato : formatosFecha) {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat(formato, Locale.getDefault());
                    Date d = sdf.parse(fechaEm.trim());
                    if (d != null) {
                        long sec = extraerNumeroSecuencia(c.getNumeroCotizacion());
                        return d.getTime() + (sec > 0 ? sec * 1000 : 0);
                    }
                } catch (Exception ignored) {}
            }
        }

        // 3. Fallback: extraer año del código
        int anio = extraerAnio(c.getNumeroCotizacion());
        Calendar cal = Calendar.getInstance();
        cal.set(anio, Calendar.JANUARY, 1, 0, 0, 0);
        long sec = extraerNumeroSecuencia(c.getNumeroCotizacion());
        return cal.getTimeInMillis() + (sec > 0 ? sec * 1000 : 0);
    }

    public static boolean detectarDuplicadosONecesidadMigracion(List<Cotizacion> lista) {
        if (lista == null || lista.isEmpty()) return false;
        Set<String> codigosVistos = new HashSet<>();
        for (Cotizacion c : lista) {
            String codigo = c.getNumeroCotizacion();
            if (codigo == null || codigo.trim().isEmpty() || necesitaMigracion(codigo)) {
                return true;
            }
            if (!codigosVistos.add(codigo.trim())) {
                return true; // Código duplicado encontrado
            }
        }
        return false;
    }

    public void reordenarYRenumerarCronologicamente(List<Cotizacion> lista) {
        if (lista == null || lista.isEmpty()) return;

        // 1. Agrupar cotizaciones por año
        Map<Integer, List<Cotizacion>> porAnio = new HashMap<>();
        for (Cotizacion c : lista) {
            int anio = extraerAnioDeFecha(c.getFechaEmision());
            if (anio <= 0) {
                anio = extraerAnio(c.getNumeroCotizacion());
            }
            if (anio <= 0) {
                anio = Calendar.getInstance().get(Calendar.YEAR);
            }

            if (!porAnio.containsKey(anio)) {
                porAnio.put(anio, new ArrayList<>());
            }
            porAnio.get(anio).add(c);
        }

        WriteBatch batch = db.batch();
        int opsInBatch = 0;
        int totalModificados = 0;
        Map<String, Object> secUpdates = new HashMap<>();

        // 2. Para cada año, ordenar cronológicamente de la más antigua a la más reciente
        for (Map.Entry<Integer, List<Cotizacion>> entry : porAnio.entrySet()) {
            int anio = entry.getKey();
            List<Cotizacion> cotizacionesAnio = entry.getValue();

            Collections.sort(cotizacionesAnio, (c1, c2) -> {
                long t1 = obtenerTimestampCronologico(c1);
                long t2 = obtenerTimestampCronologico(c2);
                if (t1 != t2) {
                    return Long.compare(t1, t2); // Más antigua primero (menor timestamp)
                }
                long s1 = extraerNumeroSecuencia(c1.getNumeroCotizacion());
                long s2 = extraerNumeroSecuencia(c2.getNumeroCotizacion());
                if (s1 != s2 && s1 > 0 && s2 > 0) {
                    return Long.compare(s1, s2);
                }
                String id1 = c1.getId() != null ? c1.getId() : "";
                String id2 = c2.getId() != null ? c2.getId() : "";
                return id1.compareTo(id2);
            });

            // 3. Renumerar secuencialmente: 1, 2, 3, ... N
            long secuencia = 1;
            for (Cotizacion c : cotizacionesAnio) {
                String nuevoCodigo = com.example.maquirentapp.strategy.StrategyProvider.getCorrelativoStrategy()
                        .generarCodigoCotizacion(secuencia, anio);

                if (!nuevoCodigo.equals(c.getNumeroCotizacion())) {
                    c.setNumeroCotizacion(nuevoCodigo);
                    batch.update(db.collection(COL_COTIZACIONES).document(c.getId()), "numeroCotizacion", nuevoCodigo);
                    opsInBatch++;
                    totalModificados++;

                    if (opsInBatch >= 450) {
                        batch.commit();
                        batch = db.batch();
                        opsInBatch = 0;
                    }
                }
                secuencia++;
            }

            long ultimoContadorAnio = cotizacionesAnio.size();
            secUpdates.put(FIELD_ULTIMA_COT + "_" + anio, ultimoContadorAnio);
            if (anio == Calendar.getInstance().get(Calendar.YEAR)) {
                secUpdates.put(FIELD_ULTIMA_COT, ultimoContadorAnio);
            }
        }

        // 4. Actualizar el contador en configuracion/secuencias
        if (!secUpdates.isEmpty()) {
            batch.set(db.collection(COL_CONFIG).document(DOC_SECUENCIAS), secUpdates, SetOptions.merge());
            opsInBatch++;
        }

        if (opsInBatch > 0) {
            final int modCount = totalModificados;
            batch.commit()
                    .addOnSuccessListener(aVoid -> Log.d("CotizacionesRepo", "Reordenamiento cronológico completado con éxito. Modificados: " + modCount))
                    .addOnFailureListener(e -> Log.e("CotizacionesRepo", "Error al confirmar reordenamiento cronológico", e));
        }
    }

    public void actualizarCotizacion(Cotizacion cotizacion, Callback<Void> callback) {
        db.collection("cotizaciones").document(cotizacion.getId())
                .set(cotizacion)
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(callback::onError);
    }
}