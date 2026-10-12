package com.hazeroot.scriptrunner;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.NotificationManagerCompat;
import kotlin.Metadata;
import kotlin.jvm.internal.Intrinsics;

/* compiled from: Perms.kt */

/* loaded from: classes3.dex */
public final class Perms {
    public static final Perms INSTANCE = new Perms();
    private static final String NOTIFICATION = "android.permission.POST_NOTIFICATIONS";

    private Perms() {
    }

    public final boolean notificationGranted(Context ctx) {
        Intrinsics.checkNotNullParameter(ctx, "ctx");
        return Build.VERSION.SDK_INT < 33 || NotificationManagerCompat.from(ctx).areNotificationsEnabled();
    }

    public final String getNOTIFICATION() {
        return NOTIFICATION;
    }

    public final void openAppSettings(Activity activity) {
        Intrinsics.checkNotNullParameter(activity, "activity");
        activity.startActivity(new Intent("android.settings.APPLICATION_DETAILS_SETTINGS", Uri.fromParts("package", activity.getPackageName(), null)));
    }
}
