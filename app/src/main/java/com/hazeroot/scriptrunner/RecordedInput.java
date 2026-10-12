package com.hazeroot.scriptrunner;

import androidx.constraintlayout.widget.ConstraintLayout;
import kotlin.Metadata;
import kotlin.jvm.internal.DefaultConstructorMarker;
import kotlin.jvm.internal.Intrinsics;

/* compiled from: Models.kt */

/* loaded from: classes3.dex */
public final /* data */ class RecordedInput {
    private String prompt;
    private String value;

    /* JADX WARN: Multi-variable type inference failed */
    public RecordedInput() {
        this(null, null, 3, null);
    }

    public static /* synthetic */ RecordedInput copy$default(RecordedInput recordedInput, String str, String str2, int i, Object obj) {
        if ((i & 1) != 0) {
            str = recordedInput.prompt;
        }
        if ((i & 2) != 0) {
            str2 = recordedInput.value;
        }
        return recordedInput.copy(str, str2);
    }



    public final RecordedInput copy(String prompt, String value) {
        Intrinsics.checkNotNullParameter(prompt, "prompt");
        Intrinsics.checkNotNullParameter(value, "value");
        return new RecordedInput(prompt, value);
    }

    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof RecordedInput)) {
            return false;
        }
        RecordedInput recordedInput = (RecordedInput) other;
        return Intrinsics.areEqual(this.prompt, recordedInput.prompt) && Intrinsics.areEqual(this.value, recordedInput.value);
    }

    public int hashCode() {
        return (this.prompt.hashCode() * 31) + this.value.hashCode();
    }

    public String toString() {
        return "RecordedInput(prompt=" + this.prompt + ", value=" + this.value + ")";
    }

    public RecordedInput(String prompt, String value) {
        Intrinsics.checkNotNullParameter(prompt, "prompt");
        Intrinsics.checkNotNullParameter(value, "value");
        this.prompt = prompt;
        this.value = value;
    }

    public /* synthetic */ RecordedInput(String str, String str2, int i, DefaultConstructorMarker defaultConstructorMarker) {
        this((i & 1) != 0 ? "" : str, (i & 2) != 0 ? "" : str2);
    }

    public final String getPrompt() {
        return this.prompt;
    }

    public final String getValue() {
        return this.value;
    }

    public final void setPrompt(String str) {
        Intrinsics.checkNotNullParameter(str, "<set-?>");
        this.prompt = str;
    }

    public final void setValue(String str) {
        Intrinsics.checkNotNullParameter(str, "<set-?>");
        this.value = str;
    }
}
