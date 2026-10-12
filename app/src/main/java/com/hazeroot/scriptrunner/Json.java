package com.hazeroot.scriptrunner;

import androidx.constraintlayout.widget.ConstraintLayout;
import java.util.ArrayList;
import java.util.List;
import kotlin.Metadata;
import kotlin.jvm.internal.Intrinsics;
import org.json.JSONArray;
import org.json.JSONObject;

/* compiled from: Models.kt */

/* loaded from: classes3.dex */
public final class Json {
    public static final Json INSTANCE = new Json();

    private Json() {
    }

    public final String scriptsToJson(List<ScriptItem> list) {
        Intrinsics.checkNotNullParameter(list, "list");
        JSONArray arr = new JSONArray();
        List<ScriptItem> $this$forEach$iv = list;
        for (Object element$iv : $this$forEach$iv) {
            ScriptItem s = (ScriptItem) element$iv;
            arr.put(INSTANCE.scriptToJson(s));
        }
        String jSONArray = arr.toString();
        Intrinsics.checkNotNullExpressionValue(jSONArray, "toString(...)");
        return jSONArray;
    }

    public final JSONObject scriptToJson(ScriptItem s) {
        Intrinsics.checkNotNullParameter(s, "s");
        try {
            JSONObject o = new JSONObject();
            o.put("name", s.getName());
            o.put("path", s.getPath());
            JSONArray ris = new JSONArray();
            Iterable $this$forEach$iv = s.getRecordedInputs();
            for (Object element$iv : $this$forEach$iv) {
                RecordedInput ri = (RecordedInput) element$iv;
                ris.put(new JSONObject().put("prompt", ri.getPrompt()).put("value", ri.getValue()));
            }
            o.put("rec", ris);
            return o;
        } catch (org.json.JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public final List<ScriptItem> scriptsFromJson(String txt) {
        Intrinsics.checkNotNullParameter(txt, "txt");
        List list = new ArrayList();
        try {
            JSONArray arr = new JSONArray(txt);
            int length = arr.length();
            for (int i = 0; i < length; i++) {
                JSONObject jSONObject = arr.getJSONObject(i);
                Intrinsics.checkNotNullExpressionValue(jSONObject, "getJSONObject(...)");
                list.add(scriptFromJson(jSONObject));
            }
        } catch (Exception e) {
        }
        return list;
    }

    public final ScriptItem scriptFromJson(JSONObject o) {
        Intrinsics.checkNotNullParameter(o, "o");
        try {
            String optString = o.optString("name");
            Intrinsics.checkNotNullExpressionValue(optString, "optString(...)");
            String optString2 = o.optString("path");
            Intrinsics.checkNotNullExpressionValue(optString2, "optString(...)");
            ScriptItem s = new ScriptItem(optString, optString2, null, false, false, null, 60, null);
            JSONArray ris = o.optJSONArray("rec");
            if (ris == null) {
                ris = new JSONArray();
            }
            int length = ris.length();
            for (int i = 0; i < length; i++) {
                JSONObject ro2 = ris.getJSONObject(i);
                List<RecordedInput> recordedInputs = s.getRecordedInputs();
                String optString3 = ro2.optString("prompt");
                Intrinsics.checkNotNullExpressionValue(optString3, "optString(...)");
                String optString4 = ro2.optString("value");
                Intrinsics.checkNotNullExpressionValue(optString4, "optString(...)");
                recordedInputs.add(new RecordedInput(optString3, optString4));
            }
            return s;
        } catch (org.json.JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public final String presetsToJson(List<Preset> list) {
        Intrinsics.checkNotNullParameter(list, "list");
        try {
            JSONArray jSONArray = new JSONArray();
            List<Preset> $this$forEach$iv = list;
            for (Object element$iv : $this$forEach$iv) {
                Preset p = (Preset) element$iv;
                JSONObject o = new JSONObject();
                o.put("name", p.getName());
                if (p.getTargetApp().length() > 0) {
                    o.put("targetApp", p.getTargetApp());
                }
                JSONArray ss = new JSONArray();
                Iterable $this$forEach$iv2 = p.getScripts();
                for (Object element$iv2 : $this$forEach$iv2) {
                    ScriptItem it = (ScriptItem) element$iv2;
                    ss.put(INSTANCE.scriptToJson(it));
                }
                o.put("scripts", ss);
                jSONArray.put(o);
            }
            String jSONArray2 = jSONArray.toString();
            Intrinsics.checkNotNullExpressionValue(jSONArray2, "toString(...)");
            return jSONArray2;
        } catch (org.json.JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public final List<Preset> presetsFromJson(String txt) {
        Intrinsics.checkNotNullParameter(txt, "txt");
        List list = new ArrayList();
        try {
            JSONArray arr = new JSONArray(txt);
            int length = arr.length();
            for (int i = 0; i < length; i++) {
                JSONObject o = arr.getJSONObject(i);
                List ss = new ArrayList();
                JSONArray sa = o.optJSONArray("scripts");
                if (sa == null) {
                    sa = new JSONArray();
                }
                int length2 = sa.length();
                for (int j = 0; j < length2; j++) {
                    JSONObject jSONObject = sa.getJSONObject(j);
                    Intrinsics.checkNotNullExpressionValue(jSONObject, "getJSONObject(...)");
                    ss.add(scriptFromJson(jSONObject));
                }
                String optString = o.optString("name");
                Intrinsics.checkNotNullExpressionValue(optString, "optString(...)");
                String optString2 = o.optString("targetApp");
                Intrinsics.checkNotNullExpressionValue(optString2, "optString(...)");
                list.add(new Preset(optString, ss, optString2));
            }
        } catch (Exception e) {
        }
        return list;
    }

    public final ScriptItem deepcopy(ScriptItem s) {
        Intrinsics.checkNotNullParameter(s, "s");
        return scriptFromJson(scriptToJson(s));
    }
}
