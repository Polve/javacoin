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

package hu.netmind.bitcoin.block;

import hu.netmind.bitcoin.Block;
import hu.netmind.bitcoin.Block;
import hu.netmind.bitcoin.Transaction;
import hu.netmind.bitcoin.TransactionInput;
import hu.netmind.bitcoin.TransactionOutput;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.math.BigInteger;

/**
 * A primitive implementation of a storage which stores all blocks
 * in memory and executes each operation sequentially (search, etc.).
 * @author Robert Brautigam
 */
public class DummyStorage implements BlockChainLinkStorage
{
   private static Logger logger = LoggerFactory.getLogger(DummyStorage.class);

   private List<BlockChainLink> links = new ArrayList<BlockChainLink>();
   private List<BlockChainLink> newLinks = new ArrayList<BlockChainLink>();
   private Map<BigInteger,BlockChainLink> linksMap = new HashMap<BigInteger,BlockChainLink>();

   public DummyStorage(Block genesisBlock)
   {
      List<Block> blocks = new ArrayList<Block>();
      blocks.add(genesisBlock);
      init(blocks,0);
   }

   public DummyStorage()
   {
   }

   public DummyStorage(List<Block> blocks)
   {
      init(blocks,0);
   }

   public DummyStorage(List<Block> blocks, long blockOffset)
   {
      init(blocks,blockOffset);
   }

   private void init(List<Block> blocks, long blockOffset)
   {
      for ( Block block : blocks )
      {
         BlockChainLink previousLink = getLink(block.getPreviousBlockHash());
         if ( previousLink == null )
         {
            if ( links.isEmpty() )
               addLinkInternal(new BlockChainLink(block,
                        new Difficulty(new DifficultyTarget(block.getCompressedTarget())),blockOffset,false));
            else
               addLinkInternal(new BlockChainLink(block,
                        new Difficulty(new DifficultyTarget(block.getCompressedTarget())),blockOffset,true));
         } else {
            addLinkInternal(new BlockChainLink(block,
                     previousLink.getTotalDifficulty().add(new Difficulty(
                           new DifficultyTarget(block.getCompressedTarget()))),
                     previousLink.getHeight()+1,false));
         }
      }
   }

   public BlockChainLink getGenesisLink()
   {
      logger.debug("getting genesis link...");
      if ( links.isEmpty() )
         return null;
      return links.get(0);
   }

   public BlockChainLink getLastLink()
   {
      logger.debug("getting last link...");
      BlockChainLink lastLink = null;
      for ( BlockChainLink link : links )
         if ( (lastLink==null) || 
               (lastLink.getTotalDifficulty().compareTo(link.getTotalDifficulty())<0) )
            lastLink = link;
      return lastLink;
   }

   public BlockChainLink getLink(byte[] hash)
   {
      return linksMap.get(new BigInteger(1,hash));
   }

   public List<BlockChainLink> getNextLinks(byte[] hash)
   {
      logger.debug("getting next links for hash: {}",Arrays.toString(hash));
      List<BlockChainLink> nextLinks = new ArrayList<BlockChainLink>();
      for ( BlockChainLink link : links )
         if ( Arrays.equals(link.getBlock().getPreviousBlockHash(),hash) )
            nextLinks.add(link);
      return nextLinks;
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

   private void addLinkInternal(BlockChainLink link)
   {
      links.add(link);
      linksMap.put(new BigInteger(1,link.getBlock().getHash()),link);
   }

   public void addLink(BlockChainLink link)
   {
      addLinkInternal(link);
      newLinks.add(link);
   }

   public List<BlockChainLink> getNewLinks()
   {
      return newLinks;
   }
}
