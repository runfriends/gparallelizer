//  GPars (formerly GParallelizer)
//
//  Copyright © 2008-9  The original author or authors
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//        http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package groovyx.gpars.remote.netty;

import groovyx.gpars.remote.RemoteConnection;
import groovyx.gpars.remote.RemoteHost;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.handler.codec.serialization.ObjectEncoder;

@ChannelPipelineCoverage("one")
public class RemoteObjectEncoder extends ObjectEncoder {
    private final RemoteConnection connection;

    /**
     * Creates a new encoder.
     *
     * @param connection connection handling serialization details
     */
    public RemoteObjectEncoder(final RemoteConnection connection) {
        super();
        this.connection = connection;
    }

    @Override
    protected Object encode(final ChannelHandlerContext ctx, final Channel channel, final Object msg) throws Exception {
        final RemoteHost remoteHost = connection.getHost();

        if (remoteHost != null) {
            remoteHost.enter();
        }
        try {
            return super.encode(ctx, channel, msg);
        }
        finally {
            if (remoteHost != null) {
                remoteHost.leave();
            }
        }
    }
}
