package com.example.maquirentapp.adaptadores;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.net.ParseException;
import androidx.recyclerview.widget.RecyclerView;

import com.example.maquirentapp.Model.AlquilerMensual;
import com.example.maquirentapp.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AlquilerMensualAdapter extends RecyclerView.Adapter<AlquilerMensualAdapter.VH>{
    private final List<AlquilerMensual> items = new ArrayList<>();

    public void setItems(List<AlquilerMensual> nuevos) {
        items.clear();
        items.addAll(nuevos);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_alquiler_mensual, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        AlquilerMensual a = items.get(pos);

        h.txtEmpresa.setText(a.getNombreCliente());
        h.txtUbicacion.setText(a.getUbicacion());
//        h.txtFechaInicial.setText(a.getFechaInicial());
//        h.txtFechaFinal.setText(a.getFechaFinal());
        h.txtHorasInicio.setText(a.getHorometroInicial() + " horas");
        h.txtHorasFinal.setText(a.getHorometroFinal() + " horas");

        String fechaIso = a.getFechaInicial(); // "2025-04-23T00:00:00"
        String fechaFormateada;
        try {
            // 1) Parsear el ISO
            SimpleDateFormat isoFormat =
                    new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            Date date = isoFormat.parse(fechaIso);

            // 2) Formatear al nuevo estilo
            SimpleDateFormat targetFormat =
                    new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
            fechaFormateada = targetFormat.format(date);
        } catch (ParseException e) {
            // si algo falla, muestro el original o una cadena vacía
            fechaFormateada = fechaIso.substring(0, 10); // "2025-04-23"
        } catch (java.text.ParseException e) {
            throw new RuntimeException(e);
        }
        h.txtFechaInicial.setText(fechaFormateada);

        String fechaIso2 = a.getFechaInicial(); // "2025-04-23T00:00:00"
        String fechaFormateada2;
        try {
            // 1) Parsear el ISO
            SimpleDateFormat isoFormat =
                    new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            Date date = isoFormat.parse(fechaIso2);

            // 2) Formatear al nuevo estilo
            SimpleDateFormat targetFormat =
                    new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
            fechaFormateada2 = targetFormat.format(date);
        } catch (ParseException e) {
            fechaFormateada2 = fechaIso2.substring(0, 10); // "2025-04-23"
        } catch (java.text.ParseException e) {
            throw new RuntimeException(e);
        }
        h.txtFechaFinal.setText(fechaFormateada2);

        // Limpia los íconos anteriores
        h.contenedorAccesorios.removeAllViews();

        // Lógica para añadir accesorios si están en true (máximo 6)
        int maxIcons = 6;
        int count = 0;

        if (a.isExtintor9kg() && count < maxIcons) {
            h.contenedorAccesorios.addView(crearIcono(h, R.drawable.icon_extintor_blanco));
            count++;
        }
        if (a.isExtintor6kg() && count < maxIcons) {
            h.contenedorAccesorios.addView(crearIcono(h, R.drawable.icon_extintor_blanco));
            count++;
        }
        if (a.isVarillaTierra() && count < maxIcons) {
            h.contenedorAccesorios.addView(crearIcono(h, R.drawable.icon_varilla_blanco));
            count++;
        }
        if (a.isBandejaAntiderrame() && count < maxIcons) {
            h.contenedorAccesorios.addView(crearIcono(h, R.drawable.icon_bandeja_blanco));
            count++;
        }
        if (a.isKitAntiderrame() && count < maxIcons) {
            h.contenedorAccesorios.addView(crearIcono(h, R.drawable.icon_kit_blanco));
            count++;
        }
        if (a.isCableElectrico() && count < maxIcons) {
            h.contenedorAccesorios.addView(crearIcono(h, R.drawable.icon_cable_blanco));
            count++;
        }
        if (a.isTableroDistribucion() && count < maxIcons) {
            h.contenedorAccesorios.addView(crearIcono(h, R.drawable.icon_tablero_blanco));
            count++;
        }
        if (a.isCarreta() && count < maxIcons) {
            h.contenedorAccesorios.addView(crearIcono(h, R.drawable.icon_remolque_blanco));
            count++;
        }
    }

    @Override public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView txtEmpresa, txtUbicacion, txtFechaInicial, txtFechaFinal, txtHorasInicio, txtHorasFinal;
        LinearLayout contenedorAccesorios;

        VH(View item) {
            super(item);
            txtEmpresa = item.findViewById(R.id.txtEmpresa);
            txtUbicacion     = item.findViewById(R.id.txtUbicacion);
            txtFechaInicial  = item.findViewById(R.id.txtFechaInicial);
            txtFechaFinal    = item.findViewById(R.id.txtFechaFinal);
            txtHorasInicio   = item.findViewById(R.id.txtHorasInicio);
            txtHorasFinal    = item.findViewById(R.id.txtHorasFinal);
            contenedorAccesorios = item.findViewById(R.id.contenedorAccesorios);
        }
    }

    private ImageView crearIcono(VH h, int drawableId) {
        ImageView icon = new ImageView(h.itemView.getContext());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(110, 110);
        params.setMargins(0, 0, 15, 0);
        icon.setLayoutParams(params);
        icon.setImageResource(drawableId);
        return icon;
    }

}
