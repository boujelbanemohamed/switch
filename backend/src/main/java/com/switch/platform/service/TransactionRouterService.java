package com.switch.platform.service;

import com.switch.platform.model.Participant;
import com.switch.platform.model.RoutingRule;
import com.switch.platform.model.Transaction;
import com.switch.platform.repository.ParticipantRepository;
import com.switch.platform.repository.RoutingRuleRepository;
import com.switch.platform.router.RoutingContext;
import com.switch.platform.router.RoutingEngine;
import com.switch.platform.router.RoutingResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionRouterService {

    private final RoutingEngine routingEngine;
    private final ParticipantRepository participantRepository;
    private final RoutingRuleRepository routingRuleRepository;

    public RoutingResult routeTransaction(String pan, String mti,
                                          String amount, String currencyCode,
                                          String merchantId, String terminalId,
                                          String protocol, String sourceCode) {
        String bin = pan.length() >= 6 ? pan.substring(0, 6) : pan;

        RoutingContext context = RoutingContext.builder()
                .pan(pan)
                .bin(bin)
                .amountStr(amount)
                .currencyCode(currencyCode)
                .mti(mti)
                .merchantId(merchantId)
                .terminalId(terminalId)
                .source(sourceCode)
                .protocol(protocol)
                .build();

        return routingEngine.route(context);
    }

    public RoutingResult routeTransaction(RoutingContext context) {
        return routingEngine.route(context);
    }
}
