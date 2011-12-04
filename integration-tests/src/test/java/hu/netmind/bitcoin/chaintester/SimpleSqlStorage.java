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

package hu.netmind.bitcoin.chaintester;

import hu.netmind.bitcoin.block.BlockChainLinkStorage;
import hu.netmind.bitcoin.block.BlockChainLink;
import hu.netmind.bitcoin.TransactionInput;
import hu.netmind.bitcoin.BitCoinException;
import java.util.List;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.SQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A storage implemented as simple as possible to hold links.
 * @author Robert Brautigam
 */
public class SimpleSqlStorage implements BlockChainLinkStorage
{
   private static Logger logger = LoggerFactory.getLogger(SimpleSqlStorage.class);
   private Connection connection = null;

   public SimpleSqlStorage(String path)
   {
      try
      {
         // Initialize the sql storage to the path given
         Connection connection = DriverManager.getConnection("jdbc:hsqldb:file:"+path+"/bitcoin", "SA", "");
         logger.debug("get connection to database: "+connection.getMetaData().getDatabaseProductName());
         // Create the schema if not already present
         ResultSet tables = connection.getMetaData().getTables(null,null,"LINK",new String[] {"TABLE"});
         if ( ! tables.next() )
         {
            logger.debug("schema doesn't exists yet in database, creating...");
            logger.debug("creating txout...");
            connection.prepareStatement(
                  "create table txout ( txhash binary(32) not null, value bigint not null, script varbinary(1024), "+
                  "primary key ( txhash ))").executeUpdate();
            logger.debug("creating txin...");
            connection.prepareStatement(
                  "create table txin ( txhash binary(32) not null, claimedhash binary(32), "+
                  "claimedindex integer, script varbinary(1024), sequence bigint, "+
                  "primary key ( txhash ))").executeUpdate();
            logger.debug("creating tx...");
            connection.prepareStatement(
                  "create table tx ( hash binary(32) not null, sequence bigint, "+
                  "primary key ( hash ))").executeUpdate();
            logger.debug("creating link...");
            connection.prepareStatement(
                  "create table link ( hash binary(32) not null, creationtime bigint, "+
                  "nonce bigint, compressedtarget bigint, previoushash binary(32), "+
                  "merkleroot binary(32), primary key ( hash ))").executeUpdate();
            connection.commit();
            logger.debug("changed commited.");
         }
      } catch ( SQLException e ) {
         throw new RuntimeException("error while initializing database for chain links",e);
      }
   }

   public void addLink(BlockChainLink link)
   {
   }

   public BlockChainLink getGenesisLink()
   {
      return null;
   }

   public BlockChainLink getLastLink()
   {
      return null;
   }

   public BlockChainLink getLink(byte[] hash)
   {
      return null;
   }

   public List<BlockChainLink> getNextLinks(byte[] hash)
   {
      return null;
   }

   public BlockChainLink getClaimedLink(BlockChainLink link, TransactionInput in)
   {
      return null;
   }

   public BlockChainLink getClaimerLink(BlockChainLink link, TransactionInput in)
   {
      return null;
   }
}

