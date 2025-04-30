package com.example.travist;

import android.app.DatePickerDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class SelectedKeypointsModifyAdapter extends RecyclerView.Adapter<SelectedKeypointsModifyAdapter.ViewHolder> {
    // Initialisation des variables
    private OnItemRemoveListener removeListener;
    public static Map<Integer, String> visitStartDates = new HashMap<>();
    public static Map<Integer, String> visitEndDates = new HashMap<>();
    public static Map<Integer,String> availStartDates = new HashMap<>();
    public static Map<Integer,String> availEndDates   = new HashMap<>();

    // Constructeur de l'Adapter
    public SelectedKeypointsModifyAdapter(OnItemRemoveListener removeListener) {
        this.removeListener = removeListener;
    }

    // Méthode onCreateViewHolder pour la gestion du layout de l'item, en fonction de sa vue
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.selected_kp_item, parent, false);
        return new ViewHolder(view);
    }

    // Méthode onBindViewHolder pour définir les attributs de l'item
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Keypoint kp = KpListHolderModify.selectedKeypointsModify.get(position);
        holder.bind(kp);
    }

    // Méthode pour retourner le nombre d'items dans l'adapter
    @Override
    public int getItemCount() {
        return KpListHolderModify.selectedKeypointsModify.size();
    }

    // Classe ViewHolder pour définir et attribuer les éléments de la vue
    public class ViewHolder extends RecyclerView.ViewHolder {

        TextView tvKeypointName;
        EditText etStartDate;
        EditText etEndDate;
        Button btnRemove;

        public ViewHolder(View itemView) {
            super(itemView);
            tvKeypointName = itemView.findViewById(R.id.tvKpItemName);
            etStartDate = itemView.findViewById(R.id.etKpItemStartDate);
            etEndDate = itemView.findViewById(R.id.etKpItemEndDate);
            btnRemove = itemView.findViewById(R.id.deleteSelectedBtn);

            etStartDate.setFocusable(false);
            etStartDate.setOnClickListener(v -> showDate(true));
            etEndDate.setFocusable(false);
            etEndDate.setOnClickListener(v -> showDate(false));

            btnRemove.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && removeListener != null) {
                    removeListener.onRemove(KpListHolderModify.selectedKeypointsModify.get(pos));
                }
            });
        }

        // Méthode pour afficher les dates de visites des lieux sélectionnés, soit celles de base, soit celles définies lors de la création du voyage
        private void showDate(boolean isStart) {
            int pos = getAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;

            Keypoint kp = KpListHolderModify.selectedKeypointsModify.get(pos);

            // bornes globales
            String globalStart = availStartDates.getOrDefault(kp.id, kp.startDate);
            String globalEnd   = availEndDates  .getOrDefault(kp.id, kp.endDate);
            long min = parseDate(globalStart);
            long max = parseDate(globalEnd);

            // valeur courante = visit ou default
            String current = isStart
                    ? visitStartDates.getOrDefault(kp.id, kp.startDate)
                    : visitEndDates  .getOrDefault(kp.id, kp.endDate);
            long initial = parseDate(current);

            showDatePickerDialog(
                    isStart ? etStartDate : etEndDate,
                    min, max, initial,
                    date -> {
                        if (isStart) {
                            visitStartDates.put(kp.id, date);
                            etStartDate.setText(date);
                        } else {
                            visitEndDates.put(kp.id, date);
                            etEndDate.setText(date);
                        }
                    }
            );
        }

        public void bind(Keypoint kp) {
            tvKeypointName.setText(kp.name);

            String start = visitStartDates.getOrDefault(kp.id, kp.startDate);
            String end   = visitEndDates  .getOrDefault(kp.id, kp.endDate);
            etStartDate.setText(start);
            etEndDate.setText(end);
        }

        // Méthode pour afficher le DatePicker
        private void showDatePickerDialog(EditText editText, long min, long max, long initial, OnDateSelectedListener listener) {
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(initial);
            int y = c.get(Calendar.YEAR), m = c.get(Calendar.MONTH), d = c.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog dialog = new DatePickerDialog(editText.getContext(), (v, year, month, day) -> {
                String date = String.format("%d-%02d-%02d", year, month + 1, day);
                listener.onDateSelected(date);
            }, y, m, d);

            dialog.getDatePicker().setMinDate(min);
            dialog.getDatePicker().setMaxDate(max);
            dialog.show();
        }

        // Méthode pour parser une date dans le bon format
        private long parseDate(String dateStr) {
            try {
                return new SimpleDateFormat("yyyy-MM-dd").parse(dateStr).getTime();
            } catch (ParseException e) {
                return System.currentTimeMillis();
            }
        }
    }

    /* --- Interfaces pour les éléments de l'adapter --- */
    public interface OnItemRemoveListener {
        void onRemove(Keypoint kp);
    }

    public interface OnDateSelectedListener {
        void onDateSelected(String date);
    }
    /* ------------------------------------------------- */
}