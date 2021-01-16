package com.wesfalc.remagine.domain;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors (fluent = true)
public class Round {
    private String topicSetter;
    private String topic = "";
    private int roundNumber;
}
