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
package org.jboss.hal.client.configuration.subsystem.microprofile;

import javax.inject.Inject;

import elemental2.dom.HTMLElement;
import org.jboss.hal.ballroom.form.Form;
import org.jboss.hal.core.mbui.form.ModelNodeForm;
import org.jboss.hal.core.mvp.HalViewImpl;
import org.jboss.hal.dmr.ModelNode;
import org.jboss.hal.meta.Metadata;
import org.jboss.hal.meta.MetadataRegistry;
import org.jboss.hal.resources.Ids;
import org.jboss.hal.resources.Names;

import static org.jboss.gwt.elemento.core.Elements.h;
import static org.jboss.gwt.elemento.core.Elements.p;
import static org.jboss.hal.ballroom.LayoutBuilder.column;
import static org.jboss.hal.ballroom.LayoutBuilder.row;
import static org.jboss.hal.client.configuration.subsystem.microprofile.AddressTemplates.MICRO_PROFILE_METRICS_TEMPLATE;

public class MicroProfileMetricsView extends HalViewImpl implements MicroProfileMetricsPresenter.MyView {

    private final Form<ModelNode> form;
    private MicroProfileMetricsPresenter presenter;

    @Inject
    public MicroProfileMetricsView(MetadataRegistry metadataRegistry) {
        Metadata metadata = metadataRegistry.lookup(MICRO_PROFILE_METRICS_TEMPLATE);
        form = new ModelNodeForm.Builder<>(Ids.MICRO_PROFILE_METRICS_FORM, metadata)
                .build();
        registerAttachable(form);

        HTMLElement root = row()
                .add(column()
                        .add(h(1).textContent(Names.MICROPROFILE_METRICS))
                        .add(p().textContent(metadata.getDescription().getDescription()))
                        .add(form))
                .asElement();
        initElement(root);
    }

    @Override
    public void setPresenter(MicroProfileMetricsPresenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public void update(ModelNode payload) {
        form.view(payload);
    }
}
