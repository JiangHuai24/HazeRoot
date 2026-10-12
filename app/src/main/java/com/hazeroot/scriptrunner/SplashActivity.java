package com.hazeroot.scriptrunner;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import kotlin.Metadata;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.internal.Intrinsics;

/* compiled from: SplashActivity.kt */

/* loaded from: classes3.dex */
public final class SplashActivity extends AppCompatActivity {
    private boolean launched;

    @Override // androidx.fragment.app.FragmentActivity, androidx.activity.ComponentActivity, androidx.core.app.ComponentActivity, android.app.Activity
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final ConvergeSplashView view = new ConvergeSplashView(this);
        setContentView(view);
        view.setOnLaunch(new Function0<Unit>() { // from class: com.hazeroot.scriptrunner.SplashActivity$onCreate$1

            @Override // kotlin.jvm.functions.Function0
            public /* bridge */ /* synthetic */ Unit invoke() {
                invoke2();
                return Unit.INSTANCE;
            }

            /* renamed from: invoke, reason: avoid collision after fix types in other method */
            public final void invoke2() {
                boolean z;
                z = SplashActivity.this.launched;
                if (!z) {
                    SplashActivity.this.launched = true;
                    SplashActivity.this.startActivity(new Intent(SplashActivity.this, (Class<?>) MainActivity.class));
                    SplashActivity.this.overridePendingTransition(android.R.anim.fade_in, 0);
                }
            }
        });
        view.setOnComplete(new Function0<Unit>() { // from class: com.hazeroot.scriptrunner.SplashActivity$onCreate$2

            @Override // kotlin.jvm.functions.Function0
            public /* bridge */ /* synthetic */ Unit invoke() {
                invoke2();
                return Unit.INSTANCE;
            }

            /* renamed from: invoke, reason: avoid collision after fix types in other method */
            public final void invoke2() {
                boolean z;
                z = SplashActivity.this.launched;
                if (!z) {
                    SplashActivity.this.launched = true;
                    SplashActivity.this.finishToMain();
                } else {
                    SplashActivity.this.finish();
                    SplashActivity.this.overridePendingTransition(0, 0);
                }
            }
        });
        view.setOnClickListener(new View.OnClickListener() { // from class: com.hazeroot.scriptrunner.SplashActivity$$ExternalSyntheticLambda0
            @Override // android.view.View.OnClickListener
            public final void onClick(View view2) {
                SplashActivity.onCreate$lambda$0(view, view2);
            }
        });
        view.post(new Runnable() { // from class: com.hazeroot.scriptrunner.SplashActivity$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                SplashActivity.onCreate$lambda$1(view);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void onCreate$lambda$0(ConvergeSplashView view, View it) {
        Intrinsics.checkNotNullParameter(view, "$view");
        view.skip();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void onCreate$lambda$1(ConvergeSplashView view) {
        Intrinsics.checkNotNullParameter(view, "$view");
        view.start();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final void finishToMain() {
        startActivity(new Intent(this, (Class<?>) MainActivity.class));
        finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
}
