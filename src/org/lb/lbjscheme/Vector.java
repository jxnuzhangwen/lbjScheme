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

import java.util.*;

public final class Vector implements SchemeObject {
	private final SchemeObject[] _values;

	public Vector(int length) {
		_values = new SchemeObject[length];
	}

	public Vector(SchemeList items) {
		List<SchemeObject> asList = new ArrayList<SchemeObject>();
		for (SchemeObject o : items)
			asList.add(o);
		_values = asList.toArray(new SchemeObject[0]);
	}

	public int getLength() {
		return _values.length;
	}

	public SchemeObject getAt(int position) {
		return _values[position];
	}

	public void setAt(int position, SchemeObject value) {
		_values[position] = value;
	}

	@Override
	public String toString() {
		StringBuilder ret = new StringBuilder();
		ret.append("#(");
		for (SchemeObject c : _values) {
			ret.append(c.toString());
			ret.append(" ");
		}

		if (ret.length() > 2)
			ret.setCharAt(ret.length() - 1, ')');
		else
			ret.append(')');

		return ret.toString();
	}
}