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
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.ServerSocket;
import java.util.Arrays;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

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
    * Create a new dummy node which connects to a node and has no server.
    */
   public DummyNode createDummyNode(InetSocketAddress address)
      throws IOException
   {
      DummyNode node = new DummyNode(address);
      dummyNodes.add(node);
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
      throws IOException
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
      node.addHandler(new MessageRepeaterHandler());
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

   public void testBroadcast()
      throws IOException
   {
      DummyNode dummyNodes[] = new DummyNode[3];
      for ( int i=0; i<dummyNodes.length; i++ )
         dummyNodes[i] = createDummyNode();
      // Create bootstrapper
      List<InetSocketAddress> addresses = new ArrayList<InetSocketAddress>();
      for ( int i=0; i<dummyNodes.length; i++ )
         addresses.add(dummyNodes[i].getAddress());
      AddressSource source = EasyMock.createMock(AddressSource.class);
      EasyMock.expect(source.getAddresses()).andReturn(addresses);
      EasyMock.replay(source);
      // Create node
      Node node = createNode();
      node.setAddressSource(source);
      // Start node
      node.start();
      // Accept the connection from node
      for ( int i=0; i<dummyNodes.length; i++ )
         dummyNodes[i].accept();
      // Broadcast a message to all connected nodes
      node.broadcast(new AlertMessage(Message.MAGIC_TEST,"Message","Signature"));
      // Get the messages from all nodes
      for ( int i=0; i<dummyNodes.length; i++ )
      {
         AlertMessage answer = (AlertMessage) dummyNodes[i].read();
         // Check
         Assert.assertEquals(answer.getMessage(),"Message");
         Assert.assertEquals(answer.getSignature(),"Signature");
      }
   }

   public void testSameAddressTwice()
      throws IOException
   {
      DummyNode dummyNode = createDummyNode();
      // Create bootstrapper
      List<InetSocketAddress> addresses = new ArrayList<InetSocketAddress>();
      addresses.add(dummyNode.getAddress());
      addresses.add(dummyNode.getAddress()); // 2nd time
      AddressSource source = EasyMock.createMock(AddressSource.class);
      EasyMock.expect(source.getAddresses()).andReturn(addresses);
      EasyMock.replay(source);
      // Create node
      Node node = createNode();
      node.setAddressSource(source);
      // Start node
      node.start();
      // Accept the connection from node
      dummyNode.accept();
      // Broadcast 2 messages
      node.broadcast(new AlertMessage(Message.MAGIC_TEST,"Message1","Signature"));
      node.broadcast(new AlertMessage(Message.MAGIC_TEST,"Message2","Signature"));
      // Now check that the 1st message arrives only once
      AlertMessage message = (AlertMessage) dummyNode.read();
      Assert.assertEquals(message.getMessage(),"Message1");
      message = (AlertMessage) dummyNode.read();
      Assert.assertEquals(message.getMessage(),"Message2");
   }

   public void testAcceptExternalConnection()
      throws IOException
   {
      // Create & start node
      Node node = createNode();
      node.start();
      // Create dummy
      DummyNode dummyNode = createDummyNode(new InetSocketAddress(node.getPort()));
      // Check connection
      node.broadcast(new AlertMessage(Message.MAGIC_TEST,"Message","Signature"));
      AlertMessage message = (AlertMessage) dummyNode.read();
      Assert.assertEquals(message.getMessage(),"Message");
   }

   public void testMaxConnections()
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
      node.setMinConnections(1);
      node.setMaxConnections(1);
      node.setAddressSource(source);
      // Start node
      node.start();
      // Accept the connection from node
      dummyNode.accept();
      // Now try to connect new node and communicate
      DummyNode dummyNode2 = createDummyNode(new InetSocketAddress(node.getPort()));
      try
      {
         Message message = dummyNode2.read();
         Assert.fail("read successful although node should not be connected");
      } catch ( IOException e ) {
         // Good, communication failed because socket should be closed
      }
   }

   public void testCleanupNodes()
      throws IOException, InterruptedException
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
      node.setMinConnections(1);
      node.setMaxConnections(1);
      node.setAddressSource(source);
      // Create a handler which repeats and also notifies
      final Semaphore semaphore = new Semaphore(0);
      node.addHandler(new MessageRepeaterHandler(){
               public void onLeave(Connection conn)
               {
                  semaphore.release();
               }
            });
      // Start node
      node.start();
      // Accept the connection from node
      dummyNode.accept();
      // Now terminate the connection to the dummy node
      dummyNode.close();
      // Wait until the node releases the node
      if ( ! semaphore.tryAcquire(500,TimeUnit.MILLISECONDS) )
         Assert.fail("didn't receive the 'leave' event of closed node in time");
      // Now try to connect new node and communicate
      DummyNode dummyNode2 = createDummyNode(new InetSocketAddress(node.getPort()));
      dummyNode2.send(new PingMessage(Message.MAGIC_TEST));
      Message message = dummyNode2.read();
   }

   public void testSingleHandlerReply()
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
      // Create two repeater handlers
      node.addHandler(new MessageRepeaterHandler());
      node.addHandler(new MessageRepeaterHandler());
      // Create a mock to make sure all handlers are invoked
      MessageHandler signalHandler = EasyMock.createMock(MessageHandler.class);
      EasyMock.expect(signalHandler.onJoin((Connection) EasyMock.anyObject())).andReturn(null);
      EasyMock.expect(signalHandler.onMessage(
               (Connection) EasyMock.anyObject(),(Message) EasyMock.anyObject())).andReturn(null).times(2);
      signalHandler.onLeave((Connection) EasyMock.anyObject()); 
      EasyMock.expectLastCall().anyTimes(); // This depends on timing whether it is actually invoked from cleanup()
      EasyMock.replay(signalHandler);
      node.addHandler(signalHandler);
      // Start node
      node.start();
      // Accept the connection from node
      dummyNode.accept();
      // Send two messages to node
      dummyNode.send(new AlertMessage(Message.MAGIC_TEST,"Message1","Signature"));
      dummyNode.send(new AlertMessage(Message.MAGIC_TEST,"Message2","Signature"));
      // Check that first message only arrives once
      AlertMessage incoming = (AlertMessage) dummyNode.read();
      Assert.assertEquals(incoming.getMessage(),"Message1");
      incoming = (AlertMessage) dummyNode.read();
      Assert.assertEquals(incoming.getMessage(),"Message2");
      // Check that the last control handler was invoked at both messages
      EasyMock.verify(signalHandler);
      EasyMock.reset(signalHandler);
   }

   public void testInitialMessage()
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
      node.addHandler(new MessageRepeaterHandler() {
               public Message onJoin(Connection conn)
               {
                  return new VerackMessage(Message.MAGIC_TEST);
               }
            });
      // Start node
      node.start();
      // Accept the connection from node
      dummyNode.accept();
      // Dummy node should now have received the message automatically
      VerackMessage verack = (VerackMessage) dummyNode.read();
   }

   public void testCleanupInitialTimeoutNodes()
      throws IOException, InterruptedException
   {
      DummyNode dummyNode = createDummyNode();
      // Create bootstrapper
      List<InetSocketAddress> addresses = new ArrayList<InetSocketAddress>();
      addresses.add(dummyNode.getAddress());
      AddressSource source = EasyMock.createMock(AddressSource.class);
      EasyMock.expect(source.getAddresses()).andReturn(addresses).anyTimes();
      EasyMock.replay(source);
      // Create node
      Node node = createNode();
      node.setSoTimeout(100); // Make sure resolution is small enough
      node.setInitialTimeout(100); // 100 msec
      node.setAddressSource(source);
      // Create a handler which repeats and also notifies
      final Semaphore semaphore = new Semaphore(0);
      node.addHandler(new MessageRepeaterHandler(){
               public void onLeave(Connection conn)
               {
                  semaphore.release();
               }
            });
      // Start node
      node.start();
      // Accept the connection from node
      dummyNode.accept();
      // The node should release this node soon, because there was no message
      if ( ! semaphore.tryAcquire(500,TimeUnit.MILLISECONDS) )
         Assert.fail("didn't receive the 'leave' event, although the initial timeout should have ran out");
   }

   private class DummyNode 
   {
      private ServerSocket serverSocket;
      private Socket socket = null;
      private MessageMarshaller marshaller;
      private BitCoinInputStream input;
      private BitCoinOutputStream output;
      private InetSocketAddress address;

      /**
       * Make dummy "client" node which connects from external.
       */
      public DummyNode(InetSocketAddress address)
         throws IOException
      {
         this.address=address;
         socket = new Socket();
         socket.setSoTimeout(1000);
         socket.connect(address,1000); // 0.5 seconds to connect
         input = new BitCoinInputStream(new BufferedInputStream(socket.getInputStream()));
         output = new BitCoinOutputStream(socket.getOutputStream());
         marshaller = new MessageMarshaller();
      }

      /**
       * Make dummy server.
       */
      public DummyNode()
         throws IOException
      {
         serverSocket = new ServerSocket(0);
         address = (InetSocketAddress) serverSocket.getLocalSocketAddress();
         serverSocket.setSoTimeout(1000); // Wait for incoming for 0.5 sec
         marshaller = new MessageMarshaller();
      }

      public InetSocketAddress getAddress()
      {
         return address;
      }

      public boolean isAccepted()
      {
         return socket != null;
      }

      public boolean isClosed()
      {
         return socket.isClosed();
      }

      public void close()
         throws IOException
      {
         if ( serverSocket != null )
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

   public class MessageRepeaterHandler implements MessageHandler
   {
      public Message onJoin(Connection conn)
      {
         return null;
      }

      public void onLeave(Connection conn)
      {
      }

      public Message onMessage(Connection conn, Message message)
      {
         // Send right back
         return message;
      }
   }
}


