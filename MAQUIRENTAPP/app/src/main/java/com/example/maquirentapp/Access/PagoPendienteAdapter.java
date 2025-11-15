package com.example.maquirentapp.Access;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.maquirentapp.Model.PagoPendiente;
import com.example.maquirentapp.R;

import java.util.List;
import java.util.Locale;

public class PagoPendienteAdapter extends RecyclerView.Adapter<PagoPendienteAdapter.ViewHolder> {

    private List<PagoPendiente> items;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(PagoPendiente pago);
    }

    public PagoPendienteAdapter(List<PagoPendiente> items, OnItemClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    public void setItems(List<PagoPendiente> nuevosItems) {
        this.items = nuevosItems;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_pago_pendiente, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PagoPendiente pago = items.get(position);
        holder.bind(pago, listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        View barraEstado;
        TextView tvNombreCliente, tvCodigoGrupo, tvTituloPeriodo, tvMontoMes, tvMontoHE;
        LinearLayout llMontoMes, llMontoHE;
        Context context;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            context = itemView.getContext();
            barraEstado = itemView.findViewById(R.id.barraEstado);
            tvNombreCliente = itemView.findViewById(R.id.tvNombreCliente);
            tvCodigoGrupo = itemView.findViewById(R.id.tvCodigoGrupo);
            tvTituloPeriodo = itemView.findViewById(R.id.tvTituloPeriodo);
            tvMontoMes = itemView.findViewById(R.id.tvMontoMes);
            tvMontoHE = itemView.findViewById(R.id.tvMontoHE);
            llMontoMes = itemView.findViewById(R.id.llMontoMes);
            llMontoHE = itemView.findViewById(R.id.llMontoHE);
        }

        public void bind(PagoPendiente pago, OnItemClickListener listener) {
            tvNombreCliente.setText(pago.getNombreCliente());
            tvCodigoGrupo.setText(pago.getCodigoGrupo());
            tvTituloPeriodo.setText(pago.getTituloPeriodo());

            if (pago.getMontoPendienteMes() > 0) {
                llMontoMes.setVisibility(View.VISIBLE);
                tvMontoMes.setText(String.format(Locale.US, "Mes: %s%.2f",
                        pago.getMoneda(), pago.getMontoPendienteMes()));
            } else {
                llMontoMes.setVisibility(View.GONE);
            }

            if (pago.getMontoPendienteHE() > 0) {
                llMontoHE.setVisibility(View.VISIBLE);
                tvMontoHE.setText(String.format(Locale.US, "Horas extra: %s%.2f",
                        pago.getMoneda(), pago.getMontoPendienteHE()));
            } else {
                llMontoHE.setVisibility(View.GONE);
            }

            barraEstado.setBackgroundColor(ContextCompat.getColor(context, pago.getEstadoColor()));

            itemView.setOnClickListener(v -> listener.onItemClick(pago));
        }
    }
}