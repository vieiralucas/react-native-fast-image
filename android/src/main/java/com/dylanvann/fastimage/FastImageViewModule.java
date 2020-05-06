package com.dylanvann.fastimage;

import android.app.Activity;
import android.graphics.drawable.Drawable;

import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.views.imagehelper.ImageSource;

import java.util.concurrent.atomic.AtomicInteger;

class FastImageViewModule extends ReactContextBaseJavaModule {

    private static final String REACT_CLASS = "FastImageView";

    FastImageViewModule(ReactApplicationContext reactContext) {
        super(reactContext);
    }

    @Override
    public String getName() {
        return REACT_CLASS;
    }

    @ReactMethod
    public void preload(final ReadableArray sources, Promise promise) {
        final Activity activity = getCurrentActivity();
        if (activity == null) return;

        final PreloadRequestListener requestListener = new PreloadRequestListener(promise, sources.size());

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < sources.size(); i++) {
                    final ReadableMap source = sources.getMap(i);
                    final FastImageSource imageSource = FastImageViewConverter.getImageSource(activity, source);

                    Glide
                            .with(activity.getApplicationContext())
                            // This will make this work for remote and local images. e.g.
                            //    - file:///
                            //    - content://
                            //    - res:/
                            //    - android.resource://
                            //    - data:image/png;base64
                            .load(
                                    imageSource.isBase64Resource() ? imageSource.getSource() :
                                    imageSource.isResource() ? imageSource.getUri() : imageSource.getGlideUrl()
                            )
                            .apply(FastImageViewConverter.getOptions(activity, imageSource, source))

                            .listener(requestListener)
                            .preload();
                }
            }
        });
    }

    private class PreloadRequestListener implements RequestListener<Drawable> {
        private AtomicInteger resourcesLoaded = new AtomicInteger(0);
        private int resourcesAmount;
        Promise promise;

        public PreloadRequestListener(Promise promise, int resourcesAmount) {
            this.resourcesAmount = resourcesAmount;
            this.promise = promise;
        }

        @Override
        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
            return false;
        }

        @Override
        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
            int loadedAmount = resourcesLoaded.incrementAndGet();
            if (loadedAmount == resourcesAmount) {
                promise.resolve(true);
            }
        }
    }
}
