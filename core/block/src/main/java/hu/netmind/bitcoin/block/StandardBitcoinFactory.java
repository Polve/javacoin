/**
 * Copyright (C) 2012 nibbles.it.
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
package hu.netmind.bitcoin.block;

import hu.netmind.bitcoin.BitcoinException;
import hu.netmind.bitcoin.Block;
import hu.netmind.bitcoin.ScriptFactory;
import hu.netmind.bitcoin.ScriptFragment;
import it.nibbles.bitcoin.utils.BtcUtil;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Alessandro Polverini
 */
public class StandardBitcoinFactory implements BitcoinFactory
{

   protected Logger logger = LoggerFactory.getLogger(this.getClass());
   private Block GENESIS_BLOCK;
   protected ScriptFactory scriptFactory;
   protected long compressedTarget;
   protected long messageMagic;
   protected boolean isOldTestnet = false;
   protected boolean isTestnet3 = false;
   public static final DifficultyTarget MAX_PRODNET_TARGET =
      new DifficultyTarget(new BigInteger("FFFF0000000000000000000000000000000000000000000000000000", 16));
   public static final DifficultyTarget MAX_TESTNET_TARGET =
      new DifficultyTarget(new BigInteger("FFFFF0000000000000000000000000000000000000000000000000000", 16));

   protected StandardBitcoinFactory()
   {
   }

   public StandardBitcoinFactory(ScriptFactory scriptFactory) throws BitcoinException
   {
      this.scriptFactory = scriptFactory;
      setNetworkParams(0xf9beb4d9L, 1231006505000l, 2083236893l, 0x1d00ffffL, BtcUtil.hexIn("000000000019D6689C085AE165831E934FF763AE46A2A6C172B3F1B60A8CE26F"));
   }

   @Override
   public ScriptFactory getScriptFactory()
   {
      return scriptFactory;
   }

   @Override
   public long getMessageMagic()
   {
      return messageMagic;
   }

   @Override
   public BlockChainLink newBlockChainLink(Block block, BigDecimal chainWork, long height)
   {
      return new BlockChainLink(block, new Difficulty(chainWork, maxDifficultyTarget()), height, false);
   }

   @Override
   public Difficulty newDifficulty()
   {
      return new Difficulty(BigDecimal.ZERO, maxDifficultyTarget());
   }

   @Override
   public Difficulty newDifficulty(BigDecimal target)
   {
      return new Difficulty(target, maxDifficultyTarget());
   }

   @Override
   public Difficulty newDifficulty(DifficultyTarget target)
   {
      return new Difficulty(target, maxDifficultyTarget());
   }

   @Override
   public DifficultyTarget maxDifficultyTarget()
   {
      return isOldTestnet ? MAX_TESTNET_TARGET : MAX_PRODNET_TARGET;
   }

   @Override
   public Block getGenesisBlock()
   {
      return GENESIS_BLOCK;
   }

   @Override
   public Difficulty getGenesisDifficulty()
   {
      return new Difficulty(new DifficultyTarget(compressedTarget), maxDifficultyTarget());
   }

   @Override
   public boolean isTestnet3()
   {
      return isTestnet3;
   }

   @Override
   public boolean isTestnet2()
   {
      return isOldTestnet;
   }

   protected final void setNetworkParams(
      long messageMagic,
      long genesisCreationTime,
      long genesisNonce,
      long genesisCompressedTarget,
      byte[] genesisHash) throws BitcoinException
   {
      this.messageMagic = messageMagic;
      this.compressedTarget = genesisCompressedTarget;
      GENESIS_BLOCK = new BlockImpl(getGenesisTransactions(), genesisCreationTime, genesisNonce, genesisCompressedTarget,
         BtcUtil.hexIn("0000000000000000000000000000000000000000000000000000000000000000"),
         BtcUtil.hexIn("4A5E1E4BAAB89F3A32518A88C31BC87F618F76673E2CC77AB2127B7AFDEDA33B"),
         null);
      if (!Arrays.equals(GENESIS_BLOCK.getHash(), genesisHash))
      {
         throw new BitcoinException("Computed Hash of genesis block does not match known value");
      }
   }

   protected static List<TransactionImpl> getGenesisTransactions() throws BitcoinException
   {
      List<TransactionInputImpl> ins = new LinkedList<>();
      TransactionInputImpl input = new TransactionInputImpl(
         BtcUtil.hexIn("0000000000000000000000000000000000000000000000000000000000000000"),
         -1,
         new ScriptFragment()
         {
            private byte[] scriptBytes = BtcUtil.hexIn("04FFFF001D0104455468652054696D65732030332F4A616E2F32303039204368616E63656C6C6F72206F6E206272696E6B206F66207365636F6E64206261696C6F757420666F722062616E6B73");

            @Override
            public byte[] toByteArray()
            {
               return scriptBytes;
            }

            @Override
            public boolean isComputationallyExpensive()
            {
               return false;
            }

            @Override
            public ScriptFragment getSubscript(byte[]... sigs)
            {
               return this;
            }
         },
         0xFFFFFFFFl);
      ins.add(input);
      List<TransactionOutputImpl> outs = new LinkedList<>();
      TransactionOutputImpl output = new TransactionOutputImpl(5000000000l,
         new ScriptFragment()
         {
            private byte[] scriptBytes =
               BtcUtil.hexIn("4104678AFDB0FE5548271967F1A67130B7105CD6A828E03909A67962E0EA1F61DEB649F6BC3F4CEF38C4F35504E51EC112DE5C384DF7BA0B8D578A4C702B6BF11D5FAC");

            @Override
            public byte[] toByteArray()
            {
               return scriptBytes;
            }

            @Override
            public boolean isComputationallyExpensive()
            {
               return false;
            }

            @Override
            public ScriptFragment getSubscript(byte[]... sigs)
            {
               return this;
            }
         });
      outs.add(output);
      List<TransactionImpl> transactions = new LinkedList<>();
      TransactionImpl tx = new TransactionImpl(ins, outs, 0l);
      transactions.add(tx);
      return transactions;
   }
//   public TransactionImpl newTransactionImpl(List<TransactionInputImpl> inputs, List<TransactionOutputImpl> outputs, long lockTime, byte[] hash)
//      throws BitcoinException
//   {
//      return new TransactionImpl(inputs, outputs, lockTime, hash);
//   }
//
//   public TransactionOutputImpl newTransactionOutputImpl(long value, byte[] scriptBytes)
//   {
//      return new TransactionOutputImpl(value, scriptFactory.createFragment(scriptBytes));
//   }
//
//   public TransactionInputImpl newTransactionInputImpl(byte[] referredTxHash, int referredTxIndex, byte[] scriptBytes, long sequence)
//   {
//      return new TransactionInputImpl(referredTxHash, referredTxIndex, scriptFactory.createFragment(scriptBytes), sequence);
//   }
//
//   public Block newBlock(List<TransactionImpl> transactions, long nTime, long nonce, long nBits,
//      byte[] prevBlockHash, byte[] hashMerkleRoot, byte[] hash) throws BitcoinException
//   {
//      return new BlockImpl(transactions, nTime, nonce, nBits, prevBlockHash, hashMerkleRoot, hash);
//   }
//
}
