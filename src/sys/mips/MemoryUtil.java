package sys.mips;

import java.nio.charset.StandardCharsets;
import java.util.*;

public class MemoryUtil {
	
	/** round up to nearest word */
	public static final int nextWord (final int addr) {
		return (addr + 3) & ~3;
	}
	
	/** return pointer to first word after data */
	public static final int storeWords (final Memory mem, final int addr, final List<Integer> data) {
		System.out.println("memory store integers " + data.size());
		for (int n = 0; n < data.size(); n++) {
			final int a = addr + (n * 4);
			mem.storeWord(a, data.get(n));
		}
		return addr + (data.size() * 4);
	}
	
	/** return pointer to first word after data */
	public static final int storeWords (final Memory mem, final int addr, final int[] data) {
		System.out.println("memory store ints " + data.length);
		for (int n = 0; n < data.length; n++) {
			final int a = addr + (n * 4);
			mem.storeWord(a, data[n]);
		}
		return addr + (data.length * 4);
	}
	
	/** fill in addresses and trailing null, return pointer to first byte after data */
	public static final int storeStrings (final Memory mem, final int addr, final List<Integer> addresses, final List<String> values) {
		System.out.println("memory store strings " + values.size());
		int p = addr;
		for (String value : values) {
			addresses.add(p);
			p = storeString(mem, p, value);
		}
		// add trailing null
		addresses.add(0);
		return p;
	}
	
	/** return pointer to first byte after data */
	public static final int storeString (final Memory mem, final int addr, String value) {
		System.out.println("memory store string " + value);
		return storeBytes(mem, addr, (value + "\0").getBytes(StandardCharsets.US_ASCII));
	}
	
	/** return pointer to first byte after data */
	public static final int storeBytes (final Memory mem, final int addr, final byte[] data) {
		System.out.println("memory store bytes " + data.length);
		if (data.length > 0) {
			if ((addr & 3) == 0 && (data.length & 3) == 0) {
				storeBytesAsWords(mem, addr, data, 0, data.length);
				
			} else {
				System.out.println("store " + data.length + " bytes as bytes");
				// slow...
				for (int n = 0; n < data.length; n++) {
					mem.storeByte(addr + n, data[n]);
				}
			}
			return addr + data.length;
		} else {
			throw new IllegalArgumentException("zero length");
		}
	}
	
	private static void storeBytesAsWords (final Memory mem, final int addr, byte[] data, int offset, int len) {
		System.out.println("memory store bytes as words " + len);
		if ((addr & 3) == 0 && (len & 3) == 0) {
			for (int n = offset; n < (offset + len); n += 4) {
				final int b1 = data[n] & 0xff;
				final int b2 = data[n + 1] & 0xff;
				final int b3 = data[n + 2] & 0xff;
				final int b4 = data[n + 3] & 0xff;
				mem.storeWord(addr + n, (b1 << 24) | (b2 << 16) | (b3 << 8) | b4);
			}
		} else {
			throw new IllegalArgumentException("unaligned");
		}
	}
	
	public static int byteswap(final int x) {
		// abcd -> dcba
		return (x & 0xff) << 24 | (x & 0xff00) << 8 | (x & 0xff0000) >>> 8 | (x & 0xff000000) >>> 24;
	}
	
	private MemoryUtil () {
		//
	}
}
