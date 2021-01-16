package com.wesfalc.remagine.domain;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors (fluent = true)
public class Score {
    private String player;
    private int score;
}
