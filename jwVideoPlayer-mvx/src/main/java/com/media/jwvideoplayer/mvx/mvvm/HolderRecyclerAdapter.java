package com.media.jwvideoplayer.mvx.mvvm;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public abstract class HolderRecyclerAdapter<T, H extends RecyclerView.ViewHolder>
        extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements ViewHolder.OnItemClick {

    private Context context;

    private List<T> listData;

    private LayoutInflater layoutInflater;

    private View mFooterView;

    private OnItemClickListener mOnItemClickListener;
    private OnItemChildClickListener mOnItemChildClickListener;

    public interface OnItemClickListener<T> {
        void onItemClick(T data, int position);

        void onItemLongClick(T data, int position);
    }

    public void setOnItemClickListener(OnItemClickListener clickListener) {
        this.mOnItemClickListener = clickListener;
    }

    protected void onItemClick(T data, int position) {
        if (mOnItemClickListener != null) {
            mOnItemClickListener.onItemClick(data, position);
        }
    }

    protected void onItemLongClick(T data, int position) {
        if (mOnItemClickListener != null) {
            mOnItemClickListener.onItemLongClick(data, position);
        }
    }

    public interface OnItemChildClickListener {
        void onItemChildClick(View v, int position);
    }

    public void setOnItemChildClickListener(OnItemChildClickListener clickListener) {
        this.mOnItemChildClickListener = clickListener;
    }

    public HolderRecyclerAdapter(Context context, List<T> listData) {
        super();
        this.context = context;
        this.listData = new ArrayList<T>();
        if (listData != null) {
            this.listData.addAll(listData);
        }
        this.layoutInflater = LayoutInflater.from(context);
    }

    @Override
    public void onItemClick(View v, int position, boolean isChild) {
        if (isChild) {
            if (mOnItemChildClickListener != null) {
                mOnItemChildClickListener.onItemChildClick(v, position);
            }
        } else {
            onItemClick(getListData() == null ? null : getListData().get(position), position);
        }

    }

    public LayoutInflater getLayoutInflater() {
        return layoutInflater;
    }

    public Context getContext() {
        return context;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if(viewType == 1){
            return new FooterViewHolder(mFooterView);
        }
        View itemView = buildConvertView(layoutInflater, parent, viewType);
        return buildHolder(itemView, viewType);
    }

    @Override
    public int getItemViewType(int position) {
        if (position == listData.size()) {
            return 1;
        }
        return super.getItemViewType(position);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, @SuppressLint("RecyclerView") final int position) {
        if (mFooterView != null && viewHolder instanceof FooterViewHolder) {
            return;
        }

        H holder = (H) viewHolder;

        T item = position < listData.size() ? listData.get(position) : null;

        bindViewDatas(holder, item, position);
        if (this.mOnItemClickListener != null) {
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onItemClick(item, position);
                }
            });

            holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    onItemLongClick(item, position);
                    return true;
                }
            });
        }

        holder.itemView.setOnFocusChangeListener((v, hasFocus) -> {
            holder.itemView.setSelected(hasFocus);
        });

        if (holder instanceof ViewHolder) {
            ((ViewHolder) holder).setItemClick(this);
        }
    }

    @Override
    public int getItemCount() {
        return listData == null ? 0 : mFooterView == null ? listData.size() : listData.size() + 1;
    }

    public View inflate(@LayoutRes int layoutId, ViewGroup parent, boolean attachToRoot) {
        return layoutInflater.inflate(layoutId, parent, attachToRoot);
    }

    public List<T> getListData() {
        return listData;
    }

    public void setListData(List<T> listData) {
        this.listData.clear();
        this.listData.addAll(listData);
    }

    /**
     * 建立convertView
     *
     * @param layoutInflater
     * @param parent
     * @param viewType
     * @return
     */
    public abstract View buildConvertView(LayoutInflater layoutInflater, ViewGroup parent, int viewType);

    /**
     * 建立视图Holder
     *
     * @param convertView
     * @param viewType
     * @return
     */
    public abstract H buildHolder(View convertView, int viewType);

    /**
     * 绑定数据
     *
     * @param holder
     * @param item
     * @param position
     */
    public abstract void bindViewDatas(H holder, T item, int position);


    public void setFooterView(View view){
        mFooterView = view;
    }


    static class FooterViewHolder extends RecyclerView.ViewHolder {

        public FooterViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}