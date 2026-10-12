package com.hazeroot.scriptrunner;

import android.app.Application;
import androidx.constraintlayout.widget.ConstraintLayout;
import kotlin.Metadata;

/* compiled from: App.kt */

/* loaded from: classes3.dex */
public final class App extends Application {
    @Override // android.app.Application
    public void onCreate() {
        super.onCreate();
        ThemeStore.INSTANCE.apply(this);
    }
}
