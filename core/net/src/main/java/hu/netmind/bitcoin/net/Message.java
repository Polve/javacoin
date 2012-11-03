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
import java.io.OutputStream;
import java.security.MessageDigest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is the base class for all messages, provides serialization and
 * deserialization of the header.
 * @author Robert Brautigam
 */
public class Message
{
   private static final Logger logger = LoggerFactory.getLogger(Message.class);

   private long magic;
   private String command;
   private long length = -1;
   private long checksum = 0;
   private long calculatedChecksum = 0;
   private MessageDigest digest = null;

   /**
    * All messages must provide a constructor to construct
    * the message by supplying all attributes. Note: length of
    * payload will be automatically calculated.
    */
   public Message(long magic, String command) throws IOException
   {
      this.magic=magic;
      this.command=command;
      try
      {
         digest = MessageDigest.getInstance("SHA-256");
      } catch ( Exception e ) {
         throw new IOException("could not initialize SHA-256 digest",e);
      }
   }

   /**
    * An empty contructor must always be present to construct an empty
    * message for the deserialization.
    */
   Message() throws IOException
   {
      try
      {
         digest = MessageDigest.getInstance("SHA-256");
      } catch ( Exception e ) {
         throw new IOException("could not initialize SHA-256 digest",e);
      }
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
   void readFrom(BitcoinInputStream input, long version, Object param)
      throws IOException
   {
      magic = input.readUInt32BE();
      command = input.readString(12);
      length = input.readUInt32();
      checksum = input.readUInt32();
      // Now let's make sure we keep track of the real checksum
      input.setListener(new BitcoinInputStream.Listener() {
         @Override
               public void update(int value)
               {
                  digest.update((byte) value);
               }
            });
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
   void postReadFrom(BitcoinInputStream input, long version, Object param)
   {
      // Calculate the checksum as sha256(sha256(content))
      input.clearListener();
      byte[] tmp = digest.digest();
      digest.reset();
      byte[] result = digest.digest(tmp);
      // Calculate checksum first 4 bytes
      calculatedChecksum = (long) ((long) result[0]&0xff) | (((long) result[1]&0xff)<<8) |
         (((long) result[2]&0xff)<<16) | (((long) result[3]&0xff)<<24);
      logger.debug("digest checksum: {}",calculatedChecksum);
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
    * the contents of the message to the Bitcoin protocol. Note,
    * implementations must call the superclass' <code>preWriteTo()</code>
    * always <strong>first</strong>.
    * @param output The output stream to write to.
    * @param version The protocol version to use.
    */
   void writeTo(BitcoinOutputStream output, long version)
      throws IOException
   {
      output.writeUInt32BE(magic);
      output.writeString(command,12);
      output.writeUInt32(0);  // We don't know the length yet
      output.writeUInt32(0);  // We don't know the checksum yet
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
      //if ( this instanceof ChecksummedMessage )
         length = serializedBytes.length - 24;
      //else
      //   length = serializedBytes.length - 20;
      // Overwrite previous 0 value of length
      BitcoinOutputStream output = new BitcoinOutputStream(
            new OverwriterByteArrayOutputStream(serializedBytes,16));
      output.writeUInt32(length);
      // Calculate checksum
      digest.reset();
      digest.update(serializedBytes,24,serializedBytes.length-24);
      byte[] tmp = digest.digest();
      digest.reset();
      byte[] result = digest.digest(tmp);
      // Overwrite previous 0 value with first 4 bytes of checksum
      BitcoinOutputStream tmpOut = new BitcoinOutputStream(
            new OverwriterByteArrayOutputStream(serializedBytes,20));
      tmpOut.writeU(result[0]&0xff);
      tmpOut.writeU(result[1]&0xff);
      tmpOut.writeU(result[2]&0xff);
      tmpOut.writeU(result[3]&0xff);
   }

   public long getChecksum()
   {
      return checksum;
   }

   public boolean verify()
   {
      logger.debug("verifying message calculated: {} vs. {}",calculatedChecksum,checksum);
      return calculatedChecksum == checksum;
   }

   /**
    * The string representation of the message. String representations
    * should be concatenable, so subclasses can re-use representations
    * of superclasses.
    */
   public String toString()
   {
      return "Bitcoin command "+command+" ("+Long.toHexString(magic)+")";
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

