package com.hazeroot.scriptrunner.core.fs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class FileSortTest {

    private static FileEntry dir(String name) {
        return new FileEntry(name, "/x/" + name, true, "drwxr-xr-x", 4096, null);
    }

    private static FileEntry file(String name, long size) {
        return new FileEntry(name, "/x/" + name, false, "-rw-rw-r--", size, null);
    }

    private static List<FileEntry> sample() {
        List<FileEntry> l = new ArrayList<>();
        l.add(file("b.sh", 100));
        l.add(dir("Zeta"));
        l.add(file("a.sh", 500));
        l.add(dir("alpha"));
        l.add(file("c.sh", 20));
        return l;
    }

    @Test
    public void nameMode_dirsFirstThenNameAscending() {
        List<FileEntry> l = sample();
        FileSort.sort(l, FileSort.Mode.NAME);
        // 目录优先，目录内忽略大小写升序
        assertEquals("alpha", l.get(0).getName());
        assertEquals("Zeta", l.get(1).getName());
        // 文件按名称
        assertEquals("a.sh", l.get(2).getName());
        assertEquals("b.sh", l.get(3).getName());
        assertEquals("c.sh", l.get(4).getName());
    }

    @Test
    public void sizeMode_dirsFirstThenSizeDescending() {
        List<FileEntry> l = sample();
        FileSort.sort(l, FileSort.Mode.SIZE);
        assertEquals("alpha", l.get(0).getName());
        assertEquals("Zeta", l.get(1).getName());
        // 大的在前
        assertEquals("a.sh", l.get(2).getName());
        assertEquals("b.sh", l.get(3).getName());
        assertEquals("c.sh", l.get(4).getName());
    }

    @Test
    public void nullMode_treatedAsName() {
        List<FileEntry> l = sample();
        FileSort.sort(l, null);
        assertEquals("a.sh", l.get(2).getName());
    }

    @Test
    public void emptyOrSingleList_noException() {
        List<FileEntry> empty = new ArrayList<>();
        FileSort.sort(empty, FileSort.Mode.SIZE);
        assertTrue(empty.isEmpty());

        List<FileEntry> one = new ArrayList<>();
        one.add(file("only.sh", 1));
        FileSort.sort(one, FileSort.Mode.NAME);
        assertEquals(1, one.size());
    }

    @Test
    public void nullElements_doNotCrash() {
        List<FileEntry> l = new ArrayList<>();
        l.add(null);
        l.add(file("a.sh", 1));
        l.add(null);
        FileSort.sort(l, FileSort.Mode.NAME);
        assertEquals(3, l.size());   // 不删元素，只是排序
    }

    @Test
    public void sameSize_fallsBackToName() {
        List<FileEntry> l = new ArrayList<>();
        l.add(file("z.sh", 100));
        l.add(file("a.sh", 100));
        FileSort.sort(l, FileSort.Mode.SIZE);
        assertEquals("a.sh", l.get(0).getName());
        assertEquals("z.sh", l.get(1).getName());
    }
}
