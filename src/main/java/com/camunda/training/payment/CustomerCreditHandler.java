package com.camunda.training.payment;

import com.camunda.training.services.CustomerService;
import java.util.HashMap;
import java.util.Map;
import org.camunda.bpm.client.spring.annotation.ExternalTaskSubscription;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskHandler;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@ExternalTaskSubscription("creditDeduction")
public class CustomerCreditHandler implements ExternalTaskHandler {

  private static final Logger LOG = LoggerFactory.getLogger(CustomerCreditHandler.class);

  private CustomerService service;

  @Autowired
  public CustomerCreditHandler(CustomerService service) {
    this.service = service;
  }

  @Override
  public void execute(ExternalTask externalTask, ExternalTaskService externalTaskService) {
    LOG.info("handle topic {} for task id {}", externalTask.getTopicName(), externalTask.getId());

    String customerId = externalTask.getVariable("customerId");
    Double orderTotal = externalTask.getVariable("orderTotal");

    Double openAmount = service.deductCredit(customerId, orderTotal);
    Double customerCredit = service.getCustomerCredit(customerId);

    Map<String,Object> variables = new HashMap<>();
    variables.put("openAmount", openAmount);
    variables.put("customerCredit", customerCredit);


    externalTaskService.complete(externalTask, variables);
  }
}
