package com.hazeroot.scriptrunner.core.fs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class FileFilterTest {

    private static FileEntry dir(String name) {
        return new FileEntry(name, "/x/" + name, true, "drwxr-xr-x", 4096, null);
    }

    private static FileEntry file(String name) {
        return new FileEntry(name, "/x/" + name, false, "-rw-rw-r--", 100, null);
    }

    private static List<FileEntry> sample() {
        List<FileEntry> l = new ArrayList<>();
        l.add(dir("GJX"));
        l.add(dir(".hidden_dir"));
        l.add(file("run.sh"));
        l.add(file("new.sh"));
        l.add(file("notes.txt"));
        l.add(file(".nomedia"));
        return l;
    }

    @Test
    public void showHiddenTrue_keepsDotFiles() {
        List<FileEntry> out = FileFilter.apply(sample(), true, false);
        assertEquals(6, out.size());
    }

    @Test
    public void showHiddenFalse_dropsDotFiles() {
        List<FileEntry> out = FileFilter.apply(sample(), false, false);
        assertEquals(4, out.size());
        for (FileEntry e : out) {
            assertFalse(e.getName().startsWith("."));
        }
    }

    @Test
    public void scriptsOnly_keepsOnlyShFiles() {
        List<FileEntry> out = FileFilter.apply(sample(), true, true);
        assertEquals(2, out.size());
        assertEquals("run.sh", out.get(0).getName());
        assertEquals("new.sh", out.get(1).getName());
    }

    @Test
    public void scriptsOnly_dropsDirectories() {
        List<FileEntry> out = FileFilter.apply(sample(), true, true);
        for (FileEntry e : out) {
            assertFalse(e.isDir());
        }
    }

    @Test
    public void scriptsOnlyPlusHiddenFalse_combined() {
        List<FileEntry> out = FileFilter.apply(sample(), false, true);
        assertEquals(2, out.size());   // .nomedia 不是 sh 也不显示
    }

    @Test
    public void nullList_returnsEmpty() {
        assertTrue(FileFilter.apply(null, true, false).isEmpty());
    }

    @Test
    public void nullElements_skipped() {
        List<FileEntry> l = new ArrayList<>();
        l.add(null);
        l.add(file("a.sh"));
        assertEquals(1, FileFilter.apply(l, true, false).size());
    }

    @Test
    public void isShellScript_variants() {
        assertTrue(FileFilter.isShellScript("a.sh"));
        assertTrue(FileFilter.isShellScript("a.SH"));
        assertTrue(FileFilter.isShellScript("a.bash"));
        assertFalse(FileFilter.isShellScript("a.txt"));
        assertFalse(FileFilter.isShellScript("a.zip"));
        assertFalse(FileFilter.isShellScript("noext"));
        assertFalse(FileFilter.isShellScript(null));
    }
}
