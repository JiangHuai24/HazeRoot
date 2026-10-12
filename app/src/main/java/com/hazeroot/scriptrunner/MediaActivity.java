package com.hazeroot.scriptrunner;

import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.VideoView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import java.io.File;
import java.util.Arrays;
import kotlin.Metadata;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.internal.Intrinsics;
import kotlin.jvm.internal.Lambda;
import kotlin.text.StringsKt;

/* compiled from: MediaActivity.kt */

/* loaded from: classes3.dex */
public final class MediaActivity extends AppCompatActivity {
    private TextView btnPlay;
    private MediaPlayer mp;
    private String path;
    private boolean prepared;
    private SeekBar seek;
    private long size;
    private TextView timeView;
    private boolean userSeeking;
    private String name = "";
    private String kind = "video";
    private final Handler h = new Handler(Looper.getMainLooper());
    private final Runnable tick = new Runnable() { // from class: com.hazeroot.scriptrunner.MediaActivity$tick$1
        /* JADX WARN: Code restructure failed: missing block: B:10:0x001a, code lost:
        
            r1 = r5.this$0.seek;
         */
        @Override // java.lang.Runnable
        /*
            Code decompiled incorrectly, please refer to instructions dump.
            To view partially-correct add '--show-bad-code' argument
        */
        public void run() {
            android.media.MediaPlayer mp = MediaActivity.this.mp;
            if (mp == null) return;
            try {
                if (MediaActivity.this.prepared && !MediaActivity.this.userSeeking && MediaActivity.this.seek != null) {
                    MediaActivity.this.seek.setProgress(mp.getCurrentPosition());
                }
                if (MediaActivity.this.timeView != null) {
                    MediaActivity.this.timeView.setText(MediaActivity.this.fmt(mp.getCurrentPosition()) + " / " + MediaActivity.this.fmt(mp.getDuration()));
                }
            } catch (Exception ignored) {
            }
            MediaActivity.this.h.postDelayed(this, 500L);
        }
    };

    @Override // androidx.fragment.app.FragmentActivity, androidx.activity.ComponentActivity, androidx.core.app.ComponentActivity, android.app.Activity
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String stringExtra = getIntent().getStringExtra("path");
        if (stringExtra == null) {
            MediaActivity $this$onCreate_u24lambda_u240 = this;
            $this$onCreate_u24lambda_u240.finish();
            return;
        }
        this.path = stringExtra;
        String stringExtra2 = getIntent().getStringExtra("name");
        if (stringExtra2 == null) {
            String str = this.path;
            if (str == null) {
                Intrinsics.throwUninitializedPropertyAccessException("path");
                str = null;
            }
            stringExtra2 = Kt.substringAfterLast(str, '/', (String) null, 2, (Object) null);
        }
        this.name = stringExtra2;
        String stringExtra3 = getIntent().getStringExtra("kind");
        if (stringExtra3 == null) {
            stringExtra3 = "video";
        }
        this.kind = stringExtra3;
        this.size = getIntent().getLongExtra("size", 0L);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(1);
        root.setBackgroundColor(c(R.color.bg_page));
        root.setPadding(dp(12), dp(8), dp(12), dp(12));
        LinearLayout head = new LinearLayout(this);
        head.setOrientation(0);
        head.setGravity(16);
        TextView $this$onCreate_u24lambda_u244 = new TextView(this);
        $this$onCreate_u24lambda_u244.setText("‹");
        $this$onCreate_u24lambda_u244.setTextSize(26.0f);
        $this$onCreate_u24lambda_u244.setTextColor(c(R.color.label));
        $this$onCreate_u24lambda_u244.setPadding(dp(8), dp(2), dp(16), dp(2));
        $this$onCreate_u24lambda_u244.setContentDescription(getString(R.string.cd_close));
        $this$onCreate_u24lambda_u244.setOnClickListener(new View.OnClickListener() { // from class: com.hazeroot.scriptrunner.MediaActivity$$ExternalSyntheticLambda7
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MediaActivity.onCreate$lambda$4$lambda$3(MediaActivity.this, view);
            }
        });
        head.addView($this$onCreate_u24lambda_u244);
        TextView $this$onCreate_u24lambda_u245 = new TextView(this);
        $this$onCreate_u24lambda_u245.setText(this.name);
        $this$onCreate_u24lambda_u245.setTextSize(16.0f);
        $this$onCreate_u24lambda_u245.setTextColor(c(R.color.label));
        $this$onCreate_u24lambda_u245.setMaxLines(1);
        $this$onCreate_u24lambda_u245.setEllipsize(TextUtils.TruncateAt.MIDDLE);
        $this$onCreate_u24lambda_u245.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
        head.addView($this$onCreate_u24lambda_u245);
        root.addView(head);
        String str2 = this.kind;
        if (Intrinsics.areEqual(str2, "image")) {
            final ImageView $this$onCreate_u24lambda_u247 = new ImageView(this);
            LinearLayout.LayoutParams $this$onCreate_u24lambda_u247_u24lambda_u246 = new LinearLayout.LayoutParams(-1, 0, 1.0f);
            $this$onCreate_u24lambda_u247_u24lambda_u246.topMargin = dp(10);
            $this$onCreate_u24lambda_u247.setLayoutParams($this$onCreate_u24lambda_u247_u24lambda_u246);
            $this$onCreate_u24lambda_u247.setScaleType(ImageView.ScaleType.FIT_CENTER);
            root.addView($this$onCreate_u24lambda_u247);
            setContentView(root);
            resolve(new Function1<String, Unit>() { // from class: com.hazeroot.scriptrunner.MediaActivity$onCreate$4
                /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */

                @Override // kotlin.jvm.functions.Function1
                public /* bridge */ /* synthetic */ Unit invoke(String str3) {
                    invoke2(str3);
                    return Unit.INSTANCE;
                }

                /* renamed from: invoke, reason: avoid collision after fix types in other method */
                public final void invoke2(String p) {
                    if (p == null) {
                        MediaActivity mediaActivity = MediaActivity.this;
                        final MediaActivity mediaActivity2 = MediaActivity.this;
                        mediaActivity.ui(new Function0<Unit>() { // from class: com.hazeroot.scriptrunner.MediaActivity$onCreate$4.1

                            @Override // kotlin.jvm.functions.Function0
                            public /* bridge */ /* synthetic */ Unit invoke() {
                                invoke2();
                                return Unit.INSTANCE;
                            }

                            /* renamed from: invoke, reason: avoid collision after fix types in other method */
                            public final void invoke2() {
                                MediaActivity mediaActivity3 = MediaActivity.this;
                                String string = MediaActivity.this.getString(R.string.media_load_fail);
                                Intrinsics.checkNotNullExpressionValue(string, "getString(...)");
                                mediaActivity3.toast(string);
                            }
                        });
                    } else {
                        Bitmap bmpTmp;
                        try {
                            bmpTmp = BitmapFactory.decodeFile(p);
                        } catch (Exception e) {
                            bmpTmp = null;
                        }
                        final Bitmap bmp = bmpTmp;
                        MediaActivity mediaActivity3 = MediaActivity.this;
                        final MediaActivity mediaActivity4 = MediaActivity.this;
                        final ImageView imageView = $this$onCreate_u24lambda_u247;
                        mediaActivity3.ui(new Function0<Unit>() { // from class: com.hazeroot.scriptrunner.MediaActivity$onCreate$4.2
                            /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */

                            @Override // kotlin.jvm.functions.Function0
                            public /* bridge */ /* synthetic */ Unit invoke() {
                                invoke2();
                                return Unit.INSTANCE;
                            }

                            /* renamed from: invoke, reason: avoid collision after fix types in other method */
                            public final void invoke2() {
                                if (bmp == null) {
                                    MediaActivity mediaActivity5 = mediaActivity4;
                                    String string = mediaActivity4.getString(R.string.media_load_fail);
                                    Intrinsics.checkNotNullExpressionValue(string, "getString(...)");
                                    mediaActivity5.toast(string);
                                    return;
                                }
                                imageView.setImageBitmap(bmp);
                            }
                        });
                    }
                }
            });
            return;
        }
        if (Intrinsics.areEqual(str2, "video")) {
            FrameLayout frame = new FrameLayout(this);
            LinearLayout.LayoutParams $this$onCreate_u24lambda_u249_u24lambda_u248 = new LinearLayout.LayoutParams(-1, 0, 1.0f);
            $this$onCreate_u24lambda_u249_u24lambda_u248.topMargin = dp(10);
            frame.setLayoutParams($this$onCreate_u24lambda_u249_u24lambda_u248);
            frame.setBackgroundColor(ViewCompat.MEASURED_STATE_MASK);
            final VideoView $this$onCreate_u24lambda_u2410 = new VideoView(this);
            $this$onCreate_u24lambda_u2410.setLayoutParams(new FrameLayout.LayoutParams(-1, -1, 17));
            frame.addView($this$onCreate_u24lambda_u2410);
            root.addView(frame);
            setContentView(root);
            resolve(new Function1<String, Unit>() { // from class: com.hazeroot.scriptrunner.MediaActivity$onCreate$5
                /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */

                @Override // kotlin.jvm.functions.Function1
                public /* bridge */ /* synthetic */ Unit invoke(String str3) {
                    invoke2(str3);
                    return Unit.INSTANCE;
                }

                /* compiled from: MediaActivity.kt */
                
                /* renamed from: com.hazeroot.scriptrunner.MediaActivity$onCreate$5$2, reason: invalid class name */
                static final class AnonymousClass2 extends Lambda<Unit> implements Function0<Unit> {
                    final /* synthetic */ String $p;
                    final /* synthetic */ VideoView $vv;
                    final /* synthetic */ MediaActivity this$0;

                    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
                    AnonymousClass2(VideoView videoView, String str, MediaActivity mediaActivity) {
                        super(0);
                        this.$vv = videoView;
                        this.$p = str;
                        this.this$0 = mediaActivity;
                    }

                    @Override // kotlin.jvm.functions.Function0
                    public /* bridge */ /* synthetic */ Unit invoke() {
                        invoke2();
                        return Unit.INSTANCE;
                    }

                    /* renamed from: invoke, reason: avoid collision after fix types in other method */
                    public final void invoke2() {
                        this.$vv.setVideoPath(this.$p);
                        this.$vv.setMediaController(new MediaController(this.this$0));
                        this.$vv.setOnPreparedListener(new android.media.MediaPlayer.OnPreparedListener() {
                            @Override
                            public void onPrepared(android.media.MediaPlayer mp) {
                                AnonymousClass2.invoke$lambda$0(AnonymousClass2.this.$vv, mp);
                            }
                        });
                        this.$vv.setOnErrorListener(new android.media.MediaPlayer.OnErrorListener() {
                            @Override
                            public boolean onError(android.media.MediaPlayer mp, int what, int extra) {
                                return AnonymousClass2.invoke$lambda$1(AnonymousClass2.this.this$0, mp, what, extra);
                            }
                        });
                        return;
                    }

                    /* JADX INFO: Access modifiers changed from: private */
                    public static final void invoke$lambda$0(VideoView vv, MediaPlayer mp) {
                        Intrinsics.checkNotNullParameter(vv, "$vv");
                        mp.setLooping(false);
                        vv.start();
                    }

                    /* JADX INFO: Access modifiers changed from: private */
                    public static final boolean invoke$lambda$1(MediaActivity this$0, MediaPlayer mediaPlayer, int i, int i2) {
                        Intrinsics.checkNotNullParameter(this$0, "this$0");
                        String string = this$0.getString(R.string.media_load_fail);
                        Intrinsics.checkNotNullExpressionValue(string, "getString(...)");
                        this$0.toast(string);
                        return true;
                    }
                }

                /* renamed from: invoke, reason: avoid collision after fix types in other method */
                public final void invoke2(String p) {
                    if (p == null) {
                        MediaActivity mediaActivity = MediaActivity.this;
                        final MediaActivity mediaActivity2 = MediaActivity.this;
                        mediaActivity.ui(new Function0<Unit>() { // from class: com.hazeroot.scriptrunner.MediaActivity$onCreate$5.1

                            @Override // kotlin.jvm.functions.Function0
                            public /* bridge */ /* synthetic */ Unit invoke() {
                                invoke2();
                                return Unit.INSTANCE;
                            }

                            /* renamed from: invoke, reason: avoid collision after fix types in other method */
                            public final void invoke2() {
                                MediaActivity mediaActivity3 = MediaActivity.this;
                                String string = MediaActivity.this.getString(R.string.media_load_fail);
                                Intrinsics.checkNotNullExpressionValue(string, "getString(...)");
                                mediaActivity3.toast(string);
                            }
                        });
                        return;
                    }
                    MediaActivity.this.ui(new AnonymousClass2($this$onCreate_u24lambda_u2410, p, MediaActivity.this));
                }
            });
            return;
        }
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(1);
        box.setGravity(17);
        box.setLayoutParams(new LinearLayout.LayoutParams(-1, 0, 1.0f));
        TextView $this$onCreate_u24lambda_u2412 = new TextView(this);
        $this$onCreate_u24lambda_u2412.setText("♪");
        $this$onCreate_u24lambda_u2412.setTextSize(56.0f);
        $this$onCreate_u24lambda_u2412.setTextColor(c(R.color.accent));
        $this$onCreate_u24lambda_u2412.setGravity(17);
        box.addView($this$onCreate_u24lambda_u2412);
        TextView $this$onCreate_u24lambda_u2413 = new TextView(this);
        $this$onCreate_u24lambda_u2413.setText("0:00 / 0:00");
        $this$onCreate_u24lambda_u2413.setTextSize(14.0f);
        $this$onCreate_u24lambda_u2413.setTypeface(Typeface.MONOSPACE);
        $this$onCreate_u24lambda_u2413.setTextColor(c(R.color.label_secondary));
        $this$onCreate_u24lambda_u2413.setGravity(17);
        $this$onCreate_u24lambda_u2413.setPadding(0, dp(16), 0, dp(8));
        this.timeView = $this$onCreate_u24lambda_u2413;
        box.addView(this.timeView);
        SeekBar $this$onCreate_u24lambda_u2415 = new SeekBar(this);
        LinearLayout.LayoutParams $this$onCreate_u24lambda_u2415_u24lambda_u2414 = new LinearLayout.LayoutParams(-1, -2);
        $this$onCreate_u24lambda_u2415_u24lambda_u2414.setMargins(dp(28), 0, dp(28), 0);
        $this$onCreate_u24lambda_u2415.setLayoutParams($this$onCreate_u24lambda_u2415_u24lambda_u2414);
        $this$onCreate_u24lambda_u2415.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() { // from class: com.hazeroot.scriptrunner.MediaActivity$onCreate$8$2
            /* JADX WARN: Code restructure failed: missing block: B:4:0x000a, code lost:
            
                r0 = r1.this$0.mp;
             */
            @Override // android.widget.SeekBar.OnSeekBarChangeListener
            /*
                Code decompiled incorrectly, please refer to instructions dump.
                To view partially-correct add '--show-bad-code' argument
            */
            public void onProgressChanged(android.widget.SeekBar r2, int r3, boolean r4) {
                /*
                    r1 = this;
                    if (r4 == 0) goto L15
                    com.hazeroot.scriptrunner.MediaActivity r0 = com.hazeroot.scriptrunner.MediaActivity.this
                    boolean r0 = com.hazeroot.scriptrunner.MediaActivity.access$getPrepared$p(r0)
                    if (r0 == 0) goto L15
                    com.hazeroot.scriptrunner.MediaActivity r0 = com.hazeroot.scriptrunner.MediaActivity.this
                    android.media.MediaPlayer r0 = com.hazeroot.scriptrunner.MediaActivity.access$getMp$p(r0)
                    if (r0 == 0) goto L15
                    r0.seekTo(r3)
                L15:
                    return
                */
                throw new UnsupportedOperationException("Method not decompiled: com.hazeroot.scriptrunner.MediaActivity$onCreate$8$2.onProgressChanged(android.widget.SeekBar, int, boolean):void");
            }

            @Override // android.widget.SeekBar.OnSeekBarChangeListener
            public void onStartTrackingTouch(SeekBar sb) {
                MediaActivity.this.userSeeking = true;
            }

            @Override // android.widget.SeekBar.OnSeekBarChangeListener
            public void onStopTrackingTouch(SeekBar sb) {
                MediaActivity.this.userSeeking = false;
            }
        });
        this.seek = $this$onCreate_u24lambda_u2415;
        box.addView(this.seek);
        TextView $this$onCreate_u24lambda_u2417 = new TextView(this);
        $this$onCreate_u24lambda_u2417.setText("▶");
        $this$onCreate_u24lambda_u2417.setTextSize(36.0f);
        $this$onCreate_u24lambda_u2417.setTextColor(c(R.color.accent));
        $this$onCreate_u24lambda_u2417.setGravity(17);
        $this$onCreate_u24lambda_u2417.setPadding(0, dp(16), 0, 0);
        $this$onCreate_u24lambda_u2417.setOnClickListener(new View.OnClickListener() { // from class: com.hazeroot.scriptrunner.MediaActivity$$ExternalSyntheticLambda8
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MediaActivity.onCreate$lambda$17$lambda$16(MediaActivity.this, view);
            }
        });
        this.btnPlay = $this$onCreate_u24lambda_u2417;
        box.addView(this.btnPlay);
        root.addView(box);
        setContentView(root);
        resolve(new Function1<String, Unit>() { // from class: com.hazeroot.scriptrunner.MediaActivity$onCreate$10

            @Override // kotlin.jvm.functions.Function1
            public /* bridge */ /* synthetic */ Unit invoke(String str3) {
                invoke2(str3);
                return Unit.INSTANCE;
            }

            /* renamed from: invoke, reason: avoid collision after fix types in other method */
            public final void invoke2(final String p) {
                if (p == null) {
                    MediaActivity mediaActivity = MediaActivity.this;
                    final MediaActivity mediaActivity2 = MediaActivity.this;
                    mediaActivity.ui(new Function0<Unit>() { // from class: com.hazeroot.scriptrunner.MediaActivity$onCreate$10.1

                        @Override // kotlin.jvm.functions.Function0
                        public /* bridge */ /* synthetic */ Unit invoke() {
                            invoke2();
                            return Unit.INSTANCE;
                        }

                        /* renamed from: invoke, reason: avoid collision after fix types in other method */
                        public final void invoke2() {
                            MediaActivity mediaActivity3 = MediaActivity.this;
                            String string = MediaActivity.this.getString(R.string.media_load_fail);
                            Intrinsics.checkNotNullExpressionValue(string, "getString(...)");
                            mediaActivity3.toast(string);
                        }
                    });
                } else {
                    MediaActivity mediaActivity3 = MediaActivity.this;
                    final MediaActivity mediaActivity4 = MediaActivity.this;
                    mediaActivity3.ui(new Function0<Unit>() { // from class: com.hazeroot.scriptrunner.MediaActivity$onCreate$10.2
                        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */

                        @Override // kotlin.jvm.functions.Function0
                        public /* bridge */ /* synthetic */ Unit invoke() {
                            invoke2();
                            return Unit.INSTANCE;
                        }

                        /* renamed from: invoke, reason: avoid collision after fix types in other method */
                        public final void invoke2() {
                            MediaActivity.this.startAudio(p);
                        }
                    });
                }
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void onCreate$lambda$4$lambda$3(MediaActivity this$0, View it) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        this$0.finish();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void onCreate$lambda$17$lambda$16(MediaActivity this$0, View it) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        this$0.toggle();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final void startAudio(String p) {
        try {
            MediaPlayer m = new MediaPlayer();
            m.setAudioAttributes(new AudioAttributes.Builder().setUsage(1).setContentType(2).build());
            m.setDataSource(p);
            m.setOnPreparedListener(new MediaPlayer.OnPreparedListener() { // from class: com.hazeroot.scriptrunner.MediaActivity$$ExternalSyntheticLambda0
                @Override // android.media.MediaPlayer.OnPreparedListener
                public final void onPrepared(MediaPlayer mediaPlayer) {
                    MediaActivity.startAudio$lambda$18(MediaActivity.this, mediaPlayer);
                }
            });
            m.setOnCompletionListener(new MediaPlayer.OnCompletionListener() { // from class: com.hazeroot.scriptrunner.MediaActivity$$ExternalSyntheticLambda1
                @Override // android.media.MediaPlayer.OnCompletionListener
                public final void onCompletion(MediaPlayer mediaPlayer) {
                    MediaActivity.startAudio$lambda$19(MediaActivity.this, mediaPlayer);
                }
            });
            m.setOnErrorListener(new MediaPlayer.OnErrorListener() { // from class: com.hazeroot.scriptrunner.MediaActivity$$ExternalSyntheticLambda2
                @Override // android.media.MediaPlayer.OnErrorListener
                public final boolean onError(MediaPlayer mediaPlayer, int i, int i2) {
                    boolean startAudio$lambda$20;
                    startAudio$lambda$20 = MediaActivity.startAudio$lambda$20(MediaActivity.this, mediaPlayer, i, i2);
                    return startAudio$lambda$20;
                }
            });
            this.mp = m;
            m.prepareAsync();
        } catch (Exception e) {
            String string = getString(R.string.media_load_fail);
            Intrinsics.checkNotNullExpressionValue(string, "getString(...)");
            toast(string);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void startAudio$lambda$18(MediaActivity this$0, MediaPlayer player) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        this$0.prepared = true;
        SeekBar seekBar = this$0.seek;
        if (seekBar != null) {
            seekBar.setMax(player.getDuration());
        }
        player.start();
        TextView textView = this$0.btnPlay;
        if (textView != null) {
            textView.setText("⏸");
        }
        this$0.h.post(this$0.tick);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void startAudio$lambda$19(MediaActivity this$0, MediaPlayer it) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        TextView textView = this$0.btnPlay;
        if (textView == null) {
            return;
        }
        textView.setText("▶");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final boolean startAudio$lambda$20(MediaActivity this$0, MediaPlayer mediaPlayer, int i, int i2) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        String string = this$0.getString(R.string.media_load_fail);
        Intrinsics.checkNotNullExpressionValue(string, "getString(...)");
        this$0.toast(string);
        return true;
    }

    private final void toggle() {
        MediaPlayer m = this.mp;
        if (m == null) {
            return;
        }
        try {
            if (!m.isPlaying()) {
                m.start();
                TextView textView = this.btnPlay;
                if (textView != null) {
                    textView.setText("⏸");
                }
                return;
            }
            m.pause();
            TextView textView2 = this.btnPlay;
            if (textView2 != null) {
                textView2.setText("▶");
            }
        } catch (Exception e) {
        }
    }

    private final void resolve(final Function1<? super String, Unit> onReady) {
        String str = this.path;
        String str2 = null;
        if (str == null) {
            Intrinsics.throwUninitializedPropertyAccessException("path");
            str = null;
        }
        File f = new File(str);
        if (!f.canRead()) {
            if (this.size > 314572800) {
                new AlertDialog.Builder(this).setTitle(this.name).setMessage(getString(R.string.media_big_confirm, new Object[]{fmtSize(this.size)})).setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() { // from class: com.hazeroot.scriptrunner.MediaActivity$$ExternalSyntheticLambda4
                    @Override // android.content.DialogInterface.OnClickListener
                    public final void onClick(DialogInterface dialogInterface, int i) {
                        MediaActivity.resolve$lambda$21(MediaActivity.this, onReady, dialogInterface, i);
                    }
                }).setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() { // from class: com.hazeroot.scriptrunner.MediaActivity$$ExternalSyntheticLambda5
                    @Override // android.content.DialogInterface.OnClickListener
                    public final void onClick(DialogInterface dialogInterface, int i) {
                        MediaActivity.resolve$lambda$22(MediaActivity.this, dialogInterface, i);
                    }
                }).show();
                return;
            } else {
                copyThen(onReady);
                return;
            }
        }
        String str3 = this.path;
        if (str3 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("path");
        } else {
            str2 = str3;
        }
        onReady.invoke(str2);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void resolve$lambda$21(MediaActivity this$0, Function1 onReady, DialogInterface dialogInterface, int i) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        Intrinsics.checkNotNullParameter(onReady, "$onReady");
        this$0.copyThen(onReady);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void resolve$lambda$22(MediaActivity this$0, DialogInterface dialogInterface, int i) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        this$0.finish();
    }

    private final void copyThen(final Function1<? super String, Unit> onReady) {
        String string = getString(R.string.media_copying);
        Intrinsics.checkNotNullExpressionValue(string, "getString(...)");
        toast(string);
        new Thread(new Runnable() { // from class: com.hazeroot.scriptrunner.MediaActivity$$ExternalSyntheticLambda3
            @Override // java.lang.Runnable
            public final void run() {
                MediaActivity.copyThen$lambda$24(MediaActivity.this, onReady);
            }
        }).start();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void copyThen$lambda$24(final MediaActivity this$0, final Function1 onReady) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        Intrinsics.checkNotNullParameter(onReady, "$onReady");
        final File dst = new File(this$0.getCacheDir(), "media_" + this$0.name);
        Engine engine = Engine.INSTANCE;
        Engine engine2 = Engine.INSTANCE;
        String str = this$0.path;
        if (str == null) {
            Intrinsics.throwUninitializedPropertyAccessException("path");
            str = null;
        }
        String q = engine2.q(str);
        Engine engine3 = Engine.INSTANCE;
        String absolutePath = dst.getAbsolutePath();
        Intrinsics.checkNotNullExpressionValue(absolutePath, "getAbsolutePath(...)");
        final boolean ok = engine.exec(new StringBuilder().append("cp ").append(q).append(" ").append(engine3.q(absolutePath)).toString()) != null && dst.length() > 0;
        this$0.runOnUiThread(new Runnable() { // from class: com.hazeroot.scriptrunner.MediaActivity$$ExternalSyntheticLambda9
            @Override // java.lang.Runnable
            public final void run() {
                MediaActivity.copyThen$lambda$24$lambda$23(ok, onReady, dst, this$0);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void copyThen$lambda$24$lambda$23(boolean $ok, Function1 onReady, File dst, MediaActivity this$0) {
        Intrinsics.checkNotNullParameter(onReady, "$onReady");
        Intrinsics.checkNotNullParameter(dst, "$dst");
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        if (!$ok) {
            String string = this$0.getString(R.string.media_load_fail);
            Intrinsics.checkNotNullExpressionValue(string, "getString(...)");
            this$0.toast(string);
            this$0.finish();
            return;
        }
        onReady.invoke(dst.getAbsolutePath());
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final void ui(final Function0<Unit> f) {
        runOnUiThread(new Runnable() { // from class: com.hazeroot.scriptrunner.MediaActivity$$ExternalSyntheticLambda6
            @Override // java.lang.Runnable
            public final void run() {
                MediaActivity.ui$lambda$25(f);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void ui$lambda$25(Function0 tmp0) {
        Intrinsics.checkNotNullParameter(tmp0, "$tmp0");
        tmp0.invoke();
    }

    @Override // androidx.appcompat.app.AppCompatActivity, androidx.fragment.app.FragmentActivity, android.app.Activity
    protected void onDestroy() {
        this.h.removeCallbacks(this.tick);
        try {
            MediaPlayer mediaPlayer = this.mp;
            if (mediaPlayer != null) {
                mediaPlayer.release();
            }
        } catch (Exception e) {
        }
        this.mp = null;
        super.onDestroy();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final String fmt(int ms) {
        int s = ms / 1000;
        String format = String.format("%d:%02d", Arrays.copyOf(new Object[]{Integer.valueOf(s / 60), Integer.valueOf(s % 60)}, 2));
        Intrinsics.checkNotNullExpressionValue(format, "format(...)");
        return format;
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

    private final int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }

    private final int c(int id) {
        return ContextCompat.getColor(this, id);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final void toast(String msg) {
        Ui.INSTANCE.toast(this, msg);
    }
}
