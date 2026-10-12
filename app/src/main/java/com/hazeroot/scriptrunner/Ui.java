package com.hazeroot.scriptrunner;

import android.content.Context;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import kotlin.Metadata;
import kotlin.jvm.internal.Intrinsics;

/* compiled from: Ui.kt */

/* loaded from: classes3.dex */
public final class Ui {
    public static final Ui INSTANCE = new Ui();

    private Ui() {
    }

    public final void toast(Context ctx, String msg) {
        Intrinsics.checkNotNullParameter(ctx, "ctx");
        Intrinsics.checkNotNullParameter(msg, "msg");
        LinearLayout box = new LinearLayout(ctx);
        box.setOrientation(0);
        box.setGravity(16);
        box.setBackgroundResource(R.drawable.bg_toast);
        int pad = (int) (14 * ctx.getResources().getDisplayMetrics().density);
        float f = 10;
        box.setPadding(pad, (int) (ctx.getResources().getDisplayMetrics().density * f), pad, (int) (f * ctx.getResources().getDisplayMetrics().density));
        ImageView $this$toast_u24lambda_u241 = new ImageView(ctx);
        float f2 = 18;
        $this$toast_u24lambda_u241.setLayoutParams(new LinearLayout.LayoutParams((int) (ctx.getResources().getDisplayMetrics().density * f2), (int) (f2 * ctx.getResources().getDisplayMetrics().density)));
        $this$toast_u24lambda_u241.setBackgroundResource(R.drawable.bg_brand);
        float f3 = 2;
        $this$toast_u24lambda_u241.setPadding((int) (ctx.getResources().getDisplayMetrics().density * f3), (int) (ctx.getResources().getDisplayMetrics().density * f3), (int) (ctx.getResources().getDisplayMetrics().density * f3), (int) (f3 * ctx.getResources().getDisplayMetrics().density));
        $this$toast_u24lambda_u241.setImageResource(R.drawable.ic_brand_h);
        box.addView($this$toast_u24lambda_u241);
        TextView $this$toast_u24lambda_u243 = new TextView(ctx);
        $this$toast_u24lambda_u243.setText(msg);
        $this$toast_u24lambda_u243.setTextSize(13.0f);
        $this$toast_u24lambda_u243.setTextColor(ContextCompat.getColor(ctx, R.color.label));
        $this$toast_u24lambda_u243.setTypeface($this$toast_u24lambda_u243.getTypeface(), 1);
        $this$toast_u24lambda_u243.setMaxLines(2);
        LinearLayout.LayoutParams $this$toast_u24lambda_u243_u24lambda_u242 = new LinearLayout.LayoutParams(-2, -2);
        $this$toast_u24lambda_u243_u24lambda_u242.setMarginStart((int) (8 * ctx.getResources().getDisplayMetrics().density));
        $this$toast_u24lambda_u243.setLayoutParams($this$toast_u24lambda_u243_u24lambda_u242);
        box.addView($this$toast_u24lambda_u243);
        Toast t = new Toast(ctx);
        t.setView(box);
        t.setDuration(0);
        t.setGravity(81, 0, (int) (76 * ctx.getResources().getDisplayMetrics().density));
        t.show();
    }
}
