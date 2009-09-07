package org.gparallelizer.remote.sharedmemory;

import org.gparallelizer.remote.RemoteNode;
import org.gparallelizer.remote.LocalNode;
import org.gparallelizer.remote.RemoteActor;
import org.gparallelizer.actors.ActorMessage;

import java.io.Serializable;
import java.util.UUID;

public class SharedMemoryNode extends RemoteNode {
    private final LocalNode localNode;

    public SharedMemoryNode(LocalNode node) {
        super();
        this.localNode = node;
    }

    public UUID getId() {
        return localNode.getId();
    }

    public void send(RemoteActor receiver, ActorMessage<Serializable> message) {
        throw new UnsupportedOperationException();
    }

    public LocalNode getLocalNode() {
        return localNode;
    }

    public void onDisconnect(SharedMemoryNode smn) {
        localNode.onDisconnect(smn);
    }

    protected RemoteActor createRemoteActor(UUID uid) {
        if (uid == RemoteNode.MAIN_ACTOR_ID)
            return new SharedMemoryActor(this, localNode.getMainActor());

        throw new UnsupportedOperationException();
    }
}
