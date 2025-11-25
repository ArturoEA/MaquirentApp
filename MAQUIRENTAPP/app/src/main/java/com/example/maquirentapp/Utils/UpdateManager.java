package com.example.maquirentapp.Utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.example.maquirentapp.BuildConfig;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;

public class UpdateManager {

    private final Activity activity;
    private final FirebaseFirestore db;
    private final FirebaseStorage storage;
    private final String APK_NAME = "update_maquirent.apk";

    public interface UpdateCheckListener {
        void onNoUpdateNeeded();
    }

    public UpdateManager(Activity activity) {
        this.activity = activity;
        this.db = FirebaseFirestore.getInstance();
        this.storage = FirebaseStorage.getInstance();
    }

    public void checkForUpdates(UpdateCheckListener listener) {
        db.collection("configuracion").document("app_version")
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Long latestVersionCode = documentSnapshot.getLong("version_code");
                        int currentVersionCode = BuildConfig.VERSION_CODE;

                        if (latestVersionCode != null && latestVersionCode > currentVersionCode) {
                            mostrarDialogoActualizacion();
                        } else {
                            listener.onNoUpdateNeeded();
                        }
                    } else {
                        listener.onNoUpdateNeeded();
                    }
                })
                .addOnFailureListener(e -> {
                    listener.onNoUpdateNeeded();
                });
    }

    private void mostrarDialogoActualizacion() {
        if (activity.isFinishing()) return;

        new AlertDialog.Builder(activity)
                .setTitle("Actualización disponible")
                .setMessage("Existe una nueva versión de MaquirentApp. Es necesario actualizar para continuar.")
                .setCancelable(false)
                .setPositiveButton("Actualizar ahora", (dialog, which) -> {
                    if (tienePermisoInstalar()) {
                        descargarEInstalarApk();
                    } else {
                        solicitarPermisoInstalacion();
                    }
                })
                .show();
    }
    private boolean tienePermisoInstalar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return activity.getPackageManager().canRequestPackageInstalls();
        }
        return true;
    }
    private void solicitarPermisoInstalacion() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            new AlertDialog.Builder(activity)
                    .setTitle("Permiso necesario")
                    .setMessage("Para poder actualizar la app, necesitas activar el permiso de 'Instalar aplicaciones desconocidas' para MaquirentApp.")
                    .setPositiveButton("Ir a configuración", (dialog, which) -> {
                        Uri packageUri = Uri.parse("package:" + activity.getPackageName());
                        Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, packageUri);
                        activity.startActivity(intent);
                    })
                    .setNegativeButton("Cancelar", (dialog, which) -> mostrarDialogoActualizacion())
                    .show();
        }
    }

    private void descargarEInstalarApk() {
        ProgressDialog progressDialog = new ProgressDialog(activity);
        progressDialog.setMessage("Descargando actualización...");
        progressDialog.setIndeterminate(false);
        progressDialog.setMax(100);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setCancelable(false);
        progressDialog.show();

        StorageReference apkRef = storage.getReference().child("updates/app-release.apk");

        File updateDir = new File(activity.getExternalFilesDir(null), "updates");
        if (!updateDir.exists()) updateDir.mkdirs();
        File apkFile = new File(updateDir, APK_NAME);

        if (apkFile.exists()) apkFile.delete();

        apkRef.getFile(apkFile)
                .addOnProgressListener(snapshot -> {
                    double progress = (100.0 * snapshot.getBytesTransferred()) / snapshot.getTotalByteCount();
                    progressDialog.setProgress((int) progress);
                })
                .addOnSuccessListener(taskSnapshot -> {
                    progressDialog.dismiss();
                    instalarApk(apkFile);
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(activity, "Error descarga: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    mostrarDialogoActualizacion();
                });
    }

    private void instalarApk(File apkFile) {
        try {
            if (!tienePermisoInstalar()) {
                solicitarPermisoInstalacion();
                return;
            }

            Uri apkUri = FileProvider.getUriForFile(
                    activity,
                    BuildConfig.APPLICATION_ID + ".provider",
                    apkFile
            );

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            activity.startActivity(intent);

        } catch (Exception e) {
            Toast.makeText(activity, "Error al instalar: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}