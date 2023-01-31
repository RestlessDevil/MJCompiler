package rs.ac.bg.etf.pp1;

import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

import rs.ac.bg.etf.pp1.ast.Assignment;
import rs.ac.bg.etf.pp1.ast.CascadingSymbols;
import rs.ac.bg.etf.pp1.ast.MultipleSymbolAssignment;
import rs.ac.bg.etf.pp1.ast.Program;
import rs.ac.bg.etf.pp1.ast.SingleSymbolAssignment;
import rs.ac.bg.etf.pp1.ast.VisitorAdaptor;

public class RuleVisitor extends VisitorAdaptor {
	Logger log = Logger.getLogger(getClass());

	public void visit(Program program) {
		System.out.println("IME PROGRAMA: " + program.getName());
	}

	private static List<String> getAssignedSymbolList(CascadingSymbols cs) {
		List<String> assignedSymbols;
		if (cs instanceof MultipleSymbolAssignment) {
			assignedSymbols = getAssignedSymbolList(((MultipleSymbolAssignment) cs).getCascadingSymbols());
			assignedSymbols.add(((MultipleSymbolAssignment) cs).getSymbolName());
		} else {
			assignedSymbols = new LinkedList<String>();
			assignedSymbols.add(((SingleSymbolAssignment) cs).getSymbolName());
		}
		return assignedSymbols;
	}

	public void visit(Assignment assignment) {
		CascadingSymbols cs = assignment.getCascadingSymbols();
		List<String> assignedSymbols = getAssignedSymbolList(cs);
		System.out.println("Cascade assignment detected");
		for (String as : assignedSymbols) {
			System.out.println(as + " = " + assignment.getExpression());
		}
	}
}
