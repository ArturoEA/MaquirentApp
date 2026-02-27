package com.example.maquirentapp.View;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.maquirentapp.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PdfViewerActivity extends AppCompatActivity {
    private ImageView ivPdfPage;
    private TextView tvPageInfo;
    private FloatingActionButton fabNext, fabPrev;
    private ProgressBar progressBar;
    private Toolbar toolbar;

    private PdfRenderer pdfRenderer;
    private PdfRenderer.Page currentPage;
    private ParcelFileDescriptor parcelFileDescriptor;
    private File pdfFile;

    private int currentPageIndex = 0;
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    private String pdfUrl;
    private String nombreArchivo;

    public static Intent newIntent(Context context, String pdfUrl, String nombreArchivo) {
        Intent intent = new Intent(context, PdfViewerActivity.class);
        intent.putExtra("PDF_URL", pdfUrl);
        intent.putExtra("NOMBRE_ARCHIVO", nombreArchivo);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf_viewer);

        // Obtener datos del Intent
        pdfUrl = getIntent().getStringExtra("PDF_URL");
        nombreArchivo = getIntent().getStringExtra("NOMBRE_ARCHIVO");

        if (pdfUrl == null || nombreArchivo == null) {
            Toast.makeText(this, "Error: datos no válidos", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupToolbar();
        cargarPdf();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        ivPdfPage = findViewById(R.id.iv_pdf_page);
        tvPageInfo = findViewById(R.id.tv_page_info);
        fabNext = findViewById(R.id.fab_next_page);
        fabPrev = findViewById(R.id.fab_prev_page);
        progressBar = findViewById(R.id.progress_bar);

        fabNext.setOnClickListener(v -> mostrarSiguientePagina());
        fabPrev.setOnClickListener(v -> mostrarPaginaAnterior());
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(nombreArchivo);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_pdf_viewer, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            finish();
            return true;
        } else if (id == R.id.action_compartir) {
            compartirPdf();
            return true;
        } else if (id == R.id.action_descargar) {
            descargarPdf();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    private void cargarPdf() {
        progressBar.setVisibility(View.VISIBLE);
        ivPdfPage.setVisibility(View.GONE);
        fabNext.setEnabled(false);
        fabPrev.setEnabled(false);

        if (!pdfUrl.startsWith("http")) {
            File localFile = new File(pdfUrl);
            if (localFile.exists()) {
                mostrarPdfDesdeArchivo(localFile);
            } else {
                Toast.makeText(this, "El archivo no existe", Toast.LENGTH_SHORT).show();
                finish();
            }
            return;
        }

        executor.execute(() -> {
            try {
                pdfFile = new File(getCacheDir(), "temp_" + nombreArchivo + ".pdf");
                URL url = new URL(pdfUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                FileOutputStream fos = new FileOutputStream(pdfFile);
                InputStream is = connection.getInputStream();

                byte[] buffer = new byte[1024];
                int len;
                while ((len = is.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }
                fos.close();
                is.close();

                runOnUiThread(() -> mostrarPdfDesdeArchivo(pdfFile));

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Error al descargar PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }
    private void mostrarPdfDesdeArchivo(File file) {
        try {
            pdfFile = file;
            parcelFileDescriptor = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY);
            pdfRenderer = new PdfRenderer(parcelFileDescriptor);

            runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                ivPdfPage.setVisibility(View.VISIBLE);
                mostrarPagina(0);
            });

        } catch (Exception e) {
            e.printStackTrace();
            runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(this, "Error al abrir PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
        }
    }
    private void mostrarPagina(int index) {
        if (pdfRenderer == null) return;

        // Cerrar página actual si existe
        if (currentPage != null) {
            currentPage.close();
        }

        // Abrir nueva página
        currentPage = pdfRenderer.openPage(index);
        currentPageIndex = index;

        //Previsualización en mejor resolución
        float scaleFactor = 2.5f;

        int width = (int) (currentPage.getWidth() * scaleFactor);
        int height = (int) (currentPage.getHeight() * scaleFactor);

        // Crear bitmap en alta resolución
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        // PINTAR FONDO BLANCO (Muy importante porque PdfRenderer dibuja con fondo transparente)
        bitmap.eraseColor(android.graphics.Color.WHITE);

        // Crear una matriz para escalar el dibujo del PDF al nuevo tamaño del Bitmap
        android.graphics.Matrix matrix = new android.graphics.Matrix();
        matrix.postScale(scaleFactor, scaleFactor);

        // Renderizar página en el bitmap aplicando la matriz
        currentPage.render(bitmap, null, matrix, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
        // -----------------------------------

        // Mostrar en ImageView
        ivPdfPage.setImageBitmap(bitmap);

        // Actualizar info de página
        tvPageInfo.setText(String.format(Locale.getDefault(), "Página %d de %d", index + 1, pdfRenderer.getPageCount()));

        // Actualizar botones
        fabPrev.setEnabled(index > 0);
        fabNext.setEnabled(index < pdfRenderer.getPageCount() - 1);
    }

    private void mostrarSiguientePagina() {
        if (currentPageIndex < pdfRenderer.getPageCount() - 1) {
            mostrarPagina(currentPageIndex + 1);
        }
    }

    private void mostrarPaginaAnterior() {
        if (currentPageIndex > 0) {
            mostrarPagina(currentPageIndex - 1);
        }
    }

    private void compartirPdf() {
        if (pdfFile != null && pdfFile.exists()) {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("application/pdf");
            shareIntent.putExtra(Intent.EXTRA_STREAM,
                    androidx.core.content.FileProvider.getUriForFile(
                            this,
                            getPackageName() + ".provider",
                            pdfFile
                    ));
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, nombreArchivo);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(Intent.createChooser(shareIntent, "Compartir PDF"));
        } else {
            Toast.makeText(this, "Archivo no disponible", Toast.LENGTH_SHORT).show();
        }
    }

    private void descargarPdf() {
        // Aquí puedes llamar al mét0do del ViewModel para descargar
        Toast.makeText(this, "Descargando...", Toast.LENGTH_SHORT).show();

        // Implementar descarga real usando DownloadManager o copiando el archivo
        try {
            File downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(
                    android.os.Environment.DIRECTORY_DOWNLOADS
            );
            File destFile = new File(downloadsDir, nombreArchivo);

            // Copiar archivo de caché a descargas
            java.nio.file.Files.copy(
                    pdfFile.toPath(),
                    destFile.toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING
            );

            Toast.makeText(this, "Descargado en: " + destFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Error al descargar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Limpiar recursos
        if (currentPage != null) {
            currentPage.close();
        }

        if (pdfRenderer != null) {
            pdfRenderer.close();
        }

        if (parcelFileDescriptor != null) {
            try {
                parcelFileDescriptor.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Eliminar archivo temporal
        if (pdfFile != null && pdfFile.exists()) {
            pdfFile.delete();
        }

        executor.shutdown();
    }
}