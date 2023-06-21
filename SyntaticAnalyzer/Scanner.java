package miniJava.SyntaticAnalyzer;

import miniJava.ErrorReporter;

public class Scanner {
	private SourceFile file; 
	private char currentChar;
	private StringBuffer currentSpelling;
	private ErrorReporter reporter;
	
	public Scanner(SourceFile file, ErrorReporter reporter) {
		this.file = file;
		this.reporter = reporter;
		currentChar = file.getChar();
	}
	
	
	private void takeIt() {	
		
		currentSpelling.append(currentChar);
		currentChar = file.getChar();
		
	}
	
	private void skipIt() {		
		currentChar = file.getChar();
		
	}
	
	
	private boolean isChar(char c) {
		return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
	}
	

	private boolean isDigit(char c) {
		return (c >= '0' && c <= '9');
	}
	
	
	private int scanToken() {
		
		if (SourceFile.EOT) {
			return Token.EOT;
		}
		
	
		switch (currentChar) {
		
			//letters
		case 'a':  case 'b':  case 'c':  case 'd':  case 'e':
	    case 'f':  case 'g':  case 'h':  case 'i':  case 'j':
	    case 'k':  case 'l':  case 'm':  case 'n':  case 'o':
	    case 'p':  case 'q':  case 'r':  case 's':  case 't':
	    case 'u':  case 'v':  case 'w':  case 'x':  case 'y':
	    case 'z':
	    case 'A':  case 'B':  case 'C':  case 'D':  case 'E':
	    case 'F':  case 'G':  case 'H':  case 'I':  case 'J':
	    case 'K':  case 'L':  case 'M':  case 'N':  case 'O':
	    case 'P':  case 'Q':  case 'R':  case 'S':  case 'T':
	    case 'U':  case 'V':  case 'W':  case 'X':  case 'Y':
	    case 'Z':
	    	takeIt();
	    	while (isChar(currentChar) || isDigit(currentChar) || currentChar == '_') {
	    		takeIt();
	    	}
	    	return Token.IDENTIFIER;
	    	
	    	
	    case '0':  case '1':  case '2':  case '3':  case '4':
	    case '5':  case '6':  case '7':  case '8':  case '9':
	    	takeIt();
	    	while (isDigit(currentChar)) {
	    		takeIt();
	    	}
	    	return Token.INTLITERAL;
	    	
	    	
	    case '+':	case '*':
	    	takeIt();
	    	return Token.BINOP;
	    	
		case '-':
			takeIt();
			return Token.MINUS;
	    	
	    case '<':	case '>':
	    	takeIt();
	    	if (currentChar == '=') {
	    		takeIt();
	    	}
	    	return Token.BINOP;
	    	
	    	
		case '!':
			takeIt();
			if (currentChar == '=') {
				takeIt();
				return Token.BINOP;
			}
			return Token.UNOP;
	    	
	    case '=':
	    	takeIt();
	    	if (currentChar == '=') {
	    		takeIt();
	    		return Token.BINOP;
	    	}
	    	return Token.EQUAL;
	    	
	    	
	    case '&':
	    	takeIt();
	    	if (currentChar == '&') {
	    		takeIt();
	    		return Token.BINOP;
	    	}
	    	reporter.reportError("invalid character, expected &&");
	    	break;
	    	
	    
	    case '|':
	    	takeIt();
	    	if (currentChar == '|') {
	    		takeIt();
	    		return Token.BINOP;
	    	}
	    	reporter.reportError("invalid character, expected ||");
	    	break;
	    	
	    case '.':
			takeIt();
			return Token.DOT;

		case ';':
			takeIt();
			return Token.SEMICOLON;

		case ',':
			takeIt();
			return Token.COMMA;

		case '(':
			takeIt();
			return Token.LPAREN;

		case ')':
			takeIt();
			return Token.RPAREN;

		case '[':
			takeIt();
			return Token.LBRAC;

		case ']':
			takeIt();
			return Token.RBRAC;

		case '{':
			takeIt();
			return Token.LCURLY;

		case '}':
			takeIt();
			return Token.RCURLY;
			
		default:
			reporter.reportError("not valid characters in miniJava");
			return Token.ERROR;			
		}	
		return Token.ERROR;	
	}
	
	
	private int scanSeparator() {		
		/*
		 -1 for EOT or not comments nor white spaces
		  1 for skipping 
		  2 for operator /
		 */
		switch (currentChar) {
		
		case '/':
			
			skipIt();
		
			switch (currentChar) {
			
			case '/':											// single line comment
				
				skipIt();
				
				while (currentChar != '\n' && !SourceFile.EOT) {
					skipIt();
				}
				
				return 1; 
			
			case '*':                							 // multiple line comment
				skipIt();
				
				while (true) {
					while (currentChar == '*') {
						skipIt();
						if (currentChar == '/') {
							skipIt();
							return 1;						  
						}
					}
					if (SourceFile.EOT) {
						reporter.reportError("invalid multi-line comment");
						return -1;
					}
					skipIt();
				}
			
			default:
				return 2;
			}	
			
			
		case ' ':	case '\n':	case '\t':	case '\r':
			skipIt();
			while (currentChar == ' ' || currentChar == '\n' || currentChar == '\t' || currentChar == '\r') {
				skipIt();
			}
			return 1;
			
		default:
			return -1;
		}
	}
	
	
	public Token Scan() {
		Token token;
		int kind;
		
		int check = 1;
		
		while (currentChar == ' ' || currentChar == '\n' || currentChar == '\t' || currentChar == '\r' || currentChar == '/') {
			check = scanSeparator();
			if (check == 2) {
				break;
			}
		}
		
		SourcePosition pos = new SourcePosition();
		pos.start = SourceFile.getCurrentLine(); 
				
		currentSpelling = new StringBuffer("");
		
		if (check == 2) {
			kind = Token.BINOP;
			currentSpelling.append('/');
		} else {
			kind = scanToken();
		}
		
		pos.finish = SourceFile.getCurrentLine();
		
		token = new Token(kind, currentSpelling.toString(), pos);
		return token;
		
	}
}
 