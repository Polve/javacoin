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
import java.io.BufferedInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ResourceBundle;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

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
   private static int defaultInitialTimeout = 1800000; // 30 mins
   private static int defaultTimeout = 30000; // 30 seconds

   private int port = defaultPort;
   private int soTimeout = defaultSoTimeout;
   private int maxConnections = defaultMaxConnections;
   private int minConnections = defaultMinConnections;
   private int connectTimeout = defaultConnectTimeout;
   private int initialTimeout = defaultInitialTimeout;
   private int timeout = defaultTimeout;

   private boolean running = false;
   
   private AddressSource addressSource;
   private List<MessageHandler> handlers = new ArrayList<MessageHandler>();

   private List<NodeWorker> workers = new ArrayList<NodeWorker>();
   private NodeListener nodeListener;
   
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
      throws IOException
   {
      if ( running )
         return;
      running=true;
      // Start listening
      nodeListener = new NodeListener();
      nodeListener.start();
   }

   /**
    * Create initial connections to some nodes to join the network.
    */
   private void bootstrapWorkers()
   {
      logger.debug("bootstrapping workers...");
      if ( addressSource == null )
      {
         logger.warn("no address source setup for node, no nodes will be connected");
         return; // No address source
      }
      List<InetSocketAddress> addresses = addressSource.getAddresses();
      for ( InetSocketAddress address : addresses )
      {
         // If enough nodes are there, stop
         synchronized ( workers )
         {
            if ( workers.size() >= minConnections )
               return;
         }
         // Connect. Node this is outside the synchronization,
         // so there is a possibility that we end up with more
         // workers than minimum.
         try
         {
            Socket socket = new Socket();
            socket.connect(address,connectTimeout);
            if ( ! addWorker(socket) )
               socket.close();
            else
               logger.debug("worker added for address {}, current number of workers {}",address,workers.size());
         } catch ( IOException e ) {
            logger.error("error connecting to address: {}",address);
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
            worker.start();
            return true;
         } else {
            logger.debug("not creating worker because maximum number of connections reached ({})",maxConnections);
         }
         return false;
      }                  
   }

   /**
    * Remove workers which already stopped. Workers can't remove themselves, because
    * then it's not possible to implement a synchronous shutdown, we want to make sure
    * the thread of the node worker has really ended.
    */
   private void cleanupWorkers()
   {
      logger.debug("running cleanup of stopped workers...");
      synchronized ( workers )
      {
         Iterator<NodeWorker> workerIterator = workers.iterator();
         while ( workerIterator.hasNext() )
         {
            long currentTime = System.currentTimeMillis();
            NodeWorker worker = workerIterator.next();
            logger.debug("running on worker {}, created {}, last message received on {}, current time {}",
                  new Object[] { worker.getAddress(), worker.creationTime, worker.lastIncomingTime, currentTime });
            if ( 
                  // If it's not running anymore
                  (! worker.isRunning()) ||
                  // OR if there was no message yet, and the "initial timeout" ran out
                  ((worker.lastIncomingTime==0) && (currentTime-worker.creationTime>initialTimeout)) ||
                  // OR there was a message, but it was longer than "timeout" ago
                  ((worker.lastIncomingTime>0) && (currentTime-worker.lastIncomingTime>timeout))
               )
            {
               // Remove
               workerIterator.remove();
               worker.stop();
            }
         }
      }
   }

   /**
    * Stop listening to messages, close all connections with other nodes. This
    * method is synchronous, it returns after the node is completely closed.
    */
   public void stop()
   {
      logger.debug("stop called on node...");
      running = false; // Non-invasive way to stop, but it won't be immediate
      // Stop also all worker nodes
      synchronized ( workers )
      {
         for ( NodeWorker worker : workers )
            worker.stop();
      }
      // Interrupt thread
      if ( nodeListener != null )
         nodeListener.stop();
      logger.info("node stopped");
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
      private Thread thread;
      private ServerSocket serverSocket;

      /**
       * Start node listener and return only when node started to listen and connections
       * will be accepted.
       */
      public void start()
         throws IOException
      {
         // Establish server socket
         serverSocket = new ServerSocket(port);
         serverSocket.setSoTimeout(soTimeout);
         // Start thread
         thread = new Thread(this,"BitCoin Node Listener");
         thread.setDaemon(true);
         thread.start();
      }

      /**
       * Stop the node listener, and only return if listener is really stopped.
       */
      public void stop()
      {
         try
         {
            if ( (serverSocket!=null) && (!serverSocket.isClosed()) )
               serverSocket.close();
         } catch ( IOException e ) {
            logger.error("error closing server socket",e);
         }
         try
         {
            thread.join();
         } catch ( InterruptedException e ) {
            logger.error("interrupted while waiting for node to stop, node might not be stopped",e);
         }
      }

      public void run()
      {
         logger.info("starting node listener on port: "+port);
         try
         {
            // Wait for new connections
            while ( running )
            {
               // First, try to make sure we have enough nodes connected
               bootstrapWorkers();
               // Accept a new socket from outside
               Socket socket = null;
               try
               {
                  socket = serverSocket.accept();
               } catch ( SocketTimeoutException e ) {
                  // Normal for a socket to time out do nothing!
               }
               // Do some cleanup, remove obsolate workers
               cleanupWorkers();
               // If socket is available, try to connect
               try
               {
                  if ( (socket!=null) && ((!running) || (! addWorker(socket))) )
                     socket.close();
               } catch ( IOException e ) {
                  logger.error("could not add worker for socket {}",socket);
                  try
                  {
                     if ( socket != null )
                        socket.close();
                  } catch ( IOException ioe ) {
                     logger.error("could not close socket {}",socket,ioe);
                  }
               }
            }
         } catch ( Exception e ) {
            logger.error("node listener exiting because of an exception",e);
         } finally {
            running = false;
            // Close server
            try
            {
               if ( serverSocket != null )
                  serverSocket.close();
            } catch ( IOException e ) {
               logger.error("error while closing server socket",e);
            }
         }
         logger.info("node listener stopped for port: {}",port);
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
      private Thread workerThread;
      private MessageMarshaller marshaller = new MessageMarshaller();
      private Connection connection;
      private long lastIncomingTime = 0;
      private long creationTime = 0;

      private NodeWorker(Socket socket)
         throws IOException
      {
         input = new BitCoinInputStream(new BufferedInputStream(socket.getInputStream()));
         output = new BitCoinOutputStream(socket.getOutputStream());
         this.socket=socket;
         this.running = true;
         connection = new NodeWorkerConnection();
         creationTime = System.currentTimeMillis();
      }

      public void start()
      {
         // Start worker
         workerThread = new Thread(this,"BitCoin Node Connection");
         workerThread.setDaemon(true);
         workerThread.start();
         // Invoke listeners
         for ( MessageHandler handler : handlers )
         {
            try
            {
               Message message = handler.onJoin(connection);
               if ( message != null )
                  send(message);
            } catch ( IOException e ) {
               logger.error("failed to handle join by handler {}",handler);
            }
         }
      }

      public boolean isRunning()
      {
         return running;
      }

      private void stopInternal()
      {
         // Stop running
         running = false;
         // Close socket
         try
         {
            if ( ! socket.isClosed() )
               socket.close();
         } catch ( IOException e ) {
            logger.error("error closing socket {}",socket,e);
         }
         // Invoke listeners
         for ( MessageHandler handler : handlers )
         {
            try
            {
               handler.onLeave(connection);
            } catch ( IOException e ) {
               logger.error("handler "+handler+" could not execute onLeave()",e);
            }
         }
      }

      public void stop()
      {
         stopInternal();
         logger.debug("stopped node worker.");
         // Wait for thread to stop
         try
         {
            workerThread.join();
         } catch ( InterruptedException e ) {
            logger.error("error while waiting for worker to stop, worker may not be completely stopped",e);
         }
      }

      private SocketAddress getAddress()
      {
         return socket.getRemoteSocketAddress();
      }

      public synchronized void send(Message message)
         throws IOException
      {
         logger.debug("sending message {}, to socket {}",message,socket);
         if ( running )
         {
            marshaller.write(message,output);
            logger.debug("message sent");
         } else {
            logger.debug("not sent, not running");
         }
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
               logger.debug("received message {}, from socket {}",message,socket);
               lastIncomingTime = System.currentTimeMillis();
               // Pass to handlers
               replied = false;
               for ( MessageHandler handler : handlers )
               {
                  Message reply = null;
                  try
                  {
                     reply = handler.onMessage(connection,message);
                  } catch ( IOException e ) {
                     logger.error("handler "+handler+" failed to handle message",e);
                  }
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
            stopInternal(); // Stop worker properly
         }
      }

      /**
       * This inner class of the NodeWorker will be passed to handlers as a shortcut to
       * NodeWorker values and functionality.
       */
      public class NodeWorkerConnection implements Connection
      {
         private Map session = new HashMap();

         public Map getSession()
         {
            return session;
         }

         public SocketAddress getRemoteAddress()
         {
            return socket.getRemoteSocketAddress();
         }

         public SocketAddress getLocalAddress()
         {
            return socket.getLocalSocketAddress();
         }

         public long getVersion()
         {
            return marshaller.getVersion();
         }

         public void setVersion(long version)
         {
            marshaller.setVersion(version);
         }

         public void close()
         {
            stop();
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

   public int getMinConnections()
   {
      return minConnections;
   }
   public void setMinConnections(int minConnections)
   {
      this.minConnections=minConnections;
   }

   public int getConnectTimeout()
   {
      return connectTimeout;
   }
   public void setConnectTimeout(int connectTimeout)
   {
      this.connectTimeout=connectTimeout;
   }

   public int getInitialTimeout()
   {
      return initialTimeout;
   }
   public void setInitialTimeout(int initialTimeout)
   {
      this.initialTimeout=initialTimeout;
   }

   public int getTimeout()
   {
      return timeout;
   }
   public void setTimeout(int timeout)
   {
      this.timeout=timeout;
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
         defaultInitialTimeout = Integer.parseInt(bundle.getString("node.initial_timeout"));
         defaultTimeout = Integer.parseInt(bundle.getString("node.timeout"));
      } catch ( Exception e ) {
         logger.error("can not read default configuration for node, will go with hardcoded values",e);
      }
   }
}

