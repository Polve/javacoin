package hu.netmind.bitcoin.block;

import java.io.Closeable;

/**
 *
 * @author Alessandro Polverini <alex@nibbles.it>
 */
public interface StorageSession
{
   public void commit();
   public void rollback();
}
