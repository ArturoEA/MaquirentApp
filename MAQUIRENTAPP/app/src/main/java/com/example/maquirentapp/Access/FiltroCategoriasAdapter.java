package com.example.maquirentapp.Access;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.maquirentapp.Model.FiltroCategoria;
import com.example.maquirentapp.Model.FiltroItem;
import com.example.maquirentapp.R;

import java.util.List;

public class FiltroCategoriasAdapter extends RecyclerView.Adapter<FiltroCategoriasAdapter.ViewHolder> {

    private List<FiltroCategoria> lista;
    private OnCategoriaActionListener listener;
    private Context context;

    public interface OnCategoriaActionListener {
        void onAgregarItem(FiltroCategoria categoria);

        void onEliminarItem(FiltroCategoria categoria, FiltroItem item);

        void onEliminarCategoria(FiltroCategoria categoria);

        void onEditarItem(FiltroCategoria categoria, FiltroItem item, int position);
    }

    public FiltroCategoriasAdapter(List<FiltroCategoria> lista, OnCategoriaActionListener listener) {
        this.lista = lista;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.item_categoria_filtro, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FiltroCategoria cat = lista.get(position);
        holder.tvNombre.setText(cat.getNombreCategoria());

        // Configurar RecyclerView interno
        holder.recyclerItems.setLayoutManager(new LinearLayoutManager(context));
        FiltroItemsAdapter itemsAdapter = new FiltroItemsAdapter(cat.getItems(), new FiltroItemsAdapter.OnItemActionListener() {
            @Override
            public void onDelete(FiltroItem item) {
                listener.onEliminarItem(cat, item);
            }

            @Override
            public void onClick(FiltroItem item, int pos) {
                listener.onEditarItem(cat, item, pos); // Notificar click para edición
            }
        });
        holder.recyclerItems.setAdapter(itemsAdapter);

        // Expandir/Colapsar
        holder.headerLayout.setOnClickListener(v -> {
            boolean isVisible = holder.bodyLayout.getVisibility() == View.VISIBLE;
            holder.bodyLayout.setVisibility(isVisible ? View.GONE : View.VISIBLE);
            holder.ivChevron.animate().rotation(isVisible ? 0 : 180).setDuration(200).start();
        });

        holder.btnAdd.setOnClickListener(v -> listener.onAgregarItem(cat));

        holder.headerLayout.setOnLongClickListener(v -> {
            listener.onEliminarCategoria(cat);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return lista.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvNombre;
        ImageView btnAdd, ivChevron;
        LinearLayout headerLayout, bodyLayout;
        RecyclerView recyclerItems;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNombre = itemView.findViewById(R.id.tvNombreCategoria);
            btnAdd = itemView.findViewById(R.id.btnAddItem);
            ivChevron = itemView.findViewById(R.id.ivChevron);
            headerLayout = itemView.findViewById(R.id.headerLayout);
            bodyLayout = itemView.findViewById(R.id.bodyLayout);
            recyclerItems = itemView.findViewById(R.id.recyclerItemsFiltro);
        }
    }
}