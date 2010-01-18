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



package org.apache.james.smtpserver;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.DefaultConfigurationBuilder;
import org.apache.james.smtpserver.integration.CoreCmdHandlerLoader;
import org.apache.james.smtpserver.protocol.core.fastfail.DNSRBLHandler;
import org.apache.james.smtpserver.protocol.core.fastfail.MaxRcptHandler;
import org.apache.james.smtpserver.protocol.core.fastfail.ResolvableEhloHeloHandler;
import org.apache.james.smtpserver.protocol.core.fastfail.ReverseEqualsEhloHeloHandler;
import org.apache.james.smtpserver.protocol.core.fastfail.ValidSenderDomainHandler;

public class SMTPTestConfiguration extends DefaultConfigurationBuilder {

    private int m_smtpListenerPort;
    private int m_maxMessageSizeKB = 0;
    private String m_authorizedAddresses = "127.0.0.0/8";
    private String m_authorizingMode = "false";
    private boolean m_verifyIdentity = false;
    private Integer m_connectionLimit = null;
    private Integer m_connectionBacklog = null;
    private boolean m_heloResolv = false;
    private boolean m_ehloResolv = false;
    private boolean m_senderDomainResolv = false;
    private boolean m_checkAuthNetworks = false;
    private boolean m_heloEhloEnforcement = true;
    private boolean m_reverseEqualsHelo = false;
    private boolean m_reverseEqualsEhlo = false;
    private int m_maxRcpt = 0;
    private boolean m_useRBL = false;
    private boolean m_addressBracketsEnforcement = true;
	private boolean m_startTLS = false;

    
    public SMTPTestConfiguration(int smtpListenerPort) {
        m_smtpListenerPort = smtpListenerPort;
    }
    
    public void setCheckAuthNetworks(boolean checkAuth) {
        m_checkAuthNetworks = checkAuth; 
    }


    public void setMaxMessageSize(int kilobytes)
    {
        m_maxMessageSizeKB = kilobytes;
    }
    
    
    public int getMaxMessageSize() {
        return m_maxMessageSizeKB;
    }

    public String getAuthorizedAddresses() {
        return m_authorizedAddresses;
    }

    public void setAuthorizedAddresses(String authorizedAddresses) {
        m_authorizedAddresses = authorizedAddresses;
    }

    public void setAuthorizingNotRequired() {
        m_authorizingMode = "false";
        m_verifyIdentity = false; 
    }

    public void setAuthorizingRequired() {
        m_authorizingMode = "true";
        m_verifyIdentity = true; 
    }

    public void setAuthorizingAnnounce() {
        m_authorizingMode = "announce";
        m_verifyIdentity = true; 
    }

    public void setConnectionLimit(int iConnectionLimit) {
        m_connectionLimit = new Integer(iConnectionLimit);
    }
    
    public void setConnectionBacklog(int iConnectionBacklog) {
        m_connectionBacklog = new Integer(iConnectionBacklog);
    }
    
    public void setHeloResolv() {
        m_heloResolv = true; 
    }
    
    public void setEhloResolv() {
        m_ehloResolv = true; 
    }
    
    public void setReverseEqualsHelo() {
        m_reverseEqualsHelo = true; 
    }
    
    public void setReverseEqualsEhlo() {
        m_reverseEqualsEhlo = true; 
    }
    
    public void setSenderDomainResolv() {
        m_senderDomainResolv = true; 
    }
    
    public void setMaxRcpt(int maxRcpt) {
        m_maxRcpt = maxRcpt; 
    }
    
    public void setHeloEhloEnforcement(boolean heloEhloEnforcement) {
        m_heloEhloEnforcement = heloEhloEnforcement; 
    }
    
    public void useRBL(boolean useRBL) {
        m_useRBL = useRBL; 
    }
    
    public void setAddressBracketsEnforcement(boolean addressBracketsEnforcement) {
        this.m_addressBracketsEnforcement = addressBracketsEnforcement;
    }
    
    public void setStartTLS() {
    	m_startTLS  = true;
    }
    public void init() throws ConfigurationException {

        addProperty("[@enabled]", true);

        addProperty("port", m_smtpListenerPort);
        if (m_connectionLimit != null) addProperty("connectionLimit",  ""+m_connectionLimit.intValue());
        if (m_connectionBacklog != null) addProperty("connectionBacklog", ""+ m_connectionBacklog.intValue());
        
        addProperty("handler.helloName", "myMailServer");
        addProperty("handler.connectiontimeout", 360000);
        addProperty("handler.authorizedAddresses", m_authorizedAddresses);
        addProperty("handler.maxmessagesize",  m_maxMessageSizeKB);
        addProperty("handler.authRequired", m_authorizingMode);
        addProperty("handler.heloEhloEnforcement", m_heloEhloEnforcement);
        addProperty("handler.addressBracketsEnforcement", m_addressBracketsEnforcement);
        
        addProperty("startTLS.[@enable]", m_startTLS);
        addProperty("startTLS.keystore","file://conf/test_keystore");
        addProperty("startTLS.secret", "jamestest");        
        if (m_verifyIdentity) addProperty("handler.verifyIdentity", m_verifyIdentity);
 
        // add the rbl handler
        if (m_useRBL) {
            
            addProperty("handler.handlerchain.handler.[@class]", DNSRBLHandler.class.getName());
            addProperty("handler.handlerchain.handler.rblservers.blacklist","bl.spamcop.net.");
        }
        if (m_heloResolv || m_ehloResolv) {
            addProperty("handler.handlerchain.handler.[@class]", ResolvableEhloHeloHandler.class.getName());
            addProperty("handler.handlerchain.handler.checkAuthNetworks", m_checkAuthNetworks);
        }
        if (m_reverseEqualsHelo || m_reverseEqualsEhlo) {
            addProperty("handler.handlerchain.handler.[@class]", ReverseEqualsEhloHeloHandler.class.getName());
            addProperty("handler.handlerchain.handler.checkAuthNetworks", m_checkAuthNetworks);
        }
        if (m_senderDomainResolv) {
            addProperty("handler.handlerchain.handler.[@class]", ValidSenderDomainHandler.class.getName());
            addProperty("handler.handlerchain.handler.checkAuthNetworks", m_checkAuthNetworks);
        }
        if (m_maxRcpt > 0) {
            addProperty("handler.handlerchain.handler.[@class]", MaxRcptHandler.class.getName());
            addProperty("handler.handlerchain.handler.maxRcpt", m_maxRcpt);
        }
       
        addProperty("handler.handlerchain.handler.[@class]", CoreCmdHandlerLoader.class.getName());
    }
    
}
