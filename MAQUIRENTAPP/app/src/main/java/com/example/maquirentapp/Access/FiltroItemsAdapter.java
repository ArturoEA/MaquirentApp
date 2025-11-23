package com.example.maquirentapp.Access;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.maquirentapp.Model.FiltroItem;
import com.example.maquirentapp.R;
import java.util.List;

public class FiltroItemsAdapter extends RecyclerView.Adapter<FiltroItemsAdapter.ViewHolder> {
    private List<FiltroItem> items;
    private OnItemActionListener listener;
    public interface OnItemActionListener {
        void onDelete(FiltroItem item);
        void onClick(FiltroItem item, int position);
    }

    public FiltroItemsAdapter(List<FiltroItem> items, OnItemActionListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_filtro_detalle, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int pos) {
        FiltroItem item = items.get(pos);
        h.tvMarca.setText(item.getMarca());
        h.tvCodigo.setText(item.getCodigo());
        h.btnDelete.setOnClickListener(v -> listener.onDelete(item));
        h.itemView.setOnClickListener(v -> listener.onClick(item, pos));
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvMarca, tvCodigo;
        ImageView btnDelete;
        public ViewHolder(View v) {
            super(v);
            tvMarca = v.findViewById(R.id.tvMarca);
            tvCodigo = v.findViewById(R.id.tvCodigo);
            btnDelete = v.findViewById(R.id.btnEliminarItem);
        }
    }
}