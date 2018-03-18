package com.ish.awtest2.adapter;

import android.support.wear.widget.WearableRecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.ish.awtest2.R;
import com.ish.awtest2.bean.SettingItem;

import java.util.List;

/**
 * Created by ish on 2018/3/16.
 */

public class MyAdapter extends WearableRecyclerView.Adapter<MyAdapter.ViewHolder> implements View.OnClickListener, View.OnLongClickListener {

    private List<SettingItem> mList;

    public MyAdapter(List<SettingItem> list){
        mList = list;
    }
    private OnItemClickListener mOnItemClickListener;//声明接口
    private OnItemLongClickListener mOnItemLongClickListener;//声明接口

    @Override
    public void onClick(View v) {
        if (mOnItemClickListener!=null){
            mOnItemClickListener.onItemClick((Integer) v.getTag(),
                    (String)v.getTag(R.string.key));
        }
    }

    @Override
    public boolean onLongClick(View v) {
        if (mOnItemLongClickListener!=null){
            mOnItemLongClickListener.onItemLongClick((Integer) v.getTag(),
                    (String)v.getTag(R.string.key));
        }
        return false;
    }

    public interface OnItemClickListener{
        void onItemClick(int position,String str);
    }

    public interface OnItemLongClickListener{
        void onItemLongClick(int position,String str);
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        mOnItemClickListener = onItemClickListener;
    }

    public void setOnItemLongClickListener(OnItemLongClickListener onItemLongClickListener) {
        mOnItemLongClickListener = onItemLongClickListener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.st_setting_item,
                parent,false);
        ViewHolder holder = new ViewHolder(view);
        view.setOnClickListener(this);
        view.setOnLongClickListener(this);
        return holder;
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        SettingItem item = mList.get(position);
        holder.title.setText(item.getTitle());
        holder.icon.setImageResource(item.getImageId());
        holder.itemView.setTag(position);
        holder.itemView.setTag(R.string.key,(String)item.getTitle());
    }

    @Override
    public int getItemCount() {
        return mList.size();
    }


    /**
     * holder
     */
    static class ViewHolder extends WearableRecyclerView.ViewHolder {
        ImageView icon;
        TextView title;

        public ViewHolder(View v) {
            super(v);
            icon = (ImageView) v.findViewById(R.id.st_setting_item_image);
            title = (TextView) v.findViewById(R.id.st_setting_item_tv);
        }
    }
}
