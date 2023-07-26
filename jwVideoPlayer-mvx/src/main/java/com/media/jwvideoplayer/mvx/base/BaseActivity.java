package com.media.jwvideoplayer.mvx.base;

import static android.view.ViewGroup.FOCUS_AFTER_DESCENDANTS;

import android.content.res.Resources;
import android.os.Bundle;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.media.jwvideoplayer.lib.callback.ICallBack;
import com.media.jwvideoplayer.lib.utils.ToastUtils;
import com.media.jwvideoplayer.mvx.appstat.AppManager;
import com.media.jwvideoplayer.mvx.layout.LayoutInflaterEx;
import com.media.jwvideoplayer.mvx.progressview.ProgressManager;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import me.jessyan.autosize.AutoSizeCompat;

public abstract class BaseActivity extends FragmentActivity implements IBaseComponent {
    private boolean isViewCreated = false;
    private long mBrowseTime;
    private final Queue<Runnable> pendingDo = new LinkedList<>();

    private final Map<String, Object> mKeyedTags = new HashMap<>();

    public Object getTag() {
        return mKeyedTags.get(this.getClass().getName());
    }

    public Map<String, Object> getTags() {
        return mKeyedTags;
    }

    public Object getTag(String key) {
        return mKeyedTags.get(key);
    }

    public void setTag(Object tag) {
        mKeyedTags.put(this.getClass().getName(), tag);
    }

    public void setTag(String key, Object tag) {
        mKeyedTags.put(key, tag);
    }

    public Long pageIdentifierIndex() {
        return null;
    }

    public String pageIdentifierStr() {
        return getClass().getSimpleName();
    }



    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(null);   //抛弃 savedInstanceState, 不考虑 系统层 关于页面状态还原的工作
        onCreateDo(savedInstanceState);
        getResources();
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(new Bundle());  //不考虑 系统层 关于页面状态还原的工作
    }

    protected void onCreateDo(Bundle savedInstanceState) {
        LayoutInflaterEx.inflate(this, getLayoutID(), new ICallBack<View>() {
            @Override
            public void call(View view) {
                setContentView(view);
                setDataBinding(view);
                ((ViewGroup) findViewById(android.R.id.content)).setDescendantFocusability(FOCUS_AFTER_DESCENDANTS);
                onViewCreated(view);
            }
        }, new ICallBack<Throwable>() {
            @Override
            public void call(Throwable throwable) {
                throwable.printStackTrace();
                ToastUtils.showToast(BaseActivity.this, throwable.getMessage(), 0);
            }
        }, allowInflateAsync());
    }

    //绑定databinding
    public boolean setDataBinding(View view) {
        return false;
    }

    protected void onViewCreated(View view) {
        configUI(view);
        isViewCreated = true;
        Runnable r;
        while ((r = pendingDo.poll()) != null) {
            runOnUiThread(r);
        }
    }


    @Override
    public final void doAfterViewReady(Runnable runnable) {
        if (viewReady()) {
            runnable.run();
        } else {
            pendingDo.offer(runnable);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mBrowseTime = System.currentTimeMillis();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        pendingDo.clear();
    }

    @Override
    public Resources getResources() {
        Resources superRes = super.getResources();
        autoSizeFix(superRes);
        return superRes;
    }

    //在MiTV android11中对ResourceManager魔改过, getResource流程会触发重新设置Resource的DisplayMetric等, 不能只改一次, 要每次都去改一下
    protected void autoSizeFix(Resources superRes) {
        if (Looper.getMainLooper() == Looper.myLooper())
            AutoSizeCompat.autoConvertDensityOfGlobal(superRes);
    }


    public long getBrowseDuration() {
        if (mBrowseTime <= 0) {
            return 0;
        }
        return (System.currentTimeMillis() - mBrowseTime) / 1000;
    }

    public boolean allowInflateAsync() {
        return false;
    }

    @Override
    public boolean viewReady() {
        return isViewCreated;
    }

    public void showErrorTypeToast(String msg) {
        ToastUtils.showToast(this, msg, 0);
    }

    public void showTipTypeToast(String msg) {
        ToastUtils.showToast(this, msg, 0);
    }

    public void showToast(String msg) {
        ToastUtils.showToast(BaseActivity.this, msg, 0);
    }

    public void showLoading() {
        ProgressManager.showProgressDialog(this);
    }

    public void hideLoading() {
        ProgressManager.closeProgressDialog();
    }

    public void exitApp() {
        AppManager.INSTANCE.getApp().exitApp();
    }
}
