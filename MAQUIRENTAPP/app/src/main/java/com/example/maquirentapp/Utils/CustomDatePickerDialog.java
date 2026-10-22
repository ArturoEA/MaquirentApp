package com.example.maquirentapp.Utils;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.maquirentapp.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CustomDatePickerDialog {

    public interface OnDateSetListener {
        void onDateSet(int year, int month, int dayOfMonth);
    }

    private final Context context;
    private final OnDateSetListener listener;
    private final Calendar displayedCalendar;
    private final Calendar selectedCalendar;
    private long minDate = -1;
    private long maxDate = -1;

    private AlertDialog dialog;
    private TextView tvMesAnio;
    private RecyclerView recyclerDias;
    private DaysAdapter daysAdapter;

    public CustomDatePickerDialog(@NonNull Context context, int year, int month, int dayOfMonth, OnDateSetListener listener) {
        this.context = context;
        this.listener = listener;

        this.displayedCalendar = Calendar.getInstance();
        this.displayedCalendar.set(year, month, 1);

        this.selectedCalendar = Calendar.getInstance();
        this.selectedCalendar.set(year, month, dayOfMonth);
    }

    public CustomDatePickerDialog(@NonNull Context context, OnDateSetListener listener) {
        this(context,
                Calendar.getInstance().get(Calendar.YEAR),
                Calendar.getInstance().get(Calendar.MONTH),
                Calendar.getInstance().get(Calendar.DAY_OF_MONTH),
                listener);
    }

    public CustomDatePickerDialog setMinDate(long minDateMillis) {
        this.minDate = minDateMillis;
        return this;
    }

    public CustomDatePickerDialog setMaxDate(long maxDateMillis) {
        this.maxDate = maxDateMillis;
        return this;
    }

    public void show() {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_custom_datepicker, null);
        dialog = new AlertDialog.Builder(context)
                .setView(view)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        tvMesAnio = view.findViewById(R.id.tvMesAnio);
        ImageView btnMesAnterior = view.findViewById(R.id.btnMesAnterior);
        ImageView btnMesSiguiente = view.findViewById(R.id.btnMesSiguiente);
        recyclerDias = view.findViewById(R.id.recyclerDiasCalendario);
        Button btnCancel = view.findViewById(R.id.btnDatePickerCancel);
        Button btnOk = view.findViewById(R.id.btnDatePickerOk);

        recyclerDias.setLayoutManager(new GridLayoutManager(context, 7));
        daysAdapter = new DaysAdapter();
        recyclerDias.setAdapter(daysAdapter);

        btnMesAnterior.setOnClickListener(v -> {
            displayedCalendar.add(Calendar.MONTH, -1);
            actualizarCalendario();
        });

        btnMesSiguiente.setOnClickListener(v -> {
            displayedCalendar.add(Calendar.MONTH, 1);
            actualizarCalendario();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnOk.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDateSet(
                        selectedCalendar.get(Calendar.YEAR),
                        selectedCalendar.get(Calendar.MONTH),
                        selectedCalendar.get(Calendar.DAY_OF_MONTH)
                );
            }
            dialog.dismiss();
        });

        actualizarCalendario();
        dialog.show();
    }

    public void dismiss() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }

    private void actualizarCalendario() {
        // Formato Mes Año en mayúsculas (ej: ENERO 2025)
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", new Locale("es", "ES"));
        String texto = sdf.format(displayedCalendar.getTime()).toUpperCase();
        tvMesAnio.setText(texto);

        // Calcular días
        Calendar temp = (Calendar) displayedCalendar.clone();
        temp.set(Calendar.DAY_OF_MONTH, 1);

        // Determinar primer día de semana (Lunes = 0 ... Domingo = 6)
        int dayOfWeek = temp.get(Calendar.DAY_OF_WEEK);
        int emptyDaysBefore = (dayOfWeek + 5) % 7;
        int maxDaysInMonth = temp.getActualMaximum(Calendar.DAY_OF_MONTH);

        List<DayItem> items = new ArrayList<>();
        // Celdas vacías antes del día 1
        for (int i = 0; i < emptyDaysBefore; i++) {
            items.add(new DayItem(0, false, false));
        }

        Calendar checkCal = (Calendar) displayedCalendar.clone();
        for (int dia = 1; dia <= maxDaysInMonth; dia++) {
            checkCal.set(Calendar.DAY_OF_MONTH, dia);

            // Verificar si está deshabilitado por minDate o maxDate
            boolean disabled = false;
            if (minDate > 0) {
                checkCal.set(Calendar.HOUR_OF_DAY, 23);
                checkCal.set(Calendar.MINUTE, 59);
                checkCal.set(Calendar.SECOND, 59);
                if (checkCal.getTimeInMillis() < minDate) {
                    disabled = true;
                }
            }
            if (!disabled && maxDate > 0) {
                checkCal.set(Calendar.HOUR_OF_DAY, 0);
                checkCal.set(Calendar.MINUTE, 0);
                checkCal.set(Calendar.SECOND, 0);
                if (checkCal.getTimeInMillis() > maxDate) {
                    disabled = true;
                }
            }

            boolean isSelected = !disabled
                    && selectedCalendar.get(Calendar.YEAR) == displayedCalendar.get(Calendar.YEAR)
                    && selectedCalendar.get(Calendar.MONTH) == displayedCalendar.get(Calendar.MONTH)
                    && selectedCalendar.get(Calendar.DAY_OF_MONTH) == dia;

            items.add(new DayItem(dia, isSelected, disabled));
        }

        daysAdapter.setItems(items);
    }

    private static class DayItem {
        final int dayNumber;
        final boolean isSelected;
        final boolean isDisabled;

        DayItem(int dayNumber, boolean isSelected, boolean isDisabled) {
            this.dayNumber = dayNumber;
            this.isSelected = isSelected;
            this.isDisabled = isDisabled;
        }
    }

    private class DaysAdapter extends RecyclerView.Adapter<DaysAdapter.DayViewHolder> {
        private final List<DayItem> dayItems = new ArrayList<>();

        void setItems(List<DayItem> items) {
            dayItems.clear();
            if (items != null) {
                dayItems.addAll(items);
            }
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public DayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_datepicker_day, parent, false);
            return new DayViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull DayViewHolder holder, int position) {
            DayItem item = dayItems.get(position);
            if (item.dayNumber == 0) {
                holder.tvDayNumber.setText("");
                holder.tvDayNumber.setBackground(null);
                holder.itemView.setOnClickListener(null);
                holder.itemView.setClickable(false);
                return;
            }

            holder.tvDayNumber.setText(String.valueOf(item.dayNumber));

            if (item.isSelected) {
                holder.tvDayNumber.setBackgroundResource(R.drawable.bg_datepicker_day_selected);
                holder.tvDayNumber.setTextColor(Color.WHITE);
            } else if (item.isDisabled) {
                holder.tvDayNumber.setBackground(null);
                holder.tvDayNumber.setTextColor(Color.parseColor("#444444"));
            } else {
                holder.tvDayNumber.setBackground(null);
                holder.tvDayNumber.setTextColor(Color.WHITE);
            }

            if (!item.isDisabled) {
                holder.itemView.setOnClickListener(v -> {
                    selectedCalendar.set(
                            displayedCalendar.get(Calendar.YEAR),
                            displayedCalendar.get(Calendar.MONTH),
                            item.dayNumber
                    );
                    actualizarCalendario();
                });
                holder.itemView.setClickable(true);
            } else {
                holder.itemView.setOnClickListener(null);
                holder.itemView.setClickable(false);
            }
        }

        @Override
        public int getItemCount() {
            return dayItems.size();
        }

        class DayViewHolder extends RecyclerView.ViewHolder {
            final TextView tvDayNumber;

            DayViewHolder(@NonNull View itemView) {
                super(itemView);
                tvDayNumber = itemView.findViewById(R.id.tvDayNumber);
            }
        }
    }
}
