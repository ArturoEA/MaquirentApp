package com.example.maquirentapp.Model;

public class GrupoElectrogeno {
    private String id;
    private String codigo;
    private String foto;
    private boolean eliminado = false;
    private long fechaEliminacion = 0;
    private String eliminadoPor = "";

    public GrupoElectrogeno() { }

    public GrupoElectrogeno(String codigo, String foto) {
        this.codigo = codigo;
        this.foto = foto;
    }

    // Getters y setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getFoto() { return foto; }
    public void setFoto(String foto) { this.foto = foto; }

    public boolean isEliminado() { return eliminado; }
    public void setEliminado(boolean eliminado) { this.eliminado = eliminado; }

    public long getFechaEliminacion() { return fechaEliminacion; }
    public void setFechaEliminacion(long fechaEliminacion) { this.fechaEliminacion = fechaEliminacion; }

    public String getEliminadoPor() { return eliminadoPor; }
    public void setEliminadoPor(String eliminadoPor) { this.eliminadoPor = eliminadoPor; }
}
