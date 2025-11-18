package com.example.maquirentapp.Access;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.maquirentapp.Model.AlquilerDia;
import com.example.maquirentapp.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AlquilerDiarioAdapter extends RecyclerView.Adapter<AlquilerDiarioAdapter.ViewHolder> {

    private List<AlquilerDia> items = new ArrayList<>();
    private OnAlquilerDiaClickListener listener;
    private Map<String, String> accesoriosMap;
    private Context context;

    public interface OnAlquilerDiaClickListener {
        void onAlquilerClick(AlquilerDia alquiler);
    }

    public AlquilerDiarioAdapter(OnAlquilerDiaClickListener listener, Map<String, String> accesoriosMap) {
        this.listener = listener;
        this.accesoriosMap = accesoriosMap;
    }

    public void setItems(List<AlquilerDia> nuevos) {
        this.items.clear();
        this.items.addAll(nuevos);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        this.context = parent.getContext();
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_alquiler_diario, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AlquilerDia alquiler = items.get(position);
        holder.bind(alquiler, listener, accesoriosMap, context);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvCliente, tvPrecio, tvEstado, txtUbicacion,
                txtHorasInicio, txtHorasFinal, txtFechaInicial, txtFechaFinal;
        LinearLayout contenedorAccesorios;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCliente = itemView.findViewById(R.id.tvCliente);
            tvPrecio = itemView.findViewById(R.id.tvPrecio);
            tvEstado = itemView.findViewById(R.id.tvEstado);
            txtUbicacion = itemView.findViewById(R.id.txtUbicacion);
            txtHorasInicio = itemView.findViewById(R.id.txtHorasInicio);
            txtHorasFinal = itemView.findViewById(R.id.txtHorasFinal);
            txtFechaFinal = itemView.findViewById(R.id.txtFechaFinal);
            txtFechaInicial = itemView.findViewById(R.id.txtFechaInicial);
            contenedorAccesorios = itemView.findViewById(R.id.contenedorAccesorios);
        }

        public void bind(AlquilerDia alquiler, OnAlquilerDiaClickListener listener,
                         Map<String, String> accesoriosMap, Context context) {

            tvCliente.setText(alquiler.getNombreCliente());
            txtUbicacion.setText(alquiler.getUbicacion());
            txtFechaInicial.setText(alquiler.getFechaInicial());
            txtHorasInicio.setText(String.format(Locale.US, "%.1f horas", alquiler.getHorometroInicial()));

            txtFechaFinal.setText(alquiler.getFechaFinal() != null && !alquiler.getFechaFinal().isEmpty() ?
                    alquiler.getFechaFinal() : "---");

            txtHorasFinal.setText(alquiler.getHorometroFinal() > 0 ?
                    String.format(Locale.US, "%.1f horas", alquiler.getHorometroFinal()) : "---");

            String simbolo = "USD".equals(alquiler.getMoneda()) ? "$" : "S/.";
            tvPrecio.setText(String.format(Locale.US, "Monto: %s %.2f",
                    simbolo, alquiler.getPrecioTotal()));

            if (alquiler.isFinalizado()) {
                tvEstado.setText("Finalizado");
                tvEstado.setTextColor(ContextCompat.getColor(context, R.color.green_accent));
            } else {
                tvEstado.setText("Activo");
                tvEstado.setTextColor(ContextCompat.getColor(context, R.color.yellow_accent));
            }

            itemView.setOnClickListener(v -> listener.onAlquilerClick(alquiler));

            contenedorAccesorios.removeAllViews();
            if (alquiler.getAccesoriosIds() != null && !alquiler.getAccesoriosIds().isEmpty()) {
                contenedorAccesorios.setVisibility(View.VISIBLE);

                for (String id : alquiler.getAccesoriosIds()) {
                    String nombreAccesorio = accesoriosMap.get(id);
                    if (nombreAccesorio == null) continue;

                    String nombreIcono = "icon_" + nombreAccesorio.toLowerCase().replace(" ", "_") + "_blanco";

                    int resId = context.getResources().getIdentifier(nombreIcono, "drawable", context.getPackageName());

                    if (resId != 0) {
                        ImageView iv = new ImageView(context);
                        iv.setImageResource(resId);

                        int sizeInDp = 24;
                        int marginInDp = 4;
                        float scale = context.getResources().getDisplayMetrics().density;
                        int sizeInPixels = (int) (sizeInDp * scale + 0.5f);
                        int marginInPixels = (int) (marginInDp * scale + 0.5f);

                        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(sizeInPixels, sizeInPixels);
                        params.setMarginEnd(marginInPixels);
                        iv.setLayoutParams(params);

                        contenedorAccesorios.addView(iv);
                    }
                }
            } else {
                contenedorAccesorios.setVisibility(View.GONE);
            }
        }
    }
}