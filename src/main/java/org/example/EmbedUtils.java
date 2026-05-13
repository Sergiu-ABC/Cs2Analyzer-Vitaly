package org.example;

import java.awt.Color;

public class EmbedUtils {

    public static String bar(double value, double max) {
        int filled = (int) Math.round((value / max) * 10);
        filled = Math.max(0, Math.min(10, filled));
        return "█".repeat(filled) + "░".repeat(10 - filled);
    }

    public static String dot(double value, double good, double great) {
        if (value >= great) return "🟢";
        if (value >= good) return "🟡";
        return "🔴";
    }

    public static String levelBadge(int level) {
        return switch (level) {
            case 10 -> "💎 Level 10";
            case 9  -> "🔴 Level 9";
            case 8  -> "🟠 Level 8";
            case 7  -> "🟡 Level 7";
            case 6  -> "🟢 Level 6";
            case 5  -> "🔵 Level 5";
            case 4  -> "🟣 Level 4";
            case 3  -> "⚪ Level 3";
            case 2  -> "⚫ Level 2";
            default -> "🩶 Level 1";
        };
    }

    public static Color levelColor(int level) {
        if (level == 10) return new Color(0, 230, 255);
        else if (level >= 8) return new Color(255, 60, 60);
        else if (level >= 6) return new Color(255, 140, 0);
        else if (level >= 4) return new Color(255, 220, 0);
        else return new Color(120, 120, 140);
    }

    public static String crown(double thisVal, double otherVal, boolean isPercentage) {
        String suffix = isPercentage ? "%" : "";
        if (thisVal > otherVal) return "👑 **" + thisVal + suffix + "**";
        if (thisVal == otherVal) return "🤝 **" + thisVal + suffix + "**";
        return thisVal + suffix;
    }

    public static String getBannerForRole(String roleResult) {
        String firstLine = roleResult.split("\n")[0].toUpperCase();

        if (firstLine.contains("AWP") || firstLine.contains("SNIPER"))
            return "https://media1.tenor.com/m/Yw_D4DqPqLAAAAAd/s1mple-awp.gif";

        if (firstLine.contains("ENTRY") || firstLine.contains("SPACE")
                || firstLine.contains("BLOOD") || firstLine.contains("CANNON"))
            return "https://media1.tenor.com/m/r-4jK-U57vUAAAAd/csgo-entry.gif";

        if (firstLine.contains("LURK") || firstLine.contains("CLUTCH")
                || firstLine.contains("RETAKE") || firstLine.contains("CLOSER"))
            return "https://media1.tenor.com/m/b_JGEB3B48AAAAAC/csgo-smoke.gif";

        if (firstLine.contains("SUPPORT") || firstLine.contains("UTILITY")
                || firstLine.contains("FLASH"))
            return "https://media1.tenor.com/m/Xf1l7uXzMuoAAAAd/csgo-flash.gif";

        return "https://media1.tenor.com/m/FwGv-2Vp4P8AAAAC/cs2-counter-strike-2.gif";
    }
}