package com.example.maquirentapp.Access;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.maquirentapp.Model.Accesorio;
import com.example.maquirentapp.R;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AccesorioSeleccionAdapter extends RecyclerView.Adapter<AccesorioSeleccionAdapter.VH> {
    private final List<Accesorio> items = new ArrayList<>();
    private final Set<String> accesoriosSeleccionados = new HashSet<>();
    private boolean clickEnabled = true;

    public void setItems(List<Accesorio> nuevos) {
        items.clear();
        items.addAll(nuevos);
        notifyDataSetChanged();
    }

    public void setAccesoriosSeleccionados(List<String> ids) {
        accesoriosSeleccionados.clear();
        if (ids != null) {
            accesoriosSeleccionados.addAll(ids);
        }
        notifyDataSetChanged();
    }

    public List<String> getAccesoriosSeleccionados() {
        return new ArrayList<>(accesoriosSeleccionados);
    }

    public void setClickEnabled(boolean enabled) {
        this.clickEnabled = enabled;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_accesorio_seleccion, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Accesorio a = items.get(pos);
        boolean seleccionado = accesoriosSeleccionados.contains(a.getId());

        h.tvNombre.setText(a.getNombre());
        h.checkbox.setChecked(seleccionado);

        // Cambiar el color del card según selección
        if (seleccionado) {
            h.cardView.setCardBackgroundColor(h.itemView.getContext().getColor(R.color.background_dark));
            h.tvNombre.setTextColor(h.itemView.getContext().getColor(R.color.white));
        } else {
            h.cardView.setCardBackgroundColor(h.itemView.getContext().getColor(R.color.noseleccionado_accesorio));
            h.tvNombre.setTextColor(h.itemView.getContext().getColor(R.color.white));
        }

        // Cargar ícono desde Firebase Storage
        if (a.getIcono() != null && !a.getIcono().isEmpty()) {
            Glide.with(h.itemView.getContext())
                    .load(a.getIcono())
                    .placeholder(R.drawable.icon_extintor_blanco)
                    .error(R.drawable.icon_extintor_blanco)
                    .into(h.imgIcono);
        }

        // Click en todo el item
        h.itemView.setOnClickListener(v -> {
            if (!clickEnabled) return; // No hacer nada si está deshabilitado

            if (accesoriosSeleccionados.contains(a.getId())) {
                accesoriosSeleccionados.remove(a.getId());
            } else {
                accesoriosSeleccionados.add(a.getId());
            }
            notifyItemChanged(pos);
        });

        // Deshabilitar visualmente si no está habilitado
        h.itemView.setAlpha(clickEnabled ? 1.0f : 0.7f);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        CardView cardView;
        ImageView imgIcono;
        TextView tvNombre;
        CheckBox checkbox;

        VH(View item) {
            super(item);
            cardView = (CardView) item;
            imgIcono = item.findViewById(R.id.imgIconoAccesorio);
            tvNombre = item.findViewById(R.id.tvNombreAccesorio);
            checkbox = item.findViewById(R.id.checkboxAccesorio);
        }
    }
}