package com.example.maquirentapp.adapters;

import android.app.Activity;
import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import android.graphics.Color;
import androidx.appcompat.app.AlertDialog;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.maquirentapp.Model.DetalleMes;
import com.example.maquirentapp.R;
import com.example.maquirentapp.Utils.CustomDatePickerDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class DetalleMesAdapter extends RecyclerView.Adapter<DetalleMesAdapter.ViewHolder> {
    private final List<DetalleMes> items = new ArrayList<>();
    private OnDetalleMesListener listener;
    private boolean modoSoloLectura = false;
    private double precioMensualBase = 0;
    private String simboloMoneda = "S/.";

    public interface OnDetalleMesListener {
        void onHorometroChanged(DetalleMes detalle, double nuevoHorometro);
        void onHorometroCalculated(DetalleMes detalle, double nuevoHorometro, int position);
        void onHorometroFinalized(DetalleMes detalle);

        void onPagoMesConfirmado(DetalleMes detalle, int position);

        void onPagoHEConfirmado(DetalleMes detalle, int position);

        void onFechaFinModificada(DetalleMes detalle);

        void onGenerarValorizacion(DetalleMes detalle, int position);
    }

    public void setOnDetalleMesListener(OnDetalleMesListener listener) {
        this.listener = listener;
    }

    public void setPrecioMensualBase(double precio) {
        this.precioMensualBase = precio;
    }

    public void setSimboloMoneda(String simbolo) {
        this.simboloMoneda = (simbolo != null && !simbolo.isEmpty()) ? simbolo : "S/.";
    }

    public void setItems(List<DetalleMes> nuevos) {
        items.clear();
        if (nuevos != null) {
            items.addAll(nuevos);
        }
        notifyDataSetChanged();
    }

    public void setModoSoloLectura(boolean soloLectura) {
        this.modoSoloLectura = soloLectura;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_detalle_mes, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DetalleMes detalle = items.get(position);

        if (holder.horometroWatcher != null) {
            holder.inputHorometro.removeTextChangedListener(holder.horometroWatcher);
        }
        holder.inputHorometro.setOnFocusChangeListener(null);
        holder.inputHorometro.setOnEditorActionListener(null);

        holder.tvTituloPeriodo.setText(detalle.getTituloPeriodo());
        double montoAMostrar = detalle.getMontoMes() > 0 ? detalle.getMontoMes() : precioMensualBase;
        holder.tvMontoMes.setText(String.format(Locale.US, "Monto: %s %.2f", simboloMoneda, montoAMostrar));

        if (detalle.getFechaFin() != null && !detalle.getFechaFin().isEmpty()) {
            holder.textInputHorometro.setHint("Horómetro al " + detalle.getFechaFin());
        }

        holder.inputHorometro.setText(detalle.getHorometro() > 0 ?
                String.valueOf(detalle.getHorometro()) : "");

        holder.inputHorasExtras.setText(String.valueOf((int) detalle.getHorasExtras()));
        holder.inputPrecioHE.setText(String.format(Locale.US, "%s %.2f", simboloMoneda, detalle.getPrecioHorasExtras()));


        configurarConfirmaciones(holder, detalle);
        actualizarEstadoBotones(holder, detalle);
        actualizarColorFranja(holder, detalle);

        holder.btnExpandir.setRotation(detalle.isExpandido() ? 180f : 0f);
        holder.layoutContenido.setVisibility(detalle.isExpandido() ? View.VISIBLE : View.GONE);

        View.OnClickListener expandListener = v -> {
            ocultarTeclado(v);
            quitarFocoDeInputs(holder);

            // Quitar foco global si alguna vista lo tenía
            if (v.getContext() instanceof Activity) {
                View focused = ((Activity) v.getContext()).getCurrentFocus();
                if (focused != null) {
                    focused.clearFocus();
                }
            }

            boolean nuevoEstado = !detalle.isExpandido();
            detalle.setExpandido(nuevoEstado);

            // Expandir / colapsar directamente en la vista sin llamar a notifyItemChanged
            // para evitar que RecyclerView recicle/desconecte vistas con foco dentro de NestedScrollView
            holder.layoutContenido.setVisibility(nuevoEstado ? View.VISIBLE : View.GONE);
            holder.btnExpandir.animate()
                    .rotation(nuevoEstado ? 180f : 0f)
                    .setDuration(200)
                    .start();
        };

        if (holder.headerLayout != null) {
            holder.headerLayout.setOnClickListener(expandListener);
        }
        holder.itemView.setOnClickListener(expandListener);
        holder.btnExpandir.setOnClickListener(expandListener);

        // Configurar botón de generar valorización SIEMPRE (accesible tanto en solo lectura como en edición)
        if (holder.btnGenerarValorizacionMes != null) {
            holder.btnGenerarValorizacionMes.setEnabled(true);
            holder.btnGenerarValorizacionMes.setOnClickListener(v -> {
                if (listener != null) {
                    int adapterPos = holder.getBindingAdapterPosition();
                    if (adapterPos != RecyclerView.NO_POSITION && adapterPos < items.size()) {
                        listener.onGenerarValorizacion(items.get(adapterPos), adapterPos);
                    } else {
                        listener.onGenerarValorizacion(detalle, position);
                    }
                }
            });
        }

        if (!modoSoloLectura) {
            holder.btnEditarFechaFin.setVisibility(View.VISIBLE);
            holder.btnEditarFechaFin.setOnClickListener(v -> mostrarDialogoEditarPeriodo(holder.itemView.getContext(), detalle, position));
        } else {
            holder.btnEditarFechaFin.setVisibility(View.GONE);
        }

        if (modoSoloLectura) {
            deshabilitarCampos(holder);
        } else {
            configurarListeners(holder, detalle, position);
            habilitarCamposEdicion(holder);
            if (detalle.isPagoMesConfirmado() || detalle.isPagoHEConfirmado()) {
                deshabilitarInputHorometro(holder);
            }
        }
    }

    private void mostrarDialogoEditarPeriodo(Context context, DetalleMes detalle, int position) {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_editar_periodo_mes, null);
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextView tvTituloDialogo = dialogView.findViewById(R.id.tvTituloDialogo);
        TextView tvFechaInicioInfo = dialogView.findViewById(R.id.tvFechaInicioInfo);
        TextInputEditText inputFechaFin = dialogView.findViewById(R.id.inputFechaFin);
        TextView tvDiasCalculados = dialogView.findViewById(R.id.tvDiasCalculados);
        TextInputEditText inputMontoMes = dialogView.findViewById(R.id.inputMontoMes);
        TextInputEditText inputHorasMinimas = dialogView.findViewById(R.id.inputHorasMinimas);
        Button btnCancelar = dialogView.findViewById(R.id.btnCancelar);
        Button btnGuardar = dialogView.findViewById(R.id.btnGuardar);

        tvTituloDialogo.setText("Editar período (Mes " + detalle.getNumeroMes() + ")");
        final String fechaInicioStr = detalle.getFechaInicio() != null ? detalle.getFechaInicio() : "";
        tvFechaInicioInfo.setText("Fecha inicio: " + (fechaInicioStr.isEmpty() ? "--" : fechaInicioStr));
        inputFechaFin.setText(detalle.getFechaFin() != null ? detalle.getFechaFin() : "");

        double montoInicial = detalle.getMontoMes() > 0 ? detalle.getMontoMes() : precioMensualBase;
        inputMontoMes.setText(String.format(Locale.US, "%.2f", montoInicial));

        int horasMinInicial = detalle.getHorasMinimas() > 0 ? detalle.getHorasMinimas() : 200;
        inputHorasMinimas.setText(String.valueOf(horasMinInicial));

        final long[] diasArr = new long[]{calcularDiferenciaDias(fechaInicioStr, inputFechaFin.getText().toString().trim())};
        actualizarTextoDias(tvDiasCalculados, diasArr[0]);

        View.OnClickListener dateClick = v -> {
            Calendar calFin = Calendar.getInstance();
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.US);
            try {
                if (inputFechaFin.getText() != null && !inputFechaFin.getText().toString().isEmpty()) {
                    calFin.setTime(sdf.parse(inputFechaFin.getText().toString().trim()));
                }
            } catch (Exception ignored) {}

            CustomDatePickerDialog dpd = new CustomDatePickerDialog(context,
                    calFin.get(Calendar.YEAR), calFin.get(Calendar.MONTH), calFin.get(Calendar.DAY_OF_MONTH),
                    (year, month, day) -> {
                        String nuevaFechaFin = String.format(Locale.US, "%02d/%02d/%d", day, month + 1, year);
                        inputFechaFin.setText(nuevaFechaFin);
                        diasArr[0] = calcularDiferenciaDias(fechaInicioStr, nuevaFechaFin);
                        actualizarTextoDias(tvDiasCalculados, diasArr[0]);

                        if (diasArr[0] > 0 && diasArr[0] < 30) {
                            double prorrateado = (precioMensualBase / 30.0) * diasArr[0];
                            inputMontoMes.setText(String.format(Locale.US, "%.2f", prorrateado));
                        } else if (diasArr[0] >= 30) {
                            inputMontoMes.setText(String.format(Locale.US, "%.2f", precioMensualBase));
                        }
                    });

            try {
                Date dInicio = sdf.parse(fechaInicioStr);
                if (dInicio != null) {
                    dpd.setMinDate(dInicio.getTime());
                }
            } catch (Exception ignored) {}
            dpd.show();
        };

        inputFechaFin.setOnClickListener(dateClick);
        View layoutFechaFin = dialogView.findViewById(R.id.layoutFechaFin);
        if (layoutFechaFin != null) layoutFechaFin.setOnClickListener(dateClick);

        btnCancelar.setOnClickListener(v -> dialog.dismiss());

        btnGuardar.setOnClickListener(v -> {
            String finStr = inputFechaFin.getText() != null ? inputFechaFin.getText().toString().trim() : "";
            if (finStr.isEmpty()) {
                Toast.makeText(context, "Ingrese una fecha final", Toast.LENGTH_SHORT).show();
                return;
            }
            long dias = calcularDiferenciaDias(fechaInicioStr, finStr);
            if (dias <= 0) {
                Toast.makeText(context, "La fecha final debe ser posterior a la fecha de inicio", Toast.LENGTH_SHORT).show();
                return;
            }

            double nuevoMonto;
            try {
                nuevoMonto = Double.parseDouble(inputMontoMes.getText().toString().trim());
                if (nuevoMonto < 0) throw new NumberFormatException();
            } catch (Exception e) {
                Toast.makeText(context, "Ingrese una tarifa válida", Toast.LENGTH_SHORT).show();
                return;
            }

            int nuevasHorasMin;
            try {
                nuevasHorasMin = Integer.parseInt(inputHorasMinimas.getText().toString().trim());
                if (nuevasHorasMin < 0) nuevasHorasMin = 0;
            } catch (Exception e) {
                nuevasHorasMin = 0;
            }

            detalle.setFechaFin(finStr);
            detalle.setMontoMes(nuevoMonto);
            detalle.setHorasMinimas(nuevasHorasMin);

            if (dias < 30) {
                detalle.setTituloPeriodo(String.format(Locale.US, "Mes %d (%s - %s) [%d días]",
                        detalle.getNumeroMes(), detalle.getFechaInicio(), finStr, dias));
            } else {
                detalle.setTituloPeriodo(String.format(Locale.US, "Mes %d (%s - %s)",
                        detalle.getNumeroMes(), detalle.getFechaInicio(), finStr));
            }

            dialog.dismiss();

            if (listener != null) {
                listener.onFechaFinModificada(detalle);
            }
            notifyItemChanged(position);
        });

        dialog.show();
    }

    private void actualizarTextoDias(TextView tvDias, long dias) {
        if (tvDias == null) return;
        if (dias <= 0) {
            tvDias.setText("Duración inválida");
            tvDias.setTextColor(Color.RED);
        } else if (dias < 30) {
            tvDias.setText("Duración: " + dias + " días (corte parcial)");
            tvDias.setTextColor(Color.parseColor("#2877BC"));
        } else {
            tvDias.setText("Duración: " + dias + " días (período completo)");
            tvDias.setTextColor(Color.parseColor("#2877BC"));
        }
    }

    private long calcularDiferenciaDias(String inicio, String fin) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.US);
        try {
            Date d1 = sdf.parse(inicio);
            Date d2 = sdf.parse(fin);

            if (d1 == null || d2 == null) return 30;

            if (d2.before(d1)) return 1;

            long diff = d2.getTime() - d1.getTime();
            long dias = TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS);

            return dias + 1;

        } catch (Exception e) {
            return 30;
        }
    }

    public void forzarGuardadoDatosVisibles(RecyclerView recyclerView) {
        if (recyclerView == null) return;
        Log.d("DetalleMesAdapter", "Iniciando guardado forzado de horómetros...");

        for (int i = 0; i < recyclerView.getChildCount(); i++) {
            View view = recyclerView.getChildAt(i);
            ViewHolder holder = (ViewHolder) recyclerView.getChildViewHolder(view);

            if (holder != null && holder.getAdapterPosition() != RecyclerView.NO_POSITION) {
                guardarValorHorometroEnModelo(holder, items.get(holder.getAdapterPosition()));
            }
        }
        Log.d("DetalleMesAdapter", "Guardado forzado completado.");
    }

    private void configurarConfirmaciones(ViewHolder holder, DetalleMes detalle) {
        if (detalle.isPagoMesConfirmado() && detalle.getFechaConfirmacionPagoMes() != null) {
            holder.tvConfirmacionPagoMes.setText("Pago realizado el " + detalle.getFechaConfirmacionPagoMes());
            holder.tvConfirmacionPagoMes.setVisibility(View.VISIBLE);
        } else {
            holder.tvConfirmacionPagoMes.setVisibility(View.GONE);
        }

        if (detalle.isPagoHEConfirmado() && detalle.getFechaConfirmacionPagoHE() != null) {
            holder.tvConfirmacionPagoHE.setText("Pago realizado el " + detalle.getFechaConfirmacionPagoHE());
            holder.tvConfirmacionPagoHE.setVisibility(View.VISIBLE);
        } else {
            holder.tvConfirmacionPagoHE.setVisibility(View.GONE);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position, @NonNull List<Object> payloads) {
        if (!payloads.isEmpty()) {
            for (Object payload : payloads) {
                if (payload.equals("CALCULATED_FIELDS")) {
                    DetalleMes detalle = items.get(position);
                    holder.inputHorasExtras.setText(String.valueOf((int) detalle.getHorasExtras()));
                    holder.inputPrecioHE.setText(String.format(Locale.US, "%.2f", detalle.getPrecioHorasExtras()));
                    actualizarEstadoBotones(holder, detalle);
                    actualizarColorFranja(holder, detalle);
                    return;
                }
            }
        }
        super.onBindViewHolder(holder, position, payloads);
    }
    private void configurarListeners(ViewHolder holder, DetalleMes detalle, int position) {
        holder.horometroWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                // Evitamos ciclos infinitos verificando si el valor realmente cambió
                String texto = s.toString().trim();
                double valorActual = texto.isEmpty() ? 0 : Double.parseDouble(texto);

                if (valorActual != detalle.getHorometro()) {
                    detalle.setHorometro(valorActual);
                    if (listener != null) {
                        // Notificamos cálculo (sin guardar en BD aún)
                        listener.onHorometroCalculated(detalle, valorActual, position);
                    }
                }
            }
        };
        holder.inputHorometro.addTextChangedListener(holder.horometroWatcher);

        // 2. FocusListener para Guardar en BD al terminar
        holder.inputHorometro.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus && listener != null) {
                listener.onHorometroFinalized(detalle);
            }
        });

        // 3. EditorAction para Guardar al dar "Enter/Done" en el teclado
        holder.inputHorometro.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                ocultarTeclado(v);
                quitarFocoDeInputs(holder);
                if (listener != null) {
                    listener.onHorometroFinalized(detalle);
                }
                return true;
            }
            return false;
        });

        // Listeners de botones (igual que antes)
        holder.btnConfirmarPagoMes.setOnClickListener(v -> mostrarConfirmacion(v.getContext(), detalle, position, true));
        holder.btnConfirmarPagoHE.setOnClickListener(v -> mostrarConfirmacion(v.getContext(), detalle, position, false));
        if (holder.btnGenerarValorizacionMes != null) {
            holder.btnGenerarValorizacionMes.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onGenerarValorizacion(detalle, position);
                }
            });
        }
    }
    private void mostrarConfirmacion(Context context, DetalleMes detalle, int position, boolean esMes) {
        if (listener == null) return;
        new MaterialAlertDialogBuilder(context, R.style.DialogoConFuenteAnta)
                .setTitle("Confirmar Pago")
                .setMessage("¿Estás seguro de confirmar el pago " + (esMes ? "del mes" : "de horas extras") + "?")
                .setPositiveButton("Sí", (dialog, which) -> {
                    String fechaActual = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());
                    if (esMes) {
                        detalle.setPagoMesConfirmado(true);
                        detalle.setFechaConfirmacionPagoMes(fechaActual);
                        listener.onPagoMesConfirmado(detalle, position);
                    } else {
                        detalle.setPagoHEConfirmado(true);
                        detalle.setFechaConfirmacionPagoHE(fechaActual);
                        listener.onPagoHEConfirmado(detalle, position);
                    }
                    notifyItemChanged(position); // Actualizar UI completa del item
                })
                .setNegativeButton("No", null)
                .show();
    }
    private String obtenerFechaActual() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        return sdf.format(new Date());
    }

    private void deshabilitarInputHorometro(ViewHolder holder) {
        holder.inputHorometro.setEnabled(false);
        holder.inputHorometro.setFocusable(false);
        holder.inputHorometro.setFocusableInTouchMode(false);
        holder.inputHorometro.setOnFocusChangeListener(null);
        holder.inputHorometro.setOnEditorActionListener(null);
    }

    private void guardarValorHorometroEnModelo(ViewHolder holder, DetalleMes detalle) {
        String nuevoValor = holder.inputHorometro.getText().toString().trim();
        if (nuevoValor.isEmpty()) {
            detalle.setHorometro(0);
            return;
        }
        try {
            double horometro = Double.parseDouble(nuevoValor);
            detalle.setHorometro(horometro);
        } catch (NumberFormatException e) {
            Log.e("DetalleMesAdapter", "Número inválido al forzar guardado");
        }
    }

    private void procesarYActualizarHorometro(ViewHolder holder, DetalleMes detalle, int position) {
        double horometroAntes = detalle.getHorometro();
        guardarValorHorometroEnModelo(holder, detalle);
        double horometroDespues = detalle.getHorometro();

        if (horometroAntes != horometroDespues && listener != null) {
            listener.onHorometroChanged(detalle, horometroDespues);
            holder.itemView.postDelayed(() -> {
                notifyItemChanged(position, "CALCULATED_FIELDS");
            }, 100);
        }
    }

    private void habilitarCamposEdicion(ViewHolder holder) {
        holder.inputHorometro.setEnabled(true);
        holder.inputHorometro.setFocusable(true);
        holder.inputHorometro.setFocusableInTouchMode(true);
        holder.inputHorometro.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_DONE);
    }

    private void ocultarTeclado(View view) {
        InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private void quitarFocoDeInputs(ViewHolder holder) {
        if (holder == null) return;
        if (holder.inputHorometro != null && holder.inputHorometro.hasFocus()) {
            holder.inputHorometro.clearFocus();
        }
        if (holder.inputHorasExtras != null && holder.inputHorasExtras.hasFocus()) {
            holder.inputHorasExtras.clearFocus();
        }
        if (holder.inputPrecioHE != null && holder.inputPrecioHE.hasFocus()) {
            holder.inputPrecioHE.clearFocus();
        }
        if (holder.itemView != null && holder.itemView.hasFocus()) {
            holder.itemView.clearFocus();
        }
    }

    @Override
    public void onViewRecycled(@NonNull ViewHolder holder) {
        super.onViewRecycled(holder);
        quitarFocoDeInputs(holder);
    }

    private void deshabilitarCampos(ViewHolder holder) {
        holder.inputHorometro.setEnabled(false);
        holder.inputHorometro.setFocusable(false);
        holder.btnConfirmarPagoMes.setEnabled(false);
        holder.btnConfirmarPagoHE.setEnabled(false);
        if (holder.btnGenerarValorizacionMes != null) {
            holder.btnGenerarValorizacionMes.setEnabled(true);
        }
    }

    public List<DetalleMes> getItems() {
        return new ArrayList<>(items);
    }

    private void actualizarEstadoBotones(ViewHolder holder, DetalleMes detalle) {
        boolean habilitarBotones = !modoSoloLectura;

        // Botón HE
        boolean hayHE = detalle.getHorasExtras() > 0;
        holder.btnConfirmarPagoHE.setEnabled(habilitarBotones && hayHE && !detalle.isPagoHEConfirmado());
        holder.btnConfirmarPagoHE.setAlpha((hayHE && !detalle.isPagoHEConfirmado()) ? 1f : 0.5f);

        // Botón Mes
        holder.btnConfirmarPagoMes.setEnabled(habilitarBotones && !detalle.isPagoMesConfirmado());
        holder.btnConfirmarPagoMes.setAlpha(!detalle.isPagoMesConfirmado() ? 1f : 0.5f);

        // Textos de confirmación
        if (detalle.isPagoMesConfirmado()) {
            holder.tvConfirmacionPagoMes.setText("Pago realizado el " + detalle.getFechaConfirmacionPagoMes());
            holder.tvConfirmacionPagoMes.setVisibility(View.VISIBLE);
        } else {
            holder.tvConfirmacionPagoMes.setVisibility(View.GONE);
        }

        if (detalle.isPagoHEConfirmado()) {
            holder.tvConfirmacionPagoHE.setText("Pago realizado el " + detalle.getFechaConfirmacionPagoHE());
            holder.tvConfirmacionPagoHE.setVisibility(View.VISIBLE);
        } else {
            holder.tvConfirmacionPagoHE.setVisibility(View.GONE);
        }
    }

    private void actualizarColorFranja(ViewHolder holder, DetalleMes detalle) {
        int color;
        if (detalle.getHorasExtras() > 0) {
            if (detalle.isPagoMesConfirmado() && detalle.isPagoHEConfirmado()) {
                color = ContextCompat.getColor(holder.itemView.getContext(), R.color.green_accent);
            } else if (detalle.isPagoMesConfirmado() || detalle.isPagoHEConfirmado()) {
                color = ContextCompat.getColor(holder.itemView.getContext(), R.color.yellow_accent);
            } else {
                color = ContextCompat.getColor(holder.itemView.getContext(), R.color.red_accent);
            }
        } else {
            if (detalle.isPagoMesConfirmado()) {
                color = ContextCompat.getColor(holder.itemView.getContext(), R.color.green_accent);
            } else {
                color = ContextCompat.getColor(holder.itemView.getContext(), R.color.red_accent);
            }
        }
        holder.franjaColor.setBackgroundColor(color);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTituloPeriodo, tvMontoMes;
        ImageView btnExpandir, btnEditarFechaFin;
        LinearLayout headerLayout, layoutContenido;
        View franjaColor;
        TextInputLayout textInputHorometro;
        TextInputEditText inputHorometro, inputHorasExtras, inputPrecioHE;
        Button btnConfirmarPagoMes, btnConfirmarPagoHE, btnGenerarValorizacionMes;
        TextView tvConfirmacionPagoMes, tvConfirmacionPagoHE;
        TextWatcher horometroWatcher;

        ViewHolder(View itemView) {
            super(itemView);
            headerLayout = itemView.findViewById(R.id.headerLayout);
            tvTituloPeriodo = itemView.findViewById(R.id.tvTituloPeriodo);
            tvMontoMes = itemView.findViewById(R.id.tvMontoMes);

            btnExpandir = itemView.findViewById(R.id.btnExpandir);
            btnEditarFechaFin = itemView.findViewById(R.id.btnEditarFechaFin);

            layoutContenido = itemView.findViewById(R.id.layoutContenido);
            franjaColor = itemView.findViewById(R.id.franjaColor);
            textInputHorometro = itemView.findViewById(R.id.textInputHorometro);
            inputHorometro = itemView.findViewById(R.id.inputHorometro);
            inputHorasExtras = itemView.findViewById(R.id.inputHorasExtras);
            inputPrecioHE = itemView.findViewById(R.id.inputPrecioHE);
            btnConfirmarPagoMes = itemView.findViewById(R.id.btnConfirmarPagoMes);
            btnConfirmarPagoHE = itemView.findViewById(R.id.btnConfirmarPagoHE);
            btnGenerarValorizacionMes = itemView.findViewById(R.id.btnGenerarValorizacionMes);
            tvConfirmacionPagoMes = itemView.findViewById(R.id.tvConfirmacionPagoMes);
            tvConfirmacionPagoHE = itemView.findViewById(R.id.tvConfirmacionPagoHE);
        }
    }
}
