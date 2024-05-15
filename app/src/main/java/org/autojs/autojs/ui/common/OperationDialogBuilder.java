package org.autojs.autojs.ui.common;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.stardust.theme.ThemeColor;
import com.stardust.theme.widget.ThemeColorImageView;

import org.autojs.autoxjs.R;

import java.util.ArrayList;

/**
 * Created by Stardust on 2017/6/26.
 */

public class OperationDialogBuilder extends MaterialDialog.Builder {

    private final RecyclerView mOperations;
    private final ArrayList<Integer> mIds = new ArrayList<>();
    private final ArrayList<Integer> mIcons = new ArrayList<>();
    private final ArrayList<String> mTexts = new ArrayList<>();

    public OperationDialogBuilder(@NonNull Context context) {
        super(context);
        mOperations = new RecyclerView(context);
        mOperations.setLayoutManager(new LinearLayoutManager(context));
        mOperations.setAdapter(new RecyclerView.Adapter<ViewHolder>() {
            @Override
            public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.operation_dialog_item, parent, false));
            }

            @Override
            public void onBindViewHolder(ViewHolder holder, int position) {
                holder.itemView.setId(mIds.get(position));
                holder.text.setText(mTexts.get(position));
                holder.icon.setImageResource(mIcons.get(position));
                holder.icon.setThemeColor(new ThemeColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.on_surface)));
            }

            @Override
            public int getItemCount() {
                return mIds.size();
            }
        });
        customView(mOperations, false);
    }

    public OperationDialogBuilder item(int id, int iconRes, int textRes) {
        return item(id, iconRes, getContext().getString(textRes));
    }

    public OperationDialogBuilder item(int id, int iconRes, String text) {
        mIds.add(id);
        mIcons.add(iconRes);
        mTexts.add(text);
        return this;
    }

    public OperationDialogBuilder bindItemClick(Object target) {
        return this;
    }

    @SuppressLint("NonConstantResourceId")
    class ViewHolder extends RecyclerView.ViewHolder {
        ThemeColorImageView icon;
        TextView text;

        public ViewHolder(View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.icon);
            text = itemView.findViewById(R.id.text);
        }

    }
}