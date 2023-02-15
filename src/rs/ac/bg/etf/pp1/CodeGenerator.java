package rs.ac.bg.etf.pp1;

import rs.ac.bg.etf.pp1.CounterVisitor.VarCounter;
import rs.ac.bg.etf.pp1.ast.*;
import rs.etf.pp1.mj.runtime.Code;
import rs.etf.pp1.symboltable.*;
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

		VarCounter varCnt = new VarCounter();
		methodNode.traverseTopDown(varCnt);

		// Generate the entry
		Code.put(Code.enter);
		Code.put(0); // void main() only
		Code.put(0 + varCnt.getCount());

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
		Code.load(factor.getDesignator().obj);
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
		Code.store(statement.getDesignator().obj);
	}

	public void visit(StatementIncrement statement) {
		Code.load(statement.getDesignator().obj);
		Code.put(Code.inc);
		Code.store(statement.getDesignator().obj);
	}

	public void visit(StatementDecrement statement) {
		Code.load(statement.getDesignator().obj);
		Code.loadConst(1);
		Code.put(Code.sub);
		Code.store(statement.getDesignator().obj);
	}

	// TODO: rest of the statements

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
		
		if (designatorType == SemanticAnalyzer.STRUCT_INT) {
			Code.put(Code.read);
		} else {
			Code.put(Code.bread);
		}
		Code.store(designatorObj);
	}

	public void visit(StatementAllocateArray statement) {
		Code.put(Code.newarray);
		if (statement.getType().struct == SemanticAnalyzer.STRUCT_CHAR) {
			Code.put(0);
		} else {
			Code.put(1);
		}
	}
}