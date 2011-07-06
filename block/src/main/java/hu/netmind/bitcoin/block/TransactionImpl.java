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

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import hu.netmind.bitcoin.Transaction;
import hu.netmind.bitcoin.TransactionInput;
import hu.netmind.bitcoin.TransactionOutput;
import hu.netmind.bitcoin.Block;
import hu.netmind.bitcoin.BitCoinException;
import hu.netmind.bitcoin.node.p2p.TxIn;
import hu.netmind.bitcoin.node.p2p.TxOut;
import hu.netmind.bitcoin.node.p2p.Tx;
import hu.netmind.bitcoin.node.p2p.BitCoinOutputStream;
import hu.netmind.bitcoin.node.p2p.HexUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Robert Brautigam
 */
public class TransactionImpl implements Transaction
{
   private static final int TX_VERSION = 1;
   
   private static Logger logger = LoggerFactory.getLogger(TransactionImpl.class);

   private List<TransactionInputImpl> inputs;
   private List<TransactionOutputImpl> outputs;
   private long lockTime;
   private transient Block block; // Only set runtime by block
   private byte[] hash;

   /**
    * Create the transaction with the inputs, outputs and locking time. This method
    * computes the transaction hash.
    */
   public TransactionImpl(List<TransactionInputImpl> inputs, List<TransactionOutputImpl> outputs,
         long lockTime)
      throws BitCoinException
   {
      this(inputs,outputs,lockTime,null);
   }
   
   /**
    * Create the transaction with all the necessary parameters, but also the computed hash.
    * This constructor should be used when deserializing transactions.
    */
   public TransactionImpl(List<TransactionInputImpl> inputs, List<TransactionOutputImpl> outputs,
         long lockTime, byte[] hash)
      throws BitCoinException
   {
      this.inputs=inputs;
      this.outputs=outputs;
      this.lockTime=lockTime;
      this.hash=hash;
      // If no hash given, then calculate it
      if ( hash == null )
         calculateHash(null);
      // Make all inputs and outputs be a part of this transaction
      for ( TransactionInputImpl input : inputs )
         input.setTransaction(this);
      for ( int index = 0; index <outputs.size(); index++ )
      {
         TransactionOutputImpl output = outputs.get(index);
         output.setTransaction(this);
         output.setIndex(index);
      }
   }

   public List<TransactionInput> getInputs()
   {
      return Collections.unmodifiableList((List<? extends TransactionInput>) inputs);
   }

   public List<TransactionOutput> getOutputs()
   {
      return Collections.unmodifiableList((List<? extends TransactionOutput>) outputs);
   }

   public long getLockTime()
   {
      return lockTime;
   }

   public Block getBlock()
   {
      return block;
   }

   /**
    * Block is set by the block itself when the transaction is added to it.
    */
   void setBlock(Block block)
   {
      this.block=block;
   }

   public byte[] getHash()
   {
      return hash;
   }

   /**
    * Convert this transaction object into a protocol transaction object.
    */
   private Tx getTx()
   {
      // Create txouts
      List<TxOut> outs = new ArrayList<TxOut>();
      for ( TransactionOutput output : outputs )
         outs.add(new TxOut(output.getValue(),
                  (output.getScript()==null?new byte[] {}:output.getScript().toByteArray())));
      // Create txins
      List<TxIn> ins = new ArrayList<TxIn>();
      for ( TransactionInput input : inputs )
         ins.add(new TxIn(input.getClaimedOutput().getTransaction().getHash(),
                  input.getClaimedOutput().getIndex(), 
                  (input.getSignatureScript()==null?new byte[] {}:input.getSignatureScript().toByteArray()), 
                  input.getSequence()));
      // Create transaction itself
      Tx tx = new Tx(TX_VERSION,ins,outs,lockTime);
      return tx;
   }

   /**
    * Calculate the hash of the whole transaction, with some optional additional bytes.
    */
   void calculateHash(byte[] postfix)
      throws BitCoinException
   {
      try
      {
         Tx tx = getTx();
         // Now serialize this to byte array
         ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
         BitCoinOutputStream output = new BitCoinOutputStream(byteOutput);
         tx.writeTo(output);
         // Add postfix if it's there
         if ( postfix != null )
            output.write(postfix);
         output.close();
         byte[] txBytes = byteOutput.toByteArray();
         if ( logger.isDebugEnabled() )
            logger.debug("hashing transaction: {}",HexUtil.toHexString(txBytes));
         // Hash this twice
         MessageDigest digest = MessageDigest.getInstance("SHA-256");
         byte[] firstHash = digest.digest(txBytes);
         digest.reset();
         hash = digest.digest(firstHash);
      } catch ( NoSuchAlgorithmException e ) {
         throw new BitCoinException("can not find sha-256 algorithm for hash calculation",e);
      } catch ( IOException e ) {
         throw new BitCoinException("failed to calculate hash for transaction",e);
      }
   }
}

