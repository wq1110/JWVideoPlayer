package com.media.jwvideoplayer.mvx.mvvm;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ViewDataBinding;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.media.jwvideoplayer.mvx.base.BaseFragment;
import com.media.jwvideoplayer.mvx.viewmodel.BaseViewModel;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * Created by Joyce.wang on 2023/6/28.
 */
public abstract class MVVMBaseFragment<VM extends BaseViewModel, T extends ViewDataBinding> extends BaseFragment {
    public T mBinding;
    public VM mViewModel;

    private ViewModelProvider mActivityProvider;

    private boolean isLazyLoad = false;//是否已经懒加载
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mViewModel = getViewModel();
    }

    @Override
    public void configUI(View view) {
        if (mBinding == null) {
            mBinding = DataBindingUtil.bind(view);
        }

        initViewModel();
        mBinding.setVariable(getBindingVariable(), mViewModel);
        mBinding.setLifecycleOwner(this);
        mBinding.executePendingBindings();

        initView();
        if (allowLazy()) {
            if (isVisibleToUser() && !isLazyLoad()) {
                isLazyLoad = true;
                initData();
            }
        } else {
            initData();
        }

        initObserver();
    }

    protected abstract void initData();

    protected void initView() {
    }

    public abstract int getBindingVariable();

    public T getViewDataBinding() {
        return mBinding;
    }

    public VM getViewModel() {
        return null;
    }

    ;

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mViewModel != null) {
            getLifecycle().removeObserver(mViewModel);
            mViewModel = null;
        }

        if (mBinding != null) {
            mBinding.unbind();
        }
    }

    /**
     * 初始化 {@link #mViewModel}
     */
    private void initViewModel() {
        mViewModel = getViewModel();
        if (mViewModel == null) {
            mViewModel = obtainViewModel(getVMClass());
        }

        if (mViewModel != null) {
            getLifecycle().addObserver(mViewModel);
        }
    }

    /**
     * 通过 {@link ViewModelProvider.Factory}获得 ViewModel
     *
     * @param modelClass
     * @param <T>
     * @return
     */
    public <T extends ViewModel> T obtainViewModel(@NonNull Class<T> modelClass) {
        if (mActivityProvider == null) {
            mActivityProvider = new ViewModelProvider(this);
        }
        return mActivityProvider.get(modelClass);
    }

    private Class<VM> getVMClass() {
        Class cls = getClass();
        Class<VM> vmClass = null;
        while (vmClass == null && cls != null) {
            vmClass = getVMClass(cls);
            cls = cls.getSuperclass();
        }
        if (vmClass == null) {
            vmClass = (Class<VM>) BaseViewModel.class;
        }
        return vmClass;
    }

    private Class getVMClass(Class cls) {
        Type type = cls.getGenericSuperclass();
        if (type instanceof ParameterizedType) {
            Type[] types = ((ParameterizedType) type).getActualTypeArguments();
            for (Type t : types) {
                if (t instanceof Class) {
                    Class vmClass = (Class) t;
                    if (BaseViewModel.class.isAssignableFrom(vmClass)) {
                        return vmClass;
                    }
                } else if (t instanceof ParameterizedType) {
                    Type rawType = ((ParameterizedType) t).getRawType();
                    if (rawType instanceof Class) {
                        Class vmClass = (Class) rawType;
                        if (BaseViewModel.class.isAssignableFrom(vmClass)) {
                            return vmClass;
                        }
                    }
                }
            }
        }

        return null;
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        if (getView() != null && allowLazy() && isVisibleToUser && !isLazyLoad()) {
            isLazyLoad = true;
            initData();
        }
        super.setUserVisibleHint(isVisibleToUser);
    }

    public boolean isVisibleToUser() {
        return isDisplaying();
    }


    protected boolean isLazyLoad() {
        return isLazyLoad;
    }

    protected boolean allowLazy() {
        return false;
    }

    protected void initObserver() {
        mViewModel.isLoading().observe(this, isLoading -> {
            if (isLoading) {
                showLoading();
            } else {
                hideLoading();
            }
        });

        mViewModel.isFinishActivity().observe(this, isFinish -> {
            if (isFinish) {
                if (getActivity() != null) {
                    getActivity().finish();
                }
            }
        });
    }
}
