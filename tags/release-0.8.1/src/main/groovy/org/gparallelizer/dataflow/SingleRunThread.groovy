package org.gparallelizer.dataflow

/**
 * An actor representing a dataflow thread. Runs the supplied block of code inside the act() actor method once.
 *
 * @author Vaclav Pech
 * Date: Jun 5, 2009
 */
final class SingleRunThread extends DataFlowActor {

    Closure body

    void act() {
        body.delegate = this
        body()
    }
}
