package com.hazeroot.scriptrunner;

import androidx.constraintlayout.widget.ConstraintLayout;
import java.util.List;
import kotlin.Metadata;
import kotlin.collections.CollectionsKt;
import kotlin.jvm.internal.DefaultConstructorMarker;
import kotlin.jvm.internal.Intrinsics;

/* compiled from: Models.kt */

/* loaded from: classes3.dex */
public final /* data */ class Preset {
    private String name;
    private List<ScriptItem> scripts;
    private String targetApp;

    public Preset() {
        this(null, null, null, 7, null);
    }

    /* JADX WARN: Multi-variable type inference failed */
    public static /* synthetic */ Preset copy$default(Preset preset, String str, List list, String str2, int i, Object obj) {
        if ((i & 1) != 0) {
            str = preset.name;
        }
        if ((i & 2) != 0) {
            list = preset.scripts;
        }
        if ((i & 4) != 0) {
            str2 = preset.targetApp;
        }
        return preset.copy(str, list, str2);
    }


    public final List<ScriptItem> component2() {
        return this.scripts;
    }


    public final Preset copy(String name, List<ScriptItem> scripts, String targetApp) {
        Intrinsics.checkNotNullParameter(name, "name");
        Intrinsics.checkNotNullParameter(scripts, "scripts");
        Intrinsics.checkNotNullParameter(targetApp, "targetApp");
        return new Preset(name, scripts, targetApp);
    }

    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Preset)) {
            return false;
        }
        Preset preset = (Preset) other;
        return Intrinsics.areEqual(this.name, preset.name) && Intrinsics.areEqual(this.scripts, preset.scripts) && Intrinsics.areEqual(this.targetApp, preset.targetApp);
    }

    public int hashCode() {
        return (((this.name.hashCode() * 31) + this.scripts.hashCode()) * 31) + this.targetApp.hashCode();
    }

    public String toString() {
        return "Preset(name=" + this.name + ", scripts=" + this.scripts + ", targetApp=" + this.targetApp + ")";
    }

    public Preset(String name, List<ScriptItem> scripts, String targetApp) {
        Intrinsics.checkNotNullParameter(name, "name");
        Intrinsics.checkNotNullParameter(scripts, "scripts");
        Intrinsics.checkNotNullParameter(targetApp, "targetApp");
        this.name = name;
        this.scripts = scripts;
        this.targetApp = targetApp;
    }

    public /* synthetic */ Preset(String str, List list, String str2, int i, DefaultConstructorMarker defaultConstructorMarker) {
        this((i & 1) != 0 ? "" : str, (i & 2) != 0 ? CollectionsKt.emptyList() : list, (i & 4) != 0 ? "" : str2);
    }

    public final String getName() {
        return this.name;
    }

    public final void setName(String str) {
        Intrinsics.checkNotNullParameter(str, "<set-?>");
        this.name = str;
    }

    public final List<ScriptItem> getScripts() {
        return this.scripts;
    }

    public final void setScripts(List<ScriptItem> list) {
        Intrinsics.checkNotNullParameter(list, "<set-?>");
        this.scripts = list;
    }

    public final String getTargetApp() {
        return this.targetApp;
    }

    public final void setTargetApp(String str) {
        Intrinsics.checkNotNullParameter(str, "<set-?>");
        this.targetApp = str;
    }
}
