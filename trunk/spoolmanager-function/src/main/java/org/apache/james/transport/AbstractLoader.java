/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/


package org.apache.james.transport;
import java.util.List;
import java.util.Vector;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;


import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.logging.Log;
import org.apache.james.api.kernel.LoaderService;
import org.apache.mailet.MailetContext;
import org.apache.mailet.MailetException;

/**
 * Common services for loaders.
 */
public abstract class AbstractLoader {

    /**
     * The list of packages that may contain Mailets or matchers
     */
    protected Vector<String> packages;

    /**
     * Mailet context
     */
    protected MailetContext mailetContext;

    private LoaderService loaderService;

    private Log logger;

    private HierarchicalConfiguration config;

    
    /**
     * Gets the loader service used by this instance.
     * @return the loaderService 
     */
    public final LoaderService getLoaderService() {
        return loaderService;
    }

    /**
     * Sets the loader service used by this instance.
     * @param loaderService the loaderService to set, not null
     */
    @Resource(name="org.apache.james.LoaderService")
    public final void setLoaderService(LoaderService loaderService) {
        this.loaderService = loaderService;
    }

    @Resource(name="org.apache.commons.logging.Log")
    public final void setLogger(Log logger) {
        this.logger = logger;
    }
    

    @Resource(name="org.apache.commons.configuration.Configuration")
    public final void setConfiguration(HierarchicalConfiguration config) {
        this.config = config;
    }
    /**
     * Set the MailetContext
     * 
     * @param mailetContext the MailetContext
     */
    @Resource(name="org.apache.mailet.MailetContext") 
    public void setMailetContext(MailetContext mailetContext) {
        this.mailetContext = mailetContext;
    }

    protected Log getLogger() {
        return logger;
    }
    protected Object load(String className) throws ClassNotFoundException {
        final Object newInstance = loaderService.load(Thread.currentThread().getContextClassLoader().loadClass(className));
        return newInstance;
    }

    @SuppressWarnings("unchecked")
    protected void getPackages(HierarchicalConfiguration conf, String packageType)
        throws ConfigurationException {
        packages = new Vector<String>();
        packages.addElement("");
        final List<String> pkgConfs = conf.getList(packageType);
        for (int i = 0; i < pkgConfs.size(); i++) {
            String packageName = pkgConfs.get(i);
            if (!packageName.endsWith(".")) {
                packageName += ".";
            }
            packages.addElement(packageName);
        }
    }
    
    @PostConstruct
    public void init() throws Exception {
        configure(config);
    }
        
    protected abstract void configure(HierarchicalConfiguration arg0) throws ConfigurationException;
    
    /**
     * Gets a human readable description of the loader.
     * Used for messages.
     * @return not null
     */
    protected abstract String getDisplayName();

    /**
     * Constructs an appropriate exception with an appropriate message
     * @param name not null
     * @return not null
     */
    protected ClassNotFoundException classNotFound(String name) throws ClassNotFoundException {
        final StringBuilder builder =
            new StringBuilder(128)
                .append("Requested ")
                .append(getDisplayName())
                .append(" not found: ")
                .append(name)
                .append(".  Package searched: ");
        for (final String packageName:packages) {
            builder.append(packageName);
            builder.append(" ");
        }
        return new ClassNotFoundException(builder.toString());
    }

    /**
     * Constructs an appropriate exception with an appropriate message.
     * @param name not null
     * @param e not null
     * @return not null
     */
    protected MailetException loadFailed(String name, Exception e) {
        final StringBuilder builder =
            new StringBuilder(128).append("Could not load ").append(getDisplayName())
                .append(" (").append(name).append(")");
        final MailetException mailetException = new MailetException(builder.toString(), e);
        return mailetException;
    }

}
