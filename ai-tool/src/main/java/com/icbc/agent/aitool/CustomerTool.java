package com.icbc.agent.aitool;

import com.icbc.agent.aitool.model.Customer;
import org.springframework.stereotype.Component;


@Component
public class CustomerTool {

    public Customer queryCustomer(String customerId){
        Customer customer = new Customer();
        customer.setCustomerName("张三");
        customer.setLevel("VIP");
        return customer;
    }
}
