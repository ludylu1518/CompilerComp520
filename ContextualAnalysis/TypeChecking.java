package miniJava.ContextualAnalysis;

import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.ErrorReporter;

public class TypeChecking implements Visitor<TypeDenoter, TypeDenoter> {
	
	
	private AST ast;
	private ErrorReporter reporter;
	
	private boolean isMain;
	private int numMain;
	private boolean correctMain;
	public MethodDecl mainDecl;

	
	
	public TypeChecking(AST ast, ErrorReporter reporter) {
		this.ast = ast;
		this.reporter = reporter;
		this.numMain = 0;
		this.isMain = false;
		this.correctMain = false;
	}
	
	
	public void startTypeChecking() {
		ast.visit(this, null);
	
		
		if (numMain != 1) {
			
			if (numMain == 0) {
				reporter.reportError("*** Expected 1 main method but found none");
			} else {
				reporter.reportError("*** Expected 1 main method but found " + numMain);
			}
			
		} else if (!correctMain) {
			reporter.reportError("*** Invalid main method declaration, expect exactly: public static void main (string [] args)");
		}
		
	}
		

	@Override
	public TypeDenoter visitPackage(Package prog, TypeDenoter arg) {
		// TODO Auto-generated method stub
		for (ClassDecl cd : prog.classDeclList) {
			cd.visit(this, arg);
		}
		
				
		return null;
	}

	@Override
	public TypeDenoter visitClassDecl(ClassDecl cd, TypeDenoter arg) {
		// TODO Auto-generated method stub
		
		for (FieldDecl fd : cd.fieldDeclList) {
			fd.visit(this, arg);
		}
		
		for (MethodDecl md : cd.methodDeclList) {
			
			if (md.name.equals("main")) {
				numMain++;
				isMain = true;
				mainDecl = md;
			}
			
			md.visit(this, arg);
			
		}
		
		return null;
	}

	@Override
	public TypeDenoter visitFieldDecl(FieldDecl fd, TypeDenoter arg) {
		// TODO Auto-generated method stub
		
		fd.type = fd.type.visit(this, arg);
		
		return fd.type;
	}

	@Override
	public TypeDenoter visitMethodDecl(MethodDecl md, TypeDenoter arg) {
		// TODO Auto-generated method stub
		
		md.type = md.type.visit(this, arg);
		
		if (isMain && numMain == 1) {
					
			if (!md.isPrivate && md.isStatic && md.type.typeKind == TypeKind.VOID && md.parameterDeclList.size() == 1) {
				correctMain = true;
			} else {
				correctMain = false;
			}
			
		}
				
		md.returnType = null;
		
		for (ParameterDecl pd : md.parameterDeclList) {
			
			pd.visit(this, arg);
		}
		
		for (Statement s : md.statementList) {
			s.correspondingMethod = md;
			s.visit(this, arg);
		}
		
		if (md.type.typeKind == TypeKind.VOID) {
			
			if (md.returnType != null && md.returnType.typeKind != TypeKind.VOID) {
				
				reporter.reportError("*** line " + md.posn + ": error, invalid return type");
			}
			
		} else {
			
		
			if (!(md.type.equals(md.returnType))) {
				
				reporter.reportError("*** line " + md.posn + ": error, return type don't match expected type");
			}
		
		}
		
		
		return md.type;
	}

	@Override
	public TypeDenoter visitParameterDecl(ParameterDecl pd, TypeDenoter arg) {
		// TODO Auto-generated method stub
		
		pd.type = pd.type.visit(this, arg);
		
	
		if (isMain && correctMain && numMain == 1) {

			if (pd.type.typeKind == TypeKind.ARRAY) {
				ArrayType pt = (ArrayType) pd.type;
				
				if (pt.eltType.typeKind == TypeKind.UNSUPPORTED && pd.name.equals("args")) {
					correctMain = true;
				} else {
					correctMain = false;
				}
			} else {
				correctMain = false;
			}
			isMain = false;
		}
	
		
		
		if (pd.type.typeKind == TypeKind.VOID) {
			reporter.reportError("*** line " + pd.posn.start + ": error, parameter can't have void type");
		}
		
		
		return pd.type;
	}

	@Override
	public TypeDenoter visitVarDecl(VarDecl decl, TypeDenoter arg) {
		// TODO Auto-generated method stub
		decl.type = decl.type.visit(this, arg);
		
		if (decl.type.typeKind == TypeKind.VOID) {
			reporter.reportError("*** line " + decl.posn.start + ": error, can't have void as declared variable type");
		}
		
		
		return decl.type;
	}

	@Override
	public TypeDenoter visitBaseType(BaseType type, TypeDenoter arg) {
		// TODO Auto-generated method stub
		return type;
	}

	@Override
	public TypeDenoter visitClassType(ClassType type, TypeDenoter arg) {
		// TODO Auto-generated method stub
		
		if (type.className.spelling.equals("String")) {
			return new BaseType(TypeKind.UNSUPPORTED, type.posn);
		}
		
		return type;
	}

	@Override
	public TypeDenoter visitArrayType(ArrayType type, TypeDenoter arg) {
		// TODO Auto-generated method stub
		TypeDenoter t = type.eltType.visit(this, arg);
	
		
		return new ArrayType(t, type.posn);
	}

	@Override
	public TypeDenoter visitBlockStmt(BlockStmt stmt, TypeDenoter arg) {
		// TODO Auto-generated method stub
		for (Statement s : stmt.sl) {
			s.visit(this, arg);
		}
		
		return null;
	}

	@Override
	public TypeDenoter visitVardeclStmt(VarDeclStmt stmt, TypeDenoter arg) {
		// TODO Auto-generated method stub
		TypeDenoter t = stmt.varDecl.visit(this, arg);
		
		if (stmt.initExp != null) {
			TypeDenoter type = stmt.initExp.visit(this, arg);
			
			if (type == null) {
				reporter.reportError("*** line " + stmt.posn + ": error, cannot set variable to class");
				
			}else if (type.typeKind == TypeKind.ERROR || type.typeKind == TypeKind.UNSUPPORTED) {
				reporter.reportError("*** line " + stmt.posn + ": error, incompatible types");
				
			} else if (!t.equals(type)) {
				reporter.reportError("*** line " + stmt.posn + ": error, incompatible types");
			}
	
			
		}
		
		return null;
	}

	@Override
	public TypeDenoter visitAssignStmt(AssignStmt stmt, TypeDenoter arg) {
		// TODO Auto-generated method stub
		
		TypeDenoter refType = stmt.ref.visit(this, arg);
		TypeDenoter exprType = stmt.val.visit(this, arg);
		
		if (refType.typeKind == TypeKind.ERROR || refType.typeKind == TypeKind.UNSUPPORTED 
				|| exprType.typeKind == TypeKind.ERROR || exprType.typeKind == TypeKind.UNSUPPORTED) {
			
			reporter.reportError("*** line " + stmt.posn + ": error, incompatible types");
		} else {
			
			if (!refType.equals(exprType)) {
				reporter.reportError("*** line " + stmt.posn + ": error, incompatible types");
			}
		}
		
		
		return null;
	}

	@Override
	public TypeDenoter visitIxAssignStmt(IxAssignStmt stmt, TypeDenoter arg) {
		// TODO Auto-generated method stub
		
		TypeDenoter refType = stmt.ref.visit(this, arg);
		TypeDenoter e1Type = stmt.ix.visit(this, arg);
		TypeDenoter e2Type = stmt.exp.visit(this, arg);
		
		if (!(refType instanceof ArrayType)) {
			reporter.reportError("*** line " + stmt.posn + ": error, expected arraytype");
			return null;
		}
		
		if (!(e1Type.typeKind == TypeKind.INT)) {
			reporter.reportError("*** line " + stmt.posn + ": error, expected int type for index");
			return null;
		} 
		
		if (!(((ArrayType) refType).eltType.equals(e2Type))) {
			reporter.reportError("*** line " + stmt.posn + ": error, incompatible types");
		}
		
		
		return null;
	}

	@Override
	public TypeDenoter visitCallStmt(CallStmt stmt, TypeDenoter arg) {
		// TODO Auto-generated method stub
		if (stmt.methodRef.decl instanceof MethodDecl) {
			
			MethodDecl md = (MethodDecl) stmt.methodRef.decl;
			
			if (md.parameterDeclList.size() != stmt.argList.size()) {
				reporter.reportError("*** line " + stmt.posn + ": error, parameters and arguments don't match");
				
			} else {
				
				for (int i = 0; i < stmt.argList.size(); i++) {
					TypeDenoter pType = stmt.argList.get(i).visit(this, arg);
					TypeDenoter aType =md.parameterDeclList.get(i).visit(this, arg);
					
					if (! (pType.equals(aType))) {
						reporter.reportError("*** line " + stmt.posn + ": error, incompatible arg vs param type");
					}
				}
				
			}
			
		} else {
			reporter.reportError("*** line " + stmt.posn + ": error, tried to call non-method declaration");
		}
		
		
		return null;
	}

	@Override
	public TypeDenoter visitReturnStmt(ReturnStmt stmt, TypeDenoter arg) {
		// TODO Auto-generated method stub
		TypeDenoter type;
		
		if (stmt.returnExpr == null) {
			type = new BaseType(TypeKind.VOID, stmt.posn);
		} else {
			type = stmt.returnExpr.visit(this, arg);
		}
		
		if (stmt.correspondingMethod.returnType == null) {
			stmt.correspondingMethod.returnType = type;
		} else {
			
			if (!(stmt.correspondingMethod.returnType.equals(type))) {
				reporter.reportError("*** line " + stmt.posn + ": error, returning two different types");
			}
			
		}
		
		
		return null;
	}

	@Override
	public TypeDenoter visitIfStmt(IfStmt stmt, TypeDenoter arg) {
		// TODO Auto-generated method stub
		
		TypeDenoter boolType = stmt.cond.visit(this, arg);
	
		if (boolType.typeKind != TypeKind.BOOLEAN) {
			reporter.reportError("*** line " + stmt.posn + ": error, expected type bool");
		}
		
		stmt.thenStmt.correspondingMethod = stmt.correspondingMethod;
		stmt.thenStmt.visit(this, arg);
		
		if (stmt.elseStmt != null) {
			stmt.elseStmt.correspondingMethod = stmt.correspondingMethod;
			stmt.elseStmt.visit(this, arg);
			
		}
		
		return null;
	}

	@Override
	public TypeDenoter visitWhileStmt(WhileStmt stmt, TypeDenoter arg) {
		// TODO Auto-generated method stub
		TypeDenoter boolType = stmt.cond.visit(this, arg);
		
		if (boolType.typeKind != TypeKind.BOOLEAN) {
			reporter.reportError("*** line " + stmt.posn + ": error, expected type bool");
		}
		
		stmt.body.correspondingMethod = stmt.correspondingMethod;
		stmt.body.visit(this, arg);
		
		return null;
	}

	@Override
	public TypeDenoter visitUnaryExpr(UnaryExpr expr, TypeDenoter arg) {
		// TODO Auto-generated method stub
		TypeDenoter type = expr.expr.visit(this, arg);
		
		if (expr.operator.spelling.equals("-")) {
			
			if (type.typeKind == TypeKind.INT) {
				
				return new BaseType(TypeKind.INT, expr.posn);				
			}
			
			return new BaseType(TypeKind.ERROR, expr.posn);
			
		} else if (expr.operator.spelling.equals("!")) {
			
			if (type.typeKind == TypeKind.BOOLEAN) {
				
				return new BaseType(TypeKind.BOOLEAN, expr.posn);
			}
			
			return new BaseType(TypeKind.ERROR, expr.posn);
		}
		
		
		return new BaseType(TypeKind.ERROR, expr.posn);
	}

	@Override
	public TypeDenoter visitBinaryExpr(BinaryExpr expr, TypeDenoter arg) {
		// TODO Auto-generated method stub
		String op = expr.operator.spelling;
		
		TypeDenoter type1 = expr.left.visit(this, arg);
		TypeDenoter type2 = expr.right.visit(this, arg);
		
		if (op.equals("||") || op.equals("&&")) {
			
			if (type1.typeKind == TypeKind.BOOLEAN && type2.typeKind == TypeKind.BOOLEAN) {
				
				return new BaseType(TypeKind.BOOLEAN, expr.posn);
			}
			
			return new BaseType(TypeKind.ERROR, expr.posn);	
			
		} else if (op.equals("==") || op.equals("!=")) {
			
			if (type1.equals(type2)) {
		
				return new BaseType(TypeKind.BOOLEAN, expr.posn);
			}
			
			return new BaseType(TypeKind.ERROR, expr.posn);
			
		} else if (op.equals("<=") || op.equals("<") || op.equals(">") || op.equals(">=")) {
			
			if (type1.typeKind == TypeKind.INT && type2.typeKind == TypeKind.INT) {
				
				return new BaseType(TypeKind.BOOLEAN, expr.posn);
			}
			
			return new BaseType(TypeKind.ERROR, expr.posn);
			
		} else if (op.equals("+") || op.equals("-") || op.equals("*") || op.equals("/")) {
			
			if (type1.typeKind == TypeKind.INT && type2.typeKind == TypeKind.INT) {
				
				return new BaseType(TypeKind.INT, expr.posn);
			}
			
			return new BaseType(TypeKind.ERROR, expr.posn);
		}
		
		
		return new BaseType(TypeKind.ERROR, expr.posn);
	}

	@Override
	public TypeDenoter visitRefExpr(RefExpr expr, TypeDenoter arg) {
		// TODO Auto-generated method stub
		
		return expr.ref.visit(this, arg);
	}

	@Override
	public TypeDenoter visitIxExpr(IxExpr expr, TypeDenoter arg) {
		// TODO Auto-generated method stub
		
		TypeDenoter refType = expr.ref.visit(this, arg);
		
		if (!(refType instanceof ArrayType)) {
			
			return new BaseType(TypeKind.ERROR, expr.posn);
		}
		
		ArrayType type = (ArrayType) refType;
		
		TypeDenoter exprType = expr.ixExpr.visit(this, arg);
		
		if (exprType.typeKind == TypeKind.INT) {
			
			return type.eltType;
		}
		
		return new BaseType(TypeKind.ERROR, expr.posn);
	}

	@Override
	public TypeDenoter visitCallExpr(CallExpr expr, TypeDenoter arg) {
		// TODO Auto-generated method stub
	
		
		if (expr.functionRef.decl instanceof MethodDecl) {
			MethodDecl md = (MethodDecl) expr.functionRef.decl;
			
			if (md.parameterDeclList.size() != expr.argList.size()) {
				reporter.reportError("*** line " + expr.posn + ": error, number of args and params don't match");
				
				return new BaseType(TypeKind.ERROR, expr.posn);
			} 
			
			for (int i = 0; i < md.parameterDeclList.size(); i++) {
				
				TypeDenoter pType = md.parameterDeclList.get(i).visit(this, arg);
				TypeDenoter aType = expr.argList.get(i).visit(this, arg);
				
				if (!(pType.equals(aType))) {
					
					reporter.reportError("*** line " + expr.posn + ": error, type of arg and param don't match on position " + i);
					
					return new BaseType(TypeKind.ERROR, expr.posn);
				}
			}
			
			return md.type;
		}
		
		return new BaseType(TypeKind.ERROR, expr.posn);
	}

	@Override
	public TypeDenoter visitLiteralExpr(LiteralExpr expr, TypeDenoter arg) {
		// TODO Auto-generated method stub
		return expr.lit.visit(this, arg);
	}

	@Override
	public TypeDenoter visitNewObjectExpr(NewObjectExpr expr, TypeDenoter arg) {
		// TODO Auto-generated method stub
		return expr.classtype.visit(this, arg);
	}

	@Override
	public TypeDenoter visitNewArrayExpr(NewArrayExpr expr, TypeDenoter arg) {
		// TODO Auto-generated method stub
		
		TypeDenoter exprType = expr.sizeExpr.visit(this, arg);
		
		if (exprType.typeKind == TypeKind.INT) {
			
			return new ArrayType(expr.eltType, expr.posn);
		}
		
		return new BaseType(TypeKind.ERROR, expr.posn);
	}

	@Override
	public TypeDenoter visitThisRef(ThisRef ref, TypeDenoter arg) {
		// TODO Auto-generated method stub
		if (ref.decl instanceof ClassDecl) {
			ClassDecl dl = (ClassDecl) ref.decl;
			return dl.type;
		}
		
		return ref.decl.type;
	}

	@Override
	public TypeDenoter visitIdRef(IdRef ref, TypeDenoter arg) {
		// TODO Auto-generated method stub
		if (ref.decl.type == null) {
			System.out.println("*** line " + ref.posn + ": error, invalid reference");
			System.out.println("Identification error");
			System.exit(4);
		}
		return ref.decl.type;
	}

	@Override
	public TypeDenoter visitQRef(QualRef ref, TypeDenoter arg) {
		// TODO Auto-generated method stub
		return ref.decl.type;
	}

	@Override
	public TypeDenoter visitIdentifier(Identifier id, TypeDenoter arg) {
		// TODO Auto-generated method stub
		return id.decl.type;
	}

	@Override
	public TypeDenoter visitOperator(Operator op, TypeDenoter arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public TypeDenoter visitIntLiteral(IntLiteral num, TypeDenoter arg) {
		// TODO Auto-generated method stub
		return new BaseType(TypeKind.INT, num.posn);
	}

	@Override
	public TypeDenoter visitBooleanLiteral(BooleanLiteral bool, TypeDenoter arg) {
		// TODO Auto-generated method stub
		return new BaseType(TypeKind.BOOLEAN, bool.posn);
	}

	@Override
	public TypeDenoter visitNullLiteral(NullLiteral n, TypeDenoter arg) {
		// TODO Auto-generated method stub
		return new BaseType(TypeKind.NULL, n.posn);
	}

	
	
}
