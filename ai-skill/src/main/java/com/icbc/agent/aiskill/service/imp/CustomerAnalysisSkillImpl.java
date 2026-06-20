package com.icbc.agent.aiskill.service.imp;

import com.icbc.agent.aimodel.AiModelService;
import com.icbc.agent.aiskill.service.CustomerAnalysisSkill;
import com.icbc.agent.aitool.model.Customer;
import com.icbc.agent.aitool.model.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.alibaba.fastjson2.JSON;
import reactor.core.publisher.Flux;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerAnalysisSkillImpl implements CustomerAnalysisSkill {

    private final AiModelService modelService;
    @Override
    public String execute(Customer customer, List<Order> orders) {

        String prompt = "你是CRM客户分析专家，客户信息： %s  订单信息i: %s 请分析客户风险，并给出建议"
                .formatted(JSON.toJSONString(customer), JSON.toJSONString(orders));
        return modelService.chat(prompt);
    }


    @Override
    public Flux<Object> executeStream(Customer customer, List<Order> orders) {
        String prompt = "你是CRM客户分析专家，客户信息： %s  订单信息i: %s 请分析客户风险，并给出建议"
                .formatted(JSON.toJSONString(customer), JSON.toJSONString(orders));
        return modelService.streamChat(prompt);
    }
}
