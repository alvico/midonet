/*ß
 * Copyright 2012 Midokura Europe SARL
 */
package com.midokura.util.process;

import java.util.concurrent.TimeUnit;

import javax.swing.*;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;

import static com.midokura.util.process.ProcessHelper.OutputStreams.StdError;
import static com.midokura.util.process.ProcessHelper.OutputStreams.StdOutput;

/**
 * Tests that test the ProcessHelper internal api entry point.
 *
 * @author Mihai Claudiu Toader <mtoader@midokura.com>
 *         Date: 3/30/12
 */
public class TestProcessHelper {

    private static final Logger log = LoggerFactory
        .getLogger(TestProcessHelper.class);

    @Test
    public void testLaunchProcess() throws Exception {
        assertCommandCode("false", 1);
        assertCommandCode("true", 0);
    }

    @Test
    public void testNoCommand() throws Exception {
        assertCommandCode("xyzt", -1);
    }

    @Test
    public void testLongerProcess() throws Exception {
        long currentTime = System.currentTimeMillis();

        assertCommandCode("sleep 1", 0);

        assertThat("The process helper waited at least 1 second",
                   System.currentTimeMillis() - currentTime,
                   greaterThan(TimeUnit.SECONDS.toMillis(1)));
    }

    private void assertCommandOutpout(String command, String stdOutput, String stdError) {
//        ProcessHelper.newProcess(command)
//                   .setDrainTarget(DrainTargets.stringCollector());
    }

    private void assertCommandCode(String command, int code) {
        assertThat(
            "The command \"" + command + "\" didn't return the proper exit code",
            ProcessHelper
                .newProcess(command)
                .logOutput(log, "command",
                           StdOutput, StdError)
                .runAndWait(),
            equalTo(code));
    }
}