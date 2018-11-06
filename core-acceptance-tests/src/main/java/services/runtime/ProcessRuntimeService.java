package services.runtime;

import feign.Headers;
import feign.Param;
import feign.RequestLine;
import org.activiti.api.process.model.ProcessDefinition;
import org.activiti.api.process.model.payloads.StartProcessPayload;
import org.activiti.cloud.api.process.model.CloudProcessInstance;
import org.springframework.hateoas.PagedResources;
import org.activiti.cloud.qa.service.BaseService;

public interface ProcessRuntimeService extends BaseService {

    @RequestLine("POST /v1/process-instances")
    @Headers("Content-Type: application/json")
    CloudProcessInstance startProcess(StartProcessPayload startProcess);

    @RequestLine("POST /v1/process-instances/{id}/suspend")
    @Headers("Content-Type: application/json")
    void suspendProcess(@Param("id") String id);

    @RequestLine("POST /v1/process-instances/{id}/activate")
    @Headers("Content-Type: application/json")
    void activateProcess(@Param("id") String id);

    @RequestLine("DELETE /v1/process-instances/{id}")
    @Headers("Content-Type: application/json")
    void deleteProcess(@Param("id") String id);

    @RequestLine("GET /v1/process-instances?sort=startDate,desc&sort=id,desc")
    @Headers("Content-Type: application/json")
    PagedResources<CloudProcessInstance> getAllProcessInstances();

    @RequestLine("GET /v1/process-instances/{id}")
    @Headers("Content-Type: application/json")
    CloudProcessInstance getProcessInstance(@Param("id") String id);

    @RequestLine("GET /v1/process-definitions")
    @Headers("Content-Type: application/json")
    PagedResources<ProcessDefinition> getProcessDefinitions();

    @RequestLine("GET /v1/process-definitions/{processDefinitionKey}")
    @Headers("Content-Type: application/json")
    ProcessDefinition getProcessDefinitionByKey(@Param("processDefinitionKey") String processDefinitionKey);
}
