package com.kangworld.fantasybaseball.point.model;

public sealed interface PointRecordCode permits BatterPointRecordCode, PitcherPointRecordCode {

    String name();

}
