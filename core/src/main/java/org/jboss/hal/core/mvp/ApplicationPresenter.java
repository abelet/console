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
package org.jboss.hal.core.mvp;

import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.proxy.ProxyPlace;
import org.jboss.hal.core.finder.Finder;
import org.jboss.hal.core.finder.FinderColumn;
import org.jboss.hal.core.finder.FinderContextEvent;
import org.jboss.hal.core.finder.FinderPath;
import org.jboss.hal.core.finder.FinderSegment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for all application presenters displayed in {@link Slots#MAIN}.
 *
 * @author Harald Pehl
 */
public abstract class ApplicationPresenter<V extends PatternFlyView, Proxy_ extends ProxyPlace<?>>
        extends PatternFlyPresenter<V, Proxy_> {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationPresenter.class);

    protected final Finder finder;

    public ApplicationPresenter(final EventBus eventBus, final V view, final Proxy_ proxy, final Finder finder) {
        super(eventBus, view, proxy, Slots.MAIN);
        this.finder = finder;
    }

    @Override
    protected void onReset() {
        super.onReset();
        updateBreadcrumb();
    }

    /**
     * Application presenters need to provide information about their path in the finder. Normally this path is
     * updated automatically when navigating in the finder. However since application presenters can also be revealed
     * directly using the breadcrumb dropdown, this information is crucial to restore the path in the finder context.
     * <p>
     * If this method returns {@code null} the path in the finder context is not touched.
     */
    protected abstract FinderPath finderPath();

    private void updateBreadcrumb() {
        FinderPath applicationPath = finderPath();
        if (applicationPath != null) {
            // try to connect segments with existing columns from the finder
            for (FinderSegment segment : applicationPath) {
                FinderColumn column = finder.getColumn(segment.getKey());
                if (column != null) {
                    //noinspection unchecked
                    segment.connect(column);
                } else {
                    //noinspection HardCodedStringLiteral
                    logger.warn("Unable to find column '{}' to connect breadcrumb segment '{}' for token '{}'",
                            segment.getKey(), segment, getProxy().getNameToken());
                }
            }
            finder.getContext().reset(applicationPath);
        }
        // The breadcrumb is part of the header. Notify the the header presenter to take care of updating the breadcrumb
        getEventBus().fireEvent(new FinderContextEvent(finder.getContext()));
    }
}