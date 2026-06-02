package com.mes.application.command.typesetting.proces.liubai;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LiubaiProcessService {
    private final List<AbstractLiubaiProcessStrategy> strategies;

    public LiubaiProcessService(List<AbstractLiubaiProcessStrategy> strategies) {
        this.strategies = strategies;
    }

    public void process(LiubaiProcessContext context) {
        if (strategies == null || strategies.isEmpty()) {
            return;
        }
        for (AbstractLiubaiProcessStrategy strategy : strategies) {
            if (strategy.matches(context)) {
                strategy.process(context);
                return;
            }
        }
    }
}
