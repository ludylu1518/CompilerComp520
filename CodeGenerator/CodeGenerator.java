package miniJava.CodeGenerator;

import miniJava.SyntaticAnalyzer.*;

import java.util.ArrayList;

import miniJava.ErrorReporter;
import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.mJAM.Machine;
import miniJava.mJAM.Machine.*;


public class CodeGenerator implements Visitor<Object, Object> {

	private ErrorReporter reporter;
	private AST ast;
	private int staticSize;
	private int fieldIndex;
	private int staticFields;
	private int fieldoffset;
	private int mainOffset;
	private ArrayList<methodPatch> patchList;
	
	//methods
	private int paramOffset;
	private int frameOffset;
	
	public CodeGenerator(AST ast, ErrorReporter reporter) {
		this.ast = ast;
		this.reporter = reporter;
		
		patchList = new ArrayList<methodPatch>();
	}
	
	public void Start() {  //runner
		
		
		Machine.initCodeGen();
		//System.out.println("Generating code ...");
		ast.visit(this, null);
		
	}
	
	
	@Override
	public Object visitPackage(Package prog, Object arg) {
		// TODO Auto-generated method stub
		staticSize = 0;             // count how many static fields are in total
		
		// go through fields first
		for (ClassDecl cd : prog.classDeclList) {
			
			fieldIndex = 0;             // count how many field in class
			staticFields = 0;          // count how many static field in class
			fieldoffset = 0;            // offset for nonstatic field
						 
			for (FieldDecl fd : cd.fieldDeclList) {
				fd.visit(this, null);
			}
			
			cd.entity = new Address(fieldIndex - staticFields, fieldIndex - staticFields);
			
			
			fieldIndex = 0;
			staticFields = 0;
			fieldoffset = 0;
		}
		
		
		/*
		 * Preamble:
		 *   generate call to main
		 *   
		 */
		Machine.emit(Op.LOADL,0);            // array length 0
		Machine.emit(Prim.newarr);           // empty String array argument
		int patchAddr_Call_main = Machine.nextInstrAddr();  // record instr addr where main is called                                                
		Machine.emit(Op.CALL,Reg.CB,-1);     // static call main (address to be patched)
		Machine.emit(Op.HALT,0,0,0);         // end execution
		
		
		mainOffset = patchAddr_Call_main;
		
		for (ClassDecl cd : prog.classDeclList) {
			cd.visit(this, null);
		}
		
		for (methodPatch mp : patchList) {          // some md entity might not have been set, restoring them now
			
			Machine.patch(mp.line, ((Address) mp.md.entity).offset);			
		}
		
		return null;
	}
	@Override
	public Object visitClassDecl(ClassDecl cd, Object arg) {
		// TODO Auto-generated method stub
		
		for (MethodDecl md : cd.methodDeclList) {
			md.visit(this, null);
		}
		
		
		return null;
	}
	
	
	
	@Override
	public Object visitFieldDecl(FieldDecl fd, Object arg) {
		// TODO Auto-generated method stub
		if (fd.isStatic) {
			//Machine.emit(Op.PUSH, 1);
			fd.entity = new Address(1, staticSize);
			staticSize++;
			staticFields++;
						
		} else {
		
			fd.entity = new Address(1, fieldoffset);
			fieldoffset++;
			//System.out.println(fieldIndex);
		}
		
		fieldIndex++;
		return null;
	}
	
	
	@Override
	public Object visitMethodDecl(MethodDecl md, Object arg) {
		// TODO Auto-generated method stub
		
		md.entity = new Address(1, Machine.nextInstrAddr());
		
		if (md.name.equals("main")) {
			Machine.patch(mainOffset, ((Address) md.entity).offset);
		}
		
		paramOffset = -1 * md.parameterDeclList.size();
		
		for (ParameterDecl pd : md.parameterDeclList) {
			pd.visit(this, null);
		}
		
		
		
		frameOffset = 3;
		
		for (Statement stmt : md.statementList) {
			
			stmt.visit(this, null);
		}
		
		if (md.returnType == null) {                          // void might not have a return stmt
			Machine.emit(Op.RETURN, 0, 0, md.parameterDeclList.size());
		}
		
		return null;
	}
	
	
	
	@Override
	public Object visitParameterDecl(ParameterDecl pd, Object arg) {
		// TODO Auto-generated method stub
		
		pd.entity = new Address(1, paramOffset);
		paramOffset++;
		
		return null;
	}
	@Override
	public Object visitVarDecl(VarDecl decl, Object arg) {
		// TODO Auto-generated method stub
		
		decl.entity = new Address(1, frameOffset);
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
		return null;
	}
	@Override
	public Object visitArrayType(ArrayType type, Object arg) {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public Object visitBlockStmt(BlockStmt stmt, Object arg) {
		// TODO Auto-generated method stub
		
		int localSize = 0;
		
		for (Statement s : stmt.sl) {
			
			if (s instanceof VarDeclStmt) {
				localSize++;
			}
			
			s.visit(this, null);
		}
		
		frameOffset = frameOffset - localSize;           // delete local var and reset frameOffset
		
		Machine.emit(Op.POP, localSize);
		
		return null;
	}
	@Override
	public Object visitVardeclStmt(VarDeclStmt stmt, Object arg) {
		// TODO Auto-generated method stub
		
		stmt.varDecl.visit(this, null);
		
		stmt.initExp.visit(this, null);
		
		frameOffset++;
			
		return null;
	}
	
	
	@Override
	public Object visitAssignStmt(AssignStmt stmt, Object arg) {
		// TODO Auto-generated method stub
		
		if (stmt.ref instanceof QualRef) {
			
			// can't be method or class declaration , in call stmt
			FieldDecl fd = (FieldDecl) stmt.ref.decl;
			
			if (fd.isStatic) {
				
				stmt.val.visit(this, null);
				Machine.emit(Op.STORE, Reg.SB, ((Address) fd.entity).offset);
				
			} else {
						
				//System.out.println(stmt.ref.decl.entity.size);
			
				stmt.ref.visit(this, "address");           // get address from reference
					
				stmt.val.visit(this, null);
				Machine.emit(Prim.fieldupd);
			}
			
			
		} else if (stmt.ref instanceof IdRef) {			
			
			// idRef, field or local			
			
			if (stmt.ref.decl instanceof FieldDecl) {
				
				FieldDecl fd = (FieldDecl) stmt.ref.decl;
				
				stmt.val.visit(this, null);      // load value and store
				
				if (fd.isStatic) {
		
					Machine.emit(Op.STORE, Reg.SB, ((Address) fd.entity).offset);
					
				} else {
					
					Machine.emit(Op.STORE, Reg.OB, ((Address) fd.entity).offset);
				}
				
			} else {
				
				stmt.val.visit(this, null);
				Machine.emit(Op.STORE, Reg.LB, ((Address) stmt.ref.decl.entity).offset);
				
			}				
			
		} else {
			
			reporter.reportError("*** code generation error invalid reference, should be caught in Contextual Analysis");
		}
		
		
		
		return null;
	}
	
	
	
	@Override
	public Object visitIxAssignStmt(IxAssignStmt stmt, Object arg) {
		// TODO Auto-generated method stub
		stmt.ref.visit(this, null);
		stmt.ix.visit(this, null);
		stmt.exp.visit(this, null);
		
		Machine.emit(Prim.arrayupd);
		
		return null;
	}
	
	
	@Override
	public Object visitCallStmt(CallStmt stmt, Object arg) {        // need to patch method calls
		// TODO Auto-generated method stub
		
		for (Expression e : stmt.argList) {
			e.visit(this, null);
		}
		
		MethodDecl md = (MethodDecl) stmt.methodRef.decl;
	
		if (stmt.methodRef instanceof IdRef) {
			
			if (md.isStatic) {
				
				int i = Machine.nextInstrAddr();
				
				Machine.emit(Op.CALL, Reg.CB, -1);
				patchList.add(new methodPatch(i, md));      
								
			} else {
				
				Machine.emit(Op.LOADA, Reg.OB, 0);
				
				int i = Machine.nextInstrAddr();
				
				Machine.emit(Op.CALLI, Reg.CB, -1);
				patchList.add(new methodPatch(i, md));      
				
			}
			
		} else if (stmt.methodRef instanceof QualRef) {
			
			QualRef qref = (QualRef) stmt.methodRef;
			
			if (qref.ref instanceof ThisRef) {                // same as id ref
				
				 if (md.isStatic) {
					 
					 int i = Machine.nextInstrAddr();
					 
					 Machine.emit(Op.CALL, Reg.CB, -1);
					 patchList.add(new methodPatch(i, md));      
					 
				 } else {
					 
					 Machine.emit(Op.LOADA, Reg.OB, 0);
					 
					 int i = Machine.nextInstrAddr();
					 
					 Machine.emit(Op.CALLI, Reg.CB, -1);
					 patchList.add(new methodPatch(i, md));      
					 
				 }
				
				
				
			} else {
				
				if (md.builtIn) {                                 // check if builtIn println
					
					if (md.name.equals("println")) {
						
						Machine.emit(Prim.putintnl);
					}
					
					
				} else {					
										
					
					 if (md.isStatic) {
						 
						 int i = Machine.nextInstrAddr();
						 
						 Machine.emit(Op.CALL, Reg.CB, -1);
						 patchList.add(new methodPatch(i, md));      
						 
					 } else {
						 
						 stmt.methodRef.visit(this, null);
						 
						 int i = Machine.nextInstrAddr();
						 
						 Machine.emit(Op.CALLI, Reg.CB, -1);
						 patchList.add(new methodPatch(i, md));      
						 
					 }
				}
			}
			
		}
		
		
		
		if (md.type.typeKind != TypeKind.VOID) {
			Machine.emit(Op.POP, 1);
		}
		
		return null;
	}
	
	
	@Override
	public Object visitReturnStmt(ReturnStmt stmt, Object arg) {
		// TODO Auto-generated method stub
		MethodDecl md = stmt.correspondingMethod;
		
			
		if (stmt.returnExpr != null) {
			stmt.returnExpr.visit(this, null);
			Machine.emit(Op.RETURN, 1, 0, md.parameterDeclList.size());
		} else {
			
			Machine.emit(Op.RETURN, 0, 0, md.parameterDeclList.size());
		}
		
		return null;
	}
	
	
	@Override
	public Object visitIfStmt(IfStmt stmt, Object arg) {
		// TODO Auto-generated method stub
		
		stmt.cond.visit(this, null);
		
		int i = Machine.nextInstrAddr();
		
		Machine.emit(Op.JUMPIF, 0, Reg.CB, 0);
		
		stmt.thenStmt.visit(this, null);
		
		int j = Machine.nextInstrAddr();
		
		Machine.emit(Op.JUMP, Reg.CB, 0);
		
		int g = Machine.nextInstrAddr();
		
		if (stmt.elseStmt != null) {
			
			Machine.patch(i, g);
			stmt.elseStmt.visit(this, null);				
			
		} else {
			
			Machine.patch(i,  g);
		}
		
		Machine.patch(j, Machine.nextInstrAddr());
		
		return null;
	}
	
	
	@Override
	public Object visitWhileStmt(WhileStmt stmt, Object arg) {
		// TODO Auto-generated method stub
		
		int j = Machine.nextInstrAddr();
		
		Machine.emit(Op.JUMP, Reg.CB, 0);
		
		int g = Machine.nextInstrAddr();
		
		stmt.body.visit(this, null);
		
		int h = Machine.nextInstrAddr();
		
		Machine.patch(j, h);
		
		stmt.cond.visit(this, null);
		
		Machine.emit(Op.JUMPIF, 1, Reg.CB, g);
		
		return null;
	}
	
	
	
	
	@Override
	public Object visitUnaryExpr(UnaryExpr expr, Object arg) {
		// TODO Auto-generated method stub
		expr.expr.visit(this, null);
		
		if (expr.operator.spelling.equals("-")) {
			Machine.emit(Prim.neg);
		} else {
			
			Machine.emit(Prim.not);
		}
		
		return null;
	}
	
	@Override
	public Object visitBinaryExpr(BinaryExpr expr, Object arg) {
		// TODO Auto-generated method stub
		
		expr.left.visit(this, null);
		
		// short circuit conditional 
		
		if (expr.operator.spelling.equals("&&")) {        // using semantics of if then else
		
			int i = Machine.nextInstrAddr();
			
			Machine.emit(Op.JUMPIF, 0, Reg.CB, 0);          // if false skip right expr 
			
			expr.right.visit(this, null);                 // otherwise depends on right expr
			 
			int j = Machine.nextInstrAddr();
			
			Machine.emit(Op.JUMP, Reg.CB, 0);
			
			int g = Machine.nextInstrAddr();
			
			Machine.emit(Op.LOADL, 0);                 // else whole thing is false
			
			Machine.patch(i, g);
			Machine.patch(j, Machine.nextInstrAddr());
			
			
			return null;
			
		} else if (expr.operator.spelling.equals("||")) {
			
			int i = Machine.nextInstrAddr();
			
			Machine.emit(Op.JUMPIF, 1, Reg.CB, 0);          // if true skip right expr

			expr.right.visit(this, null);              
			
			int j = Machine.nextInstrAddr();
			
			Machine.emit(Op.JUMP, Reg.CB, 0);
			
			int g = Machine.nextInstrAddr();
			
			Machine.emit(Op.LOADL, 1);
			
			Machine.patch(i, g);
			Machine.patch(j, Machine.nextInstrAddr());
			
			return null;
		}
		
		
		expr.right.visit(this, null);
		
		switch (expr.operator.spelling) {
		
			case "+":
				Machine.emit(Prim.add);
				break;
				
			case "-":
				Machine.emit(Prim.sub);
				break;
				
			case "*":
				Machine.emit(Prim.mult);
				break;
				
			case "/":
				Machine.emit(Prim.div);
				break;
				
			case "<":
				Machine.emit(Prim.lt);
				break;
				
			case "<=": 
				Machine.emit(Prim.le);
				break;
				
			case ">":
				Machine.emit(Prim.gt);
				break;
				
			case ">=":
				Machine.emit(Prim.ge);
				break;
				
			case "==":
				Machine.emit(Prim.eq);
				break;
				
			case "!=":
				Machine.emit(Prim.ne);
				break;
				
			case "&&":
				Machine.emit(Prim.and);
				break;
				
			case "||":
				Machine.emit(Prim.or);
				break;
				
			default:
				reporter.reportError("*** code generation error, invalid operator, should be caught in syntatic analysis");		
		
		
		}
		
		
		return null;		
	}
	
	
	@Override
	public Object visitRefExpr(RefExpr expr, Object arg) {
		// TODO Auto-generated method stub
		expr.ref.visit(this, null);
		return null;
	}
	
	
	@Override
	public Object visitIxExpr(IxExpr expr, Object arg) {
		// TODO Auto-generated method stub
		expr.ref.visit(this, null);
		expr.ixExpr.visit(this, null);
		
		Machine.emit(Prim.arrayref);
		
		return null;
	}
	
	
	@Override
	public Object visitCallExpr(CallExpr expr, Object arg) {
		// TODO Auto-generated method stub
		
		// shouldn't be able to set anything to System.out.println();
		
		for (Expression e : expr.argList) {
			e.visit(this, null);
		}
		
		MethodDecl md =  (MethodDecl) expr.functionRef.decl;
		
		if (expr.functionRef instanceof IdRef) {
			
			if (md.isStatic) {
				
				int i = Machine.nextInstrAddr();
				
				Machine.emit(Op.CALL, Reg.CB, -1);
				patchList.add(new methodPatch(i, md));      
				
			} else {
				
				Machine.emit(Op.LOADA, Reg.OB, 0);
				
				int i = Machine.nextInstrAddr();
				
				Machine.emit(Op.CALLI, Reg.CB, -1);
				patchList.add(new methodPatch(i, md));      
			}
			
		} else if (expr.functionRef instanceof QualRef) {
			
			QualRef qref = (QualRef) expr.functionRef;
			
			if (md.isStatic) {
				
				int i = Machine.nextInstrAddr();
				
				Machine.emit(Op.CALL, Reg.CB, -1);
				patchList.add(new methodPatch(i, md));      
				
			} else {
				
				if (qref.ref instanceof ThisRef) {
					
					Machine.emit(Op.LOADA, Reg.OB, 0);
					
					int i = Machine.nextInstrAddr();
					
					Machine.emit(Op.CALLI, Reg.CB, -1);
					patchList.add(new methodPatch(i, md));      
					
				} else {
					
					expr.functionRef.visit(this, null);
					
					int i = Machine.nextInstrAddr();
					
					Machine.emit(Op.CALLI, Reg.CB, -1);
					patchList.add(new methodPatch(i, md));      
				}
				
			}
			
		}
		
		return null;
	}
	
	
	
	
	@Override
	public Object visitLiteralExpr(LiteralExpr expr, Object arg) {
		// TODO Auto-generated method stub
		switch (expr.lit.kind) {
		
			case Token.INTLITERAL:
				Machine.emit(Op.LOADL, Integer.parseInt(expr.lit.spelling));
				break;
				
			case Token.TRUE:				
				Machine.emit(Op.LOADL,  Machine.trueRep);
				break;
				
			case Token.FALSE:
				Machine.emit(Op.LOADL, Machine.falseRep);
				break;
				
			case Token.NULL:
				Machine.emit(Op.LOADL, Machine.nullRep);
				break;
		}
		
		
		return null;
	}
	@Override
	public Object visitNewObjectExpr(NewObjectExpr expr, Object arg) {
		// TODO Auto-generated method stub
		ClassDecl cd =  (ClassDecl) expr.classtype.className.decl;
		
		Machine.emit(Op.LOADL, -1);
		
		Machine.emit(Op.LOADL, cd.entity.size);
		Machine.emit(Prim.newobj);
		
		
		return null;
	}
	@Override
	public Object visitNewArrayExpr(NewArrayExpr expr, Object arg) {
		// TODO Auto-generated method stub
		expr.sizeExpr.visit(this, null);
		Machine.emit(Prim.newarr);
		
		return null;
	}
	@Override
	public Object visitThisRef(ThisRef ref, Object arg) {
		// TODO Auto-generated method stub
		Machine.emit(Op.LOADA, Reg.OB, 0);
		
		return null;
	}
	
	@Override
	public Object visitIdRef(IdRef ref, Object arg) {
		// TODO Auto-generated method stub                     // methodRef handled in call
		
		if (ref.decl instanceof FieldDecl) {
			
			FieldDecl fd = (FieldDecl) ref.decl;
			
			if (fd.isStatic) {
				
				Machine.emit(Op.LOAD, Reg.SB, ((Address) ref.decl.entity).offset);
				
			} else {
				
				Machine.emit(Op.LOAD, Reg.OB, ((Address) ref.decl.entity).offset);
			}			
			
			
		} else if (ref.decl instanceof LocalDecl) {
			
			
			Machine.emit(Op.LOAD, Reg.LB, ((Address) ref.decl.entity).offset);
			
		} else {
			
			reporter.reportError("*** invalid reference at code generation, should be caught in contextual analysis");
		}
		
		return null;
	}
	
	@Override
	public Object visitQRef(QualRef ref, Object arg) {
		// TODO Auto-generated method stub
		
		if (arg != null) {                      // load address index
			
			ref.ref.visit(this, null);
						
			Machine.emit(Op.LOADL, ((Address) ref.id.decl.entity).offset);
			
			return null;
			
		} else {
			
			if (ref.ref.decl instanceof ClassDecl) {         
				
				if (ref.id.decl instanceof FieldDecl) {
					
					FieldDecl fd = (FieldDecl) ref.id.decl;
					
					if (fd.isStatic) {
						Machine.emit(Op.LOAD, Reg.SB, ((Address) fd.entity).offset);
					} else {
						Machine.emit(Op.LOAD, Reg.OB, ((Address) fd.entity).offset);
					}
					
				} else {        //non-static methodDecl
					
					Machine.emit(Op.LOAD, Reg.OB, ((Address) ref.ref.decl.entity).offset);
				}
				
				
			} else if (ref.ref.decl instanceof FieldDecl || ref.ref.decl instanceof LocalDecl) {
				
				ref.ref.visit(this, null);
				
				if (ref.id.decl instanceof FieldDecl) {
					
					FieldDecl fd = (FieldDecl) ref.id.decl;
					
					// check if array follow by length
					if (ref.ref.decl.type.typeKind == TypeKind.ARRAY && ref.id.spelling.equals("length")) {
						Machine.emit(Prim.arraylen);
						
					} else {
						
						Machine.emit(Op.LOADL, ((Address) fd.entity).offset);
						Machine.emit(Prim.fieldref);
					}
					
				}   // for methodDecl, just need to load ref.ref address
				
			}	
			
			
		}
		
		
		return null;
	}
	
	
	@Override
	public Object visitIdentifier(Identifier id, Object arg) {
		// TODO Auto-generated method stub
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