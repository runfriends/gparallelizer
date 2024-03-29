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

package groovyx.gpars

import java.lang.Thread.UncaughtExceptionHandler
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import jsr166y.forkjoin.ForkJoinPool

/**
 * @author Vaclav Pech
 * Date: Oct 23, 2008
 */
public class ParallelUtilTest extends GroovyTestCase {

    public void testEach() {
        Parallelizer.withParallelizer(5) {
            final AtomicInteger result = new AtomicInteger(0)
            ParallelArrayUtil.eachParallel([1, 2, 3, 4, 5], {result.addAndGet(it)})
            assertEquals 15, result
        }
    }

    public void testEachWithIndex() {
        Parallelizer.withParallelizer(5) {
            final AtomicInteger result = new AtomicInteger(0)
            ParallelArrayUtil.eachWithIndexParallel([1, 2, 3, 4, 5], {element, int index -> result.addAndGet(element * index)})
            assertEquals 40, result
        }
    }

    public void testCollect() {
        Parallelizer.withParallelizer(5) {
            final List result = ParallelArrayUtil.collectParallel([1, 2, 3, 4, 5], {it * 2})
            assert ([2, 4, 6, 8, 10].equals(result))
            assert !([2, 41, 6, 8, 10].equals(result))
        }
    }

    public void testFindAll() {
        Parallelizer.withParallelizer(5) {
            final List result = ParallelArrayUtil.findAllParallel([1, 2, 3, 4, 5], {it > 2})
            assert !(1 in result)
            assert !(1 in result)
            assert (3 in result)
            assert (4 in result)
            assert (5 in result)
            assertEquals 3, result.size()
        }
    }

    public void testSplit() {
        Parallelizer.withParallelizer(5) {
            def result = [1, 2, 3, 4, 5].splitParallel{it > 2}
            assert [3, 4, 5] as Set == result[0] as Set
            assert [1, 2] as Set == result[1] as Set
            assertEquals 2, result.size()
            assert  [[], []] == [].splitParallel{it > 2}
            result = [3].splitParallel{it > 2}
            assert [[3], []] == result
            result = [1].splitParallel{it > 2}
            assert [[], [1]] == result
        }
    }

    public void testSplitOnString() {
        Parallelizer.withParallelizer(5) {
            def result = 'abc'.splitParallel{it == 'b'}
            assert ['b'] as Set == result[0] as Set
            assert ['a', 'c'] as Set == result[1] as Set
            assertEquals 2, result.size()
            result = ''.splitParallel{it == 'b'}
            assert  [[], []] == result
            result = 'b'.splitParallel{it == 'b'}
            assert [['b'], []] == result
            result = 'a'.splitParallel{it == 'b'}
            assert [[], ['a']] == result
        }
    }

    public void testGrep() {
        Parallelizer.withParallelizer(5) {
            final List result = ParallelArrayUtil.grepParallel([1, 2, 3, 4, 5], 3..6)
            assert !(1 in result)
            assert !(1 in result)
            assert (3 in result)
            assert (4 in result)
            assert (5 in result)
            assertEquals 3, result.size()
        }
    }

    public void testEmptyFindAll() {
        Parallelizer.withParallelizer(5) {
            final List<Integer> result = ParallelArrayUtil.findAllParallel([1, 2, 3, 4, 5], {it > 6})
            assertEquals 0, result.size()
        }
    }

    public void testFind() {
        Parallelizer.withParallelizer(5) {
            final int result = ParallelArrayUtil.findParallel([1, 2, 3, 4, 5], {it > 2})
            assert result in [3, 4, 5]
        }
    }

    public void testEmptyFind() {
        Parallelizer.withParallelizer(5) {
            final int result = ParallelArrayUtil.findParallel([1, 2, 3, 4, 5], {it > 6})
            assertNull result
        }
    }

    public void testAny() {
        Parallelizer.withParallelizer(5) {
            assert ParallelArrayUtil.anyParallel([1, 2, 3, 4, 5], {it > 2})
            assert !ParallelArrayUtil.anyParallel([1, 2, 3, 4, 5], {it > 6})
        }
    }

    public void testAll() {
        Parallelizer.withParallelizer(5) {
            assert ParallelArrayUtil.everyParallel([1, 2, 3, 4, 5], {it > 0})
            assert !ParallelArrayUtil.everyParallel([1, 2, 3, 4, 5], {it > 1})
        }
    }

    @SuppressWarnings("GroovyOverlyComplexBooleanExpression")
    public void testGroupBy() {
        Parallelizer.withParallelizer(5) {
            assert ParallelArrayUtil.groupByParallel([1, 2, 3, 4, 5], {it > 2})
            assert ParallelArrayUtil.groupByParallel([1, 2, 3, 4, 5], {Number number -> 1}).size() == 1
            assert ParallelArrayUtil.groupByParallel([1, 2, 3, 4, 5], {Number number -> number}).size() == 5
            final def groups = ParallelArrayUtil.groupByParallel([1, 2, 3, 4, 5], {Number number -> number % 2})
            assert groups.size() == 2
            assert (groups[0].containsAll([2, 4]) && groups[0].size() == 2) || (groups[0].containsAll([1, 3, 5]) && groups[0].size() == 3)
            assert (groups[1].containsAll([2, 4]) && groups[1].size() == 2) || (groups[1].containsAll([1, 3, 5]) && groups[1].size() == 3)

        }
    }

    public void testParallelPools() {
        final AtomicReference reference = new AtomicReference()
        final CyclicBarrier barrier1 = new CyclicBarrier(2)
        final CyclicBarrier barrier2 = new CyclicBarrier(2)

        def thread = new Thread({
            Parallelizer.withParallelizer(2) {
                final ForkJoinPool pool2 = Parallelizer.retrieveCurrentPool()
                reference.set pool2
                barrier1.await()
                barrier2.await()
            }
        } as Runnable)
        thread.start()
        Parallelizer.withParallelizer(5) {
            final ForkJoinPool pool1 = Parallelizer.retrieveCurrentPool()
            barrier1.await()
            final ForkJoinPool nestedPool = reference.get() as ForkJoinPool
            assert pool1 != nestedPool
            barrier2.await()
        }
    }

    public void testMissingPool() {
        final AtomicInteger counter = new AtomicInteger(0)
        shouldFail(IllegalStateException.class) {
            ParallelArrayUtil.eachParallel([], {counter.incrementAndGet()})
        }
        assertEquals 0, counter.get()
    }

    public void testNestedPools() {
        Parallelizer.doParallel{a->
            Parallelizer.doParallel{b->
                Parallelizer.doParallel{c->
                    Parallelizer.doParallel{d->
                        assert d != c != b != a
                        assert Parallelizer.retrieveCurrentPool() == d
                    }
                    assert Parallelizer.retrieveCurrentPool() == c
                }
                assert Parallelizer.retrieveCurrentPool() == b
            }
            assert Parallelizer.retrieveCurrentPool() == a
        }
    }

    public void testNestedExistingPools() {
        final def pool1 = new ForkJoinPool()
        final def pool2 = new ForkJoinPool()
        final def pool3 = new ForkJoinPool()
        Parallelizer.withExistingParallelizer(pool1){a->
            Parallelizer.withExistingParallelizer(pool2){b->
                Parallelizer.withExistingParallelizer(pool1){c->
                    Parallelizer.withExistingParallelizer(pool3){d->
                        assert d == pool3
                        assert c == pool1
                        assert b == pool2
                        assert a == pool1
                        assert Parallelizer.retrieveCurrentPool() == pool3
                    }
                    assert Parallelizer.retrieveCurrentPool() == pool1
                }
                assert Parallelizer.retrieveCurrentPool() == pool2
            }
            assert Parallelizer.retrieveCurrentPool() == pool1
        }
    }

    public void testExceptionHandler() {
        final AtomicInteger exceptionCount = new AtomicInteger(0)
        def handler = {Thread t, Throwable e ->
            exceptionCount.incrementAndGet()
        } as UncaughtExceptionHandler

        shouldFail(RuntimeException.class) {
            Parallelizer.withParallelizer(5, handler) {ForkJoinPool pool ->
                'abc'.eachParallel {throw new RuntimeException('test')}
            }
        }
//        assertEquals 3, exceptionCount.get()
    }

    public testNestedCalls() {
        Parallelizer.withParallelizer(5) {pool ->
            def result = ['abc', '123', 'xyz'].findAllParallel {word ->
                Parallelizer.withExistingParallelizer(pool) {
                    word.anyParallel {it in ['a', 'y', '5']}
                }
            }
            assertEquals(['abc', 'xyz'], result)
        }
    }

    public void testMin() {
        Parallelizer.doParallel(5) {
            assertEquals 1, [1, 2, 3, 4, 5].minParallel {a, b -> a - b}
            assertEquals 5, [1, 2, 3, 4, 5].minParallel {a, b -> b - a}
            assertEquals 1, [1, 2, 3, 4, 5].minParallel {it}
            assertEquals 1, [1, 2, 3, 4, 5].minParallel {it * 2}
            assertEquals 1, [1, 2, 3, 4, 5].minParallel {a -> a + 10}
            assertEquals 1, [1, 2, 3, 4, 5].minParallel()
            assertEquals 'a', 'abc'.minParallel()
        }
    }

    public void testMax() {
        Parallelizer.doParallel(5) {
            assertEquals 5, [1, 2, 3, 4, 5].maxParallel {a, b -> a - b}
            assertEquals 1, [1, 2, 3, 4, 5].maxParallel {a, b -> b - a}
            assertEquals 5, [1, 2, 3, 4, 5].maxParallel {it}
            assertEquals 5, [1, 2, 3, 4, 5].maxParallel {it * 2}
            assertEquals 5, [1, 2, 3, 4, 5].maxParallel {a -> a + 10}
            assertEquals 5, [1, 2, 3, 4, 5].maxParallel()
            assertEquals 'c', 'abc'.maxParallel()
        }
    }

    public void testSum() {
        Parallelizer.doParallel(5) {
            assertEquals 15, [1, 2, 3, 4, 5].sumParallel()
            assertEquals 'abc', 'abc'.sumParallel()
        }
    }

    public void testCount() {
        Parallelizer.doParallel(5) {
            assertEquals 1, [1, 2, 3, 4, 5].countParallel(3)
            assertEquals 5, [3, 2, 3, 4, 5, 3, 3, 3].countParallel(3)
            assertEquals 0, [3, 2, 3, 4, 5, 3, 3, 3].countParallel(6)
            assertEquals 0, [].countParallel(6)
            assertEquals 1, 'abc'.countParallel('a')
            assertEquals 3, 'abcaa'.countParallel('a')
            assertEquals 0, 'ebc'.countParallel('a')
            assertEquals 0, ''.countParallel('a')
        }
    }

    public void testReduce() {
        Parallelizer.doParallel(5) {
            assertEquals 15, [1, 2, 3, 4, 5].foldParallel() {a, b -> a + b}
            assertEquals 'abc', 'abc'.foldParallel {a, b -> a + b}
            assertEquals 55, [1, 2, 3, 4, 5].collectParallel {it ** 2}.foldParallel {a, b -> a + b}
        }
    }

    public void testReduceThreads() {
        final ConcurrentHashMap map = new ConcurrentHashMap()

        Parallelizer.doParallel(5) {
            assertEquals 55, [1, 2, 3, 4, 5, 6, 7, 8, 9, 10].foldParallel {a, b ->
                Thread.sleep 200
                map[Thread.currentThread()] = ''
                a + b
            }
            assert map.keys().size() > 1
        }
    }
}
