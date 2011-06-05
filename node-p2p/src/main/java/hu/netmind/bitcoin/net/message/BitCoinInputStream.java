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

package hu.netmind.bitcoin.net.message;

import java.io.InputStream;
import java.io.IOException;
import java.util.Observer;

/**
 * This is an input stream that supports the various types defined in the
 * BitCoin protocol. This still has to be subclassed to provide the actual
 * <code>read()</code>. Note: input stream must support the mark mechanism!
 * @author Robert Brautigam
 */
public abstract class BitCoinInputStream extends InputStream
{
   private Listener listener = null;

   /**
    * Set an observer which will receive each byte read in this stream through
    * the methods provided by this class.
    */
   public void setListener(Listener listener)
   {
      this.listener=listener;
   }

   /**
    * Clear the observer watching this stream.
    */
   public void clearListener()
   {
      listener=null;
   }

   /**
    * Read an unsigned byte from the stream as a long.
    */
   public long readU()
      throws IOException
   {
      int readValue = read();
      if ( readValue < 0 )
         throw new IOException("stream ended, can't read more values");
      if ( listener != null )
         listener.update(readValue);
      return (readValue & 0xFFl);
   }

   public long readUInt16()
      throws IOException
   {
      return (readU()) | (readU()<<8);
   }

   public long readUInt16BE()
      throws IOException
   {
      return (readU()<<8) | (readU());
   }

   public long readUInt32()
      throws IOException
   {
      return (readU()) | (readU()<<8) | (readU()<<16) | (readU()<<24);
   }

   public long readUInt32BE()
      throws IOException
   {
      return (readU()<<24) | (readU()<<16) | (readU()<<8) | (readU());
   }

   /**
    * Read an unsigned long. There is no such thing in Java, so we return a
    * signed long. Check overflow to negative on usage!
    */
   public long readUInt64()
      throws IOException
   {
      return (readU()) | (readU()<<8) | (readU()<<16) | (readU()<<24) |
        (readU()<<32) | (readU()<<40) | (readU()<<48) | (readU()<<56);
   }

   public long readUIntVar()
      throws IOException
   {
      long result = readU();
      if ( result == 0xff )
         return readUInt64();
      else if ( result == 0xfe )
         return readUInt32();
      else if ( result == 0xfd )
         return readUInt16();
      return  result;
   }

   /**
    * Read a variable length string.
    */
   public String readString()
      throws IOException
   {
      long length = readUIntVar();
      return readString(length);
   }

   /**
    * Read a string that is fitted in the given length exactly,
    * with terminating nulls.
    */
   public String readString(long length)
      throws IOException
   {
      StringBuilder builder = new StringBuilder();
      boolean nullReached = false;
      int readChar;
      for ( int i=0; i<length; i++ )
      {
         readChar = read();
         if ( (nullReached) && (readChar!=0) )
            throw new IOException("reading fixed length string ("+builder.toString()+") was not properly null padded");
         if ( readChar == 0 )
            nullReached = true;
         else
            builder.append((char) readChar);
      }
      return builder.toString();
   }

   /**
    * Read a byte array from stream.
    */
   public byte[] readBytes(int length)
      throws IOException
   {
      byte[] result = new byte[length];
      for ( int i=0; i<length; i++ )
         result[i]=(byte) readU();
      return result;
   }

   public interface Listener
   {
      void update(int value);
   }
}

