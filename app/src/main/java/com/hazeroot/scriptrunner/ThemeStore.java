package com.hazeroot.scriptrunner;

import android.content.Context;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.constraintlayout.widget.ConstraintLayout;
import kotlin.Metadata;
import kotlin.jvm.internal.Intrinsics;

/* compiled from: ThemeStore.kt */

/* loaded from: classes3.dex */
public final class ThemeStore {
    public static final ThemeStore INSTANCE = new ThemeStore();
    private static final String KEY = "night_mode";
    private static final String PREF = "ui_prefs";

    private ThemeStore() {
    }

    public final int mode(Context ctx) {
        Intrinsics.checkNotNullParameter(ctx, "ctx");
        return ctx.getSharedPreferences(PREF, 0).getInt(KEY, -1);
    }

    public final void set(Context ctx, int mode) {
        Intrinsics.checkNotNullParameter(ctx, "ctx");
        ctx.getSharedPreferences(PREF, 0).edit().putInt(KEY, mode).apply();
        AppCompatDelegate.setDefaultNightMode(mode);
    }

    public final boolean isNight(Context ctx) {
        Intrinsics.checkNotNullParameter(ctx, "ctx");
        return (ctx.getResources().getConfiguration().uiMode & 48) == 32;
    }

    public final void apply(Context ctx) {
        Intrinsics.checkNotNullParameter(ctx, "ctx");
        AppCompatDelegate.setDefaultNightMode(mode(ctx));
    }
}
