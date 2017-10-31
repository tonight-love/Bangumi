package com.fanchen.imovie.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;

import com.fanchen.imovie.R;
import com.fanchen.imovie.adapter.HistoryAdapter;
import com.fanchen.imovie.base.BaseAdapter;
import com.fanchen.imovie.base.BaseRecyclerActivity;
import com.fanchen.imovie.db.LiteOrmManager;
import com.fanchen.imovie.dialog.BaseAlertDialog;
import com.fanchen.imovie.dialog.OnButtonClickListener;
import com.fanchen.imovie.entity.bmob.VideoHistory;
import com.fanchen.imovie.retrofit.RetrofitManager;
import com.fanchen.imovie.thread.AsyTaskQueue;
import com.fanchen.imovie.thread.task.AsyTaskListenerImpl;
import com.fanchen.imovie.util.DialogUtil;
import com.fanchen.imovie.view.CustomEmptyView;
import com.litesuits.orm.LiteOrm;
import com.litesuits.orm.db.assit.WhereBuilder;
import com.squareup.picasso.Picasso;

import java.util.List;

/**
 * 播放历史
 * Created by fanchen on 2017/8/15.
 */
public class HistoryActivity extends BaseRecyclerActivity implements BaseAdapter.OnItemLongClickListener {

    private static final String[] DIALOG_TITLE = new String[]{"删除记录", "直接打开"};

    private HistoryAdapter mHistoryAdapter;

    private LiteOrm liteOrm;

    /**
     * @param context
     */
    public static void startActivity(Context context) {
        Intent intent = new Intent(context, HistoryActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected void initActivity(Bundle savedState, LayoutInflater inflater) {
        liteOrm = LiteOrmManager.getInstance(this).getLiteOrm("imovie.db");
        super.initActivity(savedState, inflater);
    }

    @Override
    protected String getActivityTitle() {
        return getString(R.string.player_history);
    }

    @Override
    protected RecyclerView.LayoutManager getLayoutManager() {
        return new LinearLayoutManager(this);
    }

    @Override
    protected BaseAdapter getAdapter(Picasso picasso) {
        return mHistoryAdapter = new HistoryAdapter(this, picasso);
    }

    @Override
    protected void setListener() {
        super.setListener();
        mHistoryAdapter.setOnItemLongClickListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_clear, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_clear) {
            DialogUtil.showMaterialDialog(this, getString(R.string.delete_history), buttonClickListener);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void loadData(RetrofitManager retrofit,int page) {
        AsyTaskQueue.newInstance().execute(queryCallback);
    }

    @Override
    public void onItemClick(List<?> datas, View v, int position) {
        if (datas == null || datas.size() <= position || position < 0) return;
    }

    @Override
    public boolean onItemLongClick(List<?> datas, View v, int position) {
        if (datas == null || datas.size() <= position || position < 0) return false;
        return true;
    }

    /**
     *
     */
    private TaskRecyclerActivityImpl<List<VideoHistory>> queryCallback = new TaskRecyclerActivityImpl<List<VideoHistory>>() {

        @Override
        public List<VideoHistory> onTaskBackground() {
            if (isFinishing() || liteOrm == null) return null;
            return liteOrm.query(VideoHistory.class);
        }

        @Override
        public void onTaskSuccess(List<VideoHistory> data) {
            if (isFinishing() || data == null) return;
            mHistoryAdapter.clear();
            mHistoryAdapter.addAll(data);
        }

    };

    /**
     *
     */
    private OnButtonClickListener buttonClickListener = new OnButtonClickListener() {

        @Override
        public void onButtonClick(BaseAlertDialog<?> dialog, int btn) {
            if (btn == OnButtonClickListener.RIGHT) {
                AsyTaskQueue.newInstance().execute(new DeleteListenerImpl());
            }
            dialog.dismiss();
        }

    };

    /**
     *
     */
    private class ItemClickListener implements AdapterView.OnItemClickListener {

        private int position;
        private VideoHistory item;

        public ItemClickListener(VideoHistory item, int position) {
            this.position = position;
            this.item = item;
        }

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            switch (position) {
                case 0:
                    AsyTaskQueue.newInstance().execute(new DeleteListenerImpl(item.getId(), this.position));
                    break;
                case 1:
                    HistoryActivity.this.onItemClick(mHistoryAdapter.getList(), view, this.position);
                    break;
                default:
                    break;
            }
        }
    }

    /**
     *
     */
    private class DeleteListenerImpl extends AsyTaskListenerImpl<Integer> {
        public int DELETEALL = -1;
        public int DELETEERROR = -2;

        private int pisotion = -1;
        private String id;

        public DeleteListenerImpl(String id, int pisotion) {
            this.id = id;
            this.pisotion = pisotion;
        }

        public DeleteListenerImpl() {
        }

        @Override
        public Integer onTaskBackground() {
            if (isFinishing() || liteOrm == null) return DELETEERROR;
            if (TextUtils.isEmpty(id) && pisotion == -1) {
                //删除全部
                liteOrm.delete(VideoHistory.class);
                return DELETEALL;
            } else {
                //按条件删除
                liteOrm.delete(new WhereBuilder(VideoHistory.class, "id = ? ", new Object[]{id}));
                return pisotion;
            }
        }

        @Override
        public void onTaskSuccess(Integer data) {
            if (isFinishing()) return;
            if (data == DELETEERROR) {
                showSnackbar(getString(R.string.delete_error));
            } else {
                if (data == DELETEALL) {
                    mHistoryAdapter.clear();
                } else {
                    mHistoryAdapter.remove(data);
                }
                if(mHistoryAdapter.getList().size() == 0){
                    mCustomEmptyView.setEmptyType(CustomEmptyView.TYPE_EMPTY);
                }
            }
        }
    }

}