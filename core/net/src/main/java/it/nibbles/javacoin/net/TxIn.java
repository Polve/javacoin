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

package it.nibbles.javacoin.net;

import it.nibbles.javacoin.utils.BtcUtil;
import java.io.IOException;

/**
 * @author Robert Brautigam
 */
public class TxIn
{
   private byte[] referencedTxHash;
   private long referencedTxOutIndex;
   private byte[] signatureScript;
   private long sequence;

   public TxIn(byte[] referencedTxHash, long referencedTxOutIndex,
         byte[] signatureScript, long sequence)
   {
      this.referencedTxHash=referencedTxHash;
      this.referencedTxOutIndex=referencedTxOutIndex;
      this.signatureScript=signatureScript;
      this.sequence=sequence;
   }

   TxIn()
   {
   }

   void readFrom(BitcoinInputStream input)
      throws IOException
   {
      referencedTxHash = input.readReverseBytes(32);
      referencedTxOutIndex = input.readUInt32();
      long scriptLength = input.readUIntVar();
      if ( scriptLength >= Integer.MAX_VALUE )
         throw new IOException("signature script too large: "+scriptLength);
      signatureScript = input.readBytes((int) scriptLength);
      sequence = input.readUInt32();
   }

   void writeTo(BitcoinOutputStream output)
      throws IOException
   {
      output.writeReverse(referencedTxHash);
      output.writeUInt32(referencedTxOutIndex);
      output.writeUIntVar(signatureScript.length);
      output.write(signatureScript);
      output.writeUInt32(sequence);
   }

   @Override
   public String toString()
   {
      return "version: "+sequence+", tx hash/out: "+BtcUtil.hexOut(referencedTxHash)+"/"+referencedTxOutIndex+", signature: "+BtcUtil.hexOut(signatureScript);
   }

   public byte[] getReferencedTxHash()
   {
      return referencedTxHash;
   }

   public long getReferencedTxOutIndex()
   {
      return referencedTxOutIndex;
   }

   public byte[] getSignatureScript()
   {
      return signatureScript;
   }

   public long getSequence()
   {
      return sequence;
   }
}

