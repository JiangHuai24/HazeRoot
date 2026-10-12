package com.hazeroot.scriptrunner;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import androidx.constraintlayout.widget.ConstraintLayout;
import com.hazeroot.scriptrunner.Engine;
import java.util.Iterator;
import kotlin.Metadata;
import kotlin.jvm.internal.DefaultConstructorMarker;
import kotlin.jvm.internal.Intrinsics;

/* compiled from: RunnerService.kt */

/* loaded from: classes3.dex */
public final class RunnerService extends Service {
    public static final String ACTION_STOP = "com.hazeroot.scriptrunner.STOP";
    private static final String CHANNEL = "runner";

    /* renamed from: Companion, reason: from kotlin metadata */
    public static final Companion INSTANCE = new Companion(null);
    private static final int NOTI_ID = 1;
    private final Engine.Listener listener = new Engine.Listener() { // from class: com.hazeroot.scriptrunner.RunnerService$listener$1
        @Override // com.hazeroot.scriptrunner.Engine.Listener
        public void onEvent(String type, String payload) {
            Object element$iv;
            Intrinsics.checkNotNullParameter(type, "type");
            Intrinsics.checkNotNullParameter(payload, "payload");
            if (Intrinsics.areEqual(type, "done")) {
                RunnerService.this.updateNotification(payload);
                RunnerService.this.stopSelf();
                return;
            }
            if (Intrinsics.areEqual(type, "state")) {
                Iterable $this$firstOrNull$iv = Engine.INSTANCE.getScripts();
                Iterator it = $this$firstOrNull$iv.iterator();
                while (true) {
                    if (it.hasNext()) {
                        element$iv = it.next();
                        ScriptItem it2 = (ScriptItem) element$iv;
                        if (it2.getRunningNow()) {
                            break;
                        }
                    } else {
                        element$iv = null;
                        break;
                    }
                }
                ScriptItem cur = (ScriptItem) element$iv;
                if (cur != null) {
                    RunnerService.this.updateNotification("执行中: " + cur.getName());
                }
            }
        }
    };

    @Override // android.app.Service
    public void onCreate() {
        super.onCreate();
        Engine.INSTANCE.addListener(this.listener);
    }

    @Override // android.app.Service
    public void onDestroy() {
        Engine.INSTANCE.removeListener(this.listener);
        super.onDestroy();
    }

    @Override // android.app.Service
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (Intrinsics.areEqual(intent != null ? intent.getAction() : null, ACTION_STOP)) {
            Engine.INSTANCE.abort();
            stopSelf();
            return 2;
        }
        startForeground(1, buildNotification("执行中…"));
        return 2;
    }

    private final Notification buildNotification(String text) {
        Object systemService = getSystemService("notification");
        Intrinsics.checkNotNull(systemService, "null cannot be cast to non-null type android.app.NotificationManager");
        NotificationManager nm = (NotificationManager) systemService;
        nm.createNotificationChannel(new NotificationChannel(CHANNEL, "脚本执行", 2));
        Notification build = new Notification.Builder(this, CHANNEL).setSmallIcon(android.R.drawable.stat_sys_download).setContentTitle(getString(R.string.app_name)).setContentText(text).setOngoing(true).build();
        Intrinsics.checkNotNullExpressionValue(build, "build(...)");
        return build;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final void updateNotification(String text) {
        Object systemService = getSystemService("notification");
        Intrinsics.checkNotNull(systemService, "null cannot be cast to non-null type android.app.NotificationManager");
        NotificationManager nm = (NotificationManager) systemService;
        nm.notify(1, buildNotification(text));
    }

    @Override // android.app.Service
    public IBinder onBind(Intent intent) {
        return null;
    }

    /* compiled from: RunnerService.kt */
    
    public static final class Companion {
        public /* synthetic */ Companion(DefaultConstructorMarker defaultConstructorMarker) {
            this();
        }

        private Companion() {
        }

        public final void start(Context c) {
            Intrinsics.checkNotNullParameter(c, "c");
            c.startForegroundService(new Intent(c, (Class<?>) RunnerService.class));
        }

        public final void stop(Context c) {
            Intrinsics.checkNotNullParameter(c, "c");
            c.startService(new Intent(c, (Class<?>) RunnerService.class).setAction(RunnerService.ACTION_STOP));
        }
    }
}
