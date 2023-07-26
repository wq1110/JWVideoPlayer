package com.media.jwvideoplayer.mvx.mvvm;

import android.view.View;

import androidx.databinding.DataBindingUtil;
import androidx.databinding.ViewDataBinding;

/**
 * Created by Joyce.wang on 2023/6/28.
 */
public class BindingHolder<VDB extends ViewDataBinding> extends ViewHolder {

    VDB mBinding;

    public BindingHolder(View convertView) {
        super(convertView);
        mBinding = DataBindingUtil.bind(convertView);

    }

    public VDB getBinding(){
        return mBinding;
    }
}
