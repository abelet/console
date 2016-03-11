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
package org.jboss.hal.client.bootstrap.functions;

import com.google.gwt.core.client.GWT;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.proxy.PlaceManager;
import org.jboss.gwt.flow.Control;
import org.jboss.gwt.flow.FunctionContext;
import org.jboss.hal.resources.Resources;
import org.jboss.hal.spi.Message;
import org.jboss.hal.spi.MessageEvent;

import javax.inject.Inject;

/**
 * @author Harald Pehl
 */
public class FinishBootstrap implements BootstrapFunction {

    private final EventBus eventBus;
    private final PlaceManager placeManager;
    private final Resources resources;

    @Inject
    public FinishBootstrap(EventBus eventBus,
            PlaceManager placeManager,
            Resources resources) {
        this.eventBus = eventBus;
        this.placeManager = placeManager;
        this.resources = resources;
    }

    @Override
    public void execute(final Control<FunctionContext> control) {
        // reset the uncaught exception handler setup in HalPreBootstrapper
        GWT.setUncaughtExceptionHandler(e -> {
            logger.error("Uncaught exception: {}", e.getMessage()); //NON-NLS
            placeManager.unlock();
            eventBus.fireEvent(new MessageEvent(Message.error(resources.constants().unknownError(), e.getMessage())));
        });
        control.proceed();
    }

    @Override
    public String name() {
        return "Bootstrap[FinishBootstrap]";
    }
}
