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
import hu.netmind.bitcoin.Transaction;
import hu.netmind.bitcoin.TransactionInput;
import hu.netmind.bitcoin.TransactionOutput;
import hu.netmind.bitcoin.TransactionFilter;
import hu.netmind.bitcoin.BlockPath;
import hu.netmind.bitcoin.BitCoinException;
import hu.netmind.bitcoin.node.p2p.BlockHeader;
import hu.netmind.bitcoin.node.p2p.BitCoinOutputStream;
import hu.netmind.bitcoin.node.p2p.HexUtil;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Block is a container in which BitCoin transactions are grouped. Generating a
 * Block is a relatively hard computational task that is constantly adjusted so that
 * the whole BitCoin network is able to produce one Block approximately every 10 minutes.
 * When a Miner succeeds in generating a Block it will include all the pending transactions
 * in the network into this Block thereby claiming transaction fees (and generating new coins
 * also). Transactions are considered valid if they are in a Block on a longest path, all other
 * transactions are candidates to include in a next Block. Note: this implementation requires
 * <code>all</code> transactions be supplied if the merkle root hash is not yet calculated,
 * otherwise merkle root can not be calculated. Note: this block implementation does not retain
 * the merkle tree. If transactions are removed later, the root can not be re-calculated. This
 * should not be a problem, if we can trust that all transactions validated already.
 * @author Robert Brautigam
 */
public class BlockImpl extends PrefilteredTransactionContainer implements Block
{
   private static Logger logger = LoggerFactory.getLogger(BlockImpl.class);
   private static int BLOCK_VERSION = 1;

   // These are unalterable properties of the block
   private long creationTime;
   private long nonce;
   private long difficulty;
   private byte[] previousBlockHash;
   private byte[] merkleRoot;
   private byte[] calculatedMerkleRoot;
   private byte[] hash;
   private List<Transaction> transactions;
   
   // Below are properties filled runtime (and potentially altered depending on chain changes)
   private BlockImpl previousBlock;

   public BlockImpl(List<Transaction> transactions, TransactionFilter preFilter,
         long creationTime, long nonce, long difficulty, byte[] previousBlockHash, byte[] merkleRoot)
      throws BitCoinException
   {
      this(transactions,preFilter,creationTime,nonce,difficulty,previousBlockHash,merkleRoot,null,null);
   }

   public BlockImpl(List<Transaction> transactions, TransactionFilter preFilter,
         long creationTime, long nonce, long difficulty, byte[] previousBlockHash, 
         byte[] merkleRoot, byte[] calculatedMerkleRoot, byte[] hash)
      throws BitCoinException
   {
      this.creationTime=creationTime;
      this.nonce=nonce;
      this.difficulty=difficulty;
      this.previousBlockHash=previousBlockHash;
      this.merkleRoot=merkleRoot;
      this.calculatedMerkleRoot=calculatedMerkleRoot;
      if ( calculatedMerkleRoot == null )
         calculatedMerkleRoot(transactions);
      this.hash=hash;
      if ( hash == null )
         calculateHash();
      setPreFilter(preFilter);
      addTransactions(transactions);
   }

   /**
    * Get all the stored transactions.
    */
   protected List<Transaction> getStoredTransactions()
   {
      return transactions;
   }

   /**
    * Add transactions permanently to this block. This may be only invoked once when constructing
    * this object.
    */
   protected void addStoredTransactions(List<Transaction> transactions)
   {
      this.transactions=transactions;
   }

   /**
    * Calculate the merkle hash from the given transactions.
    */
   private void calculatedMerkleRoot(List<Transaction> transactions)
      throws BitCoinException
   {
      if ( (transactions==null) && (transactions.isEmpty()) )
         throw new BitCoinException("can not calculate merkle hash, since there were no transactions");
      // The merkle root here is calculated level by level, starting from the 
      // leaves, that is the actual transaction hashes. At each level two hashes
      // are added and then hashed, so each level is half as long as the previous
      // one, stopping when we arrive at a single hash.
      try
      {
         // Get digest algorithm
         MessageDigest digest = MessageDigest.getInstance("SHA-256");
         // Setup the lowest level (the transaction hashes)
         List<byte[]> level = new ArrayList<byte[]>();
         for ( Transaction tx : transactions )
            level.add(tx.getHash());
         // Now create a new level until there is only one value
         while ( level.size() > 1 )
         {
            if ( logger.isDebugEnabled() )
            {
               StringBuilder loggerBuilder = new StringBuilder("hashes on level:");
               for ( byte[] loggerHash : level )
                  loggerBuilder.append(" <"+HexUtil.toHexString(loggerHash)+">");
               logger.debug(loggerBuilder.toString());
            }
            List<byte[]> newLevel = new ArrayList<byte[]>();
            // Go through the items and add them up in pairs
            // (if the number of items is odd, just duplicate last item)
            for ( int i=0; i<level.size(); i+=2 )
            {
               // Calculate hash sum hash
               byte[] hash1 = level.get(i);
               byte[] hash2 = null;
               if ( i+1 >= level.size() )
                  hash2 = level.get(i);
               else
                  hash2 = level.get(i+1);
               // Double hash it
               digest.reset();
               digest.update(hash1,0,hash1.length);
               digest.update(hash2,0,hash1.length);
               byte[] firstHash = digest.digest();
               digest.reset();
               byte[] secondHash = digest.digest(firstHash);
               // Save the result
               newLevel.add(secondHash);
            }
            // Step to the next level and repeat
            level=newLevel;
         }
         // The merkle root is the only item on the top level
         calculatedMerkleRoot = level.get(0);
      } catch ( NoSuchAlgorithmException e ) {
         throw new BitCoinException("can not find sha-256 algorithm for merkle root calculation",e);
      }
   }

   /**
    * Get the network block header representation of this Block.
    */
   private BlockHeader getBlockHeader()
   {
      return new BlockHeader(BLOCK_VERSION,previousBlockHash,merkleRoot,creationTime,
            difficulty,nonce);
   }

   /**
    * Calculate the hash of this block.
    */
   private void calculateHash()
      throws BitCoinException
   {
      try
      {
         BlockHeader blockHeader = getBlockHeader();
         // Now serialize this to byte array
         ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
         BitCoinOutputStream output = new BitCoinOutputStream(byteOutput);
         blockHeader.writeTo(output);
         output.close();
         byte[] blockHeaderBytes = byteOutput.toByteArray();
         if ( logger.isDebugEnabled() )
            logger.debug("hashing block header: {}",HexUtil.toHexString(blockHeaderBytes));
         // Hash this twice
         MessageDigest digest = MessageDigest.getInstance("SHA-256");
         byte[] firstHash = digest.digest(blockHeaderBytes);
         digest.reset();
         hash = digest.digest(firstHash);
      } catch ( NoSuchAlgorithmException e ) {
         throw new BitCoinException("can not find sha-256 algorithm for hash calculation",e);
      } catch ( IOException e ) {
         throw new BitCoinException("failed to calculate hash for block header",e);
      }
   }

   /**
    * Get the previous block.
    */
   public Block getPreviousBlock()
   {
      return previousBlock;
   }
   void setPreviousBlock(BlockImpl previousBlock)
   {
      this.previousBlock=previousBlock;
   }

   /**
    * Get the next block on a given path.
    */
   Block getNextBlock(BlockPath path)
   {
      // TODO
      return null;
   }

   /**
    * Get claimer input for a given output in this block for a given path.
    */
   TransactionInput getClaimerInput(TransactionOutput output, BlockPath path)
   {
      // TODO
      return null;
   }

   /**
    * Get the longest path this block is on. Note, that might not be the overall
    * longest path in the block chain.
    */
   public BlockPath getLongestPath()
   {
      // TODO
      return null;
   }

   public long getCreationTime()
   {
      return creationTime;
   }
   public long getNonce()
   {
      return nonce;
   }
   public long getDifficulty()
   {
      return difficulty;
   }
   public byte[] getMerkleRoot()
   {
      return merkleRoot;
   }
   public byte[] getCalculatedMerkleRoot()
   {
      return calculatedMerkleRoot;
   }
   public byte[] getHash()
   {
      return hash;
   }
   public byte[] getPreviousBlockHash()
   {
      return previousBlockHash;
   }
}

