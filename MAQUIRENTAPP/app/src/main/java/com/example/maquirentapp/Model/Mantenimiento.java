package com.example.maquirentapp.Model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Mantenimiento {
    private String id;
    private String codigoGrupo;
    private String empresa;
    private String horometro;
    private String fecha;
    private List<String> itemsRealizados;
    private String comentarios;
    private List<String> fotos;
    private long fechaCreacion;
    // --- DATOS OPCIONALES PARA INFORME PDF ---
    private String proximoHorometro;
    private String proximaFecha;
    private boolean tieneBandeja;
    private boolean tieneExtintor;
    private boolean tieneKit;
    private boolean tienePuestaTierra;
    private String cliente;
    private String lugar;
    private String estadoMaquina; // Operativa, Inoperativa, etc.
    private String ubicacionMaquina; // Taller del cliente, Campo, etc.
    private String tipoServicio; // Mantenimiento, Evaluación, etc.
    private String aceiteUtilizado;
    private String contactoCliente;
    private String fallaEncontrada; // Opcional
    private String causaFalla; // Opcional
    private String trabajosRealizados; // Texto auto-generado + notas manuales
    private String tecnicoFirmaId; // UID del usuario que firma
    private String supervisorFirmaId; // UID del supervisor (opcional)
    private Map<String, String> codigosFiltrosUsados;
    private String cantidadAceite;

    public Mantenimiento() {
        this.itemsRealizados = new ArrayList<>();
        this.fotos = new ArrayList<>();
    }

    public Mantenimiento(String codigoGrupo, String empresa, String horometro, String fecha,
                         List<String> itemsRealizados, String comentarios, List<String> fotos) {
        this.codigoGrupo = codigoGrupo;
        this.empresa = empresa;
        this.horometro = horometro;
        this.fecha = fecha;
        this.itemsRealizados = itemsRealizados != null ? itemsRealizados : new ArrayList<>();
        this.comentarios = comentarios;
        this.fotos = fotos != null ? fotos : new ArrayList<>();
        this.fechaCreacion = System.currentTimeMillis();
    }

    // Getters y Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCodigoGrupo() {
        return codigoGrupo;
    }

    public void setCodigoGrupo(String codigoGrupo) {
        this.codigoGrupo = codigoGrupo;
    }

    public String getEmpresa() {
        return empresa;
    }

    public void setEmpresa(String empresa) {
        this.empresa = empresa;
    }

    public String getHorometro() {
        return horometro;
    }

    public void setHorometro(String horometro) {
        this.horometro = horometro;
    }

    public String getFecha() {
        return fecha;
    }

    public void setFecha(String fecha) {
        this.fecha = fecha;
    }

    public List<String> getItemsRealizados() {
        return itemsRealizados;
    }

    public void setItemsRealizados(List<String> itemsRealizados) {
        this.itemsRealizados = itemsRealizados;
    }

    public String getComentarios() {
        return comentarios;
    }

    public void setComentarios(String comentarios) {
        this.comentarios = comentarios;
    }

    public List<String> getFotos() {
        return fotos;
    }

    public void setFotos(List<String> fotos) {
        this.fotos = fotos;
    }

    public long getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(long fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public boolean tieneFotos() {
        return fotos != null && !fotos.isEmpty();
    }

    public int getCantidadFotos() {
        return fotos != null ? fotos.size() : 0;
    }

    public String getProximoHorometro() {
        return proximoHorometro;
    }

    public void setProximoHorometro(String proximoHorometro) {
        this.proximoHorometro = proximoHorometro;
    }

    public String getSupervisorFirmaId() {
        return supervisorFirmaId;
    }

    public void setSupervisorFirmaId(String supervisorFirmaId) {
        this.supervisorFirmaId = supervisorFirmaId;
    }

    public String getTecnicoFirmaId() {
        return tecnicoFirmaId;
    }

    public void setTecnicoFirmaId(String tecnicoFirmaId) {
        this.tecnicoFirmaId = tecnicoFirmaId;
    }

    public String getFallaEncontrada() {
        return fallaEncontrada;
    }

    public void setFallaEncontrada(String fallaEncontrada) {
        this.fallaEncontrada = fallaEncontrada;
    }

    public String getCausaFalla() {
        return causaFalla;
    }

    public void setCausaFalla(String causaFalla) {
        this.causaFalla = causaFalla;
    }

    public String getTrabajosRealizados() {
        return trabajosRealizados;
    }

    public void setTrabajosRealizados(String trabajosRealizados) {
        this.trabajosRealizados = trabajosRealizados;
    }

    public String getContactoCliente() {
        return contactoCliente;
    }

    public void setContactoCliente(String contactoCliente) {
        this.contactoCliente = contactoCliente;
    }

    public String getAceiteUtilizado() {
        return aceiteUtilizado;
    }

    public void setAceiteUtilizado(String aceiteUtilizado) {
        this.aceiteUtilizado = aceiteUtilizado;
    }

    public String getTipoServicio() {
        return tipoServicio;
    }

    public void setTipoServicio(String tipoServicio) {
        this.tipoServicio = tipoServicio;
    }

    public String getUbicacionMaquina() {
        return ubicacionMaquina;
    }

    public void setUbicacionMaquina(String ubicacionMaquina) {
        this.ubicacionMaquina = ubicacionMaquina;
    }

    public String getEstadoMaquina() {
        return estadoMaquina;
    }

    public void setEstadoMaquina(String estadoMaquina) {
        this.estadoMaquina = estadoMaquina;
    }

    public String getLugar() {
        return lugar;
    }

    public void setLugar(String lugar) {
        this.lugar = lugar;
    }

    public String getCliente() {
        return cliente;
    }

    public void setCliente(String cliente) {
        this.cliente = cliente;
    }

    public boolean isTienePuestaTierra() {
        return tienePuestaTierra;
    }

    public void setTienePuestaTierra(boolean tienePuestaTierra) {
        this.tienePuestaTierra = tienePuestaTierra;
    }

    public boolean isTieneKit() {
        return tieneKit;
    }

    public void setTieneKit(boolean tieneKit) {
        this.tieneKit = tieneKit;
    }

    public boolean isTieneExtintor() {
        return tieneExtintor;
    }

    public void setTieneExtintor(boolean tieneExtintor) {
        this.tieneExtintor = tieneExtintor;
    }

    public boolean isTieneBandeja() {
        return tieneBandeja;
    }

    public void setTieneBandeja(boolean tieneBandeja) {
        this.tieneBandeja = tieneBandeja;
    }

    public String getProximaFecha() {
        return proximaFecha;
    }

    public void setProximaFecha(String proximaFecha) {
        this.proximaFecha = proximaFecha;
    }

    public Map<String, String> getCodigosFiltrosUsados() {
        return codigosFiltrosUsados;
    }

    public void setCodigosFiltrosUsados(Map<String, String> codigosFiltrosUsados) {
        this.codigosFiltrosUsados = codigosFiltrosUsados;
    }

    public String getCantidadAceite() {
        return cantidadAceite;
    }

    public void setCantidadAceite(String cantidadAceite) {
        this.cantidadAceite = cantidadAceite;
    }
}