package rs.ac.bg.etf.pp1;

import org.apache.log4j.Logger;

import rs.ac.bg.etf.pp1.ast.*;
import rs.etf.pp1.symboltable.Tab;
import rs.etf.pp1.symboltable.concepts.*;

public class SemanticAnalyzer extends VisitorAdaptor {

	private static final Logger LOG = Logger.getLogger(SemanticAnalyzer.class);

	public int numberOfVars = 0;
	private int numberOfErrors = 0;
	private Obj currentMethod = null;

	public int getNumberOfVars() {
		return numberOfVars;
	}

	public int getNumberOfErrors() {
		return numberOfErrors;
	}

	private void reportError(String message, SyntaxNode info) {
		numberOfErrors++;
		StringBuilder msg = new StringBuilder(message);
		int line = (info == null) ? 0 : info.getLine();
		if (line != 0)
			msg.append(" na liniji ").append(line);
		LOG.error(msg.toString());
	}

	public void reportInfo(String message, SyntaxNode info) {
		StringBuilder msg = new StringBuilder(message);
		int line = (info == null) ? 0 : info.getLine();
		if (line != 0)
			msg.append(" na liniji ").append(line);
		LOG.info(msg.toString());
	}

	// Visitor metode

	public void visit(ProgramName programName) {
		programName.obj = Tab.insert(Obj.Prog, programName.getName(), Tab.noType);
		Tab.openScope();
	}

	public void visit(Program program) {
		numberOfVars = Tab.currentScope.getnVars();
		Tab.chainLocalSymbols(program.getProgramName().obj);
		Tab.closeScope();
	}

	public void visit(MethodTypeName methodTypeName) {
		// TODO: samo je void podrzan, zato je Struct.NONE
		currentMethod = Tab.insert(Obj.Meth, methodTypeName.getName(), new Struct(Struct.None));
		methodTypeName.obj = currentMethod;
		Tab.openScope();
		reportInfo("Obradjuje se funkcija " + methodTypeName.getName() + " koja vraca "
				+ methodTypeName.getType().getTypeName(), methodTypeName);
	}

	public void visit(Method method) {
		// Podrzan je samo void, tako da nema return-a
		Tab.chainLocalSymbols(currentMethod);
		Tab.closeScope();

		currentMethod = null;
	}

}