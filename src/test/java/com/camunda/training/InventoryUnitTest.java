package com.camunda.training;

import static org.camunda.bpm.engine.test.assertions.ProcessEngineTests.*;
import static org.assertj.core.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.test.Deployment;
import org.camunda.bpm.extension.process_test_coverage.junit5.ProcessEngineCoverageExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@Deployment(resources = { "inventory-process.bpmn" })
@ExtendWith(ProcessEngineCoverageExtension.class)
public class InventoryUnitTest {
  public static final String INVENTORY_PROCESS_ID = "InventoryProcess";

  public static final String CHECK_AVAILABILITY_TASK = "Check availability";
  public static final String RESERVE_AVAILABLE_ITEMS_TASK = "Reserve available items";
  public static final String REMOVE_UNAVAILABLE_ITEMS_TASK = "Remove unavailable items";
  public static final String INVENTORY_COMPLETED_EVENT = "Inventory completed";

  public static final String AVAILABILITY_CHECK_TOPIC = "availabilityCheck";
  public static final String ITEM_REMOVAL_TOPIC = "itemRemoval";
  public static final String ITEM_RESERVATION_TOPIC = "itemReservation";

  public static final String ORDER_ITEMS_NUM_VAR = "orderItemsNum";
  public static final String AVAILABLE_ITEMS_NUM_VAR = "availableItemsNum";

  @Test
  public void testHappyPath() {
    Map<String, Object> variables = new HashMap<>();
    variables.put(ORDER_ITEMS_NUM_VAR, 42);
    variables.put(AVAILABLE_ITEMS_NUM_VAR, 42);

    ProcessInstance processInstance = runtimeService()
      .startProcessInstanceByKey(INVENTORY_PROCESS_ID, variables);

    assertThat(processInstance).isStarted();

    assertThat(processInstance).isWaitingAt(findId(CHECK_AVAILABILITY_TASK))
      .externalTask().hasTopicName(AVAILABILITY_CHECK_TOPIC);
    complete(externalTask());

    assertThat(processInstance).isWaitingAt(findId(RESERVE_AVAILABLE_ITEMS_TASK))
      .externalTask().hasTopicName(ITEM_RESERVATION_TOPIC);
    complete(externalTask());

    assertThat(processInstance).isEnded()
      .hasPassed(findId(INVENTORY_COMPLETED_EVENT));
  }

  @Test
  public void testRemoveUnavailableItemsPath() {
    Map<String, Object> variables = new HashMap<>();
    variables.put(ORDER_ITEMS_NUM_VAR, 50);
    variables.put(AVAILABLE_ITEMS_NUM_VAR, 42);

    ProcessInstance processInstance = runtimeService()
      .createProcessInstanceByKey(INVENTORY_PROCESS_ID).setVariables(variables)
      .startAfterActivity(findId(CHECK_AVAILABILITY_TASK))
      .execute();

    assertThat(processInstance).isStarted();

    assertThat(processInstance).isWaitingAt(findId(REMOVE_UNAVAILABLE_ITEMS_TASK))
      .externalTask().hasTopicName(ITEM_REMOVAL_TOPIC);
    complete(externalTask());
  }
}
