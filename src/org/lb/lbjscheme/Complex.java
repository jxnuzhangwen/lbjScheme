// lbjScheme
// An experimental Scheme subset interpreter in Java, based on SchemeNet.cs
// Copyright (c) 2013, Leif Bruder <leifbruder@gmail.com>
//
// Permission to use, copy, modify, and/or distribute this software for any
// purpose with or without fee is hereby granted, provided that the above
// copyright notice and this permission notice appear in all copies.
//
// THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
// WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
// MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
// ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
// WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
// ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
// OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.

package org.lb.lbjscheme;

public final class Complex extends SchemeNumber {
	private final SchemeNumber _real;
	private final SchemeNumber _imag;

	private Complex(SchemeNumber real, SchemeNumber imag) {
		_real = real;
		_imag = imag;
	}

	public Complex(Real real) {
		_real = real;
		_imag = Fixnum.valueOf(0);
	}

	@Override
	public int getLevel() {
		return 5; // 1 = Fixnum, 2 = Bignum, 3 = Rational, 4 = Real, 5 = Complex
	}

	@Override
	public String toString(boolean forDisplay, int base) throws SchemeException {
		assertBaseTen(base);
		return _real.toString()
				+ (_imag.compareTo(Fixnum.valueOf(0)) < 0 ? "" : "+")
				+ _imag.toString() + "i";
	}

	private static void assertBaseTen(int base) throws SchemeException {
		if (base != 10)
			throw new SchemeException(
					"Complex numbers may only be converted from or to string in base 10");
	}

	@Override
	public SchemeNumber promote() {
		return null; // Not possible
	}

	public static SchemeNumber valueOf(String value, int base)
			throws SchemeException {
		assertBaseTen(base);

		if (!value.endsWith("i"))
			throw new SchemeException("Value can not be converted to a complex");
		int pos = value.indexOf('+');
		if (pos == -1)
			pos = value.indexOf('-');
		if (pos == -1)
			throw new SchemeException("Value can not be converted to a complex");

		final String realPart = value.substring(0, pos);
		final String imagPart = value.substring(pos, value.length() - 1);

		return valueOf(SchemeNumber.fromString(realPart, 10),
				SchemeNumber.fromString(imagPart, 10));
	}

	public static SchemeNumber valueOf(SchemeNumber real, SchemeNumber imag) {
		if (imag.isExact() && imag.isZero())
			return real;
		else
			return new Complex(real, imag);
	}

	@Override
	public SchemeNumber getNumerator() {
		return null; // TODO: Throw exception: Rational expected
	}

	@Override
	public SchemeNumber getDenominator() {
		return null; // TODO: Throw exception: Rational expected
	}

	@Override
	protected SchemeNumber doAdd(SchemeNumber other) {
		Complex o = (Complex) other;
		// TODO: (+ 1+2i 100+200i) => 101+202i. Can of worms: If both real and
		// imag are exact, don't use promote() that way...
		// Better: promoteToLevel(5)? Or something entirely different? Think!
		return valueOf(_real.add(o._real), _imag.add(o._imag));
	}

	@Override
	public SchemeNumber doSub(SchemeNumber other) {
		Complex o = (Complex) other;
		return valueOf(_real.sub(o._real), _imag.sub(o._imag));
	}

	@Override
	public SchemeNumber doMul(SchemeNumber other) {
		Complex o = (Complex) other;
		return valueOf(_real.mul(o._real).sub(_imag.mul(o._imag)),
				_real.mul(o._imag).add(_imag.mul(o._real)));
	}

	@Override
	public SchemeNumber doDiv(SchemeNumber other) {
		return mul(((Complex) other).recip());
	}

	private SchemeNumber recip() {
		final SchemeNumber realSq = _real.mul(_real);
		final SchemeNumber imagSq = _imag.mul(_imag);
		final SchemeNumber commonDenominator = realSq.add(imagSq);
		final SchemeNumber newReal = _real.div(commonDenominator);
		final SchemeNumber newImag = Fixnum.valueOf(0).sub(_imag)
				.div(commonDenominator);
		return valueOf(newReal, newImag);
	}

	@Override
	public SchemeNumber doIdiv(SchemeNumber other) throws SchemeException {
		throw new SchemeException("quotient: Integer expected");
	}

	@Override
	public SchemeNumber doMod(SchemeNumber other) throws SchemeException {
		throw new SchemeException("remainder: Integer expected");
	}

	@Override
	public boolean isZero() {
		// No Complex can ever be zero, as it would be converted to a Fixnum
		// on the fly.
		return false;
	}

	@Override
	protected int doCompareTo(SchemeNumber other) {
		// BigInteger diff = _n.multiply(((Complex) other)._d).subtract(
		// _d.multiply(((Complex) other)._n));
		return 0; // TODO
	}

	@Override
	public SchemeNumber roundToNearestInteger() {
		return this; // TODO: Throw exception: Rational expected
	}

	@Override
	public SchemeNumber sqrt() throws SchemeException {
		return this; // TODO
	}

	@Override
	public SchemeNumber floor() {
		return this; // TODO: Throw exception: Rational expected
	}

	@Override
	public SchemeNumber ceiling() {
		return this; // TODO: Throw exception: Rational expected
	}

	@Override
	public SchemeNumber truncate() {
		return this; // TODO: Throw exception: Rational expected
	}

	@Override
	public SchemeNumber round() {
		return this; // TODO: Throw exception: Rational expected
	}

	public SchemeNumber getRealPart() {
		return _real;
	}

	public SchemeNumber getImagPart() {
		return _imag;
	}
}
