package com.hazeroot.scriptrunner.core.fs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class DirHistoryTest {

    @Test
    public void record_andBack_worksLikeBrowser() {
        DirHistory h = new DirHistory();
        h.record("/");
        h.record("/data");
        h.record("/data/adb");

        assertEquals("/data/adb", h.current());
        assertEquals("/data", h.back());
        assertEquals("/", h.back());
        assertFalse(h.canBack());

        assertEquals("/data", h.forward());
        assertEquals("/data/adb", h.forward());
        assertFalse(h.canForward());
    }

    @Test
    public void record_afterBack_truncatesForwardBranch() {
        DirHistory h = new DirHistory();
        h.record("/");
        h.record("/data");
        h.record("/data/adb");
        h.back();                 // 回到 /data
        h.record("/data/local");  // 此时 /data/adb 的前进分支应被丢弃

        assertEquals("/data/local", h.current());
        assertFalse(h.canForward());
        assertEquals(3, h.size());   // /, /data, /data/local
    }

    @Test
    public void record_sameDirTwice_notDuplicated() {
        DirHistory h = new DirHistory();
        h.record("/data");
        h.record("/data");
        assertEquals(1, h.size());
        assertEquals("/data", h.current());
    }

    @Test
    public void back_onEmpty_returnsNull() {
        DirHistory h = new DirHistory();
        assertNull(h.back());
        assertNull(h.forward());
        assertNull(h.current());
    }

    @Test
    public void maxEntries_capped() {
        DirHistory h = new DirHistory();
        for (int i = 0; i < 100; i++) {
            h.record("/d" + i);
        }
        assertTrue(h.size() <= 60);
        assertEquals("/d99", h.current());
    }

    @Test
    public void nullOrEmptyDir_ignored() {
        DirHistory h = new DirHistory();
        h.record(null);
        h.record("");
        assertEquals(0, h.size());
        assertNull(h.current());
    }

    @Test
    public void clear_resets() {
        DirHistory h = new DirHistory();
        h.record("/a");
        h.record("/b");
        h.clear();
        assertEquals(0, h.size());
        assertNull(h.current());
    }

    @Test
    public void canBack_canForward_initialState() {
        DirHistory h = new DirHistory();
        assertFalse(h.canBack());
        assertFalse(h.canForward());
    }
}
