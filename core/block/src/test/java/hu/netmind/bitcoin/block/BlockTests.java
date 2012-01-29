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

import hu.netmind.bitcoin.Transaction;
import hu.netmind.bitcoin.TransactionInput;
import hu.netmind.bitcoin.BitCoinException;
import hu.netmind.bitcoin.VerificationException;
import hu.netmind.bitcoin.ScriptFragment;
import hu.netmind.bitcoin.ScriptFactory;
import hu.netmind.bitcoin.ScriptException;
import hu.netmind.bitcoin.Script;
import org.testng.annotations.Test;
import java.io.IOException;
import org.easymock.EasyMock;
import org.testng.Assert;
import hu.netmind.bitcoin.net.HexUtil;
import java.util.Arrays;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

/**
 * @author Robert Brautigam
 */
@Test
public class BlockTests
{
   public void testBlockHashCalculation()
      throws BitCoinException
   {
      // This example is from the real block:
      // 00000000000000ed4c7dea403573c2dbddd505daef6e3aee0e9cf855686aad00
      BlockImpl block = new BlockImpl(new ArrayList<TransactionImpl>(),
            1310891749000l, 3999553309l, 436911055l, 
            HexUtil.toByteArray("00 00 00 00 00 00 04 CF BC 30 52 CD A2 C9 CD 85 E2 B1 BC 73 8E 2D 8E 01 5A FD 0E 6F 77 7C 49 19"),
            HexUtil.toByteArray("FE 9D BC 7D 67 2F 7C 82 C1 69 34 3B 46 08 6E 4B DE A9 1E FD A2 A0 4C BA B1 7E D5 EC 5C FE 52 AE"));
      Assert.assertEquals(HexUtil.toHexString(block.getHash()),
            "00 00 00 00 00 00 00 ED 4C 7D EA 40 35 73 C2 DB DD D5 05 DA EF 6E 3A EE 0E 9C F8 55 68 6A AD 00");
   }

   public void testValidBlock()
      throws BitCoinException, VerificationException
   {
      // Construct block with minimal transactions, that is only a coinbase
      TransactionImpl tx = new TransactionImpl(
            Arrays.asList(new TransactionInputImpl[] { new TransactionInputImpl(new byte[] {1}, 0, createMockScript(), 1) }),
            Arrays.asList(new TransactionOutputImpl[] { new TransactionOutputImpl(10,createMockScript()) }),
            0);
      List<TransactionImpl> transactions = new ArrayList<TransactionImpl>();
      transactions.add(tx);
      // Create block
      MerkleTree mTree = new MerkleTree(transactions);
      BlockImpl block = new BlockImpl(transactions,0,0,0,null,mTree.getRoot(),
            new byte[] { 0 });
      block.validate();
   }

   public void testEmptyTransactions()
      throws BitCoinException, VerificationException
   {
      // Construct block with minimal transactions, that is only a coinbase
      List<TransactionImpl> transactions = new ArrayList<TransactionImpl>();
      // Create block
      BlockImpl block = new BlockImpl(transactions,0,0,0,null,null,new byte[] { 0 });
      block.validate();
   }

   @Test(expectedExceptions=VerificationException.class)
   public void testFalseDifficulty()
      throws BitCoinException, VerificationException
   {
      // Construct block with minimal transactions, that is only a coinbase
      List<TransactionImpl> transactions = new ArrayList<TransactionImpl>();
      // Create block with false difficulty (hash is not as difficult as
      // claimed).
      MerkleTree tree = new MerkleTree(transactions);
      BlockImpl block = new BlockImpl(transactions,
            0,0,0x1b0404cbl,null,tree.getRoot(),
            HexUtil.toByteArray("00 00 00 00 00 04 04 DB 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00"));
      block.validate();
   }

   @Test(expectedExceptions=VerificationException.class)
   public void testBlockWithInvalidTransaction()
      throws BitCoinException, VerificationException
   {
      // Construct block with minimal transactions, that don't validate
      TransactionImpl tx = new TransactionImpl(
            Arrays.asList(new TransactionInputImpl[] { new TransactionInputImpl(new byte[] {1}, 0, createMockScript(), 1) }),
            Arrays.asList(new TransactionOutputImpl[] { }),
            0);
      List<TransactionImpl> transactions = new ArrayList<TransactionImpl>();
      transactions.add(tx);
      // Create block
      MerkleTree mTree = new MerkleTree(transactions);
      BlockImpl block = new BlockImpl(transactions,0,0,0,mTree.getRoot(),
            new byte[] { 0 }, new byte[] { 0 });
      block.validate();
   }

   @Test(expectedExceptions=VerificationException.class)
   public void testBlockWrongMerkleRoot()
      throws BitCoinException, VerificationException
   {
      // Construct block with minimal transactions, that is only a coinbase
      TransactionImpl tx = new TransactionImpl(
            Arrays.asList(new TransactionInputImpl[] { new TransactionInputImpl(new byte[] {1}, 0, createMockScript(), 1) }),
            Arrays.asList(new TransactionOutputImpl[] { new TransactionOutputImpl(10,createMockScript()) }),
            0);
      List<TransactionImpl> transactions = new ArrayList<TransactionImpl>();
      transactions.add(tx);
      // Create block
      MerkleTree mTree = new MerkleTree(transactions);
      BlockImpl block = new BlockImpl(transactions,0,0,0,new byte[] { 0 },
            new byte[] { 0 }, new byte[] { 0 });
      block.validate();
   }

   @Test(expectedExceptions=VerificationException.class)
   public void testSameOutputTwice()
      throws BitCoinException, VerificationException
   {
      TransactionImpl tx1 = new TransactionImpl(
            Arrays.asList(new TransactionInputImpl[] { new TransactionInputImpl(new byte[] {1,2,3}, 2, createMockScript(), 1) }),
            Arrays.asList(new TransactionOutputImpl[] { new TransactionOutputImpl(10,createMockScript()) }),
            0);
      TransactionImpl tx2 = new TransactionImpl(
            Arrays.asList(new TransactionInputImpl[] { new TransactionInputImpl(new byte[] {1,2,3}, 2, createMockScript(), 1) }),
            Arrays.asList(new TransactionOutputImpl[] { new TransactionOutputImpl(20,createMockScript()) }),
            0);
      List<TransactionImpl> transactions = new ArrayList<TransactionImpl>();
      transactions.add(tx1);
      transactions.add(tx2);
      // Create block
      MerkleTree mTree = new MerkleTree(transactions);
      BlockImpl block = new BlockImpl(transactions,0,0,0,null,mTree.getRoot(),
            new byte[] { 0 });
      block.validate();
   }

   public void testMainGenesisBlock()
      throws BitCoinException
   {
      BlockImpl genesis = BlockImpl.MAIN_GENESIS;
      byte[] hash = genesis.calculateHash();
      // Verify the hash of block to match its known value
      Assert.assertEquals(
            HexUtil.toByteArray("00 00 00 00 00 19 D6 68 9C 08 5A E1 65 83 1E 93 4F F7 63 AE 46 A2 A6 C1 72 B3 F1 B6 0A 8C E2 6F "),
            hash);
   }

   public void testConvertToMessageAndBack()
      throws BitCoinException, IOException
   {
      // Construct a non-trivial valid block
      TransactionImpl tx1 = new TransactionImpl(
            Arrays.asList(new TransactionInputImpl[] { 
               new TransactionInputImpl(new byte[] {1,2,3}, 0, createMockScript(), 1),
               new TransactionInputImpl(new byte[] {4,5,6}, 1, createMockScript(), 1),
               new TransactionInputImpl(new byte[] {5,6,7}, 0, createMockScript(), 1),
            }),
            Arrays.asList(new TransactionOutputImpl[] { 
               new TransactionOutputImpl(5,createMockScript()),
               new TransactionOutputImpl(10,createMockScript()),
            }),
            0);
      TransactionImpl tx2 = new TransactionImpl(
            Arrays.asList(new TransactionInputImpl[] { 
               new TransactionInputImpl(new byte[] {1,6,3}, 2, createMockScript(), 1),
               new TransactionInputImpl(new byte[] {4,2,6}, 3, createMockScript(), 1),
            }),
            Arrays.asList(new TransactionOutputImpl[] { new TransactionOutputImpl(10,createMockScript()) }),
            0);
      List<TransactionImpl> transactions = new ArrayList<TransactionImpl>();
      transactions.add(tx1);
      transactions.add(tx2);
      MerkleTree tree = new MerkleTree(transactions);
      BlockImpl block = new BlockImpl(transactions,0,0,0,new byte[] { 0 }, tree.getRoot());
      // Convert and convert back
      BlockImpl backBlock = BlockImpl.createBlock(createMockScriptFactory(),block.createBlockMessage(123));
      // Assert that the block that came back has exactly the same data
      Assert.assertEquals(block.getHash(),backBlock.getHash());
   }

   private ScriptFactory createMockScriptFactory()
   {
      return new ScriptFactory() {
         public ScriptFragment createFragment(byte[] script)
         {
            return createMockScript(script);
         }
         public Script createScript(ScriptFragment sigScript, ScriptFragment pubScript)
         {
            return null;
         }
      };
   }

   private ScriptFragment createMockScript()
   {
      return createMockScript(new byte[] {
               0,1,2,3,4,5,6,7,8,9,
               10,11,12,13,14,15,16,17,18,19,
               20,21,22,23,24,25,26,27,28,29,
               30,31,32,33,34,35,63,37,38,39 
            });
   }

   private ScriptFragment createMockScript(byte[] scriptBytes)
   {
      try
      {
         ScriptFragment script = EasyMock.createMock(ScriptFragment.class);
         EasyMock.expect(script.toByteArray()).andReturn(scriptBytes).anyTimes();
         EasyMock.expect(script.isComputationallyExpensive()).andReturn(false).anyTimes();
         EasyMock.replay(script);
         return script;
      } catch ( ScriptException e ) {
         throw new RuntimeException("could not create script mock",e);
      }
   }

}

