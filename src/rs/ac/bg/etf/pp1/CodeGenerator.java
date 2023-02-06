package rs.ac.bg.etf.pp1;

import rs.ac.bg.etf.pp1.CounterVisitor.VarCounter;
import rs.ac.bg.etf.pp1.ast.*;
import rs.etf.pp1.mj.runtime.Code;
import rs.etf.pp1.symboltable.Tab;
import rs.etf.pp1.symboltable.concepts.Obj;

public class CodeGenerator extends VisitorAdaptor {
	private int mainPC = 0;

	public int getMainPc() {
		return mainPC;
	}

	public void visit(StatementPrintExpression printStmt) {
		if (printStmt.getExpression().struct == Tab.intType) {
			Code.loadConst(5);
			Code.put(Code.print);
		} else {
			Code.loadConst(1);
			Code.put(Code.bprint);
		}
	}

	public void visit(Constant constant) {
		Obj con = Tab.insert(Obj.Con, "$", constant.struct);
		con.setLevel(0);
		con.setAdr(constant.getValue());

		Code.load(con);
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

	public void visit(StatementAssignExpression assignment) {
		Code.store(assignment.getDesignator().obj);
	}

	public void visit(Designator designator) {
		SyntaxNode parent = designator.getParent();
		if (FactorDesignator.class != parent.getClass()) {
			Code.load(designator.obj);
		}
	}

	public void visit(AddExpr addExpr) {
		switch (addExpr.getAddop().getClass().getSimpleName()) {
		case "Plus":
			Code.put(Code.add);
			break;
		case "Minus":
			Code.put(Code.sub);
		}
	}

	public void visit(MulExpr mulExpr) {
		switch (mulExpr.getMulop().getClass().getSimpleName()) {
		case "Times":
			Code.put(Code.mul);
			break;
		case "Divide":
			Code.put(Code.div);
			break;
		case "Mod":
			Code.put(Code.rem);
		}
	}
}
