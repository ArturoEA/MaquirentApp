package com.example.maquirentapp.Utils;

import android.content.Context;
import android.util.Log;

import com.example.maquirentapp.Model.GrupoElectrogeno;
import com.example.maquirentapp.Model.Mantenimiento;
import com.example.maquirentapp.Model.MantenimientoConfiguracion;
import com.example.maquirentapp.Model.Tarea;
import com.example.maquirentapp.Repository.Callback;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Gestor centralizado de la salud de maquinaria, cálculo de intervalos independientes,
 * semáforos, validación de horómetro antierrores y generación automática de tareas (SOP).
 */
public class MantenimientoSaludManager {

    private static final String TAG = "MantenimientoSalud";
    public static final double INTERVALO_ESTANDAR_ACEITE = 250.0;
    public static final double UMBRAL_PROXIMO_ACEITE = 200.0;
    public static final int MESES_MAXIMO_ACEITE = 6;
    public static final double UMBRAL_SALTO_GRANDE_HORAS = 500.0;

    public enum EstadoSalud {
        OPTIMO,          // Al día (< 200 h y < 6 meses)
        PROXIMO,         // Próximo (200 a 249 h o 5 a 6 meses)
        VENCIDO_HORAS,   // Vencido por horas (>= 250 h)
        VENCIDO_TIEMPO   // Vencido por tiempo (>= 6 meses)
    }

    public static class ResultadoSalud {
        public EstadoSalud estado = EstadoSalud.OPTIMO;
        public String componenteCritico = "Aceite de motor";
        public double horometroActual = 0.0;
        public double ultimoHorometroServicio = 0.0;
        public double horasTranscurridas = 0.0;
        public double horasRestantes = 0.0;
        public double horasExceso = 0.0;
        public int mesesTranscurridos = 0;
        public String badgeTexto = "";
        public String colorHex = "#4CAF50"; // Verde por defecto

        public boolean isVencido() {
            return estado == EstadoSalud.VENCIDO_HORAS || estado == EstadoSalud.VENCIDO_TIEMPO;
        }

        public String getMotivo() {
            if (estado == EstadoSalud.VENCIDO_HORAS) {
                return String.format(Locale.US, "Mantenimiento vencido por horas (+%.0f h de exceso). Horómetro actual: %.1f h.", horasExceso, horometroActual);
            } else if (estado == EstadoSalud.VENCIDO_TIEMPO) {
                return String.format(Locale.US, "Mantenimiento vencido por tiempo (%d meses transcurridos sin cambio de aceite).", mesesTranscurridos);
            } else if (estado == EstadoSalud.PROXIMO) {
                return String.format(Locale.US, "Mantenimiento próximo a vencer (faltan %.0f horas).", horasRestantes);
            }
            return "Mantenimiento al día.";
        }
    }

    public static class ResultadoValidacionHorometro {
        public boolean esValido = true;
        public boolean esSaltoGrande = false;
        public double incremento = 0.0;
        public String mensajeError = "";
    }

    public static class MetricasPuntualidad {
        public double intervaloPromedio = 0.0;
        public double indicePuntualidad = 100.0; // 0% a 100%
        public int totalMantenimientos = 0;
        public int mantenimientosATiempo = 0;
        public int mantenimientosConRetraso = 0;
        public String diagnostico = "Sin datos suficientes";
        public List<PuntoHistorialIntervalo> intervalos = new ArrayList<>();
    }

    public static class PuntoHistorialIntervalo {
        public String fecha;
        public double horometro;
        public double deltaHoras;
        public boolean esATiempo;

        public PuntoHistorialIntervalo(String fecha, double horometro, double deltaHoras, boolean esATiempo) {
            this.fecha = fecha;
            this.horometro = horometro;
            this.deltaHoras = deltaHoras;
            this.esATiempo = esATiempo;
        }
    }

    // =========================================================================
    // 1. CÁLCULO DE SALUD Y SEMÁFORO
    // =========================================================================

    public static ResultadoSalud evaluarSalud(GrupoElectrogeno grupo, List<Mantenimiento> listaMantenimientos) {
        ResultadoSalud res = new ResultadoSalud();

        double horometroActual = grupo != null ? grupo.getHorometroActual() : 0.0;
        double ultimoHorometroServicio = 0.0;
        long ultimaFechaServicio = 0;

        // 1. Buscar último horómetro y fecha de aceite en relojes independientes
        if (grupo != null && grupo.getRelojesComponentes().containsKey("aceite")) {
            Double val = grupo.getRelojesComponentes().get("aceite");
            if (val != null) ultimoHorometroServicio = val;
        }
        if (grupo != null && grupo.getFechasComponentes().containsKey("aceite")) {
            Long valF = grupo.getFechasComponentes().get("aceite");
            if (valF != null) ultimaFechaServicio = valF;
        }

        // 2. Si no estaba en el reloj del grupo, deducirlo del historial de mantenimientos
        if (ultimoHorometroServicio <= 0 && listaMantenimientos != null && !listaMantenimientos.isEmpty()) {
            Mantenimiento masReciente = null;
            for (Mantenimiento m : listaMantenimientos) {
                double h = parseDoubleSafe(m.getHorometro());
                if (masReciente == null || h > parseDoubleSafe(masReciente.getHorometro())) {
                    masReciente = m;
                }
            }
            if (masReciente != null) {
                ultimoHorometroServicio = parseDoubleSafe(masReciente.getHorometro());
                ultimaFechaServicio = masReciente.getFechaCreacion();
                if (horometroActual < ultimoHorometroServicio) {
                    horometroActual = ultimoHorometroServicio;
                }
            }
        }

        res.horometroActual = horometroActual;
        res.ultimoHorometroServicio = ultimoHorometroServicio;

        double horasUso = Math.max(0, horometroActual - ultimoHorometroServicio);
        res.horasTranscurridas = horasUso;

        // Calcular meses transcurridos
        if (ultimaFechaServicio > 0) {
            long diffMillis = System.currentTimeMillis() - ultimaFechaServicio;
            res.mesesTranscurridos = (int) (diffMillis / (1000L * 60 * 60 * 24 * 30));
        }

        // Determinar estado
        if (horasUso >= INTERVALO_ESTANDAR_ACEITE) {
            res.estado = EstadoSalud.VENCIDO_HORAS;
            res.horasExceso = horasUso - INTERVALO_ESTANDAR_ACEITE;
            res.colorHex = "#F44336"; // Rojo
            res.badgeTexto = "Vencido";
        } else if (res.mesesTranscurridos >= MESES_MAXIMO_ACEITE) {
            res.estado = EstadoSalud.VENCIDO_TIEMPO;
            res.colorHex = "#F44336"; // Rojo
            res.badgeTexto = "Vencido";
        } else if (horasUso >= UMBRAL_PROXIMO_ACEITE || res.mesesTranscurridos >= 5) {
            res.estado = EstadoSalud.PROXIMO;
            res.horasRestantes = Math.max(0, INTERVALO_ESTANDAR_ACEITE - horasUso);
            res.colorHex = "#FFC107"; // Amarillo
            res.badgeTexto = "Próximo";
        } else {
            res.estado = EstadoSalud.OPTIMO;
            res.horasRestantes = Math.max(0, INTERVALO_ESTANDAR_ACEITE - horasUso);
            res.colorHex = "#4CAF50"; // Verde
            res.badgeTexto = "Al día";
        }

        return res;
    }

    public static void evaluarSalud(String idGrupo, Callback<ResultadoSalud> callback) {
        if (idGrupo == null || idGrupo.isEmpty()) {
            if (callback != null) callback.onError(new IllegalArgumentException("ID de grupo no válido"));
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("gruposElectrogenos").document(idGrupo).get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.exists()) {
                        if (callback != null) callback.onError(new Exception("Grupo no encontrado"));
                        return;
                    }
                    GrupoElectrogeno grupo = snapshot.toObject(GrupoElectrogeno.class);
                    if (grupo != null) grupo.setId(snapshot.getId());

                    db.collection("mantenimientos")
                            .whereEqualTo("idGrupo", idGrupo)
                            .get()
                            .addOnSuccessListener(mSnapshot -> {
                                List<Mantenimiento> mantenimientos = new ArrayList<>();
                                for (QueryDocumentSnapshot doc : mSnapshot) {
                                    mantenimientos.add(doc.toObject(Mantenimiento.class));
                                }
                                ResultadoSalud res = evaluarSalud(grupo, mantenimientos);
                                if (callback != null) callback.onSuccess(res);
                            })
                            .addOnFailureListener(e -> {
                                ResultadoSalud res = evaluarSalud(grupo, Collections.emptyList());
                                if (callback != null) callback.onSuccess(res);
                            });
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e);
                });
    }

    // =========================================================================
    // 2. VALIDACIÓN DE HORÓMETRO ANTIERRORES
    // =========================================================================

    public static ResultadoValidacionHorometro validarHorometro(double horometroAnterior, double nuevoHorometro) {
        return validarHorometro(horometroAnterior, nuevoHorometro, false);
    }

    public static ResultadoValidacionHorometro validarHorometroMantenimiento(double horometroActual, double horometroMantenimiento) {
        return validarHorometro(horometroActual, horometroMantenimiento, true);
    }

    private static ResultadoValidacionHorometro validarHorometro(double horometroAnterior, double nuevoHorometro, boolean permitirMenor) {
        ResultadoValidacionHorometro res = new ResultadoValidacionHorometro();

        if (!permitirMenor && nuevoHorometro < horometroAnterior) {
            res.esValido = false;
            res.mensajeError = String.format(Locale.US,
                    "El horómetro ingresado (%.1f h) es menor al último registrado (%.1f h). Por favor corrige el número.",
                    nuevoHorometro, horometroAnterior);
            return res;
        }

        res.incremento = nuevoHorometro - horometroAnterior;

        if (horometroAnterior > 0 && res.incremento > UMBRAL_SALTO_GRANDE_HORAS) {
            res.esSaltoGrande = true;
            res.mensajeError = String.format(Locale.US,
                    "El nuevo horómetro (%.1f h) representa un incremento de %.1f horas con respecto al anterior (%.1f h).\n\n¿Confirmas que es correcto o hubo un error de tipeo?",
                    nuevoHorometro, res.incremento, horometroAnterior);
        }

        return res;
    }

    // =========================================================================
    // 3. MÉTRICAS E ÍNDICE DE PUNTUALIDAD
    // =========================================================================

    public static MetricasPuntualidad calcularMetricasPuntualidad(List<Mantenimiento> mantenimientos) {
        MetricasPuntualidad metricas = new MetricasPuntualidad();
        if (mantenimientos == null || mantenimientos.size() < 2) {
            metricas.diagnostico = "Se requieren al menos 2 mantenimientos registrados para calcular el índice de puntualidad.";
            return metricas;
        }

        // Ordenar por horómetro ascendente
        List<Mantenimiento> lista = new ArrayList<>(mantenimientos);
        Collections.sort(lista, (m1, m2) -> Double.compare(
                parseDoubleSafe(m1.getHorometro()),
                parseDoubleSafe(m2.getHorometro())
        ));

        double sumaIntervalos = 0.0;
        int cantidadIntervalos = 0;
        int aTiempo = 0;
        int conRetraso = 0;

        for (int i = 1; i < lista.size(); i++) {
            double hAnterior = parseDoubleSafe(lista.get(i - 1).getHorometro());
            double hActual = parseDoubleSafe(lista.get(i).getHorometro());
            double delta = hActual - hAnterior;

            if (delta > 0) {
                sumaIntervalos += delta;
                cantidadIntervalos++;

                // Consideramos a tiempo si no excede 260 horas (250 h + margen de tolerancia)
                boolean puntual = delta <= 260.0;
                if (puntual) aTiempo++;
                else conRetraso++;

                metricas.intervalos.add(new PuntoHistorialIntervalo(
                        lista.get(i).getFecha(),
                        hActual,
                        delta,
                        puntual
                ));
            }
        }

        metricas.totalMantenimientos = cantidadIntervalos;
        metricas.mantenimientosATiempo = aTiempo;
        metricas.mantenimientosConRetraso = conRetraso;

        if (cantidadIntervalos > 0) {
            metricas.intervaloPromedio = sumaIntervalos / cantidadIntervalos;
            metricas.indicePuntualidad = (aTiempo * 100.0) / cantidadIntervalos;

            if (metricas.indicePuntualidad >= 85.0) {
                metricas.diagnostico = "Excelente disciplina de mantenimiento. La máquina se atiende oportunamente.";
            } else if (metricas.indicePuntualidad >= 60.0) {
                metricas.diagnostico = "Puntualidad moderada. Se observan retrasos ocasionales entre servicios.";
            } else {
                metricas.diagnostico = "Tendencia a sobreuso recurrente. Alto riesgo de desgaste acelerado de motor.";
            }
        }

        return metricas;
    }

    // =========================================================================
    // 4. GENERACIÓN AUTOMÁTICA DE TAREAS (SOP)
    // =========================================================================

    public interface OnTareaGeneradaCallback {
        void onSuccess(String tareaId);
        void onError(Exception e);
    }

    public static void crearTareaPreventivaSiNoExiste(String codigoGrupo, double horometro,
                                                      List<String> itemsSugeridos, OnTareaGeneradaCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String titulo = "Mantenimiento Preventivo (250h) - " + codigoGrupo;

        // Verificar si ya existe una tarea pendiente para evitar duplicados
        db.collection("tareas")
                .whereEqualTo("codigoGrupo", codigoGrupo)
                .whereEqualTo("tipoTarea", "PREVENTIVO")
                .whereEqualTo("completada", false)
                .get()
                .addOnSuccessListener(query -> {
                    if (query != null && !query.isEmpty()) {
                        Log.d(TAG, "Ya existe una tarea preventiva pendiente para " + codigoGrupo);
                        if (callback != null) callback.onSuccess(query.getDocuments().get(0).getId());
                        return;
                    }

                    List<Map<String, Object>> checklist = new ArrayList<>();
                    List<String> listaItems = new ArrayList<>();
                    if (itemsSugeridos != null && !itemsSugeridos.isEmpty()) {
                        listaItems.addAll(itemsSugeridos);
                    } else {
                        listaItems.add("Cambio de aceite de motor (15W-40)");
                        listaItems.add("Cambio de filtro de aceite");
                        listaItems.add("Cambio / purga de filtro de petróleo");
                        listaItems.add("Limpieza / sopleteo de filtro de aire");
                        listaItems.add("Ajuste y torque de pernos de carreta y soportes");
                        listaItems.add("Medición de voltaje de batería (reposo y arranque)");
                        listaItems.add("Prueba de encendido y verificación de fugas");
                    }

                    for (String it : listaItems) {
                        Map<String, Object> itemMap = new HashMap<>();
                        itemMap.put("item", it);
                        itemMap.put("titulo", it);
                        itemMap.put("completado", false);
                        checklist.add(itemMap);
                    }

                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    Tarea tarea = new Tarea();
                    tarea.setTitulo(titulo);
                    tarea.setTipoTarea("PREVENTIVO");
                    tarea.setCodigoGrupo(codigoGrupo);
                    tarea.setFechaCreacion(sdf.format(new Date()));
                    tarea.setCompletada(false);
                    tarea.setAprobadoPorGerencia(false);
                    tarea.setChecklist(checklist);

                    db.collection("tareas").add(tarea)
                            .addOnSuccessListener(doc -> {
                                if (callback != null) callback.onSuccess(doc.getId());
                            })
                            .addOnFailureListener(e -> {
                                if (callback != null) callback.onError(e);
                            });
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e);
                });
    }

    public static void crearTareaRetornoCarreta(String codigoGrupo, OnTareaGeneradaCallback callback) {
        if (codigoGrupo == null || codigoGrupo.trim().isEmpty()) {
            if (callback != null) callback.onError(new IllegalArgumentException("Código de grupo vacío"));
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // 1. Obtener la máquina para consultar o actualizar su contador de retornos de alquiler
        db.collection("gruposElectrogenos")
                .whereEqualTo("codigo", codigoGrupo)
                .get()
                .addOnSuccessListener(queryGrupo -> {
                    if (queryGrupo == null || queryGrupo.isEmpty()) {
                        crearTareaRetornoFallback(codigoGrupo, 1, callback);
                        return;
                    }

                    com.google.firebase.firestore.DocumentSnapshot docGrupo = queryGrupo.getDocuments().get(0);
                    String docId = docGrupo.getId();
                    String tipoEquipo = docGrupo.getString("tipoEquipo");
                    if (tipoEquipo == null || tipoEquipo.trim().isEmpty()) {
                        tipoEquipo = "Grupo electrógeno";
                    }

                    long contadorActual = docGrupo.contains("contadorRetornos") && docGrupo.getLong("contadorRetornos") != null
                            ? docGrupo.getLong("contadorRetornos") : 0L;
                    long nuevoContador = contadorActual + 1;

                    // Actualizar el contador de retornos acumulados en la máquina
                    db.collection("gruposElectrogenos").document(docId).update("contadorRetornos", nuevoContador);

                    final long contadorFinal = nuevoContador;
                    final String tipoFinal = tipoEquipo;

                    // 2. Consultar mantenimientos_configuracion para obtener las tareas de retorno configuradas
                    db.collection("mantenimientos_configuracion")
                            .get()
                            .addOnSuccessListener(queryConfig -> {
                                List<String> itemsChecklist = new ArrayList<>();

                                if (queryConfig != null && !queryConfig.isEmpty()) {
                                    boolean esCompresora = tipoFinal.toLowerCase().contains("compresor");

                                    for (com.google.firebase.firestore.DocumentSnapshot docCfg : queryConfig.getDocuments()) {
                                        MantenimientoConfiguracion cfg = docCfg.toObject(MantenimientoConfiguracion.class);
                                        if (cfg == null) continue;

                                        String tipoCfg = cfg.getTipoEquipo() != null ? cfg.getTipoEquipo().toLowerCase().trim() : "";
                                        boolean pertenece = esCompresora
                                                ? tipoCfg.contains("compresor")
                                                : (tipoCfg.isEmpty() || tipoCfg.contains("grupo") || tipoCfg.contains("electr"));

                                        if (pertenece && (cfg.isEsInspeccionRetorno() || cfg.getFrecuenciaRetornos() > 0)) {
                                            int frec = cfg.getFrecuenciaRetornos() > 0 ? cfg.getFrecuenciaRetornos() : 1;
                                            if (contadorFinal % frec == 0) {
                                                itemsChecklist.add(cfg.getNombre());
                                            }
                                        }
                                    }
                                }

                                if (itemsChecklist.isEmpty()) {
                                    itemsChecklist.add("Ajuste y torque de pernos de carreta y muelles");
                                    itemsChecklist.add("Inspección de lanza de tiro y perno de acople");
                                    itemsChecklist.add("Revisión de presión y estado de neumáticos");
                                    itemsChecklist.add("Limpieza general y registro de horómetro final");
                                }

                                crearDocumentoTareaRetorno(codigoGrupo, contadorFinal, itemsChecklist, callback);
                            })
                            .addOnFailureListener(e -> {
                                crearTareaRetornoFallback(codigoGrupo, contadorFinal, callback);
                            });
                })
                .addOnFailureListener(e -> {
                    crearTareaRetornoFallback(codigoGrupo, 1, callback);
                });
    }

    private static void crearDocumentoTareaRetorno(String codigoGrupo, long numeroRetorno,
                                                   List<String> items, OnTareaGeneradaCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String titulo = "Inspección de Retorno de Carreta (Retorno #" + numeroRetorno + ") - " + codigoGrupo;

        List<Map<String, Object>> checklist = new ArrayList<>();
        for (String it : items) {
            Map<String, Object> itemMap = new HashMap<>();
            itemMap.put("item", it);
            itemMap.put("titulo", it);
            itemMap.put("completado", false);
            checklist.add(itemMap);
        }

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        Tarea tarea = new Tarea();
        tarea.setTitulo(titulo);
        tarea.setTipoTarea("RETORNO");
        tarea.setCodigoGrupo(codigoGrupo);
        tarea.setFechaCreacion(sdf.format(new Date()));
        tarea.setCompletada(false);
        tarea.setAprobadoPorGerencia(false);
        tarea.setChecklist(checklist);

        db.collection("tareas").add(tarea)
                .addOnSuccessListener(doc -> {
                    if (callback != null) callback.onSuccess(doc.getId());
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e);
                });
    }

    private static void crearTareaRetornoFallback(String codigoGrupo, long numeroRetorno, OnTareaGeneradaCallback callback) {
        List<String> items = new ArrayList<>();
        items.add("Ajuste y torque de pernos de carreta y muelles");
        items.add("Ajuste de pernos de soportes antivibratorios motor/alternador");
        items.add("Inspección de lanza de tiro y perno de acople");
        items.add("Revisión de presión y estado de neumáticos");
        items.add("Purga de agua en trampa de combustible Racor");
        items.add("Limpieza general y registro de horómetro final");
        crearDocumentoTareaRetorno(codigoGrupo, numeroRetorno, items, callback);
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    public static double parseDoubleSafe(String val) {
        if (val == null || val.trim().isEmpty()) return 0.0;
        try {
            return Double.parseDouble(val.trim().replace(",", "."));
        } catch (Exception e) {
            return 0.0;
        }
    }
}
