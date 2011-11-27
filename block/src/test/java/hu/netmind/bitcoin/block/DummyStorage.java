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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

   public DummyStorage(Block genesisBlock)
   {
      List<Block> blocks = new ArrayList<Block>();
      blocks.add(genesisBlock);
      init(blocks);
   }

   public DummyStorage()
   {
   }

   public DummyStorage(List<Block> blocks)
   {
      init(blocks);
   }

   private void init(List<Block> blocks)
   {
      for ( Block block : blocks )
      {
         BlockChainLink previousLink = getLink(block.getPreviousBlockHash());
         if ( previousLink == null )
         {
            if ( links.isEmpty() )
               links.add(new BlockChainLink(block,
                        new Difficulty(new DifficultyTarget(block.getCompressedTarget())),1,false));
            else
               links.add(new BlockChainLink(block,
                        new Difficulty(new DifficultyTarget(block.getCompressedTarget())),0,true));
         } else {
            links.add(new BlockChainLink(block,
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
      logger.debug("getting link for hash: {}",Arrays.toString(hash));
      for ( BlockChainLink link : links )
         if ( Arrays.equals(link.getBlock().getHash(),hash) )
            return link;
      return null;
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

   public void addLink(BlockChainLink link)
   {
      links.add(link);
      newLinks.add(link);
   }

   public List<BlockChainLink> getNewLinks()
   {
      return newLinks;
   }
}

