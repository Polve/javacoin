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

import hu.netmind.bitcoin.BlockChain;
import hu.netmind.bitcoin.Block;
import hu.netmind.bitcoin.VerificationException;
import java.math.BigInteger;
import java.util.Observable;
import java.util.Map;
import java.util.HashMap;
import java.util.ResourceBundle;
import java.util.MissingResourceException;
import java.util.Enumeration;
import java.util.StringTokenizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The BlockChain is responsible for maintaining the list of valid Blocks
 * and also calculating the longest chain starting from the Genesis Block.
 * @author Robert Brautigam
 * TODO: use ? extends to make methods for implementation (don't have to cast)
 * TODO: remove network type from keys
 */
public class BlockChainImpl extends Observable implements BlockChain
{
   private static Logger logger = LoggerFactory.getLogger(BlockChainImpl.class);
   private static final long TARGET_TIMESPAN = 14*24*60*60*1000; // A target lasts 14 days
   private static final long TARGET_SPACING = 10*60*1000; // Spacing between two blocks 10 minutes
   private static final long TARGET_RECALC = TARGET_TIMESPAN / TARGET_SPACING;
   private static final int MEDIAN_BLOCKS = 11;

   private static Map<BigInteger,Map<Long,BigInteger>> knownHashes =
      new HashMap<BigInteger,Map<Long,BigInteger>>();
   private BlockImpl genesisBlock = null;
   private BlockStorage blockStorage = null;
   private BlockChainListener listener = null;

   public BlockChainImpl(BlockImpl genesisBlock, BlockStorage blockStorage)
      throws VerificationException
   {
      this.genesisBlock=genesisBlock;
      this.blockStorage=blockStorage;
      // Check if the genesis blocks equal, or add genesis block if storage is empty.
      // Here we assume that the storage is not tampered!
      BlockImpl storedGenesisBlock = blockStorage.getGenesisBlock();
      if ( storedGenesisBlock == null )
      {
         addBlock(genesisBlock);
      } else {
         if ( ! storedGenesisBlock.equals(genesisBlock) )
            throw new VerificationException("genesis block in storage is not the same as the block chain's");
      }
   }

   public void setListener(BlockChainListener listener)
   {
      this.listener=listener;
   }
   public BlockChainListener getListener()
   {
      return listener;
   }

   /**
    * Get the previous block.
    */
   public Block getPreviousBlock(Block current)
   {
      return blockStorage.getBlock(current.getPreviousBlockHash());
   }

   /**
    * Add a block to the chain. The block is only added if it is verified, and
    * passes all known checks. If the block already exists in the chain, nothing
    * is done (there are no changes). Note: orphan blocks are not fully checked
    * when letting into the store, but they will not be cleaned when it turns out
    * they are not valid (so they can't be repeated). Potential DOS attack vector.
    */
   public void addBlock(Block rawBlock)
      throws VerificationException
   {
      logger.debug("adding block: {}",rawBlock);
      // For now, only allow the implementation from here
      BlockImpl block = (BlockImpl) rawBlock;

      // In this method we do all the necessary checks documented on the
      // "protocol rules" section of the wiki.
      
      // Checks 1-10, except 2: covered by the context independent
      // block validation.
      logger.debug("validating block internally...");
      block.validate();
      // Check 2: Reject if duplicate of block we have in any of the three categories 
      // Note: we don't have three categories, but we can check every block we have easily
      logger.debug("checking whether block is already known...");
      if ( blockStorage.getBlock(block.getHash()) != null )
         throw new VerificationException("block ("+block+") was already present in storage");
      // Check 11: Check whether block will be an orphan block, in which case notify
      // listener to try to get that block and stop
      logger.debug("checking whether block is orphan...");
      BlockImpl previousBlock = blockStorage.getBlock(block.getPreviousBlockHash());
      if ( (previousBlock == null) || (previousBlock.isOrphan()) )
      {
         block.setOrphan(true);
         blockStorage.addBlock(block); // Store as orphan block
         // Notify listeners that we have a missing block
         if ( listener != null )
            listener.notifyMissingBlock(block.getPreviousBlockHash());
      }
      // Check 12: Check that nBits value matches the difficulty rules 
      logger.debug("checking whether block has the appropriate target...");
      block.setHeight(previousBlock.getHeight()+1);
      DifficultyTarget blockTarget = new DifficultyTarget(block.getCompressedTarget());
      Difficulty blockDifficulty = new Difficulty(blockTarget);
      block.setTotalDifficulty(previousBlock.getTotalDifficulty().add(blockDifficulty));
      DifficultyTarget calculatedTarget = getNextDifficultyTarget(previousBlock);
      if ( blockTarget.compareTo(calculatedTarget) != 0 )
      {
         // Target has to exactly match the one calculated, otherwise it is
         // considered invalid!
         throw new VerificationException("block has wrong target "+blockTarget+
               ", when calculated is: "+calculatedTarget);
      }
      // Check 13: Reject if timestamp is before the median time of the last 11 blocks
      long medianTimestamp = getMedianTimestamp(previousBlock);
      if ( block.getCreationTime() <= medianTimestamp )
         throw new VerificationException("block's creation time ("+block.getCreationTime()+
               ") is before median of previous blocks: "+medianTimestamp);
      // Check 14: Check for known hashes
      BigInteger genesisHash = new BigInteger(1,genesisBlock.getHash());
      BigInteger blockHash = new BigInteger(1,block.getHash());
      if ( knownHashes.containsKey(genesisHash) )
      {
         BigInteger knownHash = knownHashes.get(genesisHash).get(block.getHeight());
         if ( (knownHash != null) && (!knownHash.equals(blockHash)) )
            throw new VerificationException("block should have a hash we already know, but it doesn't, might indicate a tampering or attack");
      }
      else
      {
         logger.warn("known hashes don't exist for this chain, security checks for known blocks can not be made");
      }
   }

   /**
    * Calculate the median of the (some number of) blocks starting at the given block.
    */
   private long getMedianTimestamp(Block block)
   {
      long[] times = new long[MEDIAN_BLOCKS];
      for ( int i=0; (block!=null) && (i<MEDIAN_BLOCKS); i++ )
      {
         times[i]=block.getCreationTime();
         block = getPreviousBlock(block);
      }
      return times[MEDIAN_BLOCKS/2];
   }

   /**
    * Calculate the difficulty for the next block after the one supplied.
    */
   public DifficultyTarget getNextDifficultyTarget(Block rawBlock)
   {
      BlockImpl block = (BlockImpl) rawBlock;
      // If we're calculating for the genesis block return
      // fixed difficulty
      if ( block == null )
         return DifficultyTarget.MAX_TARGET;
      // Look whether it's time to change the difficulty setting
      // (only change every TARGET_RECALC blocks). If not, return the
      // setting of this block, because the next one has to have the same
      // target.
      if ( (block.getHeight()+1) % TARGET_RECALC != 0 )
         return new DifficultyTarget(block.getCompressedTarget());
      // We have to change the target. First collect the last TARGET_RECALC 
      // blocks (including the given block) 
      Block startBlock = block;
      for ( int i=0; (i<TARGET_RECALC-1) && (startBlock!=null); i++ )
         startBlock = getPreviousBlock(startBlock);
      if ( startBlock == null )
         return DifficultyTarget.MAX_TARGET; // This shouldn't happen, we reached genesis
      // Calculate the time the TARGET_RECALC blocks took
      long calculatedTimespan = block.getCreationTime() - startBlock.getCreationTime();
      if (calculatedTimespan < TARGET_TIMESPAN/4)
         calculatedTimespan = TARGET_TIMESPAN/4;
      if (calculatedTimespan > TARGET_TIMESPAN*4)
         calculatedTimespan = TARGET_TIMESPAN*4;
      // Calculate new target, but allow no more than maximum target
      DifficultyTarget difficultyTarget = new DifficultyTarget(block.getCompressedTarget());
      BigInteger target = difficultyTarget.getTarget();
      target = target.multiply(BigInteger.valueOf(calculatedTimespan));
      target = target.divide(BigInteger.valueOf(TARGET_TIMESPAN));
      // Return the new difficulty setting
      DifficultyTarget resultTarget = new DifficultyTarget(target);
      if ( resultTarget.compareTo(DifficultyTarget.MAX_TARGET) > 0 )
         return DifficultyTarget.MAX_TARGET;
      return resultTarget;
   }

   public Block getGenesisBlock()
   {
      return genesisBlock;
   }

   public Block getLastBlock()
   {
      return blockStorage.getLastBlock();
   }

   static
   {
      logger.debug("reading known hashes...");
      try
      {
         Map<Integer,BigInteger> idGenesis = new HashMap<Integer,BigInteger>();
         ResourceBundle bundle = ResourceBundle.getBundle("chain-knownhashes");
         Enumeration<String> keys = bundle.getKeys();
         while ( keys.hasMoreElements() )
         {
            String key = keys.nextElement();
            StringTokenizer tokens = new StringTokenizer(key,".");
            tokens.nextToken();
            int id = Integer.valueOf(tokens.nextToken());
            long height = Long.valueOf(tokens.nextToken());
            BigInteger hash = new BigInteger(bundle.getString(key).substring(2),16);
            // Put into maps
            if ( height == 0 )
            {
               // This is a genesis block, so rememeber and create maps
               idGenesis.put(id,hash);
               knownHashes.put(hash,new HashMap<Long,BigInteger>());
            }
            else
            {
               // This is a random height, so there should be a map for that
               Map<Long, BigInteger> values = knownHashes.get(idGenesis.get(id));
               if ( values == null )
                  logger.warn("can not accept known value for id "+id+", height "+height+
                        ", because genesis hash not yet defined");
               else
                  values.put(height,hash);
            }
         }
      } catch ( MissingResourceException e ) {
         logger.warn("can not read known hashes for the block chain, security might be impacted",e);
      }
   }
}

