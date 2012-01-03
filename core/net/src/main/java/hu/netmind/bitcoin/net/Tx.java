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

package hu.netmind.bitcoin.net;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

/**
 * @author Robert Brautigam
 */
public class Tx
{
   private long version;
   private List<TxIn> inputs;
   private List<TxOut> outputs;
   private long lockTime;

   public Tx(long version, List<TxIn> inputs, List<TxOut> outputs, long lockTime)
   {
      this.version=version;
      this.inputs=inputs;
      this.outputs=outputs;
      this.lockTime=lockTime;
   }

   Tx()
   {
   }

   void readFrom(BitCoinInputStream input, long protocolVersion, Object param)
      throws IOException
   {
      version = input.readUInt32();
      long inCount = input.readUIntVar();
      inputs = new ArrayList<TxIn>();
      for ( long i=0; i<inCount; i++ )
      {
         TxIn in = new TxIn();
         in.readFrom(input);
         inputs.add(in);
      }
      long outCount = input.readUIntVar();
      outputs = new ArrayList<TxOut>();
      for ( long i=0; i<outCount; i++ )
      {
         TxOut out = new TxOut();
         out.readFrom(input);
         outputs.add(out);
      }
      lockTime = input.readUInt32();
   }

   /**
    * Write this transaction object with the latest protocol version to the output.
    */
   public void writeTo(BitCoinOutputStream output)
      throws IOException
   {
      writeTo(output,0);
   }

   void writeTo(BitCoinOutputStream output, long protocolVersion)
      throws IOException
   {
      output.writeUInt32(version);
      output.writeUIntVar(inputs.size());
      for ( TxIn in : inputs )
         in.writeTo(output);
      output.writeUIntVar(outputs.size());
      for ( TxOut out : outputs )
         out.writeTo(output);
      output.writeUInt32(lockTime);
   }

   public String toString()
   {
      return super.toString()+" version: "+version+", inputs: "+inputs+", outputs: "+outputs;
   }

   public long getVersion()
   {
      return version;
   }

   public List<TxIn> getInputs()
   {
      return inputs;
   }

   public List<TxOut> getOutputs()
   {
      return outputs;
   }

   public long getLockTime()
   {
      return lockTime;
   }
}

