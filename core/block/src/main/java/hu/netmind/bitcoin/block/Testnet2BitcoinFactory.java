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
import hu.netmind.bitcoin.ScriptFactory;
import it.nibbles.bitcoin.utils.BtcUtil;
import java.math.BigInteger;

/**
 *
 * @author Alessandro Polverini
 */
public class Testnet2BitcoinFactory extends StandardBitcoinFactory
{
   public static final DifficultyTarget MAX_TESTNET_TARGET =
      new DifficultyTarget(new BigInteger("FFFFF0000000000000000000000000000000000000000000000000000", 16));

   public Testnet2BitcoinFactory(ScriptFactory scriptFactory) throws BitcoinException
   {
      this.scriptFactory = scriptFactory;
      setNetworkParams(0xfabfb5daL, 1296688602000l, 384568319l, 0x1d07fff8L, BtcUtil.hexIn("00000007199508e34a9ff81e6ec0c477a4cccff2a4767a8eee39c11db367b008"));
   }

   @Override
   public boolean isTestnet2()
   {
      return true;
   }

   @Override
   public DifficultyTarget maxDifficultyTarget()
   {
      return MAX_TESTNET_TARGET;
   }
}
