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
import org.lb.lbjscheme.builtins.*;

public final class Environment extends SchemeObject {
	private final Environment _outer;
	private final HashMap<Symbol, SchemeObject> _values = new HashMap<>();
	private boolean _locked;
	private InputPort _currentInputPort;
	private OutputPort _currentOutputPort;

	public Environment() {
		_outer = null;
	}

	public Environment(Environment outer) {
		_outer = outer;
	}

	@Override
	public String toString(boolean forDisplay) {
		return "<environment>";
	}

	public SchemeObject get(Symbol name) throws SchemeException {
		if (_values.containsKey(name)) return _values.get(name);
		if (_outer != null) return _outer.get(name);
		throw new SchemeException("Unknown symbol " + name.toString());
	}

	public void define(Symbol name, SchemeObject value) throws SchemeException {
		assertSymbolMayBeRedefined(name);
		_values.put(name, value);
	}

	private void assertSymbolMayBeRedefined(Symbol name) throws SchemeException {
		if (!mayBeRedefined(name))
			throw new SchemeException("Symbol '" + name.toString()
					+ "' is constant and must not be changed");
	}

	private boolean mayBeRedefined(Symbol name) {
		final String nameAsString = name.toString();

		switch (nameAsString) {
		case "if":
		case "define":
		case "set!":
		case "lambda":
		case "quote":
		case "begin":
			return false;
		default:
			if (nameAsString.startsWith("##gensym##")) return true;
			if (_locked && nameAsString.startsWith("##")
					&& _values.containsKey(name)) return false;
			return true;
		}
	}

	public void set(Symbol name, SchemeObject value) throws SchemeException {
		assertSymbolMayBeRedefined(name);

		if (_values.containsKey(name))
			_values.put(name, value);
		else if (_outer != null)
			_outer.set(name, value);
		else
			throw new SchemeException("Unknown symbol " + name.toString());
	}

	public Set<Symbol> getDefinedSymbols() {
		return _values.keySet();
	}

	public void expand(List<Symbol> parameterNames, boolean hasRestParameter,
			List<SchemeObject> parameters) throws SchemeException {
		if (hasRestParameter) {
			if (parameterNames.size() - 1 > parameters.size())
				throw new SchemeException(
						"Invalid parameter count: Expected at least "
								+ (parameterNames.size() - 1) + ", got "
								+ parameters.size());
			for (int i = 0; i < parameterNames.size() - 1; ++i)
				define(parameterNames.get(i), parameters.get(i));
			define(parameterNames.get(parameterNames.size() - 1),
					Pair.fromIterable(parameters.subList(
							parameterNames.size() - 1, parameters.size())));
		} else {
			if (parameterNames.size() != parameters.size())
				throw new SchemeException("Invalid parameter count: Expected "
						+ parameterNames.size() + ", got " + parameters.size());
			for (int i = 0; i < parameters.size(); ++i)
				define(parameterNames.get(i), parameters.get(i));
		}
	}

	public InputPort getInputPort() {
		return _outer == null ? _currentInputPort : _outer.getInputPort();
	}

	public OutputPort getOutputPort() {
		return _outer == null ? _currentOutputPort : _outer.getOutputPort();
	}

	public void setInputPort(InputPort value) {
		if (_outer == null)
			_currentInputPort = value;
		else
			_outer.setInputPort(value);
	}

	public void setOutputPort(OutputPort value) {
		if (_outer == null)
			_currentOutputPort = value;
		else
			_outer.setOutputPort(value);
	}

	public static Environment newNullEnvironment(final int version)
			throws SchemeException {
		if (version != 5)
			throw new SchemeException(
					"null-environment: Only version 5 supported");
		return new Environment(); // TODO: Syntax transformers...
	}

	public static Environment newReportEnvironment(final int version,
			final Environment global) throws SchemeException {
		if (version != 5)
			throw new SchemeException(
					"scheme-report-environment: Only version 5 supported");
		final Environment ret = new Environment(global);
		ret.addBuiltins();
		new InterpretingEvaluator(ret).eval(getReportInitScript());
		ret.addRedefinableBuiltins();
		ret.lock();
		return ret;
	}

	public static Environment newInteractionEnvironment(final Environment global)
			throws SchemeException {
		final Environment ret = new Environment(global);
		ret.addBuiltins();
		new InterpretingEvaluator(ret).eval(getInteractionInitScript());
		ret.addRedefinableBuiltins();
		ret.lock();
		return ret;
	}

	public static String getReportInitScript() {
		return _reportInitScript;
	}

	public static String getInteractionInitScript() {
		return _reportInitScript + " " + _interactionInitScript;
	}

	public void lock() {
		_locked = true;
	}

	public void unlock() {
		_locked = false;
	}

	public void addBuiltins() throws SchemeException {
		addBuiltin(new Acos());
		addBuiltin(new Add());
		addBuiltin(new Asin());
		addBuiltin(new Atan());
		addBuiltin(new BooleanP());
		addBuiltin(new Car());
		addBuiltin(new Cdr());
		addBuiltin(new Ceiling());
		addBuiltin(new CharP());
		addBuiltin(new CharReadyP(this));
		addBuiltin(new CharToInteger());
		addBuiltin(new CloseInputPort());
		addBuiltin(new CloseOutputPort());
		addBuiltin(new Cons());
		addBuiltin(new Cos());
		addBuiltin(new CurrentInputPort(this));
		addBuiltin(new CurrentOutputPort(this));
		addBuiltin(new Denominator());
		addBuiltin(new Display(this));
		addBuiltin(new Div());
		addBuiltin(new EofObjectP());
		addBuiltin(new EqP());
		addBuiltin(new Eval());
		addBuiltin(new ExactP());
		addBuiltin(new ExactToInexact());
		addBuiltin(new Exp());
		addBuiltin(new Floor());
		addBuiltin(new Ge());
		addBuiltin(new Gt());
		addBuiltin(new ImagPart());
		addBuiltin(new InexactP());
		addBuiltin(new InexactToExact());
		addBuiltin(new InputPortP());
		addBuiltin(new IntegerP());
		addBuiltin(new IntegerToChar());
		addBuiltin(new InteractionEnvironment(this));
		addBuiltin(new Le());
		addBuiltin(new Log());
		addBuiltin(new Lt());
		addBuiltin(new MakeString());
		addBuiltin(new MakeVector());
		addBuiltin(new Mul());
		addBuiltin(new NullEnvironment());
		addBuiltin(new NullP());
		addBuiltin(new NumberP());
		addBuiltin(new NumberToString());
		addBuiltin(new NumEq());
		addBuiltin(new Numerator());
		addBuiltin(new OpenInputFile());
		addBuiltin(new OpenOutputFile());
		addBuiltin(new OutputPortP());
		addBuiltin(new PairP());
		addBuiltin(new PeekChar(this));
		addBuiltin(new ProcedureP());
		addBuiltin(new Quotient());
		addBuiltin(new Rationalize());
		addBuiltin(new RationalP());
		addBuiltin(new Read(this));
		addBuiltin(new ReadChar(this));
		addBuiltin(new RealP());
		addBuiltin(new RealPart());
		addBuiltin(new Remainder());
		addBuiltin(new Round());
		addBuiltin(new SchemeReportEnvironment(this));
		addBuiltin(new SetCar());
		addBuiltin(new SetCdr());
		addBuiltin(new Sin());
		addBuiltin(new StringLength());
		addBuiltin(new StringP());
		addBuiltin(new StringRef());
		addBuiltin(new StringSet());
		addBuiltin(new StringToNumber());
		addBuiltin(new StringToSymbol());
		addBuiltin(new Sub());
		addBuiltin(new SymbolP());
		addBuiltin(new SymbolToString());
		addBuiltin(new SysCall());
		addBuiltin(new SysError());
		addBuiltin(new SysExpt());
		addBuiltin(new SysGetMethodNames());
		addBuiltin(new SysSetCurrentInputPort(this));
		addBuiltin(new SysNew());
		addBuiltin(new SysSetCurrentOutputPort(this));
		addBuiltin(new SysSqrt());
		addBuiltin(new Tan());
		addBuiltin(new Truncate());
		addBuiltin(new VectorLength());
		addBuiltin(new VectorP());
		addBuiltin(new VectorRef());
		addBuiltin(new VectorSet());
		addBuiltin(new Write(this));
		addBuiltin(new WriteChar(this));
		addBuiltin(new ZeroP());
	}

	private void addBuiltin(Builtin builtin) throws SchemeException {
		final Symbol builtinSymbol = Symbol.fromString(builtin.getName());
		if (_values.containsKey(builtinSymbol))
			throw new SchemeException("Internal error: Builtin '"
					+ builtin.getName() + "' is being defined twice!");
		define(builtinSymbol, builtin);
	}

	public void addRedefinableBuiltins() {
		for (Symbol s : getDefinedSymbols().toArray(new Symbol[0])) {
			if (s.toString().startsWith("##")) {
				try {
					define(Symbol.fromString(s.toString().substring(2)), get(s));
				} catch (SchemeException e) {
					throw new RuntimeException(
							"Impossible exception thrown while generating environment");
				}
			}
		}
	}

	// HACK: Written in Scheme for simplicity. Re-write as builtins for
	// performance and better error messages! This code is horribly inefficient!

	private final static String _interactionInitScript = "(define (flip f) (lambda (a b) (f b a)))"
			+ "(define (##reduce f ridentity lst) (if (##null? lst) ridentity (##fold f (##car lst) (##cdr lst))))"
			+ "(define (##last-pair lst) (if (##null? (##cdr lst)) lst (##last-pair (##cdr lst))))"
			+ "(define (##last lst) (##car (##last-pair lst)))"
			+ "(define (##dotted-list? lst) (if (##null? lst) #f (if (##pair? lst) (##dotted-list? (##cdr lst)) #t)))"
			+ "(define ##drop ##list-tail)"
			+ "(define (##filter f lst) (define (iter l acc) (if (##null? l) (##reverse acc) (if (f (##car l)) (iter (##cdr l) (##cons (##car l) acc)) (iter (##cdr l) acc)))) (iter lst '()))"
			+ "(define (##range from to) (define (iter t acc) (if (##> from t) acc (iter (##- t 1) (##cons t acc)))) (iter to '()))"
			+ "(define (##find-tail f lst) (if (##null? lst) #f (if (f (##car lst)) lst (##find-tail f (##cdr lst)))))"
			+ "(define (##find f lst) (if (##null? lst) #f (if (f (##car lst)) (##car lst) (##find f (##cdr lst)))))"
			+ "(define (##drop-while f lst) (if (##null? lst) '() (if (f (##car lst)) (##drop-while f (##cdr lst)) lst)))"
			+ "(define (take-while f lst) (define (iter l acc) (if (##null? l) acc (if (f (##car l)) (iter (##cdr l) (##cons (##car l) acc)) acc))) (##reverse (iter lst '())))"
			+ "(define (take lst i) (define (iter l totake acc) (if (##null? l) acc (if (##zero? totake) acc (iter (##cdr l) (##- totake 1) (##cons (##car l) acc))))) (##reverse (iter lst i '())))"
			+ "(define (make-proper-list lst) (define (iter i acc) (cond ((##pair? i) (iter (##cdr i) (##cons (##car i) acc))) ((##null? i) acc) (else (##cons i acc)))) (##reverse (iter lst '())))"
			+ "(defmacro when (expr . body) `(if ,expr ,(##cons 'begin body) #f))"
			+ "(defmacro unless (expr . body) `(if ,expr #f ,(##cons 'begin body)))"
			+ "(defmacro aif (expr then . rest) `(let ((it ,expr)) (if it ,then ,(if (##null? rest) #f (##car rest)))))"
			+ "(defmacro awhen (expr . then) `(let ((it ,expr)) (if it ,(##cons 'begin then) #f)))"
			+ "(define (##count upto f) (define (iter i) (if (##= i upto) 'undefined (begin (f i) (iter (##+ i 1))))) (iter 0))"
			+ "(defmacro dotimes (lst . body) (##list '##count (##cadr lst) (##cons 'lambda (##cons (##list (##car lst)) body))))"
			+ "(defmacro dolist (lst . forms) (##list '##for-each (##cons 'lambda (##cons (##list (##car lst)) forms)) (##cadr lst)))"
			+ "(defmacro while (exp . body) (##cons 'do (##cons '() (##cons `((##not ,exp) 'undefined) body))))"
			+ "(define (##id x) x)";

	private final static String _reportInitScript = ""
			+ "(define (##newline . args) (if (##null? args) (##display \"\\n\") (##display \"\\n\" (##car args))))"
			+ "(define ##complex? ##number?)"
			+ "(define (##caar x) (##car (##car x)))"
			+ "(define (##cadr x) (##car (##cdr x)))"
			+ "(define (##cdar x) (##cdr (##car x)))"
			+ "(define (##cddr x) (##cdr (##cdr x)))"
			+ "(define (##caaar x) (##car (##car (##car x))))"
			+ "(define (##caadr x) (##car (##car (##cdr x))))"
			+ "(define (##cadar x) (##car (##cdr (##car x))))"
			+ "(define (##caddr x) (##car (##cdr (##cdr x))))"
			+ "(define (##cdaar x) (##cdr (##car (##car x))))"
			+ "(define (##cdadr x) (##cdr (##car (##cdr x))))"
			+ "(define (##cddar x) (##cdr (##cdr (##car x))))"
			+ "(define (##cdddr x) (##cdr (##cdr (##cdr x))))"
			+ "(define (##caaaar x) (##car (##car (##car (##car x)))))"
			+ "(define (##caaadr x) (##car (##car (##car (##cdr x)))))"
			+ "(define (##caadar x) (##car (##car (##cdr (##car x)))))"
			+ "(define (##caaddr x) (##car (##car (##cdr (##cdr x)))))"
			+ "(define (##cadaar x) (##car (##cdr (##car (##car x)))))"
			+ "(define (##cadadr x) (##car (##cdr (##car (##cdr x)))))"
			+ "(define (##caddar x) (##car (##cdr (##cdr (##car x)))))"
			+ "(define (##cadddr x) (##car (##cdr (##cdr (##cdr x)))))"
			+ "(define (##cdaaar x) (##cdr (##car (##car (##car x)))))"
			+ "(define (##cdaadr x) (##cdr (##car (##car (##cdr x)))))"
			+ "(define (##cdadar x) (##cdr (##car (##cdr (##car x)))))"
			+ "(define (##cdaddr x) (##cdr (##car (##cdr (##cdr x)))))"
			+ "(define (##cddaar x) (##cdr (##cdr (##car (##car x)))))"
			+ "(define (##cddadr x) (##cdr (##cdr (##car (##cdr x)))))"
			+ "(define (##cdddar x) (##cdr (##cdr (##cdr (##car x)))))"
			+ "(define (##cddddr x) (##cdr (##cdr (##cdr (##cdr x)))))"
			+ "(define (##list . lst) lst)"
			+ "(define (##positive? x) (##> x 0))"
			+ "(define (##negative? x) (##< x 0))"
			+ "(define (##abs x) (if (##positive? x) x (##- 0 x)))"
			+ "(define (##modulo a b) (define (sgn x) (if (##>= x 0) 1 -1)) (if (##= (sgn a) (sgn b)) (##remainder a b) (##+ b (##remainder a b))))"
			+ "(define (##not x) (if x #f #t))"
			+ "(define (##fold f acc lst) (if (##null? lst) acc (##fold f (f (##car lst) acc) (##cdr lst))))"
			+ "(define (##reverse lst) (##fold ##cons '() lst))"
			+ "(define (##map1 f lst) (##reverse (##fold (lambda (i acc) (##cons (f i) acc)) '() lst)))"
			+ "(define (##for-each f lst) (##fold (lambda (i acc) (f i) 'undefined) 'undefined lst))"
			+ "(define (##even? x) (##zero? (##remainder x 2)))"
			+ "(define (##odd? x) (if (##even? x) #f #t))"
			+ "(define (##gcd2 a b) (if (##zero? b) a (##gcd2 b (##remainder a b))))"
			+ "(define (##lcm2 a b) (##/ (##* a b) (##gcd2 a b)))"
			+ "(define (##gcd . args) (if (##null? args) 0 (##abs (##fold ##gcd2 (##car args) (##cdr args)))))"
			+ "(define (##lcm . args) (if (##null? args) 1 (##abs (##fold ##lcm2 (##car args) (##cdr args)))))"
			+ "(define (##every f lst) (if (##null? lst) #t (if (f (##car lst)) (##every f (##cdr lst)) #f)))"
			+ "(define (##any f lst) (if (##null? lst) #f (if (f (##car lst)) #t (##any f (##cdr lst)))))"
			+ "(define (##length lst) (define (iter l acc) (if (##null? l) acc (iter (##cdr l) (##+ acc 1)))) (iter lst 0))"
			+ "(define (##char=? a b) (##= (##char->integer a) (##char->integer b)))"
			+ "(define (##char>? a b) (##> (##char->integer a) (##char->integer b)))"
			+ "(define (##char<? a b) (##< (##char->integer a) (##char->integer b)))"
			+ "(define (##char>=? a b) (##>= (##char->integer a) (##char->integer b)))"
			+ "(define (##char<=? a b) (##<= (##char->integer a) (##char->integer b)))"
			+ "(define (##char-upper-case? x) (if (##< (##char->integer x) 65) #f (##< (##char->integer x) 91)))"
			+ "(define (##char-lower-case? x) (if (##< (##char->integer x) 97) #f (##< (##char->integer x) 123)))"
			+ "(define (##char-upcase x) (if (##char-lower-case? x) (##integer->char (##- (##char->integer x) 32)) x))"
			+ "(define (##char-downcase x) (if (##char-upper-case? x) (##integer->char (##+ (##char->integer x) 32)) x))"
			+ "(define (##char-ci=? a b) (##char=? (##char-downcase a) (##char-downcase b)))"
			+ "(define (##char-ci<? a b) (##char<? (##char-downcase a) (##char-downcase b)))"
			+ "(define (##char-ci>? a b) (##char>? (##char-downcase a) (##char-downcase b)))"
			+ "(define (##char-ci<=? a b) (##char<=? (##char-downcase a) (##char-downcase b)))"
			+ "(define (##char-ci>=? a b) (##char>=? (##char-downcase a) (##char-downcase b)))"
			+ "(define (##char-alphabetic? x) (if (##char-upper-case? x) #t (##char-lower-case? x)))"
			+ "(define (##char-numeric? x) (if (##>= (##char->integer x) 48) (##<= (##char->integer x) 57) #f))"
			+ "(define (##char-whitespace? x) (if (##char=? x #\\space) #t (if (##char=? x #\\tab) #t (if (##char=? x #\\newline) #t (##char=? x #\\cr)))))"
			+ "(define (##append . lists) (define (add-list lst acc) (if (##null? lst) acc (add-list (##cdr lst) (##cons (##car lst) acc)))) (define (iter rest-lists acc) (if (##null? rest-lists) acc (iter (##cdr rest-lists) (add-list (##car rest-lists) acc)))) (if (##null? lists) lists (let* ((reversed (##reverse lists)) (last-list (##car reversed)) (rest-lists (##map1 ##reverse (##cdr reversed)))) (iter rest-lists last-list))))"
			+ "(define (##string=? a b) (define (check i max) (if (##>= i max) #t (if (##char=? (##string-ref a i) (##string-ref b i)) (check (##+ i 1) max) #f))) (if (##= (##string-length a) (##string-length b)) (check 0 (##string-length a)) #f))"
			+ "(define (##string-ci=? a b) (define (check i max) (if (##>= i max) #t (if (##char-ci=? (##string-ref a i) (##string-ref b i)) (check (##+ i 1) max) #f))) (if (##= (##string-length a) (##string-length b)) (check 0 (##string-length a)) #f))"
			+ "(define (##string>? a b) (define (check i max-a max-b) (if (##>= i max-a) #f (if (##>= i max-b) #t (if (##char=? (##string-ref a i) (##string-ref b i)) (check (##+ i 1) max-a max-b) (##char>? (##string-ref a i) (##string-ref b i)))))) (check 0 (##string-length a) (##string-length b)))"
			+ "(define (##string-ci>? a b) (define (check i max-a max-b) (if (##>= i max-a) #f (if (##>= i max-b) #t (if (##char-ci=? (##string-ref a i) (##string-ref b i)) (check (##+ i 1) max-a max-b) (##char-ci>? (##string-ref a i) (##string-ref b i)))))) (check 0 (##string-length a) (##string-length b)))"
			+ "(define (##string<? a b) (if (##string=? a b) #f (##not (##string>? a b))))"
			+ "(define (##string-ci<? a b) (if (##string-ci=? a b) #f (##not (##string-ci>? a b))))"
			+ "(define (##string>=? a b) (##not (##string<? a b)))"
			+ "(define (##string<=? a b) (##not (##string>? a b)))"
			+ "(define (##string-ci>=? a b) (##not (##string-ci<? a b)))"
			+ "(define (##string-ci<=? a b) (##not (##string-ci>? a b)))"
			+ "(define (##string->list x) (define (iter i max acc) (if (##>= i max) (##reverse acc) (iter (##+ i 1) max (##cons (##string-ref x i) acc)))) (iter 0 (##string-length x) '()))"
			+ "(define (##list->string lst) (define (iter i l acc) (if (##null? l) acc (begin (##string-set! acc i (##car l)) (iter (##+ i 1) (##cdr l) acc)))) (iter 0 lst (##make-string (##length lst))))"
			+ "(define (##string . values) (##list->string values))"
			+ "(define (##substring s start end) (##list->string (take (##list-tail (##string->list s) start) (##- end start))))"
			+ "(define (##string-append . strings) (##list->string (##fold ##append '() (##map1 ##string->list (##reverse strings)))))"
			+ "(define (##string-copy s) (##list->string (##string->list s)))"
			+ "(define (##string-fill! s c) (define (iter i max) (if (##>= i max) s (begin (##string-set! s i c) (iter (##+ i 1) max)))) (iter 0 (##string-length s)))"
			+ "(defmacro letrec (lst . forms) (##cons (##append '(lambda) (##list (##map1 ##car lst)) (##map1 (lambda (i) (##list 'set! (##car i) (##cadr i))) lst) forms) (##map1 (lambda (x) #f) lst)))"
			+ "(defmacro let data (if (##symbol? (##car data)) (##cons 'letrec (##cons (##list (##cons (##car data) (##list (##cons 'lambda (##cons (##map1 ##car (##cadr data)) (##cddr data)))))) (##list (##cons (##car data) (##map1 ##cadr (##cadr data)))))) (##cons (##cons 'lambda (##cons (##map1 ##car (##car data)) (##cdr data))) (##map1 ##cadr (##car data)))))"
			+ "(defmacro let* (lst . forms) (if (##null? lst) (##cons 'begin forms) (##list 'let (##list (##car lst)) (##cons 'let* (##cons (##cdr lst) forms)))))"
			+ "(defmacro cond list-of-forms (define (expand-cond lst) (if (##null? lst) #f (if (##eq? (##caar lst) 'else) (##cons 'begin (##cdar lst)) (##list 'if (##caar lst) (##cons 'begin (##cdar lst)) (expand-cond (##cdr lst)))))) (expand-cond list-of-forms))"
			+ "(defmacro and list-of-forms (if (##null? list-of-forms) #t (if (##null? (##cdr list-of-forms)) (##car list-of-forms) (##list 'if (##car list-of-forms) (##append '(and) (##cdr list-of-forms)) #f))))"
			+ "(defmacro quasiquote (x) (if (##pair? x) (cond ((##eq? (##car x) 'unquote) (##cadr x)) ((and (##pair? (##car x)) (##eq? (##caar x) 'unquote-splicing)) (##list '##append (##cadar x) (##list 'quasiquote (##cdr x)))) (else (##list '##cons (##list 'quasiquote (##car x)) (##list 'quasiquote (##cdr x))))) (##list 'quote x)))"
			+ "(define (##make-promise f) (let ((value #f) (forced #f)) (lambda () (if forced value (begin (set! value (f)) (set! forced #t) value)))))"
			+ "(define (##force obj) (obj))"
			+ "(defmacro delay (expression) (##list '##make-promise (##list 'lambda '() expression)))"
			+ "(define (##min . args) (define (min-of-two a b) (if (##< a b) a b)) (let ((l (##length args))) (cond ((##zero? l) (##error \"min called without parameters\")) ((##= 1 l) (##car args)) (else (let ((ret (##fold min-of-two (##car args) (##cdr args)))) (if (##any ##inexact? args) (##exact->inexact ret) ret))))))"
			+ "(define (##max . args) (define (max-of-two a b) (if (##> a b) a b)) (let ((l (##length args))) (cond ((##zero? l) (##error \"max called without parameters\")) ((##= 1 l) (##car args)) (else (let ((ret (##fold max-of-two (##car args) (##cdr args)))) (if (##any ##inexact? args) (##exact->inexact ret) ret))))))"
			+ "(defmacro or args (if (##null? (##cdr args)) (##car args) (##list 'aif (##car args) 'it (##cons 'or (##cdr args)))))"
			+ "(define (##eqv? a b) (define (bool=? a b) (if a b (##not b))) (cond ((##eq? a b) #t) ((and (##number? a) (##number? b)) (and (bool=? (##exact? a) (##exact? b)) (##= a b))) ((and (##char? a) (##char? b)) (##char=? a b)) ((and (##boolean? a) (##boolean? b)) (bool=? a b)) (else #f)))"
			+ "(define (##equal? a b) (cond ((##eqv? a b) #t) ((and (##string? a) (##string? b)) (##string=? a b)) ((and (##pair? a) (##pair? b)) (if (##equal? (##car a) (##car b)) (##equal? (##cdr a) (##cdr b)) #f)) ((and (##vector? a) (##vector? b)) (##equal? (##vector->list a) (##vector->list b))) (else #f)))"
			+ "(define (##memq obj lst) (if (##pair? lst) (if (##eq? obj (##car lst)) lst (##memq obj (##cdr lst))) #f))"
			+ "(define (##memv obj lst) (if (##pair? lst) (if (##eqv? obj (##car lst)) lst (##memv obj (##cdr lst))) #f))"
			+ "(define (##member obj lst) (if (##pair? lst) (if (##equal? obj (##car lst)) lst (##member obj (##cdr lst))) #f))"
			+ "(define (##assq obj lst) (if (##pair? lst) (if (##eq? obj (##caar lst)) (##car lst) (##assq obj (##cdr lst))) #f))"
			+ "(define (##assv obj lst) (if (##pair? lst) (if (##eqv? obj (##caar lst)) (##car lst) (##assv obj (##cdr lst))) #f))"
			+ "(define (##assoc obj lst) (if (##pair? lst) (if (##equal? obj (##caar lst)) (##car lst) (##assoc obj (##cdr lst))) #f))"
			+ "(defmacro case (exp . clauses) (define (make-thunk-symbol index) (##string->symbol (##string-append \"thunk\" (##number->string index)))) (define (expand-case-thunks c index) (if (##null? c) '() (##cons (##list (make-thunk-symbol index) (##cons 'lambda (##cons '() (##cdar c)))) (expand-case-thunks (##cdr c) (##+ index 1))))) (define (expand-case-cond c index) (if (##null? c) '() (##cons (##list (if (##eq? (##caar c) 'else) 'else (##list '##memv 'key (##list 'quote (##caar c)))) (##list (make-thunk-symbol index))) (expand-case-cond (##cdr c) (##+ index 1))))) (##list 'let (##cons (##list 'key exp) (expand-case-thunks clauses 1)) (##cons 'cond (expand-case-cond clauses 1))))"
			+ "(define ##gensym (let ((sym 0)) (lambda () (set! sym (##+ sym 1)) (##string->symbol (##string-append \"##gensym##\" (##number->string sym))))))"
			+ "(defmacro do (vars pred . body) (define (caddr-or-car x) (if (##null? (##cddr x)) (##car x) (##caddr x))) (let ((symbol (##gensym))) `(let ((,symbol '())) (set! ,symbol (lambda ,(##map1 ##car vars) (if ,(##car pred) ,(##cadr pred) ,(##cons 'begin (##append body (##list (##cons symbol (##map1 caddr-or-car vars)))))))) ,(##cons symbol (##map1 ##cadr vars))))) "
			+ "(define (##vector-fill! v obj) (define (iter i max) (if (##>= i max) v (begin (##vector-set! v i obj) (iter (##+ i 1) max)))) (iter 0 (##vector-length v)))"
			+ "(define (##list->vector lst) (define (iter v i vals) (##vector-set! v i (##car vals)) (if (##zero? i) v (iter v (##- i 1) (##cdr vals)))) (let ((v (##make-vector (##length lst)))) (if (##zero? (##vector-length v)) v (iter v (##- (##vector-length v) 1) (##reverse lst)))))"
			+ "(define (##vector->list v) (define (iter i acc) (if (##< i 0) acc (iter (##- i 1) (##cons (##vector-ref v i) acc)))) (iter (##- (##vector-length v) 1) '()))"
			+ "(define (##vector . lst) (##list->vector lst))"
			+ "(define (##call-with-output-file filename thunk) (let* ((f (##open-output-file filename)) (output (thunk f))) (##close-output-port f) output))"
			+ "(define (##with-output-to-file filename proc) (let ((f (##open-output-file filename)) (old-output (##current-output-port))) (sys:set-current-output-port f) (let ((output (proc))) (sys:set-current-output-port old-output) (##close-output-port f) output)))"
			+ "(define (##call-with-input-file filename thunk) (let* ((f (##open-input-file filename)) (input (thunk f))) (##close-input-port f) input))"
			+ "(define (##with-input-from-file filename proc) (let ((f (##open-input-file filename)) (old-input (##current-input-port))) (sys:set-current-input-port f) (let ((input (proc))) (sys:set-current-input-port old-input) (##close-input-port f) input)))"
			+ "(define (##sqrt n) (define (isqrt n) (if (##negative? n) (##* 0+1i (isqrt (##- 0 n))) (let* ((guess (##inexact->exact (sys:sqrt n))) (guess_f (##floor guess)) (guess_c (##ceiling guess))) (cond ((##= n (##* guess_c guess_c)) guess_c) ((##= n (##* guess_f guess_f)) guess_f) (else (##exact->inexact guess)))))) (cond ((##integer? n) (isqrt n)) ((##rational? n) (##/ (isqrt (##numerator n)) (isqrt (##denominator n)))) ((##real? n) (##exact->inexact (isqrt n))) (else (let* ((a (##real-part n)) (b (##imag-part n)) (m (##sqrt (##+ (##* a a) (##* b b)))) (l (##sqrt (##/ (##+ m a) 2))) (sgn (lambda (x) (if (##positive? x) 1 (if (##negative? x) -1 0)))) (d (##* (sgn b) (##sqrt (##/ (##- m a) 2))))) (##+ l (##* 0+1i d))))))"
			+ "(define (##square x) (##* x x))"
			+ "(define (##magnitude n) (if (##real? n) (##abs n) (##sqrt (##+ (##square (##real-part n)) (##square (##imag-part n))))))"
			+ "(define (##make-rectangular x1 x2) (##+ x1 (##* 0+1i x2)))"
			+ "(define (##angle n) (let* ((r (##real-part n)) (i (##imag-part n)) (pi 3.1415926535897932384626433) (a (##atan (##/ (##abs i) (##abs r))))) (if (##positive? r) (if (##positive? i) a (##- a)) (if (##positive? i) (##- pi a) (##- a pi)))))"
			+ "(define (##make-polar r a) (##+ (##* r (##cos a)) (##* r (##sin a) 0+1i)))"
			+ "(define (##apply f arglist) (sys:apply f arglist))"
			+ "(define (##list? x) (if (##pair? x) (let loop ((slow x) (fast (##cdr x)) (i #f)) (cond ((##null? fast) #t) ((##eq? fast slow) #f) ((##pair? fast) (loop (if i slow (##cdr slow)) (##cdr fast) (##not i))) (else #f))) (##null? x)))"
			+ "(define (##expt a b) (cond ((##zero? b) 1) ((##= 1 b) a) ((##not (##integer? b)) (sys:expt a b)) ((##negative? b) (##/ (##expt a (##- b)))) ((##even? b) (##square (##expt a (##quotient b 2)))) (else (##* a (##expt a (##- b 1))))))"
			+ "(define ##values ##list)"
			+ "(define (##call-with-values generator consumer) (let ((v (generator))) (if (##list? v) (##apply consumer v) (consumer v))))"
			+ "(define (##call-with-current-continuation f) #f)"
			// (sys:call/cc f))"
			+ "(define (##call/cc f) #f)"// (sys:call/cc f))"
			+ "(define (##error . args) (sys:error args))"
			+ "(define (##port? x) (if (##input-port? x) #t (##output-port? x)))"
			+ "(define (##map f . lists) (define (iter acc ls) (if (##any ##null? ls) (##reverse acc) (iter (##cons (##apply f (##map1 ##car ls)) acc) (##map1 ##cdr ls)))) (iter '() lists))"
			+ "(define (##list-tail lst k) (if (##zero? k) lst (##list-tail (##cdr lst) (##- k 1))))"
			+ "(define (##list-ref lst k) (##car (##list-tail lst k)))";

	@Override
	public Object toJavaObject() throws SchemeException {
		throw new SchemeException(
				"Environment cannot be converted into a plain Java object");
	}
}
