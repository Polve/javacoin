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

package hu.netmind.bitcoin.net.p2p;

import hu.netmind.bitcoin.net.p2p.source.DNSFallbackNodesSource;
import hu.netmind.bitcoin.net.VerackMessage;
import hu.netmind.bitcoin.net.VersionMessage;
import hu.netmind.bitcoin.net.NodeAddress;
import hu.netmind.bitcoin.net.Message;
import java.io.IOException;
import java.net.SocketAddress;
import java.net.InetSocketAddress;

/**
 * Tap the bitcoin network and display all messages on the network.
 * @author Robert Brautigam
 */
public class networkTap
{
   public static void main(String[] argv)
      throws Exception
   {
      System.out.println("BitCoin Network Tapper... Press CTRL-C to exit.");
      // Initialize node
      final Node node = new Node();
      node.setMinConnections(1);
      node.setMaxConnections(1);
      node.setAddressSource(new DNSFallbackNodesSource());
      node.addHandler(new MessageHandler() {
               public void onJoin(Connection conn) throws IOException
               {
                  System.out.println("Connected to "+conn.getRemoteAddress()+" (from: "+conn.getLocalAddress()+")");
                  // Send our version information
                  VersionMessage version = new VersionMessage(Message.MAGIC_MAIN,32100,1,System.currentTimeMillis()/1000,
                     new NodeAddress(1,(InetSocketAddress) conn.getRemoteAddress()),
                     new NodeAddress(1,new InetSocketAddress(((InetSocketAddress)conn.getLocalAddress()).getAddress(),node.getPort())),
                     123,"",0);
                  System.out.println("Sending handshake version: "+version);
                  conn.send(version);
               }

               public void onLeave(Connection conn)
               {
                  System.out.println("Disconnected from "+conn.getRemoteAddress()+" (on local: "+conn.getLocalAddress()+")");
               }

               public void onMessage(Connection conn, Message message) throws IOException
               {
                  System.out.println("Incoming ("+conn.getRemoteAddress()+"): "+message);
                  if ( message instanceof VersionMessage )
                  {
                     // Let's answer version, so we get more messages
                     VerackMessage verack = new VerackMessage(Message.MAGIC_MAIN);
                     System.out.println("Answering: "+verack);
                     conn.send(verack);
                  }
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

