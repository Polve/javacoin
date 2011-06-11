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
import java.io.OutputStream;

/**
 * This is the base class for all messages, provides serialization and
 * deserialization of the header.
 * @author Robert Brautigam
 */
public class Message
{
   public static final long MAGIC_MAIN = 0xF9BEB4D9l;
   public static final long MAGIC_TEST = 0xFABFB5DAl;

   private long magic;
   private String command;
   private long length = -1;

   /**
    * All messages must provide a constructor to construct
    * the message by supplying all attributes. Note: length of
    * payload will be automatically calculated.
    */
   public Message(long magic, String command)
   {
      this.magic=magic;
      this.command=command;
   }

   /**
    * An empty contructor must always be present to construct an empty
    * message for the deserialization.
    */
   Message()
   {
   }

   /**
    * Deserialize the object reading from an input stream.
    * @param input The stream to read this object from.
    * @param version The protocol version number as communicated by the version command,
    * if it's known.
    * @param param A message type specific parameter to be used to control the
    * deserialization process. Original intent is to allow for block pre-filtering,
    * so that client does not construct potentially large objects in memory.
    */
   void readFrom(BitCoinInputStream input, long version, Object param)
      throws IOException
   {
      magic = input.readUInt32BE();
      command = input.readString(12);
      length = input.readUInt32();
   }

   /**
    * Notification when the full message has been deserialized. Call receives
    * the same paramters as the <code>readFrom()</code> call.
    * @param input The stream to read this object from.
    * @param version The protocol version number as communicated by the version command,
    * if it's known.
    * @param param A message type specific parameter to be used to control the
    * deserialization process. Original intent is to allow for block pre-filtering,
    * so that client does not construct potentially large objects in memory.
    */
   void postReadFrom(BitCoinInputStream input, long version, Object param)
   {
   }

   public long getMagic()
   {
      return magic;
   }

   public String getCommand()
   {
      return command;
   }

   /**
    * Get the length of the message. Note this is only available if
    * message has been read from stream or has been written to stream.
    * @return The length of the payload, or -1 if it's not known.
    */
   public long getLength()
   {
      return length;
   }
   
   /**
    * Called by the serialization logic to make the object write
    * everything to the output stream for which values already
    * exist. Note that attributes for which values do not exist
    * must write a placeholder, because the post processing may
    * change values but not add or remove.
    * All messages must override this method to serialize
    * the contents of the message to the BitCoin protocol. Note,
    * implementations must call the superclass' <code>preWriteTo()</code>
    * always <strong>first</strong>.
    * @param output The output stream to write to.
    * @param version The protocol version to use.
    */
   void writeTo(BitCoinOutputStream output, long version)
      throws IOException
   {
      output.writeUInt32BE(magic);
      output.writeString(command,12);
      output.writeUInt32(0); // We don't know the length yet
   }

   /**
    * Called by the deserialization logic to compute/fill out
    * any values in the generated byte array for this message. This is
    * called directly after the <code>preWriteTo()</code> method. Note,
    * implements must call the superclass' <code>postWriteTo()</code>
    * always <strong>last</strong>.
    */
   void postWriteTo(byte[] serializedBytes, long version)
      throws IOException
   {
      // Let's fill out the length now (we couldn't have known that
      // in the writeTo method)
      if ( this instanceof ChecksummedMessage )
         length = serializedBytes.length - 24;
      else
         length = serializedBytes.length - 20;
      // Overwrite previous 0 value
      BitCoinOutputStream output = new BitCoinOutputStream(
            new OverwriterByteArrayOutputStream(serializedBytes,16));
      output.writeUInt32(length);
   }

   /**
    * The string representation of the message. String representations
    * should be concatenable, so subclasses can re-use representations
    * of superclasses.
    */
   public String toString()
   {
      return "BitCoin command "+command+" ("+Long.toHexString(magic)+")";
   }

   protected static class OverwriterByteArrayOutputStream extends OutputStream
   {
      private byte[] byteArray;
      private int position = 0; 

      /**
       * @param byteArray The array to overwrite content in.
       * @param offset The offset at which to begin overwriting.
       */
      public OverwriterByteArrayOutputStream(byte[] byteArray, int offset)
      {
         this.byteArray=byteArray;
         this.position=offset;
      }

      public void write(int value)
         throws IOException
      {
         if ( position >= byteArray.length )
            throw new IOException("tried to write past the array length to position "+position);
         byteArray[position++] = (byte) value;
      }
   }
}

