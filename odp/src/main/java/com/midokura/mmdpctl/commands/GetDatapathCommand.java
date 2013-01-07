/*
* Copyright 2012 Midokura Europe SARL
*/
package com.midokura.mmdpctl.commands;

import java.util.concurrent.Future;

import com.midokura.mmdpctl.commands.callables.GetDatapathCallable;
import com.midokura.mmdpctl.commands.results.GetDatapathResult;
import com.midokura.odp.protos.OvsDatapathConnection;


public class GetDatapathCommand extends Command<GetDatapathResult> {

    private String datapathName;

    public GetDatapathCommand(String datapathName) {
        this.datapathName = datapathName;
    }

    public Future<GetDatapathResult> execute(OvsDatapathConnection connection) {
        return run(new GetDatapathCallable(connection, datapathName));
    }

}