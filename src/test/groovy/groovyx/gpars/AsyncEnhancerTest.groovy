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

public class AsyncEnhancerTest extends GroovyTestCase {
    public void testInstanceEnhancement() {
        final List list = [1, 2, 3, 4, 5]
        AsyncEnhancer.enhanceInstance list
        assert list.anyParallel {it > 4}
        assert list.everyParallel {it > 0}
        assertEquals 1, list.findParallel {it < 2}
        assertEquals([1, 2], list.findAllParallel {it < 3})
        assertEquals([2, 4, 6, 8, 10], list.collectParallel {2 * it})
        def result = Collections.synchronizedSet(new HashSet())
        list.eachParallel {result << 2 * it}
        assertEquals(new HashSet([2, 4, 6, 8, 10]), result)
    }

    public void testClassEnhancement() {
        AsyncEnhancer.enhanceClass LinkedList
        final List list = new LinkedList([1, 2, 3, 4, 5])
        assert list.anyParallel {it > 4}
        assert list.everyParallel {it > 0}
        assertEquals 1, list.findParallel {it < 2}
        assertEquals([1, 2], list.findAllParallel {it < 3})
        assertEquals([2, 4, 6, 8, 10], list.collectParallel {2 * it})
        def result = Collections.synchronizedSet(new HashSet())
        list.eachParallel {result << 2 * it}
        assertEquals(5, result.size())
        assertEquals(new HashSet([2, 4, 6, 8, 10]), result)
    }

    public void testMapInstanceEnhancement() {
        final Map map = [1: 1, 2: 2, 3: 3, 4: 4, 5: 5]
        AsyncEnhancer.enhanceInstance map
        assert map.anyParallel {it.key > 4}
        assert map.everyParallel {it.value > 0}
    }

    public void testMapClassEnhancement() {
        AsyncEnhancer.enhanceClass HashMap
        final Map map = new HashMap([1: 1, 2: 2, 3: 3, 4: 4, 5: 5])
        assert map.anyParallel {it.key > 4}
        assert map.everyParallel {it.value > 0}
    }

    public void testInstanceEnhancementException() {
        final List list = [1, 2, 3, 4, 5]
        AsyncEnhancer.enhanceInstance list
        performExceptionCheck(list)
    }

    public void testClassEnhancementException() {
        AsyncEnhancer.enhanceClass LinkedList
        final List list = new LinkedList([1, 2, 3, 4, 5])
        performExceptionCheck(list)
    }

    public void testDualEnhancement() {
        AsyncEnhancer.enhanceClass LinkedList
        final List list = new LinkedList([1, 2, 3, 4, 5])
        assertEquals([2, 4, 6, 8, 10], list.collectParallel {2 * it})

        AsyncEnhancer.enhanceInstance list
        assertEquals([2, 4, 6, 8, 10], list.collectParallel {2 * it})

        assertEquals([2, 4, 6, 8, 10], new LinkedList([1, 2, 3, 4, 5]).collectParallel {2 * it})
    }

    private String performExceptionCheck(List list) {
        shouldFail(AsyncException) {list.anyParallel {if (it > 4) throw new IllegalArgumentException('test') else false}}
        shouldFail(AsyncException) {list.everyParallel {if (it > 4) throw new IllegalArgumentException('test') else true}}
        shouldFail(AsyncException) {list.findParallel {if (it > 4) throw new IllegalArgumentException('test') else false}}
        shouldFail(AsyncException) {list.findAllParallel {if (it > 4) throw new IllegalArgumentException('test') else true}}
        shouldFail(AsyncException) {list.collectParallel {if (it > 4) throw new IllegalArgumentException('test') else 1}}
        shouldFail(AsyncException) {list.eachParallel {if (it > 4) throw new IllegalArgumentException('test')}}
    }

}
