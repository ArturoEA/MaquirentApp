package com.example.maquirentapp.adapters;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.maquirentapp.Model.GrupoElectrogeno;
import com.example.maquirentapp.R;
import com.example.maquirentapp.Utils.MantenimientoSaludManager;
import com.example.maquirentapp.Utils.MantenimientoSaludManager.EstadoSalud;
import com.example.maquirentapp.Utils.MantenimientoSaludManager.EstadoSaludTarea;
import com.example.maquirentapp.Utils.MantenimientoSaludManager.ReporteSaludEquipo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Adapter de tarjetas de máquinas en Lista de Tareas.
 * Muestra el horómetro sin decimales y únicamente las actividades vencidas y próximas a vencer.
 */
public class MaquinaTareaAdapter extends RecyclerView.Adapter<MaquinaTareaAdapter.ViewHolder> {

    public interface OnCardClickListener {
        void onCardClicked(GrupoElectrogeno grupo, ReporteSaludEquipo reporte);
    }

    public static class MaquinaConReporte {
        public final GrupoElectrogeno grupo;
        public final ReporteSaludEquipo reporte;

        public MaquinaConReporte(GrupoElectrogeno grupo, ReporteSaludEquipo reporte) {
            this.grupo = grupo;
            this.reporte = reporte;
        }
    }

    private final Context context;
    private final List<MaquinaConReporte> data = new ArrayList<>();
    private final OnCardClickListener listener;

    public MaquinaTareaAdapter(Context context, OnCardClickListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setData(List<MaquinaConReporte> list) {
        data.clear();
        if (list != null) data.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_maquina_tarea_grid, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MaquinaConReporte item = data.get(position);
        holder.bind(item);
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCardClicked(item.grupo, item.reporte);
            }
        });
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvCodigo, tvSubtitulo, tvBadge, tvHorometro;
        private final TextView tvVistazoItem1, tvVistazoItem2, tvMasTareas, tvSinTareas;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCodigo = itemView.findViewById(R.id.tvCodigoMaquinaGrid);
            tvSubtitulo = itemView.findViewById(R.id.tvSubtituloMaquinaGrid);
            tvBadge = itemView.findViewById(R.id.tvBadgeEstadoGrid);
            tvHorometro = itemView.findViewById(R.id.tvHorometroMaquinaGrid);
            tvVistazoItem1 = itemView.findViewById(R.id.tvVistazoItem1);
            tvVistazoItem2 = itemView.findViewById(R.id.tvVistazoItem2);
            tvMasTareas = itemView.findViewById(R.id.tvMasTareasGrid);
            tvSinTareas = itemView.findViewById(R.id.tvSinTareasGrid);
        }

        void bind(MaquinaConReporte item) {
            GrupoElectrogeno grupo = item.grupo;
            ReporteSaludEquipo rep = item.reporte;

            tvCodigo.setText(grupo.getCodigo() != null ? grupo.getCodigo() : "-");
            if (tvSubtitulo != null) {
                tvSubtitulo.setText(rep != null && rep.tipoEquipo != null ? rep.tipoEquipo : "Grupo electrógeno");
            }

            double horometro = (rep != null && rep.horometroActual > 0)
                    ? rep.horometroActual
                    : grupo.getHorometroActual();
            tvHorometro.setText(MantenimientoSaludManager.formatearHorometroSinDecimal(horometro));

            // Filtrar únicamente tareas vencidas y próximas a vencer
            List<EstadoSaludTarea> pendientes = new ArrayList<>();
            int vencidas = 0;
            int proximas = 0;
            if (rep != null && rep.todasLasTareas != null) {
                for (EstadoSaludTarea t : rep.todasLasTareas) {
                    if (t.isVencido()) {
                        pendientes.add(t);
                        vencidas++;
                    } else if (t.estado == EstadoSalud.PROXIMO) {
                        pendientes.add(t);
                        proximas++;
                    }
                }
            }

            // Ordenar: vencidas primero, luego próximas
            Collections.sort(pendientes, (a, b) -> Boolean.compare(!a.isVencido(), !b.isVencido()));

            if (vencidas > 0) {
                tvBadge.setText(vencidas + " venc.");
                tvBadge.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#D32F2F")));
            } else if (proximas > 0) {
                tvBadge.setText(proximas + " próx.");
                tvBadge.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#F57C00")));
            } else {
                tvBadge.setText("Al día");
                tvBadge.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#388E3C")));
            }

            tvVistazoItem1.setVisibility(View.GONE);
            tvVistazoItem2.setVisibility(View.GONE);
            tvMasTareas.setVisibility(View.GONE);
            tvSinTareas.setVisibility(View.GONE);

            if (pendientes.isEmpty()) {
                tvSinTareas.setVisibility(View.VISIBLE);
                return;
            }

            if (pendientes.size() >= 1) {
                aplicarVistazo(tvVistazoItem1, pendientes.get(0));
            }
            if (pendientes.size() >= 2) {
                aplicarVistazo(tvVistazoItem2, pendientes.get(1));
            }
            if (pendientes.size() > 2) {
                int mas = pendientes.size() - 2;
                tvMasTareas.setText("+ " + mas + (mas == 1 ? " tarea más" : " tareas más"));
                tvMasTareas.setVisibility(View.VISIBLE);
            }
        }

        private void aplicarVistazo(TextView tv, EstadoSaludTarea t) {
            String nombre = t.nombre != null ? t.nombre : "Actividad";
            String sufijo;
            int color;
            if (t.estado == EstadoSalud.VENCIDO_HORAS) {
                sufijo = String.format(Locale.US, " (+%.0fh)", Math.max(0, t.horasExceso));
                color = Color.parseColor("#EF9A9A");
            } else if (t.estado == EstadoSalud.VENCIDO_TIEMPO) {
                sufijo = " (" + t.mesesTranscurridos + "m)";
                color = Color.parseColor("#EF9A9A");
            } else if (t.isVencido()) {
                sufijo = " (venc.)";
                color = Color.parseColor("#EF9A9A");
            } else {
                sufijo = t.horasRestantes > 0
                        ? String.format(Locale.US, " (en %.0fh)", t.horasRestantes)
                        : " (próx.)";
                color = Color.parseColor("#FFE082");
            }
            tv.setText("• " + nombre + sufijo);
            tv.setTextColor(color);
            tv.setVisibility(View.VISIBLE);
        }
    }
}
