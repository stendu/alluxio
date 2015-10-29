/*
 * Licensed to the University of California, Berkeley under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package tachyon.heartbeat;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests for {@link HeartbeatThread}. This test uses
 * {@link tachyon.heartbeat.HeartbeatScheduler} to have synchronous tests.
 */
public final class HeartbeatThreadTest {

  private static final String THREAD_NAME = "heartbeat-thread-test-thread-name";

  private static final int NUMBER_OF_THREADS = 10;

  private final ExecutorService mExecutorService = Executors.newFixedThreadPool(NUMBER_OF_THREADS);

  @Test
  public void concurrentHeartbeatThreadTest() throws Exception {
    List<Thread> mThreads = new LinkedList<Thread>();

    // Start the threads.
    for (int i = 0; i < NUMBER_OF_THREADS; i ++) {
      Thread thread = new DummyHeartbeatTestThread(i);
      thread.start();
      mThreads.add(thread);
    }

    // Wait for the threads to finish.
    for (Thread thread : mThreads) {
      thread.join();
    }
  }

  private class DummyHeartbeatTestThread extends Thread  {
    private String mThreadName;

    public DummyHeartbeatTestThread(int id) {
      mThreadName = THREAD_NAME + "-" + id;
    }

    @Override
    public void run()  {
      try {
        HeartbeatContext.setTimerClass(mThreadName, HeartbeatContext.SCHEDULED_TIMER_CLASS);

        DummyHeartbeatExecutor executor = new DummyHeartbeatExecutor();
        HeartbeatThread ht = new HeartbeatThread(mThreadName, executor, 1);

        // Run the HeartbeatThread.
        mExecutorService.submit(ht);

        // Wait for the DummyHeartbeatExecutor executor to be ready to execute its heartbeat.
        Assert.assertTrue("Initial wait failed.",
            HeartbeatScheduler.await(mThreadName, 5, TimeUnit.SECONDS));

        final int numIterations = 100000;
        for (int i = 0; i < numIterations; i++) {
          HeartbeatScheduler.schedule(mThreadName);
          Assert.assertTrue("Iteration " + i + " failed.",
              HeartbeatScheduler.await(mThreadName, 5, TimeUnit.SECONDS));
        }

        Assert.assertEquals("The executor counter is wrong.", numIterations, executor.getCounter());
      } catch (Exception e) {
        throw new RuntimeException(e.getMessage());
      }
    }
  }

  private class DummyHeartbeatExecutor implements HeartbeatExecutor {

    private int mCounter = 0;

    @Override
    public void heartbeat() {
      mCounter ++;
    }

    public int getCounter() {
      return mCounter;
    }
  }
}
