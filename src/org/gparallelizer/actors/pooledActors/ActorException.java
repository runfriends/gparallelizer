package org.gparallelizer.actors.pooledActors;

/**
 * Pooled actors need to simulate continuations to create stacktrace-less chunks of work (ActorActions) to assign
 * to the threads from the pool. To achieve this ActorActions throw exceptions to terminate the current chuck of work
 * and allow another chunk of work on the same actor to begin.
 * ActorAction is a parent to these exception. It also holds initialized instances of each of the concrete subclasses
 * to avoid need for exception object creation each time.
 * @author Vaclav Pech
 * Date: Feb 17, 2009
 */
abstract class ActorException extends RuntimeException {

    public static final ActorException CONTINUE = new ActorContinuationException();
    public static final ActorException TERMINATE = new ActorTerminationException();

    ActorException() { }
}