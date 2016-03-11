/*
 * Copyright 2015-2016 Red Hat, Inc, and individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.hal.client.deployment;

import org.jboss.hal.dmr.ModelNode;
import org.jboss.hal.dmr.model.NamedNode;

import java.util.ArrayList;
import java.util.List;

/**
 * An uploaded deployment blob.
 *
 * @author Harald Pehl
 */
public class Content extends NamedNode {

    private final List<Assignment> assignments;

    public Content(final ModelNode node) {
        super(node);
        this.assignments = new ArrayList<>();
    }

    public String getRuntimeName() {
        ModelNode runtimeName = get("runtime-name");
        return runtimeName.isDefined() ? runtimeName.asString() : null;
    }

    public void addAssignment(Assignment assignment) {
        assignments.add(assignment);
    }

    public List<Assignment> getAssignments() {
        return assignments;
    }

    @Override
    public String toString() {
        return "Content{" + getName() + ", assigned to " + assignments + "}";
    }
}
