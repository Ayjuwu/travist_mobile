    package com.example.travist;

    import android.view.LayoutInflater;
    import android.view.View;
    import android.view.ViewGroup;
    import android.widget.Button;
    import android.widget.TextView;

    import androidx.recyclerview.widget.RecyclerView;

    import java.util.List;

    public class TagAdapter extends RecyclerView.Adapter<TagAdapter.ViewHolder> {

        // Interface dédiée pour chaque élément de l'adapter
        public interface OnTagActionListener {
            void onModify(Tag tag);
            void onDelete(Tag tag);
        }

        // Initialisation des variables
        private List<Tag> tags;
        private OnTagActionListener listener;

        // Contructeur de l'adapter
        public TagAdapter(List<Tag> tags, OnTagActionListener listener) {
            this.tags = tags;
            this.listener = listener;
        }

        // Méthode onCreateViewHolder pour la gestion du layout de l'item, en fonction de sa vue
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.tag_item, parent, false);
            return new ViewHolder(view);
        }

        // Méthode onBindViewHolder pour définir les attributs de l'item (vue + appel des méthodes du listener => interface)
        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            Tag tag = tags.get(position);
            holder.tvTagId.setText(String.valueOf(tag.id));
            holder.tvTagName.setText(tag.name);
            holder.btnModify.setOnClickListener(v -> listener.onModify(tag));
            holder.btnDelete.setOnClickListener(v -> listener.onDelete(tag));
        }

        // Méthode pour retourner le nombre d'items dans l'adapter
        @Override
        public int getItemCount() {
            return tags.size();
        }

        // Classe ViewHolder pour définir et attribuer les éléments de la vue
        public class ViewHolder extends RecyclerView.ViewHolder {
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
