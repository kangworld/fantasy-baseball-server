package com.kangworld.fantasybaseball.roster.service;

import java.time.LocalDate;

import com.kangworld.fantasybaseball.roster.service.dto.RosterResponse;
import com.kangworld.fantasybaseball.roster.service.dto.RosterSubmitRequest;

public interface RosterService {

    RosterResponse submit(long memberId, RosterSubmitRequest request);

    RosterResponse update(long memberId, RosterSubmitRequest request);

    RosterResponse find(long memberId, LocalDate date);

}
