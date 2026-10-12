package com.hazeroot.scriptrunner;

import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import kotlin.Metadata;
import kotlin.jvm.internal.Intrinsics;

/* compiled from: ManualActivity.kt */

/* loaded from: classes3.dex */
public final class ManualActivity extends AppCompatActivity {
    private WebView web;

    /* JADX INFO: Access modifiers changed from: private */
    public final boolean isLightTheme() {
        return (getResources().getConfiguration().uiMode & 48) != 32;
    }

    @Override // androidx.fragment.app.FragmentActivity, androidx.activity.ComponentActivity, androidx.core.app.ComponentActivity, android.app.Activity
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manual);
        View findViewById = findViewById(R.id.manualWeb);
        Intrinsics.checkNotNullExpressionValue(findViewById, "findViewById(...)");
        this.web = (WebView) findViewById;
        WebView webView = this.web;
        WebView webView2 = null;
        if (webView == null) {
            Intrinsics.throwUninitializedPropertyAccessException("web");
            webView = null;
        }
        webView.getSettings().setJavaScriptEnabled(true);
        WebView webView3 = this.web;
        if (webView3 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("web");
            webView3 = null;
        }
        webView3.setWebViewClient(new WebViewClient() { // from class: com.hazeroot.scriptrunner.ManualActivity$onCreate$1
            @Override // android.webkit.WebViewClient
            public void onPageFinished(WebView view, String url) {
                boolean isLightTheme;
                Intrinsics.checkNotNullParameter(view, "view");
                Intrinsics.checkNotNullParameter(url, "url");
                isLightTheme = ManualActivity.this.isLightTheme();
                view.evaluateJavascript("document.body.classList." + (isLightTheme ? "add" : "remove") + "('light')", null);
            }
        });
        if (savedInstanceState == null) {
            WebView webView4 = this.web;
            if (webView4 == null) {
                Intrinsics.throwUninitializedPropertyAccessException("web");
            } else {
                webView2 = webView4;
            }
            webView2.loadUrl("file:///android_asset/manual.html");
            return;
        }
        WebView webView5 = this.web;
        if (webView5 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("web");
        } else {
            webView2 = webView5;
        }
        webView2.restoreState(savedInstanceState);
    }

    @Override // androidx.activity.ComponentActivity, android.app.Activity
    public void onBackPressed() {
        WebView webView = this.web;
        WebView webView2 = null;
        if (webView == null) {
            Intrinsics.throwUninitializedPropertyAccessException("web");
            webView = null;
        }
        if (webView.canGoBack()) {
            WebView webView3 = this.web;
            if (webView3 == null) {
                Intrinsics.throwUninitializedPropertyAccessException("web");
            } else {
                webView2 = webView3;
            }
            webView2.goBack();
            return;
        }
        super.onBackPressed();
    }

    @Override // androidx.activity.ComponentActivity, androidx.core.app.ComponentActivity, android.app.Activity
    protected void onSaveInstanceState(Bundle outState) {
        Intrinsics.checkNotNullParameter(outState, "outState");
        super.onSaveInstanceState(outState);
        WebView webView = this.web;
        if (webView == null) {
            Intrinsics.throwUninitializedPropertyAccessException("web");
            webView = null;
        }
        webView.saveState(outState);
    }
}
