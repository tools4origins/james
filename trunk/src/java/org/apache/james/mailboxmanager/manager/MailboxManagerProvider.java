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

package org.apache.james.mailboxmanager.manager;

import java.util.Map;

import org.apache.james.mailboxmanager.MailboxManagerException;
import org.apache.james.mailboxmanager.mailbox.MailboxSession;
import org.apache.james.services.User;

public interface MailboxManagerProvider {
    
    public MailboxManager getMailboxManagerInstance(User user) throws MailboxManagerException;
 

    /**
     * <b>WARNING</b> this really deletes everything. Useful for testing
     * 
     * @throws MailboxManagerException
     */
    public void deleteEverything() throws MailboxManagerException;
    
    MailboxSession getInboxSession(User user);

    /** @param authUser the authorized User for checking credentials 
        @param mailboxName a logical/hierarchical mailbox name **/ 

    MailboxSession getMailboxSession(
         User authUser, String mailboxName);
    
    /**
     * removes all data (mailboxes, quota, acls...) that is associated 
     * with this user.
     * 
     * @param authUser the authorized User for checking credentials 
     * @param targetUser the user whos data will be deleted
     */
    
    void deleteAllUserData(User authUser,User targetUser);
    
    
    /**
     * key: <b>String</b> - mailbox name <br />
     * value: <b>Integer</b> - count of open sessions <br />
     * <br />
     * useful for testing
     * @return Map of mailbox name/open session count
     */
    
    Map getOpenMailboxSessionCountMap();

}
