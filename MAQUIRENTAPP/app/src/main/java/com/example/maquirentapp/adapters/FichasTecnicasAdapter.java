package com.example.maquirentapp.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.os.ParcelFileDescriptor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.maquirentapp.Model.FichaTecnica;
import com.example.maquirentapp.R;
import com.example.maquirentapp.Utils.PdfCacheManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FichasTecnicasAdapter extends RecyclerView.Adapter<FichasTecnicasAdapter.ViewHolder> {
    private List<FichaTecnica> fichas;
    private OnItemClickListener listener;
    private final ExecutorService executor = Executors.newFixedThreadPool(3);

    public interface OnItemClickListener {
        void onItemClick(FichaTecnica ficha);
        void onCompartirClick(FichaTecnica ficha);
        void onDescargarClick(FichaTecnica ficha);
        void onEliminarClick(FichaTecnica ficha);
    }

    public FichasTecnicasAdapter(List<FichaTecnica> fichas, OnItemClickListener listener) {
        this.fichas = fichas != null ? fichas : new ArrayList<>();
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_ficha_tecnica, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FichaTecnica ficha = fichas.get(position);
        holder.bind(ficha, listener);
    }

    @Override
    public int getItemCount() {
        return fichas.size();
    }

    public void actualizarLista(List<FichaTecnica> nuevasFichas) {
        this.fichas = nuevasFichas != null ? nuevasFichas : new ArrayList<>();
        notifyDataSetChanged();
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        shutdown();
    }

    public void shutdown() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdownNow();
        }
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private ImageView ivPreview, ivPdfIcon;
        private TextView tvNombre, tvFecha;
        private ImageButton btnCompartir, btnDescargar, btnEliminar;
        private ProgressBar progressPreview;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPreview = itemView.findViewById(R.id.iv_pdf_preview);
            ivPdfIcon = itemView.findViewById(R.id.iv_pdf_icon);
            tvNombre = itemView.findViewById(R.id.tv_nombre_archivo);
            tvFecha = itemView.findViewById(R.id.tv_fecha);
            btnCompartir = itemView.findViewById(R.id.btn_compartir);
            btnDescargar = itemView.findViewById(R.id.btn_descargar);
            btnEliminar = itemView.findViewById(R.id.btn_eliminar);
            progressPreview = itemView.findViewById(R.id.progress_preview);
        }

        public void bind(FichaTecnica ficha, OnItemClickListener listener) {
            tvNombre.setText(ficha.getNombreArchivo());
            tvFecha.setText(ficha.getFechaSubida());

            // Click en el item completo para abrir el PDF
            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onItemClick(ficha);
            });

            // Botón compartir
            btnCompartir.setOnClickListener(v -> {
                if (listener != null) listener.onCompartirClick(ficha);
            });

            // Botón descargar
            btnDescargar.setOnClickListener(v -> {
                if (listener != null) listener.onDescargarClick(ficha);
            });

            // Botón eliminar
            btnEliminar.setOnClickListener(v -> {
                if (listener != null) listener.onEliminarClick(ficha);
            });

            // Cargar vista previa del PDF
            cargarVistaPrevia(ficha);
        }

        private void cargarVistaPrevia(FichaTecnica ficha) {
            // Resetear vistas
            ivPreview.setVisibility(View.GONE);
            ivPdfIcon.setVisibility(View.VISIBLE);
            progressPreview.setVisibility(View.VISIBLE);

            if (executor.isShutdown()) return;

            Context context = itemView.getContext();

            executor.execute(() -> {
                ParcelFileDescriptor fd = null;
                PdfRenderer renderer = null;
                PdfRenderer.Page page = null;

                try {
                    // Modo Offline Cache-First: obtiene el archivo de la caché local o lo descarga y guarda para siempre
                    File pdfFile = PdfCacheManager.obtenerPdf(context, ficha.getUrlPdf(), ficha.getNombreArchivo());

                    if (pdfFile != null && pdfFile.exists() && pdfFile.length() > 0) {
                        fd = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY);
                        renderer = new PdfRenderer(fd);
                        if (renderer.getPageCount() > 0) {
                            page = renderer.openPage(0);
                            Bitmap bitmap = Bitmap.createBitmap(page.getWidth(), page.getHeight(), Bitmap.Config.ARGB_8888);
                            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);

                            // Actualizar UI en el hilo principal
                            itemView.post(() -> {
                                progressPreview.setVisibility(View.GONE);
                                ivPdfIcon.setVisibility(View.GONE);
                                ivPreview.setVisibility(View.VISIBLE);
                                ivPreview.setImageBitmap(bitmap);
                            });
                        }
                    } else {
                        itemView.post(() -> {
                            progressPreview.setVisibility(View.GONE);
                            ivPdfIcon.setVisibility(View.VISIBLE);
                        });
                    }
                } catch (Exception e) {
                    itemView.post(() -> {
                        progressPreview.setVisibility(View.GONE);
                        ivPdfIcon.setVisibility(View.VISIBLE);
                    });
                } finally {
                    if (page != null) {
                        try { page.close(); } catch (Exception ignored) {}
                    }
                    if (renderer != null) {
                        try { renderer.close(); } catch (Exception ignored) {}
                    }
                    if (fd != null) {
                        try { fd.close(); } catch (Exception ignored) {}
                    }
                }
            });
        }
    }
}
