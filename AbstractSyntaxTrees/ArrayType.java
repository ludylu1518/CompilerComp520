/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */

package miniJava.AbstractSyntaxTrees;

import miniJava.SyntaticAnalyzer.SourcePosition;

public class ArrayType extends TypeDenoter {

	    public ArrayType(TypeDenoter eltType, SourcePosition posn){
	        super(TypeKind.ARRAY, posn);
	        this.eltType = eltType;
	    }
	        
	    public <A,R> R visit(Visitor<A,R> v, A o) {
	        return v.visitArrayType(this, o);
	    }

	    public TypeDenoter eltType;

		@Override
		public boolean equals(Object obj) {
			// TODO Auto-generated method stub			
			
			if (obj == null) {
				return false;
				
			} else if (((TypeDenoter)obj).typeKind == TypeKind.NULL) {
				return true;
				
			} else if (obj instanceof ArrayType) {
				TypeDenoter objType = ((ArrayType) obj).eltType;
				
				if (objType instanceof BaseType) {
					return objType.equals(this.eltType);
					
				} else if (objType instanceof ClassType) {
					return objType.equals(this.eltType);
					
				} else {
					return false;
				}
			}
			
			
			return false;
		}
	}

