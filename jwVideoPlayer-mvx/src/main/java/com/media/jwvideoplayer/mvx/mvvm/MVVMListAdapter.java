package com.media.jwvideoplayer.mvx.mvvm;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import androidx.annotation.LayoutRes;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ViewDataBinding;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Joyce.wang on 2023/6/28.
 */
public class MVVMListAdapter<T> extends BaseAdapter {
    private int variableId;
    private int layoutId;
    private Context context;

    private List<T> mList;

    public MVVMListAdapter(Context context, @LayoutRes int layoutId, int variableId) {
        this.context = context;
        this.layoutId = layoutId;
        this.variableId = variableId;
        mList = new ArrayList<>();
    }

    public void updateData(List<T> list){
        mList = list;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return mList.size();
    }

    @Override
    public T getItem(int position) {
        return mList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewDataBinding binding = null;
        if (convertView == null) {
            binding = DataBindingUtil.inflate(LayoutInflater.from(context), layoutId, parent, false);
        } else {
            binding = DataBindingUtil.getBinding(convertView);
        }
        binding.setVariable(variableId, mList.get(position));
        return binding.getRoot();
    }
}
