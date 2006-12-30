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

package org.apache.james.smtpserver.core.filter.fastfail;

import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.james.smtpserver.SMTPSession;
import org.apache.james.smtpserver.hook.HookResult;
import org.apache.james.smtpserver.hook.HookReturnCode;
import org.apache.james.smtpserver.hook.MessageHook;
import org.apache.james.util.junkscore.ComposedJunkScore;
import org.apache.james.util.junkscore.JunkScore;
import org.apache.james.util.junkscore.JunkScoreImpl;
import org.apache.james.util.mail.dsn.DSNStatus;
import org.apache.mailet.Mail;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import java.util.Iterator;
import java.util.Map;

/**
 * Check if a configured JunkScore is reached and perform an action. Valid actions are: reject, compose, header. 
 * 
 * -Reject action reject the mail if the limit is reached.
 * -Compose action stores the junkScore values in the mail attributes
 * -Header action create headers which holds the junkScore for each check
 */
public class JunkScoreHandler extends AbstractLogEnabled implements MessageHook, Configurable {

    private double maxScore = 0;
    private String action;
    private static final String REJECT_ACTION = "reject";
    private static final String COMPOSE_ACTION = "compose";
    private static final String HEADER_ACTION = "header";

    /**
     * @see org.apache.avalon.framework.configuration.Configurable#configure(org.apache.avalon.framework.configuration.Configuration)
     */
    public void configure(Configuration arg0) throws ConfigurationException {
        Configuration maxScoreConfig = arg0.getChild("maxScore");

        if (maxScoreConfig != null) {
            setMaxScore(maxScoreConfig.getValueAsDouble(0));
        }
        
        Configuration actionConfig = arg0.getChild("action");
        if (actionConfig != null) {
            setAction(actionConfig.getValue().toLowerCase());
        } else {
            throw new ConfigurationException("Please configure the action");
        }
    }
    
    /**
     * Set the max JunkScore
     * 
     * @param maxScore the score
     */
    public void setMaxScore(double maxScore) {
        this.maxScore = maxScore;
    }
    
    /**
     * Set the action to perform if the JunkScore limit is reached
     * 
     * @param action the action
     * @throws ConfigurationException if invalid action is used
     */
    public void setAction(String action) throws ConfigurationException {
        if (!action.equals(REJECT_ACTION) && !action.equals(COMPOSE_ACTION) && !action.equals(HEADER_ACTION)) 
            throw new ConfigurationException("Illegal action: " + action);
     
        this.action = action;  
    }
    
    /**
     * @see org.apache.james.smtpserver.hook.MessageHook#onMessage(org.apache.james.smtpserver.SMTPSession, org.apache.mailet.Mail)
     */
    public HookResult onMessage(SMTPSession session, Mail mail) {
        return checkScore(session, mail);
    }

    /**
     * Check if the JunkScore limit is reached and perform the configured action
     * 
     * @param session the SMTPSession
     */
    private HookResult checkScore(SMTPSession session, Mail mail) {
        JunkScore score1 = getLazyJunkScoreHandler(session.getConnectionState(),JunkScore.JUNK_SCORE_SESSION);
        JunkScore score2 = getLazyJunkScoreHandler(session.getState(),JunkScore.JUNK_SCORE);
        
        JunkScore composed = new ComposedJunkScore(score1,score2);
        
        if (action.equals(COMPOSE_ACTION)) {
            // Save the scores attribute to maybe use it later!
            mail.setAttribute(JunkScore.JUNK_SCORE_SESSION_ATTR, String.valueOf(score1.getCompleteStoredScores()));
            mail.setAttribute(JunkScore.JUNK_SCORE_ATTR, String.valueOf(score2.getCompleteStoredScores()));
            mail.setAttribute(JunkScore.JUNK_SCORE_COMPOSED_ATTR, String.valueOf(composed.getCompleteStoredScores()));
        } else if (action.equals(REJECT_ACTION)) {
            if (maxScore <  composed.getCompleteStoredScores()) {
 
                StringBuffer buffer = new StringBuffer(256).append(
                    "Rejected message from ").append(
                    session.getState().get(SMTPSession.SENDER)
                            .toString()).append(" from host ")
                    .append(session.getRemoteHost()).append(" (")
                    .append(session.getRemoteIPAddress()).append(
                            "). This message reach the smap hits treshold. Required rejection hits: ")
                    .append(maxScore).append(" hits: ")
                    .append(composed.getCompleteStoredScores());
                getLogger().info(buffer.toString());
                
                return new HookResult(HookReturnCode.DENY, DSNStatus.getStatus(DSNStatus.PERMANENT,
                            DSNStatus.SECURITY_OTHER) + " This message reach the spam hits treshold. Please contact the Postmaster if the email is not SPAM. Message rejected");
            }
        } else if (action.equals(HEADER_ACTION)) {
            try {
                MimeMessage message = mail.getMessage();
                Map scores = composed.getStoredScores();
                Iterator itScores = scores.keySet().iterator();
        
                StringBuffer header = new StringBuffer();
                while (itScores.hasNext()) {
                    String key = itScores.next().toString();
                    header.append(key);
                    header.append("=");
                    header.append(scores.get(key));
                    header.append("; ");
                }
        
                message.setHeader("X-JUNKSCORE", header.toString());
                message.setHeader("X-JUNKSCORE-COMPOSED", String.valueOf(composed.getCompleteStoredScores()));
        
            } catch (MessagingException e) {
                getLogger().info("Unable to add Junkscore to header: " + e.getMessage());
            }
        }
        return null;
    }
    
    public static JunkScore getLazyJunkScoreHandler(Map state, String key) {
        JunkScore score = (JunkScore) state.get(key);
        if (score == null) {
            score = new JunkScoreImpl();
            state.put(key, score);
        }
        return score;
    }
}
