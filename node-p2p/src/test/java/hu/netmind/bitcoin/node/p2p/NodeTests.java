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
import java.io.BufferedInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.easymock.EasyMock;
import java.net.InetSocketAddress;
import java.net.InetAddress;
import java.net.Socket;
import java.net.ServerSocket;
import java.util.Arrays;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

/**
 * Tests the p2p network node implementation.
 * @author Robert Brautigam
 */
@Test
public class NodeTests
{
   private static final Logger logger = LoggerFactory.getLogger(NodeTests.class);

   private List<DummyNode> dummyNodes = new ArrayList<DummyNode>();
   private Node node = null;

   /**
    * Create a node which will be destroyed at the end of test.
    */
   public Node createNode()
   {
      node = new Node();
      return node;
   }

   /**
    * Create a new dummy node.
    */
   public DummyNode createDummyNode()
      throws IOException
   {
      DummyNode node = new DummyNode();
      dummyNodes.add(node);
      return node;
   }

   @AfterMethod
   public void cleanup()
   {
      // Stop node if it was created with createNode()
      if ( node != null )
         node.stop();
      node = null;
      // Stop all dummy nodes
      for ( DummyNode dummyNode : dummyNodes )
      {
         try
         {
            dummyNode.close();
         } catch ( IOException e ) {
            // All good
         }
      }
      dummyNodes.clear();
      // Check that everything really stopped
      StackTraceElement[] stack = getNodeStackTraceElements();
      if ( stack != null )
         Assert.fail("there are node related threads still active after test, stack: "+Arrays.toString(stack));
   }

   private StackTraceElement[] getNodeStackTraceElements()
   {
      Map<Thread,StackTraceElement[]> traces = Thread.getAllStackTraces();
      for ( Map.Entry<Thread,StackTraceElement[]> entry : traces.entrySet() )
         if ( entry.getKey().getName().contains("BitCoin") )
            return entry.getValue();
      return null;
   }

   private boolean isNodeThreadActive()
   {
      return getNodeStackTraceElements() != null;
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

   public void testBoostrapWithGoodNode()
      throws IOException
   {
      DummyNode dummyNode = createDummyNode();
      // Create bootstrapper
      List<InetSocketAddress> addresses = new ArrayList<InetSocketAddress>();
      addresses.add(dummyNode.getAddress());
      AddressSource source = EasyMock.createMock(AddressSource.class);
      EasyMock.expect(source.getAddresses()).andReturn(addresses);
      EasyMock.replay(source);
      // Create node
      Node node = createNode();
      node.setAddressSource(source);
      node.start();
      // Wait for connection on dummy node
      dummyNode.accept();
      Assert.assertTrue(dummyNode.isAccepted());
   }

   public void testCommunicationConcept()
      throws IOException
   {
      DummyNode dummyNode = createDummyNode();
      // Create bootstrapper
      List<InetSocketAddress> addresses = new ArrayList<InetSocketAddress>();
      addresses.add(dummyNode.getAddress());
      AddressSource source = EasyMock.createMock(AddressSource.class);
      EasyMock.expect(source.getAddresses()).andReturn(addresses);
      EasyMock.replay(source);
      // Create node
      Node node = createNode();
      node.setAddressSource(source);
      // Create a repeater handler
      node.addHandler(new MessageHandler() {
               public Message handle(Message message)
               {
                  // Send right back
                  return message;
               }
            });
      // Start node
      node.start();
      // Accept the connection from node
      dummyNode.accept();
      // Send a message to node
      dummyNode.send(new AlertMessage(Message.MAGIC_TEST,"Message","Signature"));
      // Get the repeated message right back
      AlertMessage answer = (AlertMessage) dummyNode.read();
      // Check
      Assert.assertEquals(answer.getMessage(),"Message");
      Assert.assertEquals(answer.getSignature(),"Signature");
   }

   private class DummyNode 
   {
      private ServerSocket serverSocket;
      private Socket socket = null;
      private MessageMarshaller marshaller;
      private BitCoinInputStream input;
      private BitCoinOutputStream output;

      public DummyNode()
         throws IOException
      {
         serverSocket = new ServerSocket(0);
         serverSocket.setSoTimeout(500); // Wait for incoming for 0.5 sec
         marshaller = new MessageMarshaller();
      }

      public InetSocketAddress getAddress()
      {
         return (InetSocketAddress) serverSocket.getLocalSocketAddress();
      }

      public boolean isAccepted()
      {
         return socket != null;
      }

      public void close()
         throws IOException
      {
         serverSocket.close();
         if ( socket != null )
            socket.close();
      }

      public Message read()
         throws IOException
      {
         return marshaller.read(input);
      }

      public void send(Message message)
         throws IOException
      {
         marshaller.write(message,output);
      }

      public void accept()
         throws IOException
      {
         socket = serverSocket.accept();
         input = new BitCoinInputStream(new BufferedInputStream(socket.getInputStream()));
         output = new BitCoinOutputStream(socket.getOutputStream());
      }
   }
}


