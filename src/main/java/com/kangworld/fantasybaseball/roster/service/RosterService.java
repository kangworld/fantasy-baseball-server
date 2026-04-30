package com.kangworld.fantasybaseball.roster.service;

import java.time.LocalDate;
import java.util.List;

import com.kangworld.fantasybaseball.game.service.dto.PlayerRecordResponse;
import com.kangworld.fantasybaseball.roster.service.dto.RosterResponse;
import com.kangworld.fantasybaseball.roster.service.dto.RosterSubmitRequest;

public interface RosterService {

    RosterResponse submit(long memberId, RosterSubmitRequest request);

    RosterResponse update(long memberId, RosterSubmitRequest request);

    RosterResponse find(long memberId, LocalDate date);

    List<PlayerRecordResponse> findMyPlayerRecords(long memberId);

}
