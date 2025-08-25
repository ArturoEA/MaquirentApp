package com.example.maquirentapp.Model;

public class GrupoElectrogeno {
    private String id;
    private String codigo;
    private String foto;
    public GrupoElectrogeno() {}
    public GrupoElectrogeno(String codigo, String foto) {
        this.codigo = codigo;
        this.foto = foto;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getFoto() {
        return foto;
    }

    public void setFoto(String foto) {
        this.foto = foto;
    }
}
