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
import hu.netmind.bitcoin.BitcoinException;
import hu.netmind.bitcoin.Block;
import hu.netmind.bitcoin.block.BitcoinFactory;
import hu.netmind.bitcoin.block.BlockChainLink;
import hu.netmind.bitcoin.block.BlockImpl;
import it.nibbles.javacoin.storage.StorageException;
import hu.netmind.bitcoin.block.TransactionImpl;
import static it.nibbles.javacoin.storage.bdb.BytesBinding.readBytes;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Serializes and deserializes block headers as using the "tuple" serialization methods.
 *
 * @author Alessandro Polverini
 */
public class BlockChainHeaderBinding extends TupleBinding<BlockChainLink> {

  private BitcoinFactory bitcoinFactory;

  public BlockChainHeaderBinding(BitcoinFactory bitcoinFactory) {
    this.bitcoinFactory = bitcoinFactory;
  }

  @Override
  public void objectToEntry(BlockChainLink e, TupleOutput to) {
    Block block = e.getBlock();
    to.writeLong(block.getCreationTime());
    to.writeLong(block.getNonce());
    to.writeLong(block.getCompressedTarget());
    to.write(block.getPreviousBlockHash());
    to.write(block.getMerkleRoot());
    to.write(block.getHash());
    to.writeLong(block.getVersion());
    to.writeLong(e.getTotalDifficulty().getDifficulty().longValue());
    to.writeInt(e.getHeight());
  }

  @Override
  public BlockChainLink entryToObject(TupleInput in) {
    List<TransactionImpl> transactions = new ArrayList<>();
    try {
      return bitcoinFactory.newBlockChainLink(
              new BlockImpl(
              transactions, in.readLong(), in.readLong(), in.readLong(),
              readBytes(in, 32), readBytes(in, 32), readBytes(in, 32), in.readLong()),
              new BigDecimal(in.readLong()),
              in.readInt());
    } catch (BitcoinException ex) {
      throw new StorageException("Error reading blockheader from db: "+ex.getMessage(), ex);
    }
  }

}
