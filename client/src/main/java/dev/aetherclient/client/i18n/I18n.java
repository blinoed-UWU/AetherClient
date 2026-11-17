package dev.aetherclient.client.i18n;

/**
 * Lightweight runtime localization helper for RU/EN switching without restarting.
 */
public final class I18n {
    public enum Lang {
        RU, EN
    }

    private static volatile Lang current = Lang.RU;

    private I18n() {}

    public static Lang getLang() {
        return current;
    }

    public static boolean isEn() {
        return current == Lang.EN;
    }

    public static void setLang(String code) {
        current = "EN".equalsIgnoreCase(code) ? Lang.EN : Lang.RU;
    }

    public static String tr(String ru, String en) {
        return current == Lang.EN && en != null && !en.isEmpty() ? en : ru;
    }
}
