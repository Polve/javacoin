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
import hu.netmind.bitcoin.BitCoinException;
import org.testng.annotations.Test;
import org.easymock.EasyMock;
import org.testng.Assert;
import hu.netmind.bitcoin.node.p2p.HexUtil;
import java.util.List;
import java.util.ArrayList;

/**
 * @author Robert Brautigam
 */
@Test
public class MerkleTreeTests
{
   public void testSingleTransactionMerkleRoot()
      throws BitCoinException
   {
      Transaction tx1 = EasyMock.createMock(Transaction.class);
      EasyMock.expect(tx1.getHash()).andReturn(
            HexUtil.toByteArray("F0 3B 56 C4 DF CA 0E 4D 53 B0 7C D4 7C B5 8F C4 75 66 58 12 39 68 80 63 D3 D4 A0 37 CC AB C3 14 "));
      EasyMock.replay(tx1);
      List<Transaction> transactions = new ArrayList<Transaction>();
      transactions.add(tx1);
      // Check merkle root
      Assert.assertEquals(HexUtil.toHexString(new MerkleTree(transactions).getRoot()),
            "F0 3B 56 C4 DF CA 0E 4D 53 B0 7C D4 7C B5 8F C4 75 66 58 12 39 68 80 63 D3 D4 A0 37 CC AB C3 14");
   }

   public void testBlockMerkleRootCalculation()
      throws BitCoinException
   {
      // This is from the real block (with 5 transactions):
      // 0000000000000426bbdb6dd53d4477009de06cd3264178f724e6fff2a9768c65

      // Setup transaction hashes
      // Note we reverse the hashes only because they are copied from their "number" form
      // from the block explorer.
      Transaction tx1 = EasyMock.createMock(Transaction.class);
      EasyMock.expect(tx1.getHash()).andReturn(
            HexUtil.toByteArray("F0 3B 56 C4 DF CA 0E 4D 53 B0 7C D4 7C B5 8F C4 75 66 58 12 39 68 80 63 D3 D4 A0 37 CC AB C3 14 "));
      EasyMock.replay(tx1);
      Transaction tx2 = EasyMock.createMock(Transaction.class);
      EasyMock.expect(tx2.getHash()).andReturn(
            HexUtil.toByteArray("C5 D1 11 7E A2 2E 83 15 54 E4 39 4F E8 E1 59 16 1C 6D 01 D0 A5 D7 6A B7 5B BD 90 5E D4 C6 7A 54 "));
      EasyMock.replay(tx2);
      Transaction tx3 = EasyMock.createMock(Transaction.class);
      EasyMock.expect(tx3.getHash()).andReturn(
            HexUtil.toByteArray("0D 7D 45 88 E6 69 21 E5 79 5D 41 1C A1 43 1C 07 8C CC D3 16 03 0C 06 74 C6 F8 0F DB 82 D6 DB ED "));
      EasyMock.replay(tx3);
      Transaction tx4 = EasyMock.createMock(Transaction.class);
      EasyMock.expect(tx4.getHash()).andReturn(
            HexUtil.toByteArray("FD A5 B0 A6 7F C3 AE D0 53 EC C3 65 CB 31 77 02 BA 0C 74 EA AB EF 8B 84 3C A0 27 E0 2A 1D 50 35 "));
      EasyMock.replay(tx4);
      Transaction tx5 = EasyMock.createMock(Transaction.class);
      EasyMock.expect(tx5.getHash()).andReturn(
            HexUtil.toByteArray("29 88 48 31 F9 C8 8B C0 49 7F 41 9E 7E 79 AF FA 91 B9 40 82 48 3C 0B 72 81 F3 A6 05 CF 32 A9 AD "));
      EasyMock.replay(tx5);
      List<Transaction> transactions = new ArrayList<Transaction>();
      transactions.add(tx1);
      transactions.add(tx2);
      transactions.add(tx3);
      transactions.add(tx4);
      transactions.add(tx5);
      // Check merkle root
      Assert.assertEquals(HexUtil.toHexString(new MerkleTree(transactions).getRoot()),
            "4A C0 AC C8 EC EE F5 68 2E ED F4 BB C0 26 D3 B5 4E 9C 36 4F 4E E8 19 91 25 0F DF ED E5 5B 5A 3C");
   }

   public void testMerkleRootAllTransactionsRemoved()
      throws BitCoinException
   {
      Transaction tx1 = EasyMock.createMock(Transaction.class);
      EasyMock.expect(tx1.getHash()).andReturn(
            HexUtil.toByteArray("F0 3B 56 C4 DF CA 0E 4D 53 B0 7C D4 7C B5 8F C4 75 66 58 12 39 68 80 63 D3 D4 A0 37 CC AB C3 14 ")).anyTimes();
      EasyMock.replay(tx1);
      Transaction tx2 = EasyMock.createMock(Transaction.class);
      EasyMock.expect(tx2.getHash()).andReturn(
            HexUtil.toByteArray("C5 D1 11 7E A2 2E 83 15 54 E4 39 4F E8 E1 59 16 1C 6D 01 D0 A5 D7 6A B7 5B BD 90 5E D4 C6 7A 54 ")).anyTimes();
      EasyMock.replay(tx2);
      Transaction tx3 = EasyMock.createMock(Transaction.class);
      EasyMock.expect(tx3.getHash()).andReturn(
            HexUtil.toByteArray("0D 7D 45 88 E6 69 21 E5 79 5D 41 1C A1 43 1C 07 8C CC D3 16 03 0C 06 74 C6 F8 0F DB 82 D6 DB ED ")).anyTimes();
      EasyMock.replay(tx3);
      Transaction tx4 = EasyMock.createMock(Transaction.class);
      EasyMock.expect(tx4.getHash()).andReturn(
            HexUtil.toByteArray("FD A5 B0 A6 7F C3 AE D0 53 EC C3 65 CB 31 77 02 BA 0C 74 EA AB EF 8B 84 3C A0 27 E0 2A 1D 50 35 ")).anyTimes();
      EasyMock.replay(tx4);
      Transaction tx5 = EasyMock.createMock(Transaction.class);
      EasyMock.expect(tx5.getHash()).andReturn(
            HexUtil.toByteArray("29 88 48 31 F9 C8 8B C0 49 7F 41 9E 7E 79 AF FA 91 B9 40 82 48 3C 0B 72 81 F3 A6 05 CF 32 A9 AD ")).anyTimes();
      EasyMock.replay(tx5);
      List<Transaction> transactions = new ArrayList<Transaction>();
      transactions.add(tx1);
      transactions.add(tx2);
      transactions.add(tx3);
      transactions.add(tx4);
      transactions.add(tx5);
      // Create merkle tree
      MerkleTree tree = new MerkleTree(transactions);
      Assert.assertEquals(HexUtil.toHexString(tree.getRoot()),
            "4A C0 AC C8 EC EE F5 68 2E ED F4 BB C0 26 D3 B5 4E 9C 36 4F 4E E8 19 91 25 0F DF ED E5 5B 5A 3C");
      // Remove all transactions
      tree.removeTransaction(tx1);
      tree.removeTransaction(tx2);
      tree.removeTransaction(tx3);
      tree.removeTransaction(tx4);
      tree.removeTransaction(tx5);
      // Check that the outer nodes should now contain only one item (the root)
      Assert.assertEquals(tree.getOuterNodes().size(),1);
      // Construct back the merkle tree with no transactions but the root
      MerkleTree treeReconstruction = new MerkleTree(tree.getOuterNodes(),new ArrayList<Transaction>());
      Assert.assertEquals(HexUtil.toHexString(treeReconstruction.getRoot()),
            "4A C0 AC C8 EC EE F5 68 2E ED F4 BB C0 26 D3 B5 4E 9C 36 4F 4E E8 19 91 25 0F DF ED E5 5B 5A 3C");
   }

   public void testMerkleRootPartialReconstruction()
      throws BitCoinException
   {
      Transaction tx1 = EasyMock.createMock(Transaction.class);
      EasyMock.expect(tx1.getHash()).andReturn(
            HexUtil.toByteArray("F0 3B 56 C4 DF CA 0E 4D 53 B0 7C D4 7C B5 8F C4 75 66 58 12 39 68 80 63 D3 D4 A0 37 CC AB C3 14 ")).anyTimes();
      EasyMock.replay(tx1);
      Transaction tx2 = EasyMock.createMock(Transaction.class);
      EasyMock.expect(tx2.getHash()).andReturn(
            HexUtil.toByteArray("C5 D1 11 7E A2 2E 83 15 54 E4 39 4F E8 E1 59 16 1C 6D 01 D0 A5 D7 6A B7 5B BD 90 5E D4 C6 7A 54 ")).anyTimes();
      EasyMock.replay(tx2);
      Transaction tx3 = EasyMock.createMock(Transaction.class);
      EasyMock.expect(tx3.getHash()).andReturn(
            HexUtil.toByteArray("0D 7D 45 88 E6 69 21 E5 79 5D 41 1C A1 43 1C 07 8C CC D3 16 03 0C 06 74 C6 F8 0F DB 82 D6 DB ED ")).anyTimes();
      EasyMock.replay(tx3);
      Transaction tx4 = EasyMock.createMock(Transaction.class);
      EasyMock.expect(tx4.getHash()).andReturn(
            HexUtil.toByteArray("FD A5 B0 A6 7F C3 AE D0 53 EC C3 65 CB 31 77 02 BA 0C 74 EA AB EF 8B 84 3C A0 27 E0 2A 1D 50 35 ")).anyTimes();
      EasyMock.replay(tx4);
      Transaction tx5 = EasyMock.createMock(Transaction.class);
      EasyMock.expect(tx5.getHash()).andReturn(
            HexUtil.toByteArray("29 88 48 31 F9 C8 8B C0 49 7F 41 9E 7E 79 AF FA 91 B9 40 82 48 3C 0B 72 81 F3 A6 05 CF 32 A9 AD ")).anyTimes();
      EasyMock.replay(tx5);
      List<Transaction> transactions = new ArrayList<Transaction>();
      transactions.add(tx1);
      transactions.add(tx2);
      transactions.add(tx3);
      transactions.add(tx4);
      transactions.add(tx5);
      // Create merkle tree
      MerkleTree tree = new MerkleTree(transactions);
      Assert.assertEquals(HexUtil.toHexString(tree.getRoot()),
            "4A C0 AC C8 EC EE F5 68 2E ED F4 BB C0 26 D3 B5 4E 9C 36 4F 4E E8 19 91 25 0F DF ED E5 5B 5A 3C");
      // Remove 2 transactions from the beginning
      tree.removeTransaction(tx1);
      tree.removeTransaction(tx2);
      // Check that the outer nodes should now contain only one item (the parent of the 2)
      Assert.assertEquals(tree.getOuterNodes().size(),1);
      // Construct back the merkle tree with no transactions but the root
      List<Transaction> transactionsReconstruction = new ArrayList<Transaction>();
      transactionsReconstruction.add(tx3);
      transactionsReconstruction.add(tx4);
      transactionsReconstruction.add(tx5);
      MerkleTree treeReconstruction = new MerkleTree(tree.getOuterNodes(),transactionsReconstruction);
      Assert.assertEquals(HexUtil.toHexString(treeReconstruction.getRoot()),
            "4A C0 AC C8 EC EE F5 68 2E ED F4 BB C0 26 D3 B5 4E 9C 36 4F 4E E8 19 91 25 0F DF ED E5 5B 5A 3C");
   }

}

