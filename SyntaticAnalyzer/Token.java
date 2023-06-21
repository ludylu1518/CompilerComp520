package miniJava.SyntaticAnalyzer;

public class Token {
	
	public int kind;
	public String spelling;
	public SourcePosition posn;
	
	public Token(int kind, String spelling, SourcePosition posn) {
		this.kind = kind;
		this.spelling = spelling;
		this.posn = posn;
		
		if (kind == Token.IDENTIFIER) {
			for (int k = Token.BEGIN; k <= Token.END; k++) {
				if (spelling.equals(tokens[k])) {
					this.kind = k;
					break;
				}
			}
		}
	}
		
	public static String spell(int kind) {
		return tokens[kind];
	}
	
	public static final int 
		
		IDENTIFIER = 0,
		INTLITERAL = 1,

		//operators
		BINOP = 2,
		UNOP = 3,
		
		//Keys
		CLASS = 4,
		PUBLIC = 5,
		PRIVATE = 6,
		STATIC = 7,
		TRUE = 8,
		FALSE = 9,
		IF = 10,
		ELSE = 11,
		WHILE = 12,
		INT = 13,
		BOOLEAN = 14,
		THIS = 15,
		VOID = 16,
		NEW = 17,
		NULL = 18,
		RETURN = 19,
		
		//Others
		EQUAL = 20,
		DOT = 21,
		COMMA = 22,
		SEMICOLON = 23,
		LPAREN = 24,
		RPAREN = 25,
		LBRAC = 26,
		RBRAC = 27,
		LCURLY = 28,         // {
		RCURLY = 29,         // }
		EOT = 30,
		ERROR = 31,
		MINUS = 32;       // unique can be both unop or binop
		
		
	
		
	public static String[] tokens = {
			"<identifier>",
			"<int>",
			"<binop>",
			"<unop>",
			"class",
			"public",
			"private",
			"static",
			"true",
			"false",
			"if",
			"else",
			"while",
			"int",
			"boolean",
			"this",
			"void",
			"new",
			"null",
			"return",
			"<=>",
			".",
			",",
			";",
			"(",
			")",
			"[",
			"]",
			"{",
			"}",
			"<EOT>",
			"<error>",
			"-"
	};
		
	
	public static final int BEGIN = Token.CLASS;
	public static final int END = Token.RETURN;
	

}
