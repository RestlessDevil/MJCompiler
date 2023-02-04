package rs.ac.bg.etf.pp1;

import rs.ac.bg.etf.pp1.ast.*;
import rs.etf.pp1.symboltable.Tab;
import rs.etf.pp1.symboltable.concepts.Obj;

public class SemanticPass extends VisitorAdaptor {

	public void visit(ProgramName programName) {
		programName.obj = Tab.insert(Obj.Prog, programName.getName(), Tab.noType);
		Tab.openScope();
	}

	public void visit(Program program) {
		Tab.chainLocalSymbols(program.getProgramName().obj);
		Tab.closeScope();
	}

	private static void processSingleSymbol(Type type, VarSymbol symbol) {
		System.out.println("OBRADJUJEM SIMBOL " + symbol);
		//report_info("Deklarisana promenljiva " + varDecl.getVarName(), varDecl);
		//Obj varNode = Tab.insert(Obj.Var, varDecl.getVarName(), type.struct);
	}

	private static void processSymbols(Type type, VarSymbolList symbols) {
		if (symbols instanceof VarSymbols) {
			processSymbols(type, ((VarSymbols) symbols).getVarSymbolList());
			processSingleSymbol(type, ((VarSymbols) symbols).getVarSymbol());
		} else {
			processSingleSymbol(type, ((VarSymbolSingle) symbols).getVarSymbol());
		}
	}

	public void visit(VarDeclaration varDeclaration) {
		Type type = varDeclaration.getType();
		processSymbols(type, varDeclaration.getVarSymbolList());
	}

}
