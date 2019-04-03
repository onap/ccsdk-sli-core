package org.onap.ccsdk.sli.core.lighty;

import io.lighty.core.controller.api.AbstractLightyModule;
import io.lighty.core.controller.api.LightyModule;
import java.util.concurrent.ExecutionException;
import org.onap.ccsdk.sli.core.dblib.DBLibConnection;
import org.onap.ccsdk.sli.core.dblib.lighty.DblibModule;
import org.onap.ccsdk.sli.core.sli.lighty.SliModule;
import org.onap.ccsdk.sli.core.sliapi.lighty.SliApiModule;
import org.onap.ccsdk.sli.core.slipluginutils.lighty.SliPluginUtilsModule;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.NotificationPublishService;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CcsdkCoreLightyModule extends AbstractLightyModule {

    private static final Logger LOG = LoggerFactory.getLogger(DBLibConnection.class);

    private DataBroker dataBroker;
    private NotificationPublishService notificationPublishService;
    private RpcProviderRegistry rpcProviderRegistry;

    private DblibModule dblibModule;
    private SliModule sliModule;
    private SliApiModule sliApiModule;
    private SliPluginUtilsModule sliPluginUtilsModule;

    public CcsdkCoreLightyModule(DataBroker dataBroker, NotificationPublishService notificationPublishService,
            RpcProviderRegistry rpcProviderRegistry) {
        this.dataBroker = dataBroker;
        this.notificationPublishService = notificationPublishService;
        this.rpcProviderRegistry = rpcProviderRegistry;
    }

    protected boolean initProcedure() {
        LOG.debug("Initializing CCSDK Core Lighty module...");
        this.dblibModule = new DblibModule();
        if (!startLightyModule(dblibModule)) {
            LOG.error("Unable to start DblibModule in CCSDK Core Lighty module!");
            return false;
        }

        this.sliModule = new SliModule(dblibModule.getDBResourceManager());
        if (!startLightyModule(sliModule)) {
            LOG.error("Unable to start SliModule in CCSDK Core Lighty module!");
            return false;
        }

        this.sliApiModule = new SliApiModule(dataBroker, notificationPublishService, rpcProviderRegistry);
        if (!startLightyModule(sliApiModule)) {
            LOG.error("Unable to start SliApiModule in CCSDK Core Lighty module!");
            return false;
        }

        this.sliPluginUtilsModule = new SliPluginUtilsModule();
        if (!startLightyModule(sliPluginUtilsModule)) {
            LOG.error("Unable to start SliPluginUtilsModule in CCSDK Core Lighty module!");
            return false;
        }

        LOG.debug("CCSDK Core Lighty module was initialized successfully");
        return true;
    }

    protected boolean stopProcedure() {
        LOG.debug("Stopping CCSDK Core Lighty module...");

        boolean stopSuccessfull = true;

        if (!stopLightyModule(sliPluginUtilsModule)) {
            stopSuccessfull = false;
        }

        if (!stopLightyModule(sliApiModule)) {
            stopSuccessfull = false;
        }

        if (!stopLightyModule(sliModule)) {
            stopSuccessfull = false;
        }

        if (!stopLightyModule(dblibModule)) {
            stopSuccessfull = false;
        }

        if (stopSuccessfull) {
            LOG.debug("CCSDK Core Lighty module was stopped successfully");
        } else {
            LOG.error("CCSDK Core Lighty module was not stopped successfully!");
        }
        return stopSuccessfull;
    }

    private boolean startLightyModule(LightyModule lightyModule) {
        try {
            return lightyModule.start().get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Exception thrown while initializing {} in CCSDK Core Lighty module!", lightyModule.getClass(),
                    e);
            return false;
        }
    }

    private boolean stopLightyModule(LightyModule lightyModule) {
        try {
            if (!lightyModule.shutdown().get()) {
                LOG.error("{} was not stopped successfully in CCSDK Core Lighty module!", lightyModule.getClass());
                return false;
            } else {
                return true;
            }
        } catch (Exception e) {
            LOG.error("Exception thrown while shutting down {} in CCSDK Core Lighty module!", lightyModule.getClass(),
                    e);
            return false;
        }
    }

    public DblibModule getDblibModule() {
        return dblibModule;
    }

    public SliModule getSliModule() {
        return sliModule;
    }

    public SliApiModule getSliApiModule() {
        return sliApiModule;
    }

    public SliPluginUtilsModule getSliPluginUtilsModule() {
        return sliPluginUtilsModule;
    }
}
