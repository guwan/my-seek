package top.knos.mySeek;

import org.springframework.core.env.Profiles;

public final class AppProfiles {
    public static final Profiles test = Profiles.of("test");
    public static final Profiles prod = Profiles.of("prod");
    public static final Profiles dev = Profiles.of("dev");
}
