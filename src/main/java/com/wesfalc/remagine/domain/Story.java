package com.wesfalc.remagine.domain;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors (fluent = true)
public class Story {
    private boolean trueStory;
    private boolean liked;
}
