package com.example.imagehostingservice.sharing.model;

import java.time.Duration;

public enum ShareExpiration {

    ONE_HOUR(Duration.ofHours(1)),
    ONE_DAY(Duration.ofDays(1)),
    SEVEN_DAYS(Duration.ofDays(7));

    private final Duration duration;

    ShareExpiration(Duration duration) {
        this.duration = duration;
    }

    public Duration duration() {
        return duration;
    }
}
