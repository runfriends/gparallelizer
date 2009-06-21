package org.gparallelizer.actors;

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
     * Adds a message to the Actor's queue. Can only be called on a started Actor.
     */
    Actor send(Object message) throws InterruptedException;
}