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

import hu.netmind.bitcoin.Block;
import hu.netmind.bitcoin.ScriptFactory;
import hu.netmind.bitcoin.net.NetworkMessageFactory;
import java.math.BigDecimal;

/**
 *
 * @author Alessandro Polverini
 */
public interface BitcoinFactory
{

   public ScriptFactory getScriptFactory();

   public NetworkMessageFactory getMessageFactory();

   public long getMessageMagic();

   public BlockChainLink newBlockChainLink(Block block, BigDecimal chainWork, long height);

   /**
    * Construct with no difficulty.
    */
   public Difficulty newDifficulty();

   public Difficulty newDifficulty(BigDecimal difficulty);

   public Difficulty newDifficulty(DifficultyTarget target);

   public DifficultyTarget maxDifficultyTarget();

   public boolean isTestnet2();

   public boolean isTestnet3();

   public Block getGenesisBlock();

   public Difficulty getGenesisDifficulty();
}
