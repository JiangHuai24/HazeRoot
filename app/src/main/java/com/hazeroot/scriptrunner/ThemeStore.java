package com.hazeroot.scriptrunner;

import android.content.Context;
import androidx.appcompat.app.AppCompatDelegate;
import kotlin.jvm.internal.Intrinsics;

/* compiled from: ThemeStore.kt */

/**
 * 日夜模式存储。
 *
 * ★ 修复「App 不跟随系统切换日夜」：
 *   原实现 mode() 默认返回 -1，而 -1 不是合法的 AppCompatDelegate 夜间模式常量，
 *   apply() 把它直接丢给 setDefaultNightMode(-1) → 系统配置永远不生效，
 *   App 一直停在首次安装时那一套配色。
 *   现在默认值改为 MODE_NIGHT_FOLLOW_SYSTEM，用户没手动选过主题时完全跟随系统。
 */
public final class ThemeStore {
    public static final ThemeStore INSTANCE = new ThemeStore();
    private static final String KEY = "night_mode";
    private static final String PREF = "ui_prefs";

    private ThemeStore() {
    }

    public final int mode(Context ctx) {
        Intrinsics.checkNotNullParameter(ctx, "ctx");
        int saved = ctx.getSharedPreferences(PREF, 0).getInt(KEY, -100);
        // 兼容旧版本写入的非法值（-1 或其它非 AppCompat 常量）
        switch (saved) {
            case AppCompatDelegate.MODE_NIGHT_NO:
            case AppCompatDelegate.MODE_NIGHT_YES:
            case AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM:
            case AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY:
                return saved;
            default:
                return AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
        }
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
