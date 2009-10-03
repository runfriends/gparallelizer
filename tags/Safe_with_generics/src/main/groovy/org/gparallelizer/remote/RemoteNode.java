//  GParallelizer
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

package org.gparallelizer.remote;

import org.gparallelizer.actors.Actor;
import org.gparallelizer.serial.SerialContext;

import java.util.UUID;

/**
 * Representation of remote node
 *
 * @author Alex Tkachman
 */
public final class RemoteNode<T extends LocalHost> {
    private final UUID id;

    private final SerialContext remoteHost;
    private final Actor mainActor;

    public RemoteNode(UUID id, SerialContext remoteHost, Actor mainActor) {
        this.id = id;
        this.remoteHost = remoteHost;
        this.mainActor = mainActor;
    }

    public final UUID getId() {
        return id;
    }

    @Override
    public String toString() {
        return getId().toString();
    }

    public final Actor getMainActor() {
        return mainActor;
    }

    public SerialContext getRemoteHost() {
        return remoteHost;
    }
}