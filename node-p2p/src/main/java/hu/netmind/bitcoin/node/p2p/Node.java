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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ResourceBundle;
import java.net.InetSocketAddress;

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
      Thread thread = new Thead(new NodeListener(),"BitCoin Node Listener");
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
   {
      logger.debug("adding worker for socket: {}",socket);
      synchronized ( workers )
      {
         if ( workers.size() < maxConnections )
         {
            // See whether there is a worker for the same address
            for ( NodeWorker worker : workers )
               if ( worker.getAddress().eqausl(socket.getRemoteSocketAddress()) )
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
         ServerSocket serverSocket;
         try
         {
            // Establish server socket
            serverSocket = new ServerSocket(port);
            socket.setSoTimeout(SO_TIMEOUT);
            // Wait for new connections
            while ( running )
            {
               Socket socket = null;
               try
               {
                  socket = serverSocket.accept();
                  // Socket arrived, so setup worker node
                  if ( ! addWorker(socket) )
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
               }
            }
         } catch ( Exception e ) {
            logger.error("node listener exiting because of an exception",e);
         } finally {
            // Close server
            serverSocket.close();
         }
      }
   }

   /**
    * A worker is responsible for handling a single connection to another node.
    */
   private class NodeWorker
   {
      private Socket socket;

      private NodeWorker(Socket socket)
      {
         this.socket=socket;
      }

      private InetSocketAddress getAddress()
      {
         return socket.getRemoteSocketAddress();
      }

      public void run()
      {
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

   public AddressStorage getAddressStorage()
   {
      return addressStorage;
   }
   public void setAddressStorage(AddressStorage addressStorage)
   {
      this.addressStorage=addressStorage;
   }

   public void addHandler(MessageHandler handler)
   {
      synchronized ( handlers )
      {
         handlers.add(handler);
      }
   }

   public void removeHandler(MessageHandler handler)
   {
      synchronized ( handlers )
      {
         handlers.remove(handler);
      }
   }

   public NodeBootstrapper getBootstrapper()
   {
      return bootstrapper;
   }
   public void setBootstrapper(NodeBootstrapper bootstrapper)
   {
      this.bootstrapper=bootstrapper;
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

