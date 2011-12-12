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

package hu.netmind.bitcoin.node.p2p.source;

import hu.netmind.bitcoin.node.p2p.AddressSource;
import java.util.List;
import java.util.LinkedList;
import java.util.Enumeration;
import java.util.ResourceBundle;
import java.util.Collections;
import java.util.StringTokenizer;
import java.net.InetSocketAddress;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.math.BigInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This address source delivers the static fallback nodes listed on the
 * official wiki. Note that this may be out of date.
 * @author Robert Brautigam
 */
public class FallbackNodesSource implements AddressSource
{
   private static final int DEFAULT_PORT = 8333;
   private static Logger logger = LoggerFactory.getLogger(FallbackNodesSource.class);

   private List<InetSocketAddress> addresses;

   public FallbackNodesSource()
   {
      addresses = new LinkedList<InetSocketAddress>();
      // Read addresses from properties file
      String addressesString = ResourceBundle.getBundle("fallback-nodes").getString("seed.addresses");
      StringTokenizer tokens = new StringTokenizer(addressesString,",");
      while ( tokens.hasMoreTokens() )
      {
         String token = tokens.nextToken().trim().substring(2); // In the form of 0x1ddb1032
         byte[] tokenBytes = new BigInteger(token,16).toByteArray();
         try
         {
            addresses.add(new InetSocketAddress(InetAddress.getByAddress(tokenBytes),DEFAULT_PORT));
         } catch ( UnknownHostException e ) {
            logger.warn("could not parse to address: "+token,e);
         }
      }
      // Randomize
      Collections.shuffle(addresses);
      logger.debug("fallback hosts read");
   }

   public List<InetSocketAddress> getAddresses()
   {
      return Collections.unmodifiableList(addresses);
   }
}

