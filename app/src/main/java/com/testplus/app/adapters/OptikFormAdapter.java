package com.testplus.app.adapters;

import android.view.*;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.testplus.app.R;
import com.testplus.app.database.entities.OptikForm;
import java.util.ArrayList;
import java.util.List;

public class OptikFormAdapter extends RecyclerView.Adapter<OptikFormAdapter.ViewHolder> {
    public interface OnItemClickListener { void onItemClick(OptikForm form); }
    public interface OnItemLongClickListener { void onItemLongClick(OptikForm form); }

    private List<OptikForm> data = new ArrayList<>();
    private final OnItemClickListener clickListener;
    private final OnItemLongClickListener longClickListener;

    public OptikFormAdapter(OnItemClickListener click, OnItemLongClickListener longClick) {
        this.clickListener = click;
        this.longClickListener = longClick;
    }

    public void setData(List<OptikForm> list) {
        data.clear();
        if (list != null) data.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_optik_form, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        OptikForm form = data.get(position);
        holder.tvAd.setText(form.ad);
        holder.tvBilgi.setText(form.kagit + " • " + form.yon);
        holder.itemView.setOnClickListener(v -> clickListener.onItemClick(form));
        holder.itemView.setOnLongClickListener(v -> { longClickListener.onItemLongClick(form); return true; });
    }

    @Override
    public int getItemCount() { return data.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvAd, tvBilgi;
        ViewHolder(View view) {
            super(view);
            tvAd = view.findViewById(R.id.tvAd);
            tvBilgi = view.findViewById(R.id.tvBilgi);
        }
    }
}
