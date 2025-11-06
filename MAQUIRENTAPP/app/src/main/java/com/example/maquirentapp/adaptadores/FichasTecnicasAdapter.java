package com.example.maquirentapp.adaptadores;

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
    private ExecutorService executor = Executors.newFixedThreadPool(3);

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

            executor.execute(() -> {
                try {
                    // Descargar el PDF temporalmente
                    URL url = new URL(ficha.getUrlPdf());
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.connect();

                    File tempFile = File.createTempFile("preview", ".pdf", itemView.getContext().getCacheDir());
                    FileOutputStream fos = new FileOutputStream(tempFile);
                    InputStream is = connection.getInputStream();

                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = is.read(buffer)) > 0) {
                        fos.write(buffer, 0, len);
                    }
                    fos.close();
                    is.close();

                    // Renderizar la primera página
                    ParcelFileDescriptor fd = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY);
                    PdfRenderer renderer = new PdfRenderer(fd);
                    PdfRenderer.Page page = renderer.openPage(0);

                    Bitmap bitmap = Bitmap.createBitmap(page.getWidth(), page.getHeight(), Bitmap.Config.ARGB_8888);
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);

                    page.close();
                    renderer.close();
                    fd.close();
                    tempFile.delete();

                    // Actualizar UI en el hilo principal
                    itemView.post(() -> {
                        progressPreview.setVisibility(View.GONE);
                        ivPdfIcon.setVisibility(View.GONE);
                        ivPreview.setVisibility(View.VISIBLE);
                        ivPreview.setImageBitmap(bitmap);
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                    itemView.post(() -> {
                        progressPreview.setVisibility(View.GONE);
                        ivPdfIcon.setVisibility(View.VISIBLE);
                    });
                }
            });
        }
    }
}