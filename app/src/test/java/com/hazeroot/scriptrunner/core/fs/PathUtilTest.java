package com.hazeroot.scriptrunner.core.fs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PathUtilTest {

    @Test
    public void parent_normal() {
        assertEquals("/data", PathUtil.parent("/data/adb"));
        assertEquals("/", PathUtil.parent("/data"));
        assertEquals("/", PathUtil.parent("/"));
    }

    @Test
    public void parent_trailingSlash() {
        assertEquals("/data", PathUtil.parent("/data/adb/"));
    }

    @Test
    public void parent_edgeCases() {
        assertEquals("/", PathUtil.parent(null));
        assertEquals("/", PathUtil.parent(""));
    }

    @Test
    public void fileName_normal() {
        assertEquals("adb", PathUtil.fileName("/data/adb"));
        assertEquals("run.sh", PathUtil.fileName("/data/adb/GJX/run.sh"));
    }

    @Test
    public void fileName_trailingSlash() {
        assertEquals("adb", PathUtil.fileName("/data/adb/"));
    }

    @Test
    public void fileName_noSlash() {
        assertEquals("run.sh", PathUtil.fileName("run.sh"));
    }

    @Test
    public void fileName_empty() {
        assertEquals("", PathUtil.fileName(null));
        assertEquals("", PathUtil.fileName(""));
    }

    @Test
    public void join_noDoubleSlash() {
        assertEquals("/data/adb/x", PathUtil.join("/data/adb", "x"));
        assertEquals("/data/adb/x", PathUtil.join("/data/adb/", "x"));
        assertEquals("/x", PathUtil.join("/", "x"));
    }

    @Test
    public void join_edgeCases() {
        assertEquals("/x", PathUtil.join("", "x"));
        assertEquals("/data/adb", PathUtil.join("/data/adb", ""));
        assertEquals("/data/adb", PathUtil.join("/data/adb", null));
    }

    @Test
    public void isRoot_works() {
        assertTrue(PathUtil.isRoot("/"));
        assertFalse(PathUtil.isRoot("/data"));
        assertFalse(PathUtil.isRoot(null));
    }

    @Test
    public void extension_works() {
        assertEquals("sh", PathUtil.extension("run.sh"));
        assertEquals("zip", PathUtil.extension("a.ZIP"));
        assertEquals("", PathUtil.extension("noext"));
        assertEquals("", PathUtil.extension("a."));
        assertEquals("", PathUtil.extension(null));
        // 点文件（整名就是扩展名）没有"扩展名"概念，必须返回空
        assertEquals("", PathUtil.extension(".gitignore"));
        assertEquals("", PathUtil.extension(".bashrc"));
        // 中间带点的正常文件仍取最后一段
        assertEquals("gz", PathUtil.extension("a.tar.gz"));
    }
}
