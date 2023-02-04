package rs.ac.bg.etf.pp1;

import org.apache.log4j.Logger;

import rs.ac.bg.etf.pp1.ast.*;
import rs.etf.pp1.symboltable.Tab;
import rs.etf.pp1.symboltable.concepts.Obj;

public class SemanticPass extends VisitorAdaptor {

	private static final Logger LOG = Logger.getLogger(SemanticPass.class);
	
	private Obj currentMethod = null;

	public void visit(ProgramName programName) {
		programName.obj = Tab.insert(Obj.Prog, programName.getName(), Tab.noType);
    	Tab.openScope();
	}

	public void visit(Program program) {
    	//int nVars = Tab.currentScope.getnVars();
    	Tab.chainLocalSymbols(program.getProgramName().obj);
    	Tab.closeScope();
	}

	private static void processSingleSymbol(Type type, VarSymbol symbol) {

		String symbolName;
		Obj varNode;

		switch (symbol.getClass().getSimpleName()) {
		case "VarArray":
			symbolName = ((VarArray) symbol).getSymbolName();
			varNode = Tab.insert(Obj.Var, symbolName, type.struct);

			LOG.info("Detektovan simbol " + symbolName + " koji predstavlja niz tipa " + type.getTypeName());
			break;

		case "VarNoValue":
			symbolName = ((VarNoValue) symbol).getSymbolName();
			varNode = Tab.insert(Obj.Var, symbolName, type.struct);

			LOG.info("Detektovan simbol " + symbolName + " tipa " + type.getTypeName());
			break;

		case "VarWithValue":
			symbolName = ((VarWithValue) symbol).getSymbolName();
			varNode = Tab.insert(Obj.Var, symbolName, type.struct);
			// TODO: implement value Assignment (int || char)

			LOG.info("Detektovan simbol " + symbolName + " tipa " + type.getTypeName() + " sa dodelom vrednosti "
					+ ((VarWithValue) symbol).getConstValue() + " koja jos nije podrzana");
			break;
		}
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
	
	public void visit(MethodTypeName methodTypeName) {
    	//currentMethod = Tab.insert(Obj.Meth, methodTypeName.getName(), methodTypeName.getType().struct);
    	LOG.info("Processing method " + methodTypeName.getName() + " which returns " + methodTypeName.getType().getTypeName());
    	methodTypeName.obj = currentMethod;
    	//Tab.openScope();
		//report_info("Obradjuje se funkcija " + methodTypeName.getName(), methodTypeName);
	}
	
    public void visit(Method method){
    	//if(!returnFound && currentMethod.getType() != Tab.noType){
		//	report_error("Semanticka greska na liniji " + methodDecl.getLine() + ": funkcija " + currentMethod.getName() + " nema return iskaz!", null);
    	//}
    	//Tab.chainLocalSymbols(currentMethod);
    	//Tab.closeScope();
    	
    	currentMethod = null;
    }

}
