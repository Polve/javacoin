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
import java.io.ByteArrayOutputStream;
import java.util.Map;
import java.util.HashMap;
import java.util.ResourceBundle;
import java.util.Enumeration;
import java.lang.reflect.InvocationTargetException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles serialization and deserialization of messages.
 * @author Robert Brautigam
 */
public class MessageMarshaller
{
   private static final Logger logger = LoggerFactory.getLogger(MessageMarshaller.class);

   private static Map<String,Class> messageTypes = new HashMap<String,Class>();
   private Map<Class,Object> params = new HashMap<Class,Object>();
   private long version = -1;

   static
   {
      try
      {
         // Read all configured message types
         ResourceBundle bundle = ResourceBundle.getBundle("message-types");
         Enumeration<String> keys = bundle.getKeys();
         while ( keys.hasMoreElements() )
         {
            String key = keys.nextElement();
            messageTypes.put(key, Class.forName(bundle.getString(key)));
         }
         logger.debug("read following message types: {}",messageTypes);
      } catch ( Exception e ) {
         logger.error("error while reading message types",e);
      }
   }

   /**
    * Set a parameter to the construction of a specific message.
    * @param messageType The interface type of the message.
    * @param param The parameter to submit to the message construction. See message API 
    * documentation for message implementations what parameters are valid.
    */
   public void setParam(Class messageType, Object param)
   {
      params.put(messageType,param);
   }

   /**
    * Deserialize a message using the specified BitCoin input
    * stream. The call will block until all the bytes for one
    * message arrives.
    * @param input The stream to read the message from.
    */
   public Message read(BitCoinInputStream input)
      throws IOException
   {
      // First read the header from the stream, repeat this step
      // until we find a message we recognize
      if ( ! input.markSupported() )
         throw new IOException("input stream for deserialization does not support mark");
      Message header = new Message();
      Class messageType = null;
      while ( messageType == null )
      {
         input.mark(20);
         header.readFrom(input,version,null);
         input.reset(); // Rewind, so message will read header again
         // Now search for a suitable message
         messageType = messageTypes.get(header.getCommand());
         if ( messageType == null )
         {
            // Did not recognize, so skip this message altogether
            input.skip(header.getLength() + 24); // +24 is to skip header
            logger.warn("did not find message type for command (skipping message): {}",header.getCommand());
         }
      }
      logger.debug("message type {} found for command {}",messageType,header.getCommand());
      // Search for the construction parameter if there is any
      Object param = params.get(messageType);
      // Instantiate message and use the message deserialization in constructor
      try
      {
         Message message = (Message) messageType.newInstance();
         input.resetByteCount();
         message.readFrom(input,version,param);
         // If we read less bytes than the announced length of message,
         // it is probable there was an extension to this message and we don't know it
         // yet. Just skip the rest of message, hopefully it was not important :)
         input.skip(header.getLength()+((message instanceof ChecksummedMessage)?24:20)-input.getByteCount());
         message.postReadFrom(input,version,param);
         logger.debug("deserialized message: {}",message);
         if ( (message instanceof ChecksummedMessage) && (!((ChecksummedMessage)message).verify()) )
            throw new IOException("message checksum wrong for message: "+message);
         return message;
      } catch ( IOException e ) {
         throw e;
      } catch ( Exception e ) {
         throw new IOException("unexpected exception from the message construction",e);
      }
   }

   /**
    * Serialize a message to a given output stream. This method implements a two
    * phase (two-pass) serialization. In the first pass all known values are written
    * to the stream, and the values not known will have placeholders. Then a second
    * pass fills out the values for which the output has to be already present to calculate
    * (like checksum, length, etc). This also means that this method must needs at least
    * as much memory as the message itself.
    * @param output The output stream to write to.
    */
   public void write(Message message, BitCoinOutputStream output)
      throws IOException
   {
      // Serialize known values into a byte array stream
      ByteArrayOutputStream byteArrayOutput = new ByteArrayOutputStream();
      BitCoinOutputStream byteOutput = new BitCoinOutputStream(byteArrayOutput);
      message.writeTo(byteOutput,version);
      byteOutput.close();
      byte[] byteArray = byteArrayOutput.toByteArray();
      // Invoke post write to finalize content
      message.postWriteTo(byteArray,version);
      // Copy it to the output
      output.write(byteArray);
      output.flush(); // Send
   }

   public long getVersion()
   {
      return version;
   }
   public void setVersion(long version)
   {
      this.version=version;
   }

}

