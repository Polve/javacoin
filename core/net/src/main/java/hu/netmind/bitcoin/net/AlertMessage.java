/**
 * Copyright (C) 2011 NetMind Consulting Bt.
 *
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
package hu.netmind.bitcoin.net;

import hu.netmind.bitcoin.PublicKey;
import hu.netmind.bitcoin.VerificationException;
import hu.netmind.bitcoin.keyfactory.ecc.KeyFactoryImpl;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Alessandro Polverini
 */
public class AlertMessage extends ChecksummedMessage {

  private static final Logger logger = LoggerFactory.getLogger(AlertMessage.class);
  // Chosen arbitrarily to avoid memory blowups.
  private static final long MAX_SET_SIZE = 100;
  private long version;
  private long relayUntil;
  private long expiration;
  private long id;
  private long cancel;
  private Set<Long> cancelSet;
  private long minVer;
  private long maxVer;
  private Set<String> matchingSubVers;
  private long priority;
  private String comment;
  private String reserved;
  private String message;
  private String signature;
  private boolean signatureVerified = false;
  public static final byte[] satoshiPubKeyBytes = HexUtil.toByteArray(
          "04 fc 97 02 84 78 40 aa f1 95 de 84 42 eb ec ed f5 b0 95 cd "
          + "bb 9b c7 16 bd a9 11 09 71 b2 8a 49 e0 ea d8 56 4f f0 db 22 "
          + "20 9e 03 74 78 2c 09 3b b8 99 69 2d 52 4e 9d 6a 69 56 e7 c5 "
          + "ec bc d6 82 84");
  private KeyFactoryImpl factory = new KeyFactoryImpl(null);
  private PublicKey satoshiPubKey = factory.createPublicKey(satoshiPubKeyBytes);

  public AlertMessage(long magic, String message, String signature)
          throws IOException {
    super(magic, "alert");
    this.message = message;
    this.signature = signature;
  }

  AlertMessage()
          throws IOException {
    super();
  }

  public byte[] doubleDigest(byte[] input) throws NoSuchAlgorithmException {
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    byte[] firstHash = digest.digest(input);
    digest.reset();
    return digest.digest(firstHash);
  }

  @Override
  void readFrom(BitCoinInputStream input, long protocolVersion, Object param)
          throws IOException {
    super.readFrom(input, protocolVersion, param);
    byte[] alertPayload = input.readBytes();
    byte[] sig = input.readBytes();
    try {
      if (satoshiPubKey.verify(doubleDigest(alertPayload), sig)) {
        signatureVerified = true;
      }
    } catch (NoSuchAlgorithmException ex) {
      // Should not happen
      signatureVerified = false;
    } catch (VerificationException ex) {
      signatureVerified = false;
    }
    BitCoinInputStream alertStream = new BitCoinInputStream(new ByteArrayInputStream(alertPayload));
    version = alertStream.readUInt32();
    relayUntil = alertStream.readUInt64();
    expiration = alertStream.readUInt64();
    id = alertStream.readUInt32();
    cancel = alertStream.readUInt32();
    long numCancels = alertStream.readU();
    if (numCancels >= 0 && numCancels < MAX_SET_SIZE) {
      cancelSet = new HashSet<Long>((int) numCancels);
      for (long i = 0; i < numCancels; i++) {
        cancelSet.add(input.readUInt32());
      }
    } else {
      // Too many cancel sets, skip them
      logger.warn("Cancel set too big in AlertMessage: " + numCancels);
      for (long i = 0; i < numCancels; i++) {
        input.readUInt32();
      }
    }
    minVer = alertStream.readUInt32();
    maxVer = alertStream.readUInt32();
    long numSubVers = alertStream.readU();
    if (numSubVers >= 0 && numSubVers < MAX_SET_SIZE) {
      matchingSubVers = new HashSet<String>((int) numSubVers);
      for (long i = 0; i < numSubVers; i++) {
        matchingSubVers.add(input.readString());
      }
    } else {
      logger.warn("SubVers set too big in AlertMessage: " + numSubVers);
      for (long i = 0; i < numSubVers; i++) {
        input.readString();
      }
    }
    priority = alertStream.readUInt32();
    comment = alertStream.readString();
    message = alertStream.readString();
    reserved = alertStream.readString();
  }

  @Override
  void writeTo(BitCoinOutputStream output, long protocolVersion)
          throws IOException {
    super.writeTo(output, protocolVersion);
    output.writeString(message);
    output.writeString(signature);
  }

  @Override
  public String toString() {
    return super.toString() + " version: " + version + " relayUntil: " + relayUntil + " expiration: " + expiration + " id: " + id + " priority: " + priority + " message: " + message + ", signature: " + signature;
  }

  public boolean isSignatureVerified() {
    return signatureVerified;
  }

  public String getMessage() {
    return message;
  }

  public String getSignature() {
    return signature;
  }

  public long getCancel() {
    return cancel;
  }

  public Set<Long> getCancelSet() {
    return cancelSet;
  }

  public String getComment() {
    return comment;
  }

  public long getExpiration() {
    return expiration;
  }

  public long getId() {
    return id;
  }

  public long getMaxVer() {
    return maxVer;
  }

  public long getMinVer() {
    return minVer;
  }

  public long getPriority() {
    return priority;
  }

  public long getRelayUntil() {
    return relayUntil;
  }

  public String getReserved() {
    return reserved;
  }

  public long getVersion() {
    return version;
  }
}
