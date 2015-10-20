# Package names #

Plural should not be used in package names.
_import static org.gparallelizer.actors.pooledActors.PooledActors.
would become
**import static org.gpars.actor.pooledActor.PooledActors.**_

Plural form for class names should also be the exception. It holds static methods actor() and reactor() that return new instances. So maybe "Instance"? "Factory"?

If we rather want to have the class name to contain the context, we could have _org.gpars.util.PooledActorFactory_

The bottom line is that the package structure should be logical and easy to remember (when IDE support is not available).