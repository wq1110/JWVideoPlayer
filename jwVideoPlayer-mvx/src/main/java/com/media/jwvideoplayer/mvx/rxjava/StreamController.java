package com.media.jwvideoplayer.mvx.rxjava;

import java.util.ArrayList;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;

/**
* 流
* 职责:
*/
public class StreamController<T> {
    private Observable<T> observable;
    private boolean isDispose = false;
    private final ArrayList<ObservableEmitter<T>> emitters = new ArrayList<>();

    public StreamController() {
        this.observable = Observable.create(new ObservableOnSubscribe<T>() {
            @Override
            public void subscribe(@NonNull ObservableEmitter<T> emitter) throws Exception {
                emitters.add(emitter);
                emitter.setDisposable(new Dispose(emitter));
            }
        });
    }

    public void push(T object) {
        for (ObservableEmitter<T> emitter : emitters) {
            emitter.onNext(object);
        }
    }

    public void error(Throwable throwable) {
        for (ObservableEmitter<T> emitter : emitters) {
            emitter.onError(throwable);
        }
    }

    public Observable<T> stream(){
        return observable;
    }

    public boolean hasBeenObserve() {
        return emitters.size() >0;
    }

    class Dispose implements Disposable {
        private ObservableEmitter<T> emitter;

        public Dispose(ObservableEmitter<T> emitter) {
            this.emitter = emitter;
        }

        @Override
        public void dispose() {
            emitters.remove(emitter);
            emitter = null;
            isDispose = true;
        }

        @Override
        public boolean isDisposed() {
            return isDispose;
        }
    }
}
