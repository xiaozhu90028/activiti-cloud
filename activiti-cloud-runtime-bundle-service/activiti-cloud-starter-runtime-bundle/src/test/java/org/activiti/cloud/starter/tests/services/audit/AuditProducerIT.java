package org.activiti.cloud.starter.tests.services.audit;

import static org.activiti.api.model.shared.event.VariableEvent.VariableEvents.VARIABLE_CREATED;
import static org.activiti.api.model.shared.event.VariableEvent.VariableEvents.VARIABLE_UPDATED;
import static org.activiti.api.process.model.events.BPMNActivityEvent.ActivityEvents.ACTIVITY_CANCELLED;
import static org.activiti.api.process.model.events.BPMNActivityEvent.ActivityEvents.ACTIVITY_COMPLETED;
import static org.activiti.api.process.model.events.BPMNActivityEvent.ActivityEvents.ACTIVITY_STARTED;
import static org.activiti.api.process.model.events.IntegrationEvent.IntegrationEvents.INTEGRATION_ERROR_RECEIVED;
import static org.activiti.api.process.model.events.IntegrationEvent.IntegrationEvents.INTEGRATION_REQUESTED;
import static org.activiti.api.process.model.events.IntegrationEvent.IntegrationEvents.INTEGRATION_RESULT_RECEIVED;
import static org.activiti.api.process.model.events.ProcessDefinitionEvent.ProcessDefinitionEvents.PROCESS_DEPLOYED;
import static org.activiti.api.process.model.events.ProcessRuntimeEvent.ProcessEvents.PROCESS_CANCELLED;
import static org.activiti.api.process.model.events.ProcessRuntimeEvent.ProcessEvents.PROCESS_COMPLETED;
import static org.activiti.api.process.model.events.ProcessRuntimeEvent.ProcessEvents.PROCESS_CREATED;
import static org.activiti.api.process.model.events.ProcessRuntimeEvent.ProcessEvents.PROCESS_RESUMED;
import static org.activiti.api.process.model.events.ProcessRuntimeEvent.ProcessEvents.PROCESS_STARTED;
import static org.activiti.api.process.model.events.ProcessRuntimeEvent.ProcessEvents.PROCESS_SUSPENDED;
import static org.activiti.api.process.model.events.ProcessRuntimeEvent.ProcessEvents.PROCESS_UPDATED;
import static org.activiti.api.process.model.events.SequenceFlowEvent.SequenceFlowEvents.SEQUENCE_FLOW_TAKEN;
import static org.activiti.api.task.model.events.TaskCandidateGroupEvent.TaskCandidateGroupEvents.TASK_CANDIDATE_GROUP_ADDED;
import static org.activiti.api.task.model.events.TaskCandidateGroupEvent.TaskCandidateGroupEvents.TASK_CANDIDATE_GROUP_REMOVED;
import static org.activiti.api.task.model.events.TaskCandidateUserEvent.TaskCandidateUserEvents.TASK_CANDIDATE_USER_ADDED;
import static org.activiti.api.task.model.events.TaskCandidateUserEvent.TaskCandidateUserEvents.TASK_CANDIDATE_USER_REMOVED;
import static org.activiti.api.task.model.events.TaskRuntimeEvent.TaskEvents.TASK_ACTIVATED;
import static org.activiti.api.task.model.events.TaskRuntimeEvent.TaskEvents.TASK_ASSIGNED;
import static org.activiti.api.task.model.events.TaskRuntimeEvent.TaskEvents.TASK_CANCELLED;
import static org.activiti.api.task.model.events.TaskRuntimeEvent.TaskEvents.TASK_COMPLETED;
import static org.activiti.api.task.model.events.TaskRuntimeEvent.TaskEvents.TASK_CREATED;
import static org.activiti.api.task.model.events.TaskRuntimeEvent.TaskEvents.TASK_SUSPENDED;
import static org.activiti.api.task.model.events.TaskRuntimeEvent.TaskEvents.TASK_UPDATED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.awaitility.Awaitility.await;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.activiti.api.model.shared.event.RuntimeEvent;
import org.activiti.api.model.shared.model.ApplicationElement;
import org.activiti.api.process.model.BPMNActivity;
import org.activiti.api.process.model.IntegrationContext;
import org.activiti.api.process.model.ProcessInstance;
import org.activiti.api.process.model.builders.ProcessPayloadBuilder;
import org.activiti.api.process.model.builders.StartProcessPayloadBuilder;
import org.activiti.api.process.model.events.BPMNActivityEvent;
import org.activiti.api.runtime.model.impl.ApplicationElementImpl;
import org.activiti.api.task.model.Task;
import org.activiti.api.task.model.TaskCandidateGroup;
import org.activiti.api.task.model.TaskCandidateUser;
import org.activiti.api.task.model.builders.TaskPayloadBuilder;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.process.model.CloudProcessDefinition;
import org.activiti.cloud.api.process.model.CloudProcessInstance;
import org.activiti.cloud.api.process.model.events.CloudBPMNActivityCompletedEvent;
import org.activiti.cloud.api.process.model.events.CloudBPMNActivityStartedEvent;
import org.activiti.cloud.api.process.model.events.CloudIntegrationErrorReceivedEvent;
import org.activiti.cloud.api.process.model.events.CloudIntegrationRequestedEvent;
import org.activiti.cloud.api.process.model.events.CloudIntegrationResultReceivedEvent;
import org.activiti.cloud.api.process.model.events.CloudProcessDeployedEvent;
import org.activiti.cloud.api.process.model.impl.CandidateGroup;
import org.activiti.cloud.api.process.model.impl.CandidateUser;
import org.activiti.cloud.api.process.model.impl.IntegrationErrorImpl;
import org.activiti.cloud.api.process.model.impl.IntegrationRequestImpl;
import org.activiti.cloud.api.process.model.impl.IntegrationResultImpl;
import org.activiti.cloud.api.task.model.CloudTask;
import org.activiti.cloud.api.task.model.events.CloudTaskCancelledEvent;
import org.activiti.cloud.api.task.model.events.CloudTaskCandidateUserRemovedEvent;
import org.activiti.cloud.api.task.model.events.CloudTaskCreatedEvent;
import org.activiti.cloud.starter.tests.helper.ProcessInstanceRestTemplate;
import org.activiti.cloud.starter.tests.helper.TaskRestTemplate;
import org.activiti.engine.RuntimeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.cloud.stream.binding.BinderAwareChannelResolver;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.Resources;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

@ActiveProfiles(AuditProducerIT.AUDIT_PRODUCER_IT)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource("classpath:application-test.properties")
@DirtiesContext
@ContextConfiguration(classes = ServicesAuditITConfiguration.class)
public class AuditProducerIT {

    private static final String SIMPLE_SUB_PROCESS1 = "simpleSubProcess1";
    private static final String SIMPLE_SUB_PROCESS2 = "simpleSubProcess2";
    private static final String CALL_TWO_SUB_PROCESSES = "callTwoSubProcesses";

    public static final String ROUTING_KEY_HEADER = "routingKey";
    public static final String[] RUNTIME_BUNDLE_INFO_HEADERS = {"appName", "serviceName", "serviceVersion", "serviceFullName", ROUTING_KEY_HEADER};
    public static final String[] ALL_REQUIRED_HEADERS = Stream.of(RUNTIME_BUNDLE_INFO_HEADERS)
            .flatMap(Stream::of)
            .toArray(String[]::new);

    public static final String AUDIT_PRODUCER_IT = "AuditProducerIT";
    private static final String SIMPLE_PROCESS = "SimpleProcess";
    private static final String PROCESS_DEFINITIONS_URL = "/v1/process-definitions/";

    @Value("${activiti.keycloak.test-user}")
    protected String keycloakTestUser;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ProcessInstanceRestTemplate processInstanceRestTemplate;

    @Autowired
    private TaskRestTemplate taskRestTemplate;

    @Autowired
    private AuditConsumerStreamHandler streamHandler;

    @Autowired
    private BinderAwareChannelResolver channelResolver;

    @Value("integrationResult_${spring.application.name}")
    private String integrationResultDestination;

    @Value("integrationError_${spring.application.name}")
    private String integrationErrorDestination;

    private Map<String, String> processDefinitionIds = new HashMap<>();

    @Autowired
    private RuntimeService runtimeService;

    @BeforeEach
    public void setUp() {
        ResponseEntity<PagedResources<CloudProcessDefinition>> processDefinitions = getProcessDefinitions();
        assertThat(processDefinitions.getStatusCode()).isEqualTo(HttpStatus.OK);

        assertThat(processDefinitions.getBody()).isNotNull();
        assertThat(processDefinitions.getBody().getContent()).isNotNull();
        for (CloudProcessDefinition pd : processDefinitions.getBody().getContent()) {
            processDefinitionIds.put(pd.getName(),
                    pd.getId());
        }
    }

    @Test
    public void shouldProduceEventsForProcessDeployment() {
        //when
        List<CloudRuntimeEvent<?, ?>> receivedEvents = streamHandler.getAllReceivedEvents();

        assertThat(streamHandler.getReceivedHeaders()).containsKeys(RUNTIME_BUNDLE_INFO_HEADERS);

        //then
        List<CloudProcessDeployedEvent> processDeployedEvents = receivedEvents
                .stream()
                .filter(event -> PROCESS_DEPLOYED.name().equals(event.getEventType().name()))
                .map(CloudProcessDeployedEvent.class::cast)
                .collect(Collectors.toList());
        assertThat(processDeployedEvents)
                .extracting(event -> event.getEntity().getKey())
                .contains(SIMPLE_PROCESS);

        CloudProcessDeployedEvent processDeployedEvent = processDeployedEvents.stream().filter(event -> SIMPLE_PROCESS.equals(event.getEntity().getKey()))
                .findFirst().orElse(null);
        assertThat(processDeployedEvent).isNotNull();
        assertThat(processDeployedEvent.getProcessModelContent())
                .isXmlEqualToContentOf(new File("src/test/resources/processes/SimpleProcess.bpmn20.xml"));
    }

    @Test
    public void shouldProduceEventsDuringSimpleProcessExecution() {

        //when
        ResponseEntity<CloudProcessInstance> startProcessEntity = processInstanceRestTemplate.startProcess(ProcessPayloadBuilder
                .start()
                .withProcessDefinitionKey(SIMPLE_PROCESS)
                .withProcessDefinitionId(processDefinitionIds.get(SIMPLE_PROCESS))
                .withVariable("name",
                        "peter")
                .withName("my instance name")
                .withBusinessKey("my business key")
                .build());

        //then
        await().untilAsserted(() -> {
            List<CloudRuntimeEvent<?, ?>> receivedEvents = streamHandler.getLatestReceivedEvents();

            assertThat(streamHandler.getReceivedHeaders()).containsKeys(ALL_REQUIRED_HEADERS);

            assertThat(receivedEvents)
                    .extracting(event -> event.getEventType().name())
                    .containsExactly(PROCESS_CREATED.name(),
                            VARIABLE_CREATED.name(),
                            PROCESS_UPDATED.name(),
                            PROCESS_STARTED.name(),
                            ACTIVITY_STARTED.name()/*start event*/,
                            BPMNActivityEvent.ActivityEvents.ACTIVITY_COMPLETED.name()/*start event*/,
                            SEQUENCE_FLOW_TAKEN.name(),
                            ACTIVITY_STARTED.name()/*user task*/,
                            VARIABLE_CREATED.name(), /*task variable copy of proc var*/
                            TASK_CANDIDATE_GROUP_ADDED.name(),
                            TASK_CANDIDATE_USER_ADDED.name(),
                            TASK_CREATED.name());
            assertThat(receivedEvents)
                    .filteredOn(event -> ACTIVITY_STARTED.equals(event.getEventType()))
                    .extracting(event -> ((CloudBPMNActivityStartedEvent) event).getEntity().getActivityType())
                    .containsExactly("startEvent",
                            "userTask");
            assertThat(receivedEvents).filteredOn(cloudRuntimeEvent -> PROCESS_CREATED.equals(cloudRuntimeEvent.getEventType()))
                    .extracting(cloudRuntimeEvent -> ((ProcessInstance) cloudRuntimeEvent.getEntity()).getBusinessKey())
                    .containsExactly("my business key");
            assertThat(receivedEvents).filteredOn(cloudRuntimeEvent -> PROCESS_STARTED.equals(cloudRuntimeEvent.getEventType()))
                    .extracting(cloudRuntimeEvent -> ((ProcessInstance) cloudRuntimeEvent.getEntity()).getName())
                    .containsExactly("my instance name");
            assertThat(receivedEvents)
                    .filteredOn(event -> TASK_CREATED.equals(event.getEventType()))
                    .extracting(event -> event.getProcessDefinitionVersion(),
                                event -> event.getBusinessKey(),
                                event -> ((CloudTaskCreatedEvent) event).getEntity().getTaskDefinitionKey(),
                                event -> ((CloudTaskCreatedEvent) event).getEntity().getFormKey())
                    .containsExactly(tuple(startProcessEntity.getBody().getProcessDefinitionVersion(),
                                           startProcessEntity.getBody().getBusinessKey(),
                                           "sid-CDFE7219-4627-43E9-8CA8-866CC38EBA94",
                                           "taskFormKey"));
        });

        //when
        processInstanceRestTemplate.suspend(startProcessEntity);

        //then
        await().untilAsserted(() -> {
            List<CloudRuntimeEvent<?, ?>> receivedEvents1 = streamHandler.getLatestReceivedEvents();
            assertThat(streamHandler.getReceivedHeaders()).containsKeys(ALL_REQUIRED_HEADERS);

            assertThat(receivedEvents1)
                    .extracting(event -> event.getEventType().name())
                    .containsExactly(PROCESS_SUSPENDED.name(),
                            TASK_SUSPENDED.name());

            assertThat(receivedEvents1.get(0).getEntity()).isInstanceOf(ProcessInstance.class);
            assertThat(receivedEvents1.get(0).getProcessDefinitionKey()).isEqualTo(SIMPLE_PROCESS);
        });

        //when
        processInstanceRestTemplate.resume(startProcessEntity);

        //then
        await().untilAsserted(() -> {
            List<CloudRuntimeEvent<?, ?>> receivedEvents2 = streamHandler.getLatestReceivedEvents();
            assertThat(streamHandler.getReceivedHeaders()).containsKeys(ALL_REQUIRED_HEADERS);

            assertThat(receivedEvents2)
                    .extracting(event -> event.getEventType().name())
                    .containsExactly(PROCESS_RESUMED.name(),
                            TASK_ACTIVATED.name());

            assertThat(receivedEvents2.get(0).getEntity()).isInstanceOf(ProcessInstance.class);
            assertThat(receivedEvents2.get(0).getProcessDefinitionKey()).isEqualTo(SIMPLE_PROCESS);
        });

        //when
        processInstanceRestTemplate.setVariables(startProcessEntity.getBody().getId(),
                Collections.singletonMap("name",
                        "paul"));

        //then
        await().untilAsserted(() -> {
            assertThat(streamHandler.getReceivedHeaders()).containsKeys(ALL_REQUIRED_HEADERS);

            assertThat(streamHandler.getLatestReceivedEvents())
                    .extracting(event -> event.getEventType().name())
                    .containsExactly(VARIABLE_UPDATED.name());
        });

        //given
        ResponseEntity<PagedResources<CloudTask>> tasks = processInstanceRestTemplate.getTasks(startProcessEntity);
        Task task = tasks.getBody().iterator().next();

        //when
        taskRestTemplate.claim(task);
        await().untilAsserted(() -> {
            assertThat(streamHandler.getReceivedHeaders()).containsKeys(ALL_REQUIRED_HEADERS);

            assertThat(streamHandler.getLatestReceivedEvents())
                    .extracting(event -> event.getEventType().name())
                    .containsExactly(TASK_ASSIGNED.name(),
                            TASK_UPDATED.name()
                    );
        });

        //when
        taskRestTemplate.complete(task);

        //then
        await().untilAsserted(() -> {
            assertThat(streamHandler.getReceivedHeaders()).containsKeys(ALL_REQUIRED_HEADERS);

            assertThat(streamHandler.getLatestReceivedEvents())
                    .extracting(event -> event.getEventType().name())
                    .containsExactly(TASK_COMPLETED.name(),
                            TASK_CANDIDATE_GROUP_REMOVED.name(),
                            TASK_CANDIDATE_USER_REMOVED.name(),
                            VARIABLE_UPDATED.name(),/*task local var copied back to proc var*/
                            BPMNActivityEvent.ActivityEvents.ACTIVITY_COMPLETED.name()/*user task*/,
                            SEQUENCE_FLOW_TAKEN.name(),
                            ACTIVITY_STARTED.name()/*end event*/,
                            BPMNActivityEvent.ActivityEvents.ACTIVITY_COMPLETED.name()/*end event*/,
                            PROCESS_COMPLETED.name());
        });

        assertThat(streamHandler.getLatestReceivedEvents())
                .filteredOn(event -> event.getEventType().equals(TASK_COMPLETED))
                .extracting(event -> ((Task) event.getEntity()).getStatus())
                .containsOnly(Task.TaskStatus.COMPLETED);
    }

    @Test
    public void shouldProduceEventsForAProcessDeletion() {
        //given
        ResponseEntity<CloudProcessInstance> startProcessEntity = processInstanceRestTemplate.startProcess(new StartProcessPayloadBuilder()
                .withProcessDefinitionId(processDefinitionIds.get(SIMPLE_PROCESS))
                .withName("processInstanceName")
                .withBusinessKey("businessKey")
                .withVariables(Collections.emptyMap())
                .build());

        //when
        processInstanceRestTemplate.delete(startProcessEntity);

        //then
        await().untilAsserted(() -> {
            List<CloudRuntimeEvent<?, ?>> receivedEvents = streamHandler.getLatestReceivedEvents();

            assertThat(streamHandler.getReceivedHeaders()).containsKeys(ALL_REQUIRED_HEADERS);

            assertThat(receivedEvents)
                    .extracting(event -> event.getEventType().name())
                    .containsExactly(ACTIVITY_CANCELLED.name(),
                            TASK_CANDIDATE_GROUP_REMOVED.name(),
                            TASK_CANDIDATE_USER_REMOVED.name(),
                            TASK_CANCELLED.name(),
                            PROCESS_CANCELLED.name());
        });
    }

    @Test
    public void shouldSendIntegrationResultReceiveEvent() {
        //given
        ResponseEntity<CloudProcessInstance> processInstanceResponseEntity = processInstanceRestTemplate.startProcess(
            ProcessPayloadBuilder.start()
                .withProcessDefinitionKey("connectorConstants")
                .withBusinessKey("businessKey")
                .build());

        await().untilAsserted(() -> {
            //when
            List<CloudRuntimeEvent<?, ?>> receivedEvents = streamHandler.getAllReceivedEvents();

            assertThat(receivedEvents)
                .extracting(event -> event.getEventType().name())
                .contains(INTEGRATION_RESULT_RECEIVED.name())
                .contains(INTEGRATION_REQUESTED.name());
        });
    }


    @Test
    public void shouldProduceEventsForAProcessUpdate() {
        //given
        ResponseEntity<CloudProcessInstance> startProcessEntity = processInstanceRestTemplate.startProcess(processDefinitionIds.get(SIMPLE_PROCESS));

        //when
        processInstanceRestTemplate.update(startProcessEntity,
                "businessKey",
                "name");

        //then
        await().untilAsserted(() -> {
            List<CloudRuntimeEvent<?, ?>> receivedEvents = streamHandler.getLatestReceivedEvents();

            assertThat(streamHandler.getReceivedHeaders()).containsKeys(ALL_REQUIRED_HEADERS);

            assertThat(receivedEvents)
                    .extracting(event -> event.getEventType().name())
                    .containsExactly(PROCESS_UPDATED.name());

            assertThat(receivedEvents)
                    .extracting(event -> event.getEntity())
                    .extracting(ProcessInstance.class::cast)
                    .extracting(event -> event.getName())
                    .containsExactly("name");

            assertThat(receivedEvents)
                    .extracting(event -> event.getEntity())
                    .extracting(ProcessInstance.class::cast)
                    .extracting(entity -> entity.getBusinessKey())
                    .containsExactly("businessKey");
        });

        // Clean up
        runtimeService.deleteProcessInstance(startProcessEntity.getBody().getId(), "Clean up");

    }

    @Test
    public void shouldEmitEventsForTaskDelete() {
        //given
        CloudTask task = taskRestTemplate.createTask(TaskPayloadBuilder.create()
            .withName("my task name")
            .withDescription("long description here")
            .withCandidateUsers("hruser")
            .build());

        //when
        taskRestTemplate.delete(task);

        //then
        await().untilAsserted(() -> {
            List<CloudRuntimeEvent<?, ?>> receivedEvents = streamHandler.getAllReceivedEvents();

            CloudRuntimeEvent<?, ?> cloudTaskCancelledEvent = receivedEvents
                    .stream()
                    .filter(cloudRuntimeEvent -> cloudRuntimeEvent instanceof CloudTaskCancelledEvent
                            && cloudRuntimeEvent.getEntityId().equals(task.getId()))
                    .findFirst()
                    .orElse(null);
            assertThat(cloudTaskCancelledEvent).isNotNull();
            assertThat(cloudTaskCancelledEvent.getEventType()).isEqualTo(TASK_CANCELLED);
            Task cancelledTask = (Task) cloudTaskCancelledEvent.getEntity();
            assertThat(cancelledTask.getStatus()).isEqualTo(Task.TaskStatus.CANCELLED);
            assertThat(cancelledTask.getName()).isEqualTo(task.getName());
            assertThat(cancelledTask.getId()).isEqualTo(task.getId());

            CloudRuntimeEvent<?, ?> cloudTaskCandidateUserRemoved = receivedEvents
                    .stream()
                    .filter(cloudRuntimeEvent -> cloudRuntimeEvent instanceof CloudTaskCandidateUserRemovedEvent
                            && ((TaskCandidateUser) cloudRuntimeEvent.getEntity()).getTaskId().equals(task.getId()))
                    .findFirst()
                    .orElse(null);
            assertThat(cloudTaskCandidateUserRemoved).isNotNull();
            assertThat(cloudTaskCandidateUserRemoved.getEntityId()).isEqualTo("hruser");
            assertThat(cloudTaskCandidateUserRemoved.getEventType()).isEqualTo(TASK_CANDIDATE_USER_REMOVED);
            TaskCandidateUser taskCandidateUser = (TaskCandidateUser) cloudTaskCandidateUserRemoved.getEntity();
            assertThat(taskCandidateUser.getUserId()).isEqualTo("hruser");
            assertThat(taskCandidateUser.getTaskId()).isEqualTo(task.getId());
        });
    }

    @Test
    public void shouldEmitEventsForTaskUpdate() {
        //given
        CloudTask task = taskRestTemplate.createTask(TaskPayloadBuilder.create().withName("my task name 2").withDescription(
                "long description here").withAssignee("hruser").build());

        //when
        taskRestTemplate.updateTask(TaskPayloadBuilder.update().withTaskId(task.getId()).withDescription("short description").build());

        //then
        await().untilAsserted(() -> {
            List<CloudRuntimeEvent<?, ?>> receivedEvents = streamHandler.getLatestReceivedEvents();

            assertThat(streamHandler.getReceivedHeaders()).containsKeys(RUNTIME_BUNDLE_INFO_HEADERS);

            assertThat(receivedEvents)
                    .hasSize(1)
                    .extracting(CloudRuntimeEvent::getEventType,
                            CloudRuntimeEvent::getEntityId)
                    .containsExactly(tuple(TASK_UPDATED,
                            task.getId())
                    );

            assertThat(receivedEvents.get(0).getEntity()).isNotNull();
            assertThat(receivedEvents.get(0).getEntity()).isInstanceOf(Task.class);
            assertThat(((Task) receivedEvents.get(0).getEntity()).getStatus()).isEqualTo(Task.TaskStatus.ASSIGNED);
            assertThat(((Task) receivedEvents.get(0).getEntity()).getId()).isEqualTo(task.getId());
            assertThat(receivedEvents.get(0).getEntityId()).isEqualTo(task.getId());
            assertThat(((Task) receivedEvents.get(0).getEntity()).getDescription()).isEqualTo("short description");
        });
    }

    @Test
    public void shouldEmitEventsForTaskAddDeleteUserCandidates() {
        //given
        CloudTask task = taskRestTemplate.createTask(TaskPayloadBuilder.create()
            .withName("task1")
            .withDescription("task description")
            .withAssignee("hruser")
            .withCandidateUsers("hruser")
            .build());

        //when
        taskRestTemplate.addUserCandidates(TaskPayloadBuilder.addCandidateUsers().withTaskId(task.getId()).withCandidateUser("testuser").build());

        //then
        await().untilAsserted(() -> {
            List<CloudRuntimeEvent<?, ?>> receivedEvents = streamHandler.getLatestReceivedEvents();

            assertThat(streamHandler.getReceivedHeaders()).containsKeys(RUNTIME_BUNDLE_INFO_HEADERS);

            assertThat(receivedEvents)
                    .hasSize(1)
                    .extracting(CloudRuntimeEvent::getEventType,
                            CloudRuntimeEvent::getEntityId)
                    .containsExactly(tuple(TASK_CANDIDATE_USER_ADDED,
                            "testuser")
                    );

            assertThat(receivedEvents.get(0).getEntity()).isNotNull();
            assertThat(receivedEvents.get(0).getEntity()).isInstanceOf(TaskCandidateUser.class);
            assertThat(((TaskCandidateUser) receivedEvents.get(0).getEntity()).getTaskId()).isEqualTo(task.getId());
            assertThat(((TaskCandidateUser) receivedEvents.get(0).getEntity()).getUserId()).isEqualTo("testuser");
        });

        ResponseEntity<Resources<Resource<CandidateUser>>>  userCandidates = taskRestTemplate.getUserCandidates(task.getId());
        assertThat(userCandidates).isNotNull();
        assertThat(userCandidates.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(userCandidates.getBody().getContent()
                           .stream()
                           .map(Resource::getContent)
                           .map(CandidateUser::getUser)
        ).containsExactly("hruser",
                          "testuser");

        //when
        taskRestTemplate.deleteUserCandidates(TaskPayloadBuilder.deleteCandidateUsers().withTaskId(task.getId()).withCandidateUser("testuser").build());

        //then
        await().untilAsserted(() -> {
            List<CloudRuntimeEvent<?, ?>> receivedEvents = streamHandler.getLatestReceivedEvents();

            assertThat(streamHandler.getReceivedHeaders()).containsKeys(RUNTIME_BUNDLE_INFO_HEADERS);

            assertThat(receivedEvents)
                    .hasSize(1)
                    .extracting(CloudRuntimeEvent::getEventType,
                            CloudRuntimeEvent::getEntityId)
                    .containsExactly(tuple(TASK_CANDIDATE_USER_REMOVED,
                            "testuser")
                    );

            assertThat(receivedEvents.get(0).getEntity()).isNotNull();
            assertThat(receivedEvents.get(0).getEntity()).isInstanceOf(TaskCandidateUser.class);
            assertThat(((TaskCandidateUser) receivedEvents.get(0).getEntity()).getTaskId()).isEqualTo(task.getId());
            assertThat(((TaskCandidateUser) receivedEvents.get(0).getEntity()).getUserId()).isEqualTo("testuser");
        });

        userCandidates = taskRestTemplate.getUserCandidates(task.getId());
        assertThat(userCandidates).isNotNull();
        assertThat(userCandidates.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(userCandidates.getBody().getContent().size()).isEqualTo(1);
        assertThat(userCandidates.getBody().getContent()
                           .stream()
                           .map(Resource::getContent)
                           .map(CandidateUser::getUser)
        ).containsExactly("hruser");

        //Delete task
        taskRestTemplate.delete(task);
        await().untilAsserted(() -> {
            List<CloudRuntimeEvent<?, ?>> receivedEvents = streamHandler.getLatestReceivedEvents();
        });
    }

    @Test
    public void shouldEmitEventsForTaskAddDeleteGroupCandidates() {
        //given
        CloudTask task = taskRestTemplate.createTask(TaskPayloadBuilder.create().withName("task2").withDescription(
                "test task description").withAssignee("hruser").build());

        ResponseEntity<Resources<Resource<CandidateGroup>>> groupCandidates = taskRestTemplate.getGroupCandidates(task.getId());
        assertThat(groupCandidates).isNotNull();
        assertThat(groupCandidates.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(groupCandidates.getBody().getContent().size()).isEqualTo(0);

        //when
        taskRestTemplate.addGroupCandidates(TaskPayloadBuilder.addCandidateGroups().withTaskId(task.getId()).withCandidateGroup("hr").build());

        //then
        await().untilAsserted(() -> {
            List<CloudRuntimeEvent<?, ?>> receivedEvents = streamHandler.getLatestReceivedEvents();

            assertThat(streamHandler.getReceivedHeaders()).containsKeys(RUNTIME_BUNDLE_INFO_HEADERS);

            assertThat(receivedEvents)
                    .hasSize(1)
                    .extracting(CloudRuntimeEvent::getEventType,
                            CloudRuntimeEvent::getEntityId)
                    .containsExactly(tuple(TASK_CANDIDATE_GROUP_ADDED,
                            "hr")
                    );

            assertThat(receivedEvents.get(0).getEntity()).isNotNull();
            assertThat(receivedEvents.get(0).getEntity()).isInstanceOf(TaskCandidateGroup.class);
            assertThat(((TaskCandidateGroup) receivedEvents.get(0).getEntity()).getTaskId()).isEqualTo(task.getId());
            assertThat(((TaskCandidateGroup) receivedEvents.get(0).getEntity()).getGroupId()).isEqualTo("hr");
        });

        groupCandidates = taskRestTemplate.getGroupCandidates(task.getId());
        assertThat(groupCandidates).isNotNull();
        assertThat(groupCandidates.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(groupCandidates.getBody().getContent().size()).isEqualTo(1);
        assertThat(groupCandidates.getBody().getContent()
                           .stream()
                           .map(Resource::getContent)
                           .map(CandidateGroup::getGroup)
        ).containsExactly("hr");

        //when
        taskRestTemplate.deleteGroupCandidates(TaskPayloadBuilder.deleteCandidateGroups().withTaskId(task.getId()).withCandidateGroup("hr").build());

        //then
        await().untilAsserted(() -> {
            List<CloudRuntimeEvent<?, ?>> receivedEvents = streamHandler.getLatestReceivedEvents();

            assertThat(streamHandler.getReceivedHeaders()).containsKeys(RUNTIME_BUNDLE_INFO_HEADERS);

            assertThat(receivedEvents)
                    .hasSize(1)
                    .extracting(CloudRuntimeEvent::getEventType,
                            CloudRuntimeEvent::getEntityId)
                    .containsExactly(tuple(TASK_CANDIDATE_GROUP_REMOVED,
                            "hr")
                    );

            assertThat(receivedEvents.get(0).getEntity()).isNotNull();
            assertThat(receivedEvents.get(0).getEntity()).isInstanceOf(TaskCandidateGroup.class);
            assertThat(((TaskCandidateGroup) receivedEvents.get(0).getEntity()).getTaskId()).isEqualTo(task.getId());
            assertThat(((TaskCandidateGroup) receivedEvents.get(0).getEntity()).getGroupId()).isEqualTo("hr");
        });

        groupCandidates = taskRestTemplate.getGroupCandidates(task.getId());
        assertThat(groupCandidates).isNotNull();
        assertThat(groupCandidates.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(groupCandidates.getBody().getContent().size()).isEqualTo(0);

        //Delete task
        taskRestTemplate.delete(task);
        await().untilAsserted(() -> {
            List<CloudRuntimeEvent<?, ?>> receivedEvents = streamHandler.getLatestReceivedEvents();
        });
    }

    @Test
    public void testTwoSubProcesses() {
        //given
        ResponseEntity<CloudProcessInstance> processInstance = processInstanceRestTemplate.startProcess(processDefinitionIds.get(CALL_TWO_SUB_PROCESSES));

        String processInstanceId = processInstance.getBody().getId();

        // when
        List<String> subprocessIds = runtimeService.createProcessInstanceQuery()
                .superProcessInstanceId(processInstanceId)
                .list()
                .stream()
                .map(it -> it.getProcessInstanceId())
                .collect(Collectors.toList());
        // then
        assertThat(subprocessIds).hasSize(2);

        String subProcessId1 = subprocessIds.get(0);
        String subProcessId2 = subprocessIds.get(1);

        await().untilAsserted(() -> {
            assertThat(streamHandler.getReceivedHeaders()).containsKeys(ALL_REQUIRED_HEADERS);

            assertThat(streamHandler.getLatestReceivedEvents())
                    .extracting(CloudRuntimeEvent::getEventType,
                            CloudRuntimeEvent::getProcessInstanceId,
                            CloudRuntimeEvent::getParentProcessInstanceId,
                            CloudRuntimeEvent::getProcessDefinitionKey)
                        .containsExactly(tuple(PROCESS_CREATED, processInstanceId, null, CALL_TWO_SUB_PROCESSES),
                            tuple(PROCESS_UPDATED, processInstanceId, null, CALL_TWO_SUB_PROCESSES),
                            tuple(PROCESS_STARTED, processInstanceId, null, CALL_TWO_SUB_PROCESSES),
                            tuple(ACTIVITY_STARTED, processInstanceId, null, CALL_TWO_SUB_PROCESSES),
                            tuple(ACTIVITY_COMPLETED, processInstanceId, null, CALL_TWO_SUB_PROCESSES),
                            tuple(SEQUENCE_FLOW_TAKEN, processInstanceId, null, CALL_TWO_SUB_PROCESSES),
                            tuple(ACTIVITY_STARTED, processInstanceId, null, CALL_TWO_SUB_PROCESSES),
                            tuple(ACTIVITY_COMPLETED, processInstanceId, null, CALL_TWO_SUB_PROCESSES),
                            tuple(SEQUENCE_FLOW_TAKEN, processInstanceId, null, CALL_TWO_SUB_PROCESSES),
                            tuple(SEQUENCE_FLOW_TAKEN, processInstanceId, null, CALL_TWO_SUB_PROCESSES),
                            tuple(ACTIVITY_STARTED, processInstanceId, null, CALL_TWO_SUB_PROCESSES),
                            tuple(PROCESS_CREATED, subProcessId1, processInstanceId, SIMPLE_SUB_PROCESS1),
                            tuple(ACTIVITY_STARTED, processInstanceId, null, CALL_TWO_SUB_PROCESSES),
                            tuple(PROCESS_CREATED, subProcessId2, processInstanceId, SIMPLE_SUB_PROCESS2),
                            tuple(PROCESS_STARTED, subProcessId2, processInstanceId, SIMPLE_SUB_PROCESS2),
                            tuple(ACTIVITY_STARTED, subProcessId2, processInstanceId, SIMPLE_SUB_PROCESS2),
                            tuple(ACTIVITY_COMPLETED, subProcessId2, processInstanceId, SIMPLE_SUB_PROCESS2),
                            tuple(SEQUENCE_FLOW_TAKEN, subProcessId2, processInstanceId, SIMPLE_SUB_PROCESS2),
                            tuple(ACTIVITY_STARTED, subProcessId2, processInstanceId, SIMPLE_SUB_PROCESS2),
                            tuple(TASK_CREATED, subProcessId2, processInstanceId, SIMPLE_SUB_PROCESS2),
                            tuple(PROCESS_STARTED, subProcessId1, processInstanceId, SIMPLE_SUB_PROCESS1),
                            tuple(ACTIVITY_STARTED, subProcessId1, processInstanceId, SIMPLE_SUB_PROCESS1),
                            tuple(ACTIVITY_COMPLETED, subProcessId1, processInstanceId, SIMPLE_SUB_PROCESS1),
                            tuple(SEQUENCE_FLOW_TAKEN, subProcessId1, processInstanceId, SIMPLE_SUB_PROCESS1),
                            tuple(ACTIVITY_STARTED, subProcessId1, processInstanceId, SIMPLE_SUB_PROCESS1),
                            tuple(TASK_CREATED, subProcessId1, processInstanceId, SIMPLE_SUB_PROCESS1)
                    );
        });

        // Clean up
        // Clean up
        runtimeService.deleteProcessInstance(subProcessId1, "Clean up");
        runtimeService.deleteProcessInstance(subProcessId2, "Clean up");

        runtimeService.deleteProcessInstance(processInstanceId, "Clean up");

    }

    @Test
    public void shouldProduceCancelEventsDuringMultiInstanceExecution() {

        ResponseEntity<CloudProcessInstance> startProcessEntity = processInstanceRestTemplate.startProcessByKey("miParallelUserTasksCompletionCondition", null, null);
        List<CloudTask> tasks = new ArrayList<>(processInstanceRestTemplate.getTasks(startProcessEntity).getBody().getContent());
        assertThat(tasks).hasSize(5);

        taskRestTemplate.complete(tasks.get(0));
        taskRestTemplate.complete(tasks.get(1));

        //then
        await().untilAsserted(() -> {
            List<CloudRuntimeEvent<?, ?>> receivedEvents = streamHandler.getAllReceivedEvents();
            receivedEvents = receivedEvents.stream()
                    .filter(event -> startProcessEntity.getBody().getId().equals(event.getProcessInstanceId()))
                    .collect(Collectors.toList());

            assertThat(streamHandler.getReceivedHeaders()).containsKeys(ALL_REQUIRED_HEADERS);

            assertThat(receivedEvents)
                    .filteredOn(event -> event.getEventType() == TASK_CREATED)
                    .extracting(RuntimeEvent::getEventType, event -> ((Task) event.getEntity()).getName())
                    .containsExactlyInAnyOrder(
                            tuple(TASK_CREATED, "My Task 0"),
                            tuple(TASK_CREATED, "My Task 1"),
                            tuple(TASK_CREATED, "My Task 2"),
                            tuple(TASK_CREATED, "My Task 3"),
                            tuple(TASK_CREATED, "My Task 4")
                    );

            assertThat(receivedEvents)
                    .filteredOn(event -> event.getEventType() == TASK_ASSIGNED)
                    .extracting(RuntimeEvent::getEventType, event -> ((Task) event.getEntity()).getName())
                    .containsExactlyInAnyOrder(
                            tuple(TASK_ASSIGNED, "My Task 0"),
                            tuple(TASK_ASSIGNED, "My Task 1"),
                            tuple(TASK_ASSIGNED, "My Task 2"),
                            tuple(TASK_ASSIGNED, "My Task 3"),
                            tuple(TASK_ASSIGNED, "My Task 4")
                    );

            assertThat(receivedEvents)
                    .filteredOn(event -> event.getEventType() == TASK_COMPLETED)
                    .extracting(RuntimeEvent::getEventType, event -> ((Task) event.getEntity()).getName())
                    .containsExactly(
                            tuple(TASK_COMPLETED, "My Task 0"),
                            tuple(TASK_COMPLETED, "My Task 1")
                    );

            assertThat(receivedEvents)
                    .filteredOn(event -> event.getEventType() == TASK_CANCELLED).isEmpty();
        });

        //complete condition expression passed
        taskRestTemplate.complete(tasks.get(2));

        Collection<CloudTask> pendingTask = processInstanceRestTemplate.getTasks(startProcessEntity).getBody().getContent();
        assertThat(pendingTask).isEmpty();

        //then
        await().untilAsserted(() -> {
            List<CloudRuntimeEvent<?, ?>> receivedEvents = streamHandler.getAllReceivedEvents();
            receivedEvents = receivedEvents.stream()
                    .filter(event -> startProcessEntity.getBody().getId().equals(event.getProcessInstanceId()))
                    .collect(Collectors.toList());

            assertThat(streamHandler.getReceivedHeaders()).containsKeys(ALL_REQUIRED_HEADERS);

            assertThat(receivedEvents)
                    .filteredOn(event -> event.getEventType() == TASK_COMPLETED)
                    .extracting(RuntimeEvent::getEventType, event -> ((Task) event.getEntity()).getName())
                    .containsExactlyInAnyOrder(
                            tuple(TASK_COMPLETED, "My Task 0"),
                            tuple(TASK_COMPLETED, "My Task 1"),
                            tuple(TASK_COMPLETED, "My Task 2")
                    );

            assertThat(receivedEvents)
                    .filteredOn(event -> event.getEventType() == TASK_CANCELLED)
                    .extracting(RuntimeEvent::getEventType, event -> ((Task) event.getEntity()).getName())
                    .containsExactlyInAnyOrder(
                            tuple(TASK_CANCELLED, "My Task 3"),
                            tuple(TASK_CANCELLED, "My Task 4")
                    );

            assertThat(receivedEvents).extracting(RuntimeEvent::getEventType,
                    RuntimeEvent::getProcessInstanceId)
                    .contains(tuple(PROCESS_COMPLETED, startProcessEntity.getBody().getId()));
        });
    }

    @Test
    public void shouldProduceEventsDuringMultiInstanceSubProcessExecution() {

        //when
        ResponseEntity<CloudProcessInstance> startProcessEntity = processInstanceRestTemplate.startProcessByKey("miParallelSubprocessCompletionCondition", null, null);

        List<CloudTask> tasks = new ArrayList<>(processInstanceRestTemplate.getTasks(startProcessEntity).getBody().getContent());
        assertThat(tasks).hasSize(5);

        taskRestTemplate.complete(tasks.get(0));
        taskRestTemplate.complete(tasks.get(1));

        //then
        await().untilAsserted(() -> {
            List<CloudRuntimeEvent<?, ?>> receivedEvents = streamHandler.getAllReceivedEvents();
            receivedEvents = receivedEvents.stream()
                    .filter(event -> startProcessEntity.getBody().getId().equals(event.getProcessInstanceId()))
                    .collect(Collectors.toList());

            assertThat(streamHandler.getReceivedHeaders()).containsKeys(ALL_REQUIRED_HEADERS);

            assertThat(receivedEvents)
                    .filteredOn(event -> event.getEventType() == TASK_CREATED)
                    .extracting(RuntimeEvent::getEventType, event -> ((Task) event.getEntity()).getName())
                    .containsExactlyInAnyOrder(
                            tuple(TASK_CREATED, "My Task 0"),
                            tuple(TASK_CREATED, "My Task 1"),
                            tuple(TASK_CREATED, "My Task 2"),
                            tuple(TASK_CREATED, "My Task 3"),
                            tuple(TASK_CREATED, "My Task 4")
                    );

            assertThat(receivedEvents)
                    .filteredOn(event -> event.getEventType() == TASK_ASSIGNED)
                    .extracting(RuntimeEvent::getEventType, event -> ((Task) event.getEntity()).getName())
                    .containsExactlyInAnyOrder(
                            tuple(TASK_ASSIGNED, "My Task 0"),
                            tuple(TASK_ASSIGNED, "My Task 1"),
                            tuple(TASK_ASSIGNED, "My Task 2"),
                            tuple(TASK_ASSIGNED, "My Task 3"),
                            tuple(TASK_ASSIGNED, "My Task 4")
                    );

            assertThat(receivedEvents)
                    .filteredOn(event -> event.getEventType() == TASK_COMPLETED)
                    .extracting(RuntimeEvent::getEventType, event -> ((Task) event.getEntity()).getName())
                    .containsExactlyInAnyOrder(
                            tuple(TASK_COMPLETED, "My Task 0"),
                            tuple(TASK_COMPLETED, "My Task 1")
                    );

            assertThat(receivedEvents)
                    .filteredOn(event -> event.getEventType() == TASK_CANCELLED)
                    .isEmpty();
        });

        //complete condition expression passed
        taskRestTemplate.complete(tasks.get(2));

        Collection<CloudTask> pendingTask = processInstanceRestTemplate.getTasks(startProcessEntity).getBody().getContent();
        assertThat(pendingTask).isEmpty();

        //then
        await().untilAsserted(() -> {
            List<CloudRuntimeEvent<?, ?>> receivedEvents = streamHandler.getAllReceivedEvents();
            receivedEvents = receivedEvents.stream()
                    .filter(event -> startProcessEntity.getBody().getId().equals(event.getProcessInstanceId()))
                    .collect(Collectors.toList());

            assertThat(streamHandler.getReceivedHeaders()).containsKeys(ALL_REQUIRED_HEADERS);

            assertThat(receivedEvents)
                    .filteredOn(event -> event.getEventType() == TASK_COMPLETED)
                    .extracting(RuntimeEvent::getEventType, event -> ((Task) event.getEntity()).getName())
                    .contains(
                            tuple(TASK_COMPLETED, "My Task 0"),
                            tuple(TASK_COMPLETED, "My Task 1"),
                            tuple(TASK_COMPLETED, "My Task 2")
                    );

            assertThat(receivedEvents)
                    .filteredOn(event -> event.getEventType() == TASK_CANCELLED)
                    .extracting(RuntimeEvent::getEventType, event -> ((Task) event.getEntity()).getName())
                    .contains(
                            tuple(TASK_CANCELLED, "My Task 3"),
                            tuple(TASK_CANCELLED, "My Task 4")
                    );

            assertThat(receivedEvents).extracting(RuntimeEvent::getEventType,
                    RuntimeEvent::getProcessInstanceId)
                    .contains(tuple(PROCESS_COMPLETED, startProcessEntity.getBody().getId()));
        });
    }

    @Test
    public void shouldProduceIntegrationResultEventsDuringMultiInstanceCloudConnectorExecution() {

        //when
        ResponseEntity<CloudProcessInstance> startProcessEntity = processInstanceRestTemplate.startProcessByKey("miParallelCloudConnector",
                                                                                                                Collections.singletonMap("instanceCount", 3),
                                                                                                                null);

        List<CloudIntegrationRequestedEvent> integrationRequestedEvents = new ArrayList<>();

        //then
        await()
            .untilAsserted(() -> {
            List<CloudRuntimeEvent<?, ?>> receivedEvents = streamHandler.getAllReceivedEvents();
            receivedEvents = receivedEvents.stream()
                    .filter(event -> startProcessEntity.getBody().getId().equals(event.getProcessInstanceId()))
                    .collect(Collectors.toList());

            assertThat(streamHandler.getReceivedHeaders()).containsKeys(ALL_REQUIRED_HEADERS);


            List<CloudBPMNActivityStartedEvent> receivedActivityStartedEvents = receivedEvents.stream()
                    .filter(event -> event.getEventType() == ACTIVITY_STARTED && event.getEntityId()
                                                                                      .equals("miCloudConnectorId"))
                    .map(CloudBPMNActivityStartedEvent.class::cast)
                    .collect(Collectors.toList());

            assertThat(receivedActivityStartedEvents)
                    .extracting(RuntimeEvent::getEventType, event -> ((BPMNActivity) event.getEntity()).getElementId())
                    .containsExactlyInAnyOrder(
                            tuple(ACTIVITY_STARTED, "miCloudConnectorId"),
                            tuple(ACTIVITY_STARTED, "miCloudConnectorId"),
                            tuple(ACTIVITY_STARTED, "miCloudConnectorId")
                    );

            List<CloudIntegrationRequestedEvent> receivedIntegrationRequestedEvents = receivedEvents.stream()
                    .filter(event -> event.getEventType() == INTEGRATION_REQUESTED &&
                                        ((IntegrationContext) event.getEntity()).getClientId()
                                                                                .equals("miCloudConnectorId"))
                    .map(CloudIntegrationRequestedEvent.class::cast)
                    .collect(Collectors.toList());

            assertThat(receivedIntegrationRequestedEvents)
                    .extracting(RuntimeEvent::getEventType, event -> ((IntegrationContext) event.getEntity()).getClientId())
                    .containsExactlyInAnyOrder(
                            tuple(INTEGRATION_REQUESTED, "miCloudConnectorId"),
                            tuple(INTEGRATION_REQUESTED, "miCloudConnectorId"),
                            tuple(INTEGRATION_REQUESTED, "miCloudConnectorId")
                    );

            integrationRequestedEvents.addAll(receivedIntegrationRequestedEvents);

        });

        MessageChannel resultsChannel = channelResolver.resolveDestination(integrationResultDestination);

        // complete cloud connector tasks
        integrationRequestedEvents.stream()
                                  .map(request -> {
                                      return new IntegrationResultImpl(new IntegrationRequestImpl(request.getEntity()),
                                                                       request.getEntity());
                                  })
                                  .map(payload -> MessageBuilder.withPayload(payload)
                                                                .build())
                                  .forEach(resultsChannel::send);


        //then
        await()
            .untilAsserted(() -> {
            List<CloudRuntimeEvent<?, ?>> receivedEvents = streamHandler.getAllReceivedEvents();
            receivedEvents = receivedEvents.stream()
                    .filter(event -> startProcessEntity.getBody().getId().equals(event.getProcessInstanceId()))
                    .collect(Collectors.toList());

            List<CloudBPMNActivityCompletedEvent> receivedActivityCompletedEvents = receivedEvents.stream()
                    .filter(event -> event.getEventType() == ACTIVITY_COMPLETED && event.getEntityId()
                                                                                      .equals("miCloudConnectorId"))
                    .map(CloudBPMNActivityCompletedEvent.class::cast)
                    .collect(Collectors.toList());

            assertThat(receivedActivityCompletedEvents)
                    .extracting(RuntimeEvent::getEventType, event -> ((BPMNActivity) event.getEntity()).getElementId())
                    .containsExactlyInAnyOrder(
                            tuple(ACTIVITY_COMPLETED, "miCloudConnectorId"),
                            tuple(ACTIVITY_COMPLETED, "miCloudConnectorId"),
                            tuple(ACTIVITY_COMPLETED, "miCloudConnectorId")
                    );

            List<CloudIntegrationResultReceivedEvent> receivedIntegrationResultEvents = receivedEvents.stream()
                    .filter(event -> event.getEventType() == INTEGRATION_RESULT_RECEIVED &&
                                        ((IntegrationContext) event.getEntity()).getClientId()
                                                                                .equals("miCloudConnectorId"))
                    .map(CloudIntegrationResultReceivedEvent.class::cast)
                    .collect(Collectors.toList());

            assertThat(receivedIntegrationResultEvents)
                    .extracting(RuntimeEvent::getEventType, event -> ((IntegrationContext) event.getEntity()).getClientId())
                    .containsExactlyInAnyOrder(
                            tuple(INTEGRATION_RESULT_RECEIVED, "miCloudConnectorId"),
                            tuple(INTEGRATION_RESULT_RECEIVED, "miCloudConnectorId"),
                            tuple(INTEGRATION_RESULT_RECEIVED, "miCloudConnectorId")
                    );

            assertThat(receivedEvents).extracting(RuntimeEvent::getEventType,
                    RuntimeEvent::getProcessInstanceId)
                    .contains(tuple(PROCESS_COMPLETED, startProcessEntity.getBody().getId()));
        });
    }

    @Test
    public void shouldProduceIntegrationErrorEventsDuringMultiInstanceCloudConnectorExecution() {

        //when
        ResponseEntity<CloudProcessInstance> startProcessEntity = processInstanceRestTemplate.startProcessByKey("miParallelCloudConnector",
                                                                                                                Collections.singletonMap("instanceCount", 3),
                                                                                                                null);
        List<CloudIntegrationRequestedEvent> integrationRequestedEvents = new ArrayList<>();

        //then
        await()
            .untilAsserted(() -> {
            List<CloudRuntimeEvent<?, ?>> receivedEvents = streamHandler.getAllReceivedEvents();
            receivedEvents = receivedEvents.stream()
                    .filter(event -> startProcessEntity.getBody().getId().equals(event.getProcessInstanceId()))
                    .collect(Collectors.toList());

            assertThat(streamHandler.getReceivedHeaders()).containsKeys(ALL_REQUIRED_HEADERS);


            List<CloudBPMNActivityStartedEvent> receivedActivityStartedEvents = receivedEvents.stream()
                    .filter(event -> event.getEventType() == ACTIVITY_STARTED && event.getEntityId()
                                                                                      .equals("miCloudConnectorId"))
                    .map(CloudBPMNActivityStartedEvent.class::cast)
                    .collect(Collectors.toList());

            assertThat(receivedActivityStartedEvents)
                    .extracting(RuntimeEvent::getEventType, event -> ((BPMNActivity) event.getEntity()).getElementId())
                    .containsExactlyInAnyOrder(
                            tuple(ACTIVITY_STARTED, "miCloudConnectorId"),
                            tuple(ACTIVITY_STARTED, "miCloudConnectorId"),
                            tuple(ACTIVITY_STARTED, "miCloudConnectorId")
                    );

            List<CloudIntegrationRequestedEvent> receivedIntegrationRequestedEvents = receivedEvents.stream()
                    .filter(event -> event.getEventType() == INTEGRATION_REQUESTED &&
                                        ((IntegrationContext) event.getEntity()).getClientId()
                                                                                .equals("miCloudConnectorId"))
                    .map(CloudIntegrationRequestedEvent.class::cast)
                    .collect(Collectors.toList());

            assertThat(receivedIntegrationRequestedEvents)
                    .extracting(RuntimeEvent::getEventType, event -> ((IntegrationContext) event.getEntity()).getClientId())
                    .containsExactlyInAnyOrder(
                            tuple(INTEGRATION_REQUESTED, "miCloudConnectorId"),
                            tuple(INTEGRATION_REQUESTED, "miCloudConnectorId"),
                            tuple(INTEGRATION_REQUESTED, "miCloudConnectorId")
                    );

            integrationRequestedEvents.addAll(receivedIntegrationRequestedEvents);

        });

        MessageChannel resultsChannel = channelResolver.resolveDestination(integrationErrorDestination);

        Error error = new Error("IntegrationError");
        error.fillInStackTrace();

        // throw error in cloud connector
        integrationRequestedEvents.stream()
                                  .map(request -> {
                                      return new IntegrationErrorImpl(new IntegrationRequestImpl(request.getEntity()),
                                                                      error);
                                  })
                                  .map(payload -> MessageBuilder.withPayload(payload)
                                                                .build())
                                  .forEach(resultsChannel::send);
        //then
        await()
            .untilAsserted(() -> {
            List<CloudRuntimeEvent<?, ?>> receivedEvents = streamHandler.getAllReceivedEvents();
            receivedEvents = receivedEvents.stream()
                    .filter(event -> startProcessEntity.getBody().getId().equals(event.getProcessInstanceId()))
                    .collect(Collectors.toList());

            List<CloudIntegrationErrorReceivedEvent> receivedIntegrationResultEvents = receivedEvents.stream()
                    .filter(event -> event.getEventType() == INTEGRATION_ERROR_RECEIVED &&
                                        ((IntegrationContext) event.getEntity()).getClientId()
                                                                                .equals("miCloudConnectorId"))
                    .map(CloudIntegrationErrorReceivedEvent.class::cast)
                    .collect(Collectors.toList());

            assertThat(receivedIntegrationResultEvents)
                    .extracting(RuntimeEvent::getEventType, event -> ((IntegrationContext) event.getEntity()).getClientId())
                    .containsExactlyInAnyOrder(
                            tuple(INTEGRATION_ERROR_RECEIVED, "miCloudConnectorId"),
                            tuple(INTEGRATION_ERROR_RECEIVED, "miCloudConnectorId"),
                            tuple(INTEGRATION_ERROR_RECEIVED, "miCloudConnectorId")
                    );
        });
    }


    @Test
    public void shouldHaveAppVersionSetInBothEventsAndApplicationElementEntities() {

        //when
        ResponseEntity<CloudProcessInstance> startProcessEntity = processInstanceRestTemplate.startProcess(ProcessPayloadBuilder
                                                                                                                   .start()
                                                                                                                   .withProcessDefinitionKey(SIMPLE_PROCESS)
                                                                                                                   .withProcessDefinitionId(processDefinitionIds.get(SIMPLE_PROCESS))
                                                                                                                   .withName("my instance name")
                                                                                                                   .withBusinessKey("my business key")
                                                                                                                   .build());
        //then
        await().untilAsserted(() -> {
            List<CloudRuntimeEvent<?, ?>> receivedEvents = streamHandler.getAllReceivedEvents();

            assertThat(streamHandler.getReceivedHeaders()).containsKeys(ALL_REQUIRED_HEADERS);

            List<CloudRuntimeEvent<?, ?>> applicationElementEvents = receivedEvents
                    .stream()
                    .filter(event -> startProcessEntity.getBody().getId().equals(event.getProcessInstanceId()))
                    .filter(event -> event.getEntity().getClass().getSuperclass().equals(ApplicationElementImpl.class))
                    .collect(Collectors.toList());

            assertThat(applicationElementEvents)
                    .extracting(ApplicationElement::getAppVersion,
                                event ->((ApplicationElement) event.getEntity()).getAppVersion())
                    .containsOnly(
                            tuple("1",
                                        "1"));

        });

        runtimeService.deleteProcessInstance(startProcessEntity.getBody().getId(), "Clean up");
    }

    @Test
    public void shouldProduceEventsDuringMultiInstanceCallActivityExecution() {
        //given
        List<CloudTask> tasks;

        CloudProcessInstance startProcessEntity = processInstanceRestTemplate.startProcessByKey("miParallelCallActivity", null, null)
                .getBody();

        List<ProcessInstance> childProcesses = new ArrayList<>(
                processInstanceRestTemplate.getSubprocesses(startProcessEntity.getId())
                        .getBody().getContent()
        );
        assertThat(childProcesses).hasSize(5);

        //when
        //complete first two children process, completion condition not reached yet
        for(int i = 0; i < 2; i++) {
            tasks = new ArrayList<>(
                    processInstanceRestTemplate.getTasks(childProcesses.get(i).getId()).getBody().getContent()
            );
            assertThat(tasks).hasSize(1);
            taskRestTemplate.complete(tasks.get(0));
        }

        //then
        await().untilAsserted(() -> {
            List<CloudRuntimeEvent<?, ?>> receivedEvents = streamHandler.getAllReceivedEvents();

            receivedEvents = receivedEvents.stream()
                    .filter(event -> startProcessEntity.getId().equals(event.getParentProcessInstanceId()))
                    .collect(Collectors.toList());

            assertThat(streamHandler.getReceivedHeaders()).containsKeys(ALL_REQUIRED_HEADERS);

            assertThat(receivedEvents)
                    .filteredOn(event -> event.getEventType() == PROCESS_STARTED)
                    .size().isEqualTo(5);

            assertThat(receivedEvents)
                    .filteredOn(event -> event.getEventType() == TASK_ASSIGNED)
                    .size().isEqualTo(5);

            assertThat(receivedEvents)
                    .filteredOn(event -> event.getEventType() == TASK_COMPLETED)
                    .size().isEqualTo(2);

            assertThat(receivedEvents)
                    .filteredOn(event -> event.getEventType() == PROCESS_COMPLETED)
                    .size().isEqualTo(2);
        });

        //complete condition expression passed
        tasks = new ArrayList<>(processInstanceRestTemplate.getTasks(childProcesses.get(2).getId()).getBody().getContent());
        assertThat(tasks).hasSize(1);
        taskRestTemplate.complete(tasks.get(0));

        assertThat(processInstanceRestTemplate.getSubprocesses(startProcessEntity.getId())
            .getBody().getContent()).isEmpty();

        //then
        await().untilAsserted(() -> {
            List<CloudRuntimeEvent<?, ?>> receivedEvents = streamHandler.getAllReceivedEvents();
            receivedEvents = receivedEvents.stream()
                    .filter(event -> startProcessEntity.getId().equals(event.getParentProcessInstanceId()))
                    .collect(Collectors.toList());

            assertThat(streamHandler.getReceivedHeaders()).containsKeys(ALL_REQUIRED_HEADERS);

            assertThat(receivedEvents)
                    .filteredOn(event -> event.getEventType() == TASK_COMPLETED)
                    .hasSize(3);

            assertThat(receivedEvents)
                    .filteredOn(event -> event.getEventType() == PROCESS_COMPLETED || event.getEventType() == PROCESS_CANCELLED)
                    .extracting(CloudRuntimeEvent::getEventType,
                        event -> ((ProcessInstance) event.getEntity()).getId())
                    .containsExactlyInAnyOrder(
                        tuple(PROCESS_COMPLETED, childProcesses.get(0).getId()),
                        tuple(PROCESS_COMPLETED, childProcesses.get(1).getId()),
                        tuple(PROCESS_COMPLETED, childProcesses.get(2).getId()),
                        tuple(PROCESS_CANCELLED, childProcesses.get(3).getId()),
                        tuple(PROCESS_CANCELLED, childProcesses.get(4).getId())
                    );
        });
    }

    private ResponseEntity<PagedResources<CloudProcessDefinition>> getProcessDefinitions() {
        ParameterizedTypeReference<PagedResources<CloudProcessDefinition>> responseType = new ParameterizedTypeReference<PagedResources<CloudProcessDefinition>>() {
        };

        return restTemplate.exchange(PROCESS_DEFINITIONS_URL,
                HttpMethod.GET,
                null,
                responseType);
    }
}
