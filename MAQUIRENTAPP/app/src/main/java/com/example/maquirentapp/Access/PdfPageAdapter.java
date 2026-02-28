package com.example.maquirentapp.Access;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.pdf.PdfRenderer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.maquirentapp.R;

public class PdfPageAdapter extends RecyclerView.Adapter<PdfPageAdapter.PdfViewHolder> {

    private final PdfRenderer pdfRenderer;
    private final float scaleFactor = 2.5f;

    public PdfPageAdapter(PdfRenderer pdfRenderer) {
        this.pdfRenderer = pdfRenderer;
    }

    @NonNull
    @Override
    public PdfViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_pdf_page, parent, false);
        return new PdfViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PdfViewHolder holder, int position) {
        try {
            // 1. Abrimos solo la página que toca mostrar en pantalla
            PdfRenderer.Page page = pdfRenderer.openPage(position);

            int width = (int) (page.getWidth() * scaleFactor);
            int height = (int) (page.getHeight() * scaleFactor);

            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            bitmap.eraseColor(Color.WHITE);

            Matrix matrix = new Matrix();
            matrix.postScale(scaleFactor, scaleFactor);

            page.render(bitmap, null, matrix, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);

            // 2. Mostramos la imagen
            holder.ivPage.setImageBitmap(bitmap);

            // 3. Cerramos la página para liberar la memoria RAM
            page.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getItemCount() {
        return pdfRenderer != null ? pdfRenderer.getPageCount() : 0;
    }

    static class PdfViewHolder extends RecyclerView.ViewHolder {
        ImageView ivPage;
        public PdfViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPage = itemView.findViewById(R.id.ivPdfPageItem);
        }
    }
}