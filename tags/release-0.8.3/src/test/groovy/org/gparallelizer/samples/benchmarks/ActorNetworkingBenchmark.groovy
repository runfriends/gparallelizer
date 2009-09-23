package org.gparallelizer.samples.benchmarks

import java.util.concurrent.CountDownLatch
import org.gparallelizer.actors.Actor
import org.gparallelizer.actors.DefaultThreadActor
import org.gparallelizer.samples.benchmarks.Benchmark
import org.gparallelizer.actors.DefaultThreadActor

public class ActorNetworkingBenchmark implements Benchmark {

    public long perform(final int numberOfIterations) {
        final long t1 = System.currentTimeMillis()
        final NetworkingMaster master = new NetworkingMaster(numActors: 10, iterations: numberOfIterations)
        master.start()
        master.waitUntilDone()
        final long t2 = System.currentTimeMillis()
        master.stopAll()

        return (t2 - t1)
    }
}

class WorkerActor extends DefaultThreadActor {
    void act() {
        receive()
        reply '2'
    }
}

//todo send strings
final class NetworkingMaster extends DefaultThreadActor {

    int iterations = 1
    int numActors = 1

    private List<Actor> workers

    private CountDownLatch startupLatch = new CountDownLatch(1)
    private CountDownLatch doneLatch

    private void beginSorting() {
        workers = createWorkers()
        int cnt = sendTasksToWorkers()
        doneLatch = new CountDownLatch(cnt)
        startupLatch.countDown()
    }

    private List createWorkers() {
        return (1..numActors).collect {new WorkerActor().start()}
    }

    private int sendTasksToWorkers() {
        int cnt = 0
        for (i in 1..iterations) {
            workers[cnt % numActors] << '1'
            cnt += 1
        }
        return cnt
    }

    public void waitUntilDone() {
        startupLatch.await()
        doneLatch.await()
    }

    void act() {
        beginSorting()
        while (true) {
            receive()
            doneLatch.countDown()
        }
    }

    public void stopAll() {
        workers.each {it.stop()}
        stop()
        workers = null
    }
}