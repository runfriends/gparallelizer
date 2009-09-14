//  GParallelizer
//
//  Copyright � 2008-9  The original author or authors
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

import java.util.Set;

/**
 * @author Alex Tkachman
 */
public abstract class DataFlowComplexExpression<T> extends DataFlowExpression<T>  {
    protected Object[] args;

    public DataFlowComplexExpression(final Object... elements) {
        this.args = elements;
    }

    protected void collectDataFlowExpressions(Set<DataFlowExpression> collection) {
        for (int i = 0; i != args.length; ++i) {
            Object element = args[i];
            if (element instanceof DataFlowExpression) {
                collection.add((DataFlowExpression)element);
            }
        }
    }
}