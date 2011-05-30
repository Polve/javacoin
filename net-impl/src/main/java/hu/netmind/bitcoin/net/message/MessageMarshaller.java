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

import hu.netmind.bitcoin.net.Message;
import java.io.IOException;
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
   public MessageImpl read(BitCoinInputStream input)
      throws IOException
   {
      // First read the header from the stream
      if ( ! input.markSupported() )
         throw new IOException("input stream for deserialization does not support mark");
      input.mark(20);
      MessageImpl header = new MessageImpl(input,null);
      input.reset(); // Rewind, so message will read header again
      // Now search for a suitable message
      Class messageType = messageTypes.get(header.getCommand());
      if ( messageType == null )
        throw new IOException("message type not found for command: "+header.getCommand()); 
      logger.debug("message type {} found for command {}",messageType,header.getCommand());
      // Search for the construction parameter if there is any
      Object param = null;
      for ( Map.Entry<Class,Object> entry : params.entrySet() )
         if ( entry.getKey().isAssignableFrom(messageType) )
            param = entry.getValue();
      // Instantiate message and use the message deserialization in constructor
      try
      {
         MessageImpl message = (MessageImpl) messageType.getConstructor(BitCoinInputStream.class,Object.class).
            newInstance(input,param);
         logger.debug("deserialized message: {}",message);
         return message;
      } catch ( InvocationTargetException e ) {
         if ( e.getCause() instanceof IOException )
            throw (IOException) e.getCause();
         throw new IOException("unexpected exception from the message construction",e);
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
   public void write(MessageImpl message, BitCoinOutputStream output)
      throws IOException
   {
      // Serialize known values into a byte array stream
      ByteArrayBitCoinOutputStream byteOutput = new ByteArrayBitCoinOutputStream();
      message.preWriteTo(byteOutput);
      byteOutput.close();
      byte[] byteArray = byteOutput.toByteArray();
      // Invoke post write to finalize content
      message.postWriteTo(byteArray);
      // Copy it to the output
      output.write(byteArray);
   }

}

