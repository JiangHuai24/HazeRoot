package com.hazeroot.scriptrunner;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import androidx.constraintlayout.widget.ConstraintLayout;
import kotlin.Metadata;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.internal.Intrinsics;

/* compiled from: SwitchFx.kt */

/* loaded from: classes3.dex */
public final class SwitchFx {
    public static final SwitchFx INSTANCE = new SwitchFx();
    private static final long IN_MS = 260;
    private static final long OUT_MS = 320;
    private static volatile int pendingColor;

    private SwitchFx() {
    }

    public final void run(Activity activity, int fromColor, final Function0<Unit> action) {
        Intrinsics.checkNotNullParameter(activity, "activity");
        Intrinsics.checkNotNullParameter(action, "action");
        pendingColor = fromColor;
        View decorView = activity.getWindow().getDecorView();
        Intrinsics.checkNotNull(decorView, "null cannot be cast to non-null type android.view.ViewGroup");
        ViewGroup decor = (ViewGroup) decorView;
        View scrim = new View(activity);
        scrim.setBackgroundColor(fromColor);
        scrim.setAlpha(0.0f);
        scrim.setClickable(true);
        decor.addView(scrim, -1, -1);
        scrim.animate().alpha(1.0f).setDuration(IN_MS).withEndAction(new Runnable() { // from class: com.hazeroot.scriptrunner.SwitchFx$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                SwitchFx.run$lambda$1(action);
            }
        }).start();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void run$lambda$1(Function0 action) {
        Intrinsics.checkNotNullParameter(action, "$action");
        action.invoke();
    }

    public final void consume(Activity activity) {
        Intrinsics.checkNotNullParameter(activity, "activity");
        if (pendingColor == 0) {
            return;
        }
        int color = pendingColor;
        pendingColor = 0;
        View decorView = activity.getWindow().getDecorView();
        Intrinsics.checkNotNull(decorView, "null cannot be cast to non-null type android.view.ViewGroup");
        final ViewGroup decor = (ViewGroup) decorView;
        final View scrim = new View(activity);
        scrim.setBackgroundColor(color);
        scrim.setAlpha(1.0f);
        scrim.setClickable(true);
        decor.addView(scrim, -1, -1);
        scrim.animate().alpha(0.0f).setDuration(OUT_MS).withEndAction(new Runnable() { // from class: com.hazeroot.scriptrunner.SwitchFx$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                SwitchFx.consume$lambda$3(decor, scrim);
            }
        }).start();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void consume$lambda$3(ViewGroup decor, View scrim) {
        Intrinsics.checkNotNullParameter(decor, "$decor");
        Intrinsics.checkNotNullParameter(scrim, "$scrim");
        decor.removeView(scrim);
    }

    public final boolean hasPending() {
        return pendingColor != 0;
    }
}
