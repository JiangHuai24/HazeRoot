package com.hazeroot.scriptrunner.core.fs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class SizeFormatTest {

    @Test
    public void bytes_below1K() {
        assertEquals("0B", SizeFormat.format(0));
        assertEquals("257B", SizeFormat.format(257));
        assertEquals("1023B", SizeFormat.format(1023));
    }

    @Test
    public void kilobytes() {
        assertEquals("1.0K", SizeFormat.format(1024));
        assertEquals("3.4K", SizeFormat.format(3481));      // 3481/1024 = 3.3998
        // 1048575/1024 = 1023.9990，%.1f 四舍五入进位成 1024.0K
        assertEquals("1024.0K", SizeFormat.format(1048575));
    }

    @Test
    public void megabytes() {
        assertEquals("1.0M", SizeFormat.format(1048576));
        assertEquals("77.8M", SizeFormat.format(81579212L)); // 实测 base.apk1
        assertEquals("41.8M", SizeFormat.format(43830476L));
    }

    /**
     * 原实现没有 GB 档，超过 1024MB 仍然显示 M。
     * 这里锁住这个行为，防止重构时"顺手优化"改变了用户看到的数字。
     */
    @Test
    public void above1G_stillShownAsM() {
        assertEquals("2048.0M", SizeFormat.format(2147483648L));
    }
}
