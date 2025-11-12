package com.example.maquirentapp.Access;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.maquirentapp.Model.DetalleMes;
import com.example.maquirentapp.R;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;

public class DetalleMesAdapter extends RecyclerView.Adapter<DetalleMesAdapter.ViewHolder> {
    private final List<DetalleMes> items = new ArrayList<>();
    private OnDetalleMesListener listener;
    private boolean modoSoloLectura = false;

    public interface OnDetalleMesListener {
        void onHorometroChanged(DetalleMes detalle, double nuevoHorometro);
        void onPagoMesConfirmado(DetalleMes detalle);
        void onPagoHEConfirmado(DetalleMes detalle);
        void onGenerarValorizacion(DetalleMes detalle);
    }

    public void setOnDetalleMesListener(OnDetalleMesListener listener) {
        this.listener = listener;
    }

    public void setItems(List<DetalleMes> nuevos) {
        items.clear();
        if (nuevos != null) {
            items.addAll(nuevos);
        }
        notifyDataSetChanged();
    }

    public void setModoSoloLectura(boolean soloLectura) {
        this.modoSoloLectura = soloLectura;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_detalle_mes, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DetalleMes detalle = items.get(position);

        holder.tvTituloPeriodo.setText(detalle.getTituloPeriodo());

        if (detalle.getFechaFin() != null && !detalle.getFechaFin().isEmpty()) {
            holder.textInputHorometro.setHint("Horómetro al " + detalle.getFechaFin());
        }

        if (detalle.getHorometro() > 0) {
            holder.inputHorometro.setText(String.valueOf(detalle.getHorometro()));
        } else {
            holder.inputHorometro.setText("");
        }

        if (detalle.getHorasExtras() > 0) {
            holder.inputHorasExtras.setText(String.valueOf((int) detalle.getHorasExtras()));
        } else {
            holder.inputHorasExtras.setText("0");
        }

        if (detalle.getPrecioHorasExtras() > 0) {
            holder.inputPrecioHE.setText(String.format("%.2f", detalle.getPrecioHorasExtras()));
        } else {
            holder.inputPrecioHE.setText("0.00");
        }

        actualizarEstadoBotones(holder, detalle);
        actualizarColorFranja(holder, detalle);

        holder.btnExpandir.setRotation(detalle.isExpandido() ? 180f : 0f);
        holder.layoutContenido.setVisibility(detalle.isExpandido() ? View.VISIBLE : View.GONE);

        holder.itemView.setOnClickListener(v -> {
            detalle.setExpandido(!detalle.isExpandido());
            notifyItemChanged(position);
        });

        if (!modoSoloLectura) {
            configurarListeners(holder, detalle);
        } else {
            deshabilitarCampos(holder);
        }
    }

    private void configurarListeners(ViewHolder holder, DetalleMes detalle) {
        holder.inputHorometro.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (listener != null && s.toString().trim().length() > 0) {
                    try {
                        double horometro = Double.parseDouble(s.toString().trim());
                        listener.onHorometroChanged(detalle, horometro);
                    } catch (NumberFormatException e) {
                    }
                }
            }
        });

        holder.btnConfirmarPagoMes.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPagoMesConfirmado(detalle);
            }
        });

        holder.btnConfirmarPagoHE.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPagoHEConfirmado(detalle);
            }
        });

        holder.btnValorizacion.setOnClickListener(v -> {
            if (listener != null) {
                listener.onGenerarValorizacion(detalle);
            }
        });
    }

    private void actualizarEstadoBotones(ViewHolder holder, DetalleMes detalle) {
        if (detalle.getHorasExtras() <= 0) {
            holder.btnConfirmarPagoHE.setEnabled(false);
            holder.btnConfirmarPagoHE.setAlpha(0.5f);
        } else {
            holder.btnConfirmarPagoHE.setEnabled(!detalle.isPagoHEConfirmado());
            holder.btnConfirmarPagoHE.setAlpha(detalle.isPagoHEConfirmado() ? 0.5f : 1f);
        }

        holder.btnConfirmarPagoMes.setEnabled(!detalle.isPagoMesConfirmado());
        holder.btnConfirmarPagoMes.setAlpha(detalle.isPagoMesConfirmado() ? 0.5f : 1f);
    }

    private void actualizarColorFranja(ViewHolder holder, DetalleMes detalle) {
        int color;

        if (detalle.getHorasExtras() > 0) {
            if (detalle.isPagoMesConfirmado() && detalle.isPagoHEConfirmado()) {
                color = ContextCompat.getColor(holder.itemView.getContext(), R.color.green_accent);
            } else if (detalle.isPagoMesConfirmado() || detalle.isPagoHEConfirmado()) {
                color = ContextCompat.getColor(holder.itemView.getContext(), R.color.yellow_accent);
            } else {
                color = ContextCompat.getColor(holder.itemView.getContext(), R.color.red_accent);
            }
        } else {
            if (detalle.isPagoMesConfirmado()) {
                color = ContextCompat.getColor(holder.itemView.getContext(), R.color.green_accent);
            } else {
                color = ContextCompat.getColor(holder.itemView.getContext(), R.color.red_accent);
            }
        }

        holder.franjaColor.setBackgroundColor(color);
    }

    private void deshabilitarCampos(ViewHolder holder) {
        holder.inputHorometro.setEnabled(false);
        holder.btnConfirmarPagoMes.setEnabled(false);
        holder.btnConfirmarPagoHE.setEnabled(false);
        holder.btnValorizacion.setEnabled(false);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTituloPeriodo;
        ImageView btnExpandir;
        LinearLayout layoutContenido;
        View franjaColor;
        TextInputLayout textInputHorometro;
        TextInputEditText inputHorometro, inputHorasExtras, inputPrecioHE;
        Button btnConfirmarPagoMes, btnConfirmarPagoHE, btnValorizacion;

        ViewHolder(View itemView) {
            super(itemView);
            tvTituloPeriodo = itemView.findViewById(R.id.tvTituloPeriodo);
            btnExpandir = itemView.findViewById(R.id.btnExpandir);
            layoutContenido = itemView.findViewById(R.id.layoutContenido);
            franjaColor = itemView.findViewById(R.id.franjaColor);
            textInputHorometro = itemView.findViewById(R.id.textInputHorometro);
            inputHorometro = itemView.findViewById(R.id.inputHorometro);
            inputHorasExtras = itemView.findViewById(R.id.inputHorasExtras);
            inputPrecioHE = itemView.findViewById(R.id.inputPrecioHE);
            btnConfirmarPagoMes = itemView.findViewById(R.id.btnConfirmarPagoMes);
            btnConfirmarPagoHE = itemView.findViewById(R.id.btnConfirmarPagoHE);
            btnValorizacion = itemView.findViewById(R.id.btnValorizacion);
        }
    }
}