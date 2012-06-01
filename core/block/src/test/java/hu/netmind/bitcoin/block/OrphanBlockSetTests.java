/**
 * Copyright (C) 2011 NetMind Consulting Bt.
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

import hu.netmind.bitcoin.Transaction;
import hu.netmind.bitcoin.TransactionInput;
import hu.netmind.bitcoin.BitCoinException;
import hu.netmind.bitcoin.Block;
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
import java.util.LinkedList;

/**
 * @author Alessandro Polverini
 */
@Test
public class OrphanBlockSetTests
{

   public void testEmptySet()
      throws BitCoinException
   {
      OrphanBlocksSet set = new OrphanBlocksSet();
      Assert.assertNull(set.removeBlockByPreviousHash(new byte[]
         {
            3, 2
         }));
   }

   public void test2()
      throws BitCoinException
   {
      byte[] hash = new byte[]
      {
         33, 22
      };
      byte[] hash1 = new byte[]
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
      Block b1 = new BlockImpl(txs, 0, 0, 0, hash, null, hash1);
      Block b2 = new BlockImpl(txs, 0, 0, 0, hash, null, hash2);
      Block b3 = new BlockImpl(txs, 0, 0, 0, hash4, null, hash3);
      OrphanBlocksSet set = new OrphanBlocksSet();

      // Check that a single block is correctly retrieved and deleted from the set
      set.addBlock(b1);
      Assert.assertNull(set.removeBlockByPreviousHash(hash1));
      Block b = set.removeBlockByPreviousHash(hash);
      Assert.assertTrue(b == null || b == b1);
      Assert.assertNull(set.removeBlockByPreviousHash(hash));

      // Check that two blocks with same previous hash are correctly retrieved
      set = new OrphanBlocksSet();
      set.addBlock(b1);
      set.addBlock(b2);
      Block r1 = set.removeBlockByPreviousHash(hash);
      Block r2 = set.removeBlockByPreviousHash(hash);
      Assert.assertTrue((r1 == null && r2 == null)
         || (r1 == null && (r2 == b1 || r2 == b2))
         || ((r1 == b1 || r1 == b2) && r2 == null)
         || (r1 == b1 && r2 == b2)
         || (r2 == b1 && r1 == b2));
      Assert.assertNull(set.removeBlockByPreviousHash(hash));

      set = new OrphanBlocksSet();
      set.addBlock(b1);
      set.addBlock(b3);
      set.addBlock(b2);
      b = set.removeBlockByPreviousHash(hash4);
      Assert.assertTrue(b == null || b == b3);
      Assert.assertNull(set.removeBlockByPreviousHash(hash4));
   }
}
