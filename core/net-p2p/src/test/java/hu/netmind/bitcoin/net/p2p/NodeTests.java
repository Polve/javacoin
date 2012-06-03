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

package hu.netmind.bitcoin.net.p2p;

import hu.netmind.bitcoin.net.AlertMessage;
import hu.netmind.bitcoin.net.BitCoinInputStream;
import hu.netmind.bitcoin.net.BitCoinOutputStream;
import hu.netmind.bitcoin.net.Message;
import hu.netmind.bitcoin.net.MessageMarshaller;
import hu.netmind.bitcoin.net.NodeAddress;
import hu.netmind.bitcoin.net.PingMessage;
import hu.netmind.bitcoin.net.VerackMessage;
import hu.netmind.bitcoin.net.VersionMessage;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import org.easymock.EasyMock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

/**
 * Tests the p2p network node implementation.
 * @author Robert Brautigam
 */
@Test
public class NodeTests
{
   private static final Logger logger = LoggerFactory.getLogger(NodeTests.class);
   private static long BC_PROTOCOL_VERSION = 32100;

   private List<DummyNode> dummyNodes = new ArrayList<>();
   private Node savedNode = null;

   /**
    * Create a node which will be destroyed at the end of test.
    */
   public Node createNode()
   {
      savedNode = new Node();
      return savedNode;
   }

   /**
    * Create a new dummy node which connects to a node and has no server.
    */
   DummyNode createDummyNode(InetSocketAddress address)
      throws IOException
   {
      DummyNode node = new DummyNode(address);
      dummyNodes.add(node);
      return node;
   }

   /**
    * Create a new dummy node.
    */
   DummyNode createDummyNode()
      throws IOException
   {
      DummyNode node = new DummyNode();
      dummyNodes.add(node);
      return node;
   }

   @AfterMethod
   public void cleanup()
   {
      logger.debug("running cleanup...");
      // Stop node if it was created with createNode()
      if ( savedNode != null )
         savedNode.stop();
      savedNode = null;
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
      {
         Assert.fail("there are node related threads still active after test, stack: "+Arrays.toString(stack));
      }
   }

   private StackTraceElement[] getNodeStackTraceElements()
   {
      Map<Thread,StackTraceElement[]> traces = Thread.getAllStackTraces();
      for ( Map.Entry<Thread,StackTraceElement[]> entry : traces.entrySet() )
         if ( entry.getKey().getName().contains("BitCoin") )
         {
            logger.debug("found running BitCoin thread: "+entry.getKey().getName()+", stacktrace: "+Arrays.toString(entry.getValue()));
            return entry.getValue();
         }
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
      List<InetSocketAddress> addresses = new ArrayList<>();
      addresses.add(dummyNode.getAddress());
      AddressSource source = EasyMock.createMock(AddressSource.class);
      EasyMock.expect(source.getAddresses()).andReturn(addresses).anyTimes();
      EasyMock.replay(source);
      // Create node
      Node node = createNode();
      node.setAddressSource(source);
      node.start();
      // Wait for connection on dummy node
      dummyNode.accept();
      Assert.assertTrue(dummyNode.isAccepted());
   }

   public void testDetectConnectioToSelf()
      throws IOException
   {
      DummyNode dummyNode = createDummyNode();
      // Create bootstrapper
      List<InetSocketAddress> addresses = new ArrayList<>();
      addresses.add(dummyNode.getAddress());
      AddressSource source = EasyMock.createMock(AddressSource.class);
      EasyMock.expect(source.getAddresses()).andReturn(addresses).anyTimes();
      EasyMock.replay(source);

      final NodeAddress dummyNodeAddress = new NodeAddress(1, new InetSocketAddress(51234));
      final VersionMessage verMsg1 = new VersionMessage(Message.MAGIC_TEST, BC_PROTOCOL_VERSION, 0, System.currentTimeMillis() / 1000,
         dummyNodeAddress, dummyNodeAddress, 123, "test", 1);
      final VersionMessage verMsg2 = new VersionMessage(Message.MAGIC_TEST, BC_PROTOCOL_VERSION, 0, System.currentTimeMillis() / 1000,
         dummyNodeAddress, dummyNodeAddress, 124, "test", 1);

      // Create node
      Node node = createNode();
      node.setAddressSource(source);
      // Create a repeater handler
      node.addHandler(new VersionMessageSenderHandler(verMsg1));
      // Start node
      node.start();
      // Accept the connection from node
      dummyNode.accept();
      Assert.assertTrue(dummyNode.isAccepted());

      // Send different versionMessage to node
      dummyNode.send(verMsg2);

      // Verify we are able to read the version message back
      dummyNode.read();

      // This time send the same versionMessage
      dummyNode.send(verMsg1);

      // Verify we are unable to get the repeated message back because the
      // node detected connection to self and shut down the connection
      try
      {
         dummyNode.read();
         Assert.fail("Connection not closed from node");
      } catch (IOException ex)
      {
      }
   }

   public void testCommunicationConcept()
      throws IOException
   {
      DummyNode dummyNode = createDummyNode();
      // Create bootstrapper
      List<InetSocketAddress> addresses = new ArrayList<>();
      addresses.add(dummyNode.getAddress());
      AddressSource source = EasyMock.createMock(AddressSource.class);
      EasyMock.expect(source.getAddresses()).andReturn(addresses).anyTimes();
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
      dummyNode.send(new AlertMessage(Message.MAGIC_TEST,"Message"));
      // Get the repeated message right back
      AlertMessage answer = (AlertMessage) dummyNode.read();
      // Check
      Assert.assertEquals(answer.getMessage(),"Message");
   }

   public void testBroadcast()
      throws IOException
   {
      DummyNode dummyNodes[] = new DummyNode[3];
      for ( int i=0; i<dummyNodes.length; i++ )
         dummyNodes[i] = createDummyNode();
      // Create bootstrapper
      List<InetSocketAddress> addresses = new ArrayList<>();
      for ( int i=0; i<dummyNodes.length; i++ )
         addresses.add(dummyNodes[i].getAddress());
      AddressSource source = EasyMock.createMock(AddressSource.class);
      EasyMock.expect(source.getAddresses()).andReturn(addresses).anyTimes();
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
      node.broadcast(new AlertMessage(Message.MAGIC_TEST,"Message"));
      // Get the messages from all nodes
      for ( int i=0; i<dummyNodes.length; i++ )
      {
         AlertMessage answer = (AlertMessage) dummyNodes[i].read();
         // Check
         Assert.assertEquals(answer.getMessage(),"Message");
      }
   }

   public void testSameAddressTwice()
      throws IOException
   {
      DummyNode dummyNode = createDummyNode();
      // Create bootstrapper
      List<InetSocketAddress> addresses = new ArrayList<>();
      addresses.add(dummyNode.getAddress());
      addresses.add(dummyNode.getAddress()); // 2nd time
      AddressSource source = EasyMock.createMock(AddressSource.class);
      EasyMock.expect(source.getAddresses()).andReturn(addresses).anyTimes();
      EasyMock.replay(source);
      // Create node
      Node node = createNode();
      node.setAddressSource(source);
      // Start node
      node.start();
      // Accept the connection from node
      dummyNode.accept();
      // Broadcast 2 messages
      node.broadcast(new AlertMessage(Message.MAGIC_TEST,"Message1"));
      node.broadcast(new AlertMessage(Message.MAGIC_TEST,"Message2"));
      // Now check that the 1st message arrives only once
      AlertMessage message = (AlertMessage) dummyNode.read();
      Assert.assertEquals(message.getMessage(),"Message1");
      message = (AlertMessage) dummyNode.read();
      Assert.assertEquals(message.getMessage(),"Message2");
   }

   public void testAcceptExternalConnection()
      throws IOException, InterruptedException
   {
      // Create & start node
      Node node = createNode();
      JoinWaiterHandler waiter = new JoinWaiterHandler();
      node.addHandler(waiter);
      node.start();
      // Create dummy
      DummyNode dummyNode = createDummyNode(new InetSocketAddress(node.getPort()));
      // Wait unilt the node really connected to the node
      waiter.waitForJoin();
      // Check connection
      node.broadcast(new AlertMessage(Message.MAGIC_TEST,"Message"));
      AlertMessage message = (AlertMessage) dummyNode.read();
      Assert.assertEquals(message.getMessage(),"Message");
   }

   public void testMaxConnections()
      throws IOException
   {
      DummyNode dummyNode = createDummyNode();
      // Create bootstrapper
      List<InetSocketAddress> addresses = new ArrayList<>();
      addresses.add(dummyNode.getAddress());
      AddressSource source = EasyMock.createMock(AddressSource.class);
      EasyMock.expect(source.getAddresses()).andReturn(addresses).anyTimes();
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
      List<InetSocketAddress> addresses = new ArrayList<>();
      addresses.add(dummyNode.getAddress());
      AddressSource source = EasyMock.createMock(AddressSource.class);
      EasyMock.expect(source.getAddresses()).andReturn(addresses).anyTimes();
      EasyMock.replay(source);
      // Create node
      Node node = createNode();
      node.setMinConnections(1);
      node.setMaxConnections(1);
      node.setAddressSource(source);
      // Create a handler which repeats and also notifies
      final Semaphore semaphore = new Semaphore(0);
      node.addHandler(new MessageRepeaterHandler(){
         @Override
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

   public void testMultiHandlerReply()
      throws IOException
   {
      DummyNode dummyNode = createDummyNode();
      // Create bootstrapper
      List<InetSocketAddress> addresses = new ArrayList<>();
      addresses.add(dummyNode.getAddress());
      AddressSource source = EasyMock.createMock(AddressSource.class);
      EasyMock.expect(source.getAddresses()).andReturn(addresses).anyTimes();
      EasyMock.replay(source);
      // Create node
      Node node = createNode();
      node.setAddressSource(source);
      // Create two repeater handlers
      node.addHandler(new MessageRepeaterHandler());
      node.addHandler(new MessageRepeaterHandler());
      // Create a mock to make sure all handlers are invoked
      MessageHandler signalHandler = EasyMock.createMock(MessageHandler.class);
      signalHandler.onJoin((Connection) EasyMock.anyObject());
      signalHandler.onMessage( // Whether this is once or twice depends on timing, so we only need at least once to make sure it is invoked
               (Connection) EasyMock.anyObject(),(Message) EasyMock.anyObject());
      EasyMock.expectLastCall().anyTimes();
      signalHandler.onLeave((Connection) EasyMock.anyObject()); 
      EasyMock.expectLastCall().anyTimes(); // This depends on timing whether it is actually invoked from cleanup()
      EasyMock.replay(signalHandler);
      node.addHandler(signalHandler);
      // Start node
      node.start();
      // Accept the connection from node
      dummyNode.accept();
      // Send two messages to node
      dummyNode.send(new AlertMessage(Message.MAGIC_TEST,"Message1"));
      dummyNode.send(new AlertMessage(Message.MAGIC_TEST,"Message2"));
      // Check that easy is only replied once by the node, meaning that only one repeater handler
      // is allowed to answer.
      AlertMessage incoming = (AlertMessage) dummyNode.read();
      Assert.assertEquals(incoming.getMessage(),"Message1");
      incoming = (AlertMessage) dummyNode.read();
      Assert.assertEquals(incoming.getMessage(),"Message1");
      incoming = (AlertMessage) dummyNode.read();
      Assert.assertEquals(incoming.getMessage(),"Message2");
      incoming = (AlertMessage) dummyNode.read();
      Assert.assertEquals(incoming.getMessage(),"Message2");
      // Check that the last handler is invoked even if some other handler before it already replied
      EasyMock.verify(signalHandler);
      EasyMock.reset(signalHandler);
   }

   public void testInitialMessage()
      throws IOException
   {
      DummyNode dummyNode = createDummyNode();
      // Create bootstrapper
      List<InetSocketAddress> addresses = new ArrayList<>();
      addresses.add(dummyNode.getAddress());
      AddressSource source = EasyMock.createMock(AddressSource.class);
      EasyMock.expect(source.getAddresses()).andReturn(addresses).anyTimes();
      EasyMock.replay(source);
      // Create node
      Node node = createNode();
      node.setAddressSource(source);
      // Create a repeater handler
      node.addHandler(new MessageRepeaterHandler()
      {

         @Override
         public void onJoin(Connection conn)
         {
            try
            {
               conn.send(new VerackMessage(Message.MAGIC_TEST));
            } catch (IOException ex)
            {
            }
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
      List<InetSocketAddress> addresses = new ArrayList<>();
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
         @Override
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

   public class JoinWaiterHandler implements MessageHandler
   {
      private boolean joined = false;
      private final Object joinWaiter = new Object();

      @Override
      public void onJoin(Connection conn)
      {
         synchronized ( joinWaiter )
         {
            joined = true;
            joinWaiter.notifyAll();
         }
      }

      @Override
      public void onLeave(Connection conn)
      {
      }

      public void waitForJoin()
         throws InterruptedException
      {
         synchronized ( joinWaiter )
         {
            while ( ! joined )
               joinWaiter.wait(1000);
         }
      }

      @Override
      public void onMessage(Connection conn, Message message)
      {
      }
   }

   public class MessageRepeaterHandler implements MessageHandler
   {
      @Override
      public void onJoin(Connection conn)
      {
      }

      @Override
      public void onLeave(Connection conn)
      {
      }

      @Override
      public void onMessage(Connection conn, Message message)
      {
         conn.send(message);
      }
   }
   
   public class VersionMessageSenderHandler implements MessageHandler
   {
      private VersionMessage versionMessage;

      public VersionMessageSenderHandler(VersionMessage versionMessage)
      {
         this.versionMessage = versionMessage;
      }

      @Override
      public void onJoin(Connection conn)
      {
      }

      @Override
      public void onLeave(Connection conn)
      {
      }

      @Override
      public void onMessage(Connection conn, Message message)
      {
         conn.send(versionMessage);
      }
   }
}


