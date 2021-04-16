package org.autojs.autojs.ui.project;


import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.stardust.app.DialogUtils;
import com.stardust.autojs.core.image.Colors;
import com.stardust.autojs.workground.WrapContentLinearLayoutManager;
import com.stardust.pio.PFiles;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.ViewById;
import org.autojs.autojs.Pref;
import org.autojs.autojs.R;
import org.autojs.autojs.build.ApkKeyStore;
import org.autojs.autojs.build.ApkSigner;
import org.autojs.autojs.theme.dialog.ThemeColorMaterialDialogBuilder;
import org.autojs.autojs.ui.BaseActivity;

import java.util.ArrayList;

import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;

import static com.stardust.app.GlobalAppContext.getString;

@EActivity(R.layout.activity_sign_manage)
public class SignManageActivity extends BaseActivity {

    private static final String LOG_TAG = "SignManageActivity";

    private ArrayList<ApkKeyStore> mKeyStoreList = new ArrayList<>();

    @ViewById(R.id.sign_key_recycle)
    RecyclerView mRecycleView;

    private Adapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @AfterViews
    void setupViews() {
        setToolbarAsBack(getString(R.string.text_sign_manage));
        mRecycleView.setLayoutManager(new WrapContentLinearLayoutManager(this));
        loadKeyStore();
    }

    private void loadKeyStore() {
        mKeyStoreList = ApkSigner.loadKeyStore();
        mAdapter = new Adapter(mKeyStoreList);
        mRecycleView.setAdapter(mAdapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_sign_manage, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_keystore_add:
                showKeyCreateDialog();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showKeyCreateDialog() {
        new SignKeyCreateDialogBuilder(this)
                .title(R.string.text_sign_key_add)
                .whenCreated(path -> {
                    mAdapter.addItem(ApkSigner.loadApkKeyStore(path));
                })
                .autoDismiss(false)
                .show();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private Context mContext;
        private ApkKeyStore mItem;
        private CardView mItemCard;
        private TextView mPath;
        private TextView mAlias;
        private TextView mVerified;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            mContext = itemView.getContext();
            mItemCard = itemView.findViewById(R.id.item_card);
            mPath = itemView.findViewById(R.id.item_key_path);
            mAlias = itemView.findViewById(R.id.item_key_alias);
            mVerified = itemView.findViewById(R.id.item_verified);

            mItemCard.setOnClickListener(view -> {
                if (!mItem.isVerified()) {
                    showPasswordInputDialog(mItem);
                }
            });
        }

        private void bind(ApkKeyStore item) {
            this.mItem = item;
            mPath.setText(item.getName());
            mAlias.setText(item.getAlias());
            boolean isVerified = item.isVerified();
            if (isVerified) {
                mVerified.setTextColor(Colors.GREEN);
                mVerified.setText("已验证");
            } else {
                mVerified.setTextColor(Colors.RED);
                mVerified.setText("未验证");

            }
        }

        private void showPasswordInputDialog(ApkKeyStore mItem) {
            DialogUtils.showDialog(new ThemeColorMaterialDialogBuilder(mContext).title(R.string.text_sign_password)
                    .inputType(InputType.TYPE_TEXT_VARIATION_PASSWORD)
                    .autoDismiss(false)
                    .input(mContext.getString(R.string.text_sign_password_input), "", true, new MaterialDialog.InputCallback() {

                        @Override
                        public void onInput(@NonNull MaterialDialog dialog, CharSequence input) {

                        }
                    })
                    .onPositive((dialog, which) -> {
                        String password = dialog.getInputEditText().getText().toString();
                        if (ApkSigner.checkKeyStore(mItem.getPath(), password)) {
                            Pref.setKeyStorePassWord(PFiles.getName(mItem.getPath()), password);
                            mItem.setVerified(true);
                            mVerified.setTextColor(Colors.GREEN);
                            mVerified.setText("已验证");
                            dialog.dismiss();
                        } else {
                            dialog.getInputEditText().setError("验证失败");
                        }
                    })
                    .build());
        }
    }

    public static class Adapter extends RecyclerView.Adapter<ViewHolder> {

        private ArrayList<ApkKeyStore> mList;

        public Adapter(ArrayList<ApkKeyStore> mList) {
            this.mList = mList;
        }

        public void refreshList(ArrayList<ApkKeyStore> mList) {
            this.mList = mList;
            notifyDataSetChanged();
        }

        public void addItem(ApkKeyStore item) {
            if (item != null) {
                mList.add(item);
                notifyItemInserted(getItemCount());
            }
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ViewHolder viewHolder = new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_key_store, parent, false));
            return viewHolder;
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.bind(mList.get(position));
        }

        @Override
        public int getItemCount() {
            return mList.size();
        }
    }
}