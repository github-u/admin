package com.platform.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;

public final class StackTraceUtil {

    public static String stackTrace(Throwable t) {
        if (t == null) {
            return "null";
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(out);
        t.printStackTrace(ps);
        ps.flush();
        try {
            return new String(out.toByteArray());
        } finally {
            try {
                out.close();
            } catch (IOException ignored) {}
        }
    }

    private StackTraceUtil() {}
    
    public static void main(String[] args) {
        System.out.println(stackTrace(new RuntimeException()));
    }
}
