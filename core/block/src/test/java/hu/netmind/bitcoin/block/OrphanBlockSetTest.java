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
package hu.netmind.bitcoin.block;

import hu.netmind.bitcoin.BitcoinException;
import hu.netmind.bitcoin.Block;
import java.util.LinkedList;
import java.util.List;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author Alessandro Polverini
 */
@Test
public class OrphanBlockSetTest
{

   public void testEmptySet()
      throws BitcoinException
   {
      OrphanBlockSet set = new OrphanBlockSet();
      Assert.assertNull(set.removeBlockByPreviousHash(new byte[]
         {
            3, 2
         }));
   }

   public void test2()
      throws BitcoinException
   {
      byte[] hash0 = new byte[]
      {
         33, 22
      };
      byte[] hash0Copy = new byte[]
      {
         33, 22
      };
      byte[] hash1 = new byte[]
      {
         33, 34
      };
      byte[] hash1Copy = new byte[]
      {
         33, 34
      };
      byte[] hash2 = new byte[]
      {
         44, 22
      };
      byte[] hash3 = new byte[]
      {
         4, 5
      };
      byte[] hash4 = new byte[]
      {
         4, 5
      };
      List<TransactionImpl> txs = new LinkedList<>();
      Block b1 = new BlockImpl(txs, 0, 0, 0, hash0, null, hash1);
      Block b2 = new BlockImpl(txs, 0, 0, 0, hash0, null, hash2);
      Block b3 = new BlockImpl(txs, 0, 0, 0, hash4, null, hash3);
      OrphanBlockSet set = new OrphanBlockSet();

      // Check that a single block is correctly retrieved and deleted from the set
      set.addBlock(b1);
      Assert.assertNull(set.removeBlockByPreviousHash(hash1));

      Block r1 = set.removeBlockByPreviousHash(hash0Copy);
      Block r2 = set.removeBlockByPreviousHash(hash0);

      // Remember we have a weak cache so it's ok that we are unable to retrieve
      // an item just inserted.
      // But it must not happen to be able to retrieve an item that previously
      // failed to be retrieved: we use that property to check that we implemented
      // correctly the hash comparison (normal Maps uses identity for equals)
      Assert.assertNull(r2);

      Assert.assertTrue((r1 == null && r2 == null)
         || b1.equals(r1));

      // Check that two blocks with same previous hash are correctly retrieved
      set = new OrphanBlockSet();
      set.addBlock(b1);
      set.addBlock(b2);
      r1 = set.removeBlockByPreviousHash(hash0);
      r2 = set.removeBlockByPreviousHash(hash0Copy);
      Assert.assertTrue((r1 == null && r2 == null)
         || (r1 == null && (r2 == b1 || r2 == b2))
         || ((r1 == b1 || r1 == b2) && r2 == null)
         || (r1 == b1 && r2 == b2)
         || (r2 == b1 && r1 == b2));
      Assert.assertNull(set.removeBlockByPreviousHash(hash0));

      set = new OrphanBlockSet();
      set.addBlock(b1);
      set.addBlock(b3);
      set.addBlock(b2);
      Block b = set.removeBlockByPreviousHash(hash4);
      Assert.assertTrue(b == null || b == b3);
      Assert.assertNull(set.removeBlockByPreviousHash(hash4));
   }
}
