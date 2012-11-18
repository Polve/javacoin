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

import hu.netmind.bitcoin.Transaction;
import hu.netmind.bitcoin.TransactionInput;
import it.nibbles.javacoin.storage.OrphanBlockStorageException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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
   private Map<List<Byte>, BlockChainLink> links = new HashMap<>();
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

  @Override
   public BlockChainLink getGenesisLink()
   {
      for ( BlockChainLink link : links.values() )
         if ( (link.getHeight()==BlockChainLink.ROOT_HEIGHT))
            return link;
      return null;
   }

  @Override
   public BlockChainLink getLastLink()
   {
      BlockChainLink last = null;
      for ( BlockChainLink link : links.values() )
         if ( ((last==null) || (link.getTotalDifficulty().compareTo(last.getTotalDifficulty())>0)) )
            last = link;
      return last;
   }

  @Override
   public int getHeight()
   {
     BlockChainLink lastLink = getLastLink();
     if (lastLink == null)
       return 0;
     else
       return lastLink.getHeight();
   }
   
  @Override
   public BlockChainLink getLink(byte[] hash)
   {
      return links.get(toByteList(hash));
   }

//  @Override
//   public BlockChainLink getNextLink(byte[] current, byte[] target)
//   {
//      BlockChainLink result = getLink(target);
//      while ( (result!=null) && (!Arrays.equals(result.getBlock().getPreviousBlockHash(),current)) )
//         result = getLink(result.getBlock().getPreviousBlockHash());
//      return result;
//   }
//
//  @Override
//   public List<BlockChainLink> getNextLinks(byte[] hash)
//   {
//      List<BlockChainLink> nexts = new LinkedList<>();
//      for ( BlockChainLink link : links.values() )
//         if ( Arrays.equals(link.getBlock().getPreviousBlockHash(),hash) )
//            nexts.add(link);
//      return nexts;
//   }

  @Override
   public BlockChainLink getCommonLink(byte[] first, byte[] second)
   {
      BlockChainLink firstLink = getLink(first);
      BlockChainLink secondLink = getLink(second);
      while ( (firstLink!=null) && (secondLink!=null) )
      {
         if ( Arrays.equals(firstLink.getBlock().getHash(),secondLink.getBlock().getHash()) )
            return firstLink;
         if ( firstLink.getHeight() > secondLink.getHeight() )
            firstLink = getLink(firstLink.getBlock().getPreviousBlockHash());
         else
            secondLink = getLink(secondLink.getBlock().getPreviousBlockHash());
      }
      return null;
   }

  @Override
   public boolean isReachable(byte[] target, byte[] source)
   {
      BlockChainLink link = getCommonLink(target,source);
      return (link!=null) && (Arrays.equals(link.getBlock().getHash(),source));
   }

  @Override
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

  @Override
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

  @Override
  public void addLink(BlockChainLink link) {
    if (getHeight() > 0 && !blockExists(link.getBlock().getPreviousBlockHash()))
      throw new OrphanBlockStorageException("Trying to store an orphan block");
    if (!blockExists(link.getBlock().getHash()))
      links.put(toByteList(link.getBlock().getHash()), link);
  }

   public void updateLink(BlockChainLink link)
   {
      addLink(link);
   }

   private List<Byte> toByteList(byte[] hash)
   {
      List<Byte> byteList = new ArrayList<>(hash.length);
      for ( byte v : hash )
         byteList.add(v);
      return byteList;
   }

  @Override
  public byte[] getHashOfMainChainAtHeight(long height) {
    return getLastLink().getBlock().getHash();
  }

  @Override
  public BlockChainLink getPartialClaimedLink(BlockChainLink link, TransactionInput in) {
    return getClaimedLink(link, in);
  }

  @Override
  public boolean outputClaimedInSameBranch(BlockChainLink link, TransactionInput in) {
    return getClaimerLink(link, in) != null;
  }

  @Override
  public BlockChainLink getLinkAtHeight(long height) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public boolean blockExists(byte[] hash) {
    return getLinkBlockHeader(hash) != null;
  }

  @Override
  public BlockChainLink getLinkBlockHeader(byte[] hash) {
    return getLink(hash);
  }

}
