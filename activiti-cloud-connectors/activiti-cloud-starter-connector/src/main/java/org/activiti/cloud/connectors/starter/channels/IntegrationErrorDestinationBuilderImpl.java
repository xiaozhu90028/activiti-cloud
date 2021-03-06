package org.activiti.cloud.connectors.starter.channels;

import org.activiti.cloud.api.process.model.IntegrationRequest;
import org.activiti.cloud.connectors.starter.configuration.ConnectorProperties;

public class IntegrationErrorDestinationBuilderImpl implements IntegrationErrorDestinationBuilder {

    private final ConnectorProperties connectorProperties;

    public IntegrationErrorDestinationBuilderImpl(ConnectorProperties connectorProperties) {
        this.connectorProperties = connectorProperties;
    }

    @Override
    public String buildDestination(IntegrationRequest event) {
        String errorDestinationOverride = connectorProperties.getErrorDestinationOverride();

        String destination = (errorDestinationOverride == null || errorDestinationOverride.isEmpty())
                ? "integrationError" + connectorProperties.getMqDestinationSeparator() + event.getServiceFullName() : errorDestinationOverride;

        return destination;
    }
}
