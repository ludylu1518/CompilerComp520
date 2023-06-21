package miniJava.SyntaticAnalyzer;

import miniJava.ErrorReporter;
import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;



public class Parser {
	private Scanner scanner;
	private ErrorReporter reporter;
	private Token currentToken;
	private SourcePosition position;
	
	public Parser(Scanner scanner, ErrorReporter reporter) {
		this.scanner = scanner;
		this.reporter = reporter;
		position = new SourcePosition();
	}	
	
	/**
	 * SyntaxError is used to unwind parse stack when parse fails
	 *
	 */	
	class SyntaxError extends Error {
		private static final long serialVersionUID = 1L;	
	}

	
	private void startPosition(SourcePosition pos) {
		pos.start = currentToken.posn.start;
	}
	
	private void finishPosition(SourcePosition pos) {
		pos.finish = position.finish;
	}
	
	private void accept(int tokenExpected) throws SyntaxError {                         // accept if token matches else throw error
		if (currentToken.kind == tokenExpected) { 
			//pTrace();
			position = currentToken.posn;
			currentToken = scanner.Scan();
		} else {
			reporter.reportError("expected " + Token.spell(tokenExpected) + 
					" but was "+ currentToken.spelling + " at lines: " + currentToken.posn.start + "-" + currentToken.posn.finish);
		}
	}
	
	private void acceptIt() {												 	// accept token
		//pTrace();
		position = currentToken.posn;
		currentToken = scanner.Scan();
	}
	
	
	
	public AST parse() {												// beginning of parse
		currentToken = scanner.Scan();	
		Package packageAst = null;                                   // initialize package
		try {
			packageAst = parseProgram();
		}
		catch (SyntaxError e) { }
		return packageAst;
	}
	
	private Package parseProgram(){					// if class parseClass else accept EOT
		
		Package packageAst = null;
		
		SourcePosition packagePos = new SourcePosition();
		startPosition(packagePos);
		
		ClassDeclList classList = new ClassDeclList();           // class list initialize
		
		try {
			while (currentToken.kind == Token.CLASS) {               // add classes to the list
				ClassDecl classAst = parseClass();
				classList.add(classAst);
			}
			
			finishPosition(packagePos);
			packageAst = new Package(classList, packagePos);            
			
			if (currentToken.kind != Token.EOT) {
				reporter.reportError("\"%\" not expected after end of program: " + currentToken.spelling);
			}
		} catch (SyntaxError s){
			return null;
		}
		return packageAst;
	}
	
	
	private ClassDecl parseClass() throws SyntaxError{						// parsing class and  both field and method declarations
		
		ClassDecl classAst = null;                                			// String, FieldDeclList, MethodDeclList
		
		SourcePosition classPos = new SourcePosition();
		startPosition(classPos);
		
		accept(Token.CLASS); 
		
		Identifier class_id = parseIdentifier();
		
		FieldDeclList fieldList = new FieldDeclList();
		MethodDeclList methodList = new MethodDeclList();
		
		accept(Token.LCURLY);
		
		int checkVoid = 0;
		
		while (isDeclaration()) {	
			
			FieldDecl field = null;                                               // MemberDecl
			MethodDecl method = null;											 // MemberDecl, ParameterDeclList, StatementList
			
			MemberDecl member = null;                                                   // isPrivate, isStatic, TypeDenoter, String
			
			SourcePosition memberPos = new SourcePosition();
			startPosition(memberPos);
			
			boolean isPrivate = parseVisibility();											
			boolean isStatic = parseAccess();		
			
			TypeDenoter type = null; 
			
			if (currentToken.kind == Token.VOID) {						// void is for method only
				acceptIt();
				checkVoid = 1;
				type = new BaseType(TypeKind.VOID, position);
			} else {													// other types
				type = parseType();
			}
			
			
			Identifier member_id = parseIdentifier();
			
			finishPosition(memberPos);
			
			member = new FieldDecl(isPrivate, isStatic, type, member_id.spelling, memberPos);    
			
			
			if (currentToken.kind == Token.SEMICOLON && checkVoid == 0) {				// id ; 
				acceptIt();
				
				field = new FieldDecl(member, memberPos);
				fieldList.add(field);
				
			} else if (currentToken.kind == Token.LPAREN) {								// id ( ParameterList? ) {Statement*}
				acceptIt();
				
				ParameterDeclList parameterList = new ParameterDeclList();
				StatementList stmtList = new StatementList();
				

				if (currentToken.kind == Token.RPAREN) {
					acceptIt();
				} else {
						
					parameterList = parseParameter();
					
					accept(Token.RPAREN);
				}
				
				accept(Token.LCURLY);
			
				
				while (isStatement()) {			
					
					Statement statement = null;
					statement = parseStatement();	
					
					if (statement != null) {					
						stmtList.add(statement);
					}
				}
				
				accept(Token.RCURLY);
				finishPosition(memberPos);				
				
				method = new MethodDecl(member, parameterList, stmtList, memberPos);
				methodList.add(method);
				
			
			} else if (currentToken.kind == Token.SEMICOLON){                           // void field 
				System.out.println(currentToken.posn);
				acceptIt();
				reporter.reportError("void type only for method declaration");
			} else {
				reporter.reportError("invalid declarations syntax" + " at lines: " + memberPos.start + "-" + currentToken.posn.finish);
			}
			
			checkVoid = 0;
			
		}
		accept(Token.RCURLY);
		
		finishPosition(classPos);		
		
		classAst = new ClassDecl(class_id.spelling, fieldList, methodList, classPos);
		return classAst;
		
	}
	
	
	private boolean isDeclaration() {						// check for declaration starting tokens
		if (currentToken.kind == Token.PRIVATE || currentToken.kind == Token.PUBLIC 
				|| currentToken.kind == Token.STATIC || currentToken.kind == Token.INT
				|| currentToken.kind == Token.BOOLEAN || currentToken.kind == Token.IDENTIFIER 
				|| currentToken.kind == Token.VOID) {
			return true;
		}
		return false;
	}
	
	
	private boolean parseVisibility() throws SyntaxError{							// Visibility ::= ( public | private )?
		if (currentToken.kind == Token.PRIVATE) {
			acceptIt();
			return true;
		} else if (currentToken.kind == Token.PUBLIC) {
			acceptIt();
			return false;
		}
		return false;
	}
	
	private boolean parseAccess() throws SyntaxError{							// Access ::= static ?
		if (currentToken.kind == Token.STATIC) {
			acceptIt();
			return true;
		}
		return false;
	}
	
	private TypeDenoter parseType() throws SyntaxError{						// Type ::= int | boolean | id | ( int | id ) [] 
		
		TypeDenoter type = null;
		
		SourcePosition typePos = new SourcePosition();
		startPosition(typePos);
		
		switch(currentToken.kind) {
		
		case Token.INT:	
			
			acceptIt();
			
			if (currentToken.kind == Token.LBRAC) {                                           // int[]
				acceptIt();
				accept(Token.RBRAC);
				
				finishPosition(typePos);
				TypeDenoter kind = new BaseType(TypeKind.INT, typePos);
				type = new ArrayType(kind, typePos);				
			} else { 																		// int
				
				finishPosition(typePos);
				type = new BaseType(TypeKind.INT, typePos);
			}
			
			break;
			
		case Token.IDENTIFIER:
			
			Identifier id = new Identifier(currentToken);
			
			acceptIt();
			
			if (currentToken.kind == Token.LBRAC) {                                            // id[]
				acceptIt();
				accept(Token.RBRAC);
				
				finishPosition(typePos);
				TypeDenoter kind = new ClassType(id, typePos);
				type = new ArrayType(kind, typePos);	
				
			} else { 																		// id
				
				finishPosition(typePos);
				type = new ClassType(id, typePos);
			}
			
			break;
			
		case Token.BOOLEAN:
			acceptIt();
			
			finishPosition(typePos);
			type = new BaseType(TypeKind.BOOLEAN, typePos);
			
			break;
		
		default:
			reporter.reportError("invalid Type syntax" + " at lines: " + typePos.start + "-" + currentToken.posn.finish);
			break;
		}
		
		return type;
	}
	
	private ParameterDeclList parseParameter() throws SyntaxError{            // ParameterList ::= Type id ( , Type id )*
		
		ParameterDeclList parameterList = new ParameterDeclList();
		
		SourcePosition parameterPos = new SourcePosition();
		startPosition(parameterPos);
		
		TypeDenoter type = parseType();
		
		Identifier parameter_id = parseIdentifier();
		
		finishPosition(parameterPos);
		
		ParameterDecl paramter = new ParameterDecl(type, parameter_id.spelling, parameterPos);
		parameterList.add(paramter);
		
		while (currentToken.kind == Token.COMMA) {
			acceptIt();
			
			SourcePosition paraPos = new SourcePosition();
			startPosition(paraPos);
			
			type = parseType();
			parameter_id = parseIdentifier();
			
			finishPosition(paraPos);
			
			paramter = new ParameterDecl(type, parameter_id.spelling, paraPos);
			parameterList.add(paramter);
		}
		
		return parameterList;
	}
	
	private ExprList parseArgument() throws SyntaxError{        // ArgumentList ::= Expression ( , Expression )*
		
		ExprList argList = new ExprList();		
		
		Expression expr = parseExpression();
		
		argList.add(expr);
	
		
		while (currentToken.kind == Token.COMMA) {
			acceptIt();
			
			Expression expr1 = parseExpression();
			
			argList.add(expr1);
		}
		return argList;
	}
	
	private Reference parseReference() throws SyntaxError{			// Reference ::= id | this | Reference . id 
				
		Reference ref = null;
		
		SourcePosition refPos = new SourcePosition();
		startPosition(refPos);
		
		 if (currentToken.kind == Token.IDENTIFIER) {
			 Identifier ref_id = parseIdentifier();
			
			 finishPosition(refPos);
			 
			ref = new IdRef(ref_id, refPos);
					
			while (currentToken.kind == Token.DOT) {
				acceptIt();
				
				Identifier id = parseIdentifier();
				
				 finishPosition(refPos);
				
				ref = new QualRef(ref, id, refPos);
			}
			
		 } else if(currentToken.kind == Token.THIS) {
			 acceptIt();
			 
			 finishPosition(refPos);
			 ref = new ThisRef(refPos);
			 
			 while (currentToken.kind == Token.DOT) {
				 acceptIt();
				 
				 Identifier id = parseIdentifier();
				 
				 finishPosition(refPos);
				 ref = new QualRef(ref, id, refPos);
			 }
			 
		 } else { 
			 reporter.reportError("invalid reference syntax" + " at lines: " + refPos.start + "-" + currentToken.posn.finish);
		 }		 
		 return ref;
	}
	
	private boolean isStatement() {		                // check if statement starting token.
		
		if (currentToken.kind == Token.LCURLY || currentToken.kind == Token.RETURN
				|| currentToken.kind == Token.IF || currentToken.kind == Token.WHILE
				|| currentToken.kind == Token.INT || currentToken.kind == Token.BOOLEAN
				|| currentToken.kind == Token.IDENTIFIER || currentToken.kind == Token.THIS) {
			
			return true;
			
		}
		
		return false;
	}
	
	
	private boolean isExpression() {
		
		if (currentToken.kind == Token.IDENTIFIER || currentToken.kind == Token.THIS || currentToken.kind == Token.UNOP
				|| currentToken.kind == Token.LPAREN || currentToken.kind == Token.INTLITERAL || currentToken.kind == Token.TRUE
				|| currentToken.kind == Token.FALSE || currentToken.kind == Token.NEW) {
			return true;
		}
		
		return false;
	}
	
	
	private Statement parseStatement() throws SyntaxError{
		
		Statement stmt = null;
		
		SourcePosition stmtPos = new SourcePosition();
		startPosition(stmtPos);
		
		switch (currentToken.kind) {
		
		case Token.LCURLY:             // { Statement* }
			acceptIt();
			
			Statement blockStmt = null;
			StatementList SL = new StatementList();
			
			while (isStatement()) {
				
				blockStmt = parseStatement();	
				SL.add(blockStmt);
				
			}
			
			accept(Token.RCURLY);
			
			finishPosition(stmtPos);
			stmt = new BlockStmt(SL, stmtPos);
			break;
			
			
		case Token.RETURN:            // return Expression? ;
			acceptIt();
			
			
			if (currentToken.kind == Token.SEMICOLON) {
				acceptIt();
				
				finishPosition(stmtPos);
				stmt = new ReturnStmt(null, stmtPos);
				
			} else {				
				
				Expression expr = parseExpression();
				
				accept(Token.SEMICOLON);
				
				finishPosition(stmtPos);				
				stmt = new ReturnStmt(expr, stmtPos);
				
			}
			break;
				
		case Token.IF:						// if ( Expression ) Statement (else Statement)? 						
			acceptIt();
			accept(Token.LPAREN);
			
			Expression expr1 = parseExpression();
			
			accept(Token.RPAREN);
			
			Statement stmt1 = parseStatement();
			
			Statement stmt2 = null;
			
			if (currentToken.kind == Token.ELSE) {
				acceptIt();
				stmt2 = parseStatement();
			}
			
			finishPosition(stmtPos);
			stmt = new IfStmt(expr1, stmt1, stmt2, stmtPos);
			break;
			
		case Token.WHILE:				//  while ( Expression ) Statement 
			acceptIt();
			accept(Token.LPAREN);
			
			Expression expr2 = parseExpression();
			
			accept(Token.RPAREN);
			
			Statement stmt3 = parseStatement();
			
			finishPosition(stmtPos);
			stmt = new WhileStmt(expr2, stmt3, stmtPos);
			break;
		
			
		case Token.INT:	case Token.BOOLEAN:			// Type id = Expression ;       for type of int and boolean 
			TypeDenoter type = parseType();
			
			Identifier type_id = parseIdentifier();
			
			VarDecl varD = new VarDecl(type, type_id.spelling, position);
			
			accept(Token.EQUAL);
			
			Expression expr3 = parseExpression();
			
			accept(Token.SEMICOLON);
			
			finishPosition(stmtPos);
			stmt = new VarDeclStmt(varD, expr3, stmtPos);
			break;
			
		case Token.IDENTIFIER:           							// check if type or reference case
			
			SourcePosition idPos = new SourcePosition();
			startPosition(idPos);
			
			Identifier id = parseIdentifier();                          // current id;
			
			if (currentToken.kind == Token.IDENTIFIER) {               // where type = id, Type id = Expression ;
				
				finishPosition(idPos);
				TypeDenoter type1 = new ClassType(id, idPos);
				
				Identifier id2 = parseIdentifier();
				
				finishPosition(idPos);
				VarDecl varDecl = new VarDecl(type1, id2.spelling, idPos);
				
				accept(Token.EQUAL);
				
				Expression expr8 = parseExpression();
				
				accept(Token.SEMICOLON);
				
				finishPosition(stmtPos);
				stmt = new VarDeclStmt(varDecl, expr8, stmtPos);
				
			} else if (currentToken.kind == Token.LBRAC) {          
				acceptIt();
				
				if (currentToken.kind == Token.RBRAC) {                  // where type = id[], Type id = Expression ;
					accept(Token.RBRAC);
					
					finishPosition(idPos);
					
					TypeDenoter type2 = new ClassType(id, idPos);
					TypeDenoter type3 = new ArrayType(type2, idPos);
					
					Identifier id3 = parseIdentifier();
					
					finishPosition(idPos);
					VarDecl v = new VarDecl(type3, id3.spelling, idPos);
					
					accept(Token.EQUAL);
					
					Expression expr9 = parseExpression();
					
					accept(Token.SEMICOLON);
					
					finishPosition(stmtPos);
					stmt = new VarDeclStmt(v, expr9, stmtPos);
					
				} else if (isExpression()) {                      // Reference [ Expression ] = Expression ;
					
					finishPosition(idPos);
					
					Reference ref = new IdRef(id, idPos);
                    Expression expr10 = parseExpression();
					
					accept(Token.RBRAC);
					accept(Token.EQUAL);
					
					Expression expr11 = parseExpression();
					
					accept(Token.SEMICOLON);
					
					finishPosition(stmtPos);
					stmt = new IxAssignStmt(ref, expr10, expr11, stmtPos);
					
				} else {
					reporter.reportError("invalid statement" + " at lines: " + stmtPos.start + "-" + currentToken.posn.finish);
				}
				
			} else if (currentToken.kind == Token.DOT) {            // reference cases,  where reference := reference.id
				
				finishPosition(idPos);
				Reference ref1 = new IdRef(id, idPos);
				
				while (currentToken.kind == Token.DOT) {
					acceptIt();
					
					Identifier id_ref = parseIdentifier();
					
					finishPosition(idPos);
					ref1 = new QualRef(ref1, id_ref, idPos);
				}
				
				switch(currentToken.kind) {                         
				
				case Token.EQUAL:						// Reference = Expression ;
					acceptIt();
					
					Expression expr12 = parseExpression();
					
					accept(Token.SEMICOLON);
					
					finishPosition(stmtPos);
					stmt = new AssignStmt(ref1, expr12, stmtPos);
					break;
					
					
				case Token.LPAREN:					// Reference ( ArgumentList? ) ;
					acceptIt();
					
					ExprList args = new ExprList();
					
					if (currentToken.kind == Token.RPAREN) {
						acceptIt();
					} else {
						args = parseArgument();
						accept(Token.RPAREN);
					}
					accept(Token.SEMICOLON);
					
					finishPosition(stmtPos);
					stmt = new CallStmt(ref1, args, stmtPos);
					break;
					
				
				case Token.LBRAC:								// Reference [ Expression ] = Expression ;
					acceptIt();
					
					Expression expr13 = parseExpression();
					
					accept(Token.RBRAC);
					accept(Token.EQUAL);
					
					Expression expr14 = parseExpression();
					
					accept(Token.SEMICOLON);
					
					finishPosition(stmtPos);
					stmt = new IxAssignStmt(ref1, expr13, expr14, stmtPos);
					break;
					
					
					
				default:
					reporter.reportError("invalid statement syntax" + " at lines: " + stmtPos.start + "-" + currentToken.posn.finish);
					break;
				}
				
			} else {                                          // reference cases, where reference ::= id
				
				finishPosition(idPos);
				Reference ref2 = new IdRef(id, idPos);
				
				switch(currentToken.kind) {
				
				case Token.EQUAL:						 //  Reference = Expression ;
					acceptIt();
					
					Expression expr15 = parseExpression();
					
					accept(Token.SEMICOLON);
					
					finishPosition(stmtPos);
					stmt = new AssignStmt(ref2, expr15, stmtPos);
					break;
					
					
				case Token.LBRAC:							// Reference [ Expression ] = Expression ;
					acceptIt();
					
					Expression expr13 = parseExpression();
					
					accept(Token.RBRAC);
					accept(Token.EQUAL);
					
					Expression expr14 = parseExpression();
					
					accept(Token.SEMICOLON);
					
					finishPosition(stmtPos);
					stmt = new IxAssignStmt(ref2, expr13, expr14, stmtPos);
					break;
					
					
					
				case Token.LPAREN:						// Reference ( ArgumentList? ) ;
					acceptIt();
					
					ExprList args = new ExprList();
					
					if (currentToken.kind == Token.RPAREN) {
						acceptIt();
					} else {
						args = parseArgument();
						accept(Token.RPAREN);
					}
					accept(Token.SEMICOLON);
					
					finishPosition(stmtPos);
					stmt = new CallStmt(ref2, args, stmtPos);
					break;
					
					
				default:
					reporter.reportError("invalid statement syntax" + " at lines: " + stmtPos.start + "-" + currentToken.posn.finish);
					break;
				}
			}
			break;
			
			
		case Token.THIS:                                 // reference cases
			Reference ref = parseReference();
			
			switch(currentToken.kind) {
			
			case Token.EQUAL:						 //  Reference = Expression ;
				acceptIt();
				
				Expression expr4 = parseExpression();
				
				accept(Token.SEMICOLON);
				
				finishPosition(stmtPos);
				stmt = new AssignStmt(ref, expr4, stmtPos);
				break;
				
				
			case Token.LBRAC: 						// Reference [ Expression ] = Expression ;
				acceptIt();
				
				Expression expr5 = parseExpression();
				
				accept(Token.RBRAC);
				accept(Token.EQUAL);
				
				Expression expr6 = parseExpression();
				
				accept(Token.SEMICOLON);
				
				finishPosition(stmtPos);
				stmt = new IxAssignStmt(ref, expr5, expr6, stmtPos);
				break;
				
				
			case Token.LPAREN:						// Reference ( ArgumentList? ) ;
				acceptIt();
				
				ExprList arguments = new ExprList();
				
				if (currentToken.kind == Token.RPAREN) {
					acceptIt();
				} else {
					arguments = parseArgument();
					accept(Token.RPAREN);
				}
				accept(Token.SEMICOLON);
				
				finishPosition(stmtPos);
				stmt = new CallStmt(ref, arguments, stmtPos);
				break;
				
			default:
				reporter.reportError("invalid statement syntax" + "a t lines: " + stmtPos.start + "-" + currentToken.posn.finish);
				break;
			}
			break;
			
		default:
			acceptIt();
			reporter.reportError("invalid statement syntax" + " at lines: " + stmtPos.start + "-" + currentToken.posn.finish);
			break;
		}
		
		return stmt;
		
	}
	
	
	// start of expression parsing 
	
	private Expression parseExpression() throws SyntaxError {
		return parseDisjunctionExpr();
	}
	
	
	private Expression parseDisjunctionExpr() throws SyntaxError {
		
		SourcePosition exprPos = new SourcePosition();
		startPosition(exprPos);
		
		Expression expr1 = parseConjunctionExpr();
		
		while (currentToken.spelling.equals("||")) {
			
			Operator op = new Operator(currentToken);			
			acceptIt();
			
			Expression expr2 = parseConjunctionExpr();
			
			finishPosition(exprPos);
			expr1 = new BinaryExpr(op, expr1, expr2, exprPos);
		}
		return expr1;
	}
	
	
	private Expression parseConjunctionExpr() throws SyntaxError {
		
		SourcePosition exprPos = new SourcePosition();
		startPosition(exprPos);
		
		Expression expr1 = parseEqualityExpr();
		
		while (currentToken.spelling.equals("&&")) {
			Operator op = new Operator(currentToken);			
			acceptIt();
			Expression expr2 = parseEqualityExpr();
			
			finishPosition(exprPos);
			expr1 = new BinaryExpr(op, expr1, expr2, exprPos);
		}
		
		return expr1;
	}
	
	
	private Expression parseEqualityExpr() throws SyntaxError {
		
		SourcePosition exprPos = new SourcePosition();
		startPosition(exprPos);
		
		Expression expr1 = parseRelationalExpr();
		
		while (currentToken.spelling.equals("==") || currentToken.spelling.equals("!=")) {
			Operator op = new Operator(currentToken);
			acceptIt();
			Expression expr2 = parseRelationalExpr();
			
			finishPosition(exprPos);
			expr1 = new BinaryExpr(op, expr1, expr2, exprPos);
		}
		return expr1;
	}
	
	
	private Expression parseRelationalExpr() throws SyntaxError {
		
		SourcePosition exprPos = new SourcePosition();
		startPosition(exprPos);
		
		Expression expr1 = parseAdditiveExpr();
		
		while (currentToken.spelling.equals("<=") || currentToken.spelling.equals("<") 
				|| currentToken.spelling.equals(">") || currentToken.spelling.equals(">=")) {
			Operator op = new Operator(currentToken);
			acceptIt();
			Expression expr2 = parseAdditiveExpr();
			
			finishPosition(exprPos);
			expr1 = new BinaryExpr(op, expr1, expr2, exprPos);
			
		}
		return expr1;
	}
	
	
	private Expression parseAdditiveExpr() throws SyntaxError {
		
		SourcePosition exprPos = new SourcePosition();
		startPosition(exprPos);
		
		Expression expr1 = parseMultiExpr();
		
		while (currentToken.spelling.equals("+") || currentToken.spelling.equals("-")) {
			Operator op = new Operator(currentToken);
			acceptIt();
			Expression expr2 = parseMultiExpr();
			
			finishPosition(exprPos);
			expr1 = new BinaryExpr(op, expr1, expr2, exprPos);
		}
		return expr1;
	}
	
	
	private Expression parseMultiExpr() throws SyntaxError {
		
		SourcePosition exprPos = new SourcePosition();
		startPosition(exprPos);
		
		Expression expr1 = parseUnaryExpr();
		
		while (currentToken.spelling.equals("*") || currentToken.spelling.equals("/")) {
			Operator op = new Operator(currentToken);
			acceptIt();
			Expression expr2 = parseUnaryExpr();
			
			finishPosition(exprPos);
			expr1 = new BinaryExpr(op, expr1, expr2, exprPos);
		}
		return expr1;
	}
	
	
	private Expression parseUnaryExpr() throws SyntaxError {
		
		SourcePosition exprPos = new SourcePosition();
		startPosition(exprPos);
		
		Expression expr;
		
		if (currentToken.spelling.equals("-") || currentToken.spelling.equals("!")) {
			Operator op = new Operator(currentToken);
			acceptIt();
			Expression expr2 = parseUnaryExpr();
			
			finishPosition(exprPos);
			expr = new UnaryExpr(op, expr2, exprPos);
			
		} else {
			expr = parseExpr();
		}
		
		return expr;
	}
	
	
	
	private Expression parseExpr() throws SyntaxError{
		
		SourcePosition exprPos = new SourcePosition();
		startPosition(exprPos);
		
		Expression expr = null;
		
		switch(currentToken.kind) {		
		
		case Token.INTLITERAL:			    					// num | true | false
			Terminal num = new IntLiteral(currentToken);			
			acceptIt();
			
			finishPosition(exprPos);
			expr = new LiteralExpr(num, exprPos);
			
			break;
			
		case Token.TRUE:	case Token.FALSE:
			Terminal bool = new BooleanLiteral(currentToken);
			acceptIt();
			
			finishPosition(exprPos);
			expr = new LiteralExpr(bool, exprPos);
			
			break;
		
			
		case Token.NULL:
			Terminal n = new NullLiteral(currentToken);
			acceptIt();
			
			finishPosition(exprPos);
			expr = new LiteralExpr(n, exprPos);
			
			break;
			
		case Token.LPAREN:                 // ( Expression )
			acceptIt();
			
			expr = parseExpression();
			
			accept(Token.RPAREN);
			break;
			
			
		case Token.NEW:                     //  new ( id () | int [ Expression ] | id [ Expression ] )
			acceptIt();
			
			if (currentToken.kind == Token.IDENTIFIER) {  
				
				SourcePosition idPos = new SourcePosition();
				startPosition(idPos);
				
				Identifier new_id = new Identifier(currentToken);
				
				acceptIt();
				
				if (currentToken.kind == Token.LPAREN) {
					acceptIt();
					accept(Token.RPAREN);
					
					finishPosition(idPos);
					ClassType type = new ClassType(new_id, idPos);
					
					finishPosition(exprPos);
					expr = new NewObjectExpr(type, exprPos);
					
				} else if (currentToken.kind == Token.LBRAC) {
					acceptIt();
					
					Expression expr1 = parseExpression();
					
					accept(Token.RBRAC);
					
					finishPosition(idPos);
					TypeDenoter type = new ClassType(new_id, idPos);
					
					finishPosition(exprPos);
					expr = new NewArrayExpr(type, expr1, exprPos);
					
				} else {
					reporter.reportError("invalid expression sytnax" + " at lines: " + exprPos.start + "-" + currentToken.posn.finish);
				}
			} else if (currentToken.kind == Token.INT) {
				
				TypeDenoter type = new BaseType(TypeKind.INT, currentToken.posn);
				
				acceptIt();
				accept(Token.LBRAC);
				
				Expression expr1 = parseExpression();
				
				accept(Token.RBRAC);
				
				finishPosition(exprPos);
				expr = new NewArrayExpr(type, expr1, exprPos);
						
			} else {
				reporter.reportError("invalid expression syntax" + " at lines: " + exprPos.start + "-" + currentToken.posn.finish);
			}
			break;
			
			
		case Token.IDENTIFIER:	case Token.THIS:        // reference cases
			Reference ref = parseReference();                             
			
			finishPosition(exprPos);
			expr = new RefExpr(ref, exprPos);                       // Reference
			
			if (currentToken.kind == Token.LBRAC) {          // Reference [ Expression ]
				acceptIt();
				
				Expression expr1 = parseExpression();
				
				accept(Token.RBRAC);
				
				finishPosition(exprPos);
				expr = new IxExpr(ref, expr1, exprPos);
				
			} else if (currentToken.kind == Token.LPAREN) {          // Reference ( ArgumentList? )
				acceptIt();
				
				ExprList arguments = new ExprList();
				
				if (currentToken.kind == Token.RPAREN) {
					acceptIt();
				} else {
					arguments = parseArgument();
					accept(Token.RPAREN);
				}
				
				finishPosition(exprPos);
				expr = new CallExpr(ref, arguments, exprPos);
			}
			break;
		
		
		default:
			acceptIt();
			reporter.reportError("invalid expression syntax" + " at lines: " + exprPos.start + "-" + currentToken.posn.finish);
			break;
		}
		
		// finish parsing an expression
		return expr;
	}
	
	
	private Identifier parseIdentifier() {
		Identifier id = null;
		
		if (currentToken.kind == Token.IDENTIFIER) {
			id = new Identifier(currentToken);
			acceptIt();
		} else {
			reporter.reportError("expected identifier but was " + currentToken.spelling + " at lines: " + currentToken.posn.start + "-" + currentToken.posn.finish);
			id = new Identifier(currentToken);
		}
		
		return id;
	}
	
	/*private void pTrace() {
		StackTraceElement [] stl = Thread.currentThread().getStackTrace();
		for (int i = stl.length - 1; i > 0 ; i--) {
			if(stl[i].toString().contains("parse"))
				System.out.println(stl[i]);
		}
		System.out.println("accepting: " + currentToken.kind + " (\"" + currentToken.spelling + "\")");
		System.out.println();
	}*/
}
