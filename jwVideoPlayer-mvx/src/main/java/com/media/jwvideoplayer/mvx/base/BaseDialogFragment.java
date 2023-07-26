package com.media.jwvideoplayer.mvx.base;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.media.jwvideoplayer.lib.callback.ICallBack;
import com.media.jwvideoplayer.mvx.layout.LayoutInflaterEx;

import java.util.LinkedList;
import java.util.Queue;


public abstract class BaseDialogFragment extends DialogFragment implements IBaseComponent {
    private boolean isViewCreated = false;
    private Queue<Runnable> pendingDo = new LinkedList<>();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(null);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        ViewStub stub = new ViewStub(getContext());
        LayoutInflaterEx.inflate(getContext(), getLayoutID(), new ICallBack<View>() {
            @Override
            public void call(View view) {
                stub.setView(view);
                onViewCreated();
            }
        }, new ICallBack<Throwable>() {
            @Override
            public void call(Throwable throwable) {
                throwable.printStackTrace();
                if (getActivity() != null)
                    Toast.makeText(getActivity(), throwable.getMessage(), Toast.LENGTH_LONG).show();
            }
        }, allowInflateAsync());
        return stub;
    }

    public void onViewCreated() {
        isViewCreated = true;
        configUI(getView());
        Runnable r;
        while ((r = pendingDo.poll()) != null && getActivity() != null) {
            getActivity().runOnUiThread(r);
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
    public void onDestroyView() {
        super.onDestroyView();
        pendingDo.clear();
    }

    @Override
    public boolean allowInflateAsync() {
        return false;
    }

    @Override
    public boolean viewReady() {
        return isViewCreated;
    }
}
