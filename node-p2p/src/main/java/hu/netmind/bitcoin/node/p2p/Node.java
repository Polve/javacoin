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
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package hu.netmind.bitcoin.node.p2p;

import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ResourceBundle;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.ArrayList;

/**
 * A network node which keeps the communication to other nodes in the p2p
 * network. When the node is started it will wait for incoming messages
 * from all established connections (if any), and forward all messages
 * to message handlers. Message handlers should implement not only the 
 * main logic of bitcoin, but also protocol related housekeeping.
 * @author Robert Brautigam
 */
public class Node
{
   private static final Logger logger = LoggerFactory.getLogger(Node.class);

   private static int defaultSoTimeout = 30000; // 30 secs
   private static int defaultPort = 8333;
   private static int defaultMaxConnections = 5;
   private static int defaultMinConnections = 3;
   private static int defaultConnectTimeout = 10000; // 10 secs

   private int port = defaultPort;
   private int soTimeout = defaultSoTimeout;
   private int maxConnections = defaultMaxConnections;
   private int minConnections = defaultMinConnections;
   private int connectTimeout = defaultConnectTimeout;
   private boolean running = false;
   
   private MessageMarshaller marshaller = new MessageMarshaller();
   private AddressSource addressSource;
   private List<MessageHandler> handlers = new ArrayList<MessageHandler>();

   private List<NodeWorker> workers = new ArrayList<NodeWorker>();
   
   /**
    * Create this node which will then listen for incoming
    * connections from other nodes. Note that node will not work until it's started.
    * @param port The port to listen on instead of the default port.
    */
   public Node(int port)
   {
      this.port=port;
   }

   /**
    * Create this node which will then listen for incoming
    * connections from other nodes. Note that node will not work until it's started.
    */
   public Node()
   {
   }

   /**
    * Start to establish connections and listen to incoming messages.
    */
   public void start()
   {
      if ( running )
         return;
      running=true;
      // Start listening
      Thread thread = new Thread(new NodeListener(),"BitCoin Node Listener");
      thread.setDaemon(true);
      thread.start();
      // Add initial workers to the node
      bootstrapWorkers();
   }

   /**
    * Create initial connections to some nodes to join the network.
    */
   private void bootstrapWorkers()
   {
      logger.debug("bootstrapping workers...");
      List<InetSocketAddress> addresses = addressSource.getAddresses();
      synchronized ( workers )
      {
         for ( InetSocketAddress address : addresses )
         {
            // If enough nodes are there, stop
            if ( workers.size() >= minConnections )
               return;
            // Connect
            try
            {
               Socket socket = new Socket();
               socket.connect(address,connectTimeout);
               addWorker(socket);
               logger.debug("worker added for address {}",address);
            } catch ( IOException e ) {
               logger.error("error connecting to address: {}",address);
            }
         }
      }
   }

   /**
    * Add a worker node. If the maximum allowed worker count is already reached,
    * method will do nothing.
    * @return True if worker is added, false if for some reason worker was not created.
    */
   private boolean addWorker(Socket socket)
      throws IOException
   {
      logger.debug("adding worker for socket: {}",socket);
      synchronized ( workers )
      {
         if ( workers.size() < maxConnections )
         {
            // See whether there is a worker for the same address
            for ( NodeWorker worker : workers )
               if ( worker.getAddress().equals(socket.getRemoteSocketAddress()) )
               {
                  logger.debug("node already connected to address, will not connect again");
                  return false;
               }
            // Establish worker
            logger.debug("setting up worker to: {}",socket);
            NodeWorker worker = new NodeWorker(socket);
            workers.add(worker);
            Thread workerThread = new Thread(worker,"BitCoin Node Connection");
            workerThread.setDaemon(true);
            workerThread.start();
            return true;
         } else {
            logger.debug("not creating worker because maximum number of connections reached ({})",maxConnections);
         }
         return false;
      }                  
   }

   /**
    * Stop listening to messages, close all connections with other nodes.
    */
   public void stop()
   {
      running = false; // Non-invasive way to stop, but it won't be immediate
      // Stop also all worker nodes
      synchronized ( workers )
      {
         for ( NodeWorker worker : workers )
            worker.stop();
      }
   }

   /**
    * Broadcast a message to all nodes this node is in contact with. In case of
    * errors from a node the message will still be tried for other nodes, but
    * there is no guarantee that any node recieved this message.
    */
   public void broadcast(Message message)
   {
      logger.debug("broadcasting message: {}",message);
      synchronized ( workers )
      {
         for ( NodeWorker worker : workers )
         {
            try
            {
               worker.send(message);
            } catch ( IOException e ) {
               logger.error("could not broadcast message to node with socket: {}",worker.getAddress(),e);
            }
         }
      }
   }

   /**
    * Listens for incoming connections and allocates a new worker thread for 
    * all connections up until a limit is reached.
    */
   private class NodeListener implements Runnable
   {
      public void run()
      {
         logger.info("starting node listener on port: "+port);
         ServerSocket serverSocket = null;
         try
         {
            // Establish server socket
            serverSocket = new ServerSocket(port);
            serverSocket.setSoTimeout(soTimeout);
            // Wait for new connections
            while ( running )
            {
               Socket socket = null;
               try
               {
                  socket = serverSocket.accept();
                  // Socket arrived, so setup worker node
                  if ( (!running) || (! addWorker(socket)) )
                     socket.close();
               } catch ( SocketTimeoutException e ) {
                  // Normal for it to time out, this is so that we can
                  // check the exit criteria, and also to check on workers.
                  // If there are no workers, bootstrap again.
                  synchronized ( workers )
                  {
                     if ( workers.size() < minConnections )
                        bootstrapWorkers();
                  }
               } catch ( IOException e ) {
                  logger.error("could not add worker for socket {}",socket);
                  try
                  {
                     socket.close();
                  } catch ( IOException ioe ) {
                     logger.error("could not close socket {}",socket,ioe);
                  }
               }
            }
         } catch ( Exception e ) {
            logger.error("node listener exiting because of an exception",e);
         } finally {
            // Close server
            try
            {
               if ( serverSocket != null )
                  serverSocket.close();
            } catch ( IOException e ) {
               logger.error("error while closing server socket",e);
            }
         }
      }
   }

   /**
    * A worker is responsible for handling a single connection to another node.
    */
   private class NodeWorker implements Runnable
   {
      private Socket socket;
      private BitCoinInputStream input;
      private BitCoinOutputStream output;
      private boolean running;

      private NodeWorker(Socket socket)
         throws IOException
      {
         input = new BitCoinInputStream(socket.getInputStream());
         output = new BitCoinOutputStream(socket.getOutputStream());
         this.socket=socket;
         this.running = true;
      }

      public void stop()
      {
         // Stop running
         running = false;
         // Remove from workers
         synchronized ( workers )
         {
            workers.remove(this);
         }
         // Close socket
         try
         {
            socket.close();
         } catch ( IOException e ) {
            logger.error("error closing socket {}",socket,e);
         }
      }

      private SocketAddress getAddress()
      {
         return socket.getRemoteSocketAddress();
      }

      public synchronized void send(Message message)
         throws IOException
      {
         marshaller.write(message,output);
         logger.debug("sent message {}, to socket {}",message,socket);
      }

      public void run()
      {
         try
         {
            // Wait for arriving messages
            boolean replied = false;
            while ( running )
            {
               // Get message from stream
               Message message = marshaller.read(input);
               // Pass to handlers
               replied = false;
               for ( MessageHandler handler : handlers )
               {
                  logger.debug("received message {}, from socket {}",message,socket);
                  Message reply = handler.handle(message);
                  if ( (!replied) && (reply != null) )
                  {
                     send(reply);
                     replied = true; // Only reply once, but ask all handlers nontheless
                  }
               }
            }
         } catch ( IOException e ) {
            if ( running )
               logger.error("error while node communication with socket {}",socket,e);
            else
               logger.debug("error from reading, but probably calling stop() on worker on socket {}",socket);
         } finally {
            stop(); // Stop worker properly
         }
      }
   }

   public int getPort()
   {
      return port;
   }
   public void setPort(int port)
   {
      this.port=port;
   }

   public int getSoTimeout()
   {
      return soTimeout;
   }
   public void setSoTimeout(int soTimeout)
   {
      this.soTimeout=soTimeout;
   }

   public int getMaxConnections()
   {
      return maxConnections;
   }
   public void setMaxConnections(int maxConnections)
   {
      this.maxConnections=maxConnections;
   }

   public AddressSource getAddressSource()
   {
      return addressSource;
   }
   public void setAddressSource(AddressSource addressSource)
   {
      this.addressSource=addressSource;
   }

   /**
    * Add another message handler for the node. Note: this is only legal before
    * the node is started.
    */
   public void addHandler(MessageHandler handler)
   {
      if ( running )
         throw new IllegalStateException("can not set handlers after the node is already started");
      handlers.add(handler);
   }

   /**
    * Remove another message handler for the node. Note: this is only legal before
    * the node is started.
    */
   public void removeHandler(MessageHandler handler)
   {
      if ( running )
         throw new IllegalStateException("can not remove handlers after the node is already started");
      handlers.remove(handler);
   }

   static
   {
      try
      {
         ResourceBundle bundle = ResourceBundle.getBundle("bitcoin-node");
         defaultSoTimeout = Integer.parseInt(bundle.getString("node.so_timeout"));
         defaultPort = Integer.parseInt(bundle.getString("node.default_port"));
         defaultMaxConnections = Integer.parseInt(bundle.getString("node.max_connections"));
         defaultMinConnections = Integer.parseInt(bundle.getString("node.min_connections"));
         defaultConnectTimeout = Integer.parseInt(bundle.getString("node.connect_timeout"));
      } catch ( Exception e ) {
         logger.error("can not read default configuration for node, will go with hardcoded values",e);
      }
   }
}

