package com.hazeroot.scriptrunner;

import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import com.hazeroot.scriptrunner.SyntaxHighlighter;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import kotlin.Deprecated;
import kotlin.Metadata;
import kotlin.UByte;
import kotlin.collections.ArraysKt;
import kotlin.io.FilesKt;
import kotlin.jvm.internal.Intrinsics;
import kotlin.jvm.internal.Ref;
import kotlin.text.Charsets;
import kotlin.text.StringsKt;

/* compiled from: EditorActivity.kt */

/* loaded from: classes3.dex */
public final class EditorActivity extends AppCompatActivity {
    private View btnSaveIcon;
    private boolean dirty;
    private TextView edLines;
    private TextView edStatus;
    private EditText edText;
    private long fileBytes;
    private boolean loadingText;
    private String path;
    private boolean readOnly;
    private Charset charset = Charsets.UTF_8;
    private String savedText = "";
    private SyntaxHighlighter.Lang hlLang = SyntaxHighlighter.Lang.NONE;
    private String previewNote = "";
    private int lastLines = -1;
    private String statusBase = "";
    private final Handler hlHandler = new Handler(Looper.getMainLooper());
    private final Runnable hlRunnable = new Runnable() { // from class: com.hazeroot.scriptrunner.EditorActivity$$ExternalSyntheticLambda0
        @Override // java.lang.Runnable
        public final void run() {
            EditorActivity.hlRunnable$lambda$0(EditorActivity.this);
        }
    };

    /* JADX INFO: Access modifiers changed from: private */
    public static final void hlRunnable$lambda$0(EditorActivity this$0) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        this$0.refreshStatus();
        this$0.applyHighlight();
    }

    @Override // androidx.fragment.app.FragmentActivity, androidx.activity.ComponentActivity, androidx.core.app.ComponentActivity, android.app.Activity
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);
        String stringExtra = getIntent().getStringExtra("path");
        if (stringExtra == null) {
            EditorActivity $this$onCreate_u24lambda_u241 = this;
            $this$onCreate_u24lambda_u241.finish();
            return;
        }
        this.path = stringExtra;
        SyntaxHighlighter syntaxHighlighter = SyntaxHighlighter.INSTANCE;
        String str = this.path;
        EditText editText = null;
        if (str == null) {
            Intrinsics.throwUninitializedPropertyAccessException("path");
            str = null;
        }
        this.hlLang = syntaxHighlighter.detect(Kt.substringAfterLast(str, '/', (String) null, 2, (Object) null));
        View findViewById = findViewById(R.id.edText);
        Intrinsics.checkNotNullExpressionValue(findViewById, "findViewById(...)");
        this.edText = (EditText) findViewById;
        View findViewById2 = findViewById(R.id.edLines);
        Intrinsics.checkNotNullExpressionValue(findViewById2, "findViewById(...)");
        this.edLines = (TextView) findViewById2;
        View findViewById3 = findViewById(R.id.edStatus);
        Intrinsics.checkNotNullExpressionValue(findViewById3, "findViewById(...)");
        this.edStatus = (TextView) findViewById3;
        View findViewById4 = findViewById(R.id.btnSaveIcon);
        Intrinsics.checkNotNullExpressionValue(findViewById4, "findViewById(...)");
        this.btnSaveIcon = findViewById4;
        TextView textView = (TextView) findViewById(R.id.edTitle);
        String str2 = this.path;
        if (str2 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("path");
            str2 = null;
        }
        textView.setText(Kt.substringAfterLast(str2, '/', (String) null, 2, (Object) null));
        findViewById(R.id.btnClose).setOnClickListener(new View.OnClickListener() { // from class: com.hazeroot.scriptrunner.EditorActivity$$ExternalSyntheticLambda5
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                EditorActivity.onCreate$lambda$2(EditorActivity.this, view);
            }
        });
        View view = this.btnSaveIcon;
        if (view == null) {
            Intrinsics.throwUninitializedPropertyAccessException("btnSaveIcon");
            view = null;
        }
        view.setOnClickListener(new View.OnClickListener() { // from class: com.hazeroot.scriptrunner.EditorActivity$$ExternalSyntheticLambda6
            @Override // android.view.View.OnClickListener
            public final void onClick(View view2) {
                EditorActivity.onCreate$lambda$3(EditorActivity.this, view2);
            }
        });
        load();
        EditText editText2 = this.edText;
        if (editText2 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("edText");
        } else {
            editText = editText2;
        }
        editText.addTextChangedListener(new TextWatcher() { // from class: com.hazeroot.scriptrunner.EditorActivity$onCreate$4
            @Override // android.text.TextWatcher
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {
            }

            @Override // android.text.TextWatcher
            public void onTextChanged(CharSequence s, int a, int b, int c) {
            }

            @Override // android.text.TextWatcher
            public void afterTextChanged(Editable s) {
                boolean z;
                Handler handler;
                Runnable runnable;
                Handler handler2;
                Runnable runnable2;
                z = EditorActivity.this.loadingText;
                if (z) {
                    return;
                }
                EditorActivity.this.dirty = true;
                EditorActivity.this.updateMark();
                EditorActivity.this.updateLines();
                handler = EditorActivity.this.hlHandler;
                runnable = EditorActivity.this.hlRunnable;
                handler.removeCallbacks(runnable);
                handler2 = EditorActivity.this.hlHandler;
                runnable2 = EditorActivity.this.hlRunnable;
                handler2.postDelayed(runnable2, 300L);
            }
        });
        ((ScrollView) findViewById(R.id.edScroll)).getViewTreeObserver().addOnScrollChangedListener(new ViewTreeObserver.OnScrollChangedListener() { // from class: com.hazeroot.scriptrunner.EditorActivity$$ExternalSyntheticLambda7
            @Override // android.view.ViewTreeObserver.OnScrollChangedListener
            public final void onScrollChanged() {
                EditorActivity.onCreate$lambda$4(EditorActivity.this);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void onCreate$lambda$2(EditorActivity this$0, View it) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        if (this$0.dirty) {
            this$0.confirmExit();
        } else {
            this$0.finish();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void onCreate$lambda$3(EditorActivity this$0, View it) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        this$0.save();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void onCreate$lambda$4(EditorActivity this$0) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        TextView textView = this$0.edLines;
        if (textView == null) {
            Intrinsics.throwUninitializedPropertyAccessException("edLines");
            textView = null;
        }
        textView.setTranslationY(-((ScrollView) this$0.findViewById(R.id.edScroll)).getScrollY());
    }

    private final void refreshStatus() {
        String size;
        EditText editText = null;
        if (this.readOnly) {
            size = fmtSize(this.fileBytes);
        } else if (this.dirty) {
            EditText editText2 = this.edText;
            if (editText2 == null) {
                Intrinsics.throwUninitializedPropertyAccessException("edText");
                editText2 = null;
            }
            byte[] sizeProbeBytes = editText2.getText().toString().getBytes(this.charset);
            size = fmtSize(sizeProbeBytes.length);
        } else {
            size = fmtSize(this.fileBytes);
        }
        String lang = this.hlLang == SyntaxHighlighter.Lang.NONE ? "" : " · " + this.hlLang.name();
        String note = this.previewNote.length() == 0 ? "" : " · " + this.previewNote;
        String name = this.charset.name();
        int i = R.string.lines_fmt;
        Object[] objArr = new Object[1];
        EditText editText3 = this.edText;
        if (editText3 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("edText");
        } else {
            editText = editText3;
        }
        objArr[0] = Integer.valueOf(editText.getLineCount());
        this.statusBase = name + " · " + size + " · " + getString(i, objArr) + lang + note;
        updateMark();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final void updateMark() {
        String mark = getString(this.dirty ? R.string.unsaved_mark : R.string.saved_mark);
        Intrinsics.checkNotNull(mark);
        TextView textView = this.edStatus;
        if (textView == null) {
            Intrinsics.throwUninitializedPropertyAccessException("edStatus");
            textView = null;
        }
        textView.setText(this.statusBase + " · " + mark);
    }

    private final void applyHighlight() {
        if (this.hlLang == SyntaxHighlighter.Lang.NONE) {
            return;
        }
        SyntaxHighlighter syntaxHighlighter = SyntaxHighlighter.INSTANCE;
        EditText editText = this.edText;
        if (editText == null) {
            Intrinsics.throwUninitializedPropertyAccessException("edText");
            editText = null;
        }
        Editable editableText = editText.getEditableText();
        Intrinsics.checkNotNullExpressionValue(editableText, "getEditableText(...)");
        syntaxHighlighter.highlight(editableText, this.hlLang, getColor(R.color.accent), getColor(R.color.badge_elf_text), getColor(R.color.label_tertiary), getColor(R.color.prompt_text), getColor(R.color.badge_bin_text));
    }

    private final String fmtSize(long b) {
        if (b >= 1048576) {
            String format = String.format("%.1f MB", Arrays.copyOf(new Object[]{Float.valueOf(b / 1048576.0f)}, 1));
            Intrinsics.checkNotNullExpressionValue(format, "format(...)");
            return format;
        }
        if (b < 1024) {
            return b + " B";
        }
        String format2 = String.format("%.1f KB", Arrays.copyOf(new Object[]{Float.valueOf(b / 1024.0f)}, 1));
        Intrinsics.checkNotNullExpressionValue(format2, "format(...)");
        return format2;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final void updateLines() {
        EditText editText = this.edText;
        TextView textView = null;
        if (editText == null) {
            Intrinsics.throwUninitializedPropertyAccessException("edText");
            editText = null;
        }
        int n = editText.getLineCount();
        if (n == this.lastLines) {
            return;
        }
        this.lastLines = n;
        StringBuilder sb = new StringBuilder();
        int i = 1;
        if (1 <= n) {
            while (true) {
                sb.append(i).append('\n');
                if (i == n) {
                    break;
                } else {
                    i++;
                }
            }
        }
        TextView textView2 = this.edLines;
        if (textView2 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("edLines");
        } else {
            textView = textView2;
        }
        textView.setText(sb.toString());
    }

    private final void load() {
        new Thread(new Runnable() { // from class: com.hazeroot.scriptrunner.EditorActivity$$ExternalSyntheticLambda4
            @Override // java.lang.Runnable
            public final void run() {
                EditorActivity.load$lambda$8(EditorActivity.this);
            }
        }).start();
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r2v23, types: [T, byte[], java.lang.Object] */
    public static final void load$lambda$8(final EditorActivity this$0) {
        byte[] t;
        String str;
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        Engine engine = Engine.INSTANCE;
        Engine engine2 = Engine.INSTANCE;
        String str2 = this$0.path;
        if (str2 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("path");
            str2 = null;
        }
        final byte[] execBytes = engine.execBytes("cat " + engine2.q(str2));
        if (execBytes == null) {
            this$0.runOnUiThread(new Runnable() { // from class: com.hazeroot.scriptrunner.EditorActivity$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    EditorActivity.load$lambda$8$lambda$5(this$0);
                }
            });
            return;
        }
        this$0.fileBytes = execBytes.length;
        final boolean isBinary = this$0.isBinary(execBytes);
        long j = isBinary ? 65536L : 524288L;
        this$0.readOnly = isBinary || ((long) execBytes.length) > j;
        final Ref.ObjectRef objectRef = new Ref.ObjectRef();
        if (execBytes.length > j) {
            byte[] copyOf = Arrays.copyOf(execBytes, (int) j);
            Intrinsics.checkNotNullExpressionValue(copyOf, "copyOf(...)");
            t = copyOf;
        } else {
            t = execBytes;
        }
        objectRef.element = t;
        if (((byte[]) objectRef.element).length < execBytes.length && !isBinary) {
            int length = ((byte[]) objectRef.element).length - 1;
            while (length > 0 && ((byte[]) objectRef.element)[length] != 10) {
                length--;
            }
            if (length > 0) {
                byte[] copyOf2 = Arrays.copyOf((byte[]) objectRef.element, length + 1);
                Intrinsics.checkNotNullExpressionValue(copyOf2, "copyOf(...)");
                objectRef.element = copyOf2;
            }
        }
        if (isBinary) {
            str = this$0.getString(R.string.preview_bin);
            Intrinsics.checkNotNullExpressionValue(str, "getString(...)");
        } else if (this$0.readOnly) {
            str = this$0.getString(R.string.preview_trunc_fmt, new Object[]{this$0.fmtSize(execBytes.length), this$0.fmtSize(((byte[]) objectRef.element).length)});
            Intrinsics.checkNotNullExpressionValue(str, "getString(...)");
        } else {
            str = "";
        }
        this$0.previewNote = str;
        this$0.charset = this$0.detectCharset((byte[]) objectRef.element);
        String charBufferTmp;
        try {
            charBufferTmp = this$0.charset.newDecoder().onMalformedInput(CodingErrorAction.REPLACE).onUnmappableCharacter(CodingErrorAction.REPLACE).decode(ByteBuffer.wrap((byte[]) objectRef.element)).toString();
        } catch (java.nio.charset.CharacterCodingException ex) {
            charBufferTmp = new String((byte[]) objectRef.element, this$0.charset);
        }
        final String charBuffer = charBufferTmp;
        Intrinsics.checkNotNullExpressionValue(charBuffer, "toString(...)");
        this$0.runOnUiThread(new Runnable() { // from class: com.hazeroot.scriptrunner.EditorActivity$$ExternalSyntheticLambda2
            @Override // java.lang.Runnable
            public final void run() {
                EditorActivity.load$lambda$8$lambda$7(this$0, charBuffer, isBinary, execBytes, objectRef);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void load$lambda$8$lambda$5(EditorActivity this$0) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        String string = this$0.getString(R.string.read_fail);
        Intrinsics.checkNotNullExpressionValue(string, "getString(...)");
        Ui.INSTANCE.toast(this$0, string);
        this$0.finish();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void load$lambda$8$lambda$7(final EditorActivity this$0, String text, boolean $isBin, byte[] $raw, final Ref.ObjectRef view) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        Intrinsics.checkNotNullParameter(text, "$text");
        Intrinsics.checkNotNullParameter(view, "$view");
        this$0.loadingText = true;
        EditText editText = this$0.edText;
        EditText editText2 = null;
        if (editText == null) {
            Intrinsics.throwUninitializedPropertyAccessException("edText");
            editText = null;
        }
        editText.setText(text);
        this$0.savedText = text;
        this$0.dirty = false;
        if (this$0.readOnly) {
            EditText editText3 = this$0.edText;
            if (editText3 == null) {
                Intrinsics.throwUninitializedPropertyAccessException("edText");
                editText3 = null;
            }
            editText3.setKeyListener(null);
            EditText editText4 = this$0.edText;
            if (editText4 == null) {
                Intrinsics.throwUninitializedPropertyAccessException("edText");
                editText4 = null;
            }
            editText4.setTextIsSelectable(true);
            View view2 = this$0.btnSaveIcon;
            if (view2 == null) {
                Intrinsics.throwUninitializedPropertyAccessException("btnSaveIcon");
                view2 = null;
            }
            view2.setVisibility(8);
            Ui ui = Ui.INSTANCE;
            EditorActivity editorActivity = this$0;
            String string = $isBin ? this$0.getString(R.string.toast_bin) : this$0.getString(R.string.toast_trunc_fmt, new Object[]{this$0.fmtSize($raw.length)});
            Intrinsics.checkNotNull(string);
            ui.toast(editorActivity, string);
        }
        this$0.loadingText = false;
        this$0.updateLines();
        EditText editText5 = this$0.edText;
        if (editText5 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("edText");
        } else {
            editText2 = editText5;
        }
        editText2.post(new Runnable() { // from class: com.hazeroot.scriptrunner.EditorActivity$$ExternalSyntheticLambda8
            @Override // java.lang.Runnable
            public final void run() {
                EditorActivity.load$lambda$8$lambda$7$lambda$6(this$0, view);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX WARN: Multi-variable type inference failed */
    public static final void load$lambda$8$lambda$7$lambda$6(EditorActivity this$0, Ref.ObjectRef view) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        Intrinsics.checkNotNullParameter(view, "$view");
        this$0.refreshStatus();
        if (this$0.readOnly || ((byte[]) view.element).length > 131072) {
            return;
        }
        this$0.applyHighlight();
    }

    private static final int isBinary$at(byte[] $raw, int i) {
        return $raw[i] & UByte.MAX_VALUE;
    }

    private final boolean isBinary(byte[] raw) {
        if (raw.length >= 2 && ((isBinary$at(raw, 0) == 255 && isBinary$at(raw, 1) == 254) || (isBinary$at(raw, 0) == 254 && isBinary$at(raw, 1) == 255))) {
            return false;
        }
        int n = Math.min(raw.length, 8192);
        int nul = 0;
        int even = 0;
        int odd = 0;
        for (int i = 0; i < n; i++) {
            if (raw[i] == 0) {
                nul++;
                if (i % 2 == 0) {
                    even++;
                } else {
                    odd++;
                }
            }
        }
        if (nul == 0) {
            return false;
        }
        return n <= 0 || ((even * 100) / n <= 30 && (odd * 100) / n <= 30);
    }

    private static final int detectCharset$at$9(byte[] $raw, int i) {
        return $raw[i] & UByte.MAX_VALUE;
    }

    private final Charset detectCharset(byte[] raw) {
        if (raw.length >= 3 && detectCharset$at$9(raw, 0) == 239 && detectCharset$at$9(raw, 1) == 187 && detectCharset$at$9(raw, 2) == 191) {
            return Charsets.UTF_8;
        }
        if (raw.length >= 2 && detectCharset$at$9(raw, 0) == 255 && detectCharset$at$9(raw, 1) == 254) {
            return charsetFor("UTF-16LE");
        }
        if (raw.length >= 2 && detectCharset$at$9(raw, 0) == 254 && detectCharset$at$9(raw, 1) == 255) {
            return charsetFor("UTF-16BE");
        }
        if (ArraysKt.contains(raw, (byte) 0)) {
            int even0 = 0;
            int odd0 = 0;
            for (int i = 0; i < raw.length; i++) {
                if (raw[i] == 0) {
                    if (i % 2 == 0) {
                        even0++;
                    } else {
                        odd0++;
                    }
                }
            }
            int n = raw.length;
            if ((even0 * 100) / n > 30) {
                return charsetFor("UTF-16BE");
            }
            if ((odd0 * 100) / n > 30) {
                return charsetFor("UTF-16LE");
            }
        }
        CharsetDecoder strict = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT);
        try {
            strict.decode(ByteBuffer.wrap(raw));
            return Charsets.UTF_8;
        } catch (Exception e) {
            return utf8NonAsciiRatio(raw) >= 0.6f ? Charsets.UTF_8 : charsetFor("GB18030");
        }
    }

    private final float utf8NonAsciiRatio(byte[] raw) {
        int len;
        int i = 0;
        int valid = 0;
        int nonAscii = 0;
        while (i < raw.length) {
            int b = raw[i] & 255;
            if (b < 128) {
                i++;
            } else {
                nonAscii++;
                boolean z = false;
                if ((b & 224) == 192) {
                    len = 2;
                } else if ((b & 240) == 224) {
                    len = 3;
                } else {
                    len = (b & 248) == 240 ? 4 : 0;
                }
                if (2 <= len && len < 5) {
                    z = true;
                }
                if (z && i + len <= raw.length) {
                    boolean ok = true;
                    int j = 1;
                    while (true) {
                        if (j >= len) {
                            break;
                        }
                        if ((raw[i + j] & 192) != 128) {
                            ok = false;
                            break;
                        }
                        j++;
                    }
                    if (ok) {
                        valid += len;
                        i += len;
                    }
                }
                i++;
            }
        }
        if (nonAscii == 0) {
            return 0.0f;
        }
        return valid / nonAscii;
    }

    private final Charset charsetFor(String name) {
        try {
            Charset forName = Charset.forName(name);
            Intrinsics.checkNotNull(forName);
            return forName;
        } catch (Exception e) {
            return Charsets.UTF_8;
        }
    }

    private final void save() {
        if (this.readOnly) {
            return;
        }
        EditText editText = this.edText;
        EditText editText2 = null;
        if (editText == null) {
            Intrinsics.throwUninitializedPropertyAccessException("edText");
            editText = null;
        }
        byte[] bytes = editText.getText().toString().getBytes(this.charset);
        Intrinsics.checkNotNullExpressionValue(bytes, "getBytes(...)");
        File tmp = new File(getCacheDir(), "editor_tmp");
        try {
            FilesKt.writeBytes(tmp, bytes);
            Engine engine = Engine.INSTANCE;
            Engine engine2 = Engine.INSTANCE;
            String absolutePath = tmp.getAbsolutePath();
            Intrinsics.checkNotNullExpressionValue(absolutePath, "getAbsolutePath(...)");
            String q = engine2.q(absolutePath);
            Engine engine3 = Engine.INSTANCE;
            String str = this.path;
            if (str == null) {
                Intrinsics.throwUninitializedPropertyAccessException("path");
                str = null;
            }
            String r = engine.exec("cp " + q + " " + engine3.q(str));
            tmp.delete();
            if (r == null) {
                String string = getString(R.string.save_fail);
                Intrinsics.checkNotNullExpressionValue(string, "getString(...)");
                Ui.INSTANCE.toast(this, string);
                return;
            }
            Engine engine4 = Engine.INSTANCE;
            Engine engine5 = Engine.INSTANCE;
            String str2 = this.path;
            if (str2 == null) {
                Intrinsics.throwUninitializedPropertyAccessException("path");
                str2 = null;
            }
            String exec = engine4.exec("wc -c < " + engine5.q(str2));
            String verify = exec != null ? StringsKt.trim((CharSequence) exec).toString() : null;
            if (!Intrinsics.areEqual(verify, String.valueOf(bytes.length))) {
                String string2 = getString(R.string.verify_fail);
                Intrinsics.checkNotNullExpressionValue(string2, "getString(...)");
                Ui.INSTANCE.toast(this, string2);
                return;
            }
            EditText editText3 = this.edText;
            if (editText3 == null) {
                Intrinsics.throwUninitializedPropertyAccessException("edText");
            } else {
                editText2 = editText3;
            }
            this.savedText = editText2.getText().toString();
            this.fileBytes = bytes.length;
            this.dirty = false;
            refreshStatus();
            String string3 = getString(R.string.saved_fmt, new Object[]{this.charset.name()});
            Intrinsics.checkNotNullExpressionValue(string3, "getString(...)");
            Ui.INSTANCE.toast(this, string3);
        } catch (Exception e) {
            Ui ui = Ui.INSTANCE;
            EditorActivity editorActivity = this;
            int i = R.string.tmp_fail_fmt;
            Object[] objArr = new Object[1];
            String message = e.getMessage();
            if (message == null) {
                message = "";
            }
            objArr[0] = message;
            String string4 = getString(i, objArr);
            Intrinsics.checkNotNullExpressionValue(string4, "getString(...)");
            ui.toast(editorActivity, string4);
        }
    }

    @Override // androidx.activity.ComponentActivity, android.app.Activity
    @Deprecated(message = "Deprecated in Java")
    public void onBackPressed() {
        if (this.dirty) {
            confirmExit();
        } else {
            super.onBackPressed();
        }
    }

    @Override // androidx.appcompat.app.AppCompatActivity, androidx.fragment.app.FragmentActivity, android.app.Activity
    protected void onDestroy() {
        this.hlHandler.removeCallbacks(this.hlRunnable);
        super.onDestroy();
    }

    private final void confirmExit() {
        new AlertDialog.Builder(this).setTitle(getString(R.string.unsaved_title)).setMessage(getString(R.string.unsaved_msg)).setPositiveButton(getString(R.string.discard), new DialogInterface.OnClickListener() { // from class: com.hazeroot.scriptrunner.EditorActivity$$ExternalSyntheticLambda3
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i) {
                EditorActivity.confirmExit$lambda$10(EditorActivity.this, dialogInterface, i);
            }
        }).setNegativeButton(getString(R.string.keep_editing), (DialogInterface.OnClickListener) null).show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void confirmExit$lambda$10(EditorActivity this$0, DialogInterface dialogInterface, int i) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        this$0.finish();
    }
}
