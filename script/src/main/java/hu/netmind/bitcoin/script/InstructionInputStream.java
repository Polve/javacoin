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

import java.io.InputStream;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;

/**
 * Deserialize instructions from any given input stream.
 * @author Robert Brautigam
 */
public class InstructionInputStream extends InputStream
{
   private static Map<Integer,Operation> operations;
   private InputStream input;
   private int pointer;

   public InstructionInputStream(InputStream input)
   {
      this.input=input;
      this.pointer=0;
   }

   public int getPointer()
   {
      return pointer;
   }

   public void close()
      throws IOException
   {
      input.close();
   }

   public int read()
      throws IOException
   {
      pointer++;
      return input.read();
   }

   public int read(byte[] b, int off, int len)
      throws IOException
   {
      return input.read(b,off,len);
   }

   /**
    * Reads one instruction from the stream with all parameters.
    * @return The instruction read, or null if end of stream is reached (on first byte
    * read)
    * @throws IOException If end of stream is reached while parsing instruction, or underlying
    * input throws exception.
    */
   public Instruction readInstruction()
      throws IOException
   {
      // First read opcode from stream
      int opcode = read();
      if ( opcode < 0 )
         return null;
      // If it's got parameters, then determine length of data that follows
      int parameterLength = 0;
      if ( (opcode>=1) && (opcode<=76) ) // Constant
      {
         // This is a constant that with opcode length
         parameterLength = opcode;
         opcode = -1; // Extermal
      } 
      else if ( opcode == 76 )  // Next byte contains length
      {
         parameterLength = read();
      }
      else if ( opcode == 77 ) // Next 2 bytes contain length of data
      {
         parameterLength = read() | (read()<<8);
      } 
      else if ( opcode == 78 ) // Next 4 bytes contain length of data
      {
         parameterLength = (read()) | ((read())<<8) | ((read())<<16) | ((read())<<24);
         if ( parameterLength < 0 ) // Overflow
            throw new IOException("script parameter doesn't fit 31 bits, too long");
      }
      // Read data if there is any
      byte[] data = null;
      if ( parameterLength > 0 )
      {
         data = new byte[parameterLength];
         int count = 0;
         while ( count < parameterLength )
         {
            int readCount = input.read(data,count,parameterLength-count);
            if ( readCount < 0 )
               throw new IOException("could not read parameter to operation because stream ended");
            count += readCount;
         }
      }
      // Create instruction and return
      return new Instruction(operations.get(opcode),data);
   }

   static
   {
      operations = new HashMap<Integer,Operation>();
      for ( Operation op : Operation.values() )
         operations.put(op.getCode(),op);
   }
}

