package sys.mips;

/** represents a field in a system coprocessor register */
public final class Constant {
	
	public final int register;
	public final int leftShift;
	public final int size;
	public final int max;
	public final int mask;
	
	public Constant (int register, int leftShift, int size) {
		if (register >= 0 && register < 64 && leftShift >= 0 && leftShift < 32 && size > 0 && size <= 32 && leftShift + size <= 32) {
			this.register = register;
			this.leftShift = leftShift;
			this.size = size;
			this.max = (-1 >>> (32 - size));
			this.mask = max << leftShift;
		} else {
			throw new RuntimeException();
		}
	}
	
	public final void set (int[] regs, int x) {
		if (size == 32 || (x >= 0 && x <= max)) {
			regs[register] = (regs[register] & ~mask) | (x << leftShift);
		} else {
			throw new RuntimeException("value " + x + " max " + max);
		}
	}
	
	public final void set (int[] regs, boolean x) {
		set(regs, x ? 1 : 0);
	}
	
	public final int get (int[] regs) {
		return (regs[register] & mask) >>> leftShift;
	}
	
	public final boolean isSet (int[] regs) {
		return (regs[register] & mask) != 0;
	}

	@Override
	public String toString () {
		return "Constant [reg=" + register + " lsh=" + leftShift + " size=" + size + " max=" + Integer.toHexString(max) + " mask=" + Integer.toHexString(mask) + "]";
	}
	
	
}
