package com.example.maquirentapp.adaptadores;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.maquirentapp.Model.Accesorio;
import com.example.maquirentapp.Model.AlquilerMensual;
import com.example.maquirentapp.Network.FirebaseServicio;
import com.example.maquirentapp.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AlquilerMensualAdapter extends RecyclerView.Adapter<AlquilerMensualAdapter.VH>{
    private final List<AlquilerMensual> items = new ArrayList<>();
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(AlquilerMensual alquiler);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setItems(List<AlquilerMensual> nuevos) {
        items.clear();
        items.addAll(nuevos);
        notifyDataSetChanged();
    }

    public AlquilerMensual getItem(int position) {
        return items.get(position);
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
        h.txtHorasInicio.setText(a.getHorometroInicial() + " horas");
        h.txtHorasFinal.setText(a.getHorometroFinal() + " horas");

        // Formatear fecha inicial
        String fechaIso = a.getFechaInicial();
        String fechaFormateada = formatearFecha(fechaIso);
        h.txtFechaInicial.setText(fechaFormateada.isEmpty() ? "-" : fechaFormateada);

        // Formatear fecha final
        String fechaIso2 = a.getFechaFinal();
        String fechaFormateada2 = formatearFecha(fechaIso2);
        h.txtFechaFinal.setText(fechaFormateada2.isEmpty() ? "-" : fechaFormateada2);

        // Limpia los íconos anteriores
        h.contenedorAccesorios.removeAllViews();

        // Cargar accesorios desde Firebase si existen IDs
        if (a.getAccesoriosIds() != null && !a.getAccesoriosIds().isEmpty()) {
            cargarAccesorios(h, a.getAccesoriosIds());
        }

        // Click en el item
        h.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(a);
            }
        });
    }

    private String formatearFecha(String fechaIso) {
        String fechaFormateada = "";
        if (fechaIso != null && !fechaIso.isEmpty()) {
            try {
                SimpleDateFormat isoFormat =
                        new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
                Date date = isoFormat.parse(fechaIso);

                SimpleDateFormat targetFormat =
                        new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
                fechaFormateada = targetFormat.format(date);
            } catch (java.text.ParseException e) {
                if (fechaIso.length() >= 10)
                    fechaFormateada = fechaIso.substring(0, 10);
                else
                    fechaFormateada = fechaIso;
            }
        }
        return fechaFormateada;
    }

    private void cargarAccesorios(VH h, List<String> accesoriosIds) {
        FirebaseServicio firebaseServicio = new FirebaseServicio();

        for (String accesorioId : accesoriosIds) {
            firebaseServicio.getAccesorioPorId(accesorioId, new FirebaseServicio.OnAccesorioLoadedListener() {
                @Override
                public void onSuccess(Accesorio accesorio) {
                    Context context = h.itemView.getContext();
                    if (context == null) return;
                    if (context instanceof Activity) {
                        if (((Activity) context).isDestroyed() ||
                                ((Activity) context).isFinishing()) {
                            return;
                        }
                    }
                    ImageView icon = new ImageView(h.itemView.getContext());
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(110, 110);
                    params.setMargins(0, 0, 15, 0);
                    icon.setLayoutParams(params);

                    if (accesorio.getIcono() != null && !accesorio.getIcono().isEmpty()) {
                        try {
                            Glide.with(context)
                                    .load(accesorio.getIcono())
                                    .placeholder(R.drawable.icon_extintor_blanco)
                                    .error(R.drawable.icon_extintor_blanco)
                                    .into(icon);
                        } catch (IllegalArgumentException e) {
                            return;
                        }
                    } else {
                        icon.setImageResource(R.drawable.icon_extintor_blanco);
                    }

                    h.contenedorAccesorios.addView(icon);
                }

                @Override
                public void onError(Exception e) {
                    // Silenciar error
                }
            });
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