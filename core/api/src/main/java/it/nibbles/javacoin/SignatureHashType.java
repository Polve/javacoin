/**
 * Copyright (C) 2012 NetMind Consulting Bt.
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
 * The signature type defines how the hash is generated for a 
 * given transaction input. Because the hash is then cryptographically
 * attached to the usage of coins, it garantees that the context
 * in which that usage occurs (the whole transaction or parts thereof)
 * does not change.
 * @author Robert Brautigam
 */
public interface SignatureHashType
{
   /**
    * The type that defines how the inputs in the transaction are coded.
    * SIGHASH_ALLOWUPDATE is implicit in the original definitions
    * of SIGHASH_SINGLE and SIGHASH_NONE, but here it is deliberately
    * separated to allow for a more visible separation of the two concerns, 
    * since the original coupling seems to be arbitary.
    */
   enum InputSignatureHashType
   {
      SIGHASH_ALL,          // Hash all inputs as they occur but without scripts (fix sources of money)
      SIGHASH_ALLOWUPDATE,  // Hash all inputs without scripts and without sequence numbers (fix sources but allow script updates)
      SIGHASH_ANYONECANPAY, // Hash only the input coded (allow any additional sources)
   };

   /**
    * These types correspond to the original hash definitions, 
    * expect they don't imply allowing updates to inputs.
    */
   enum OutputSignatureHashType
   {
      SIGHASH_ALL,    // Hash all outputs as they occur (fix destination of money)
      SIGHASH_SINGLE, // Only hash the output corresponding in index with the input (unknown use-case)
      SIGHASH_NONE,   // Hash none of the outputs (allow any spending)
   };

   /**
    * Get the value of the type as it needs to be
    * embedded in the hash.
    */
   int getValue();

   InputSignatureHashType getInputType();

   OutputSignatureHashType getOutputType();
};

