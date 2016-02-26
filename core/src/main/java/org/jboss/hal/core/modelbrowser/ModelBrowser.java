/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.hal.core.modelbrowser;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.web.bindery.event.shared.EventBus;
import elemental.client.Browser;
import elemental.dom.Element;
import elemental.html.ButtonElement;
import org.jboss.gwt.elemento.core.Elements;
import org.jboss.gwt.elemento.core.HasElements;
import org.jboss.gwt.flow.Progress;
import org.jboss.hal.ballroom.IdBuilder;
import org.jboss.hal.ballroom.form.Form;
import org.jboss.hal.ballroom.layout.LayoutBuilder;
import org.jboss.hal.ballroom.tree.Node;
import org.jboss.hal.ballroom.tree.SelectionChangeHandler.SelectionContext;
import org.jboss.hal.ballroom.tree.Tree;
import org.jboss.hal.core.mbui.dialog.AddResourceDialog;
import org.jboss.hal.core.ui.Skeleton;
import org.jboss.hal.dmr.ModelNode;
import org.jboss.hal.dmr.Property;
import org.jboss.hal.dmr.dispatch.Dispatcher;
import org.jboss.hal.dmr.model.Composite;
import org.jboss.hal.dmr.model.CompositeResult;
import org.jboss.hal.dmr.model.Operation;
import org.jboss.hal.dmr.model.OperationFactory;
import org.jboss.hal.dmr.model.ResourceAddress;
import org.jboss.hal.meta.AddressTemplate;
import org.jboss.hal.meta.description.ResourceDescription;
import org.jboss.hal.meta.description.ResourceDescriptions;
import org.jboss.hal.meta.processing.MetadataProcessor;
import org.jboss.hal.meta.security.SecurityContext;
import org.jboss.hal.meta.security.SecurityFramework;
import org.jboss.hal.resources.CSS;
import org.jboss.hal.resources.Ids;
import org.jboss.hal.resources.Names;
import org.jboss.hal.resources.Resources;
import org.jboss.hal.spi.Footer;
import org.jboss.hal.spi.Message;
import org.jboss.hal.spi.MessageEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static elemental.css.CSSStyleDeclaration.Unit.PX;
import static java.util.Collections.singleton;
import static org.jboss.gwt.elemento.core.EventType.click;
import static org.jboss.hal.dmr.ModelDescriptionConstants.*;
import static org.jboss.hal.meta.StatementContext.Key.ANY_GROUP;
import static org.jboss.hal.meta.StatementContext.Key.ANY_PROFILE;
import static org.jboss.hal.resources.CSS.*;
import static org.jboss.hal.resources.Names.NYI;

/**
 * @author Harald Pehl
 */
public class ModelBrowser implements HasElements {

    private static final int MARGIN_BIG = 20; // keep this in sync with the
    private static final int MARGIN_SMALL = 10; // margins in modelbrowser.less
    private static final String FILTER_ELEMENT = "filterElement";
    private static final String REFRESH_ELEMENT = "refreshElement";
    static final String ROOT_ID = IdBuilder.build(Ids.MODEL_BROWSER, "root");
    static final Element PLACE_HOLDER_ELEMENT = Browser.getDocument().createDivElement();

    private static final Logger logger = LoggerFactory.getLogger(ModelBrowser.class);

    final MetadataProcessor metadataProcessor;
    final SecurityFramework securityFramework;
    final ResourceDescriptions resourceDescriptions;
    private final Dispatcher dispatcher;
    private final EventBus eventBus;
    private final Provider<Progress> progress;
    private final Resources resources;
    private final OperationFactory operationFactory;

    private final Iterable<Element> rows;
    private final Element buttonGroup;
    private final ButtonElement filter;
    private final ButtonElement refresh;
    private final Element treeContainer;
    private final Element content;
    private final ResourcePanel resourcePanel;
    private final ChildrenPanel childrenPanel;
    Tree<Context> tree;

    private boolean breadcrumb;


    // ------------------------------------------------------ ui setup

    @Inject
    public ModelBrowser(final MetadataProcessor metadataProcessor,
            final SecurityFramework securityFramework,
            final ResourceDescriptions resourceDescriptions,
            final Dispatcher dispatcher,
            final EventBus eventBus,
            @Footer final Provider<Progress> progress,
            final Resources resources) {

        this.metadataProcessor = metadataProcessor;
        this.securityFramework = securityFramework;
        this.resourceDescriptions = resourceDescriptions;
        this.dispatcher = dispatcher;
        this.eventBus = eventBus;
        this.progress = progress;
        this.resources = resources;
        this.operationFactory = new OperationFactory();

        Elements.Builder buttonsBuilder = new Elements.Builder()
                .div().css(btnGroup, modelBrowserButtons)
                .button().rememberAs(FILTER_ELEMENT).on(click, event -> onFilter()).css(btn, btnDefault).add("i")
                .css(fontAwesome(CSS.filter)).end()
                .button().rememberAs(REFRESH_ELEMENT).on(click, event -> onRefresh()).css(btn, btnDefault).add("i")
                .css(fontAwesome(CSS.refresh))
                .end()
                .end();
        filter = buttonsBuilder.referenceFor(FILTER_ELEMENT);
        refresh = buttonsBuilder.referenceFor(REFRESH_ELEMENT);
        buttonGroup = buttonsBuilder.build();
        treeContainer = new Elements.Builder().div().css(modelBrowserTree).end().build();
        content = new Elements.Builder().div().css(modelBrowserContent).end().build();

        resourcePanel = new ResourcePanel(this, dispatcher, resources);
        for (Element element : resourcePanel.asElements()) {
            content.appendChild(element);
        }
        resourcePanel.hide();

        childrenPanel = new ChildrenPanel(this, dispatcher, resources);
        for (Element element : childrenPanel.asElements()) {
            content.appendChild(element);
        }
        childrenPanel.hide();

        // @formatter:off
        rows =  new LayoutBuilder()
            .row()
                .column(0, 4)
                    .add(buttonGroup)
                    .add(treeContainer)
                .end()
                .column(0, 8)
                    .add(content)
                .end()
            .end()
        .elements();
        // @formatter:on

        Browser.getWindow().setOnresize(event -> adjustHeight());
    }

    private void adjustHeight() {
        int window = Browser.getWindow().getInnerHeight();
        int navigation = Skeleton.navigationHeight();
        int footer = Skeleton.footerHeight();
        int buttonGroup = this.buttonGroup.getOffsetHeight();
        if (navigation > 0 && footer > 0) {
            int height = window - navigation - footer;
            // keep this in sync with the margins in modelbrowser.less
            treeContainer.getStyle().setHeight(height - 2 * MARGIN_BIG - buttonGroup - 2 * MARGIN_SMALL, PX);
            content.getStyle().setHeight(height - 2 * MARGIN_BIG - 2 * MARGIN_SMALL, PX);
        }
    }

    private void updateBreadcrumb(Node<Context> node) {
        if (breadcrumb) {
            ModelBrowserPath path = new ModelBrowserPath(this, node);
            eventBus.fireEvent(new ModelBrowserPathEvent(path));
        }
    }

    private void showResourceView(Node<Context> node, ResourceAddress address) {
        Node<Context> parent = tree.api().getNode(node.parent);
        AddressTemplate template = asGenericTemplate(parent, address);
        metadataProcessor.process(Ids.MODEL_BROWSER, singleton(template), progress,
                new AsyncCallback<Void>() {
                    @Override
                    public void onFailure(final Throwable throwable) {
                        //noinspection HardCodedStringLiteral
                        logger.error(
                                "Unable to show resource for node {}({}). Error while processing metadata for {}: {}",
                                node.id, node.text, template, throwable.getMessage());
                    }

                    @Override
                    public void onSuccess(final Void aVoid) {
                        SecurityContext securityContext = securityFramework.lookup(template);
                        ResourceDescription description = resourceDescriptions.lookup(template);
                        resourcePanel.update(node, node.data.getAddress(), securityContext, description);
                        resourcePanel.show();
                    }
                });
    }

    private AddressTemplate asGenericTemplate(Node<Context> parent, ResourceAddress address) {
        StringBuilder builder = new StringBuilder();
        for (Iterator<Property> iterator = address.asPropertyList().iterator(); iterator.hasNext(); ) {
            Property property = iterator.next();
            String name = property.getName();

            if (PROFILE.equals(name)) {
                builder.append(ANY_PROFILE.variable());
            } else if (SERVER_GROUP.equals(name)) {
                builder.append(ANY_GROUP.variable());
            } else {
                builder.append(name).append("=");
                if (!iterator.hasNext() && parent != null && parent.data != null && !parent.data.hasSingletons()) {
                    builder.append("*");
                } else {
                    builder.append(property.getValue().asString());
                }
            }
            if (iterator.hasNext()) {
                builder.append("/");
            }
        }
        return AddressTemplate.of(builder.toString());
    }


    // ------------------------------------------------------ event handler

    private void onTreeSelection(SelectionContext<Context> context) {
        if ("ready".equals(context.action)) { //NON-NLS
            // only (de)selection events please
            return;
        }

        filter.setDisabled(context.selected.isEmpty() || !context.node.data.isFullyQualified());
        refresh.setDisabled(context.selected.isEmpty());

        resourcePanel.hide();
        childrenPanel.hide();
        if (context.selected.isEmpty()) {
            updateBreadcrumb(null);

        } else {
            updateBreadcrumb(context.node);

            ResourceAddress address = context.node.data.getAddress();
            if (context.node.data.isFullyQualified()) {
                showResourceView(context.node, address);

            } else {
                childrenPanel.update(context.node, address);
                childrenPanel.show();
            }
        }
    }

    private void onFilter() {
        Browser.getWindow().alert(NYI);
    }

    private void onRefresh() {
        Browser.getWindow().alert("Refresh " + NYI);
    }

    void onAdd(final Node<Context> parent, final List<String> children) {
        if (parent.data.hasSingletons()) {
            if (parent.data.getSingletons().size() == children.size()) {
                MessageEvent.fire(eventBus, Message.warning(resources.constants().allSingletonsExist()));
            } else {
                AddressTemplate template = asGenericTemplate(parent, parent.data.getAddress());
                NewSingletonWizard wizard = new NewSingletonWizard(this, template, parent, children, context -> {
                    ResourceAddress fq = parent.data.getAddress().getParent().add(parent.text, context.singleton);
                    Operation operation = new Operation.Builder(ADD, fq).payload(context.modelNode).build();
                    dispatcher.execute(operation, result -> {
                        MessageEvent.fire(eventBus,
                                Message.success(resources.messages().addResourceSuccess(context.singleton)));
                        onRefresh();
                    });
                });
                wizard.show();
            }

        } else {
            AddressTemplate template = asGenericTemplate(parent, parent.data.getAddress());
            metadataProcessor.process(Ids.MODEL_BROWSER, singleton(template), progress,
                    new AsyncCallback<Void>() {
                        @Override
                        public void onFailure(final Throwable throwable) {
                            //noinspection HardCodedStringLiteral
                            logger.error(
                                    "Unable to open add resource dialog for node {}({}). Error while processing metadata for {}: {}",
                                    parent.id, parent.text, template, throwable.getMessage());
                        }

                        @Override
                        public void onSuccess(final Void aVoid) {
                            SecurityContext securityContext = securityFramework.lookup(template);
                            ResourceDescription description = resourceDescriptions.lookup(template);
                            AddResourceDialog<ModelNode> dialog = new AddResourceDialog<>(
                                    IdBuilder.build(parent.id, "add"),
                                    resources.messages().addResourceTitle(parent.text),
                                    securityContext, description,
                                    (name, modelNode) -> {
                                        ResourceAddress fq = parent.data.getAddress().getParent()
                                                .add(parent.text, name);
                                        Operation operation = new Operation.Builder(ADD, fq).payload(modelNode)
                                                .build();
                                        dispatcher.execute(operation, result -> {
                                            MessageEvent.fire(eventBus,
                                                    Message.success(resources.messages().addResourceSuccess(name)));
                                            onRefresh();
                                        });
                                    });
                            dialog.show();
                        }
                    });
        }
    }

    void onRemove(ResourceAddress address) {
        Operation operation = new Operation.Builder(REMOVE, address).build();
        dispatcher.execute(operation, result -> {
            MessageEvent.fire(eventBus,
                    Message.success(resources.messages().removeResourceSuccess(address.lastValue())));
            onRefresh();
        });
    }

    void onReset(Form<ModelNode> form) {
        Browser.getWindow().alert(NYI);
    }

    void onSave(ResourceAddress address, Map<String, Object> changedValues) {
        Composite composite = operationFactory.fromChangeSet(address, changedValues);
        dispatcher.execute(composite, (CompositeResult result) -> {
            MessageEvent.fire(eventBus,
                    Message.success(resources.messages().modifyResourceSuccess(address.lastValue())));
            onRefresh();
        });
    }


    // ------------------------------------------------------ public API

    public void setRoot(ResourceAddress root, boolean breadcrumb) {
        this.breadcrumb = breadcrumb;
        String resource = root == ResourceAddress.ROOT ? Names.MANAGEMENT_MODEL : root.lastValue();
        if ("*".equals(resource)) {
            throw new IllegalArgumentException("Invalid root address: " + root +
                    ". ModelBrowser.setRoot() must be called with a concrete address.");
        }
        Context context = new Context(root, Collections.emptySet());
        Node<Context> rootNode = new Node.Builder<>(ROOT_ID, resource, context)
                .folder()
                .build();
        tree = new Tree<>(Ids.MODEL_BROWSER, rootNode, new ReadChildren(dispatcher));
        Elements.removeChildrenFrom(treeContainer);
        treeContainer.appendChild(tree.asElement());

        tree.attach();
        tree.onSelectionChange((event, selectionContext) -> onTreeSelection(selectionContext));
        tree.api().openNode(ROOT_ID, () -> resourcePanel.tabs.showTab(0));
        childrenPanel.attach();

        select(ROOT_ID, false);
        adjustHeight();
    }

    public void select(final String id, final boolean closeSelected) {
        tree.api().deselectAll(true);
        tree.api().selectNode(id, false, false);
        if (closeSelected) {
            tree.api().closeNode(id);
        }
        tree.asElement().focus();
    }

    @Override
    public Iterable<Element> asElements() {
        return rows;
    }
}