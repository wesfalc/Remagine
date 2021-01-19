package com.wesfalc.remagine.domain;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors (fluent = true)
@ToString
@EqualsAndHashCode
public class Player {
     private String name;
     @EqualsAndHashCode.Exclude
     private int score = 0;
}
