package com.hazeroot.scriptrunner;

import android.content.Context;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import java.util.Arrays;
import java.util.List;
import kotlin.Metadata;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.functions.Function2;
import kotlin.jvm.internal.Intrinsics;

/* compiled from: ScriptAdapter.kt */

/* loaded from: classes3.dex */
public final class ScriptAdapter extends RecyclerView.Adapter<ScriptAdapter.VH> {
    private final List<ScriptItem> items;
    private final Function1<Integer, Unit> onDelete;
    private final Function1<Integer, Unit> onEdit;
    private final Function2<Integer, Integer, Unit> onMove;

    /* JADX WARN: Multi-variable type inference failed */
    public ScriptAdapter(List<ScriptItem> items, Function2<Integer, Integer, Unit> onMove, Function1<Integer, Unit> onEdit, Function1<Integer, Unit> onDelete) {
        Intrinsics.checkNotNullParameter(items, "items");
        Intrinsics.checkNotNullParameter(onMove, "onMove");
        Intrinsics.checkNotNullParameter(onEdit, "onEdit");
        Intrinsics.checkNotNullParameter(onDelete, "onDelete");
        this.items = items;
        this.onMove = onMove;
        this.onEdit = onEdit;
        this.onDelete = onDelete;
    }

    /* compiled from: ScriptAdapter.kt */
    
    public static final class VH extends RecyclerView.ViewHolder {
        private final View activeBar;
        private final View divider;
        private final View dot;
        private final TextView index;
        private final ImageView more;
        private final TextView name;

        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        public VH(View v) {
            super(v);
            Intrinsics.checkNotNullParameter(v, "v");
            View findViewById = v.findViewById(R.id.dot);
            Intrinsics.checkNotNullExpressionValue(findViewById, "findViewById(...)");
            this.dot = findViewById;
            View findViewById2 = v.findViewById(R.id.name);
            Intrinsics.checkNotNullExpressionValue(findViewById2, "findViewById(...)");
            this.name = (TextView) findViewById2;
            View findViewById3 = v.findViewById(R.id.index);
            Intrinsics.checkNotNullExpressionValue(findViewById3, "findViewById(...)");
            this.index = (TextView) findViewById3;
            View findViewById4 = v.findViewById(R.id.btnMore);
            Intrinsics.checkNotNullExpressionValue(findViewById4, "findViewById(...)");
            this.more = (ImageView) findViewById4;
            View findViewById5 = v.findViewById(R.id.activeBar);
            Intrinsics.checkNotNullExpressionValue(findViewById5, "findViewById(...)");
            this.activeBar = findViewById5;
            View findViewById6 = v.findViewById(R.id.divider);
            Intrinsics.checkNotNullExpressionValue(findViewById6, "findViewById(...)");
            this.divider = findViewById6;
        }

        public final View getDot() {
            return this.dot;
        }

        public final TextView getName() {
            return this.name;
        }

        public final TextView getIndex() {
            return this.index;
        }

        public final ImageView getMore() {
            return this.more;
        }

        public final View getActiveBar() {
            return this.activeBar;
        }

        public final View getDivider() {
            return this.divider;
        }
    }

    @Override // androidx.recyclerview.widget.RecyclerView.Adapter
    public VH onCreateViewHolder(ViewGroup parent, int viewType) {
        Intrinsics.checkNotNullParameter(parent, "parent");
        View inflate = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_script, parent, false);
        Intrinsics.checkNotNullExpressionValue(inflate, "inflate(...)");
        return new VH(inflate);
    }

    @Override // androidx.recyclerview.widget.RecyclerView.Adapter
    public int getItemCount() {
        return this.items.size();
    }

    @Override // androidx.recyclerview.widget.RecyclerView.Adapter
    public void onBindViewHolder(VH h, final int pos) {
        int dotRes;
        Integer exitCode;
        Intrinsics.checkNotNullParameter(h, "h");
        ScriptItem s = this.items.get(pos);
        final Context ctx = h.itemView.getContext();
        h.getName().setText(s.getName());
        TextView index = h.getIndex();
        String format = String.format("%02d", Arrays.copyOf(new Object[]{Integer.valueOf(pos + 1)}, 1));
        Intrinsics.checkNotNullExpressionValue(format, "format(...)");
        index.setText(format);
        if (s.getRunningNow()) {
            dotRes = R.drawable.dot_running;
        } else if (s.getExitCode() == null || ((exitCode = s.getExitCode()) != null && exitCode.intValue() == 0)) {
            dotRes = s.getDone() ? R.drawable.dot_done : R.drawable.dot_idle;
        } else {
            dotRes = R.drawable.dot_error;
        }
        h.getDot().setBackgroundResource(dotRes);
        h.getActiveBar().setVisibility(s.getRunningNow() ? 0 : 8);
        h.getDivider().setVisibility(pos == getItemCount() - 1 ? 8 : 0);
        h.getMore().setOnClickListener(new View.OnClickListener() { // from class: com.hazeroot.scriptrunner.ScriptAdapter$$ExternalSyntheticLambda1
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                ScriptAdapter.onBindViewHolder$lambda$1(ctx, ScriptAdapter.this, pos, view);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void onBindViewHolder$lambda$1(Context $ctx, final ScriptAdapter this$0, final int $pos, View anchor) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        PopupMenu popup = new PopupMenu(new ContextThemeWrapper($ctx, R.style.Theme_App), anchor);
        popup.getMenuInflater().inflate(R.menu.menu_script_actions, popup.getMenu());
        MenuItem del = popup.getMenu().findItem(R.id.actionDelete);
        SpannableString t = new SpannableString(del.getTitle());
        t.setSpan(new ForegroundColorSpan(ContextCompat.getColor($ctx, R.color.error_text)), 0, t.length(), 33);
        del.setTitle(t);
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() { // from class: com.hazeroot.scriptrunner.ScriptAdapter$$ExternalSyntheticLambda0
            @Override // android.widget.PopupMenu.OnMenuItemClickListener
            public final boolean onMenuItemClick(MenuItem menuItem) {
                boolean onBindViewHolder$lambda$1$lambda$0;
                onBindViewHolder$lambda$1$lambda$0 = ScriptAdapter.onBindViewHolder$lambda$1$lambda$0(this$0, $pos, menuItem);
                return onBindViewHolder$lambda$1$lambda$0;
            }
        });
        popup.show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final boolean onBindViewHolder$lambda$1$lambda$0(ScriptAdapter this$0, int $pos, MenuItem mi) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        int itemId = mi.getItemId();
        if (itemId == R.id.actionUp) {
            this$0.onMove.invoke(Integer.valueOf($pos), -1);
        } else if (itemId == R.id.actionDown) {
            this$0.onMove.invoke(Integer.valueOf($pos), 1);
        } else if (itemId == R.id.actionEdit) {
            this$0.onEdit.invoke(Integer.valueOf($pos));
        } else if (itemId == R.id.actionDelete) {
            this$0.onDelete.invoke(Integer.valueOf($pos));
        }
        return true;
    }
}
