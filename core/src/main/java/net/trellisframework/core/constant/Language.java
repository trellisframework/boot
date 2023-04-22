package net.trellisframework.core.constant;

public enum Language {
    EN,
    FA,
    HY,
    TR,
    RU,
    ZH,
    JA,
    AR,
    KU;

    public static Language of(String name) {
        for (Language p : values())
            if (p.name().equalsIgnoreCase(name)) return p;
        return EN;
    }
}
