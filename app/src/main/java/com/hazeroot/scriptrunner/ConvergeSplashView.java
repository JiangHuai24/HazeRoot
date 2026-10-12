package com.hazeroot.scriptrunner;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import androidx.constraintlayout.widget.ConstraintLayout;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import kotlin.Metadata;
import kotlin.Unit;
import kotlin.collections.CollectionsKt;
import kotlin.collections.IntIterator;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.internal.Intrinsics;
import kotlin.random.Random;
import kotlin.random.RandomKt;
import kotlin.ranges.RangesKt;

/* compiled from: SplashActivity.kt */

/* loaded from: classes3.dex */
public final class ConvergeSplashView extends View {
    private static final float BLOCK_AT = 1380.0f;
    private static final float BLOCK_MS = 320.0f;
    private static final float FLY_MS = 1200.0f;
    private static final float NAME_AT = 1650.0f;
    private static final float NAME_MS = 450.0f;
    private ValueAnimator animator;
    private int blockColor;
    private final Paint blockPaint;
    private float blockRadius;
    private int blockStroke;
    private List<? extends RectF> clipRects;
    private final int colorH;
    private float cx;
    private float cy;
    private boolean done;
    private RectF dstRect;
    private final AccelerateInterpolator easeIn;
    private final DecelerateInterpolator easeOut;
    private float elapsed;
    private int fragFrom;
    private int fragTo;
    private Bitmap hBitmap;
    private final Paint hPaint;
    private float iconSize;
    private boolean launchedFired;
    private float minDim;
    private final Paint namePaint;
    private float nameSize;
    private Function0<Unit> onComplete;
    private Function0<Unit> onLaunch;
    private final Random random;
    private final List<Shard> shards;
    private final Paint strokePaint;
    private static final float CONVERGE_END = 1500.0f;
    private static final float ZOOM_MS = 700.0f;
    private static final float HOLD_END = CONVERGE_END + ZOOM_MS;
    private static final float LAUNCH_AT = HOLD_END + 210.00002f;
    private static final float TOTAL = HOLD_END + ZOOM_MS;

    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
    public ConvergeSplashView(Context context) {
        super(context);
        int i;
        Intrinsics.checkNotNullParameter(context, "context");
        this.random = RandomKt.Random(7411);
        this.shards = new ArrayList();
        this.easeIn = new AccelerateInterpolator(1.4f);
        this.easeOut = new DecelerateInterpolator(1.6f);
        this.hPaint = new Paint(1);
        Paint $this$namePaint_u24lambda_u240 = new Paint(1);
        $this$namePaint_u24lambda_u240.setTextAlign(Paint.Align.CENTER);
        $this$namePaint_u24lambda_u240.setTypeface(Typeface.create("sans-serif-medium", 0));
        this.namePaint = $this$namePaint_u24lambda_u240;
        this.dstRect = new RectF();
        this.blockPaint = new Paint(1);
        Paint $this$strokePaint_u24lambda_u241 = new Paint(1);
        $this$strokePaint_u24lambda_u241.setStyle(Paint.Style.STROKE);
        this.strokePaint = $this$strokePaint_u24lambda_u241;
        this.colorH = -1;
        this.fragFrom = -16053493;
        this.fragTo = -1;
        this.blockColor = -16053493;
        this.clipRects = CollectionsKt.emptyList();
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 3; col++) {
                List<Shard> list = this.shards;
                float nextFloat = this.random.nextFloat() * 6.2831855f;
                float nextFloat2 = (this.random.nextFloat() * 0.18f) + 0.46f;
                float f = 1.0f;
                float nextFloat3 = ((this.random.nextFloat() * 130.0f) + 90.0f) * (this.random.nextBoolean() ? 1.0f : -1.0f);
                float nextFloat4 = (this.random.nextFloat() * 0.3f) + 1.15f;
                float nextFloat5 = (this.random.nextFloat() * 0.15f) + 0.09f;
                if (!this.random.nextBoolean()) {
                    f = -1.0f;
                }
                list.add(new Shard(col, row, nextFloat, nextFloat2, nextFloat3, nextFloat4, nextFloat5 * f, this.random.nextLong(0L, 80L) + (((row * 3) + col) * 22)));
            }
        }
        boolean night = (context.getResources().getConfiguration().uiMode & 48) == 32;
        this.blockColor = night ? -14935010 : -16053493;
        this.blockStroke = night ? 553648127 : 0;
        if (!night) {
            i = -1;
        } else {
            i = -1;
            this.fragFrom = -1;
        }
        this.namePaint.setColor(night ? i : -16053493);
    }

    public final Function0<Unit> getOnLaunch() {
        return this.onLaunch;
    }

    public final void setOnLaunch(Function0<Unit> function0) {
        this.onLaunch = function0;
    }

    public final Function0<Unit> getOnComplete() {
        return this.onComplete;
    }

    public final void setOnComplete(Function0<Unit> function0) {
        this.onComplete = function0;
    }

    /* compiled from: SplashActivity.kt */
    
    private static final /* data */ class Shard {
        private final float angle;
        private final float arcF;
        private final int col;
        private final long delayMs;
        private final float radiusF;
        private final float rot0;
        private final int row;
        private final float scale0;









        public final Shard copy(int col, int row, float angle, float radiusF, float rot0, float scale0, float arcF, long delayMs) {
            return new Shard(col, row, angle, radiusF, rot0, scale0, arcF, delayMs);
        }

        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof Shard)) {
                return false;
            }
            Shard shard = (Shard) other;
            return this.col == shard.col && this.row == shard.row && Float.compare(this.angle, shard.angle) == 0 && Float.compare(this.radiusF, shard.radiusF) == 0 && Float.compare(this.rot0, shard.rot0) == 0 && Float.compare(this.scale0, shard.scale0) == 0 && Float.compare(this.arcF, shard.arcF) == 0 && this.delayMs == shard.delayMs;
        }

        public int hashCode() {
            return (((((((((((((Integer.hashCode(this.col) * 31) + Integer.hashCode(this.row)) * 31) + Float.hashCode(this.angle)) * 31) + Float.hashCode(this.radiusF)) * 31) + Float.hashCode(this.rot0)) * 31) + Float.hashCode(this.scale0)) * 31) + Float.hashCode(this.arcF)) * 31) + Long.hashCode(this.delayMs);
        }

        public String toString() {
            return "Shard(col=" + this.col + ", row=" + this.row + ", angle=" + this.angle + ", radiusF=" + this.radiusF + ", rot0=" + this.rot0 + ", scale0=" + this.scale0 + ", arcF=" + this.arcF + ", delayMs=" + this.delayMs + ")";
        }

        public Shard(int col, int row, float angle, float radiusF, float rot0, float scale0, float arcF, long delayMs) {
            this.col = col;
            this.row = row;
            this.angle = angle;
            this.radiusF = radiusF;
            this.rot0 = rot0;
            this.scale0 = scale0;
            this.arcF = arcF;
            this.delayMs = delayMs;
        }

        public final int getCol() {
            return this.col;
        }

        public final int getRow() {
            return this.row;
        }

        public final float getAngle() {
            return this.angle;
        }

        public final float getRadiusF() {
            return this.radiusF;
        }

        public final float getRot0() {
            return this.rot0;
        }

        public final float getScale0() {
            return this.scale0;
        }

        public final float getArcF() {
            return this.arcF;
        }

        public final long getDelayMs() {
            return this.delayMs;
        }
    }

    @Override // android.view.View
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w != 0 && h != 0) {
            this.cx = w / 2.0f;
            this.cy = h / 2.0f;
            this.minDim = Math.min(w, h);
            this.iconSize = this.minDim * 0.24f;
            this.blockRadius = this.iconSize * 0.24f;
            this.nameSize = this.iconSize * 0.22f;
            this.namePaint.setTextSize(this.nameSize);
            Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.drawable.ic_brand_h);
            if (bmp != null) {
                this.hBitmap = bmp;
                this.hPaint.setFilterBitmap(true);
            }
            float hH = this.iconSize * 0.56f;
            Bitmap it = this.hBitmap;
            float ratio = it != null ? it.getWidth() / (float) it.getHeight() : 0.565f;
            float wH = hH * ratio;
            float left = (-wH) / 2.0f;
            float top = (-hH) / 2.0f;
            this.dstRect = new RectF(left, top, left + wH, top + hH);
            float cw = wH / 3.0f;
            float ch = hH / 4.0f;
            Iterable $this$map$iv = RangesKt.until(0, 12);
            Collection destination$iv$iv = new ArrayList(CollectionsKt.collectionSizeOrDefault($this$map$iv, 10));
            Iterator<Integer> it2 = $this$map$iv.iterator();
            while (it2.hasNext()) {
                int item$iv$iv = ((IntIterator) it2).nextInt();
                int c = item$iv$iv % 3;
                int r = item$iv$iv / 3;
                destination$iv$iv.add(new RectF((c * cw) + left, (r * ch) + top, ((c + 1) * cw) + left, ((r + 1) * ch) + top));
            }
            this.clipRects = (List) destination$iv$iv;
        }
    }

    @Override // android.view.View
    protected void onDraw(Canvas canvas) {
        int i;
        float blockP;
        float mix;
        int tint;
        Iterator<Shard> it;
        int i2;
        float t;
        float blockP2;
        float fade;
        Intrinsics.checkNotNullParameter(canvas, "canvas");
        super.onDraw(canvas);
        float t2 = this.elapsed;
        float zoomP = RangesKt.coerceIn((t2 - HOLD_END) / ZOOM_MS, 0.0f, 1.0f);
        float fade2 = zoomP < 0.55f ? 1.0f : 1.0f - ((zoomP - 0.55f) / 0.45f);
        if (fade2 <= 0.0f) {
            return;
        }
        if (t2 >= LAUNCH_AT && !this.launchedFired) {
            this.launchedFired = true;
            Function0<Unit> function0 = this.onLaunch;
            if (function0 != null) {
                function0.invoke();
            }
        }
        float blockP3 = RangesKt.coerceIn((t2 - BLOCK_AT) / BLOCK_MS, 0.0f, 1.0f);
        float blockA = this.easeOut.getInterpolation(blockP3);
        float blockS = (this.easeOut.getInterpolation(blockP3) * 0.12f) + 0.88f;
        float popP = RangesKt.coerceIn((t2 - CONVERGE_END) / (HOLD_END - CONVERGE_END), 0.0f, 1.0f);
        float pop = (((float) Math.sin(popP * 3.1415927f)) * 0.04f) + 1.0f;
        float zoom = t2 < HOLD_END ? pop : ((this.easeIn.getInterpolation(zoomP) * 9.0f) + 1.0f) * pop;
        canvas.save();
        canvas.translate(this.cx, this.cy);
        canvas.scale(zoom, zoom);
        canvas.translate(-this.cx, -this.cy);
        if (blockA <= 0.0f) {
            i = 0;
            blockP = blockP3;
        } else {
            this.blockPaint.setColor(this.blockColor);
            this.blockPaint.setAlpha(RangesKt.coerceIn((int) (blockA * 255.0f * fade2), 0, 255));
            float half = (this.iconSize / 2.0f) * blockS;
            i = 0;
            blockP = blockP3;
            canvas.drawRoundRect(this.cx - half, this.cy - half, this.cx + half, this.cy + half, this.blockRadius, this.blockRadius, this.blockPaint);
            if (this.blockStroke != 0) {
                this.strokePaint.setColor(this.blockStroke);
                this.strokePaint.setStrokeWidth(this.minDim * 0.002f);
                this.strokePaint.setAlpha(RangesKt.coerceIn((int) (blockA * 255.0f * fade2), 0, 255));
                canvas.drawRoundRect(this.cx - half, this.cy - half, this.cx + half, this.cy + half, this.blockRadius, this.blockRadius, this.strokePaint);
            }
        }
        float mix2 = this.fragFrom == this.fragTo ? 1.0f : blockA;
        int tint2 = mix2 >= 1.0f ? this.fragTo : Color.rgb((int) ((Color.red(this.fragFrom) * (1.0f - mix2)) + (Color.red(this.fragTo) * mix2)), (int) ((Color.green(this.fragFrom) * (1.0f - mix2)) + (Color.green(this.fragTo) * mix2)), (int) ((Color.blue(this.fragFrom) * (1.0f - mix2)) + (Color.blue(this.fragTo) * mix2)));
        this.hPaint.setColorFilter(new PorterDuffColorFilter(tint2, PorterDuff.Mode.SRC_IN));
        Iterator<Shard> it2 = this.shards.iterator();
        int i3 = i;
        while (it2.hasNext()) {
            int i4 = i3;
            int i5 = i3 + 1;
            Shard s = it2.next();
            float local = RangesKt.coerceIn((t2 - s.getDelayMs()) / FLY_MS, 0.0f, 1.0f);
            if (local <= 0.0f) {
                mix = mix2;
                tint = tint2;
                it = it2;
                i2 = i5;
                t = t2;
                blockP2 = blockP;
                fade = fade2;
            } else {
                float e = this.easeOut.getInterpolation(local);
                RectF clip = this.clipRects.get(i4);
                float txm = clip.centerX();
                float tym = clip.centerY();
                mix = mix2;
                tint = tint2;
                it = it2;
                float sx = this.cx + (((float) Math.cos(s.getAngle())) * s.getRadiusF() * this.minDim);
                float sy = this.cy + (((float) Math.sin(s.getAngle())) * s.getRadiusF() * this.minDim);
                float dx = (this.cx - txm) - sx;
                float dy = (this.cy - tym) - sy;
                t = t2;
                blockP2 = blockP;
                fade = fade2;
                Float valueOf = Float.valueOf((float) Math.hypot(dx, dy));
                float it3 = valueOf.floatValue();
                if (!(it3 > 0.0f)) {
                    valueOf = null;
                }
                float len = valueOf != null ? valueOf.floatValue() : 1.0f;
                float amp = s.getArcF() * s.getRadiusF() * this.minDim * ((float) Math.sin(local * 3.1415927f));
                float px = ((dx * e) + sx) - ((dy / len) * amp);
                float py = (dy * e) + sy + ((dx / len) * amp);
                float rot = s.getRot0() * (1.0f - e);
                float sc = s.getScale0() + ((1.0f - s.getScale0()) * e);
                i2 = i5;
                float alpha = RangesKt.coerceIn(local * 3.2f, 0.0f, 1.0f);
                canvas.save();
                canvas.translate(px, py);
                canvas.rotate(rot);
                canvas.scale(sc, sc);
                canvas.translate(txm, tym);
                canvas.clipRect(clip);
                this.hPaint.setColor(this.colorH);
                this.hPaint.setAlpha(RangesKt.coerceIn((int) (alpha * 255.0f * fade), 0, 255));
                Bitmap bmp = this.hBitmap;
                if (bmp != null) {
                    canvas.drawBitmap(bmp, (Rect) null, this.dstRect, this.hPaint);
                } else {
                    this.hPaint.setTextAlign(Paint.Align.CENTER);
                    this.hPaint.setTextSize(this.iconSize * 0.6f);
                    canvas.drawText("H", 0.0f, this.iconSize * 0.21f, this.hPaint);
                }
                canvas.restore();
            }
            mix2 = mix;
            tint2 = tint;
            it2 = it;
            t2 = t;
            blockP = blockP2;
            fade2 = fade;
            i3 = i2;
        }
        float fade3 = fade2;
        float nameP = RangesKt.coerceIn((t2 - NAME_AT) / NAME_MS, 0.0f, 1.0f);
        if (nameP > 0.0f) {
            float e2 = this.easeOut.getInterpolation(nameP);
            float bottom = this.cy + ((this.iconSize / 2.0f) * blockS);
            float base = (this.iconSize * 0.22f) + bottom + (this.nameSize * 0.35f) + ((1.0f - e2) * this.nameSize * 0.9f);
            this.namePaint.setAlpha(RangesKt.coerceIn((int) (255.0f * e2 * fade3), 0, 255));
            canvas.drawText("HazeRoot", this.cx, base, this.namePaint);
        }
        canvas.restore();
    }

    public final void start() {
        if (this.animator != null) {
            return;
        }
        ValueAnimator $this$start_u24lambda_u246 = ValueAnimator.ofFloat(0.0f, 1.0f);
        $this$start_u24lambda_u246.setDuration((long) TOTAL);
        $this$start_u24lambda_u246.setInterpolator(new LinearInterpolator());
        $this$start_u24lambda_u246.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: com.hazeroot.scriptrunner.ConvergeSplashView$$ExternalSyntheticLambda0
            @Override // android.animation.ValueAnimator.AnimatorUpdateListener
            public final void onAnimationUpdate(ValueAnimator valueAnimator) {
                ConvergeSplashView.start$lambda$6$lambda$5(ConvergeSplashView.this, valueAnimator);
            }
        });
        $this$start_u24lambda_u246.addListener(new AnimatorListenerAdapter() { // from class: com.hazeroot.scriptrunner.ConvergeSplashView$start$1$2
            @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
            public void onAnimationEnd(Animator a) {
                boolean z;
                Intrinsics.checkNotNullParameter(a, "a");
                z = ConvergeSplashView.this.done;
                if (!z) {
                    ConvergeSplashView.this.done = true;
                    Function0<Unit> onComplete = ConvergeSplashView.this.getOnComplete();
                    if (onComplete != null) {
                        onComplete.invoke();
                    }
                }
            }
        });
        $this$start_u24lambda_u246.start();
        this.animator = $this$start_u24lambda_u246;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void start$lambda$6$lambda$5(ConvergeSplashView this$0, ValueAnimator a) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        Intrinsics.checkNotNullParameter(a, "a");
        this$0.elapsed = a.getAnimatedFraction() * TOTAL;
        this$0.invalidate();
    }

    public final void skip() {
        ValueAnimator valueAnimator;
        if (this.done || (valueAnimator = this.animator) == null) {
            return;
        }
        valueAnimator.cancel();
    }
}
