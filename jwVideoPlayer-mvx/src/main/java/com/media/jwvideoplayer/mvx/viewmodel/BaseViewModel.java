package com.media.jwvideoplayer.mvx.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;

import com.uber.autodispose.android.lifecycle.AndroidLifecycleScopeProvider;

import io.reactivex.disposables.CompositeDisposable;

/**
 * Created by zhangyaoa on 2020/4/7.
 */
public class BaseViewModel extends AndroidViewModel implements IViewModelLife {
    private CompositeDisposable mCompositeDisposable;

    private AndroidLifecycleScopeProvider mScopeProvider;

    private final MutableLiveData<Boolean> mIsLoading = new MutableLiveData<>();

    private final MutableLiveData<Boolean> mClosePage = new MutableLiveData<>();

    private final MutableLiveData<String> mToastMsg = new MutableLiveData<>();

    //    @Inject
    public BaseViewModel(@NonNull Application application) {
        super(application);
        this.mCompositeDisposable = new CompositeDisposable();
    }

    @Override
    protected void onCleared() {
        mCompositeDisposable.dispose();
        mScopeProvider = null;
        super.onCleared();
    }

    public MutableLiveData<Boolean> isLoading() {
        return mIsLoading;
    }

    public void setLoading(Boolean loading) {
        mIsLoading.postValue(loading);
    }

    public CompositeDisposable getCompositeDisposable() {
        return mCompositeDisposable;
    }

    public AndroidLifecycleScopeProvider getScopeProvider() {
        return mScopeProvider;
    }

    public MutableLiveData<String> showToast() {
        return mToastMsg;
    }

    public void setToast(String msg) {
        mToastMsg.postValue(msg);
    }

    public MutableLiveData<Boolean> isFinishActivity() {
        return mClosePage;
    }

    public void finishActivity() {
        mClosePage.postValue(true);
    }

    @Override
    public void onCreate() {

    }

    @Override
    public void onStart() {

    }

    @Override
    public void onResume() {

    }

    @Override
    public void onPause() {

    }

    @Override
    public void onStop() {

    }

    @Override
    public void onDestroy() {

    }

    @Override
    public void onAny(LifecycleOwner owner, Lifecycle.Event event) {
        if (mScopeProvider == null) {
            mScopeProvider = AndroidLifecycleScopeProvider.from(owner);
        }
    }

    public boolean onKeyBackIntercept() {
        return false;
    }

    public boolean onKeyEnterIntercept() {
        return false;
    }

    public boolean onKeyMenuIntercept() {
        return false;
    }

    public boolean onKeyLeftIntercept() {
        return false;
    }

    public boolean onKeyRightIntercept() {
        return false;
    }

    public boolean onKeyUpIntercept() {
        return false;
    }

    public boolean onKeyDownIntercept() {
        return false;
    }
}
