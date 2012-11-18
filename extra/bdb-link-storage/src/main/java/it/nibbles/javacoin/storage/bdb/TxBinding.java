/**
 * Copyright (C) 2012 nibbles.it
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option) any
 * later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package it.nibbles.javacoin.storage.bdb;

import com.sleepycat.bind.tuple.TupleBinding;
import com.sleepycat.bind.tuple.TupleInput;
import com.sleepycat.bind.tuple.TupleOutput;
import it.nibbles.javacoin.BitcoinException;
import it.nibbles.javacoin.Block;
import it.nibbles.javacoin.Transaction;
import it.nibbles.javacoin.TransactionInput;
import it.nibbles.javacoin.TransactionOutput;
import it.nibbles.javacoin.block.BitcoinFactory;
import it.nibbles.javacoin.block.BlockChainLink;
import it.nibbles.javacoin.block.BlockImpl;
import it.nibbles.javacoin.storage.StorageException;
import it.nibbles.javacoin.block.TransactionImpl;
import it.nibbles.javacoin.block.TransactionInputImpl;
import it.nibbles.javacoin.block.TransactionOutputImpl;
import static it.nibbles.javacoin.storage.bdb.BytesBinding.readBytes;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Serializes and deserializes transactions using the "tuple" serialization
 * methods.
 *
 * @author Alessandro Polverini
 */
public class TxBinding extends TupleBinding<Transaction> {

  private BitcoinFactory bitcoinFactory;

  public TxBinding(BitcoinFactory bitcoinFactory) {
    this.bitcoinFactory = bitcoinFactory;
  }

  @Override
  public void objectToEntry(Transaction transaction, TupleOutput out) {
    out.write(transaction.getHash());
    out.writeLong(transaction.getLockTime());
    out.writeInt(transaction.getInputs().size());
    for (TransactionInput input : transaction.getInputs())
      write(input, out);
    out.writeInt(transaction.getOutputs().size());
    for (TransactionOutput output : transaction.getOutputs())
      write(output, out);
  }

  private void write(TransactionInput input, TupleOutput out) {
    out.write(input.getClaimedTransactionHash());
    out.writeInt(input.getClaimedOutputIndex());
    out.writeLong(input.getSequence());
    out.writeInt(input.getSignatureScript().toByteArray().length);
    out.write(input.getSignatureScript().toByteArray());
  }

  private void write(TransactionOutput output, TupleOutput out) {
    out.writeLong(output.getValue());
    out.writeInt(output.getScript().toByteArray().length);
    out.write(output.getScript().toByteArray());
  }

  @Override
  public TransactionImpl entryToObject(TupleInput in) {
    try {
      byte[] hash = readBytes(in, 32);
      long lockTime = in.readLong();
      int numInputs = in.readInt();
      List<TransactionInputImpl> inputs = new LinkedList<>();
      for (int i = 0; i < numInputs; i++)
        inputs.add(readInput(in));
      int numOutputs = in.readInt();
      List<TransactionOutputImpl> outputs = new LinkedList<>();
      for (int i = 0; i < numOutputs; i++)
        outputs.add(readOutput(in));
      return new TransactionImpl(inputs, outputs, lockTime, hash);
    } catch (BitcoinException ex) {
      throw new StorageException("Error reading transaction from db: " + ex.getMessage(), ex);
    }
  }

  private TransactionInputImpl readInput(TupleInput in) {
    byte[] claimedHash = readBytes(in, 32);
    int claimedIndex = in.readInt();
    long sequence = in.readLong();
    int scriptLength = in.readInt();
    byte[] script = readBytes(in, scriptLength);
    return new TransactionInputImpl(claimedHash, claimedIndex,
            bitcoinFactory.getScriptFactory().createFragment(script), sequence);
  }

  private TransactionOutputImpl readOutput(TupleInput in) {
    long value = in.readLong();
    int scriptLength = in.readInt();
    byte[] script = readBytes(in, scriptLength);
    return new TransactionOutputImpl(value,
            bitcoinFactory.getScriptFactory().createFragment(script));
  }
}
