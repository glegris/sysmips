package sys.mips;

import static sys.mips.CpuConstants.*;
import static sys.mips.CpuFunctions.*;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import sys.util.Symbols;

/**
 * disassembly and Isn utils
 */
public class InstructionUtil {

	private static final Instruction NOP = new Instruction("nop");

	/** names of general registers */
	private static final String[] REG_NAMES = new String[] { 
		"zero", "at", "v0", "v1", 
		"a0", "a1", "a2", "a3",
		// 8
		"t0", "t1", "t2", "t3", 
		"t4", "t5", "t6", "t7",
		// 16
		"s0", "s1", "s2", "s3", 
		"s4", "s5", "s6", "s7", 
		// 24
		"t8", "t9", "k0", "k1", 
		"gp", "sp", "s8", "ra"
	};

	private static final String[][] CP_REG_NAMES = new String[][] { 
		new String[] { "Index", "MVPControl", "MVPConf0", "MVPConf1" },
		new String[] { "Random", "VPEControl", "VPEConf0", "VPEConf1", "YQMask", "VPESchedule", "VPEScheFBack", "VPEOpt" },
		new String[] { "EntryLo0", "TCStatus", "TCBind", "TCRestart", "TCHalt", "TCContext", "TCSchedule", "TCScheFBack" },
		new String[] { "EntryLo1", null, null, null, null, null, null, "TCOpt", },
		new String[] { "Context", "ContentConfig", "UserLocal" },
		new String[] { "PageMask", "PageGrain", "SegCtl0", "SegCtl1", "SegCtl2", "PWBase", "PWField", "PWSize" },
		new String[] { "Wired", "SRSConf0", "SRSConf1", "SRSConf2", "SRSConf3", "SRSConf4", "PWCtl" }, new String[] { "HWREna" },
		new String[] { "BadVaddr", "BadInstr", "BadInstrP", }, 
		new String[] { "Count" },
		new String[] { "EntryHi", null, null, null, "GuestCtl1", "GuestCtl2", "GuestCtl3" }, 
		new String[] { "Compare", null, null, null, "GuestCtl0Ext" },
		new String[] { "Status", "IntCtl", "SRSCtl", "SRSMap", "View_IPL", "SRSMap2", "GuestCtl0", "GTOffset" },
		new String[] { "Cause", null, null, null, "View_RIPL", "NestedExc", }, 
		new String[] { "EPC", null, "NestedEPC" },
		new String[] { "PRId", "EBase", "CDMMBase", "CMGCRBase" },
		new String[] { "Config", "Config1", "Config2", "Config3", "Config4", "Config5" }, 
	};
	
	//
	// methods
	//

	public static String opString (final int op) {
		return "hex=" + Integer.toHexString(op) + " dec=" + op + " oct=" + Integer.toOctalString(op);
	}
	
	/** convert rs meta instruction to d, s, w, or l */
	public static String fpFormatString (final int rs) {
		switch (rs) {
			case FP_RS_D:
				return "d";
			case FP_RS_S:
				return "s";
			case FP_RS_W:
				return "w";
			case FP_RS_L:
				return "l";
			default:
				throw new RuntimeException("unknown fp format " + rs);
		}
	}

	public static String cpRegName (int rd, int sel) {
		String name = "unknown";
		if (rd < CP_REG_NAMES.length) {
			final String[] rdnames = CP_REG_NAMES[rd];
			if (rdnames != null && sel < rdnames.length) {
				name = rdnames[sel];
			}
		}
		return rd + ", " + sel + ": " + name;
	}
	
	public static String gpRegName (int reg) {
		return REG_NAMES[reg];
	}
	
	public static String fpRegName (int reg) {
		return "f" + reg;
	}
	
	/**
	 * Disassemble an instruction
	 */
	public static String isnString (final Cpu cpu, final int isn) {
		final int op = op(isn);
		final int rs = rs(isn);
		final int rt = rt(isn);
		final int fn = fn(isn);
		final int rd = rd(isn);
		
		Instruction isnObj;
		if (op == OP_SPECIAL && fn == FN_SLL && rd == 0) {
			// pretend it's a no-op
			isnObj = NOP;
		} else {
			isnObj = InstructionSet.getInstance().getIsn(isn);
		}
		
		final String isnValue;
		if (isnObj != null) {
			isnValue = InstructionUtil.formatIsn(cpu, isn, isnObj);
		} else {
			isnValue = "op=" + op + " rt=" + rt + " rs=" + rs + " fn=" + fn;
		}
		
		final String addr = cpu.getSymbols().getNameAddrOffset(cpu.getPc());
		return String.format("%-40s %08x %s", addr, isn, isnValue);
	}
	
	private static String formatIsn (final Cpu cpu, final int isn, final Instruction isnObj) {
		final StringBuilder sb = new StringBuilder(isnObj.name);
		while (sb.length() < 8) {
			sb.append(" ");
		}
		sb.append(isnObj.format);
		int i;
		while ((i = sb.indexOf("{")) >= 0) {
			final int j = sb.indexOf("}", i);
			if (j > i) {
				sb.replace(i, j + 1, String.valueOf(formatCode(cpu, isn, sb.substring(i + 1, j))));
			} else {
				throw new RuntimeException("invalid format " + isnObj.format);
			}
		}
		return sb.toString();
	}
	
	/** get value of format code */
	private static String formatCode (final Cpu cpu, final int isn, final String name) {
		final Fpu fpu = cpu.getFpu();
		final Memory mem = cpu.getMemory();
		final int[] reg = cpu.getRegisters();
		final int pc = cpu.getPc();
		final Symbols syms = cpu.getSymbols();
		final int[] cpreg = cpu.getCpRegisters();
		
		switch (name) {
			case "fr":
				return fpRegName(fr(isn));
			case "ft":
				return fpRegName(ft(isn));
			case "fd":
				return fpRegName(fd(isn));
			case "fs":
				return fpRegName(fs(isn));
			case "regft":
				return formatDouble(fpu.getFpRegister(ft(isn), FpFormat.getInstance(fmt(isn))));
			case "regfs":
				return formatDouble(fpu.getFpRegister(fs(isn), FpFormat.getInstance(fmt(isn))));
			case "regfss":
				return formatDouble(fpu.getFpRegister(fs(isn), FpFormat.SINGLE));
			case "regfrs":
				return formatDouble(fpu.getFpRegister(fr(isn), FpFormat.SINGLE));
			case "regfd":
				return formatDouble(fpu.getFpRegister(fd(isn), FpFormat.getInstance(fmt(isn))));
			case "regfts":
				return formatDouble(fpu.getFpRegister(ft(isn), FpFormat.SINGLE));
			case "regftd":
				return formatDouble(fpu.getFpRegister(ft(isn), FpFormat.DOUBLE));
			case "regfsx": {
				int v = fpu.getFpRegister(fs(isn));
				return "0x" + Integer.toHexString(v) + "(" + formatDouble(Float.intBitsToFloat(v)) + ")";
			}
			case "rs":
				return gpRegName(rs(isn));
			case "rd":
				return gpRegName(rd(isn));
			case "rt":
				return gpRegName(rt(isn));
			case "regrtx": {
				int v = reg[rt(isn)];
				return Integer.toHexString(v) + "(" + formatDouble(Float.intBitsToFloat(v)) + ")";
			}
			case "base":
				return gpRegName(base(isn));
			case "regrd":
				return Integer.toHexString(reg[rd(isn)]);
				//return syms.getName(reg[rd(isn)]);
			case "regrt":
				return Integer.toHexString(reg[rt(isn)]);
				//return syms.getName(reg[rt(isn)]);
			case "regrs":
				return Integer.toHexString(reg[rs(isn)]);
				//return syms.getName(reg[rs(isn)]);
			case "offset":
				return Integer.toString(simm(isn));
			case "cprd":
				return cpRegName(rd(isn), sel(isn));
			case "cpregrd":
				return Integer.toHexString(cpreg[cprIndex(rd(isn), sel(isn))]);
			case "imm":
				return String.valueOf(simm(isn));
			case "branch":
				return syms.getNameAddrOffset(branch(isn, pc));
			case "hi":
				return Integer.toHexString(cpu.getHi());
			case "lo":
				return Integer.toHexString(cpu.getLo());
			case "hilo":
				return String.valueOf(cpu.getHilo());
			case "baseoffset":
				return syms.getNameAddrOffset(reg[base(isn)] + simm(isn));
			case "syscall":
				return Integer.toHexString(syscall(isn));
			case "membaseoffset": {
				Integer w = mem.loadWordSafe((reg[base(isn)] + simm(isn)));
				return w != null ? Integer.toHexString(w) : null;
			}
			case "membaseoffsets": {
				Integer w = mem.loadWordSafe((reg[base(isn)] + simm(isn)));
				if (w != null) {
					return formatDouble(Float.intBitsToFloat(w));
				}
				return null;
			}
			case "membaseoffsetd": {
				Long dw = mem.loadDoubleWordSafe((reg[base(isn)] + simm(isn)));
				if (dw != null) {
					return formatDouble(Double.longBitsToDouble(dw));
				}
				return null;
			}
			case "jump":
				return syms.getNameAddrOffset(jump(isn, pc));
			case "sa":
				return String.valueOf(sa(isn));
			case "fptf":
				return fptf(isn) ? "t" : "f";
			case "fpcc":
				return "cc" + fpcc(isn);
			case "regfpcc":
				return String.valueOf(fccrFcc(fpu.getFpControlReg(), fpcc(isn)));
			default:
				throw new RuntimeException("unknown name " + name);
		}
	}
	
	private static String formatDouble(double d) {
		return String.format("%.6f", d);
	}
	
	public static String exceptionString (int ex) {
		return InstructionUtil.lookup(CpuConstants.class, "EX_", ex);
	}
	
	/**
	 * lookup constant name
	 */
	public static String lookup (Class<?> c, String prefix, int value) {
		for (final Field f : c.getFields()) {
			String name = f.getName();
			if (name.startsWith(prefix)) {
				final int m = f.getModifiers();
				if (Modifier.isPublic(m) && Modifier.isStatic(m) && Modifier.isFinal(m) && f.getType().isAssignableFrom(int.class)) {
					try {
						if (f.getInt(null) == value) {
							return name;
						}
					} catch (final Exception e) {
						throw new RuntimeException(e);
					}
				}
			}
		}
		return null;
	}
		
}
