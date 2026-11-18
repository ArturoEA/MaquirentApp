package com.example.maquirentapp.Model;

public class MantenimientoConfiguracion {
    private String id;
    private String nombre;
    private String icono;
    private long fechaCreacion;
    private String tipoEquipo = "Grupo electrógeno";
    private String subsistema = "Lubricación";
    private int intervaloHoras = 250;
    private int mesesMaximos = 6;
    private String unidadMedida = "HORAS";
    private String componenteKey = "general";
    private boolean esInspeccionRetorno = false;
    private int frecuenciaRetornos = 1;

    public MantenimientoConfiguracion() {
    }

    public MantenimientoConfiguracion(String nombre, String icono) {
        this.nombre = nombre;
        this.icono = icono;
        this.fechaCreacion = System.currentTimeMillis();
    }

    public MantenimientoConfiguracion(String nombre, String icono, String tipoEquipo, String subsistema,
                                      int intervaloHoras, int mesesMaximos, String componenteKey,
                                      boolean esInspeccionRetorno, int frecuenciaRetornos) {
        this.nombre = nombre;
        this.icono = icono;
        this.tipoEquipo = tipoEquipo;
        this.subsistema = subsistema;
        this.intervaloHoras = intervaloHoras;
        this.mesesMaximos = mesesMaximos;
        this.componenteKey = componenteKey;
        this.esInspeccionRetorno = esInspeccionRetorno;
        this.frecuenciaRetornos = frecuenciaRetornos;
        this.fechaCreacion = System.currentTimeMillis();
    }

    public MantenimientoConfiguracion(String nombre, String icono, String tipoEquipo, int intervaloHoras, String componenteKey) {
        this.nombre = nombre;
        this.icono = icono;
        this.tipoEquipo = tipoEquipo;
        this.intervaloHoras = intervaloHoras;
        this.componenteKey = componenteKey;
        this.fechaCreacion = System.currentTimeMillis();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getIcono() {
        return icono;
    }

    public void setIcono(String icono) {
        this.icono = icono;
    }

    public long getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(long fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public String getTipoEquipo() {
        return tipoEquipo;
    }

    public void setTipoEquipo(String tipoEquipo) {
        this.tipoEquipo = tipoEquipo;
    }

    public int getIntervaloHoras() {
        return intervaloHoras;
    }

    public void setIntervaloHoras(int intervaloHoras) {
        this.intervaloHoras = intervaloHoras;
    }

    public String getUnidadMedida() {
        return unidadMedida;
    }

    public void setUnidadMedida(String unidadMedida) {
        this.unidadMedida = unidadMedida;
    }

    public String getComponenteKey() {
        return componenteKey;
    }

    public void setComponenteKey(String componenteKey) {
        this.componenteKey = componenteKey;
    }

    public String getClaveRelojComponente() {
        return componenteKey;
    }

    public void setClaveRelojComponente(String clave) {
        this.componenteKey = clave;
    }

    public boolean isEsInspeccionRetorno() {
        return esInspeccionRetorno;
    }

    public void setEsInspeccionRetorno(boolean esInspeccionRetorno) {
        this.esInspeccionRetorno = esInspeccionRetorno;
    }

    public String getSubsistema() {
        return subsistema;
    }

    public void setSubsistema(String subsistema) {
        this.subsistema = subsistema;
    }

    public int getMesesMaximos() {
        return mesesMaximos;
    }

    public void setMesesMaximos(int mesesMaximos) {
        this.mesesMaximos = mesesMaximos;
    }

    public int getFrecuenciaRetornos() {
        return frecuenciaRetornos;
    }

    public void setFrecuenciaRetornos(int frecuenciaRetornos) {
        this.frecuenciaRetornos = frecuenciaRetornos;
    }
}
