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
import hu.netmind.bitcoin.Constants;
import hu.netmind.bitcoin.ScriptFactory;
import it.nibbles.bitcoin.utils.BtcUtil;

/**
 *
 * @author Alessandro Polverini
 */
public class Testnet3BitcoinFactory extends ProdnetBitcoinFactory
{

   public Testnet3BitcoinFactory(ScriptFactory scriptFactory) throws BitcoinException
   {
      this.scriptFactory = scriptFactory;
      setNetworkParams(Constants.TESTNET3_MESSAGE_MAGIC, 1296688602000L, 414098458L, 0x1d00ffffL, BtcUtil.hexIn("000000000933ea01ad0ee984209779baaec3ced90fa3f408719526f8d77f4943"));
   }

   @Override
   public boolean isTestnet3()
   {
      return true;
   }
}
