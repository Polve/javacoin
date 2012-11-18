/**
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option) any
 * later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package it.nibbles.javacoin.net;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Alessandro Polverini
 */
public class AlertMessage extends Message
{

   private static final Logger logger = LoggerFactory.getLogger(AlertMessage.class);
   // Chosen arbitrarily to avoid memory blowups.
   private static final long MAX_SET_SIZE = 100;
   private long version = 1;
   private long relayUntil;
   private long expiration;
   private long id;
   private long cancel;
   private Set<Long> cancelSet;
   private long minVer;
   private long maxVer;
   private Set<String> matchingSubVers;
   private long priority;
   private String comment = "";
   private String reserved = "";
   private String message = "";
   private byte[] signature = new byte[] { };

   public AlertMessage(long magic, String message)
      throws IOException
   {
      super(magic, "alert");
      this.message = message;
   }

   AlertMessage()
      throws IOException
   {
      super();
   }

   @Override
   void readFrom(BitcoinInputStream input, long protocolVersion, Object param)
      throws IOException
   {
      super.readFrom(input, protocolVersion, param);
      byte[] alertPayload = input.readBytes();
      signature = input.readBytes();
      BitcoinInputStream alertStream = new BitcoinInputStream(new ByteArrayInputStream(alertPayload));
      version = alertStream.readUInt32();
      relayUntil = alertStream.readUInt64();
      expiration = alertStream.readUInt64();
      id = alertStream.readUInt32();
      cancel = alertStream.readUInt32();
      long numCancels = alertStream.readU();
      if (numCancels >= 0 && numCancels < MAX_SET_SIZE)
      {
         cancelSet = new HashSet<Long>((int) numCancels);
         for (long i = 0; i < numCancels; i++)
         {
            cancelSet.add(input.readUInt32());
         }
      } else
      {
         // Too many cancel sets, skip them
         logger.warn("Cancel set too big in AlertMessage: " + numCancels);
         for (long i = 0; i < numCancels; i++)
         {
            input.readUInt32();
         }
      }
      minVer = alertStream.readUInt32();
      maxVer = alertStream.readUInt32();
      long numSubVers = alertStream.readU();
      if (numSubVers >= 0 && numSubVers < MAX_SET_SIZE)
      {
         matchingSubVers = new HashSet<String>((int) numSubVers);
         for (long i = 0; i < numSubVers; i++)
         {
            matchingSubVers.add(input.readString());
         }
      } else
      {
         logger.warn("SubVers set too big in AlertMessage: " + numSubVers);
         for (long i = 0; i < numSubVers; i++)
         {
            input.readString();
         }
      }
      priority = alertStream.readUInt32();
      comment = alertStream.readString();
      message = alertStream.readString();
      reserved = alertStream.readString();
   }

   @Override
   void writeTo(BitcoinOutputStream output, long protocolVersion)
      throws IOException
   {
      super.writeTo(output, protocolVersion);
      byte[] payload = getAlertPayload();
      output.writeUIntVar(payload.length);
      output.write(payload);
      output.writeUIntVar(signature.length);
      output.write(signature);
   }

   public byte[] getAlertPayload()
   {
      try
      {
         ByteArrayOutputStream payload = new ByteArrayOutputStream();
         BitcoinOutputStream payloadStream = new BitcoinOutputStream(payload);
         payloadStream.writeUInt32(version);
         payloadStream.writeUInt64(relayUntil);
         payloadStream.writeUInt64(expiration);
         payloadStream.writeUInt32(id);
         payloadStream.writeUInt32(cancel);
         if (cancelSet == null || cancelSet.isEmpty())
            payloadStream.writeU(0);
         else
         {
            payloadStream.writeU(cancelSet.size());
            for (long i : cancelSet)
               payloadStream.writeUInt32(i);
         }
         payloadStream.writeUInt32(minVer);
         payloadStream.writeUInt32(maxVer);
         if (matchingSubVers == null || matchingSubVers.isEmpty())
            payloadStream.writeU(0);
         else
         {
            payloadStream.writeU(matchingSubVers.size());
            for (String s : matchingSubVers)
               payloadStream.writeString(s);
         }
         payloadStream.writeUInt32(priority);
         payloadStream.writeString(comment);
         payloadStream.writeString(message);
         payloadStream.writeString(reserved);
         return payload.toByteArray();
      } catch (IOException ex)
      {
         return null;
      }
   }

   @Override
   public String toString()
   {
      return super.toString() + " version: " + version + " relayUntil: " + relayUntil + " expiration: " + expiration + " id: " + id + " priority: " + priority + " message: " + message + ", signature: " + signature;
   }

   public String getMessage()
   {
      return message;
   }

   public byte[] getSignature()
   {
      return signature;
   }

   public long getCancel()
   {
      return cancel;
   }

   public Set<Long> getCancelSet()
   {
      return cancelSet;
   }

   public String getComment()
   {
      return comment;
   }

   public long getExpiration()
   {
      return expiration;
   }

   public long getId()
   {
      return id;
   }

   public long getMaxVer()
   {
      return maxVer;
   }

   public long getMinVer()
   {
      return minVer;
   }

   public long getPriority()
   {
      return priority;
   }

   public long getRelayUntil()
   {
      return relayUntil;
   }

   public String getReserved()
   {
      return reserved;
   }

   public long getVersion()
   {
      return version;
   }

   public void setCancel(long cancel)
   {
      this.cancel = cancel;
   }

   public void setCancelSet(Set<Long> cancelSet)
   {
      this.cancelSet = cancelSet;
   }

   public void setComment(String comment)
   {
      this.comment = comment;
   }

   public void setExpiration(long expiration)
   {
      this.expiration = expiration;
   }

   public void setId(long id)
   {
      this.id = id;
   }

   public void setMatchingSubVers(Set<String> matchingSubVers)
   {
      this.matchingSubVers = matchingSubVers;
   }

   public void setMaxVer(long maxVer)
   {
      this.maxVer = maxVer;
   }

   public void setMessage(String message)
   {
      this.message = message;
   }

   public void setMinVer(long minVer)
   {
      this.minVer = minVer;
   }

   public void setPriority(long priority)
   {
      this.priority = priority;
   }

   public void setRelayUntil(long relayUntil)
   {
      this.relayUntil = relayUntil;
   }

   public void setReserved(String reserved)
   {
      this.reserved = reserved;
   }

   public void setSignature(byte[] signature)
   {
      this.signature = signature;
   }

   public void setVersion(long version)
   {
      this.version = version;
   }
}
