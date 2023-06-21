package miniJava.ContextualAnalysis;

import miniJava.ErrorReporter;
import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.SyntaticAnalyzer.SourcePosition;
import miniJava.SyntaticAnalyzer.Token;

public class Identification implements Visitor<Object, Object>{

	
	public IdentificationTable table;
	private ErrorReporter reporter;
	private ClassDecl currClass;
	private AST ast;
	private String VarDeclName;
	
	private ClassDecl printClass;
	private ClassDecl systemClass;
	private ClassDecl stringClass;
	private int methodcall;
	private int methodLine;
	private Statement lastStmt;
	private int lastStmtLine;
	private int BuiltIndex;

	public Identification(AST ast, ErrorReporter reporter) {
		this.reporter = reporter;
		table = new IdentificationTable(reporter);
		this.ast = ast;
		methodcall = 0;
		
		// initializing intial env
		
		SourcePosition p = new SourcePosition();
		
		// _Printstream class
		FieldDeclList fList = new FieldDeclList();
		MethodDeclList mList = new MethodDeclList();
		
		TypeDenoter mdType = new BaseType(TypeKind.VOID, p);
		
		MemberDecl md = new FieldDecl(false, false, mdType, "println", p);
		
		ParameterDeclList pdList = new ParameterDeclList();
		StatementList stmtList = new StatementList();
		
		TypeDenoter pdType = new BaseType(TypeKind.INT, p);
		ParameterDecl pd = new ParameterDecl(pdType, "n", p);
		
		pdList.add(pd);
		
		MethodDecl m = new MethodDecl(md, pdList, stmtList, p);
		
		mList.add(m);
		
		this.printClass = new ClassDecl("_PrintStream", fList, mList, p);
		
		// System class
		FieldDeclList fList1 = new FieldDeclList();
		MethodDeclList mList1 = new MethodDeclList();
		
		Token t = new Token(Token.IDENTIFIER, "_PrintStream", p);
		Identifier f1Id = new Identifier(t);
		TypeDenoter f1Type = new ClassType(f1Id, p);
		
		FieldDecl f1 = new FieldDecl(false, true, f1Type, "out", p);
		fList1.add(f1);
		
		this.systemClass = new ClassDecl("System", fList1, mList1, p);
		
		
		// String class
		FieldDeclList fList2 = new FieldDeclList();
		MethodDeclList mList2 = new MethodDeclList();
		
		this.stringClass = new ClassDecl("String", fList2, mList2, p);
		
	}
	
	public void startIdentification() {
		ast.visit(this, null);
	}
	
	@Override
	public Object visitPackage(Package prog, Object arg) {
		// TODO Auto-generated method stub
		table.openScope();
		
		BuiltIndex = 3;
		
		// enter intial env
		table.enter(printClass);
		table.enter(systemClass);
		table.enter(stringClass);
		
		for (ClassDecl cd : prog.classDeclList) {
			table.enter(cd);
		}
		
		
		printClass.visit(this, null);
		systemClass.visit(this, null);
		stringClass.visit(this, null);
		
		for (ClassDecl cd : prog.classDeclList) {
			currClass = cd;
			cd.visit(this, null);
		}
		
		table.closeScope();
		

		if (methodcall != 0) {
			reporter.reportError("*** line " + methodLine + ": error invalid method call");
		}
		
		return null;
	}

	@Override
	public Object visitClassDecl(ClassDecl cd, Object arg) {
		// TODO Auto-generated method stub
		
		// add members so all fields and methods are visible
		table.openScope();
		for(FieldDecl fd: cd.fieldDeclList) {
			table.enter(fd);
		}
		
		if (BuiltIndex != 0) {
		
			for(MethodDecl md: cd.methodDeclList) {
				table.enter(md);
				md.builtIn = true;
			}
			BuiltIndex--;
			
		} else {
			
			for(MethodDecl md: cd.methodDeclList) {
				table.enter(md);
			}
		}
					
		// visit all members
		for(FieldDecl fd: cd.fieldDeclList) {
			fd.visit(this, null);
		}
		
		for(MethodDecl md: cd.methodDeclList) {
			md.visit(this, null);
		}
				
		table.closeScope();
		return null;
	}

	@Override
	public Object visitFieldDecl(FieldDecl fd, Object arg) {
		// TODO Auto-generated method stub
		fd.type.visit(this, null);
		
		return null;
	}

	@Override
	public Object visitMethodDecl(MethodDecl md, Object arg) {
		// TODO Auto-generated method stub
	
		md.type.visit(this, null);
				
		table.openScope();
		
		for(ParameterDecl pd: md.parameterDeclList) {
			pd.visit(this, null);
		}
		
		table.openScope();
		
		for(Statement st: md.statementList) {
			st.visit(this, md);
		}
		
		if (md.type.typeKind != TypeKind.VOID) {
			if (!(lastStmt instanceof ReturnStmt)) {
				reporter.reportError("*** line " + lastStmtLine + ": error, expected return stmt as last stmt but was " + lastStmt);
			}
		}
		
		table.closeScope();
		table.closeScope();

		return null;
	}

	@Override
	public Object visitParameterDecl(ParameterDecl pd, Object arg) {
		// TODO Auto-generated method stub		
		pd.type.visit(this, null);
	
		table.enter(pd);
		
		return null;
	}

	@Override
	public Object visitVarDecl(VarDecl decl, Object arg) {
		// TODO Auto-generated method stub
		decl.type.visit(this, null);
		
		table.enter(decl);
		
		return null;
	}

	@Override
	public Object visitBaseType(BaseType type, Object arg) {     
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitClassType(ClassType type, Object arg) {
		// TODO Auto-generated method stub
		Declaration decl = table.findClass(type.className.spelling);
		
		if (decl != null) {
			ClassDecl ct = (ClassDecl) decl;
			type.className.decl = ct;
		} else {
			reporter.reportError("*** line " + type.posn + ": error, class not defined");
		}
		
		return null;
	}

	@Override
	public Object visitArrayType(ArrayType type, Object arg) {
		// TODO Auto-generated method stub		
	
		type.eltType.visit(this, null);		
		return null;
	}

	@Override
	public Object visitBlockStmt(BlockStmt stmt, Object arg) {
		// TODO Auto-generated method stub
		MethodDecl md = (MethodDecl) arg;
		
		table.openScope();
		
		for (Statement st : stmt.sl) {
			st.visit(this, md);
			lastStmt = st;
			lastStmtLine = st.posn.start;
		}
		
		
		table.closeScope();
		return null;
	}

	@Override
	public Object visitVardeclStmt(VarDeclStmt stmt, Object arg) {
		// TODO Auto-generated method stub
		MethodDecl md = (MethodDecl) arg;
		VarDeclName = stmt.varDecl.name;         // check if initExp has ref same name as VarDeclName
		
		stmt.initExp.visit(this, md);
		
		VarDeclName = "";                        // reset VarDeclName
		stmt.varDecl.visit(this, null);
	
		lastStmt = stmt;
		lastStmtLine = stmt.posn.start;
		
		return null;
	}

	@Override
	public Object visitAssignStmt(AssignStmt stmt, Object arg) {
		// TODO Auto-generated method stub
		MethodDecl md = (MethodDecl) arg;
		
		stmt.ref.visit(this, md);
		stmt.val.visit(this, md);
		
		lastStmt = stmt;
		lastStmtLine = stmt.posn.start;
		
		if (stmt.ref.decl instanceof FieldDecl) {
			FieldDecl rfd = (FieldDecl) stmt.ref.decl;		
					
			if (rfd.name.equals("length")) {
				reporter.reportError("*** line " + stmt.ref.posn + ": error, cannot set the length of an array, only readable");
			
			}
		
		}
		
		return null;
	}

	@Override
	public Object visitIxAssignStmt(IxAssignStmt stmt, Object arg) {
		// TODO Auto-generated method stub
		MethodDecl md = (MethodDecl) arg;
		
		stmt.ref.visit(this, md);
		stmt.ix.visit(this, md);
		stmt.exp.visit(this, md);
		
		lastStmt = stmt;
		lastStmtLine = stmt.posn.start;
		
		return null;
	}

	@Override
	public Object visitCallStmt(CallStmt stmt, Object arg) {
		// TODO Auto-generated method stub
		MethodDecl md = (MethodDecl) arg;
		
		stmt.methodRef.visit(this, md);
	
		for (Expression e : stmt.argList) {
			e.visit(this, md);
		}
		
		lastStmt = stmt;
		lastStmtLine = stmt.posn.start;
		
		methodcall--;
		return null;
	}

	@Override
	public Object visitReturnStmt(ReturnStmt stmt, Object arg) {
		// TODO Auto-generated method stub
		MethodDecl md = (MethodDecl) arg;
		
		if (stmt.returnExpr != null) {
			stmt.returnExpr.visit(this, md);
		}
		
		lastStmt = stmt;
		lastStmtLine = stmt.posn.start;
		
		return null;
	}

	@Override
	public Object visitIfStmt(IfStmt stmt, Object arg) {
		// TODO Auto-generated method stub
		MethodDecl md = (MethodDecl) arg;
		
		stmt.cond.visit(this, md);

		
		if (stmt.thenStmt instanceof VarDeclStmt) {
			
			reporter.reportError("*** line " + stmt.posn + ": error, conditional stmt can't be followed by only varDecl stmt");
		}
		
		stmt.thenStmt.visit(this, md);
		
		if (stmt.elseStmt != null) {
			
			if (stmt.elseStmt instanceof VarDeclStmt) {
				
				reporter.reportError("*** line " + stmt.posn + ": error, conditional stmt can't be followed by only varDecl stmt");
			}
			
			stmt.elseStmt.visit(this, md);
		}
		lastStmt = stmt;
		lastStmtLine = stmt.posn.start;
		
		return null;
	}

	@Override
	public Object visitWhileStmt(WhileStmt stmt, Object arg) {
		// TODO Auto-generated method stub
		MethodDecl md = (MethodDecl) arg;
		
		stmt.cond.visit(this, md);
		
		if (stmt.body instanceof VarDeclStmt) {
			
			reporter.reportError("*** line " + stmt.posn + ": error, conditional stmt can't be followed by only varDecl stmt");
		}
		
		stmt.body.visit(this, md);
		
		lastStmt = stmt;
		lastStmtLine = stmt.posn.start;
		
		return null;
	}

	@Override
	public Object visitUnaryExpr(UnaryExpr expr, Object arg) {
		// TODO Auto-generated method stub
		MethodDecl md = (MethodDecl) arg;
		
		expr.operator.visit(this, null);
		expr.expr.visit(this, md);
		
		return null;
	}

	@Override
	public Object visitBinaryExpr(BinaryExpr expr, Object arg) {
		// TODO Auto-generated method stub
		MethodDecl md = (MethodDecl) arg;
		
		expr.operator.visit(this, null);
		expr.left.visit(this, md);
		expr.right.visit(this, md);
		
		return null;
	}

	@Override
	public Object visitRefExpr(RefExpr expr, Object arg) {
		// TODO Auto-generated method stub
		MethodDecl md = (MethodDecl) arg;
		
		expr.ref.visit(this, md);
		
		return null;
	}

	@Override
	public Object visitIxExpr(IxExpr expr, Object arg) {
		// TODO Auto-generated method stub
		MethodDecl md = (MethodDecl) arg;
		
		expr.ref.visit(this, md);
		expr.ixExpr.visit(this, md);
		
		return null;
	}

	@Override
	public Object visitCallExpr(CallExpr expr, Object arg) {
		// TODO Auto-generated method stub
		MethodDecl md = (MethodDecl) arg;
		
		expr.functionRef.visit(this, md);
		
		for (Expression e : expr.argList) {
			e.visit(this, md);
		}
		
		methodcall--;
		
		return null;
	}

	@Override
	public Object visitLiteralExpr(LiteralExpr expr, Object arg) {
		// TODO Auto-generated method stub		
		return null;
	}

	@Override
	public Object visitNewObjectExpr(NewObjectExpr expr, Object arg) {
		// TODO Auto-generated method stub
				
		expr.classtype.visit(this, null);
		
		return null;
	}

	@Override
	public Object visitNewArrayExpr(NewArrayExpr expr, Object arg) {
		// TODO Auto-generated method stub
		MethodDecl md = (MethodDecl) arg;
		
		expr.eltType.visit(this, null);
		expr.sizeExpr.visit(this, md);
		
		return null;
	}

	@Override
	public Object visitThisRef(ThisRef ref, Object arg) {
		// TODO Auto-generated method stub
		MethodDecl md = (MethodDecl) arg;
		
		if (md.isStatic) {
			reporter.reportError("*** line " +  ref.posn.start + ": error, can't use this in a static method");
		}
		
		ref.decl = currClass;
		
		return null;
	}

	@Override
	public Object visitIdRef(IdRef ref, Object arg) {
		// TODO Auto-generated method stub
		MethodDecl md = (MethodDecl) arg;
		
		ref.id.visit(this, md);
		
		if (ref.id.spelling.equals(VarDeclName)) {
			reporter.reportError("*** line " +  ref.posn.start + ": error, cannot reference same name as initializing name");
		}
		
		ref.decl = ref.id.decl;
		
		if (ref.decl instanceof MethodDecl) {
			methodcall++;
			methodLine = ref.posn.start;
		}
		
		return null;
	}

	@Override
	public Object visitQRef(QualRef ref, Object arg) {
		// TODO Auto-generated method stub
		MethodDecl md = (MethodDecl) arg;
		
		ref.ref.visit(this, md);
	
		
		if (ref.ref.decl instanceof MethodDecl) {
			methodcall++;
			methodLine = ref.ref.posn.start;
		}
				
		Declaration decl = ref.ref.decl;
		
		if (decl == null) {
			reporter.reportError("*** line " + ref.posn.start + ": error, referencing non-exiting object");
			return null;
		}
		
		if (decl instanceof ClassDecl) {
			ClassDecl cd = (ClassDecl) decl;
			
			Declaration d = null;
			
			for (FieldDecl f : cd.fieldDeclList) {
				if (f.name.equals(ref.id.spelling)) {
					d = f;
				}
				if (d != null) {
					break;
				}
			}
			
			if (d == null) {
				for (MethodDecl m : cd.methodDeclList) {
					if (m.name.equals(ref.id.spelling)) {
						d = m;
					}
					if (d != null) {
						break;
					}
				}
			}
			
			if (d == null) {
				reporter.reportError("*** line " + ref.posn + ": error, reference not found in class");
				return null;
			}
			
			if (d instanceof MemberDecl) {
				MemberDecl md1 = (MemberDecl) d;
				
				if (d instanceof MethodDecl) {
					methodcall++;
					methodLine = ref.id.posn.start;
				}
				
				if (md.isStatic && !md1.isStatic) {
					reporter.reportError("*** line " + ref.posn + ": error, cannot reference non-static member in a static method");
					return null;
				}
				
				if (!md1.isStatic && !cd.name.equals(currClass.name)) {
					reporter.reportError("*** line " + ref.posn + ": error, cannot refernce non-static member of another class");
					return null;
				}
				
				if (md1.isPrivate && !cd.name.equals(currClass.name)) {
					reporter.reportError("*** line " + ref.posn + ": error, cannot reference private member of another class");
					return null;
				}
			}
			
			ref.id.decl = d;
			ref.decl = d;
			//System.out.println(ref.decl);
			
		} else if (decl instanceof MemberDecl) {
			MemberDecl _md = (MemberDecl) decl;
			
			if (_md.type.typeKind == TypeKind.CLASS) {
				ClassType ct = (ClassType) _md.type;
				ClassDecl cd = (ClassDecl) table.findClass(ct.className.spelling);
				
				if (cd == null) {
					reporter.reportError("*** line " + ref.posn + ": error, class not found.");
				} else {
				
					Declaration d = null;
					
					for (FieldDecl f : cd.fieldDeclList) {
						if (f.name.equals(ref.id.spelling)) {
							d = f;
						}
						if (d != null) {
							break;
						}
					}
					
					if (d == null) {
						for (MethodDecl m : cd.methodDeclList) {
							if (m.name.equals(ref.id.spelling)) {
								d = m;
							}
							if (d != null) {
								break;
							}
						}
					}
					
					if (d == null) {
						reporter.reportError("*** line " + ref.posn + ": error, reference not found in class");
						return null;
					}
					if (d instanceof MemberDecl) {
						MemberDecl md1 = (MemberDecl) d;
						
						if (d instanceof MethodDecl) {
							methodcall++;
							methodLine = ref.id.posn.start;
						}
						
						if (md1.isPrivate && !cd.name.equals(currClass.name)) {
							reporter.reportError("*** line " + ref.posn + ": error, cannot reference private member of another class");
							return null;
						}
					}
					
					ref.id.decl = d;
					ref.decl = d;
					//System.out.println(d);
				}
			} else if (_md.type.typeKind == TypeKind.ARRAY) {
				if (ref.id.spelling.equals("length")) {
					ref.id.decl = new FieldDecl(false, false, new BaseType(TypeKind.INT, null), "length", null);
					ref.decl = ref.id.decl;
					
				}
			
			} else {
				reporter.reportError("*** line " + ref.posn + ": error, invalid reference");	
			}
		} else if (decl instanceof LocalDecl) {
			LocalDecl ld = (LocalDecl) decl;
			
			if (ld.type.typeKind == TypeKind.CLASS) {
				ClassType ct = (ClassType) ld.type;
				ClassDecl cd = (ClassDecl) ct.className.decl;
				
				Declaration d = null;
				
				for (FieldDecl f : cd.fieldDeclList) {
					if (f.name.equals(ref.id.spelling)) {
						d = f;
					}
					if (d != null) {
						break;
					}
				}
				
				if (d == null) {
					for (MethodDecl m : cd.methodDeclList) {
						if (m.name.equals(ref.id.spelling)) {
							d = m;
						}
						if (d != null) {
							break;
						}
					}
				}
				
				if (d == null) {
					reporter.reportError("*** line " + ref.posn + ": error, reference not found in class");
					return null;
				}
				if (d instanceof MemberDecl) {
					MemberDecl md1 = (MemberDecl) d;
					
					if (d instanceof MethodDecl) {
						methodcall++;
						methodLine = ref.id.posn.start;
					}
					
					if (md1.isPrivate && !cd.name.equals(currClass.name)) {
						reporter.reportError("*** line " + ref.posn + ": error, cannot reference private member of another class");
						return null;
					}
				}
				
				ref.id.decl = d;
				ref.decl = d;
				//System.out.println(d);
				
			} else if (ld.type.typeKind == TypeKind.ARRAY) {
				if (ref.id.spelling.equals("length")) {
					ref.id.decl = new FieldDecl(false, false, new BaseType(TypeKind.INT, null), "length", null);
					ref.decl = ref.id.decl;
					
				}
			
			} else {
				reporter.reportError("*** line " + ref.posn + ": error, invalid reference");
			}

		}
		
		
		
		
		
		return null;
	}

	@Override
	public Object visitIdentifier(Identifier id, Object arg) {
		// TODO Auto-generated method stub
		MethodDecl md = (MethodDecl) arg;
			
		Declaration decl1 = table.retrieve(id, md);		
		
		id.decl = decl1;
		
	
		
		//System.out.println(id.decl);
		
		return null;
	}

	@Override
	public Object visitOperator(Operator op, Object arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override 
	public Object visitIntLiteral(IntLiteral num, Object arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitBooleanLiteral(BooleanLiteral bool, Object arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitNullLiteral(NullLiteral n, Object arg) {
		// TODO Auto-generated method stub
		return null;
	}

}
