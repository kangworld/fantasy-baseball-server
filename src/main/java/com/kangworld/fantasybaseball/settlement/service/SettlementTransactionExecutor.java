package com.kangworld.fantasybaseball.settlement.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SettlementTransactionExecutor {

    @Transactional
    public void execute(Runnable databaseWork) {
        databaseWork.run();
    }

}
