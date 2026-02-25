package com.college.icrs.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Getter
@ConfigurationProperties(prefix = "icrs")
public class IcrsProperties {

    private final Cors cors = new Cors();
    private final Files files = new Files();

    @Setter
    @Getter
    public static class Cors {
        private List<String> allowedOrigins = new ArrayList<>();

    }

    @Setter
    @Getter
    public static class Files {
        private List<String> allowedMimeTypes = new ArrayList<>();

    }
}
