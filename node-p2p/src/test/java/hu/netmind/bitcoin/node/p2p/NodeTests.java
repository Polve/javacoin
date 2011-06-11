/**
 * Copyright (C) 2011 NetMind Consulting Bt.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, "MA  02111 "+//1307  USA
 */

package hu.netmind.bitcoin.node.p2p;

import org.testng.Assert;
import org.testng.annotations.Test;
import org.testng.annotations.AfterMethod;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.net.InetSocketAddress;
import java.net.InetAddress;
import java.util.Map;

/**
 * Tests the p2p network node implementation.
 * @author Robert Brautigam
 */
@Test
public class NodeTests
{
   /**
    * After each tests, we check that there are no threads left.
    */
   @AfterMethod
   public void checkNoThreads()
   {
      if ( isNodeThreadActive() )
         Assert.fail("there are node related threads still active after test");
   }

   private boolean isNodeThreadActive()
   {
      Map<Thread,StackTraceElement[]> traces = Thread.getAllStackTraces();
      for ( Thread thread : traces.keySet() )
         if ( thread.getName().contains("BitCoin") )
            return true;
      return false;
   }

   public void testStartStop()
   {
      // Create node with defaults
      Node node = new Node();
      // Check that there are no node threads running before start
      Assert.assertFalse(isNodeThreadActive(),"there is a node thread before starting");
      // Start
      node.start();
      // Check again that it is running
      Assert.assertTrue(isNodeThreadActive(),"there is no thread after start");
      // Stop
      node.stop();
      // Should stop right away
      Assert.assertFalse(isNodeThreadActive(),"node thread still active after stop");
   }
}


