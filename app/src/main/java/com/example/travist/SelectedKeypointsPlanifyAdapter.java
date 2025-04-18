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

public class SelectedKeypointsPlanifyAdapter extends RecyclerView.Adapter<SelectedKeypointsPlanifyAdapter.ViewHolder> {

    private OnItemRemoveListener removeListener;
    public static Map<Integer, String> visitStartDates = new HashMap<>();
    public static Map<Integer, String> visitEndDates = new HashMap<>();

    public SelectedKeypointsPlanifyAdapter(OnItemRemoveListener removeListener) {
        this.removeListener = removeListener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.selected_kp_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Keypoint kp = KpListHolderPlanify.selectedKeypointsPlanify.get(position);
        holder.bind(kp);
    }

    @Override
    public int getItemCount() {
        return KpListHolderPlanify.selectedKeypointsPlanify.size();
    }

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
                    removeListener.onRemove(KpListHolderPlanify.selectedKeypointsPlanify.get(pos));
                }
            });
        }

        private void showDate(boolean isStart) {
            int pos = getAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;

            Keypoint kp = KpListHolderPlanify.selectedKeypointsPlanify.get(pos);
            long min = parseDate(kp.startDate);
            long max = parseDate(kp.endDate);

            String currentDate = isStart ? visitStartDates.get(kp.id) : visitEndDates.get(kp.id);
            if (currentDate == null || currentDate.isEmpty()) {
                currentDate = isStart ? kp.startDate : kp.endDate;
            }
            long initial = parseDate(currentDate);

            showDatePickerDialog(isStart ? etStartDate : etEndDate, min, max, initial, date -> {
                if (isStart) {
                    visitStartDates.put(kp.id, date);
                    etStartDate.setText(date);
                } else {
                    visitEndDates.put(kp.id, date);
                    etEndDate.setText(date);
                }
            });
        }

        public void bind(Keypoint kp) {
            tvKeypointName.setText(kp.name);

            String start = visitStartDates.get(kp.id);
            String end = visitEndDates.get(kp.id);

            etStartDate.setText(start != null ? start : kp.startDate);
            etEndDate.setText(end != null ? end : kp.endDate);
        }

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

        private long parseDate(String dateStr) {
            try {
                return new SimpleDateFormat("yyyy-MM-dd").parse(dateStr).getTime();
            } catch (ParseException e) {
                return System.currentTimeMillis();
            }
        }
    }

    public interface OnItemRemoveListener {
        void onRemove(Keypoint kp);
    }

    public interface OnDateSelectedListener {
        void onDateSelected(String date);
    }
}