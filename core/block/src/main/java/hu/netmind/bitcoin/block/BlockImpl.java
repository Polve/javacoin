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
//   public static BlockImpl MAIN_GENESIS;
//   public static BlockImpl TESTNET_GENESIS;
//   public static BlockImpl TESTNET3_GENESIS;

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
      this.transactions = Collections.unmodifiableList(new LinkedList<TransactionImpl>(transactions));
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
      return Collections.<Transaction>unmodifiableList(transactions);
   }

   public int hashCode()
   {
      return Arrays.hashCode(hash);
   }

   public long getVersion()
   {
      return version;
   }

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
      List<TransactionImpl> txs = new LinkedList<TransactionImpl>();
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
      List<Tx> txs = new LinkedList<Tx>();
      for ( TransactionImpl transaction : transactions )
         txs.add(transaction.createTx());
      BlockMessage message = new BlockMessage(magic,createBlockHeader(),txs);
      return message;
   }

//   static
//   {
//      try
//      {
//         // Create genesis transaction for production network
//         List<TransactionInputImpl> ins = new LinkedList<>();
//         TransactionInputImpl input = new TransactionInputImpl(
//               BtcUtil.hexIn("0000000000000000000000000000000000000000000000000000000000000000"),
//               -1,
//               new ScriptFragment() {
//                  private byte[] scriptBytes = 
//                     BtcUtil.hexIn("04FFFF001D0104455468652054696D65732030332F4A616E2F32303039204368616E63656C6C6F72206F6E206272696E6B206F66207365636F6E64206261696C6F757420666F722062616E6B73");
//                  @Override
//                  public byte[] toByteArray()
//                  {
//                     return scriptBytes;                  
//                  }
//                  @Override
//                  public boolean isComputationallyExpensive()
//                  {
//                     return false;
//                  }
//                  @Override
//                  public ScriptFragment getSubscript(byte[]... sigs)
//                  {
//                     return this;
//                  }
//               },
//               0xFFFFFFFFl);
//         ins.add(input);
//         List<TransactionOutputImpl> outs = new LinkedList<>();
//         TransactionOutputImpl output = new TransactionOutputImpl(5000000000l,
//               new ScriptFragment() {
//                  private byte[] scriptBytes = 
//                     BtcUtil.hexIn("4104678AFDB0FE5548271967F1A67130B7105CD6A828E03909A67962E0EA1F61DEB649F6BC3F4CEF38C4F35504E51EC112DE5C384DF7BA0B8D578A4C702B6BF11D5FAC");
//                  @Override
//                  public byte[] toByteArray()
//                  {
//                     return scriptBytes;                  
//                  }
//                  @Override
//                  public boolean isComputationallyExpensive()
//                  {
//                     return false;
//                  }
//                  @Override
//                  public ScriptFragment getSubscript(byte[]... sigs)
//                  {
//                     return this;
//                  }
//               });
//         outs.add(output);
//         List<TransactionImpl> transactions = new LinkedList<>();
//         TransactionImpl tx = new TransactionImpl(ins,outs,0l);
//         transactions.add(tx);
//         // Create the main network genesis block as a constant
//         MAIN_GENESIS = new BlockImpl(transactions,1231006505000l,2083236893l,0x1d00ffffl,
//               BtcUtil.hexIn("0000000000000000000000000000000000000000000000000000000000000000"),
//               BtcUtil.hexIn("4A5E1E4BAAB89F3A32518A88C31BC87F618F76673E2CC77AB2127B7AFDEDA33B"),
//               null);
//         if (!Arrays.equals(MAIN_GENESIS.getHash(), BtcUtil.hexIn("000000000019D6689C085AE165831E934FF763AE46A2A6C172B3F1B60A8CE26F")))
//            throw new BitcoinException("Can't compute correct hash for production network genesis block");
//      } catch ( BitcoinException e ) {
//         logger.error("can not construct main genesis block, main network will not be usable",e);
//      }
//         
//      try
//      {
//         // Create genesis transaction for old testnet (before bitcoin 0.7)
//         List<TransactionInputImpl> ins = new LinkedList<>();
//         TransactionInputImpl input = new TransactionInputImpl(
//               BtcUtil.hexIn("0000000000000000000000000000000000000000000000000000000000000000"),
//               -1,
//               new ScriptFragment() {
//                  private byte[] scriptBytes = 
//                     BtcUtil.hexIn("04FFFF001D0104455468652054696D65732030332F4A616E2F32303039204368616E63656C6C6F72206F6E206272696E6B206F66207365636F6E64206261696C6F757420666F722062616E6B73");
//                  @Override
//                  public byte[] toByteArray()
//                  {
//                     return scriptBytes;                  
//                  }
//                  @Override
//                  public boolean isComputationallyExpensive()
//                  {
//                     return false;
//                  }
//                  @Override
//                  public ScriptFragment getSubscript(byte[]... sigs)
//                  {
//                     return this;
//                  }
//               },
//               0xFFFFFFFFl);
//         ins.add(input);
//         List<TransactionOutputImpl> outs = new LinkedList<>();
//         TransactionOutputImpl output = new TransactionOutputImpl(5000000000l,
//               new ScriptFragment() {
//                  private byte[] scriptBytes = 
//                     BtcUtil.hexIn("4104678AFDB0FE5548271967F1A67130B7105CD6A828E03909A67962E0EA1F61DEB649F6BC3F4CEF38C4F35504E51EC112DE5C384DF7BA0B8D578A4C702B6BF11D5FAC");
//                  @Override
//                  public byte[] toByteArray()
//                  {
//                     return scriptBytes;                  
//                  }
//                  @Override
//                  public boolean isComputationallyExpensive()
//                  {
//                     return false;
//                  }
//                  @Override
//                  public ScriptFragment getSubscript(byte[]... sigs)
//                  {
//                     return this;
//                  }
//               });
//         outs.add(output);
//         List<TransactionImpl> transactions = new LinkedList<>();
//         TransactionImpl tx = new TransactionImpl(ins,outs,0l);
//         transactions.add(tx);
//         // Create the test network genesis block as a constant
//         TESTNET_GENESIS = new BlockImpl(transactions,1296688602000l,384568319l,0x1d07fff8l,
//               BtcUtil.hexIn("0000000000000000000000000000000000000000000000000000000000000000"),
//               BtcUtil.hexIn("4A5E1E4BAAB89F3A32518A88C31BC87F618F76673E2CC77AB2127B7AFDEDA33B"),
//               null);
//               //HexUtil.toByteArray("00 00 00 07 19 95 08 E3 4A 9F F8 1E 6E C0 C4 77 A4 CC CF F2 A4 76 7A 8E EE 39 C1 1D B3 67 B0 08 "));
//         if (!Arrays.equals(TESTNET_GENESIS.getHash(), BtcUtil.hexIn("00000007199508e34a9ff81e6ec0c477a4cccff2a4767a8eee39c11db367b008")))
//            throw new BitcoinException("Can't compute correct hash for test network genesis block");
//      } catch ( BitcoinException e ) {
//         logger.error("can not construct old test genesis block, test network will not be usable",e);
//      }
//
//      try
//      {
//         // Create genesis transaction for testnet3 (bitcoin >= 0.7)
//         List<TransactionInputImpl> ins = new LinkedList<>();
//         TransactionInputImpl input = new TransactionInputImpl(
//               BtcUtil.hexIn("0000000000000000000000000000000000000000000000000000000000000000"),
//               -1,
//               new ScriptFragment() {
//                  private byte[] scriptBytes = 
//                     BtcUtil.hexIn("04FFFF001D0104455468652054696D65732030332F4A616E2F32303039204368616E63656C6C6F72206F6E206272696E6B206F66207365636F6E64206261696C6F757420666F722062616E6B73");
//                  @Override
//                  public byte[] toByteArray()
//                  {
//                     return scriptBytes;                  
//                  }
//                  @Override
//                  public boolean isComputationallyExpensive()
//                  {
//                     return false;
//                  }
//                  @Override
//                  public ScriptFragment getSubscript(byte[]... sigs)
//                  {
//                     return this;
//                  }
//               },
//               0xFFFFFFFFl);
//         ins.add(input);
//         List<TransactionOutputImpl> outs = new LinkedList<>();
//         TransactionOutputImpl output = new TransactionOutputImpl(5000000000l,
//               new ScriptFragment() {
//                  private byte[] scriptBytes = 
//                     BtcUtil.hexIn("4104678AFDB0FE5548271967F1A67130B7105CD6A828E03909A67962E0EA1F61DEB649F6BC3F4CEF38C4F35504E51EC112DE5C384DF7BA0B8D578A4C702B6BF11D5FAC");
//                  @Override
//                  public byte[] toByteArray()
//                  {
//                     return scriptBytes;                  
//                  }
//                  @Override
//                  public boolean isComputationallyExpensive()
//                  {
//                     return false;
//                  }
//                  @Override
//                  public ScriptFragment getSubscript(byte[]... sigs)
//                  {
//                     return this;
//                  }
//               });
//         outs.add(output);
//         List<TransactionImpl> transactions = new LinkedList<>();
//         TransactionImpl tx = new TransactionImpl(ins,outs,0l);
//         transactions.add(tx);
//         // Create the test network genesis block as a constant
//         TESTNET3_GENESIS = new BlockImpl(transactions, 1296688602000L, 414098458L, 0x1d00ffffL,
//               BtcUtil.hexIn("0000000000000000000000000000000000000000000000000000000000000000"),
//               BtcUtil.hexIn("4A5E1E4BAAB89F3A32518A88C31BC87F618F76673E2CC77AB2127B7AFDEDA33B"),
//               null);
//         if (!Arrays.equals(TESTNET3_GENESIS.getHash(), BtcUtil.hexIn("000000000933ea01ad0ee984209779baaec3ced90fa3f408719526f8d77f4943")))
//            throw new BitcoinException("Can't compute correct hash for testnet3 genesis block");
//      } catch ( BitcoinException e ) {
//         logger.error("can not construct testnet3 genesis block, test network will not be usable",e);
//      }
//   }

   public String toString()
   {
      return "Block (hash "+BtcUtil.hexOut(hash)+") created at "+new Date(creationTime)+", transactions: "+transactions;
   }
}
