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

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by Stardust on 2017/6/26.
 */

public class OperationDialogBuilder extends MaterialDialog.Builder {

    private RecyclerView mOperations;
    private ArrayList<Integer> mIds = new ArrayList<>();
    private ArrayList<Integer> mIcons = new ArrayList<>();
    private ArrayList<String> mTexts = new ArrayList<>();
    private Object mOnItemClickTarget;

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
                if (mOnItemClickTarget != null) {
                    //// TODO: 2017/6/26   效率
                    ButterKnife.bind(mOnItemClickTarget, holder.itemView);
                }
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
        mOnItemClickTarget = target;
        return this;
    }

    @SuppressLint("NonConstantResourceId")
    class ViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.icon)
        ThemeColorImageView icon;
        @BindView(R.id.text)
        TextView text;

        public ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);

        }

    }
}