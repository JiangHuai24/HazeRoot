package com.hazeroot.scriptrunner;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ListView;
import androidx.constraintlayout.widget.ConstraintLayout;
import kotlin.Metadata;
import kotlin.jvm.internal.DefaultConstructorMarker;
import kotlin.jvm.internal.Intrinsics;

/* compiled from: FilesListView.kt */

/* loaded from: classes3.dex */
public final class FilesListView extends ListView {
    private boolean gestureLocked;

    /* JADX WARN: 'this' call moved to the top of the method (can break code semantics) */
    public FilesListView(Context context) {
        this(context, null, 0, 6, null);
        Intrinsics.checkNotNullParameter(context, "context");
    }

    /* JADX WARN: 'this' call moved to the top of the method (can break code semantics) */
    public FilesListView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0, 4, null);
        Intrinsics.checkNotNullParameter(context, "context");
    }

    public /* synthetic */ FilesListView(Context context, AttributeSet attributeSet, int i, int i2, DefaultConstructorMarker defaultConstructorMarker) {
        this(context, (i2 & 2) != 0 ? null : attributeSet, (i2 & 4) != 0 ? android.R.attr.listViewStyle : i);
    }

    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
    public FilesListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        Intrinsics.checkNotNullParameter(context, "context");
    }

    public final boolean getGestureLocked() {
        return this.gestureLocked;
    }

    public final void setGestureLocked(boolean z) {
        this.gestureLocked = z;
    }

    @Override // android.widget.AbsListView, android.view.ViewGroup
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (this.gestureLocked) {
            return false;
        }
        return super.onInterceptTouchEvent(ev);
    }
}
