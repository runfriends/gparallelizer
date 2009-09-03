package org.gparallelizer.dataflow

import org.gparallelizer.dataflow.DataFlowVariable as DF
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

/**
 *
 *
 * @author Vaclav Pech, Dierk Koenig
 * Date: Sep 3, 2009
 */
public class DataFlows {
//   todo javadoc

    private ConcurrentMap variables = new ConcurrentHashMap()

    void setProperty(String name, value) {
        ensureToContainVariable(name)
        variables[name] << value
    }

    def getProperty(String name) {
        ensureToContainVariable(name)
        variables[name].val
    }

    private ensureToContainVariable(String name) { 
	    variables.putIfAbsent(name, new DF()) 
	}
}