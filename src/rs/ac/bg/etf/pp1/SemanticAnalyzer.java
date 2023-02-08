package rs.ac.bg.etf.pp1;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import rs.ac.bg.etf.pp1.ast.*;
import rs.etf.pp1.symboltable.Tab;
import rs.etf.pp1.symboltable.concepts.*;

public class SemanticAnalyzer extends VisitorAdaptor {

	private static final Logger LOG = Logger.getLogger(SemanticAnalyzer.class);

	private final Map<Struct, String> typeNameMap = new HashMap<>();

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

	// Insert a type to symbol table when needed
	private Obj insertType(String name, Struct typeStruct) {
		LOG.info("Inserting a new type " + name + " into the symbol table");

		Obj typeObj = Tab.insert(Obj.Type, name, typeStruct);
		typeNameMap.put(typeObj.getType(), name);

		return typeObj;
	}

	private Struct typeNameToStruct(String name) throws Exception {
		Obj typeObj = Tab.find(name);
		if (typeObj == Tab.noObj) { // Check if the type exists
			switch (name) {
			case "void":
				typeObj = insertType(name, new Struct(Struct.None));
				return typeObj.getType();
			case "bool":
				typeObj = insertType(name, new Struct(Struct.Bool));
				return typeObj.getType();

			default:// Type not found
				throw new Exception(name + " symbol not found");
			}

		} else if (typeObj.getKind() != 2) { // Not a type
			throw new Exception(name + " not a type");
		} else {
			typeNameMap.put(typeObj.getType(), name);

			return typeObj.getType();
		}
	}

	private String structToTypeName(Struct type) {
		return typeNameMap.get(type);
	}

	// Visitor methods

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
		Type type = methodTypeName.getType();
		String typeName = type.getTypeName();
		String methodName = methodTypeName.getName();

		try {
			type.struct = typeNameToStruct(typeName);
		} catch (Exception ex) {
			type.struct = new Struct(Struct.None); // Just to avoid an unwanted exception
			reportError("Tip " + typeName + " nije podrzan u realizaciji za nivo A.", methodTypeName);
		}

		currentMethod = Tab.insert(Obj.Meth, methodName, type.struct);
		methodTypeName.obj = currentMethod;
		Tab.openScope();
		reportInfo("Obradjuje se funkcija " + methodName + " koja vraca " + typeName, methodTypeName);
	}

	public void visit(Method method) {
		LOG.debug("Obradjena je funkcija \"" + method.getMethodTypeName().getName() + "\" koja vraca \""
				+ method.getMethodTypeName().getType().getTypeName() + "\" tip sa ukupno "
				+ Tab.currentScope().getnVars() + " lokalnih promenljivih");

		Tab.chainLocalSymbols(currentMethod);
		Tab.closeScope();

		currentMethod = null;
	}

	public void visit(DeclarationConst declaration) {
		Type type = declaration.getType();
		String typeName = type.getTypeName();
		String constName = declaration.getSymbolName();

		try {
			type.struct = typeNameToStruct(typeName);
		} catch (Exception ex) {
			reportError("Tip " + typeName + " nije podrzan u realizaciji za nivo A.", declaration);
			type.struct = new Struct(Struct.None); // Just to avoid an unwanted exception
		}
		declaration.obj = Tab.insert(Obj.Con, constName, type.struct);

		LOG.debug("Deklarisana je konstanta \"" + constName + "\" tipa " + typeName);
	}

	private void processSingleVarSymbol(Struct typeStruct, VarSymbol symbol) {
		if (symbol instanceof VarSimple) { // VarSimple
			VarSimple vs = (VarSimple) symbol;
			vs.obj = Tab.insert(Obj.Var, vs.getSymbolName(), typeStruct);

			reportInfo("Deklarisana je " + (vs.obj.getLevel() > 0 ? "lokalna" : "globalna") + " promenljiva \""
					+ vs.getSymbolName() + "\" tipa \"" + structToTypeName(typeStruct) + "\"", symbol);
		} else { // VarArray
			VarArray va = (VarArray) symbol;
			va.obj = Tab.insert(Obj.Var, va.getSymbolName(), new Struct(Struct.Array, typeStruct)); // TODO: cache

			reportInfo("Deklarisan je " + (va.obj.getLevel() > 0 ? "lokalni" : "globalni") + " niz \""
					+ va.getSymbolName() + "\" promenljivih tipa \"" + structToTypeName(typeStruct) + "\"", symbol);
		}
	}

	private void processVarSymbols(Struct typeStruct, VarSymbolList symbols) {
		if (symbols instanceof VarSymbols) { // VarSymbols
			processSingleVarSymbol(typeStruct, ((VarSymbols) symbols).getVarSymbol());
			processVarSymbols(typeStruct, ((VarSymbols) symbols).getVarSymbolList());
		} else { // VarSymbolSingle
			processSingleVarSymbol(typeStruct, ((VarSymbolSingle) symbols).getVarSymbol());
		}
	}

	public void visit(DeclarationVar declaration) {
		Type type = declaration.getType();
		String typeName = type.getTypeName();

		try {
			type.struct = typeNameToStruct(typeName);
		} catch (Exception ex) {
			reportError("Tip " + typeName + " nije podrzan u realizaciji za nivo A.", declaration);
			type.struct = new Struct(Struct.None); // Just to avoid an unwanted exception
		}

		processVarSymbols(type.struct, declaration.getVarSymbolList());
	}

	public void visit(SimpleDesignator designator) {
		String name = designator.getDesignatorName();
		designator.obj = Tab.find(name);
		LOG.debug("Detektovano je koriscenje "
				+ (designator.obj.getKind() == 1
						? ((designator.obj.getLevel() > 0 ? "lokalne" : "globalne") + " promenljive")
						: "konstante")
				+ " \"" + name + "\" tipa \"" + structToTypeName(designator.obj.getType()) + "\"");
	}

	public void visit(ArrayElementDesignator designator) {
		String name = designator.getArrayName();
		designator.obj = Tab.find(name);
		LOG.debug("Detektovan je pristup elementu " + (designator.obj.getLevel() > 0 ? "lokalnog" : "globalnog")
				+ " niza \"" + name + "\"");
	}

	// Visit Statements
	
	public void visit(StatementIncrement statement) {
		if (statement.getDesignator().obj.getKind() == Obj.Var) {
			System.out.println("INKREMENTIRAM VARIJABLU");
		}
	}
	
	public void visit(StatementAssignExpression statement) { // TODO: IMPLEMENTIRATI
		Designator designator = statement.getDesignator();
		System.out.println("POREDIM " + designator.obj.getType().getKind() + " SA ?");
	}
}