/*
 * Copyright 2018 Alfresco, Inc. and/or its affiliates.
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

package org.activiti.cloud.services.audit.jpa.events;

import org.activiti.cloud.api.process.model.events.CloudBPMNActivityCancelledEvent;

import java.util.Objects;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import java.util.Objects;

@Entity(name = ActivityCancelledAuditEventEntity.ACTIVITY_CANCELLED_EVENT)
@DiscriminatorValue(value = ActivityCancelledAuditEventEntity.ACTIVITY_CANCELLED_EVENT)
public class ActivityCancelledAuditEventEntity extends BPMNActivityAuditEventEntity {

    protected static final String ACTIVITY_CANCELLED_EVENT = "ActivityCancelledEvent";

    private String cause;

    public ActivityCancelledAuditEventEntity() {
    }

    public ActivityCancelledAuditEventEntity(CloudBPMNActivityCancelledEvent cloudEvent) {
        super(cloudEvent);
        this.cause = cloudEvent.getCause();
    }
    
    public String getCause() {
        return cause;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + Objects.hash(cause);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ActivityCancelledAuditEventEntity other = (ActivityCancelledAuditEventEntity) obj;
        return Objects.equals(cause, other.cause);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ActivityCancelledAuditEventEntity [cause=")
               .append(cause)
               .append(", toString()=")
               .append(super.toString())
               .append("]");
        return builder.toString();
    }
}
