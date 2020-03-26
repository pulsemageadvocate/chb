package pulad.chb.util;

//package org.monazilla.v2c;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class V2CSHA1Value {
	final MessageDigest mdSHA1;
	int iHV;
	long lMV;
	long lLV;
	private static final int[] ilHC2I = new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, -1, -1, -1, -1, -1, -1, -1, 10, 11, 12,
			13, 14, 15, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
			-1, -1, 10, 11, 12, 13, 14, 15};
	private static final int[] ilB32HC2I = new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, -1, -1, -1, -1, -1, -1, -1, 10, 11,
			12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31};

	public static V2CSHA1Value createInstance() {
		MessageDigest var0;
		try {
			var0 = MessageDigest.getInstance("SHA-1");
		} catch (NoSuchAlgorithmException var2) {
			return null;
		}

		return new V2CSHA1Value(var0);
	}

	public V2CSHA1Value() {
		this((MessageDigest) null);
	}

	public V2CSHA1Value(V2CSHA1Value var1) {
		this(var1.iHV, var1.lMV, var1.lLV);
	}

	public V2CSHA1Value(int var1, long var2, long var4) {
		this((MessageDigest) null);
		this.iHV = var1;
		this.lMV = var2;
		this.lLV = var4;
	}

	private V2CSHA1Value(MessageDigest var1) {
		this.mdSHA1 = var1;
	}

	public void reset() {
		this.mdSHA1.reset();
	}

	public void update(byte[] var1, int var2, int var3) {
		this.mdSHA1.update(var1, var2, var3);
	}

	public void digest() {
		byte[] var1 = this.mdSHA1.digest();
		this.iHV = (var1[0] & 255) << 24 | (var1[1] & 255) << 16 | (var1[2] & 255) << 8 | var1[3] & 255;
		int var2 = (var1[4] & 255) << 24 | (var1[5] & 255) << 16 | (var1[6] & 255) << 8 | var1[7] & 255;
		int var3 = (var1[8] & 255) << 24 | (var1[9] & 255) << 16 | (var1[10] & 255) << 8 | var1[11] & 255;
		this.lMV = (long) var2 << 32 | (long) var3 & 4294967295L;
		int var4 = (var1[12] & 255) << 24 | (var1[13] & 255) << 16 | (var1[14] & 255) << 8 | var1[15] & 255;
		int var5 = (var1[16] & 255) << 24 | (var1[17] & 255) << 16 | (var1[18] & 255) << 8 | var1[19] & 255;
		this.lLV = (long) var4 << 32 | (long) var5 & 4294967295L;
	}

	private static int hexCharToInt(char var0) {
		return var0 >= '0' && var0 <= 'f' ? ilHC2I[var0 - 48] : -1;
	}

	private static int b32HCharToInt(char var0) {
		return var0 >= '0' && var0 <= 'V' ? ilB32HC2I[var0 - 48] : -1;
	}

	public boolean parseB32H(String var1) {
		int var2 = 0;

		int var3;
		int var4;
		for (var4 = 0; var4 < 6; ++var4) {
			var3 = b32HCharToInt(var1.charAt(var4));
			if (var3 < 0) {
				return false;
			}

			var2 = var2 << 5 | var3;
		}

		var3 = b32HCharToInt(var1.charAt(6));
		if (var3 < 0) {
			return false;
		} else {
			this.iHV = var2 << 2 | var3 >>> 3;
			var2 = var3 & 7;

			for (var4 = 7; var4 < 12; ++var4) {
				var3 = b32HCharToInt(var1.charAt(var4));
				if (var3 < 0) {
					return false;
				}

				var2 = var2 << 5 | var3;
			}

			var3 = b32HCharToInt(var1.charAt(12));
			if (var3 < 0) {
				return false;
			} else {
				var4 = var2 << 4 | var3 >>> 1;
				var2 = var3 & 1;

				int var5;
				for (var5 = 13; var5 < 19; ++var5) {
					var3 = b32HCharToInt(var1.charAt(var5));
					if (var3 < 0) {
						return false;
					}

					var2 = var2 << 5 | var3;
				}

				var3 = b32HCharToInt(var1.charAt(19));
				if (var3 < 0) {
					return false;
				} else {
					this.lMV = (long) var4 << 32 | (long) var2 << 1 | (long) (var3 >>> 4);
					var2 = var3 & 15;

					for (var5 = 20; var5 < 25; ++var5) {
						var3 = b32HCharToInt(var1.charAt(var5));
						if (var3 < 0) {
							return false;
						}

						var2 = var2 << 5 | var3;
					}

					var3 = b32HCharToInt(var1.charAt(25));
					if (var3 < 0) {
						return false;
					} else {
						var5 = var2 << 3 | var3 >>> 2;
						var2 = var3 & 3;

						for (int var6 = 26; var6 < 32; ++var6) {
							var3 = b32HCharToInt(var1.charAt(var6));
							if (var3 < 0) {
								return false;
							}

							var2 = var2 << 5 | var3;
						}

						this.lLV = (long) var5 << 32 | (long) var2 & 4294967295L;
						return true;
					}
				}
			}
		}
	}

	public boolean parse(String var1) {
		if (var1 != null && var1.length() >= 40) {
			int var2 = 0;

			int var3;
			int var4;
			for (var3 = 0; var2 < 8; ++var2) {
				var4 = hexCharToInt(var1.charAt(var2));
				if (var4 < 0) {
					return false;
				}

				var3 = var3 << 4 | var4;
			}

			this.iHV = var3;

			int var5;
			for (var4 = 0; var2 < 16; ++var2) {
				var5 = hexCharToInt(var1.charAt(var2));
				if (var5 < 0) {
					return false;
				}

				var4 = var4 << 4 | var5;
			}

			int var6;
			for (var5 = 0; var2 < 24; ++var2) {
				var6 = hexCharToInt(var1.charAt(var2));
				if (var6 < 0) {
					return false;
				}

				var5 = var5 << 4 | var6;
			}

			this.lMV = (long) var4 << 32 | (long) var5 & 4294967295L;

			int var7;
			for (var6 = 0; var2 < 32; ++var2) {
				var7 = hexCharToInt(var1.charAt(var2));
				if (var7 < 0) {
					return false;
				}

				var6 = var6 << 4 | var7;
			}

			for (var7 = 0; var2 < 40; ++var2) {
				int var8 = hexCharToInt(var1.charAt(var2));
				if (var8 < 0) {
					return false;
				}

				var7 = var7 << 4 | var8;
			}

			this.lLV = (long) var6 << 32 | (long) var7 & 4294967295L;
			return true;
		} else {
			return false;
		}
	}

	public int compareTo(Object var1) {
		V2CSHA1Value var2 = (V2CSHA1Value) var1;
		int var3 = this.iHV;
		int var4 = var2.iHV;
		if (var3 > var4) {
			return 1;
		} else if (var3 < var4) {
			return -1;
		} else {
			long var5 = this.lMV;
			long var7 = var2.lMV;
			if (var5 > var7) {
				return 1;
			} else if (var5 < var7) {
				return -1;
			} else {
				long var9 = this.lLV;
				long var11 = var2.lLV;
				if (var9 > var11) {
					return 1;
				} else {
					return var9 < var11 ? -1 : 0;
				}
			}
		}
	}

	public int hashCode() {
		return (int) this.lMV;
	}

	public boolean equals(Object var1) {
		if (!(var1 instanceof V2CSHA1Value)) {
			return false;
		} else {
			V2CSHA1Value var2 = (V2CSHA1Value) var1;
			return this.iHV == var2.iHV && this.lMV == var2.lMV && this.lLV == var2.lLV;
		}
	}

	public static String toString(int var0, long var1, long var3) {
		char[] var5 = new char[40];

		int var6;
		int var7;
		for (var6 = 0; var6 < 8; ++var6) {
			var7 = var0 & 15;
			if (var7 < 10) {
				var5[7 - var6] = (char) (var7 + 48);
			} else {
				var5[7 - var6] = (char) (var7 + 87);
			}

			var0 >>>= 4;
		}

		for (var6 = 0; var6 < 16; ++var6) {
			var7 = (int) (var1 & 15L);
			if (var7 < 10) {
				var5[23 - var6] = (char) (var7 + 48);
			} else {
				var5[23 - var6] = (char) (var7 + 87);
			}

			var1 >>>= 4;
		}

		for (var6 = 0; var6 < 16; ++var6) {
			var7 = (int) (var3 & 15L);
			if (var7 < 10) {
				var5[39 - var6] = (char) (var7 + 48);
			} else {
				var5[39 - var6] = (char) (var7 + 87);
			}

			var3 >>>= 4;
		}

		return new String(var5);
	}

	public String toString() {
		return toString(this.iHV, this.lMV, this.lLV);
	}

	public static String toB32HString(int var0, long var1, long var3) {
		char[] var5 = new char[32];
		fillB32HChars(var0, var1, var3, var5);
		return new String(var5);
	}

	public static void fillB32HChars(int var0, long var1, long var3, char[] var5) {
		int var6 = appendB32HByte(var5, (int) var3, 0, 0);
		var6 = appendB32HByte(var5, (int) (var3 >>> 32), var6, 1);
		var6 = appendB32HByte(var5, (int) var1, var6, 2);
		var6 = appendB32HByte(var5, (int) (var1 >>> 32), var6, 3);
		appendB32HByte(var5, var0, var6, 4);
	}

	public static int appendB32HByte(char[] var0, int var1, int var2, int var3) {
		var3 *= 32;
		int var4 = 31 - var3 / 5;
		int var5 = var3 % 5;
		if (var5 > 0) {
			var0[var4--] = toB32HChar(var1 << var5 | var2);
			var5 = 5 - var5;
			var1 >>>= var5;
			var5 = 32 - var5;
		} else {
			var5 = 32;
		}

		while (var5 >= 5) {
			var0[var4--] = toB32HChar(var1);
			var1 >>>= 5;
			var5 -= 5;
		}

		return var1;
	}

	public static char toB32HChar(int var0) {
		var0 &= 31;
		return var0 < 10 ? (char) (48 + var0) : (char) (var0 + 55);
	}
}
