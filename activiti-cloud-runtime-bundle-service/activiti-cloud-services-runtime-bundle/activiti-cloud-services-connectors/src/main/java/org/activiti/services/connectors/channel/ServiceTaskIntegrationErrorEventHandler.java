/*
 * Copyright 2017 Alfresco, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.activiti.services.connectors.channel;

import java.util.List;
import java.util.stream.Stream;

import org.activiti.api.process.model.IntegrationContext;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.process.model.IntegrationError;
import org.activiti.cloud.api.process.model.impl.events.CloudIntegrationErrorReceivedEventImpl;
import org.activiti.cloud.services.events.configuration.RuntimeBundleProperties;
import org.activiti.cloud.services.events.converter.RuntimeBundleInfoAppender;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.impl.persistence.entity.integration.IntegrationContextEntity;
import org.activiti.engine.integration.IntegrationContextService;
import org.activiti.engine.runtime.Execution;
import org.activiti.services.connectors.message.IntegrationContextMessageBuilderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;

public class ServiceTaskIntegrationErrorEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceTaskIntegrationErrorEventHandler.class);

    private final RuntimeService runtimeService;
    private final IntegrationContextService integrationContextService;
    private final MessageChannel auditProducer;
    private final RuntimeBundleProperties runtimeBundleProperties;
    private final RuntimeBundleInfoAppender runtimeBundleInfoAppender;
    private final IntegrationContextMessageBuilderFactory messageBuilderFactory;


    public ServiceTaskIntegrationErrorEventHandler(RuntimeService runtimeService,
                                                    IntegrationContextService integrationContextService,
                                                    MessageChannel auditProducer,
                                                    RuntimeBundleProperties runtimeBundleProperties,
                                                    RuntimeBundleInfoAppender runtimeBundleInfoAppender,
                                                    IntegrationContextMessageBuilderFactory messageBuilderFactory) {
        this.runtimeService = runtimeService;
        this.integrationContextService = integrationContextService;
        this.auditProducer = auditProducer;
        this.runtimeBundleProperties = runtimeBundleProperties;
        this.runtimeBundleInfoAppender = runtimeBundleInfoAppender;
        this.messageBuilderFactory = messageBuilderFactory;
    }

    @StreamListener(ProcessEngineIntegrationChannels.INTEGRATION_ERRORS_CONSUMER)
    public void receive(IntegrationError integrationError) {
        IntegrationContext integrationContext = integrationError.getIntegrationContext();
        IntegrationContextEntity integrationContextEntity = integrationContextService.findById(integrationContext.getId());

        if (integrationContextEntity != null) {
            integrationContextService.deleteIntegrationContext(integrationContextEntity);

            List<Execution> executions = runtimeService.createExecutionQuery().executionId(integrationContextEntity.getExecutionId()).list();
            if (executions.size() > 0) {

                String message = "Received integration error with execution id `" +
                        integrationContextEntity.getExecutionId() +
                        ", flow node id `" + integrationContext.getClientId() +
                        "`. The integration error for the integration context `" + integrationContext.getId() + "` is {}";

                LOGGER.debug(message, integrationError);

            } else {
                String message = "No task is in this RB is waiting for integration result with execution id `" +
                    integrationContextEntity.getExecutionId() +
                    ", flow node id `" + integrationContext.getClientId() +
                    "`. The integration result for the integration context `" + integrationContext.getId() + "` will be ignored.";
                LOGGER.debug(message);
            }

            sendAuditMessage(integrationError);
        }
    }

    private void sendAuditMessage(IntegrationError integrationError) {
        if (runtimeBundleProperties.getEventsProperties().isIntegrationAuditEventsEnabled()) {
            CloudIntegrationErrorReceivedEventImpl integrationErrorReceived = new CloudIntegrationErrorReceivedEventImpl(integrationError.getIntegrationContext(),
                                                                                                                         integrationError.getErrorMessage(),
                                                                                                                         integrationError.getErrorClassName(),
                                                                                                                         integrationError.getStackTraceElements());
            runtimeBundleInfoAppender.appendRuntimeBundleInfoTo(integrationErrorReceived);

            CloudRuntimeEvent<?, ?>[] payload = Stream.of(integrationErrorReceived)
                                                      .toArray(CloudRuntimeEvent[]::new);

            Message<CloudRuntimeEvent<?, ?>[]> message = messageBuilderFactory.create(integrationErrorReceived.getEntity())
                                                                              .withPayload(payload)
                                                                              .build();
            auditProducer.send(message);
        }
    }
}
