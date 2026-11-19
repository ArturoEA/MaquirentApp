package com.example.maquirentapp.adapters;

import android.content.Context;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Adapter: one card per machine. Shows pending/overdue tasks from ReporteSaludEquipo.
 * ponytail: no Tarea model needed — works directly with EstadoSaludTarea from the health report.
 */
public class MaquinaTareaAdapter extends RecyclerView.Adapter<MaquinaTareaAdapter.ViewHolder> {

    public interface OnCardClickListener {
        void onCardClicked(GrupoElectrogeno grupo);
//        void onCardClicked(GrupoElectrogeno grupo, MantenimientoSaludManager.ReporteSaludEquipo reporte);
    }

//    /** Wrapper: grupo + its computed health report. */
//    public static class MaquinaConReporte {
//        public final GrupoElectrogeno grupo;
//        public final MantenimientoSaludManager.ReporteSaludEquipo reporte;
//
//        public MaquinaConReporte(GrupoElectrogeno grupo, MantenimientoSaludManager.ReporteSaludEquipo reporte) {
//            this.grupo = grupo;
//            this.reporte = reporte;
//        }
//    }

    private final Context context;
    private final List<GrupoElectrogeno> data = new ArrayList<>();
    //    private final List<MaquinaConReporte> data = new ArrayList<>();
    private final OnCardClickListener listener;

    public MaquinaTareaAdapter(Context context, OnCardClickListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setMachineList(List<GrupoElectrogeno> list) {
//    public void setData(List<MaquinaConReporte> list) {
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
//        MaquinaConReporte item = data.get(position);
//        holder.bind(item);
//        holder.itemView.setOnClickListener(v -> listener.onCardClicked(item.grupo, item.reporte));
        GrupoElectrogeno grupo = data.get(position);
        holder.bind(grupo);
        holder.itemView.setOnClickListener(v -> listener.onCardClicked(grupo));
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvCodigo, tvBadge, tvHorometro;
        private final TextView tvVistazoItem1, tvVistazoItem2, tvMasTareas, tvSinTareas;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCodigo = itemView.findViewById(R.id.tvCodigoMaquinaGrid);
            tvBadge = itemView.findViewById(R.id.tvBadgeEstadoGrid);
            tvHorometro = itemView.findViewById(R.id.tvHorometroMaquinaGrid);
            tvVistazoItem1 = itemView.findViewById(R.id.tvVistazoItem1);
            tvVistazoItem2 = itemView.findViewById(R.id.tvVistazoItem2);
            tvMasTareas = itemView.findViewById(R.id.tvMasTareasGrid);
            tvSinTareas = itemView.findViewById(R.id.tvSinTareasGrid);
        }

        void bind(GrupoElectrogeno grupo) {
//        void bind(MaquinaConReporte item) {
//            GrupoElectrogeno grupo = item.grupo;
//            MantenimientoSaludManager.ReporteSaludEquipo rep = item.reporte;

            tvCodigo.setText(grupo.getCodigo());

            // Health badge — count pending vs total tasks
            List<String> pendingKeys = getPendingKeys(grupo);
            Map<String, Double> relojes = grupo.getRelojesTareas();
            int total = (relojes != null) ? relojes.size() : 0;
            int pendientes = pendingKeys.size();

//            // Badge from real health report
//            tvBadge.setText(rep.badgeGeneral);
//            try {
//                tvBadge.getBackground().setTint(Color.parseColor(rep.colorGeneralHex));
//            } catch (Exception ignored) {}

            if (total == 0) {
                tvBadge.setText("Sin tareas");
            } else if (pendientes == 0) {
                tvBadge.setText("Al día");
                tvBadge.setBackgroundResource(R.drawable.bg_chip_anio_selected);

//            // Horometer
//            if (rep.horometroActual > 0) {
//                tvHorometro.setText(String.format("%.1f h", rep.horometroActual));
            } else {
                tvBadge.setText(pendientes + " pend.");
                tvBadge.setBackgroundResource(R.drawable.bg_chip_anio_unselected);
//                tvHorometro.setText("-");
            }

            // Horometer display — use horometroActual (double)
            double horometro = grupo.getHorometroActual();
            if (horometro > 0) {
                tvHorometro.setText(String.format("%.1f h", horometro));
            } else {
                tvHorometro.setText("-");


//            // Collect overdue + próximo tasks
//            List<MantenimientoSaludManager.EstadoSaludTarea> pendientes = new ArrayList<>();
//            for (MantenimientoSaludManager.EstadoSaludTarea t : rep.todasLasTareas) {
//                if (t.isVencido() || t.estado == MantenimientoSaludManager.EstadoSalud.PROXIMO) {
//                    pendientes.add(t);
//                }
            }

            // Pending tasks preview (max 4 shown, 2 items + "más" indicator)
            tvVistazoItem1.setVisibility(View.GONE);
            tvVistazoItem2.setVisibility(View.GONE);
            tvMasTareas.setVisibility(View.GONE);
            tvSinTareas.setVisibility(View.GONE);

            if (pendingKeys.isEmpty()) {
//            if (pendientes.isEmpty()) {
                tvSinTareas.setVisibility(View.VISIBLE);
                return;
            }

            if (pendingKeys.size() >= 1) {
                String key1 = pendingKeys.get(0);
                Double val1 = relojes.get(key1);
                String label1 = "• " + key1.replace("_", " ");
                if (val1 != null) label1 += " (+" + String.format("%.0f", val1) + " h)";
                tvVistazoItem1.setText(label1);
            }
            if (pendingKeys.size() >= 2) {
                String key2 = pendingKeys.get(1);
                Double val2 = relojes.get(key2);
                String label2 = "• " + key2.replace("_", " ");
                if (val2 != null) label2 += " (+" + String.format("%.0f", val2) + " h)";
                tvVistazoItem2.setText(label2);
            }
            if (pendingKeys.size() > 2) {
                int more = pendingKeys.size() - 2;
                tvMasTareas.setText("+ " + more + " tareas más");
            }
        }

        /**
         * Returns keys from relojesTareas where the value > 0 (pending tasks).
         */
        private List<String> getPendingKeys(GrupoElectrogeno grupo) {
            List<String> pending = new ArrayList<>();
            Map<String, Double> relojes = grupo.getRelojesTareas();
            if (relojes == null) return pending;
            for (Map.Entry<String, Double> entry : relojes.entrySet()) {
                Double valor = entry.getValue();
                if (valor != null && valor > 0) {
                    pending.add(entry.getKey());
                }
            }
            return pending;
        }
    }
}
