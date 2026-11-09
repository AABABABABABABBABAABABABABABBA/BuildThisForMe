package com.mcpmc.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Log {
    private static final Logger L = LoggerFactory.getLogger("mcpmc");
    private Log() {}
    public static void info(String m) { L.info(m); }
    public static void warn(String m) { L.warn(m); }
    public static void error(String m) { L.error(m); }
    public static void error(String m, Throwable t) { L.error(m, t); }
    public static void debug(String m) { L.debug(m); }
}
