/**
 * Copyright (C) 2012 NetMind Consulting Bt.
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

package hu.netmind.bitcoin.block;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedList;
import java.util.Arrays;
import java.util.ArrayList;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import hu.netmind.bitcoin.TransactionInput;
import hu.netmind.bitcoin.Transaction;
import hu.netmind.bitcoin.Block;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This storage simply keeps a map of links and serializes this map to
 * and from disk. The algorithms employed for the query methods are 
 * non-optimized linear searches.
 * @author Robert Brautigam
 */
public class SimpleSerializingStorage implements BlockChainLinkStorage
{
   private static Logger logger = LoggerFactory.getLogger(SimpleSerializingStorage.class);
   private Map<List<Byte>, BlockChainLink> links = new HashMap<List<Byte>, BlockChainLink>();
   private String dbFileName = null;

   public void init(String dbFileName)
   {
      try
      {
         this.dbFileName=dbFileName;
         if ( new File(dbFileName).isFile() )
         {
            // File is there, so read it
            ObjectInputStream objectInput = new ObjectInputStream(new FileInputStream(dbFileName));
            links = (Map<List<Byte>,BlockChainLink>) objectInput.readObject();
            objectInput.close();
            logger.debug("initialized storage with "+links.size()+" links from disk: "+links);
         }
      } catch ( Exception e ) {
         throw new RuntimeException("error while reading serialized db",e);
      }
   }

   public void close()
   {
      try
      {
         ObjectOutputStream objectOutput = new ObjectOutputStream(new FileOutputStream(dbFileName));
         objectOutput.writeObject(links);
         objectOutput.close();
         logger.debug("written storage with "+links.size()+" links to disk: "+links);
      } catch ( Exception e ) {
         throw new RuntimeException("error while writing serialized db",e);
      }
   }

   public BlockChainLink getGenesisLink()
   {
      for ( BlockChainLink link : links.values() )
         if ( (link.getHeight()==0) && (!link.isOrphan()) )
            return link;
      return null;
   }

   public BlockChainLink getLastLink()
   {
      BlockChainLink last = null;
      for ( BlockChainLink link : links.values() )
         if ( (last==null) || (link.getTotalDifficulty().compareTo(last.getTotalDifficulty())>0) )
            last = link;
      return last;
   }

   public BlockChainLink getLink(byte[] hash)
   {
      return links.get(toByteList(hash));
   }

   public List<BlockChainLink> getNextLinks(byte[] hash)
   {
      List<BlockChainLink> nexts = new LinkedList<BlockChainLink>();
      for ( BlockChainLink link : links.values() )
         if ( Arrays.equals(link.getBlock().getPreviousBlockHash(),hash) )
            nexts.add(link);
      return nexts;
   }

   public BlockChainLink getClaimedLink(BlockChainLink link, TransactionInput in)
   {
      while ( link != null )
      {
         for ( Transaction tx : link.getBlock().getTransactions() )
            if ( Arrays.equals(tx.getHash(),in.getClaimedTransactionHash()) )
               return link;
         link = getLink(link.getBlock().getPreviousBlockHash());
      }
      return null;
   }

   public BlockChainLink getClaimerLink(BlockChainLink link, TransactionInput in)
   {
      while ( link != null )
      {
         for ( Transaction tx : link.getBlock().getTransactions() )
            for ( TransactionInput txIn : tx.getInputs() )
               if ( (Arrays.equals(txIn.getClaimedTransactionHash(),in.getClaimedTransactionHash())) &&
                     (txIn.getClaimedOutputIndex()==in.getClaimedOutputIndex()) )
                  return link;
         link = getLink(link.getBlock().getPreviousBlockHash());
      }
      return null;
   }

   public void addLink(BlockChainLink link)
   {
      links.put(toByteList(link.getBlock().getHash()),link);
   }

   public void updateLink(BlockChainLink link)
   {
      addLink(link);
   }

   private List<Byte> toByteList(byte[] hash)
   {
      List<Byte> byteList = new ArrayList<Byte>(hash.length);
      for ( byte v : hash )
         byteList.add(v);
      return byteList;
   }
}

