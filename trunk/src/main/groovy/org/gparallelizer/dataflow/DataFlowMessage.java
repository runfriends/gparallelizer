//  GParallelizer
//
//  Copyright © 2008-9 Vaclav Pech
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

package org.gparallelizer.dataflow;

/**
 * The parent message for messages used internally to implement Dataflow Concurrency.
 *
 * @author Vaclav Pech
 * Date: Jun 5, 2009
 */
class DataFlowMessage {

    /**
     * The message to send to dataflow actors to terminate them.
     */
    static final DataFlowMessage EXIT = new DataFlowMessage();
}
