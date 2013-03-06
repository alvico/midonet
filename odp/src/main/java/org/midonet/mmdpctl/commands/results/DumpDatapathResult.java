/*
* Copyright 2012 Midokura Europe SARL
*/
package org.midonet.mmdpctl.commands.results;

import org.midonet.odp.Datapath;
import org.midonet.odp.Flow;

import java.util.Set;


public class DumpDatapathResult implements Result {
    Set<Flow> flows;
    Datapath datapath;

    public DumpDatapathResult(Datapath datapath, Set<Flow> flows) {
        this.flows = flows;
        this.datapath = datapath;
    }

    @Override
    public void printResult() {
        if (flows.isEmpty()) {
            System.out.println("No flows for the selected datapath ("+datapath.getName()+").");
        } else {
            System.out.println("Displaying " + flows.size() + " flows: ");
            for (Flow flow : flows) {
                System.out.println("\t"+flow.toString());
            }
        }
    }
}