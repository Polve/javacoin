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
import org.testng.Assert;
import hu.netmind.bitcoin.node.p2p.HexUtil;
import java.util.List;
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
      BlockImpl block = new BlockImpl(new ArrayList<Transaction>(), null,
            1310891749000l, 3999553309l, 436911055l, 
            HexUtil.toByteArray("19 49 7C 77 6F 0E FD 5A 01 8E 2D 8E 73 BC B1 E2 85 CD C9 A2 CD 52 30 BC CF 04 00 00 00 00 00 00"),
            HexUtil.toByteArray("AE 52 FE 5C EC D5 7E B1 BA 4C A0 A2 FD 1E A9 DE 4B 6E 08 46 3B 34 69 C1 82 7C 2F 67 7D BC 9D FE"));
      Assert.assertEquals(HexUtil.toHexString(block.getHash()),
            "00 AD 6A 68 55 F8 9C 0E EE 3A 6E EF DA 05 D5 DD DB C2 73 35 40 EA 7D 4C ED 00 00 00 00 00 00 00");
   }
}

