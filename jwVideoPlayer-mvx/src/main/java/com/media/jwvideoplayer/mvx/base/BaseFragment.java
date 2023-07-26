package com.media.jwvideoplayer.mvx.base;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.ActionMode;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SearchEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;

import com.media.jwvideoplayer.lib.callback.ICallBack;
import com.media.jwvideoplayer.mvx.layout.LayoutInflaterEx;
import com.media.jwvideoplayer.mvx.progressview.ProgressManager;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;

public abstract class BaseFragment extends Fragment implements IBaseComponent {
    private boolean isViewCreated = false;
    private final Queue<Runnable> pendingDo = new LinkedList<>();
    private boolean isDisplaying = true;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(null);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        ViewStub stub = new ViewStub(getContext());
        LayoutInflaterEx.inflate(getContext(), getLayoutID(), new ICallBack<View>() {
            @Override
            public void call(View view) {
                stub.setView(view);
                // onViewCreated(view);
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

    public final boolean isDisplaying() {
        return isDisplaying;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        isViewCreated = true;
        configUI(((ViewStub) view).getView());
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
    public void setUserVisibleHint(boolean isVisibleToUser) {
        isDisplaying = isVisibleToUser;
        super.setUserVisibleHint(isVisibleToUser);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        isViewCreated = false;
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

    public void showLoading() {
        ProgressManager.showProgressDialog(getActivity());
    }

    public void hideLoading() {
        ProgressManager.closeProgressDialog();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof Activity) {
            CallBackHandler.attachCallBack((Activity) context, this);
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        CallBackHandler.detachCallBack(this);
    }

    boolean backConsumed = false;


    boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                backConsumed = handleBackPress();
            }
            return backConsumed;
        }
        return false;
    }

    protected boolean handleBackPress() {
        return false;
    }

    static class CallBackHandler implements Window.Callback {
        static LinkedHashMap<Activity, Set<BaseFragment>> atyToFrgs = new LinkedHashMap<>();

        static void attachCallBack(Activity activity, BaseFragment frg) {
            if (atyToFrgs.get(activity) == null) {
                CallBackHandler handler = new CallBackHandler();
                handler.attach(activity);
                atyToFrgs.put(activity, handler.frgs);
            }
            Set<BaseFragment> fragments = Objects.requireNonNull(atyToFrgs.get(activity));
            fragments.add(frg);
            atyToFrgs.put(activity, fragments);
        }

        static void detachCallBack(BaseFragment frg) {
            Activity activity = null;
            for (Map.Entry<Activity, Set<BaseFragment>> entry : atyToFrgs.entrySet()) {
                if (entry.getValue().remove(frg)) {
                    if (entry.getValue().isEmpty()) {
                        activity = entry.getKey();
                    }
                    break;
                }
            }
            if (activity != null) atyToFrgs.remove(activity);
        }

        Window.Callback originalCallback;
        LinkedHashSet<BaseFragment> frgs = new LinkedHashSet<>();

        public void attach(Activity activity) {
            originalCallback = activity.getWindow().getCallback();
            activity.getWindow().setCallback(this);
        }

        public boolean dispatchKeyEvent(KeyEvent event) {
            try {
                boolean ref = false;
                List<BaseFragment> tmpArr = new ArrayList<>(frgs);
                for (int i = tmpArr.size() - 1; i >= 0 ; i --) {
                    BaseFragment frg = tmpArr.get(i);
                    ref = frg.dispatchKeyEvent(event);
                    if (ref) break;
                }
                return ref || this.originalCallback.dispatchKeyEvent(event);
            } catch (Exception e) {
                Log.w(getClass().getSimpleName(), "", e);
                return false;
            }
        }

        public boolean dispatchKeyShortcutEvent(KeyEvent event) {
            try {
                return this.originalCallback.dispatchKeyShortcutEvent(event);
            } catch (Exception e) {
                Log.w(getClass().getSimpleName(), "", e);
                return false;
            }
        }

        public boolean dispatchTouchEvent(MotionEvent event) {
            try {
                return this.originalCallback.dispatchTouchEvent(event);
            } catch (Exception e) {
                Log.w(getClass().getSimpleName(), "", e);
                return false;
            }
        }

        public boolean dispatchTrackballEvent(MotionEvent event) {
            try {
                return this.originalCallback.dispatchTrackballEvent(event);
            } catch (Exception e) {
                Log.w(getClass().getSimpleName(), "", e);
                return false;
            }
        }

        public boolean dispatchGenericMotionEvent(MotionEvent event) {
            try {
                return this.originalCallback.dispatchGenericMotionEvent(event);
            } catch (Exception e) {
                Log.w(getClass().getSimpleName(), "", e);
                return false;
            }
        }

        public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
            try {
                return this.originalCallback.dispatchPopulateAccessibilityEvent(event);
            } catch (Exception e) {
                Log.w(getClass().getSimpleName(), "", e);
                return false;
            }
        }

        @Nullable
        public View onCreatePanelView(int featureId) {
            try {
                return this.originalCallback.onCreatePanelView(featureId);
            } catch (Exception e) {
                Log.w(getClass().getSimpleName(), "", e);
                return null;
            }
        }

        public boolean onCreatePanelMenu(int featureId, @NonNull Menu menu) {
            try {
                return this.originalCallback.onCreatePanelMenu(featureId, menu);
            } catch (Exception e) {
                Log.w(getClass().getSimpleName(), "", e);
                return false;
            }
        }

        public boolean onPreparePanel(int featureId, @Nullable View view, @NonNull Menu menu) {
            try {
                return this.originalCallback.onPreparePanel(featureId, view, menu);
            } catch (Exception e) {
                Log.w(getClass().getSimpleName(), "", e);
                return false;
            }
        }

        public boolean onMenuOpened(int featureId, @NonNull Menu menu) {
            try {
                return this.originalCallback.onMenuOpened(featureId, menu);
            } catch (Exception e) {
                Log.w(getClass().getSimpleName(), "", e);
                return false;
            }
        }

        public boolean onMenuItemSelected(int featureId, @NonNull MenuItem item) {
            try {
                return this.originalCallback.onMenuItemSelected(featureId, item);
            } catch (Exception e) {
                Log.w(getClass().getSimpleName(), "", e);
                return false;
            }
        }

        public void onWindowAttributesChanged(WindowManager.LayoutParams attrs) {
            try {
                if (originalCallback == null) return;
                this.originalCallback.onWindowAttributesChanged(attrs);
            } catch (Exception e) {
                Log.w(getClass().getSimpleName(), "", e);
            }
        }

        public void onContentChanged() {
            try {
                if (originalCallback == null) return;
                this.originalCallback.onContentChanged();
            } catch (Exception e) {
                Log.w(getClass().getSimpleName(), "", e);
            }
        }

        public void onWindowFocusChanged(boolean hasFocus) {
            if (originalCallback == null) return;
            this.originalCallback.onWindowFocusChanged(hasFocus);
        }

        public void onAttachedToWindow() {
            try {
                if (originalCallback == null) return;
                this.originalCallback.onAttachedToWindow();
            } catch (Exception e) {
                Log.w(getClass().getSimpleName(), "", e);
            }
        }

        public void onDetachedFromWindow() {
            if (originalCallback == null) return;
            this.originalCallback.onDetachedFromWindow();
        }

        public void onPanelClosed(int featureId, @NonNull Menu menu) {
            try {
                if (originalCallback == null) return;
                this.originalCallback.onPanelClosed(featureId, menu);
            } catch (Exception e) {
                Log.w(getClass().getSimpleName(), "", e);
            }
        }

        public boolean onSearchRequested() {
            try {
                return this.originalCallback.onSearchRequested();
            } catch (Exception e) {
                Log.w(getClass().getSimpleName(), "", e);
                return false;
            }
        }

        @RequiresApi(
                api = 23
        )
        public boolean onSearchRequested(SearchEvent searchEvent) {
            try {
                return this.originalCallback.onSearchRequested(searchEvent);
            } catch (Exception e) {
                Log.w(getClass().getSimpleName(), "", e);
                return false;
            }
        }

        @Nullable
        public ActionMode onWindowStartingActionMode(ActionMode.Callback callback) {
            try {
                return this.originalCallback.onWindowStartingActionMode(callback);
            } catch (Exception e) {
                Log.w(getClass().getSimpleName(), "", e);
                return null;
            }
        }

        @RequiresApi(api = Build.VERSION_CODES.M)
        @Nullable
        public ActionMode onWindowStartingActionMode(ActionMode.Callback callback, int type) {
            try {
                return this.originalCallback.onWindowStartingActionMode(callback, type);
            } catch (Exception e) {
                Log.w(getClass().getSimpleName(), "", e);
                return null;
            }
        }

        public void onActionModeStarted(ActionMode mode) {
            try {
                this.originalCallback.onActionModeStarted(mode);
            } catch (Exception e) {
                Log.w(getClass().getSimpleName(), "", e);
            }
        }

        public void onActionModeFinished(ActionMode mode) {
            try {
                this.originalCallback.onActionModeFinished(mode);
            } catch (Exception e) {
                Log.w(getClass().getSimpleName(), "", e);
            }
        }
    }
}
