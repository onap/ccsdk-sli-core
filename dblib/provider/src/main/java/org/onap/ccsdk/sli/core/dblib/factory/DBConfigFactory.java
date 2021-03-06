/*-
 * ============LICENSE_START=======================================================
 * onap
 * ================================================================================
 * Copyright (C) 2016 - 2017 ONAP
 * ================================================================================
 * Modifications Copyright (C) 2018 IBM.
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

package org.onap.ccsdk.sli.core.dblib.factory;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;
import org.onap.ccsdk.sli.core.dblib.config.BaseDBConfiguration;
import org.onap.ccsdk.sli.core.dblib.config.DbConfigPool;
import org.onap.ccsdk.sli.core.dblib.config.JDBCConfiguration;
import org.slf4j.LoggerFactory;

/**
 * @version $Revision: 1.1 $
 * Change Log
 * Author         Date     Comments
 * ============== ======== ====================================================
 * Rich Tabedzki  01/17/08 Initial version
 */
public class DBConfigFactory {

    public static DbConfigPool createConfig(Properties resource) {
        return getConfigparams(resource);
    }

    static DbConfigPool getConfigparams(Properties properties) {
        DbConfigPool xmlConfig = new DbConfigPool(properties);
        ArrayList<Properties> propertySets = new ArrayList<>();

        if ("JDBC".equalsIgnoreCase(xmlConfig.getType())) {
            String hosts = properties.getProperty(BaseDBConfiguration.DATABASE_HOSTS);
            if (hosts == null || hosts.isEmpty()) {
                propertySets.add(properties);
            } else {
                setPropertyWhenHostsNonEmpty(hosts, properties, propertySets);
            }
        } else {
            propertySets.add(properties);
        }
        try {
            Iterator<Properties> it = propertySets.iterator();
            while (it.hasNext()) {
                BaseDBConfiguration config = parse(it.next());
                xmlConfig.addConfiguration(config);
            }
        } catch (Exception e) {
            LoggerFactory.getLogger(DBConfigFactory.class).warn("", e);
        }

        return xmlConfig;
    }

    private static void setPropertyWhenHostsNonEmpty(String hosts, Properties properties, ArrayList<Properties>
        propertySets) {
        String[] newhost = hosts.split(",");
        for (String aNewhost : newhost) {
            Properties localSet = new Properties();
            localSet.putAll(properties);
            String url = localSet.getProperty(BaseDBConfiguration.DATABASE_URL);
            if (url.contains("DBHOST")) {
                url = url.replace("DBHOST", aNewhost);
            }
            if (url.contains("dbhost")) {
                url = url.replace("dbhost", aNewhost);
            }
            localSet.setProperty(BaseDBConfiguration.DATABASE_URL, url);
            localSet.setProperty(BaseDBConfiguration.CONNECTION_NAME, aNewhost);
            propertySets.add(localSet);
        }
    }

    public static BaseDBConfiguration parse(Properties props) throws Exception {

        String type = props.getProperty(BaseDBConfiguration.DATABASE_TYPE);

        BaseDBConfiguration config = null;

        if ("JDBC".equalsIgnoreCase(type)) {
            config = new JDBCConfiguration(props);
        }

        return config;
    }
}
