package com.hazeroot.scriptrunner;

import android.graphics.drawable.Drawable;
import androidx.constraintlayout.widget.ConstraintLayout;
import kotlin.Metadata;
import kotlin.jvm.internal.DefaultConstructorMarker;
import kotlin.jvm.internal.Intrinsics;

/* compiled from: PresetsFragment.kt */

/* renamed from: com.hazeroot.scriptrunner.PresetsFragment$appPickerDialog$Row, reason: from toString */
/* loaded from: classes3.dex */
public final /* data */ class Row {
    private final Drawable icon;
    private final boolean isNone;
    private final String label;
    private final String pkg;

    public static /* synthetic */ Row copy$default(Row row, String str, String str2, Drawable drawable, boolean z, int i, Object obj) {
        if ((i & 1) != 0) {
            str = row.label;
        }
        if ((i & 2) != 0) {
            str2 = row.pkg;
        }
        if ((i & 4) != 0) {
            drawable = row.icon;
        }
        if ((i & 8) != 0) {
            z = row.isNone;
        }
        return row.copy(str, str2, drawable, z);
    }





    public final Row copy(String label, String pkg, Drawable icon, boolean isNone) {
        Intrinsics.checkNotNullParameter(label, "label");
        Intrinsics.checkNotNullParameter(pkg, "pkg");
        return new Row(label, pkg, icon, isNone);
    }

    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Row)) {
            return false;
        }
        Row row = (Row) other;
        return Intrinsics.areEqual(this.label, row.label) && Intrinsics.areEqual(this.pkg, row.pkg) && Intrinsics.areEqual(this.icon, row.icon) && this.isNone == row.isNone;
    }

    public int hashCode() {
        return (((((this.label.hashCode() * 31) + this.pkg.hashCode()) * 31) + (this.icon == null ? 0 : this.icon.hashCode())) * 31) + Boolean.hashCode(this.isNone);
    }

    public String toString() {
        return "Row(label=" + this.label + ", pkg=" + this.pkg + ", icon=" + this.icon + ", isNone=" + this.isNone + ")";
    }

    public Row(String label, String pkg, Drawable icon, boolean isNone) {
        Intrinsics.checkNotNullParameter(label, "label");
        Intrinsics.checkNotNullParameter(pkg, "pkg");
        this.label = label;
        this.pkg = pkg;
        this.icon = icon;
        this.isNone = isNone;
    }

    public /* synthetic */ Row(String str, String str2, Drawable drawable, boolean z, int i, DefaultConstructorMarker defaultConstructorMarker) {
        this(str, str2, drawable, (i & 8) != 0 ? false : z);
    }

    public final Drawable getIcon() {
        return this.icon;
    }

    public final String getLabel() {
        return this.label;
    }

    public final String getPkg() {
        return this.pkg;
    }

    public final boolean isNone() {
        return this.isNone;
    }
}
