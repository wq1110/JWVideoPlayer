package com.media.jwvideoplayer.mvx.mvvm;

import android.view.KeyEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ViewDataBinding;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.media.jwvideoplayer.mvx.base.BaseActivity;
import com.media.jwvideoplayer.mvx.viewmodel.BaseViewModel;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * Created by Joyce.wang on 2023/6/28.
 */
public abstract class MVVMBaseActivity<VM extends BaseViewModel, T extends ViewDataBinding> extends BaseActivity {

    private ViewModelProvider mActivityProvider;

    protected T mBinding;
    protected VM mViewModel;

    @Override
    public void configUI(View view) {
        initViewModel();
        initData();
        initObserver();
    }

    @Override
    public boolean setDataBinding(View view) {
        if (mBinding == null) {
            mBinding = DataBindingUtil.bind(view);
            mBinding.setLifecycleOwner(this);
        }

        initViewModel();
        mBinding.setVariable(getBindingVariable(), mViewModel);
        mBinding.executePendingBindings();
        return true;
    }

    /**
     * 初始化 {@link #mViewModel}
     */
    private void initViewModel() {
        mViewModel = createViewModel();
        if (mViewModel == null) {
            mViewModel = obtainViewModel(getVMClass());
        }

        if (mViewModel != null) {
            getLifecycle().addObserver(mViewModel);
        }
        registerLoadingEvent();
    }

    private void registerLoadingEvent() {

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

    public VM getViewModel() {
        return mViewModel;
    }

    public T getViewDataBinding() {
        return mBinding;
    }

    protected abstract int getBindingVariable();

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mViewModel != null) {
            getLifecycle().removeObserver(mViewModel);
            mViewModel = null;
        }

        if (mBinding != null) {
            mBinding.unbind();
        }
    }

    public VM createViewModel() {
        return null;
    }

    protected abstract void initData();

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (this.mViewModel == null) {
            return super.onKeyDown(keyCode, event);
        }

        boolean viewModelIntercept = false;
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            viewModelIntercept = this.mViewModel.onKeyBackIntercept();
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
            viewModelIntercept = this.mViewModel.onKeyEnterIntercept();
        } else if (keyCode == KeyEvent.KEYCODE_MENU) {
            viewModelIntercept = this.mViewModel.onKeyMenuIntercept();
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
            viewModelIntercept = this.mViewModel.onKeyLeftIntercept();
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
            viewModelIntercept = this.mViewModel.onKeyRightIntercept();
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
            viewModelIntercept = this.mViewModel.onKeyUpIntercept();
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
            viewModelIntercept = this.mViewModel.onKeyDownIntercept();
        }

        return viewModelIntercept ? true : super.onKeyDown(keyCode, event);
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
                finish();
            }
        });
    }
}
