/*-
 * ============LICENSE_START=======================================================
 * ONAP : CCSDK
 * ================================================================================
 * Copyright (C) 2017 AT&T Intellectual Property. All rights
 * 						reserved.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END=========================================================
 */

package org.onap.ccsdk.sli.core.sli.provider;

import java.util.HashMap;
import java.util.Properties;

import org.onap.ccsdk.sli.core.sli.MetricLogger;
import org.onap.ccsdk.sli.core.sli.SvcLogicContext;
import org.onap.ccsdk.sli.core.sli.SvcLogicException;
import org.onap.ccsdk.sli.core.sli.SvcLogicGraph;
import org.onap.ccsdk.sli.core.sli.SvcLogicNode;
import org.onap.ccsdk.sli.core.sli.SvcLogicStore;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

public class SvcLogicServiceImpl implements SvcLogicService {

    private static final Logger LOG = LoggerFactory
            .getLogger(SvcLogicServiceImpl.class);

    private HashMap<String, SvcLogicNodeExecutor> nodeExecutors = null;

    private BundleContext bctx = null;

    private void registerExecutors() {

        LOG.info("Entered register executors");
        if (bctx == null) {
            bctx = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
        }

        if (nodeExecutors == null) {
            nodeExecutors = new HashMap<>();
        }

        LOG.info("Opening service tracker");
        ServiceTracker tracker = new ServiceTracker(bctx, SvcLogicNodeExecutor.class.getName(), null);

        tracker.open();

        ServiceListener listener = new ServiceListener() {

            public void serviceChanged(ServiceEvent ev) {
                ServiceReference sr = ev.getServiceReference();
                switch (ev.getType()) {
                case ServiceEvent.REGISTERED: {
                    registerExecutor(sr);
                }
                    break;
                case ServiceEvent.UNREGISTERING: {
                    unregisterExecutor(sr);
                }
                    break;
                }
            }
        };

        LOG.info("Adding service listener");
        String filter = "(objectclass=" + SvcLogicNodeExecutor.class.getName() + ")";
        try {
            bctx.addServiceListener(listener, filter);
            ServiceReference[] srl = bctx.getServiceReferences(SvcLogicNodeExecutor.class.getName(), null);
            for (int i = 0; srl != null && i < srl.length; i++) {
                listener.serviceChanged(new ServiceEvent(ServiceEvent.REGISTERED, srl[i]));
            }
        } catch (InvalidSyntaxException e) {
            LOG.info("Invalid syntax", e);
        }
        LOG.info("Done registerExecutors");
    }

    public void registerExecutor(ServiceReference sr) {

        String nodeName = (String) sr.getProperty("nodeType");
        if (nodeName != null) {

            SvcLogicNodeExecutor executor;

            try {
                executor = (SvcLogicNodeExecutor) bctx.getService(sr);
            } catch (Exception e) {
                LOG.error("Cannot get service executor for {}", nodeName, e);
                return;
            }
            registerExecutor(nodeName, executor);
        }
    }

    public void registerExecutor(String nodeName, SvcLogicNodeExecutor executor)
    {
        if (nodeExecutors == null) {
            nodeExecutors = new HashMap<>();
        }
        LOG.info("SLI - registering executor for node type {}", nodeName);
        nodeExecutors.put(nodeName, executor);
    }

    public void unregisterExecutor(ServiceReference sr) {
        String nodeName = (String) sr.getProperty("nodeType");

        if (nodeName != null) {
             unregisterExecutor(nodeName);
        }
    }

    public void unregisterExecutor(String nodeName) {
        LOG.info("SLI - unregistering executor for node type {}", nodeName);
        nodeExecutors.remove(nodeName);
    }

    public SvcLogicContext execute(SvcLogicGraph graph, SvcLogicContext ctx) throws SvcLogicException {

        if (nodeExecutors == null) {
            registerExecutors();
        }

        // Set service name in MDC to reference current working directed graph
        MDC.put(MetricLogger.SERVICE_NAME, graph.getModule()+":"+graph.getRpc()+"/v"+graph.getVersion());

        MDC.put("currentGraph", graph.toString());

        SvcLogicNode curNode = graph.getRootNode();
        LOG.info("About to execute graph {}", graph.toString());

        while (curNode != null) {
            MDC.put("nodeId", curNode.getNodeId()+" ("+curNode.getNodeType()+")");
            LOG.info("About to execute node # {} ({})", curNode.getNodeId(), curNode.getNodeType());

            SvcLogicNode nextNode = executeNode(curNode, ctx);
            curNode = nextNode;
        }
        MDC.remove("nodeId");
        MDC.remove("currentGraph");

        return (ctx);
    }

    public SvcLogicNode executeNode(SvcLogicNode node, SvcLogicContext ctx) throws SvcLogicException {
        if (node == null) {
            return (null);
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Executing node {}", node.getNodeId());
        }

        SvcLogicNodeExecutor executor = nodeExecutors.get(node.getNodeType());

        if (executor != null) {
            LOG.debug("Executing node executor for node type {} - {}",
                node.getNodeType(), executor.getClass().getName());
            return (executor.execute(this, node, ctx));
        } else {
            if (LOG.isDebugEnabled()) {
                LOG.debug("{} node not implemented", node.getNodeType());
            }
            SvcLogicNode nextNode = node.getOutcomeValue("failure");
            if (nextNode != null) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("about to execute failure branch");
                }
                return (nextNode);
            }

            nextNode = node.getOutcomeValue("Other");
            if (nextNode != null) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("about to execute Other branch");
                }
            } else {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("no failure or Other branch found");
                }
            }
            return (nextNode);
        }

    }

    @Override
    public boolean hasGraph(String module, String rpc, String version, String mode) throws SvcLogicException
    {
        SvcLogicStore store = SvcLogicActivator.getStore();

        return (store.hasGraph(module, rpc, version, mode));
    }

    @Override
    public Properties execute(String module, String rpc, String version, String mode, Properties props)
            throws SvcLogicException {
        return(execute(module, rpc, version, mode, props, null));
    }


    @Override
    public Properties execute(String module, String rpc, String version, String mode,
            Properties props, DOMDataBroker domDataBroker) throws SvcLogicException {

        // See if there is a service logic defined
        SvcLogicStore store = SvcLogicActivator.getStore();

        LOG.info("Fetching service logic from data store");
        SvcLogicGraph graph = store.fetch(module, rpc, version, mode);

        if (graph == null)
        {
            Properties retProps = new Properties();
            retProps.setProperty("error-code", "401");
            retProps.setProperty("error-message", "No service logic found for ["+module+","+rpc+","+version+","+mode+"]");
            return(retProps);
        }

        SvcLogicContext ctx = new SvcLogicContext(props);
        ctx.setAttribute("currentGraph", graph.toString());
        ctx.setAttribute("X-ECOMP-RequestID", MDC.get("X-ECOMP-RequestID"));
        ctx.setDomDataBroker(domDataBroker);

        execute(graph, ctx);

        return(ctx.toProperties());
    }
}
