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

import hu.netmind.bitcoin.BitCoinException;
import java.math.BigInteger;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Test the difficulty calculations.
 * @author Robert Brautigam
 */
@Test
public class DifficultyTests
{
   static public BitcoinFactory prodnetFactory;
   static public BitcoinFactory testnet2Factory;
   
   static {
      try
      {
         prodnetFactory = new StandardBitcoinFactory(null);
         testnet2Factory = new Testnet2BitcoinFactory(null);
      } catch (BitCoinException ex)
      {
         Assert.fail("Cant create factories: "+ex.getMessage(),ex);
      }
   }
   
   public void testMaxTargetProdnet() throws BitCoinException
   {
      Assert.assertEquals(prodnetFactory.maxDifficultyTarget().getTarget(),
            new BigInteger("FFFF0000000000000000000000000000000000000000000000000000",16));
      Assert.assertEquals(
            Long.toHexString(prodnetFactory.maxDifficultyTarget().getCompressedTarget()),
            Long.toHexString(0x1d00ffffl));
   }

   public void testMaxTargetTestnet() throws BitCoinException
   {
      Assert.assertEquals(testnet2Factory.maxDifficultyTarget().getTarget(),
            new BigInteger("FFFFF0000000000000000000000000000000000000000000000000000",16));
      Assert.assertEquals(
            Long.toHexString(testnet2Factory.maxDifficultyTarget().getCompressedTarget()),
            //Long.toHexString(0x1d1fffe0L));
            Long.toHexString(0x1d0fffffl));
   }

   public void testTargetDecompression()
   {
      DifficultyTarget target = new DifficultyTarget(0x1b0404cbl);
      Assert.assertEquals(target.getTarget(),
            new BigInteger("404CB000000000000000000000000000000000000000000000000",16));
   }

   public void testTargetCompression()
   {
      DifficultyTarget target = new DifficultyTarget(
            new BigInteger("404CB000000000000000000000000000000000000000000000000",16));
      Assert.assertEquals(Long.toHexString(target.getCompressedTarget()),
            Long.toHexString(0x1b0404cbl));
   }

   public void testMaxDifficulty()
   {
      Difficulty difficulty = prodnetFactory.newDifficulty(
            new DifficultyTarget(
               new BigInteger("FFFF0000000000000000000000000000000000000000000000000000",16).toByteArray()));
      Assert.assertEquals(difficulty.getDifficulty().longValue(),1);
   }

   public void testSampleDifficulty()
   {
      Difficulty difficulty = prodnetFactory.newDifficulty(
            new DifficultyTarget(
               new BigInteger("404CB000000000000000000000000000000000000000000000000",16).toByteArray()));
      Assert.assertEquals(difficulty.getDifficulty().longValue(),16307);
   }

   public void testUncompressingMaxDifficulty()
   {
      Difficulty difficulty = prodnetFactory.newDifficulty(new DifficultyTarget(0x1d00ffffl));
      Assert.assertEquals(difficulty.getDifficulty().longValue(),1);
   }

   public void testUncompressingSampleDifficulty()
   {
      Difficulty difficulty = prodnetFactory.newDifficulty(new DifficultyTarget(0x1b0404cbl));
      Assert.assertEquals(difficulty.getDifficulty().longValue(),16307);
   }

   public void testAddDifficulties()
   {
      Difficulty difficulty = prodnetFactory.newDifficulty(new DifficultyTarget(0x1b0404cbl));
      Difficulty result = difficulty.add(prodnetFactory.newDifficulty(new DifficultyTarget(0x1b0404cbl)));
      Assert.assertEquals(result.getDifficulty().longValue(),16307*2);
   }

   public void testCompareDifficulties()
   {
      Assert.assertTrue(prodnetFactory.newDifficulty(new DifficultyTarget(0x1b0404cbl)).
            compareTo(prodnetFactory.newDifficulty(new DifficultyTarget(0x1d00ffffl))) > 0 );
   }

   public void testZeroDifficulty()
   {
      Assert.assertTrue(prodnetFactory.newDifficulty(new DifficultyTarget(0)).
            compareTo(prodnetFactory.newDifficulty(new DifficultyTarget(0x1d00ffffl))) < 0 );
   }

}

