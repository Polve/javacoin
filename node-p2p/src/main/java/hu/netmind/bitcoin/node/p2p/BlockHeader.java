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

package hu.netmind.bitcoin.node.p2p;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

/**
 * @author Robert Brautigam
 */
public class BlockHeader
{
   private long version;
   private byte[] prevBlock;
   private byte[] rootHash;
   private long timestamp;
   private long difficulty;
   private long nonce;

   public BlockHeader(long version, byte[] prevBlock, byte[] rootHash, long timestamp,
         long difficulty, long nonce)
   {
      this.version=version;
      this.prevBlock=prevBlock;
      this.rootHash=rootHash;
      this.timestamp=timestamp;
      this.difficulty=difficulty;
      this.nonce=nonce;
   }

   BlockHeader()
   {
   }

   void readFrom(BitCoinInputStream input, long protocolVersion, Object param)
      throws IOException
   {
      version = input.readUInt32();
      prevBlock = input.readBytes(32);
      rootHash = input.readBytes(32);
      timestamp = input.readUInt32()*1000;
      difficulty = input.readUInt32();
      nonce = input.readUInt32();
   }

   void writeTo(BitCoinOutputStream output, long protocolVersion)
      throws IOException
   {
      output.writeUInt32(version);
      output.write(prevBlock);
      output.write(rootHash);
      output.writeUInt32(timestamp/1000);
      output.writeUInt32(difficulty);
      output.writeUInt32(nonce);
   }

   public String toString()
   {
      return "version: "+version+", prevBlock: "+HexUtil.toHexString(prevBlock)+
         ", root hash: "+HexUtil.toHexString(rootHash)+", timestamp: "+timestamp+", difficulty: "+difficulty+
         ", nonce: "+nonce;
   }

   public long getVersion()
   {
      return version;
   }

   public byte[] getPrevBlock()
   {
      return prevBlock;
   }

   public byte[] getRootHash()
   {
      return rootHash;
   }

   public long getTimestamp()
   {
      return timestamp;
   }

   public long getDifficulty()
   {
      return difficulty;
   }

   public long getNonce()
   {
      return nonce;
   }

}

