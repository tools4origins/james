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


import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.logging.Log;
import org.apache.mailet.Mail;
import org.apache.mailet.MailetException;
import org.guiceyfruit.jsr250.Jsr250Module;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.name.Names;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.mail.MessagingException;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * This class is responsible for creating a set of named processors and
 * directing messages to the appropriate processor (given the State of the mail)
 *
 * @version CVS $Revision: 405882 $ $Date: 2006-05-12 23:30:04 +0200 (ven, 12 mag 2006) $
 */
public class StateAwareProcessorList implements MailProcessor, ProcessorList {

    /**
     * The map of processor names to processors
     */
    private final Map<String, MailProcessor> processors;

    private Log logger;

    private HierarchicalConfiguration config;
    
    public StateAwareProcessorList() {
        super();
        this.processors = new HashMap<String, MailProcessor>();
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
     * @see org.apache.avalon.framework.activity.Initializable#initialize()
     */
    @SuppressWarnings("unchecked")
    @PostConstruct
    public void init() throws Exception {
        final List<HierarchicalConfiguration> processorConfs = config.configurationsAt( "processor" );
        for ( int i = 0; i < processorConfs.size(); i++ )
        {
            final HierarchicalConfiguration processorConf = processorConfs.get(i);
            String processorName = processorConf.getString("[@name]");
            String processorClass = processorConf.getString("[@class]","org.apache.james.transport.LinearProcessor");

            try {
                Class<MailProcessor> cObj = (Class<MailProcessor>) Thread.currentThread().getContextClassLoader().loadClass(processorClass);
                MailProcessor processor = Guice.createInjector(new Jsr250Module(), new AbstractModule() {

                    @Override
                    protected void configure() {
                        bind(org.apache.commons.configuration.HierarchicalConfiguration.class).annotatedWith(Names.named("org.apache.commons.configuration.Configuration")).toInstance(processorConf);
                        bind(Log.class).annotatedWith(Names.named("org.apache.commons.logging.Log")).toInstance(logger);
                    }
                    
                }).getInstance(cObj);
                processors.put(processorName, processor);
                
                //setupLogger(processor, processorName);
                //ContainerUtil.service(processor, compMgr);
                //ContainerUtil.configure(processor, processorConf);
                
                if (logger.isInfoEnabled()) {
                    StringBuffer infoBuffer =
                        new StringBuffer(64)
                                .append("Processor ")
                                .append(processorName)
                                .append(" instantiated.");
                    logger.info(infoBuffer.toString());
                }
            } catch (Exception ex) {
                if (logger.isErrorEnabled()) {
                    StringBuffer errorBuffer =
                       new StringBuffer(256)
                               .append("Unable to init processor ")
                               .append(processorName)
                               .append(": ")
                               .append(ex.toString());
                    logger.error( errorBuffer.toString(), ex );
                }
                throw ex;
            }
        }
    }
    
    /**
     * Process this mail message by the appropriate processor as designated
     * in the state of the Mail object.
     *
     * @param mail the mail message to be processed
     *
     * @see org.apache.james.transport.MailProcessor#service(org.apache.mailet.Mail)
     */
    public void service(Mail mail) {
        while (true) {
            String processorName = mail.getState();
            if (processorName.equals(Mail.GHOST)) {
                //This message should disappear
                return;
            }
            try {
                MailProcessor processor
                    = (MailProcessor)processors.get(processorName);
                if (processor == null) {
                    StringBuffer exceptionMessageBuffer =
                        new StringBuffer(128)
                            .append("Unable to find processor ")
                            .append(processorName)
                            .append(" requested for processing of ")
                            .append(mail.getName());
                    String exceptionMessage = exceptionMessageBuffer.toString();
                    logger.debug(exceptionMessage);
                    mail.setState(Mail.ERROR);
                    throw new MailetException(exceptionMessage);
                }
                StringBuffer logMessageBuffer = null;
                if (logger.isDebugEnabled()) {
                    logMessageBuffer =
                        new StringBuffer(64)
                                .append("Processing ")
                                .append(mail.getName())
                                .append(" through ")
                                .append(processorName);
                    logger.debug(logMessageBuffer.toString());
                }
                processor.service(mail);
                if (logger.isDebugEnabled()) {
                    logMessageBuffer =
                        new StringBuffer(128)
                                .append("Processed ")
                                .append(mail.getName())
                                .append(" through ")
                                .append(processorName);
                    logger.debug(logMessageBuffer.toString());
                    logger.debug("Result was " + mail.getState());
                }
                return;
            } catch (Throwable e) {
                // This is a strange error situation that shouldn't ordinarily
                // happen
                StringBuffer exceptionBuffer = 
                    new StringBuffer(64)
                            .append("Exception in processor <")
                            .append(processorName)
                            .append(">");
                logger.error(exceptionBuffer.toString(), e);
                if (processorName.equals(Mail.ERROR)) {
                    // We got an error on the error processor...
                    // kill the message
                    mail.setState(Mail.GHOST);
                    mail.setErrorMessage(e.getMessage());
                } else {
                    //We got an error... send it to the requested processor
                    if (!(e instanceof MessagingException)) {
                        //We got an error... send it to the error processor
                        mail.setState(Mail.ERROR);
                    }
                    mail.setErrorMessage(e.getMessage());
                }
            }
            if (logger.isErrorEnabled()) {
                StringBuffer logMessageBuffer =
                    new StringBuffer(128)
                            .append("An error occurred processing ")
                            .append(mail.getName())
                            .append(" through ")
                            .append(processorName);
                logger.error(logMessageBuffer.toString());
                logger.error("Result was " + mail.getState());
            }
        }
    }

    /**
     * The dispose operation is called at the end of a components lifecycle.
     * Instances of this class use this method to release and destroy any
     * resources that they own.
     *
     * This implementation shuts down the Processors managed by this
     * Component
     *
     * @see org.apache.avalon.framework.activity.Disposable#dispose()
     */
    @PreDestroy
    public void dispose() {
        Iterator<String> it = processors.keySet().iterator();
        while (it.hasNext()) {
            String processorName = it.next();
            if (logger.isDebugEnabled()) {
                logger.debug("Processor " + processorName);
            }
            Object processor = processors.get(processorName);
            processors.remove(processor);
        }
    }

    /**
     * @return names of all configured processors
     */
    public String[] getProcessorNames() {
        return (String[]) processors.keySet().toArray(new String[]{});
    }

    public MailProcessor getProcessor(String name) {
        return (MailProcessor) processors.get(name);
    }

}
