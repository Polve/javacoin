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

import hu.netmind.bitcoin.node.p2p.source.FallbackNodesSource;
import java.net.SocketAddress;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.ArrayList;
import java.io.IOException;
import java.io.ByteArrayOutputStream;

/**
 * Execute a getdata with the given parameters.
 * @author Robert Brautigam
 */
public class getdata
{
   public static void main(String[] argv)
      throws Exception
   {
      // Get parameters
      if ( argv.length < 2 ) 
      {
         System.out.println("USAGE: getdata <type> <hash numbers>*");
         return;
      }
      final int type = Integer.parseInt(argv[0]);
      StringBuilder builder = new StringBuilder(argv[1]);
      for ( int i=2; i<argv.length; i++ )
         builder.append(" "+argv[i]);
      final byte[] hash = HexUtil.toByteArray(builder.toString());
      System.out.println("BitCoin getdata request...");
      // Initialize node
      final Node node = new Node();
      node.setMinConnections(1);
      node.setMaxConnections(1);
      node.setAddressSource(new FallbackNodesSource());
      node.addHandler(new MessageHandler() {
               public Message onJoin(Connection conn)
               {
                  System.out.println("Connected to "+conn.getRemoteAddress()+" (from: "+conn.getLocalAddress()+")");
                  // Send our version information
                  VersionMessage version = new VersionMessage(Message.MAGIC_MAIN,32100,1,System.currentTimeMillis()/1000,
                     new NodeAddress(1,(InetSocketAddress) conn.getRemoteAddress()),
                     new NodeAddress(1,new InetSocketAddress(((InetSocketAddress)conn.getLocalAddress()).getAddress(),node.getPort())),
                     123,"",0);
                  System.out.println("Sending handshake version: "+version);
                  return version;
               }

               public void onLeave(Connection conn)
               {
                  System.out.println("Disconnected from "+conn.getRemoteAddress()+" (on local: "+conn.getLocalAddress()+")");
               }

               public Message onMessage(Connection conn, Message message)
                  throws IOException
               {
                  System.out.println("Incoming ("+conn.getRemoteAddress()+"): "+message);
                  if ( message instanceof VersionMessage )
                  {
                     // Let's answer version, so we get more messages
                     VerackMessage verack = new VerackMessage(Message.MAGIC_MAIN);
                     System.out.println("Answering: "+verack);
                     return verack;
                  }
                  if ( message instanceof VerackMessage )
                  {
                     // Send our request
                     InventoryItem item = new InventoryItem(type,hash);
                     List<InventoryItem> items = new ArrayList<InventoryItem>();
                     items.add(item);
                     GetDataMessage getdataMessage =  new GetDataMessage(Message.MAGIC_MAIN,items);
                     System.out.println("Sending a request to get data: "+getdataMessage);
                     return getdataMessage;
                  }
                  if ( (message instanceof TxMessage) || (message instanceof BlockMessage) )
                  {
                     System.out.println("Answer received, exiting.");
                     // Serialize it and display
                     ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
                     BitCoinOutputStream output = new BitCoinOutputStream(byteOutput);
                     MessageMarshaller marshaller = new MessageMarshaller();
                     marshaller.write(message,output);
                     System.out.println("Got data:\n"+HexUtil.toHexString(byteOutput.toByteArray()));
                     // We got our answer, stop node
                     conn.close();
                  }
                  return null;
               }
            });
      // Start node, then wait indefinitly
      node.start();
      // Wait indefinitly
      Object waitObject = new Object();
      synchronized ( waitObject )
      {
         waitObject.wait();
      }
   }
}

