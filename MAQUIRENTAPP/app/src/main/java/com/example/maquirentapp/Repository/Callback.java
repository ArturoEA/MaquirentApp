package com.example.maquirentapp.Repository;

/**
 * Interfaz genérica para el manejo de respuestas asíncronas de datos.
 * Centraliza el patrón de éxito/error con tipado seguro en toda la arquitectura.
 *
 * @param <T> Tipo del dato retornado en caso de éxito. Para operaciones sin retorno, usar Void.
 */
public interface Callback<T> {
    void onSuccess(T result);
    void onError(Exception e);
}
