/**
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
package it.nibbles.javacoin.storage;

import hu.netmind.bitcoin.block.BlockChainLink;
import hu.netmind.bitcoin.net.HexUtil;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;

/**
 *
 * @author Alessandro Polverini
 */
public class SimplifiedStoredBlock implements Comparable<SimplifiedStoredBlock>
{

   public byte[] hash;
   public byte[] prevBlockHash;
   public int height;
   //public BigDecimal chainWork;

   public SimplifiedStoredBlock(BlockChainLink b)
   {
      hash = b.getBlock().getHash();
      prevBlockHash = b.getBlock().getPreviousBlockHash();
      height = b.getHeight();
      //chainWork = b.getTotalDifficulty().getDifficulty();
   }

   public SimplifiedStoredBlock(ResultSet rs) throws SQLException
   {
      hash = rs.getBytes("hash");
      prevBlockHash = rs.getBytes("prevBlockHash");
      height = rs.getInt("height");
      //chainWork = new BigInteger(rs.getString("chainWork"));
   }

   // We sort blocks based on their height
   @Override
   public int compareTo(SimplifiedStoredBlock o)
   {
      if (height > o.height)
         return -1;
      if (height < o.height)
         return 1;
      return o.hash.toString().compareTo(hash.toString());
   }

   @Override
   public boolean equals(Object obj)
   {
      if (!(obj instanceof SimplifiedStoredBlock))
         return false;
      return Arrays.equals(hash, ((SimplifiedStoredBlock) obj).hash);
   }

   @Override
   public int hashCode()
   {
      return Arrays.hashCode(this.hash);
   }

   @Override
   public String toString()
   {
      return "B[hash: " + HexUtil.toSingleHexString(hash) + " prev: " + HexUtil.toSingleHexString(prevBlockHash) + " h: " + height + "]";
   }
}
