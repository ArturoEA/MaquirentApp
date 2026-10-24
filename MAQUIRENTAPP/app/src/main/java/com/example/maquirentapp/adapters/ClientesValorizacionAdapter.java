package com.example.maquirentapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.maquirentapp.Model.ClienteValorizacion;
import com.example.maquirentapp.R;

public class ClientesValorizacionAdapter extends ListAdapter<ClienteValorizacion, ClientesValorizacionAdapter.ViewHolder> {

    private final OnClienteActionListener listener;

    public interface OnClienteActionListener {
        void onClienteClick(ClienteValorizacion cliente);
        void onEliminarClick(ClienteValorizacion cliente);
    }

    private static final DiffUtil.ItemCallback<ClienteValorizacion> DIFF_CALLBACK = new DiffUtil.ItemCallback<ClienteValorizacion>() {
        @Override
        public boolean areItemsTheSame(@NonNull ClienteValorizacion oldItem, @NonNull ClienteValorizacion newItem) {
            return oldItem.getId() != null && oldItem.getId().equals(newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull ClienteValorizacion oldItem, @NonNull ClienteValorizacion newItem) {
            return oldItem.getNombreEmpresa().equals(newItem.getNombreEmpresa()) &&
                    oldItem.getRuc().equals(newItem.getRuc());
        }
    };

    public ClientesValorizacionAdapter(OnClienteActionListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_cliente_valorizacion, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position), listener);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvNombre, tvRuc;
        ImageView btnEliminar;

        ViewHolder(View itemView) {
            super(itemView);
            tvNombre = itemView.findViewById(R.id.tvNombreEmpresa);
            tvRuc = itemView.findViewById(R.id.tvRucEmpresa);
            btnEliminar = itemView.findViewById(R.id.btnEliminarCliente);
        }

        void bind(ClienteValorizacion cliente, OnClienteActionListener listener) {
            tvNombre.setText(cliente.getNombreEmpresa());
            tvRuc.setText("RUC: " + cliente.getRuc());

            if (btnEliminar != null) {
                btnEliminar.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onEliminarClick(cliente);
                    }
                });
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onClienteClick(cliente);
                }
            });
        }
    }
}
