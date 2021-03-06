/*
 * JBoss, Home of Professional Open Source
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.arquillian.container.jbossas.remote_5_1;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;

import org.jboss.arquillian.container.spi.client.container.DeployableContainer;
import org.jboss.arquillian.container.spi.client.container.DeploymentException;
import org.jboss.arquillian.container.spi.client.container.LifecycleException;
import org.jboss.arquillian.container.spi.client.protocol.ProtocolDescription;
import org.jboss.arquillian.container.spi.client.protocol.metadata.ProtocolMetaData;
import org.jboss.arquillian.container.spi.context.annotation.ContainerScoped;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.deployers.spi.management.deploy.DeploymentManager;
import org.jboss.deployers.spi.management.deploy.DeploymentProgress;
import org.jboss.deployers.spi.management.deploy.DeploymentStatus;
import org.jboss.profileservice.spi.ProfileKey;
import org.jboss.profileservice.spi.ProfileService;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.descriptor.api.Descriptor;
import org.jboss.virtual.VFS;

/**
 * JbossRemoteContainer
 *
 * @author <a href="mailto:aslak@conduct.no">Aslak Knutsen</a>
 * @version $Revision: $
 */
public class JBossASRemoteContainer implements DeployableContainer<JBossASConfiguration> {
    // ProfileKey name which supports hot deployment
    private static final String DEFAULT_PROFILE_KEY_NAME = "applications";

    private final List<String> failedUndeployments = new ArrayList<String>();
    private ProfileService profileService;
    private DeploymentManager deploymentManager;

    private JBossASConfiguration configuration;

    @Inject @ContainerScoped
    private InstanceProducer<Context> contextInst;

    @Override
    public ProtocolDescription getDefaultProtocol() {
        return new ProtocolDescription("Servlet 2.5");
    }

    @Override
    public Class<JBossASConfiguration> getConfigurationClass() {
        return JBossASConfiguration.class;
    }

    @Override
    public void setup(JBossASConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public void start() throws LifecycleException {
        try {
            initDeploymentManager();
        } catch (Exception e) {
            throw new LifecycleException("Could not connect to container", e);
        }
    }

    @Override
    public void stop() throws LifecycleException {
        try {
            removeFailedUnDeployments();
        } catch (Exception e) {
            throw new LifecycleException("Could not clean up", e);
        }
    }

    @Override
    public void deploy(Descriptor descriptor) throws DeploymentException {
        String deploymentName = descriptor.getDescriptorName();
        URL deploymentUrl = ShrinkWrapUtil.toURL(descriptor);

        deploy(deploymentName, deploymentUrl);
    }

    @Override
    public void undeploy(Descriptor descriptor) throws DeploymentException {
        undeploy(descriptor.getDescriptorName());
    }

    @Override
    public ProtocolMetaData deploy(final Archive<?> archive) throws DeploymentException {
        String deploymentName = archive.getName();
        URL deploymentUrl = ShrinkWrapUtil.toURL(archive);

        deploy(deploymentName, deploymentUrl);
        try {
            return ManagementViewParser.parse(deploymentName, profileService);
        } catch (Exception e) {
            throw new DeploymentException("Could not extract deployment metadata", e);
        }
    }

    @Override
    public void undeploy(final Archive<?> archive) throws DeploymentException {
        undeploy(archive.getName());
    }

    private void deploy(String deploymentName, URL url) throws DeploymentException {
        Exception failure = null;
        try {
            DeploymentProgress distribute = deploymentManager.distribute(deploymentName, url, true);
            distribute.run();
            DeploymentStatus uploadStatus = distribute.getDeploymentStatus();
            if (uploadStatus.isFailed()) {
                failure = uploadStatus.getFailure();
                undeploy(deploymentName);
            } else {
                DeploymentProgress progress = deploymentManager.start(deploymentName);
                progress.run();
                DeploymentStatus status = progress.getDeploymentStatus();
                if (status.isFailed()) {
                    failure = status.getFailure();
                    undeploy(deploymentName);
                }
            }
        } catch (Exception e) {
            throw new DeploymentException("Could not deploy " + deploymentName, e);
        }
        if (failure != null) {
            throw new DeploymentException("Failed to deploy " + deploymentName, failure);
        }
    }

    private void undeploy(String name) throws DeploymentException {
        try {
            DeploymentProgress stopProgress = deploymentManager.stop(name);
            stopProgress.run();

            DeploymentProgress undeployProgress = deploymentManager.remove(name);
            undeployProgress.run();
            if (undeployProgress.getDeploymentStatus().isFailed()) {
                failedUndeployments.add(name);
            }
        } catch (Exception e) {
            throw new DeploymentException("Could not undeploy " + name, e);
        }
    }

    private void initDeploymentManager() throws Exception {
        Context ctx = createContext();
        profileService = (ProfileService) ctx.lookup("ProfileService");
        deploymentManager = profileService.getDeploymentManager();

        ProfileKey defaultKey = new ProfileKey(DEFAULT_PROFILE_KEY_NAME);
        deploymentManager.loadProfile(defaultKey);
        VFS.init();
    }

    private Context createContext() throws Exception {
        if (contextInst.get() == null) {
            Properties props = new Properties();
            props.put(InitialContext.INITIAL_CONTEXT_FACTORY, configuration.getContextFactory());
            props.put(InitialContext.URL_PKG_PREFIXES, configuration.getUrlPkgPrefix());
            props.put(InitialContext.PROVIDER_URL, configuration.getProviderUrl());
            contextInst.set(new InitialContext(props));
        }
        return contextInst.get();
    }

    private void removeFailedUnDeployments() throws IOException {
        List<String> remainingDeployments = new ArrayList<String>();
        for (String name : failedUndeployments) {
            try {
                DeploymentProgress undeployProgress = deploymentManager.remove(name);
                undeployProgress.run();
                if (undeployProgress.getDeploymentStatus().isFailed()) {
                    remainingDeployments.add(name);
                }
            } catch (Exception e) {
                IOException ioe = new IOException();
                ioe.initCause(e);
                throw ioe;
            }
        }
        if (remainingDeployments.size() > 0) {
            //log.error("Failed to undeploy these artifacts: " + remainingDeployments);
        }
        failedUndeployments.clear();
    }
}
