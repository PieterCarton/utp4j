/* Copyright 2013 Ivan Iljkic
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package net.utp4j.channels.exception;

import net.utp4j.channels.UtpSocketState;

import java.io.Serial;
import java.net.SocketAddress;

/**
 * Exception indicates that the channel is in the wrong state.
 *
 * @author Ivan Iljkic (i.iljkic@gmail.com)
 */
public class ChannelStateException extends RuntimeException {


    @Serial
    private static final long serialVersionUID = -6506270718267816636L;

    private UtpSocketState state;
    private SocketAddress addresse;

    public ChannelStateException(String msg) {
        super(msg);
    }

    public UtpSocketState getState() {
        return state;
    }

    public void setState(UtpSocketState state) {
        this.state = state;
    }

    public SocketAddress getAddresse() {
        return addresse;
    }

    public void setAddresse(SocketAddress addresse) {
        this.addresse = addresse;
    }

}
