/*
 * Copyright 2019 Alfresco, Inc. and/or its affiliates.
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
package org.activiti.cloud.services.modeling.validation;

import java.io.IOException;
import java.io.InputStream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.everit.json.schema.loader.SchemaClient;
import org.everit.json.schema.loader.SchemaLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

@Configuration
public class JsonSchemaModelValidatorConfiguration {

    @Value("${activiti.validation.connector-schema:schema/connector-schema.json}")
    private String connectorSchema;

    @Value("${activiti.validation.process-extensions-schema:schema/process-extensions-schema.json}")
    private String processExtensionsSchema;

    @Value("${activiti.validation.model-extensions-schema:schema/model-extensions-schema.json}")
    private String modelExtensionsSchema;

    @Autowired
    private ObjectMapper mapper;

    @Bean(name = "connectorSchemaLoader")
    public SchemaLoader getConnectorSchemaLoader() throws IOException {
        return buildSchemaLoaderFromClasspath(connectorSchema);
    }

    @Bean(name = "processExtensionsSchemaLoader")
    public SchemaLoader getProcessExtensionsSchemaLoader() throws IOException {
        return buildSchemaLoaderFromClasspath(processExtensionsSchema);
    }

    @Bean(name = "modelExtensionsSchemaLoader")
    public SchemaLoader getModelExtensionsSchemaLoader() throws IOException {
        return buildSchemaLoaderFromClasspath(modelExtensionsSchema);
    }

    private SchemaLoader buildSchemaLoaderFromClasspath(String schemaFileName) throws IOException {
        try (InputStream schemaInputStream = new ClassPathResource(schemaFileName).getInputStream()) {
            ObjectNode jsonSchema =  mapper.readValue(schemaInputStream, ObjectNode.class);

            return SchemaLoader
                    .builder()
                    .schemaClient(SchemaClient.classPathAwareClient())
                    .schemaJson(new JsonSchemaFlattener().flatten(jsonSchema))
                    .draftV7Support()
                    .build();
        }
    }

}
