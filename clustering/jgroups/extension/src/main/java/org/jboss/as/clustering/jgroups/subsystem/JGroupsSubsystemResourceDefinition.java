/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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
package org.jboss.as.clustering.jgroups.subsystem;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.UnaryOperator;

import org.jboss.as.clustering.controller.Capability;
import org.jboss.as.clustering.controller.CapabilityReference;
import org.jboss.as.clustering.controller.DefaultSubsystemDescribeHandler;
import org.jboss.as.clustering.controller.ManagementResourceRegistration;
import org.jboss.as.clustering.controller.RequirementCapability;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.clustering.controller.SimpleResourceRegistration;
import org.jboss.as.clustering.controller.SubsystemRegistration;
import org.jboss.as.clustering.controller.SubsystemResourceDefinition;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.clustering.jgroups.spi.JGroupsRequirement;
import org.wildfly.clustering.server.service.ClusteringRequirement;

/**
 * The root resource of the JGroups subsystem.
 *
 * @author Richard Achmatowicz (c) 2012 Red Hat Inc.
 */
public class JGroupsSubsystemResourceDefinition extends SubsystemResourceDefinition<SubsystemRegistration> {

    public static final PathElement PATH = pathElement(JGroupsExtension.SUBSYSTEM_NAME);

    static final Map<JGroupsRequirement, Capability> CAPABILITIES = new EnumMap<>(JGroupsRequirement.class);
    static {
        for (JGroupsRequirement requirement : EnumSet.allOf(JGroupsRequirement.class)) {
            CAPABILITIES.put(requirement, new RequirementCapability(requirement.getDefaultRequirement()));
        }
    }

    public enum Attribute implements org.jboss.as.clustering.controller.Attribute, UnaryOperator<SimpleAttributeDefinitionBuilder> {
        DEFAULT_CHANNEL("default-channel", ModelType.STRING) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.setCapabilityReference(new CapabilityReference(CAPABILITIES.get(JGroupsRequirement.CHANNEL_FACTORY), JGroupsRequirement.CHANNEL_FACTORY));
            }
        },
        @Deprecated DEFAULT_STACK("default-stack", ModelType.STRING) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.setDeprecated(JGroupsModel.VERSION_3_0_0.getVersion());
            }
        },
        ;
        private final AttributeDefinition definition;

        Attribute(String name, ModelType type) {
            this.definition = this.apply(new SimpleAttributeDefinitionBuilder(name, type)
                    .setRequired(false)
                    .setAllowExpression(false)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .setXmlName(XMLAttribute.DEFAULT.getLocalName())
                    ).build();
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }
    }

    static class AddOperationTransformer implements UnaryOperator<OperationStepHandler> {
        @Override
        public OperationStepHandler apply(OperationStepHandler handler) {
            return new OperationStepHandler() {
                @Override
                public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                    // If this is a legacy configuration containing a default-stack, but no default-channel, then fabricate a default channel using the default stack
                    // This ensures that the default channel factory capability is available to the /subsystem=infinispan/cache-container=*/transport=jgroups resource, which requires them
                    // We can drop this compatibility workaround after we drop support for model version 3.0.
                    if (!operation.hasDefined(Attribute.DEFAULT_CHANNEL.getName()) && operation.hasDefined(Attribute.DEFAULT_STACK.getName())) {
                        String defaultChannel = "auto";
                        PathAddress channelAddress = context.getCurrentAddress().append(ChannelResourceDefinition.pathElement(defaultChannel));
                        ModelNode channelOperation = Util.createAddOperation(channelAddress);
                        channelOperation.get(ChannelResourceDefinition.Attribute.STACK.getName()).set(operation.get(Attribute.DEFAULT_STACK.getName()));
                        context.addStep(channelOperation, context.getRootResourceRegistration().getOperationHandler(channelAddress, ModelDescriptionConstants.ADD), OperationContext.Stage.MODEL);
                        operation.get(Attribute.DEFAULT_CHANNEL.getName()).set(new ModelNode(defaultChannel));
                    }
                    handler.execute(context, operation);
                }
            };
        }
    }

    JGroupsSubsystemResourceDefinition() {
        super(PATH, JGroupsExtension.SUBSYSTEM_RESOLVER);
    }

    @Override
    public void register(SubsystemRegistration parentRegistration) {
        ManagementResourceRegistration registration = parentRegistration.registerSubsystemModel(this);

        new DefaultSubsystemDescribeHandler().register(registration);

        Set<ClusteringRequirement> requirements = EnumSet.allOf(ClusteringRequirement.class);
        List<Capability> capabilities = new ArrayList<>(requirements.size());
        UnaryOperator<RuntimeCapability.Builder<Void>> configurator = builder -> builder.setAllowMultipleRegistrations(true);
        for (ClusteringRequirement requirement : requirements) {
            capabilities.add(new RequirementCapability(requirement.getDefaultRequirement(), configurator));
        }

        ResourceDescriptor descriptor = new ResourceDescriptor(this.getResourceDescriptionResolver())
                .addAttributes(Attribute.class)
                .addCapabilities(model -> model.hasDefined(Attribute.DEFAULT_CHANNEL.getName()), CAPABILITIES.values())
                .addCapabilities(model -> model.hasDefined(Attribute.DEFAULT_CHANNEL.getName()), capabilities)
                .setAddOperationTransformation(new AddOperationTransformer())
                ;
        ResourceServiceHandler handler = new JGroupsSubsystemServiceHandler();
        new SimpleResourceRegistration(descriptor, handler).register(registration);

        new ChannelResourceDefinition().register(registration);
        new StackResourceDefinition().register(registration);
    }
}
