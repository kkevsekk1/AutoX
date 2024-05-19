package org.autojs.autojs.ui.common;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.autojs.autoxjs.R;

import java.util.ArrayList;

/**
 * Created by Stardust on 2017/10/20.
 */

public class OptionListView extends LinearLayout {


    public static class Builder {

        private final OptionListView mOptionListView;
        private final Context mContext;

        public Builder(Context context) {
            mContext = context;
            mOptionListView = (OptionListView) View.inflate(context, R.layout.option_list_view, null);
        }

        public Builder item(int id, int iconRes, int textRes) {
            return item(id, iconRes, mContext.getString(textRes));
        }

        public Builder item(int id, int iconRes, String text) {
            mOptionListView.mIds.add(id);
            mOptionListView.mIcons.add(iconRes);
            mOptionListView.mTexts.add(text);
            return this;
        }

        public Builder title(String title) {
            mOptionListView.mTitleView.setVisibility(VISIBLE);
            mOptionListView.mTitleView.setText(title);
            return this;
        }

        public Builder title(int title) {
            return title(mContext.getString(title));
        }

        public OptionListView build() {
            return mOptionListView;
        }
    }


    private final ArrayList<Integer> mIds = new ArrayList<>();
    private final ArrayList<Integer> mIcons = new ArrayList<>();
    private final ArrayList<String> mTexts = new ArrayList<>();
    private RecyclerView mOptionList;
    private TextView mTitleView;

    public OptionListView(Context context) {
        super(context);
    }

    public OptionListView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mTitleView = (TextView) findViewById(R.id.title);
        mOptionList = (RecyclerView) findViewById(R.id.list);
        mOptionList.setLayoutManager(new LinearLayoutManager(getContext()));
        mOptionList.setAdapter(new Adapter());
    }

    private class Adapter extends RecyclerView.Adapter<ViewHolder> {
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.operation_dialog_item, parent, false));
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            holder.itemView.setId(mIds.get(position));
            holder.text.setText(mTexts.get(position));
            holder.icon.setImageResource(mIcons.get(position));
        }

        @Override
        public int getItemCount() {
            return mIds.size();
        }
    }


    class ViewHolder extends RecyclerView.ViewHolder {
        ImageView icon;
        TextView text;

        public ViewHolder(View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.icon);
            text = itemView.findViewById(R.id.text);
        }

    }
}
