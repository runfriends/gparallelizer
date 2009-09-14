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

package org.gparallelizer

import java.lang.Thread.UncaughtExceptionHandler
import java.util.concurrent.TimeUnit
import jsr166y.forkjoin.ForkJoinPool

/**
 * Enables a ParallelArray-based (from JSR-166y) DSL on collections. In general cases the Parallel Array implementation
 * shows to be much faster (10 - 20 times) compared to the executor service implementation in Asynchronizer.
 * E.g.
 * Parallelizer.withParallelizer(5) {
 *     final AtomicInteger result = new AtomicInteger(0)
 *     [1, 2, 3, 4, 5].eachAsync {result.addAndGet(it)}
 *     assertEquals 15, result
 * }
 *
 * Parallelizer.withParallelizer(5) {
 *     final List result = [1, 2, 3, 4, 5].collectAsync {it * 2}
 *     assert ([2, 4, 6, 8, 10].equals(result))
 * }
 *
 * Parallelizer.withParallelizer(5) {
 *     assert [1, 2, 3, 4, 5].allAsync {it > 0}
 *     assert ![1, 2, 3, 4, 5].allAsync {it > 1}
 * }
 *
 * @author Vaclav Pech
 * Date: Oct 23, 2008
 */
public class Parallelizer {

    private static ThreadLocal<ForkJoinPool> currentPool=new ThreadLocal<ForkJoinPool>()

    protected static retrieveCurrentPool() {
        return currentPool.get()
    }

    private static createPool() {
        return createPool(Runtime.getRuntime().availableProcessors() + 1)
    }

    private static createPool(int poolSize) {
        return createPool(poolSize, createDefaultUncaughtExceptionHandler())
    }

    private static createPool(int poolSize, UncaughtExceptionHandler handler) {
        if (!(poolSize in 1..Integer.MAX_VALUE)) throw new IllegalArgumentException("Invalid value $poolSize for the pool size has been specified. Please supply a positive int number.")
        final ForkJoinPool pool = new ForkJoinPool(poolSize)
        pool.uncaughtExceptionHandler=handler
        return pool
    }

    private static UncaughtExceptionHandler createDefaultUncaughtExceptionHandler() {
        return {Thread failedThread, Throwable throwable ->
            System.err.println "Error processing background thread ${failedThread.name}: ${throwable.message}"
            throwable.printStackTrace(System.err)
        } as UncaughtExceptionHandler
    }

    /**
     * Creates a new instance of <i>ForkJoinPool</i>, binds it to the current thread, enables the ParallelArray DSL
     * and runs the supplied closure.
     * It is an identical alternative for withParallelizer() with a shorter name.
     * Within the supplied code block the <i>ForkJoinPool</i> is available as the only parameter, collections have been
     * enhanced with the <i>eachAsync()</i>, <i>collectAsync()</i> and other methods from the <i>ParallelArrayUtil</i>
     * category class.
     * E.g. calling <i>images.eachAsync{processImage(it}}</i> will call the potentially long-lasting <i>processImage()</i>
     * operation on each image in the <i>images</i> collection in parallel.
     * Be sure to synchronize all modifiable state shared by the asynchronously running closures.
     * <pre>
     * Parallelizer.doParallel {ForkJoinPool pool ->
     *     def result = Collections.synchronizedSet(new HashSet())
     *     [1, 2, 3, 4, 5].eachAsync {Number number -> result.add(number * 10)}
     *     assertEquals(new HashSet([10, 20, 30, 40, 50]), result)
     * }
     * </pre>
     * @param cl The block of code to invoke with the DSL enabled
     */
    public static doParallel(Closure cl) {
        return withParallelizer(cl)
    }

  /**
   * Creates a new instance of <i>ForkJoinPool</i>, binds it to the current thread, enables the ParallelArray DSL
   * and runs the supplied closure.
   * It is an identical alternative for withParallelizer() with a shorter name.
   * Within the supplied code block the <i>ForkJoinPool</i> is available as the only parameter, collections have been
   * enhanced with the <i>eachAsync()</i>, <i>collectAsync()</i> and other methods from the <i>ParallelArrayUtil</i>
   * category class.
   * E.g. calling <i>images.eachAsync{processImage(it}}</i> will call the potentially long-lasting <i>processImage()</i>
   * operation on each image in the <i>images</i> collection in parallel.
   * Be sure to synchronize all modifiable state shared by the asynchronously running closures.
   * <pre>
   * Parallelizer.doParallel(5) {ForkJoinPool pool ->
   *     def result = Collections.synchronizedSet(new HashSet())
   *     [1, 2, 3, 4, 5].eachAsync {Number number -> result.add(number * 10)}
   *     assertEquals(new HashSet([10, 20, 30, 40, 50]), result)
   * }
   * </pre>
   * @param numberOfThreads Number of threads in the newly created thread pool
   * @param cl The block of code to invoke with the DSL enabled
   */
    public static doParallel(int numberOfThreads, Closure cl) {
        return withParallelizer(numberOfThreads, cl)
    }

  /**
   * Creates a new instance of <i>ForkJoinPool</i>, binds it to the current thread, enables the ParallelArray DSL
   * and runs the supplied closure.
   * It is an identical alternative for withParallelizer() with a shorter name.
   * Within the supplied code block the <i>ForkJoinPool</i> is available as the only parameter, collections have been
   * enhanced with the <i>eachAsync()</i>, <i>collectAsync()</i> and other methods from the <i>ParallelArrayUtil</i>
   * category class.
   * E.g. calling <i>images.eachAsync{processImage(it}}</i> will call the potentially long-lasting <i>processImage()</i>
   * operation on each image in the <i>images</i> collection in parallel.
   * Be sure to synchronize all modifiable state shared by the asynchronously running closures.
   * <pre>
   * Parallelizer.doParallel(5) {ForkJoinPool pool ->
   *     def result = Collections.synchronizedSet(new HashSet())
   *     [1, 2, 3, 4, 5].eachAsync {Number number -> result.add(number * 10)}
   *     assertEquals(new HashSet([10, 20, 30, 40, 50]), result)
   * }
   * </pre>
   * @param numberOfThreads Number of threads in the newly created thread pool
   * @param handler Handler for uncaught exceptions raised in code performed by the pooled threads
   * @param cl The block of code to invoke with the DSL enabled
   */
    public static doParallel(int numberOfThreads, UncaughtExceptionHandler handler, Closure cl) {
        return withParallelizer(numberOfThreads, handler, cl)
    }
    
    /**
     * Creates a new instance of <i>ForkJoinPool</i>, binds it to the current thread, enables the ParallelArray DSL
     * and runs the supplied closure.
     * Within the supplied code block the <i>ForkJoinPool</i> is available as the only parameter, collections have been
     * enhanced with the <i>eachAsync()</i>, <i>collectAsync()</i> and other methods from the <i>ParallelArrayUtil</i>
     * category class.
     * E.g. calling <i>images.eachAsync{processImage(it}}</i> will call the potentially long-lasting <i>processImage()</i>
     * operation on each image in the <i>images</i> collection in parallel.
     * Be sure to synchronize all modifiable state shared by the asynchronously running closures.
     * <pre>
     * Parallelizer.withParallelizer {ForkJoinPool pool ->
     *     def result = Collections.synchronizedSet(new HashSet())
     *     [1, 2, 3, 4, 5].eachAsync {Number number -> result.add(number * 10)}
     *     assertEquals(new HashSet([10, 20, 30, 40, 50]), result)
     * }
     * </pre>
     * @param cl The block of code to invoke with the DSL enabled
     */
    public static withParallelizer(Closure cl) {
        return withParallelizer(3, cl)
    }

  /**
   * Creates a new instance of <i>ForkJoinPool</i>, binds it to the current thread, enables the ParallelArray DSL
   * and runs the supplied closure.
   * Within the supplied code block the <i>ForkJoinPool</i> is available as the only parameter, collections have been
   * enhanced with the <i>eachAsync()</i>, <i>collectAsync()</i> and other methods from the <i>ParallelArrayUtil</i>
   * category class.
   * E.g. calling <i>images.eachAsync{processImage(it}}</i> will call the potentially long-lasting <i>processImage()</i>
   * operation on each image in the <i>images</i> collection in parallel.
   * Be sure to synchronize all modifiable state shared by the asynchronously running closures.
   * <pre>
   * Parallelizer.withParallelizer(5) {ForkJoinPool pool ->
   *     def result = Collections.synchronizedSet(new HashSet())
   *     [1, 2, 3, 4, 5].eachAsync {Number number -> result.add(number * 10)}
   *     assertEquals(new HashSet([10, 20, 30, 40, 50]), result)
   * }
   * </pre>
   * @param numberOfThreads Number of threads in the newly created thread pool
   * @param cl The block of code to invoke with the DSL enabled
   */
    public static withParallelizer(int numberOfThreads, Closure cl) {
        return withParallelizer(numberOfThreads, createDefaultUncaughtExceptionHandler(), cl)
    }

  /**
   * Creates a new instance of <i>ForkJoinPool</i>, binds it to the current thread, enables the ParallelArray DSL
   * and runs the supplied closure.
   * Within the supplied code block the <i>ForkJoinPool</i> is available as the only parameter, collections have been
   * enhanced with the <i>eachAsync()</i>, <i>collectAsync()</i> and other methods from the <i>ParallelArrayUtil</i>
   * category class.
   * E.g. calling <i>images.eachAsync{processImage(it}}</i> will call the potentially long-lasting <i>processImage()</i>
   * operation on each image in the <i>images</i> collection in parallel.
   * Be sure to synchronize all modifiable state shared by the asynchronously running closures.
   * <pre>
   * Parallelizer.withParallelizer(5) {ForkJoinPool pool ->
   *     def result = Collections.synchronizedSet(new HashSet())
   *     [1, 2, 3, 4, 5].eachAsync {Number number -> result.add(number * 10)}
   *     assertEquals(new HashSet([10, 20, 30, 40, 50]), result)
   * }
   * </pre>
   * @param numberOfThreads Number of threads in the newly created thread pool
   * @param handler Handler for uncaught exceptions raised in code performed by the pooled threads
   * @param cl The block of code to invoke with the DSL enabled
   */
    public static withParallelizer(int numberOfThreads, UncaughtExceptionHandler handler, Closure cl) {
        final ForkJoinPool pool = createPool(numberOfThreads, handler)
        try {
            return withExistingParallelizer(pool, cl)
        } finally {
            pool.shutdown()
            pool.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS)
        }
    }

  /**
   * Reuses an instance of <i>ForkJoinPool</i>, binds it to the current thread, enables the ParallelArray DSL
   * and runs the supplied closure.
   * Within the supplied code block the <i>ForkJoinPool</i> is available as the only parameter, collections have been
   * enhanced with the <i>eachAsync()</i>, <i>collectAsync()</i> and other methods from the <i>ParallelArrayUtil</i>
   * category class.
   * E.g. calling <i>images.eachAsync{processImage(it}}</i> will call the potentially long-lasting <i>processImage()</i>
   * operation on each image in the <i>images</i> collection in parallel.
   * Be sure to synchronize all modifiable state shared by the asynchronously running closures.
   * <pre>
   * Parallelizer.withExistingParallelizer(anotherPool) {ForkJoinPool pool ->
   *     def result = Collections.synchronizedSet(new HashSet())
   *     [1, 2, 3, 4, 5].eachAsync {Number number -> result.add(number * 10)}
   *     assertEquals(new HashSet([10, 20, 30, 40, 50]), result)
   * }
   * </pre>
   * @param pool The thread pool to use, the pool will not be shutdown after this method returns
   */
    public static withExistingParallelizer(ForkJoinPool pool, Closure cl) {

        currentPool.set(pool)
        def result=null
        try {
            use(ParallelArrayUtil) {
                result = cl(pool)
            }
        } finally {
            currentPool.remove()
        }
        return result
    }
}
