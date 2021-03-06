package com.sptracer.impl;

import com.sptracer.ReporterConfiguration;
import com.sptracer.configuration.ConfigurationRegistry;
import com.sptracer.configuration.CoreConfiguration;
import com.sptracer.util.ExecutorUtils;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class MetaData {

    /**
     * Service
     * (Required)
     */
    private final Service service;
    /**
     * Process
     */
    private final ProcessInfo process;
    /**
     * System
     */
    private final SystemInfo system;

    /**
     * Cloud provider metadata
     */
    @Nullable
    private final CloudProviderInfo cloudProviderInfo;

    private final ArrayList<String> globalLabelKeys;
    private final ArrayList<String> globalLabelValues;

    MetaData(ProcessInfo process, Service service, SystemInfo system, @Nullable CloudProviderInfo cloudProviderInfo,
             Map<String, String> globalLabels) {
        this.process = process;
        this.service = service;
        this.system = system;
        this.cloudProviderInfo = cloudProviderInfo;
        globalLabelKeys = new ArrayList<>(globalLabels.keySet());
        globalLabelValues = new ArrayList<>(globalLabels.values());
    }

    public static Future<MetaData> create(ConfigurationRegistry configurationRegistry, @Nullable String ephemeralId) {
        if (ephemeralId == null) {
            ephemeralId = UUID.randomUUID().toString();
        }
        final CoreConfiguration coreConfiguration = configurationRegistry.getConfig(CoreConfiguration.class);
        final Service service = new ServiceFactory().createService(coreConfiguration, ephemeralId);
        final ProcessInfo processInformation = ProcessFactory.ForCurrentVM.INSTANCE.getProcessInformation();
        if (!configurationRegistry.getConfig(ReporterConfiguration.class).isIncludeProcessArguments()) {
            processInformation.getArgv().clear();
        }
        final SystemInfo system = SystemInfo.create(coreConfiguration.getHostname());

        final CoreConfiguration.CloudProvider cloudProvider = coreConfiguration.getCloudProvider();
        if (cloudProvider == CoreConfiguration.CloudProvider.NONE) {
            MetaData metaData = new MetaData(
                    processInformation,
                    service,
                    system,
                    null,
                    coreConfiguration.getGlobalLabels()
            );
            return new NoWaitFuture(metaData);
        }

        final int cloudDiscoveryTimeoutMs = (int) coreConfiguration.geCloudMetadataDiscoveryTimeoutMs();
        ThreadPoolExecutor executor = ExecutorUtils.createSingleThreadDaemonPool("metadata", 1);
        try {
            return executor.submit(new Callable<MetaData>() {
                @Override
                public MetaData call() {
                    // This call is blocking on outgoing HTTP connections
                    CloudProviderInfo cloudProviderInfo = CloudMetadataProvider.fetchAndParseCloudProviderInfo(cloudProvider, cloudDiscoveryTimeoutMs);
                    return new MetaData(
                            processInformation,
                            service,
                            system,
                            cloudProviderInfo,
                            coreConfiguration.getGlobalLabels()
                    );
                }
            });
        } finally {
            executor.shutdown();
        }
    }

    /**
     * Service
     * (Required)
     *
     * @return the service name
     */
    public Service getService() {
        return service;
    }

    /**
     * Process
     *
     * @return the process name
     */
    public ProcessInfo getProcess() {
        return process;
    }

    /**
     * System
     *
     * @return the system name
     */
    public SystemInfo getSystem() {
        return system;
    }

    public ArrayList<String> getGlobalLabelKeys() {
        return globalLabelKeys;
    }

    public ArrayList<String> getGlobalLabelValues() {
        return globalLabelValues;
    }

    @Nullable
    public CloudProviderInfo getCloudProviderInfo() {
        return cloudProviderInfo;
    }

    static class NoWaitFuture implements Future<MetaData> {

        private final MetaData metaData;

        NoWaitFuture(MetaData metaData) {
            this.metaData = metaData;
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return false;
        }

        @Override
        public boolean isCancelled() {
            return false;
        }

        @Override
        public boolean isDone() {
            return false;
        }

        @Override
        public MetaData get() {
            return metaData;
        }

        @Override
        public MetaData get(long timeout, TimeUnit unit) {
            return metaData;
        }
    }
}
