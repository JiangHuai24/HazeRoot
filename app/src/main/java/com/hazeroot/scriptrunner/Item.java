package com.hazeroot.scriptrunner;

import androidx.constraintlayout.widget.ConstraintLayout;
import kotlin.Metadata;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.internal.DefaultConstructorMarker;
import kotlin.jvm.internal.Intrinsics;

/* compiled from: PresetsFragment.kt */

/* renamed from: com.hazeroot.scriptrunner.PresetsFragment$showPresetSheet$Item, reason: from toString */
/* loaded from: classes3.dex */
public final /* data */ class Item {
    private final Function0<Unit> action;
    private final boolean danger;
    private final String title;

    /* JADX WARN: Multi-variable type inference failed */
    public static /* synthetic */ Item copy$default(Item item, String str, boolean z, Function0 function0, int i, Object obj) {
        if ((i & 1) != 0) {
            str = item.title;
        }
        if ((i & 2) != 0) {
            z = item.danger;
        }
        if ((i & 4) != 0) {
            function0 = item.action;
        }
        return item.copy(str, z, function0);
    }



    public final Function0<Unit> component3() {
        return this.action;
    }

    public final Item copy(String title, boolean danger, Function0<Unit> action) {
        Intrinsics.checkNotNullParameter(title, "title");
        Intrinsics.checkNotNullParameter(action, "action");
        return new Item(title, danger, action);
    }

    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Item)) {
            return false;
        }
        Item item = (Item) other;
        return Intrinsics.areEqual(this.title, item.title) && this.danger == item.danger && Intrinsics.areEqual(this.action, item.action);
    }

    public int hashCode() {
        return (((this.title.hashCode() * 31) + Boolean.hashCode(this.danger)) * 31) + this.action.hashCode();
    }

    public String toString() {
        return "Item(title=" + this.title + ", danger=" + this.danger + ", action=" + this.action + ")";
    }

    public Item(String title, boolean danger, Function0<Unit> action) {
        Intrinsics.checkNotNullParameter(title, "title");
        Intrinsics.checkNotNullParameter(action, "action");
        this.title = title;
        this.danger = danger;
        this.action = action;
    }

    public /* synthetic */ Item(String str, boolean z, Function0 function0, int i, DefaultConstructorMarker defaultConstructorMarker) {
        this(str, (i & 2) != 0 ? false : z, function0);
    }

    public final Function0<Unit> getAction() {
        return this.action;
    }

    public final boolean getDanger() {
        return this.danger;
    }

    public final String getTitle() {
        return this.title;
    }
}
