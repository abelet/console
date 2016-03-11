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
package org.jboss.hal.meta;

import com.google.gwt.inject.client.AbstractGinModule;
import com.google.inject.Singleton;
import org.jboss.hal.meta.capabilitiy.Capabilities;
import org.jboss.hal.meta.capabilitiy.RegisterCoreCapabilities;
import org.jboss.hal.meta.description.ResourceDescriptions;
import org.jboss.hal.meta.processing.MetadataProcessor;
import org.jboss.hal.meta.security.SecurityFramework;
import org.jboss.hal.meta.subsystem.Subsystems;
import org.jboss.hal.spi.GinModule;

/**
 * @author Harald Pehl
 */
@GinModule
public class MetaModule extends AbstractGinModule {

    @Override
    protected void configure() {
        bind(Capabilities.class).in(Singleton.class);
        bind(MetadataProcessor.class).in(Singleton.class);
        bind(ResourceDescriptions.class).in(Singleton.class);
        bind(SecurityFramework.class).in(Singleton.class);
        bind(Subsystems.class).in(Singleton.class);

        bind(RegisterCoreCapabilities.class).asEagerSingleton();
    }
}
