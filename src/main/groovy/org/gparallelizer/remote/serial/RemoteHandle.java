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

package org.gparallelizer.remote.serial;

import java.io.Serializable;
import java.io.ObjectStreamException;
import java.io.WriteAbortedException;
import java.util.UUID;

/**
* @author Alex Tkachman
*/
public class RemoteHandle implements Serializable {
    private final UUID serialId;

    private final Class klazz;

    public RemoteHandle(UUID id, Class klazz) {
        this.serialId = id;
        this.klazz = klazz;
    }

    protected Object readResolve () throws ObjectStreamException {
        try {
            WithSerialId obj = (WithSerialId) klazz.newInstance();
            obj.initDeserial(serialId);
            return obj;
        } catch (Exception t) {
            throw new WriteAbortedException(t.getMessage(), t);
        }
    }
}
