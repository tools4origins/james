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

package org.apache.james.experimental.imapserver;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.james.api.imap.process.ImapProcessor;
import org.apache.james.imapserver.codec.decode.ImapDecoder;
import org.apache.james.imapserver.codec.encode.ImapEncoder;
import org.apache.james.services.User;
import org.apache.james.services.UsersRepository;
import org.apache.james.test.functional.imap.HostSystem;
import org.apache.james.test.mock.avalon.MockLogger;

public class ExperimentalHostSystem implements HostSystem, UsersRepository {

    private ImapDecoder decoder;
    private ImapEncoder encoder;
    private ImapProcessor processor;
    private Resetable dataReset;
    private boolean isReadLast = true;
    private final Set users;
    
    public ExperimentalHostSystem() {
        super();
        users = new HashSet();
    }
    
    public void configure(final ImapDecoder decoder, final ImapEncoder encoder, 
            final ImapProcessor processor, final Resetable dataReset) {    
        this.decoder = decoder;
        this.encoder = encoder;
        this.processor = processor;
        this.dataReset = dataReset;
    }
    
    public boolean addUser(String username, String password) {
        User user = new MockUser(username, password);
        users.add(user);
        return true;

    }

    public HostSystem.Session newSession(Continuation continuation) throws Exception {
        return new Session(continuation);
    }

    public void reset() throws Exception {
        users.clear();
        dataReset.reset();
    }

    public String getHelloName() {
        return "JAMES";
    }

    public ImapDecoder getImapDecoder() {
        return decoder;
    }

    public ImapEncoder getImapEncoder() {
        return encoder;
    }

    public ImapProcessor getImapProcessor() {
        return processor;
    }

    public int getResetLength() {
        return Integer.MAX_VALUE;
    }

    public boolean addUser(User user) {
        users.add(user);
        return true;
    }

    public void addUser(String name, Object attributes) {
        User user = new MockUser(name, "SHA");
        users.add(user);
    }

    public boolean contains(String name) {
        boolean result = false;
        if (name != null)
        {
            for (Iterator it=users.iterator();it.hasNext();)
            {
                User user = (User) it.next();
                if (name.equals(user.getUserName())){
                    result = true;
                    break;
                }
            }
        }
        return result;
    }

    public boolean containsCaseInsensitive(String name) {
        boolean result = false;
        if (name != null)
        {
            for (Iterator it=users.iterator();it.hasNext();)
            {
                User user = (User) it.next();
                if (name.equalsIgnoreCase(user.getUserName())){
                    result = true;
                    break;
                }
            }
        }
        return result;
    }

    public int countUsers() {
        return users.size();
    }

    public String getRealName(String name) {
        return name;
    }

    public User getUserByName(String name) {
        User result = null;
        if (name != null)
        {
            for (Iterator it=users.iterator();it.hasNext();)
            {
                User user = (User) it.next();
                if (name.equals(user.getUserName())){
                    result = user;
                    break;
                }
            }
        }
        return result;
    }

    public User getUserByNameCaseInsensitive(String name) {
        User result = null;
        if (name != null)
        {
            for (Iterator it=users.iterator();it.hasNext();)
            {
                User user = (User) it.next();
                if (name.equalsIgnoreCase(user.getUserName())){
                    result = user;
                    break;
                }
            }
        }
        return result;
    }

    public Iterator list() {
        Collection results = new ArrayList();
        for (Iterator it=users.iterator();it.hasNext();)
        {
            User user = (User) it.next();
            results.add(user.getUserName());
        }

        return results.iterator();
    }

    public void removeUser(String name) {
        if (name != null)
        {
            for (Iterator it=users.iterator();it.hasNext();)
            {
                User user = (User) it.next();
                if (name.equals(user.getUserName())){
                    it.remove();
                    break;
                }
            }
        }       
    }

    public boolean test(String name, String password) {
        boolean result = false;
        if (name != null)
        {
            for (Iterator it=users.iterator();it.hasNext();)
            {
                User user = (User) it.next();
                if (name.equals(user.getUserName())){
                    result = user.verifyPassword(password);
                    break;
                }
            }
        }
        return result;
    }

    public boolean updateUser(User user) {
        users.add(user);
        return true;
    }
    
    class Session implements HostSystem.Session
    {
        ByteBufferOutputStream out;
        ByteBufferInputStream in;
        ImapRequestHandler handler;
        ImapSessionImpl session;
        
        
        public Session(Continuation continuation) {
            out = new ByteBufferOutputStream(continuation);
            in = new ByteBufferInputStream();
            handler = new ImapRequestHandler(decoder, processor, encoder);
            handler.enableLogging(new MockLogger());
            session = new ImapSessionImpl();
        }
        
        public String readLine() throws Exception {
            if (!isReadLast) {
                handler.handleRequest(in, out, session);
                isReadLast = true;
            }
            final String result = out.nextLine();
            return result;
        }

        public void start() throws Exception {
            // Welcome message handled in the server
            out.write("* OK IMAP4rev1 Server ready\r\n");
        }

        public void stop() throws Exception {
            
        }

        public void writeLine(String line) throws Exception {
            isReadLast = false;
            in.nextLine(line);
        }

        public void forceConnectionClose(String byeMessage) {
            try {
                out.write(byeMessage);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
    
    static class ByteBufferInputStream extends InputStream {
        
        ByteBuffer buffer = ByteBuffer.allocate(16384);
        CharsetEncoder encoder = Charset.forName("ASCII").newEncoder();
        boolean readLast = true;
        
        public int read() throws IOException {
            if (!readLast) {
                readLast = true;
                buffer.flip();
            }
            int result = -1;
            if (buffer.hasRemaining()) {
                result = buffer.get();
            }
            return result;
        }
        
        public void nextLine(String line) {
            if (buffer.position() > 0 && readLast) {
                buffer.compact();
            }
            encoder.encode(CharBuffer.wrap(line), buffer, true);
            buffer.put((byte)'\r');
            buffer.put((byte)'\n');
            readLast = false;
        }
    }
    
    static class ByteBufferOutputStream extends OutputStream {
        ByteBuffer buffer = ByteBuffer.allocate(65536);
        Charset ascii = Charset.forName("ASCII");
        Continuation continuation;
        boolean matchPlus = false;
        boolean matchCR = false;
        boolean matchLF = false;
        
        public ByteBufferOutputStream(Continuation continuation) {
            this.continuation = continuation;
        }
        
        public void write(String message) throws IOException {
            ascii.newEncoder().encode(CharBuffer.wrap(message), buffer, true);
        }
        
        public void write(int b) throws IOException {
            buffer.put((byte) b);
            if (b == '\n' && matchPlus && matchCR && matchLF) {
                matchPlus = false;
                matchCR = false;
                matchLF = false;
                continuation.doContinue();
            } else if (b == '\n') {
                matchLF = true;
                matchPlus = false;
                matchCR = false;
            } else if (b == '+' && matchLF) {
                matchPlus = true;
                matchCR = false;
            } else if (b == '\r' && matchPlus && matchLF) {
                matchCR = true;
            } else {
                matchPlus = false;
                matchCR = false;
                matchLF = false;
            }
        }        
        
        public String nextLine() throws Exception {
            buffer.flip();
            byte last = 0;
            while (buffer.hasRemaining()) {
                byte next = buffer.get();
                if (last == '\r' && next == '\n') {
                    break;
                }
                last = next;
            }
            final ByteBuffer readOnlyBuffer = buffer.asReadOnlyBuffer();
            readOnlyBuffer.flip();
            int limit = readOnlyBuffer.limit() - 2;
            if (limit < 0) {
                limit = 0;
            }
            readOnlyBuffer.limit(limit);
            String result = ascii.decode(readOnlyBuffer).toString();
            buffer.compact();
            return result;
        }
    }
    
    static class MockUser implements User {

        private final String user;
        private String password;
        
        
        
        public MockUser(final String user, final String password) {
            super();
            this.user = user;
            this.password = password;
        }

        public String getUserName() {
            return user;
        }

        public boolean setPassword(String newPass) {
            this.password = newPass;
            return true;
        }

        public boolean verifyPassword(String pass) {
            return password.equals(pass);
        }

        public int hashCode() {
            final int PRIME = 31;
            int result = 1;
            result = PRIME * result + ((user == null) ? 0 : user.hashCode());
            return result;
        }

        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            final MockUser other = (MockUser) obj;
            if (user == null) {
                if (other.user != null)
                    return false;
            } else if (!user.equals(other.user))
                return false;
            return true;
        }
    }
    
    public interface Resetable {
        public void reset() throws Exception;
    }
}
