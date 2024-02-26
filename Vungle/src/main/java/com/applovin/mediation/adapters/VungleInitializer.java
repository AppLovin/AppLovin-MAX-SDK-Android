package com.applovin.mediation.adapters;

import android.content.Context;
import androidx.annotation.NonNull;
import com.vungle.ads.InitializationListener;
import com.vungle.ads.VungleAds;
import com.vungle.ads.VungleError;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class VungleInitializer implements InitializationListener {

  private static final VungleInitializer instance = new VungleInitializer();
  private final AtomicBoolean isInitializing = new AtomicBoolean(false);
  private final ArrayList<VungleInitializationListener> initListeners;

  @NonNull
  public static VungleInitializer getInstance() {
    return instance;
  }

  private VungleInitializer() {
    initListeners = new ArrayList<>();
    VungleAds.setIntegrationName( VungleAds.WrapperFramework.max,
        com.applovin.mediation.adapters.vungle.BuildConfig.VERSION_NAME );
  }

  public void initialize(
      final @NonNull String appId,
      final @NonNull Context context,
      @NonNull VungleInitializationListener listener) {

    if (VungleAds.isInitialized()) {
      listener.onInitializeSuccess();
      return;
    }

    if (isInitializing.getAndSet(true)) {
      initListeners.add(listener);
      return;
    }

    VungleAds.init(context, appId, VungleInitializer.this);
    initListeners.add(listener);
  }

  @Override
  public void onSuccess() {
    for (VungleInitializationListener listener : initListeners) {
      listener.onInitializeSuccess();
    }
    initListeners.clear();
    isInitializing.set(false);
  }

  @Override
  public void onError(@NonNull final VungleError vungleError) {
    for (VungleInitializationListener listener : initListeners) {
      listener.onInitializeError(vungleError);
    }
    initListeners.clear();
    isInitializing.set(false);
  }

  public interface VungleInitializationListener {

    void onInitializeSuccess();

    void onInitializeError(VungleError error);
  }
}
