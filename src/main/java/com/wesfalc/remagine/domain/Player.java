package com.wesfalc.remagine.domain;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors (fluent = true)
@ToString
public class Player {
     private String name;
     private int score;
}
