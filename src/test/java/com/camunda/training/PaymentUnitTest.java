package com.camunda.training;

import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.*;
import static org.assertj.core.api.Assertions.*;

import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.test.Deployment;
import org.camunda.bpm.extension.process_test_coverage.junit5.ProcessEngineCoverageExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@Deployment(resources = { "payment.bpmn" })
@ExtendWith(ProcessEngineCoverageExtension.class)
public class PaymentUnitTest {
  
  @Test
  public void testHappyPath() {
    ProcessInstance processInstance = runtimeService()
      .startProcessInstanceByKey("PaymentProcess",
        withVariables("orderTotal", 45.99, "customerCredit", 30.00));

    assertThat(processInstance).isWaitingAt(findId("Deduct credit"))
      .externalTask().hasTopicName("creditDeduction");
    complete(externalTask());

    assertThat(processInstance).isWaitingAt(findId("Charge credit card"))
      .externalTask().hasTopicName("creditCardCharging");
    complete(externalTask());

    assertThat(processInstance).isEnded().hasPassed(findId("Payment completed"));
  }
}
