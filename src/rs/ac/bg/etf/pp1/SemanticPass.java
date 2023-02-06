package rs.ac.bg.etf.pp1;

import org.apache.log4j.Logger;

import rs.ac.bg.etf.pp1.ast.*;
import rs.etf.pp1.symboltable.Tab;
import rs.etf.pp1.symboltable.concepts.Obj;
import rs.etf.pp1.symboltable.concepts.Struct;

public class SemanticPass extends VisitorAdaptor {

	private static final Logger LOG = Logger.getLogger(SemanticPass.class);

	private int numberOfErrors = 0;
	private Obj currentMethod = null;

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
		int nVars = Tab.currentScope.getnVars();
		Tab.chainLocalSymbols(program.getProgramName().obj);
		Tab.closeScope();
	}

	private static void processSingleSymbol(Type type, VarSymbol symbol) {

		String symbolName;
		Struct typeStruct;
		switch (type.getTypeName()) {
		case "int":
			typeStruct = new Struct(Struct.Int);
			break;
		case "char":
			typeStruct = new Struct(Struct.Char);
			break;
		case "bool":
			typeStruct = new Struct(Struct.Bool);
			break;
		default:
			typeStruct = null; // Nek pukne Tab.dump() i dusmani
		}

		Obj varNode;

		switch (symbol.getClass().getSimpleName()) {
		case "VarArray":
			symbolName = ((VarArray) symbol).getSymbolName();
			typeStruct = new Struct(Struct.Array, typeStruct); // Override simple with array
			varNode = Tab.insert(Obj.Var, symbolName, typeStruct);

			LOG.info("Detektovan simbol " + symbolName + " koji predstavlja niz tipa " + type.getTypeName());
			break;

		case "VarSimple":
			symbolName = ((VarSimple) symbol).getSymbolName();
			varNode = Tab.insert(Obj.Var, symbolName, typeStruct);

			LOG.info("Detektovan simbol " + symbolName + " tipa " + type.getTypeName());
			break;
		}

		symbol.struct = typeStruct;
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

	public void visit(ConstDeclaration constant) {
		Struct typeStruct = Tab.intType; // jedini tip podrzane konstante
		if (!"int".equals(constant.getType().getTypeName())) {
			reportError("Konstanta " + constant.getSymbolName() + " mora biti tipa int", constant);
		}
		constant.struct = typeStruct; // redundantno, ali for good measure
		Obj varNode = Tab.insert(Obj.Con, constant.getSymbolName(), typeStruct);
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

	private void checkSymbol(String symbolName, SyntaxNode node) {
		Obj fromTable = Tab.find(symbolName);
		if (fromTable != Tab.noObj) {
			LOG.debug("Simbol " + symbolName + " se nalazi u tabeli simbola");
		} else {
			reportError("Simbol " + symbolName + " je koriscen, ali nije prethodno deklarisan", node);
		}
	}

	public void visit(SimpleDesignator designator) {
		checkSymbol(designator.getDesignatorName(), designator);
	}

	public void visit(ArrayElementDesignator designator) {
		checkSymbol(designator.getArrayName(), designator);
	}

	public void visit(FactorDesignator factorDesignator) {
		boolean inPrintStatement = "StatementPrintExpression"
				.equals(factorDesignator.getParent().getParent().getParent().getClass().getSimpleName());
		boolean charExpression;

		String designatorName;
		switch (factorDesignator.getDesignator().getClass().getSimpleName()) {
		case "SimpleDesignator":
			designatorName = ((SimpleDesignator) factorDesignator.getDesignator()).getDesignatorName();
			if (Tab.find(designatorName).getType().getKind() != Tab.intType.getKind()) {
				charExpression = Tab.find(designatorName).getType().getKind() == Tab.charType.getKind();
				if (!(charExpression && inPrintStatement)) {
					reportError(designatorName + " mora biti tipa int da bi figurisao u izrazu", factorDesignator);
				}
			}
			break;
		case "ArrayElementDesignator":
			designatorName = ((ArrayElementDesignator) factorDesignator.getDesignator()).getArrayName();
			if (Tab.find(designatorName).getType().getElemType().getKind() != Tab.intType.getKind()) {
				charExpression = Tab.find(designatorName).getType().getElemType().getKind() == Tab.charType.getKind();

				if (!(charExpression && inPrintStatement)) {
					reportError(
							"Element niza " + designatorName
									+ " mora biti tipa int da bi figurisao u izrazu osim za char u print naredbi",
							factorDesignator);
				}
			}
			break;
		default:
			designatorName = null; // Nek puknu dusmani
		}
	}
}