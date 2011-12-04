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

import hu.netmind.bitcoin.Block;
import hu.netmind.bitcoin.Transaction;
import hu.netmind.bitcoin.TransactionInput;
import hu.netmind.bitcoin.TransactionOutput;
import hu.netmind.bitcoin.BitCoinException;
import hu.netmind.bitcoin.node.p2p.BlockHeader;
import hu.netmind.bitcoin.node.p2p.BitCoinOutputStream;
import hu.netmind.bitcoin.node.p2p.HexUtil;
import hu.netmind.bitcoin.node.p2p.ArraysUtil;
import hu.netmind.bitcoin.VerificationException;
import hu.netmind.bitcoin.ScriptFragment;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.AbstractList;
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
public class BlockImpl implements Block
{
   public static BlockImpl MAIN_GENESIS;

   private static final int BLOCK_VERSION = 1;
   private static final long BLOCK_FUTURE_VALIDITY = 2*60*60*1000 ; // 2 hrs millis
   private static Logger logger = LoggerFactory.getLogger(BlockImpl.class);

   // These are unalterable properties of the block
   private long creationTime;
   private long nonce;
   private long compressedTarget;
   private byte[] previousBlockHash;
   private byte[] merkleRoot;
   private byte[] hash;
   private List<Transaction> transactions;

   /**
    * Construct hash with basic data given, without hash (which will be calculated).
    */
   public BlockImpl(List<Transaction> transactions,
         long creationTime, long nonce, long compressedTarget, byte[] previousBlockHash, byte[] merkleRoot)
      throws BitCoinException
   {
      this(transactions,creationTime,nonce,compressedTarget,previousBlockHash,merkleRoot,null);
   }

   /**
    * Construct block with hash precalculated.
    */
   public BlockImpl(List<Transaction> transactions,
         long creationTime, long nonce, long compressedTarget, byte[] previousBlockHash, 
         byte[] merkleRoot, byte[] hash)
      throws BitCoinException
   {
      this.creationTime=creationTime;
      this.nonce=nonce;
      this.compressedTarget=compressedTarget;
      this.previousBlockHash=previousBlockHash;
      this.merkleRoot=merkleRoot;
      this.hash=hash;
      if ( hash == null )
         this.hash = calculateHash();
      this.transactions = Collections.unmodifiableList(new LinkedList<Transaction>(transactions));
   }

   /**
    * Get the network block header representation of this Block.
    */
   private BlockHeader getBlockHeader()
   {
      return new BlockHeader(BLOCK_VERSION,previousBlockHash,merkleRoot,creationTime,
            compressedTarget,nonce);
   }

   /**
    * Calculate the hash of this block.
    */
   private byte[] calculateHash()
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
         byte[] result = ArraysUtil.reverse(digest.digest(firstHash));
         if ( logger.isDebugEnabled() )
            logger.debug("hashed to: {}",HexUtil.toHexString(result));
         return result;
      } catch ( NoSuchAlgorithmException e ) {
         throw new BitCoinException("can not find sha-256 algorithm for hash calculation",e);
      } catch ( IOException e ) {
         throw new BitCoinException("failed to calculate hash for block header",e);
      }
   }

   /**
    * Run all validations that require no context.
    */
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
      // 10 . Verify Merkle hash 
      try
      {
         MerkleTree tree = new MerkleTree(transactions);
         if ( ! Arrays.equals(tree.getRoot(),merkleRoot) )
            throw new VerificationException("block's ("+this+") merkle root ("+HexUtil.toHexString(merkleRoot)+") does not match transaction hashes root: "+
                  HexUtil.toHexString(tree.getRoot()));
      } catch ( VerificationException e ) {
         throw e;
      } catch ( BitCoinException e ) {
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

   public long getCreationTime()
   {
      return creationTime;
   }
   public long getNonce()
   {
      return nonce;
   }
   public long getCompressedTarget()
   {
      return compressedTarget;
   }
   public byte[] getMerkleRoot()
   {
      return merkleRoot;
   }
   public byte[] getHash()
   {
      return hash;
   }
   public byte[] getPreviousBlockHash()
   {
      return previousBlockHash;
   }

   public List<Transaction> getTransactions()
   {
      return transactions;
   }

   static
   {
      try
      {
         // Create genesis transaction
         List<TransactionInputImpl> ins = new LinkedList<TransactionInputImpl>();
         TransactionInputImpl input = new TransactionInputImpl(
               HexUtil.toByteArray("00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00"),
               -1,
               new ScriptFragment() {
                  private byte[] scriptBytes = 
                     HexUtil.toByteArray("04 FF FF 00 1D 01 04 45 54 68 65 20 54 69 6D 65 73 20 30 33 2F 4A 61 6E 2F 32 30 30 39 20 43 68 61 6E 63 65 6C 6C 6F 72 20 6F 6E 20 62 72 69 6E 6B 20 6F 66 20 73 65 63 6F 6E 64 20 62 61 69 6C 6F 75 74 20 66 6F 72 20 62 61 6E 6B 73");
                  public byte[] toByteArray()
                  {
                     return scriptBytes;                  
                  }
                  public boolean isComputationallyExpensive()
                  {
                     return false;
                  }
                  public ScriptFragment getSubscript(byte[]... sigs)
                  {
                     return this;
                  }
               },
               0xFFFFFFFFl);
         ins.add(input);
         List<TransactionOutputImpl> outs = new LinkedList<TransactionOutputImpl>();
         TransactionOutputImpl output = new TransactionOutputImpl(5000000000l,
               new ScriptFragment() {
                  private byte[] scriptBytes = 
                     HexUtil.toByteArray("41 04 67 8A FD B0 FE 55 48 27 19 67 F1 A6 71 30 B7 10 5C D6 A8 28 E0 39 09 A6 79 62 E0 EA 1F 61 DE B6 49 F6 BC 3F 4C EF 38 C4 F3 55 04 E5 1E C1 12 DE 5C 38 4D F7 BA 0B 8D 57 8A 4C 70 2B 6B F1 1D 5F AC");
                  public byte[] toByteArray()
                  {
                     return scriptBytes;                  
                  }
                  public boolean isComputationallyExpensive()
                  {
                     return false;
                  }
                  public ScriptFragment getSubscript(byte[]... sigs)
                  {
                     return this;
                  }
               });
         outs.add(output);
         List<Transaction> transactions = new LinkedList<Transaction>();
         Transaction tx = new TransactionImpl(ins,outs,0l);
         transactions.add(tx);
         // Create the main network genesis block as a constant
         MAIN_GENESIS = new BlockImpl(transactions,1231006505000l,2083236893l,0x1d00ffffl,
               HexUtil.toByteArray("00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00"),
               HexUtil.toByteArray("4A 5E 1E 4B AA B8 9F 3A 32 51 8A 88 C3 1B C8 7F 61 8F 76 67 3E 2C C7 7A B2 12 7B 7A FD ED A3 3B "),
               HexUtil.toByteArray("00 00 00 00 00 19 D6 68 9C 08 5A E1 65 83 1E 93 4F F7 63 AE 46 A2 A6 C1 72 B3 F1 B6 0A 8C E2 6F "));
      } catch ( BitCoinException e ) {
         logger.error("can not construct main genesis block, main network will not be usable",e);
      }
   }
}

