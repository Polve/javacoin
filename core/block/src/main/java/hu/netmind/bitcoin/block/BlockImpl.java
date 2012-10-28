/**
 * Copyright (C) 2011  NetMind Consulting Bt.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTAB ILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  0211 1-13 07   USA
 */

package hu.netmind.bitcoin.block;

import it.nibbles.bitcoin.utils.BtcUtil;
import hu.netmind.bitcoin.*;
import hu.netmind.bitcoin.keyfactory.ecc.BitcoinUtil;
import hu.netmind.bitcoin.net.BlockHeader;
import hu.netmind.bitcoin.net.Tx;
import hu.netmind.bitcoin.net.BlockMessage;
import hu.netmind.bitcoin.net.BitCoinOutputStream;
import hu.netmind.bitcoin.net.HexUtil;
import hu.netmind.bitcoin.net.ArraysUtil;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.LinkedList;
import java.util.Collections;
import java.util.Set;
import java.util.HashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Block is a container in which BitCoin transactions are grouped. Generating a
 * Block is a relatively hard computational task that is constantly adjusted so that
 * the whole BitCoin network is able to produce one Block approximately every 10  minutes.
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
public class BlockImpl implements Block, Hashable
{
   private static final int BLOCK_DEFAULT_VERSION = 1;
   private static final long BLOCK_FUTURE_VALIDITY = 2*60*60*1000 ; // 2 hrs millis
   private static Logger logger = LoggerFactory.getLogger(BlockImpl.class);
   private static KnownExceptions exceptions = new KnownExceptions();

   // These are unalterable properties of the block
   private long creationTime;
   private long nonce;
   private long compressedTarget;
   private long version;
   private byte[] previousBlockHash;
   private byte[] merkleRoot;
   private byte[] hash;
   private List<TransactionImpl> transactions;

   /**
    * Construct hash with basic data given, without hash (which will be calculated).
    */
   public BlockImpl(List<TransactionImpl> transactions,
         long creationTime, long nonce, long compressedTarget, byte[] previousBlockHash, byte[] merkleRoot)
      throws BitcoinException
   {
      this(transactions,creationTime,nonce,compressedTarget,previousBlockHash,merkleRoot,null);
   }

   /**
    * Construct block with hash precalculated.
    */
   public BlockImpl(List<TransactionImpl> transactions,
         long creationTime, long nonce, long compressedTarget, byte[] previousBlockHash, 
         byte[] merkleRoot, byte[] hash)
      throws BitcoinException
   {
      this(transactions, creationTime, nonce, compressedTarget, previousBlockHash, merkleRoot, hash, BLOCK_DEFAULT_VERSION);
   }
   
   public BlockImpl(List<TransactionImpl> transactions,
         long creationTime, long nonce, long compressedTarget, byte[] previousBlockHash, 
         byte[] merkleRoot, byte[] hash, long version)
      throws BitcoinException
   {
      this.version=version;
      this.creationTime=creationTime;
      this.nonce=nonce;
      this.compressedTarget=compressedTarget;
      this.previousBlockHash=previousBlockHash;
      this.merkleRoot=merkleRoot;
      this.hash=hash;
      if ( hash == null )
         this.hash = calculateHash();
      if (transactions != null)
         this.transactions = Collections.unmodifiableList(new LinkedList<>(transactions));
   }

   /**
    * Get the network block header representation of this Block.
    */
   public BlockHeader createBlockHeader()
   {
      return new BlockHeader(version,previousBlockHash,merkleRoot,creationTime,
            compressedTarget,nonce);
   }

   /**
    * Calculate the hash of this block.
    */
   protected byte[] calculateHash()
      throws BitcoinException
   {
      try
      {
         BlockHeader blockHeader = createBlockHeader();
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
         byte[] result = ArraysUtil.reverse(digest.digest(firstHash));
         if ( logger.isDebugEnabled() )
            logger.debug("hashed to: {}",HexUtil.toHexString(result));
         return result;
      } catch ( NoSuchAlgorithmException e ) {
         throw new BitcoinException("can not find sha-256 algorithm for hash calculation",e);
      } catch ( IOException e ) {
         throw new BitcoinException("failed to calculate hash for block header",e);
      }
   }

   /**
    * Run all validations that require no context.
    */
   @Override
   public void validate()
      throws VerificationException
   {
      // This method goes over all the rules mentioned at:
      // https://en.bitcoin.it/wiki/Protocol_rules#.22 block.22 _messages
      
      // 1. Check syntactic correctness 
      //    Done: already done when block is parsed
      // 2. Reject if duplicate of block we have in any of the three categories 
      //    Ommitted: needs context, and depends on the original client implementation
      // 3. Transaction list must be non-empty 
      //    Note: This is not true, we want to be able to filter, so no check is made
      // 4. Block hash must satisfy claimed nBits proof of work 
      DifficultyTarget claimedTarget = new DifficultyTarget(compressedTarget);
      DifficultyTarget hashTarget = new DifficultyTarget(hash);
      if ( hashTarget.compareTo(claimedTarget) > 0 )
         throw new VerificationException("difficulty of block ("+this+") does not have claimed difficulty of: "+claimedTarget);
      // 5. Block timestamp must not be more than two hours in the future 
      if ( creationTime > System.currentTimeMillis() + BLOCK_FUTURE_VALIDITY )
         throw new VerificationException("creation time of block ("+this+"): "+new Date(creationTime)+" is too far in future");
      // 6. First transaction must be coinbase (i.e. only 1 input, with hash=0, n=-1), the rest must not be 
      // Note: Not true, instead the first and only the first transaction can be coinbase
      for ( int i=1; i<transactions.size(); i++ )
         if ( transactions.get(i).isCoinbase() )
            throw new VerificationException("block's "+i+"the transaction is a coinbase, it should be the first transaction then");
      // 7. For each transaction, apply "tx" checks 2-4 
      //    Note: this does all the non-context aware checks for transactions
      for ( Transaction tx : transactions )
         tx.validate();
      // 8. For the coinbase (first) transaction, scriptSig length must be 2-10 0 
      //    Ommitted: checked in transaction validate
      // 9. Reject if sum of transaction sig opcounts > MAX_BLOCK_SIGOPS 
      //    Ommitted: transactions already check for script complexity
      // 10. Verify Merkle hash 
      try
      {
         MerkleTree tree = new MerkleTree(getTransactions());
         if ( ! Arrays.equals(tree.getRoot(),merkleRoot) )
            throw new VerificationException("block's ("+this+") merkle root ("+BtcUtil.hexOut(merkleRoot)+") does not match transaction hashes root: "+
                  BtcUtil.hexOut(tree.getRoot()));
      } catch ( VerificationException e ) {
         throw e;
      } catch ( BitcoinException e ) {
         throw new VerificationException("unable to create merkle tree for block "+this,e);
      }
      // Additional check: All inputs refer to a different output
      Set<String> usedOuts = new HashSet<String>();
      for ( Transaction tx : transactions )
      {
         for ( TransactionInput in : tx.getInputs() )
         {
            String referredOut = Arrays.toString(in.getClaimedTransactionHash())+"-"+
               in.getClaimedOutputIndex();
            if ( usedOuts.contains(referredOut) )
               throw new VerificationException("block "+this+" referes twice to output: "+referredOut);
            usedOuts.add(referredOut);
         }
      }
   }

   @Override
   public long getCreationTime()
   {
      return creationTime;
   }
   @Override
   public long getNonce()
   {
      return nonce;
   }
   @Override
   public long getCompressedTarget()
   {
      return compressedTarget;
   }
   @Override
   public byte[] getMerkleRoot()
   {
      return merkleRoot;
   }
   @Override
   public byte[] getHash()
   {
      return hash;
   }
   @Override
   public byte[] getPreviousBlockHash()
   {
      return previousBlockHash;
   }

   @Override
   public List<Transaction> getTransactions()
   {
      return Collections.<Transaction>unmodifiableList(transactions);
   }

   @Override
   public int hashCode()
   {
      return Arrays.hashCode(hash);
   }

   @Override
   public long getVersion()
   {
      return version;
   }

   @Override
   public boolean equals(Object o)
   {
      if ( o == null )
         return false;
      if ( !(o instanceof BlockImpl) )
         return false;
      return Arrays.equals(((BlockImpl) o).hash,hash);
   }

   public static BlockImpl createBlock(ScriptFactory scriptFactory, BlockMessage blockMessage)
      throws BitcoinException
   {
      List<TransactionImpl> txs = new LinkedList<>();
      for ( Tx tx : blockMessage.getTransactions() )
         txs.add(TransactionImpl.createTransaction(scriptFactory,tx));
      BlockImpl block = new BlockImpl(txs,blockMessage.getHeader().getTimestamp(),
            blockMessage.getHeader().getNonce(), blockMessage.getHeader().getDifficulty(),
            blockMessage.getHeader().getPrevBlock(),blockMessage.getHeader().getRootHash(),null,blockMessage.getHeader().getVersion());
      return block;
   }

   public BlockMessage createBlockMessage(long magic)
      throws IOException
   {
      List<Tx> txs = new LinkedList<>();
      for ( TransactionImpl transaction : transactions )
         txs.add(transaction.createTx());
      BlockMessage message = new BlockMessage(magic,createBlockHeader(),txs);
      return message;
   }

   @Override
   public String toString()
   {
      return "Block (hash "+BtcUtil.hexOut(hash)+") created at "+new Date(creationTime)+", transactions: "+transactions;
   }
}
