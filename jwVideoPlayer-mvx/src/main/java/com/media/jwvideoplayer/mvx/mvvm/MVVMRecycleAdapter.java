package com.media.jwvideoplayer.mvx.mvvm;

import android.content.Context;

import androidx.annotation.LayoutRes;
import androidx.databinding.ViewDataBinding;

import java.util.List;

/**
 * Created by Joyce.wang on 2023/6/28.
 */
public class MVVMRecycleAdapter<T, VDB extends ViewDataBinding> extends BaseRecyclerAdapter<T, BindingHolder<VDB>> {

    private int variableId;

    public MVVMRecycleAdapter(Context context, int layoutId, int variableId) {
        super(context, layoutId);
        this.variableId = variableId;
    }

    public MVVMRecycleAdapter(Context context, List<T> listData, @LayoutRes int layoutId) {
        super(context, listData, layoutId);
    }

    @Override
    public void bindViewDatas(BindingHolder<VDB> holder, T item, int position) {
        holder.mBinding.setVariable(variableId, item);
        holder.mBinding.executePendingBindings();
    }

    public T getItem(int position) {
        if (position < getItemCount()) {
            return getListData().get(position);
        }

        return null;
    }

    private long lastRemoveTime = 0;

    public void removeItem(T data) {
        //这里如果不限制，而进行快速点击，由于上次删除后调用notifyItemRemoved动画没有执行完成，会出现数组越界等错误
        // 这是google的bug。需做限制
        if (System.currentTimeMillis() - lastRemoveTime < 400) {
            return;
        }

        lastRemoveTime = System.currentTimeMillis();

        int position = -1;
        if (getListData() != null && getListData().contains(data)) {
            position = getListData().indexOf(data);
            getListData().remove(data);
        }

        if (position != -1) {
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, getItemCount());
        }
    }

    public void removeItem(int position) {
        //这里如果不限制，而进行快速点击，由于上次删除后调用notifyItemRemoved动画没有执行完成，会出现数组越界等错误
        // 这是google的bug。需做限制
        if (System.currentTimeMillis() - lastRemoveTime < 400) {
            return;
        }

        lastRemoveTime = System.currentTimeMillis();

        if (getListData() != null && getListData().size() > position) {
            getListData().remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, getItemCount());
        }
    }

    public void refreshData(List<T> list) {
        if (list != null) {
            setListData(list);
        } else {
            getListData().clear();
        }
        notifyDataSetChanged();
    }
}
