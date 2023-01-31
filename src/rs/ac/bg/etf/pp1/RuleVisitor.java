package rs.ac.bg.etf.pp1;

import org.apache.log4j.Logger;

import rs.ac.bg.etf.pp1.ast.Program;
import rs.ac.bg.etf.pp1.ast.VisitorAdaptor;

public class RuleVisitor extends VisitorAdaptor {
	Logger log = Logger.getLogger(getClass());

	public void visit(Program program) {
		System.out.println("IME PROGRAMA: " + program.getName());
	}

}
