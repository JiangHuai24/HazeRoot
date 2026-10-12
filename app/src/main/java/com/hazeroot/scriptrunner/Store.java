package com.hazeroot.scriptrunner;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.constraintlayout.widget.ConstraintLayout;
import java.util.ArrayList;
import java.util.List;
import kotlin.Metadata;
import kotlin.collections.CollectionsKt;
import kotlin.jvm.internal.Intrinsics;

/* compiled from: Store.kt */

/* loaded from: classes3.dex */
public final class Store {
    public static final Store INSTANCE = new Store();
    private static final String K_PRESETS = "presets";
    private static final String K_SCRIPTS = "scripts";
    private static final String PREF = "runner_store";

    private Store() {
    }

    private final SharedPreferences prefs(Context ctx) {
        return ctx.getSharedPreferences(PREF, 0);
    }

    public final List<ScriptItem> loadScripts(Context ctx) {
        List<ScriptItem> scriptsFromJson;
        Intrinsics.checkNotNullParameter(ctx, "ctx");
        String it = prefs(ctx).getString(K_SCRIPTS, null);
        return (it == null || (scriptsFromJson = Json.INSTANCE.scriptsFromJson(it)) == null) ? new ArrayList() : scriptsFromJson;
    }

    public final boolean hasScripts(Context ctx) {
        Intrinsics.checkNotNullParameter(ctx, "ctx");
        return prefs(ctx).contains(K_SCRIPTS);
    }

    public final void saveScripts(Context ctx, List<ScriptItem> list) {
        Intrinsics.checkNotNullParameter(ctx, "ctx");
        Intrinsics.checkNotNullParameter(list, "list");
        prefs(ctx).edit().putString(K_SCRIPTS, Json.INSTANCE.scriptsToJson(list)).apply();
    }

    public final List<Preset> loadPresets(Context ctx) {
        List<Preset> presetsFromJson;
        Intrinsics.checkNotNullParameter(ctx, "ctx");
        String it = prefs(ctx).getString(K_PRESETS, null);
        return (it == null || (presetsFromJson = Json.INSTANCE.presetsFromJson(it)) == null) ? new ArrayList() : presetsFromJson;
    }

    public final void savePresets(Context ctx, List<Preset> list) {
        Intrinsics.checkNotNullParameter(ctx, "ctx");
        Intrinsics.checkNotNullParameter(list, "list");
        prefs(ctx).edit().putString(K_PRESETS, Json.INSTANCE.presetsToJson(list)).apply();
    }

    /** 默认方案：保持为空。不预置任何示例，避免携带第三方脚本名与特定目录。 */
    public final List<Preset> defaultPresets() {
        return new ArrayList<>();
    }

    /** 默认队列：保持为空。首次启动由空状态引导用户点「添加脚本」自建。 */
    public final List<ScriptItem> defaultScripts() {
        return new ArrayList<>();
    }
}
