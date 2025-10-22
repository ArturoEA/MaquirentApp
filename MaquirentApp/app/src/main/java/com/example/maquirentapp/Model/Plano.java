package com.example.maquirentapp.Model;

public class Plano {
    private String id;
    private String imageUrl; // Puede ser URL o URI local
    private String nombre;

    public Plano(String id, String imageUrl, String nombre) {
        this.id = id;
        this.imageUrl = imageUrl;
        this.nombre = nombre;
    }

    // Getters
    public String getId() { return id; }
    public String getImageUrl() { return imageUrl; }
    public String getNombre() { return nombre; }
}