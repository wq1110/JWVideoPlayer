package com.media.jwvideoplayer.mvx.mvvm;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * 解决Livedata数据倒灌问她
 *Created by Joyce.wang on 2023/6/28.
 */

public class CustomerLiveData<T> extends MutableLiveData<T> {
    //目的：使得在observe被调用的时候，能够保证 if (observer.mLastVersion >= mVersion) （livedata源码里面的）成立

    @Override
    public void observe(@NonNull LifecycleOwner owner, @NonNull Observer<? super T> observer) {
        super.observe(owner, observer);
        try {
            hook(observer);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void observeSticky(@NonNull LifecycleOwner owner, @NonNull Observer<T> observer) {
        super.observe(owner, observer);
    }

    /**
     * 要修改observer.mLastVersion的值那么思考：（逆向思维）
     * mLastVersion-》observer-》iterator.next().getValue()-》mObservers
     * 反射使用的时候，正好相反
     * <p>
     * mObservers-》函数（iterator.next().getValue()）-》observer-》mLastVersion
     * 通过hook，将observer.mLastVersion = mVersion
     *
     * @param observer
     * @throws Exception
     */
    private void hook(Observer<? super T> observer) throws Exception {
        Class<LiveData> liveDataClass = LiveData.class;
        Field fieldmObservers = liveDataClass.getDeclaredField("mObservers");
        fieldmObservers.setAccessible(true);
        Object mObservers = fieldmObservers.get(this);
        Class<?> mObserversClass = mObservers.getClass();

        Method methodget = mObserversClass.getDeclaredMethod("get", Object.class);
        methodget.setAccessible(true);
        Object entry = methodget.invoke(mObservers, observer);
        Object observerWrapper = ((Map.Entry) entry).getValue();
        Class<?> mObserver = observerWrapper.getClass().getSuperclass();//observer

        Field mLastVersion = mObserver.getDeclaredField("mLastVersion");
        mLastVersion.setAccessible(true);
        Field mVersion = liveDataClass.getDeclaredField("mVersion");
        mVersion.setAccessible(true);
        Object mVersionObject = mVersion.get(this);
        mLastVersion.set(observerWrapper, mVersionObject);
    }
}