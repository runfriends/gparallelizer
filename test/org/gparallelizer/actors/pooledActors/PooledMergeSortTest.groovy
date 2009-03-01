package org.gparallelizer.actors.pooledActors
import java.util.concurrent.TimeUnit
import java.util.concurrent.CountDownLatch
import static org.gparallelizer.actors.pooledActors.PooledActors.*
import org.gparallelizer.actors.pooledActors.PooledActors

/**
 *
 * @author Vaclav Pech
 * Date: Jan 12, 2009
 */
public class PooledMergeSortTest extends GroovyTestCase {

    protected void setUp() {
        super.setUp();
        PooledActors.pool.resize(10)
    }

    protected def split(List<Integer> list) {
        int listSize = list.size()
        int middleIndex = listSize / 2
        def list1 = list[0..<middleIndex]
        def list2 = list[middleIndex..listSize - 1]
        return [list1, list2]
    }

    protected List<Integer> merge(List<Integer> a, List<Integer> b) {
        int i = 0, j = 0
        final int newSize = a.size() + b.size()
        List<Integer> result = new ArrayList<Integer>(newSize)

        while ((i < a.size()) && (j < b.size())) {
            if (a[i] <= b[j]) result << a[i++]
            else result << b[j++]
        }

        if (i < a.size()) result.addAll(a[i..-1])
        else result.addAll(b[j..-1])
        return result
    }

    Closure createMessageHandler(def parentActor) {
        return {
            react {List<Integer> message ->
                assert message != null
                switch (message.size()) {
                    case 0..1:
                        parentActor.send(message)
                        break
                    case 2:
                        if (message[0] <= message[1]) parentActor.send(message)
                        else parentActor.send(message[-1..0])
                        break
                    default:
                        def splitList = split(message)

                        def child1 = actor(createMessageHandler(delegate))
                        def child2 = actor(createMessageHandler(delegate))
                        child1.start().send(splitList[0])
                        child2.start().send(splitList[1])

                        react {message1 ->
                            react {message2 ->
                                parentActor.send merge(message1, message2)
                            }

                        }
                }
            }
        }

    }

    public void testDefaultMergeSortWithOneThreadPool() {
        PooledActors.pool.resize(1)
        volatile def result = null;
        final CountDownLatch latch = new CountDownLatch(1)

        def resultActor = actor {
            react {
                result = it
                latch.countDown()
            }
        }.start()

        def sorter = actor(createMessageHandler(resultActor))
        sorter.start().send([1, 5, 2, 4, 3, 8, 6, 7, 3, 9, 5, 3])

        latch.await(30, TimeUnit.SECONDS)
        assertEquals([1, 2, 3, 3, 3, 4, 5, 5, 6, 7, 8, 9], result)
    }

    public void testDefaultMergeSort() {
        PooledActors.pool.resize(10)
        volatile def result = null;
        final CountDownLatch latch = new CountDownLatch(1)

        def resultActor = actor {
            react {
                result = it
                latch.countDown()
            }
        }.start()

        def sorter = actor(createMessageHandler(resultActor))
        sorter.start().send([1, 5, 2, 4, 3, 8, 6, 7, 3, 9, 5, 3])

        latch.await(30, TimeUnit.SECONDS)
        assertEquals([1, 2, 3, 3, 3, 4, 5, 5, 6, 7, 8, 9], result)
    }
}