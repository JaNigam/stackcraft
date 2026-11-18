package dev.stackcraft.runner.domain;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Finds the class that holds {@code main}. Java requires a public top-level class to live in a file of the same
 * name, so we need the name before writing the file. Falls back to {@code Main}.
 */
public final class MainClassName {
    private static final Pattern PUBLIC_TYPE = Pattern.compile(
            "public\\s+(?:final\\s+|abstract\\s+)*(?:class|record|enum|interface)\\s+([A-Za-z_$][A-Za-z0-9_$]*)");
    private static final Pattern ANY_TYPE = Pattern.compile(
            "(?m)^\\s*(?:final\\s+)?(?:class|record)\\s+([A-Za-z_$][A-Za-z0-9_$]*)");

    private MainClassName() {}

    public static String of(String source) {
        Matcher m = PUBLIC_TYPE.matcher(source);
        if (m.find()) return m.group(1);
        m = ANY_TYPE.matcher(source);
        if (m.find()) return m.group(1);
        return "Main";
    }
}
