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
 * License aint with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package hu.netmind.bitcoin.script;

import java.io.OutputStream;
import java.io.IOException;

/**
 * Serialize instructions to a stream.
 * @author Robert Brautigam
 */
public class InstructionOutputStream extends OutputStream
{
   private OutputStream output;

   public InstructionOutputStream(OutputStream output)
   {
      this.output=output;
   }

   public void close()
      throws IOException
   {
      output.close();
   }

   public void write(int b)
      throws IOException
   {
      output.write(b);
   }

   public void write(byte[] b, int off, int len)
      throws IOException
   {
      output.write(b,off,len);
   }

   public void writeInstruction(Instruction instruction)
      throws IOException
   {
      writeInstruction(instruction,false);
   }

   void writeInstruction(Instruction instruction, boolean ommitData)
      throws IOException
   {
      if ( instruction.getOperation() == Operation.CONSTANT )
      {
         if ( (instruction.getData().length < 1) || (instruction.getData().length>75) )
            throw new IOException("tried to write a constant with length not between 1 - 75: "+instruction.getData().length+", use OP_PUSHDATA");
         write(instruction.getData().length);
         if ( ! ommitData )
            write(instruction.getData());
      }
      else if ( instruction.getOperation() == Operation.OP_PUSHDATA1 )
      {
         if ( (instruction.getData().length < 0) || (instruction.getData().length>0xff) )
            throw new IOException("tried to write data with length not below 256: "+instruction.getData().length+", use OP_PUSHDATA2 (or OP_PUSHDATA4)");
         write((byte)instruction.getOperation().getCode());
         int len = instruction.getData().length;
         write( (byte)(len & 0xff) );
         if ( ! ommitData )
            write(instruction.getData());
      } 
      else if ( instruction.getOperation() == Operation.OP_PUSHDATA2 )
      {
         if ( (instruction.getData().length < 0) || (instruction.getData().length>0xffff) )
            throw new IOException("tried to write data with length not below 65536: "+instruction.getData().length+", use OP_PUSHDATA4");
         write((byte)instruction.getOperation().getCode());
         int len = instruction.getData().length;
         write( (byte)(len & 0xff) );
         write( (byte)((len>>8) & 0xff) );
         if ( ! ommitData )
            write(instruction.getData());
      }
      else if ( instruction.getOperation() == Operation.OP_PUSHDATA4 )
      {
         if ( instruction.getData().length < 0 )
            throw new IOException("tried to write data with invalid length: "+instruction.getData().length);
         write((byte)instruction.getOperation().getCode());
         int len = instruction.getData().length;
         write( (byte)(len & 0xff) );
         write( (byte)((len>>8) & 0xff) );
         write( (byte)((len>>16) & 0xff) );
         write( (byte)((len>>24) & 0xff) );
         if ( ! ommitData )
            write(instruction.getData());
      }
      else
      {
         if ( instruction.getData() != null )
            throw new IOException("there is data in the instruction but it is neither a CONSTANT nor one of OP_PUSHDATA");
         // Normal operation, so write opcode
         write((byte)instruction.getOperation().getCode());
      }
   }
}

