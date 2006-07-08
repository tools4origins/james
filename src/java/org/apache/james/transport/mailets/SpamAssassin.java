/***********************************************************************
 * Copyright (c) 2006 The Apache Software Foundation.                  *
 * All rights reserved.                                                *
 * ------------------------------------------------------------------- *
 * Licensed under the Apache License, Version 2.0 (the "License"); you *
 * may not use this file except in compliance with the License. You    *
 * may obtain a copy of the License at:                                *
 *                                                                     *
 *     http://www.apache.org/licenses/LICENSE-2.0                      *
 *                                                                     *
 * Unless required by applicable law or agreed to in writing, software *
 * distributed under the License is distributed on an "AS IS" BASIS,   *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or     *
 * implied.  See the License for the specific language governing       *
 * permissions and limitations under the License.                      *
 ***********************************************************************/
package org.apache.james.transport.mailets;

import java.util.Iterator;

import org.apache.james.util.SpamAssassinInvoker;
import org.apache.mailet.GenericMailet;
import org.apache.mailet.Mail;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

/**
 * Sends the message through daemonized SpamAssassin (spamd), visit <a
 * href="SpamAssassin.org">SpamAssassin.org</a> for info on configuration. The
 * header X-Spam-Status is added to every message, this contains the score and
 * the threshold score for spam (usually 5.0). If the message exceeds the
 * threshold, the header X-Spam-Flag will be added with the value of YES. The
 * default host for spamd is localhost and the default port is 783. <br>
 * <br>
 * Sample Configuration: <br>
 * <br>
 * &lt;mailet notmatch="SenderHostIsLocal" class="SpamAssassin"&gt;
 * &lt;spamdHost&gt;localhost&lt;/spamdHost&gt;
 * &lt;spamdPort&gt;783&lt;/spamdPort&gt; <br>
 */
public class SpamAssassin extends GenericMailet {

    String spamdHost;

    int spamdPort;

    /**
     * @see org.apache.mailet.GenericMailet#init()
     */
    public void init() throws MessagingException {
        spamdHost = getInitParameter("spamdHost");
        if (spamdHost == null || spamdHost.equals("")) {
            spamdHost = "127.0.0.1";
        }

        String port = getInitParameter("spamdPort");
        if (port == null || port.equals("")) {
            spamdPort = 783;
        } else {

            try {
                spamdPort = Integer.parseInt(getInitParameter("spamdPort"));
            } catch (NumberFormatException e) {
                throw new MessagingException(
                        "Please configure a valid port. Not valid: "
                                + spamdPort);
            }
        }
    }

    /**
     * @see org.apache.mailet.GenericMailet#service(Mail)
     */
    public void service(Mail mail) {
        try {
            MimeMessage message = mail.getMessage();

            // Invoke spamassian connection and scan the message
            SpamAssassinInvoker sa = new SpamAssassinInvoker(spamdHost,
                    spamdPort);
            sa.scanMail(message);

            Iterator headers = sa.getHeaders().keySet().iterator();

            // Add headers 
            while (headers.hasNext()) {
                String key = headers.next().toString();
                message.setHeader(key, (String) sa.getHeaders().get(key));
            }

            message.saveChanges();
        } catch (MessagingException e) {
            log(e.getMessage());
        }

    }
    
    /**
     * @see org.apache.mailet.GenericMailet#getMailetInfo()
     */
    public String getMailetInfo() {
        return "Checks message against SpamAssassin";
    }
}
