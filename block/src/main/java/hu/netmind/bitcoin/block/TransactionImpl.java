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
import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;
import java.math.BigInteger;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import hu.netmind.bitcoin.Transaction;
import hu.netmind.bitcoin.TransactionInput;
import hu.netmind.bitcoin.TransactionOutput;
import hu.netmind.bitcoin.Block;
import hu.netmind.bitcoin.BitCoinException;
import hu.netmind.bitcoin.VerificationException;
import hu.netmind.bitcoin.ScriptException;
import hu.netmind.bitcoin.node.p2p.TxIn;
import hu.netmind.bitcoin.node.p2p.TxOut;
import hu.netmind.bitcoin.node.p2p.Tx;
import hu.netmind.bitcoin.node.p2p.BitCoinOutputStream;
import hu.netmind.bitcoin.node.p2p.HexUtil;
import hu.netmind.bitcoin.node.p2p.ArraysUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Robert Brautigam
 */
public class TransactionImpl implements Transaction
{
   private static final int TX_VERSION = 1;
   private static final long MAX_BLOCK_SIZE = 1000000;
   private static final long MIN_BLOCK_SIZE = 100;
   private static final long COIN = 100000000;
   private static final long MAX_MONEY = 21000000l * COIN;
   
   private static Logger logger = LoggerFactory.getLogger(TransactionImpl.class);

   private List<TransactionInputImpl> inputs;
   private List<TransactionOutputImpl> outputs;
   private long lockTime;
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
         this.hash = calculateHash(null);
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
         ins.add(new TxIn(input.getClaimedTransactionHash(),input.getClaimedOutputIndex(),
                  (input.getSignatureScript()==null?new byte[] {}:input.getSignatureScript().toByteArray()), 
                  input.getSequence()));
      // Create transaction itself
      Tx tx = new Tx(TX_VERSION,ins,outs,lockTime);
      return tx;
   }

   /**
    * Calculate the hash of the whole transaction, with some optional additional bytes.
    */
   byte[] calculateHash(byte[] postfix)
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
         byte[] result = ArraysUtil.reverse(digest.digest(firstHash));
         if ( logger.isDebugEnabled() )
            logger.debug("hashed to: {}",HexUtil.toHexString(result));
         return result;
      } catch ( NoSuchAlgorithmException e ) {
         throw new BitCoinException("can not find sha-256 algorithm for hash calculation",e);
      } catch ( IOException e ) {
         throw new BitCoinException("failed to calculate hash for transaction",e);
      }
   }

   /**
    * Do internal validations.
    */
   public void validate()
      throws VerificationException
   {
      // This method goes over all the rules mentioned at:
      // https://en.bitcoin.it/wiki/Protocol_rules#cite_note-1
      // Note: only those checks are made which need no context (1-7, except 5)

      // 1. Check syntactic correctness
      //    Done: already done when transaction is parsed
      // 2. Make sure neither in or out lists are empty
      if ( inputs.isEmpty() )
         throw new VerificationException("input list is empty for transaction: "+this);
      if ( outputs.isEmpty() )
         throw new VerificationException("output list is empty for transaction: "+this);
      // 3. Size in bytes < MAX_BLOCK_SIZE
      try
      {
         Tx tx = getTx();
         // Now serialize this to byte array
         ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
         BitCoinOutputStream output = new BitCoinOutputStream(byteOutput);
         tx.writeTo(output);
         output.close();
         int totalSize = byteOutput.toByteArray().length;
         if ( totalSize > MAX_BLOCK_SIZE )
            throw new VerificationException("transaction ("+this+") is bigger than: "+MAX_BLOCK_SIZE+" bytes: "+totalSize);
         if ( totalSize < MIN_BLOCK_SIZE )
            throw new VerificationException("transaction ("+this+") is smaller than: "+MIN_BLOCK_SIZE+" bytes: "+totalSize);
      } catch ( IOException e ) {
         throw new VerificationException("can not serialize transaction: "+this);
      }
      // 4. Each output value, as well as the total, must be in legal money range
      long totalMoney = 0;
      for ( TransactionOutput out : outputs )
      {
         if ( out.getValue() < 0 )
            throw new VerificationException("output ("+out+") of transaction "+this+", has negative value: "+out.getValue());
         if ( out.getValue() > MAX_MONEY )
            throw new VerificationException("output ("+out+") of transaction "+this+", has more value than allowed: "+out.getValue());
         totalMoney+=out.getValue();
      }
      if ( totalMoney < 0 )
         throw new VerificationException("total value of transaction "+this+" is negative: "+totalMoney);
      if ( totalMoney > MAX_MONEY )
         throw new VerificationException("total value of transaction "+this+" is more than allowed: "+totalMoney);
      // 5. Make sure none of the inputs have hash=0, n=-1 (coinbase transactions)
      //    Omitted: this check has to be done only when transaction is
      //    standalone (not in block), because coinbase is always already in a block
      // 6. Check that nLockTime <= INT_MAX, size in bytes >= 100, and sig opcount <= 2
      //    Note: min size check is done in step 3, sig opcount is done in step 7 (next)
      if ( (getLockTime()<0) || (getLockTime() > Integer.MAX_VALUE) )
         throw new VerificationException("lock time of transaction "+this+" is not in valid range (integer): "+getLockTime());
      // 7. Reject "nonstandard" transactions: scriptSig doing anything other 
      //    than pushing numbers on the stack, or scriptPubkey not matching the two usual forms
      //    Note: only implemented dos attack on sig check (count the number of sigchecks)
      try
      {
         for ( TransactionOutput out : outputs )
            if ( out.getScript().isComputationallyExpensive() )
               throw new VerificationException("transaction ("+this+") has computationally too expensive output: "+out);
         for ( TransactionInput in : inputs )
            if ( in.getSignatureScript().isComputationallyExpensive() )
               throw new VerificationException("transaction ("+this+") has computationally too expensive input: "+in);
      } catch ( ScriptException e ) {
         throw new VerificationException("could not parse script fragment",e);
      }
      // Tests after #7 ommitted because they need context.
      // Additional tests follow found in the source (or checked in other places)

      // Check coinbase / not coinbase
      if ( isCoinbase() )
      {
         // Scriptsig should be then between 2 and 100
         int scriptSigLength = inputs.get(0).getSignatureScript().toByteArray().length;
         if ( (scriptSigLength < 2) || (scriptSigLength > 100) )
            throw new VerificationException("script in coinbase transaction ("+this+") has invalid size: "+scriptSigLength);
      }
      else
      {
         for ( TransactionInputImpl in : inputs )
            if ( (in.getClaimedOutputIndex() < 0) || 
                 (new BigInteger(1,inputs.get(0).getClaimedTransactionHash()).equals(BigInteger.ZERO)) )
               throw new VerificationException("input for transaction ("+this+") has wrong reference to previous transaction");
      }
      // Additional check: All inputs refer to a different output
      Set<String> usedOuts = new HashSet<String>();
      for ( TransactionInput in : getInputs() )
      {
         String referredOut = Arrays.toString(in.getClaimedTransactionHash())+"-"+
            in.getClaimedOutputIndex();
         if ( usedOuts.contains(referredOut) )
            throw new VerificationException("transaction "+this+" referes twice to output: "+referredOut);
         usedOuts.add(referredOut);
      }
   }

   public boolean isCoinbase()
   {
      return (inputs.size()==1) &&
         (inputs.get(0).getClaimedOutputIndex()==-1) &&
         (new BigInteger(1,inputs.get(0).getClaimedTransactionHash()).equals(BigInteger.ZERO));
   }
}

