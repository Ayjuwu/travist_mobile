    package com.example.travist;

    import android.view.LayoutInflater;
    import android.view.View;
    import android.view.ViewGroup;
    import android.widget.Button;
    import android.widget.TextView;

    import androidx.recyclerview.widget.RecyclerView;

    import java.util.List;

    public class TagAdapter extends RecyclerView.Adapter<TagAdapter.ViewHolder> {

        public interface OnTagActionListener {
            void onModify(Tag tag);
            void onDelete(Tag tag);
        }

        private List<Tag> tags;
        private OnTagActionListener listener;

        public TagAdapter(List<Tag> tags, OnTagActionListener listener) {
            this.tags = tags;
            this.listener = listener;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.tag_item, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            Tag tag = tags.get(position);
            holder.tvTagId.setText(String.valueOf(tag.id));
            holder.tvTagName.setText(tag.name);
            holder.btnModify.setOnClickListener(v -> listener.onModify(tag));
            holder.btnDelete.setOnClickListener(v -> listener.onDelete(tag));
        }

        @Override
        public int getItemCount() {
            return tags.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTagId, tvTagName;
            Button btnModify, btnDelete;
            ViewHolder(View item) {
                super(item);
                tvTagId   = item.findViewById(R.id.tvTagId);
                tvTagName = item.findViewById(R.id.tvTagName);
                btnModify = item.findViewById(R.id.modifyTagBtn);
                btnDelete = item.findViewById(R.id.deleteTagBtn);
            }
        }
    }
