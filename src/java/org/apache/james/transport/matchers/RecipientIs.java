/***********************************************************************
 * Copyright (c) 2000-2004 The Apache Software Foundation.             *
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

package org.apache.james.transport.matchers;

import org.apache.mailet.GenericRecipientMatcher;
import org.apache.mailet.MailAddress;

import java.util.Collection;
import java.util.StringTokenizer;

/**
 * @version 1.0.0, 24/04/1999
 */
public class RecipientIs extends GenericRecipientMatcher {

    private Collection recipients;

    public void init() throws javax.mail.MessagingException {
        StringTokenizer st = new StringTokenizer(getCondition(), ", \t", false);
        recipients = new java.util.HashSet();
        while (st.hasMoreTokens()) {
            recipients.add(new MailAddress(st.nextToken()));
        }
    }

    public boolean matchRecipient(MailAddress recipient) {
        return recipients.contains(recipient);
    }
}
