package rs.ac.bg.etf.pp1;

import rs.ac.bg.etf.pp1.ast.*;
import rs.etf.pp1.mj.runtime.Code;
import rs.etf.pp1.symboltable.Tab;
import rs.etf.pp1.symboltable.concepts.Obj;
import rs.etf.pp1.symboltable.concepts.Struct;

public class CodeGenerator extends VisitorAdaptor {
	private int mainPC = 0;

	public int getMainPc() {
		return mainPC;
	}

	public void visit(MethodTypeName methodTypeName) {

		if ("main".equalsIgnoreCase(methodTypeName.getName())) {
			mainPC = Code.pc;
		}
		methodTypeName.obj.setAdr(Code.pc);
		// Collect arguments and local variables
		SyntaxNode methodNode = methodTypeName.getParent();

		// Generate the entry
		Code.put(Code.enter);
		Code.put(0); // void main() only
		Code.put(0 + SemanticAnalyzer.numberOfLocalVars.get(((Method) methodTypeName.getParent()).obj));

	}

	public void visit(Method method) {
		Code.put(Code.exit);
		Code.put(Code.return_);
	}

	// Factors
	public void visit(FactorNumericConstant factor) {
		Code.loadConst(factor.getValue());
	}

	public void visit(FactorDesignator factor) {
		if (factor.getDesignator() instanceof SimpleDesignator) {
			Code.load(factor.getDesignator().obj);
		} else { // ArrayElementDesignator
			Code.load(factor.getDesignator().obj);
			Code.put(Code.dup_x1);
			Code.put(Code.pop);
			Code.put(Code.aload);
		}
	}

	public void visit(FactorCharConstant factor) {
		Code.loadConst(factor.getValue().charAt(1));
	}

	// Operations
	public void visit(OperationsMultiplication operations) {
		if (operations.getMulop() instanceof OperatorAsterisk) {
			Code.put(Code.mul);
		} else if (operations.getMulop() instanceof OperatorSlash) {
			Code.put(Code.div);
		} else { // OperatorPercent
			Code.put(Code.rem);
		}
	}

	public void visit(OperationsAdd operations) {
		if (operations.getAddop() instanceof OperatorPlus) {
			Code.put(Code.add);
		} else { // OperatorMinus
			Code.put(Code.sub);
		}
	}

	public void visit(ExpressionNegated expression) {
		Code.loadConst(-1);
		Code.put(Code.mul);
	}

	// Statements

	public void visit(StatementAssignExpression statement) {
		if (statement.getDesignator() instanceof SimpleDesignator) {
			Code.store(statement.getDesignator().obj);
		} else { // ArrayElementDesignator
			// Code generator is not compatible with the Runtime, hence this
			Code.load(statement.getDesignator().obj);
			Code.put(Code.dup_x2);
			Code.put(Code.pop);
			Code.put(Code.astore);
		}
	}

	public void visit(StatementMultiAssign statement) {

	}

	public void visit(StatementIncrement statement) {
		Code.put(Code.dup); // Need that index on the stack to remain for astore
		Code.load(statement.getDesignator().obj);
		if (statement.getDesignator() instanceof ArrayElementDesignator) {
			Code.put(Code.dup_x1);
			Code.put(Code.pop);
			Code.put(Code.aload);
		}

		Code.loadConst(1);
		Code.put(Code.add);
		if (statement.getDesignator() instanceof SimpleDesignator) {
			Code.store(statement.getDesignator().obj);
		} else {
			Code.load(statement.getDesignator().obj);
			Code.put(Code.dup_x2);
			Code.put(Code.pop);
			Code.put(Code.astore);
		}
	}

	public void visit(StatementDecrement statement) {
		Code.put(Code.dup); // Need that index on the stack to remain for astore
		Code.load(statement.getDesignator().obj);
		if (statement.getDesignator() instanceof ArrayElementDesignator) {
			Code.put(Code.dup_x1);
			Code.put(Code.pop);
			Code.put(Code.aload);
		}

		Code.loadConst(1);
		Code.put(Code.sub);

		if (statement.getDesignator() instanceof SimpleDesignator) {
			Code.store(statement.getDesignator().obj);
		} else {
			Code.load(statement.getDesignator().obj);
			Code.put(Code.dup_x2);
			Code.put(Code.pop);
			Code.put(Code.astore);
		}
	}

	public void visit(StatementPrint statement) {
		Code.loadConst(1);
		if (statement.getExpression().struct == SemanticAnalyzer.STRUCT_INT) {
			Code.put(Code.print);
		} else { // STRUCT_CHAR
			Code.put(Code.bprint);
		}
	}

	public void visit(StatementPrintWidth statement) {
		Code.loadConst(statement.getWidth());
		if (statement.getExpression().struct == SemanticAnalyzer.STRUCT_INT) {
			Code.put(Code.print);
		} else { // STRUCT_CHAR
			Code.put(Code.bprint);
		}
	}

	public void visit(StatementRead statement) {
		Obj designatorObj;
		Struct designatorType;

		if (statement.getDesignator() instanceof SimpleDesignator) {
			designatorObj = ((SimpleDesignator) statement.getDesignator()).obj;
			designatorType = designatorObj.getType();
		} else { // ArrayElementDesignator
			designatorObj = ((ArrayElementDesignator) statement.getDesignator()).obj;
			designatorType = designatorObj.getType().getElemType();
		}

		if (designatorType == SemanticAnalyzer.STRUCT_INT) { // STRUCT_INT
			Code.put(Code.read);
		} else { // STRUCT_CHAR
			Code.put(Code.bread);
		}

		if (statement.getDesignator() instanceof SimpleDesignator) {
			Code.store(designatorObj);
		} else {
			Code.load(statement.getDesignator().obj);
			Code.put(Code.dup_x2);
			Code.put(Code.pop);
			Code.put(Code.astore);
		}
	}

	public void visit(StatementAllocateArray statement) {
		Code.put(Code.newarray);
		if (statement.getType().struct == SemanticAnalyzer.STRUCT_CHAR) {
			Code.put(0);
		} else {
			Code.put(1);
		}
		Code.store(statement.getDesignator().obj);
	}
}