/**
 * Copyright (C) 2011 NetMind Consulting Bt.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option) any
 * later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package it.nibbles.javacoin.net.p2p.source;

import it.nibbles.javacoin.net.p2p.AddressSource;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This node source takes a prepared list of addresses
 *
 * @author Alessandro Polverini
 */
public class NodeSource implements AddressSource
{

   protected Logger logger = LoggerFactory.getLogger(this.getClass());
   protected int defaultPort = 8333;
   private List<InetSocketAddress> addresses = new ArrayList<>();

   public NodeSource(List<String> peers, int defaultPort)
   {
      this.defaultPort = defaultPort;
      addPeers(peers, defaultPort);
   }

   public NodeSource(List<String> peers)
   {
      addPeers(peers, defaultPort);
   }

   public final void addPeers(List<String> peers, int defaultPort)
   {
      for (String peer : peers)
      {
         String[] s = peer.split(":");
         if (s.length > 2)
         {
            logger.info("Bad peer format: " + peer);
            continue;
         }
         int port = defaultPort;
         if (s.length == 2)
            port = Integer.parseInt(s[1]);
         addresses.add(new InetSocketAddress(s[0], port));
      }
   }

   @Override
   public List<InetSocketAddress> getAddresses()
   {
      return Collections.unmodifiableList(addresses);
   }

   @Override
   public String toString()
   {
      List<InetSocketAddress> list = getAddresses();
      if (list == null)
         return "AddressSource[null]";
      StringBuilder sb = new StringBuilder("AddressSource[");
      for (InetSocketAddress addr : list)
         sb.append(addr.toString()).append(" ");
      return sb.append("]").toString();
   }
}
