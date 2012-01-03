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
import org.testng.annotations.Test;
import org.easymock.EasyMock;
import org.testng.Assert;
import hu.netmind.bitcoin.net.HexUtil;
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
      BlockImpl block = new BlockImpl(new ArrayList<Transaction>(),
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
      Transaction tx = EasyMock.createMock(Transaction.class);
      EasyMock.expect(tx.getInputs()).andReturn(new ArrayList<TransactionInput>()).anyTimes();
      EasyMock.expect(tx.getHash()).andReturn(new byte[] { 0 }).anyTimes();
      tx.validate();
      EasyMock.replay(tx);
      List<Transaction> transactions = new ArrayList<Transaction>();
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
      List<Transaction> transactions = new ArrayList<Transaction>();
      // Create block
      BlockImpl block = new BlockImpl(transactions,0,0,0,null,null,new byte[] { 0 });
      block.validate();
   }

   @Test(expectedExceptions=VerificationException.class)
   public void testFalseDifficulty()
      throws BitCoinException, VerificationException
   {
      // Construct block with minimal transactions, that is only a coinbase
      Transaction tx = EasyMock.createMock(Transaction.class);
      EasyMock.expect(tx.getInputs()).andReturn(new ArrayList<TransactionInput>()).anyTimes();
      EasyMock.expect(tx.getHash()).andReturn(new byte[] { 0 });
      tx.validate();
      EasyMock.replay(tx);
      List<Transaction> transactions = new ArrayList<Transaction>();
      transactions.add(tx);
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
      Transaction tx = EasyMock.createMock(Transaction.class);
      EasyMock.expect(tx.getInputs()).andReturn(new ArrayList<TransactionInput>()).anyTimes();
      EasyMock.expect(tx.getHash()).andReturn(new byte[] { 0 });
      tx.validate();
      EasyMock.expectLastCall().andThrow(new VerificationException("transaction failed validation (test)"));
      EasyMock.replay(tx);
      List<Transaction> transactions = new ArrayList<Transaction>();
      transactions.add(tx);
      // Create block
      MerkleTree mTree = new MerkleTree(transactions);
      BlockImpl block = new BlockImpl(transactions,0,0,0,mTree.getRoot(),
            new byte[] { 0 });
      block.validate();
   }

   @Test(expectedExceptions=VerificationException.class)
   public void testBlockWrongMerkleRoot()
      throws BitCoinException, VerificationException
   {
      // Construct block with minimal transactions, that is only a coinbase
      Transaction tx = EasyMock.createMock(Transaction.class);
      EasyMock.expect(tx.getInputs()).andReturn(new ArrayList<TransactionInput>()).anyTimes();
      EasyMock.expect(tx.getHash()).andReturn(new byte[] { 0 });
      tx.validate();
      EasyMock.replay(tx);
      List<Transaction> transactions = new ArrayList<Transaction>();
      transactions.add(tx);
      // Create block
      MerkleTree mTree = new MerkleTree(transactions);
      BlockImpl block = new BlockImpl(transactions,0,0,0,new byte[] { 0 },
            new byte[] { 0 });
      block.validate();
   }

   @Test(expectedExceptions=VerificationException.class)
   public void testSameOutputTwice()
      throws BitCoinException, VerificationException
   {
      // Create two inputs
      TransactionInput input1 = EasyMock.createMock(TransactionInput.class);
      EasyMock.expect(input1.getClaimedTransactionHash()).andReturn(new byte[] { 1 }).anyTimes();
      EasyMock.expect(input1.getClaimedOutputIndex()).andReturn(2).anyTimes();
      EasyMock.replay(input1);
      List<TransactionInput> inputs1 = new ArrayList<TransactionInput>();
      inputs1.add(input1);
      TransactionInput input2 = EasyMock.createMock(TransactionInput.class);
      EasyMock.expect(input2.getClaimedTransactionHash()).andReturn(new byte[] { 1 }).anyTimes();
      EasyMock.expect(input2.getClaimedOutputIndex()).andReturn(2).anyTimes();
      EasyMock.replay(input2);
      List<TransactionInput> inputs2 = new ArrayList<TransactionInput>();
      inputs2.add(input2);
      // Construct block with minimal transactions, that is only a coinbase
      Transaction tx1 = EasyMock.createMock(Transaction.class);
      EasyMock.expect(tx1.getInputs()).andReturn(inputs1).anyTimes();
      EasyMock.expect(tx1.getHash()).andReturn(new byte[] { 0 }).anyTimes();
      tx1.validate();
      EasyMock.replay(tx1);
      Transaction tx2 = EasyMock.createMock(Transaction.class);
      EasyMock.expect(tx2.getInputs()).andReturn(inputs1).anyTimes();
      EasyMock.expect(tx2.getHash()).andReturn(new byte[] { 0 }).anyTimes();
      EasyMock.expect(tx2.isCoinbase()).andReturn(false).anyTimes();
      tx2.validate();
      EasyMock.replay(tx2);
      List<Transaction> transactions = new ArrayList<Transaction>();
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
      // First copy over details so we can calculate hash
      BlockImpl genesis = BlockImpl.MAIN_GENESIS;
      BlockImpl copy = new BlockImpl(genesis.getTransactions(),
            genesis.getCreationTime(),genesis.getNonce(),genesis.getCompressedTarget(),
            genesis.getPreviousBlockHash(),genesis.getMerkleRoot());
      // First verify block consistency
      copy.validate();
      // Verify the hash of block to match its known value
      Assert.assertEquals(
            HexUtil.toByteArray("00 00 00 00 00 19 D6 68 9C 08 5A E1 65 83 1E 93 4F F7 63 AE 46 A2 A6 C1 72 B3 F1 B6 0A 8C E2 6F "),
            copy.getHash());
   }
}

