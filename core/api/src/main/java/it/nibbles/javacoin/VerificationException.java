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

package it.nibbles.javacoin;

/**
 * Thrown if the consistency and/or cryptographic validity of any component
 * is found to be compromised, or can not be successfully verified.
 * @author Robert Brautigam
 */
public class VerificationException extends BitcoinException
{
   public VerificationException(String message)
   {
      super(message);
   }

   public VerificationException(String message, Throwable e)
   {
      super(message,e);
   }
}

