package com.example.maquirentapp.adaptadores;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.maquirentapp.Model.FichaTecnica;
import com.example.maquirentapp.R;
import com.example.maquirentapp.ViewModel.FichaTecnicaViewModel;

import org.jspecify.annotations.NonNull;

import java.util.List;

public class FichasTecnicasAdapter extends RecyclerView.Adapter<FichasTecnicasAdapter.FichaViewHolder> {
    private List<FichaTecnica> fichas;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(FichaTecnica ficha);
        void onPdfClick(FichaTecnica ficha);
    }

    public FichasTecnicasAdapter(List<FichaTecnica> fichas, OnItemClickListener listener) {
        this.fichas = fichas;
        this.listener = listener;
    }

    @NonNull
    @Override
    public FichaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_ficha_tecnica, parent, false);
        return new FichaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FichaViewHolder holder, int position) {
        FichaTecnica ficha = fichas.get(position);
        holder.bind(ficha);

        // Configurar clics
        holder.itemView.setOnClickListener(v -> {
            if (ficha.tienePdf()) {
                listener.onPdfClick(ficha);
            } else {
                listener.onItemClick(ficha);
            }
        });
    }

    @Override
    public int getItemCount() {
        return fichas.size();
    }

    public void actualizarLista(List<FichaTecnica> nuevasFichas) {
        this.fichas = nuevasFichas;
        notifyDataSetChanged();
    }

    // ViewHolder como clase interna
    public class FichaViewHolder extends RecyclerView.ViewHolder {
        private CardView cardView;
        private ImageView imgIcono, imgPdfStatus;
        private TextView txtNombre;

        public FichaViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.card_ficha_tecnica);
            imgIcono = itemView.findViewById(R.id.img_icono_ficha);
            imgPdfStatus = itemView.findViewById(R.id.img_pdf_status);
            txtNombre = itemView.findViewById(R.id.txt_nombre_ficha);
        }

        public void bind(FichaTecnica ficha) {
            txtNombre.setText(ficha.getNombre());

            if (ficha.getPdfUrl() != null && !ficha.getPdfUrl().isEmpty()) {
                imgPdfStatus.setVisibility(View.VISIBLE);
            } else {
                imgPdfStatus.setVisibility(View.GONE);
            }

            cardView.setOnClickListener(v -> listener.onItemClick(ficha));
            imgPdfStatus.setOnClickListener(v -> {
                if (ficha.getPdfUrl() != null) {
                    listener.onPdfClick(ficha);
                }
            });
        }
    }
}
