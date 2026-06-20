package com.icbc.agent.aiskill.service;

import com.icbc.agent.aitool.model.Customer;
import com.icbc.agent.aitool.model.Order;
import reactor.core.publisher.Flux;

import java.util.List;

public interface CustomerAnalysisSkill {

    String execute(Customer customer, List<Order> orders);

    Flux<Object> executeStream(Customer customer, List<Order> orders);
}
