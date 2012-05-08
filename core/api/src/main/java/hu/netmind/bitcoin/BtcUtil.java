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
package hu.netmind.bitcoin;

import java.util.StringTokenizer;
import javax.xml.bind.DatatypeConverter;

/**
 * @author Robert Brautigam, Alessandro Polverini
 */
public class BtcUtil {

  private static final String[] HEXCHARS = new String[]{
    "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "A", "B", "C", "D", "E", "F"
  };

  /**
   * Helper method to convert strings like: "F0 A4 32 05", to actual byte array
   * for easier testing.
   */
//  public static byte[] toByteArray(String hexString) {
//    StringTokenizer tokenizer = new StringTokenizer(hexString, " ");
//    byte[] result = new byte[tokenizer.countTokens()];
//    for (int i = 0; i < result.length; i++) {
//      result[i] = (byte) Integer.valueOf(tokenizer.nextToken(), 16).intValue();
//    }
//    return result;
//  }

  /**
   * Helper method to convert strings like: "F0A43205", to actual byte array
   * for easier testing.
   */
  public static byte[] hexIn(String s) {
    return DatatypeConverter.parseHexBinary(s);
  }

  /**
   * Make the byte array appear as space separated 2 digit hex numbers.
   */
  public static String hexOutSpaces(byte[] byteArray) {
    return toHexString(byteArray, true);
  }

  public static String hexOutSpaces(String hexString) {
    return toHexString(hexIn(hexString), true);
  }

  /**
   * Make the byte array appear as hex number, without spaces.
   */
  public static String hexOut(byte[] byteArray) {
    return DatatypeConverter.printHexBinary(byteArray);
    /*
    if (byteArray == null) {
      return "null";
    } else {
      return toHexString(byteArray, false);
    }
    */
  }

  public static String toHexString(byte[] byteArray, boolean withSpaces) {
    StringBuilder builder = new StringBuilder();
    for (int i = 0; i < byteArray.length; i++) {
      if (withSpaces && i > 0) {
        builder.append(" ");
      }
      builder.append(HEXCHARS[(byteArray[i] >> 4) & 0xF]);
      builder.append(HEXCHARS[byteArray[i] & 0xF]);
    }
    return builder.toString();
  }
}
