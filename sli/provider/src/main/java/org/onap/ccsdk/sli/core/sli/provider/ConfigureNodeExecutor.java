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
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.onap.ccsdk.sli.core.sli.SvcLogicAdaptor;
import org.onap.ccsdk.sli.core.sli.SvcLogicContext;
import org.onap.ccsdk.sli.core.sli.SvcLogicException;
import org.onap.ccsdk.sli.core.sli.SvcLogicExpression;
import org.onap.ccsdk.sli.core.sli.SvcLogicNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigureNodeExecutor extends SvcLogicNodeExecutor {
	private static final Logger LOG = LoggerFactory
			.getLogger(ConfigureNodeExecutor.class);

	public SvcLogicNode execute(SvcLogicServiceImpl svc, SvcLogicNode node,
			SvcLogicContext ctx) throws SvcLogicException {

		String adaptorName = SvcLogicExpressionResolver.evaluate(
				node.getAttribute("adaptor"), node, ctx);
		String outValue = "failure";

		if (LOG.isDebugEnabled()) {
			LOG.debug("configure node encountered - looking for adaptor "
					+ adaptorName);
		}

		SvcLogicAdaptor adaptor = getAdaptor(adaptorName);

		if (adaptor != null) {
			String activate = SvcLogicExpressionResolver.evaluate(
					node.getAttribute("activate"), node, ctx);
			String key = SvcLogicExpressionResolver.evaluate(
					node.getAttribute("key"), node, ctx);

			Map<String, String> parmMap = new HashMap<String, String>();

			Set<Map.Entry<String, SvcLogicExpression>> parmSet = node
					.getParameterSet();
			boolean hasParms = false;

			for (Iterator<Map.Entry<String, SvcLogicExpression>> iter = parmSet
					.iterator(); iter.hasNext();) {
				hasParms = true;
				Map.Entry<String, SvcLogicExpression> curEnt = iter.next();
				String curName = curEnt.getKey();
				SvcLogicExpression curExpr = curEnt.getValue();
				String curExprValue = SvcLogicExpressionResolver.evaluate(curExpr, node, ctx);
				
				LOG.debug("Parameter "+curName+" = "+curExpr.asParsedExpr()+" resolves to "+curExprValue);

				parmMap.put(curName,curExprValue);
			}

			if (hasParms) {
				SvcLogicAdaptor.ConfigStatus confStatus = SvcLogicAdaptor.ConfigStatus.FAILURE;
				
				try {
					confStatus = adaptor.configure(key, parmMap, ctx);
				} catch (Exception e) {
					LOG.warn("Caught exception from "+adaptorName+".configure", e);
					confStatus = SvcLogicAdaptor.ConfigStatus.FAILURE;
				}
				
				switch (confStatus) {
				case SUCCESS:
					outValue = "success";
					if ((activate != null) && (activate.length() > 0)) {
						if ("true".equalsIgnoreCase(activate)) {
							SvcLogicAdaptor.ConfigStatus activateStatus = SvcLogicAdaptor.ConfigStatus.FAILURE;
							
							try {
								activateStatus = adaptor.activate(key, ctx);
							} catch (Exception e) {

								LOG.warn("Caught exception from "+adaptorName+".activate", e);
								activateStatus = SvcLogicAdaptor.ConfigStatus.FAILURE;
							}
							switch (activateStatus) {
							case SUCCESS:
								break;
							case ALREADY_ACTIVE:
								outValue = "already-active";
								break;
							case NOT_FOUND:
								outValue = "not-found";
								break;
							case NOT_READY:
								outValue = "not-ready";
								break;
							case FAILURE:
							default:
								outValue = "failure";
							}
						} else if ("false".equalsIgnoreCase(activate)) {
							SvcLogicAdaptor.ConfigStatus deactivateStatus = SvcLogicAdaptor.ConfigStatus.FAILURE;
							
							try {
								deactivateStatus = adaptor.deactivate(key, ctx);
							} catch (Exception e) {

								LOG.warn("Caught exception from "+adaptorName+".deactivate", e);
								deactivateStatus = SvcLogicAdaptor.ConfigStatus.FAILURE;
							}
							switch (deactivateStatus) {
							case SUCCESS:
								break;
							case ALREADY_ACTIVE:
								outValue = "already-active";
								break;
							case NOT_FOUND:
								outValue = "not-found";
								break;
							case NOT_READY:
								outValue = "not-ready";
								break;
							case FAILURE:
							default:
								outValue = "failure";
							}
						}
					}
					break;
				case ALREADY_ACTIVE:
					outValue = "already-active";
					break;
				case NOT_FOUND:
					outValue = "not-found";
					break;
				case NOT_READY:
					outValue = "not-ready";
					break;
				case FAILURE:
				default:
					outValue = "failure";
				}
			} else {
				if ((activate != null) && (activate.length() > 0)) {
					if ("true".equalsIgnoreCase(activate)) {
						SvcLogicAdaptor.ConfigStatus activateStatus = SvcLogicAdaptor.ConfigStatus.FAILURE;
						try {
							activateStatus = adaptor.activate(key, ctx);
						} catch (Exception e) {
							LOG.warn("Caught exception from "+adaptorName+".activate", e);
							activateStatus = SvcLogicAdaptor.ConfigStatus.FAILURE;
						}
						switch (activateStatus) {
						case SUCCESS:
							outValue = "success";
							break;
						case ALREADY_ACTIVE:
							outValue = "already-active";
							break;
						case NOT_FOUND:
							outValue = "not-found";
							break;
						case NOT_READY:
							outValue = "not-ready";
							break;
						case FAILURE:
						default:
							outValue = "failure";
						}
					} else if ("false".equalsIgnoreCase(activate)) {
						SvcLogicAdaptor.ConfigStatus deactivateStatus = SvcLogicAdaptor.ConfigStatus.FAILURE;
						
						try {
							deactivateStatus = adaptor.deactivate(key, ctx);
						} catch (Exception e) {
							LOG.warn("Caught exception from "+adaptorName+".deactivate", e);
							deactivateStatus = SvcLogicAdaptor.ConfigStatus.FAILURE;
						}
						switch (deactivateStatus) {
						case SUCCESS:
							outValue = "success";
							break;
						case ALREADY_ACTIVE:
							outValue = "already-active";
							break;
						case NOT_FOUND:
							outValue = "not-found";
							break;
						case NOT_READY:
							outValue = "not-ready";
							break;
						case FAILURE:
						default:
							outValue = "failure";
						}
					}
				} else {
					LOG.warn("Nothing to configure - no parameters passed, and activate attribute is not set");
					outValue = "success";
				}
			}
		} else {
			if (LOG.isWarnEnabled()) {
				LOG.warn("Adaptor for " + adaptorName + " not found");
			}
		}
        return (getNextNode(node, outValue));
	}

}
