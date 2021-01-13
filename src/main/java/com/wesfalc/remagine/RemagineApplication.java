package com.wesfalc.remagine;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

import java.util.TimeZone;

@SpringBootApplication
public class RemagineApplication {

    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

        new SpringApplicationBuilder()
                .sources(RemagineApplication.class)
                .headless(true)
                .run(args);
    }
}

