package com.icbc.agent.aitool;

import com.icbc.agent.aitool.model.Order;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OrderTool {
    public List<Order> queryOrder(String customerId){
        Order order = new Order();
        order.setAmount(50000D);
        return List.of( order);
    }
}
