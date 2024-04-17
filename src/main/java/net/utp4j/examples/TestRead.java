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
package net.utp4j.examples;

import net.utp4j.channels.UtpServerSocketChannel;
import net.utp4j.channels.UtpSocketChannel;
import net.utp4j.channels.futures.UtpAcceptFuture;
import net.utp4j.channels.futures.UtpReadFuture;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

public class TestRead {

    public static void main(String[] args) throws IOException, InterruptedException {
        ByteBuffer buffer = ByteBuffer.allocate(150000000);
        UtpServerSocketChannel server = UtpServerSocketChannel.open();
        server.bind(new InetSocketAddress(13344));
        UtpAcceptFuture acceptFuture = server.accept();
        acceptFuture.block();
        UtpSocketChannel channel = acceptFuture.getChannel();
        UtpReadFuture readFuture = channel.read(buffer);
        readFuture.setListener(new SaveFileListener());
        readFuture.block();
        System.out.println("reading end");
        channel.close();
        server.close();


    }


}
