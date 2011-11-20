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
import hu.netmind.bitcoin.node.p2p.HexUtil;
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
      Transaction tx1 = EasyMock.createMock(Transaction.class);
      EasyMock.expect(tx1.getHash()).andReturn(
            MerkleTreeTests.reverse(HexUtil.toByteArray("F0 3B 56 C4 DF CA 0E 4D 53 B0 7C D4 7C B5 8F C4 75 66 58 12 39 68 80 63 D3 D4 A0 37 CC AB C3 14 ")));
      EasyMock.replay(tx1);
      List<Transaction> transactions = new ArrayList<Transaction>();
      transactions.add(tx1);
      BlockImpl block = new BlockImpl(transactions,
            1310891749000l, 3999553309l, 436911055l, 
            HexUtil.toByteArray("19 49 7C 77 6F 0E FD 5A 01 8E 2D 8E 73 BC B1 E2 85 CD C9 A2 CD 52 30 BC CF 04 00 00 00 00 00 00"),
            HexUtil.toByteArray("AE 52 FE 5C EC D5 7E B1 BA 4C A0 A2 FD 1E A9 DE 4B 6E 08 46 3B 34 69 C1 82 7C 2F 67 7D BC 9D FE"));
      Assert.assertEquals(HexUtil.toHexString(block.getHash()),
            "00 AD 6A 68 55 F8 9C 0E EE 3A 6E EF DA 05 D5 DD DB C2 73 35 40 EA 7D 4C ED 00 00 00 00 00 00 00");
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

}

