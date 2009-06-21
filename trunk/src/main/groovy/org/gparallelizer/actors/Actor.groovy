package org.gparallelizer.actors

import java.util.concurrent.TimeUnit
import groovy.time.Duration;

/**
 * Actors are active objects, which either have their own thread processing repeatedly messages submitted to them
 * or they borrow a thread from a thread pool. Actors implementing the ThreadedActor interface have their own thread,
 * whereas actors implementing PooledActor interface delegate processing to threads from a thread pool.
 * The Actor interface provides means to send messages to the actor, start and stop the background thread as well as
 * check its status.
 *
 * @author Vaclav Pech
 * Date: Jan 7, 2009
 */
public interface Actor {

    /**
     * Starts the Actor. No messages can be send or received before an Actor is started.
     */
    Actor start();

    /**
     * Stops the Actor. Unprocessed messages will be passed to the afterStop method, if exists.
     * Has no effect if the Actor is not started.
     */
    Actor stop();

    /**
     * Checks the current status of the Actor.
     */
    boolean isActive();

    /**
     * Checks whether the current thread is the actor's current thread.
     */
    boolean isActorThread();

    /**
     * Joins the actor's thread
     * @param milis Timeout in miliseconds
     */
//    void join(long milis);
    
    /**
     * Adds a message to the Actor's queue. Can only be called on a started Actor.
     */
    Actor send(Object message) throws InterruptedException;

    /**
     * Sends a message and waits for a reply.
     * Returns the reply or throws an IllegalStateException, if the target actor cannot reply.
     * @return The message that came in reply to the original send.
     */
    Object sendAndWait(Object message);

    /**
     * Sends a message and waits for a reply. Timeouts after the specified timeout. In case of timeout returns null.
     * Returns the reply or throws an IllegalStateException, if the target actor cannot reply.
     * @return The message that came in reply to the original send.
     */
    Object sendAndWait(long timeout, TimeUnit timeUnit, Object message);

    /**
     * Sends a message and waits for a reply. Timeouts after the specified timeout. In case of timeout returns null.
     * Returns the reply or throws an IllegalStateException, if the target actor cannot reply.
     * @return The message that came in reply to the original send.
     */
    Object sendAndWait(Duration duration, Object message);

    /**
     * Adds a message to the Actor's queue. Can only be called on a started Actor.
     * Identical to the send() method.
     */
    Actor leftShift(Object message) throws InterruptedException;
}