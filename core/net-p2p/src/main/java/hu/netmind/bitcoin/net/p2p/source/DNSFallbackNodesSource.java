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
package hu.netmind.bitcoin.net.p2p.source;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.StringTokenizer;

/**
 * This address source delivers the DNS based fallback nodes as listed in the
 * official client.
 *
 * @author Robert Brautigam
 */
public class DNSFallbackNodesSource extends RandomizedNodesSource
{

   public DNSFallbackNodesSource()
   {
      super();
   }

   public DNSFallbackNodesSource(int defaultPort)
   {
      super(defaultPort);
   }

   @Override
   public List<InetSocketAddress> getInitialAddresses()
   {
      List<InetSocketAddress> addresses = new LinkedList<>();
      // Read addresses from properties file
      String addressesString = ResourceBundle.getBundle("fallback-nodes").getString("seed.names");
      StringTokenizer tokens = new StringTokenizer(addressesString, ",");
      while (tokens.hasMoreTokens())
      {
         String token = tokens.nextToken().trim();
         try
         {
            // InetAddress[] allByName = InetAddress.getAllByName(token);
            // for (InetAddress address : allByName)
            //   addresses.add(new InetSocketAddress(address, defaultPort));
            addresses.add(new InetSocketAddress(InetAddress.getByName(token), defaultPort));
         } catch (UnknownHostException e)
         {
            logger.warn("could not parse to address: " + token, e);
         }
      }
      // Randomize
      logger.debug("dns fallback hosts read");
      return addresses;
   }
}
