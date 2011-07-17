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
import hu.netmind.bitcoin.TransactionFilter;
import hu.netmind.bitcoin.BitCoinException;
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
            reverse(HexUtil.toByteArray("F0 3B 56 C4 DF CA 0E 4D 53 B0 7C D4 7C B5 8F C4 75 66 58 12 39 68 80 63 D3 D4 A0 37 CC AB C3 14 ")));
      EasyMock.replay(tx1);
      List<Transaction> transactions = new ArrayList<Transaction>();
      transactions.add(tx1);
      BlockImpl block = new BlockImpl(transactions, null,
            1310891749000l, 3999553309l, 436911055l, 
            HexUtil.toByteArray("19 49 7C 77 6F 0E FD 5A 01 8E 2D 8E 73 BC B1 E2 85 CD C9 A2 CD 52 30 BC CF 04 00 00 00 00 00 00"),
            HexUtil.toByteArray("AE 52 FE 5C EC D5 7E B1 BA 4C A0 A2 FD 1E A9 DE 4B 6E 08 46 3B 34 69 C1 82 7C 2F 67 7D BC 9D FE"));
      Assert.assertEquals(HexUtil.toHexString(block.getHash()),
            "00 AD 6A 68 55 F8 9C 0E EE 3A 6E EF DA 05 D5 DD DB C2 73 35 40 EA 7D 4C ED 00 00 00 00 00 00 00");
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
            reverse(HexUtil.toByteArray("F0 3B 56 C4 DF CA 0E 4D 53 B0 7C D4 7C B5 8F C4 75 66 58 12 39 68 80 63 D3 D4 A0 37 CC AB C3 14 ")));
      EasyMock.replay(tx1);
      Transaction tx2 = EasyMock.createMock(Transaction.class);
      EasyMock.expect(tx2.getHash()).andReturn(
            reverse(HexUtil.toByteArray("C5 D1 11 7E A2 2E 83 15 54 E4 39 4F E8 E1 59 16 1C 6D 01 D0 A5 D7 6A B7 5B BD 90 5E D4 C6 7A 54 ")));
      EasyMock.replay(tx2);
      Transaction tx3 = EasyMock.createMock(Transaction.class);
      EasyMock.expect(tx3.getHash()).andReturn(
            reverse(HexUtil.toByteArray("0D 7D 45 88 E6 69 21 E5 79 5D 41 1C A1 43 1C 07 8C CC D3 16 03 0C 06 74 C6 F8 0F DB 82 D6 DB ED ")));
      EasyMock.replay(tx3);
      Transaction tx4 = EasyMock.createMock(Transaction.class);
      EasyMock.expect(tx4.getHash()).andReturn(
            reverse(HexUtil.toByteArray("FD A5 B0 A6 7F C3 AE D0 53 EC C3 65 CB 31 77 02 BA 0C 74 EA AB EF 8B 84 3C A0 27 E0 2A 1D 50 35 ")));
      EasyMock.replay(tx4);
      Transaction tx5 = EasyMock.createMock(Transaction.class);
      EasyMock.expect(tx5.getHash()).andReturn(
            reverse(HexUtil.toByteArray("29 88 48 31 F9 C8 8B C0 49 7F 41 9E 7E 79 AF FA 91 B9 40 82 48 3C 0B 72 81 F3 A6 05 CF 32 A9 AD ")));
      EasyMock.replay(tx5);
      List<Transaction> transactions = new ArrayList<Transaction>();
      transactions.add(tx1);
      transactions.add(tx2);
      transactions.add(tx3);
      transactions.add(tx4);
      transactions.add(tx5);
      // Setup block (so that it does not calculate normal hash)
      BlockImpl block = new BlockImpl(transactions,null,0,0,0,null,null,null,new byte[] {});
      // Check merkle root
      Assert.assertEquals(HexUtil.toHexString(block.getMerkleTree().getRoot()),
            "3C 5A 5B E5 ED DF 0F 25 91 19 E8 4E 4F 36 9C 4E B5 D3 26 C0 BB F4 ED 2E 68 F5 EE EC C8 AC C0 4A");
   }

   public void testBlockInitialPrefiltering()
      throws BitCoinException
   {
      // Create some transactions
      Transaction tx1 = EasyMock.createMock(Transaction.class);
      EasyMock.expect(tx1.getHash()).andReturn(
            reverse(HexUtil.toByteArray("F0 3B 56 C4 DF CA 0E 4D 53 B0 7C D4 7C B5 8F C4 75 66 58 12 39 68 80 63 D3 D4 A0 37 CC AB C3 14"))).anyTimes();
      EasyMock.replay(tx1);
      Transaction tx2 = EasyMock.createMock(Transaction.class);
      EasyMock.expect(tx2.getHash()).andReturn(
            reverse(HexUtil.toByteArray("C5 D1 11 7E A2 2E 83 15 54 E4 39 4F E8 E1 59 16 1C 6D 01 D0 A5 D7 6A B7 5B BD 90 5E D4 C6 7A 54"))).anyTimes();
      EasyMock.replay(tx2);
      Transaction tx3 = EasyMock.createMock(Transaction.class);
      EasyMock.expect(tx3.getHash()).andReturn(
            reverse(HexUtil.toByteArray("0D 7D 45 88 E6 69 21 E5 79 5D 41 1C A1 43 1C 07 8C CC D3 16 03 0C 06 74 C6 F8 0F DB 82 D6 DB ED"))).anyTimes();
      EasyMock.replay(tx3);
      List<Transaction> transactions = new ArrayList<Transaction>();
      transactions.add(tx1);
      transactions.add(tx2);
      transactions.add(tx3);
      // Setup block with filtering out hashes starting with "C5"
      BlockImpl block = new BlockImpl(transactions,
            new TransactionFilter() 
            {
               public int compareTo(TransactionFilter filter)
               {
                  throw new UnsupportedOperationException("not implemented");
               }
               public void filterTransactions(List<Transaction> transactions)
               {
                  Iterator<Transaction> txIterator = transactions.iterator();
                  while ( txIterator.hasNext() )
                  {
                     Transaction tx = txIterator.next();
                     if ( (tx.getHash()[0] & 0xff) == 0x54 )
                        txIterator.remove();
                  }
               }
            }
            ,0,0,0,null,null,null,new byte[] {});
      // Determine whether that transaction was filtered out
      Assert.assertEquals(block.getStoredTransactions().size(),2);
   }

   private byte[] reverse(byte[] byteArray)
   {
      byte[] result = new byte[byteArray.length];
      for ( int i=0; i<byteArray.length; i++ )
         result[i]=byteArray[byteArray.length-1-i];
      return result;
   }
}

