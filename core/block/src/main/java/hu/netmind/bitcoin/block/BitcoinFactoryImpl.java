/**
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
package hu.netmind.bitcoin.block;

import hu.netmind.bitcoin.BitCoinException;
import hu.netmind.bitcoin.Block;
import hu.netmind.bitcoin.ScriptFactory;
import java.math.BigDecimal;
import java.util.List;

/**
 *
 * @author Alessandro Polverini
 */
public class BitcoinFactoryImpl
{

   private ScriptFactory scriptFactory;
   private boolean isTestnet;

   public BitcoinFactoryImpl(ScriptFactory scriptFactory, boolean isTestnet)
   {
      this.scriptFactory = scriptFactory;
      this.isTestnet = isTestnet;
   }

   public TransactionImpl newTransactionImpl(List<TransactionInputImpl> inputs, List<TransactionOutputImpl> outputs, long lockTime, byte[] hash)
      throws BitCoinException
   {
      return new TransactionImpl(inputs, outputs, lockTime, hash);
   }

   public TransactionOutputImpl newTransactionOutputImpl(long value, byte[] scriptBytes)
   {
      return new TransactionOutputImpl(value, scriptFactory.createFragment(scriptBytes));
   }

   public TransactionInputImpl newTransactionInputImpl(byte[] referredTxHash, int referredTxIndex, byte[] scriptBytes, long sequence)
   {
      return new TransactionInputImpl(referredTxHash, referredTxIndex, scriptFactory.createFragment(scriptBytes), sequence);
   }

   public Block newBlock(List<TransactionImpl> transactions, long nTime, long nonce, long nBits,
      byte[] prevBlockHash, byte[] hashMerkleRoot, byte[] hash) throws BitCoinException
   {
      return new BlockImpl(transactions, nTime, nonce, nBits, prevBlockHash, hashMerkleRoot, hash);
   }

   public BlockChainLink newBlockChainLink(Block block, BigDecimal chainWork, long height)
   {
      return new BlockChainLink(block, new Difficulty(chainWork, isTestnet), height, false);
   }
}
