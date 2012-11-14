package hu.netmind.bitcoin.block;

/**
 *
 * @author Alessandro Polverini <alex@nibbles.it>
 */
public interface StorageSession
{
   public void commit();
   public void rollback();
}
