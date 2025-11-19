package com.example.maquirentapp.Access;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.maquirentapp.Model.Accesorio;
import com.example.maquirentapp.Model.AlquilerDia;
import com.example.maquirentapp.Network.FirebaseServicio;
import com.example.maquirentapp.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AlquilerDiarioAdapter extends RecyclerView.Adapter<AlquilerDiarioAdapter.ViewHolder> {

    private List<AlquilerDia> items = new ArrayList<>();
    private OnAlquilerDiaClickListener listener;
    private Context context;

    public interface OnAlquilerDiaClickListener {
        void onAlquilerClick(AlquilerDia alquiler);
    }

    public AlquilerDiarioAdapter(OnAlquilerDiaClickListener listener) {
        this.listener = listener;
    }

    public void setItems(List<AlquilerDia> nuevos) {
        this.items.clear();
        this.items.addAll(nuevos);
        notifyDataSetChanged();
    }

    public void removeItem(int position) {
        items.remove(position);
        notifyItemRemoved(position);
    }

    public AlquilerDia getItem(int position) {
        return items.get(position);
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
        holder.bind(alquiler, listener, context);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvCliente, tvPrecio, tvEstado, txtUbicacion,
                txtHorasInicio, txtHorasFinal, txtFechaInicial, txtFechaFinal, tvComentarios;
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
            tvComentarios = itemView.findViewById(R.id.tvComentarios);
            contenedorAccesorios = itemView.findViewById(R.id.contenedorAccesorios);
        }

        public void bind(AlquilerDia alquiler, OnAlquilerDiaClickListener listener, Context context) {

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

            if (alquiler.getComentarios() != null && !alquiler.getComentarios().isEmpty()) {
                tvComentarios.setVisibility(View.VISIBLE);
                tvComentarios.setText("Nota: " + alquiler.getComentarios());
            } else {
                tvComentarios.setVisibility(View.GONE);
            }

            itemView.setOnClickListener(v -> listener.onAlquilerClick(alquiler));

            contenedorAccesorios.removeAllViews();
            if (alquiler.getAccesoriosIds() != null && !alquiler.getAccesoriosIds().isEmpty()) {
                contenedorAccesorios.setVisibility(View.VISIBLE);
                FirebaseServicio firebaseServicio = new FirebaseServicio();

                for (String accesorioId : alquiler.getAccesoriosIds()) {
                    firebaseServicio.getAccesorioPorId(accesorioId, new FirebaseServicio.OnAccesorioLoadedListener() {
                        @Override
                        public void onSuccess(Accesorio accesorio) {
                            if (context == null) return;

                            ImageView icon = new ImageView(context);
                            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(110, 110);
                            params.setMargins(0, 0, 15, 0);
                            icon.setLayoutParams(params);

                            if (accesorio.getIcono() != null && !accesorio.getIcono().isEmpty()) {
                                Glide.with(context)
                                        .load(accesorio.getIcono())
                                        .placeholder(R.drawable.icon_extintor_blanco)
                                        .error(R.drawable.icon_extintor_blanco)
                                        .into(icon);
                            } else {
                                icon.setImageResource(R.drawable.icon_extintor_blanco);
                            }

                            contenedorAccesorios.addView(icon);
                        }

                        @Override
                        public void onError(Exception e) {
                            // Fallo silencioso
                        }
                    });
                }
            } else {
                contenedorAccesorios.setVisibility(View.GONE);
            }
        }
    }
}