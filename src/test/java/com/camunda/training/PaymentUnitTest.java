package com.camunda.training;

import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.*;
import static org.assertj.core.api.Assertions.*;

import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.test.Deployment;
import org.camunda.bpm.extension.process_test_coverage.junit5.ProcessEngineCoverageExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@Deployment(resources = {"payment.bpmn"})
@ExtendWith(ProcessEngineCoverageExtension.class)
public class PaymentUnitTest {

  @Test
  public void testHappyPath() {
    ProcessInstance processInstance = runtimeService()
      .startProcessInstanceByKey("PaymentProcess",
        withVariables("orderTotal", 45.99,
                      "customerID", "cust30",
                      "creditCardNumber", "1234 5678",
                      "expiryDate", "09/24",
                      "CVC", "123"));

    assertThat(processInstance).isWaitingAt(findId("Deduct credit"))
      .externalTask().hasTopicName("creditDeduction");
    complete(externalTask(), withVariables("customerCredit", 30,
      "openAmount", 15.99));

    assertThat(processInstance).isWaitingAt(findId("Charge credit card"))
      .externalTask().hasTopicName("creditCardCharging");
    complete(externalTask());

    assertThat(processInstance).isEnded().hasPassed(findId("Payment completed"))
      .variables().contains(entry("customerCredit", 30),
                            entry("openAmount", 15.99));
  }

  @Test
  public void testCreditCardFailurePath() {
    ProcessInstance processInstance = runtimeService().createProcessInstanceByKey("PaymentProcess")
      .startBeforeActivity(findId("Charge credit card"))
      .execute();

    assertThat(processInstance).isWaitingAt(findId("Charge credit card"));

    fetchAndLock("creditCardCharging", "junit-test-worker", 1);
    externalTaskService().handleBpmnError(externalTask().getId(), "junit-test-worker", "creditCardChargeError");

    assertThat(processInstance).isWaitingAt(findId("Payment failed"))
      .externalTask().hasTopicName("paymentCompletion");
  }
}
