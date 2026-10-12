package com.hazeroot.scriptrunner;

import androidx.constraintlayout.widget.ConstraintLayout;
import java.util.ArrayList;
import java.util.List;
import kotlin.Metadata;
import kotlin.jvm.internal.DefaultConstructorMarker;
import kotlin.jvm.internal.Intrinsics;

/* compiled from: Models.kt */

/* loaded from: classes3.dex */
public final /* data */ class ScriptItem {
    private transient boolean done;
    private transient Integer exitCode;
    private String name;
    private String path;
    private List<RecordedInput> recordedInputs;
    private transient boolean runningNow;

    public ScriptItem() {
        this(null, null, null, false, false, null, 63, null);
    }

    public static /* synthetic */ ScriptItem copy$default(ScriptItem scriptItem, String str, String str2, List list, boolean z, boolean z2, Integer num, int i, Object obj) {
        if ((i & 1) != 0) {
            str = scriptItem.name;
        }
        if ((i & 2) != 0) {
            str2 = scriptItem.path;
        }
        String str3 = str2;
        if ((i & 4) != 0) {
            list = scriptItem.recordedInputs;
        }
        List list2 = list;
        if ((i & 8) != 0) {
            z = scriptItem.done;
        }
        boolean z3 = z;
        if ((i & 16) != 0) {
            z2 = scriptItem.runningNow;
        }
        boolean z4 = z2;
        if ((i & 32) != 0) {
            num = scriptItem.exitCode;
        }
        return scriptItem.copy(str, str3, list2, z3, z4, num);
    }



    public final List<RecordedInput> component3() {
        return this.recordedInputs;
    }




    public final ScriptItem copy(String name, String path, List<RecordedInput> recordedInputs, boolean done, boolean runningNow, Integer exitCode) {
        Intrinsics.checkNotNullParameter(name, "name");
        Intrinsics.checkNotNullParameter(path, "path");
        Intrinsics.checkNotNullParameter(recordedInputs, "recordedInputs");
        return new ScriptItem(name, path, recordedInputs, done, runningNow, exitCode);
    }

    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ScriptItem)) {
            return false;
        }
        ScriptItem scriptItem = (ScriptItem) other;
        return Intrinsics.areEqual(this.name, scriptItem.name) && Intrinsics.areEqual(this.path, scriptItem.path) && Intrinsics.areEqual(this.recordedInputs, scriptItem.recordedInputs) && this.done == scriptItem.done && this.runningNow == scriptItem.runningNow && Intrinsics.areEqual(this.exitCode, scriptItem.exitCode);
    }

    public int hashCode() {
        return (((((((((this.name.hashCode() * 31) + this.path.hashCode()) * 31) + this.recordedInputs.hashCode()) * 31) + Boolean.hashCode(this.done)) * 31) + Boolean.hashCode(this.runningNow)) * 31) + (this.exitCode == null ? 0 : this.exitCode.hashCode());
    }

    public String toString() {
        return "ScriptItem(name=" + this.name + ", path=" + this.path + ", recordedInputs=" + this.recordedInputs + ", done=" + this.done + ", runningNow=" + this.runningNow + ", exitCode=" + this.exitCode + ")";
    }

    public ScriptItem(String name, String path, List<RecordedInput> recordedInputs, boolean done, boolean runningNow, Integer exitCode) {
        Intrinsics.checkNotNullParameter(name, "name");
        Intrinsics.checkNotNullParameter(path, "path");
        Intrinsics.checkNotNullParameter(recordedInputs, "recordedInputs");
        this.name = name;
        this.path = path;
        this.recordedInputs = recordedInputs;
        this.done = done;
        this.runningNow = runningNow;
        this.exitCode = exitCode;
    }

    public /* synthetic */ ScriptItem(String str, String str2, List list, boolean z, boolean z2, Integer num, int i, DefaultConstructorMarker defaultConstructorMarker) {
        this((i & 1) != 0 ? "" : str, (i & 2) == 0 ? str2 : "", (i & 4) != 0 ? new ArrayList() : list, (i & 8) != 0 ? false : z, (i & 16) != 0 ? false : z2, (i & 32) != 0 ? null : num);
    }

    public final String getName() {
        return this.name;
    }

    public final void setName(String str) {
        Intrinsics.checkNotNullParameter(str, "<set-?>");
        this.name = str;
    }

    public final String getPath() {
        return this.path;
    }

    public final void setPath(String str) {
        Intrinsics.checkNotNullParameter(str, "<set-?>");
        this.path = str;
    }

    public final List<RecordedInput> getRecordedInputs() {
        return this.recordedInputs;
    }

    public final void setRecordedInputs(List<RecordedInput> list) {
        Intrinsics.checkNotNullParameter(list, "<set-?>");
        this.recordedInputs = list;
    }

    public final boolean getDone() {
        return this.done;
    }

    public final void setDone(boolean z) {
        this.done = z;
    }

    public final boolean getRunningNow() {
        return this.runningNow;
    }

    public final void setRunningNow(boolean z) {
        this.runningNow = z;
    }

    public final Integer getExitCode() {
        return this.exitCode;
    }

    public final void setExitCode(Integer num) {
        this.exitCode = num;
    }
}
