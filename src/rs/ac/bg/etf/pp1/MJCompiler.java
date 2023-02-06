package rs.ac.bg.etf.pp1;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

import java_cup.runtime.Symbol;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

import rs.ac.bg.etf.pp1.ast.Program;
import rs.ac.bg.etf.pp1.util.Log4JUtils;
import rs.etf.pp1.symboltable.Tab;

public class MJCompiler {

	static {
		DOMConfigurator.configure(Log4JUtils.instance().findLoggerConfigFile());
		Log4JUtils.instance().prepareLogFile(Logger.getRootLogger());
	}

	private static final Logger LOG = Logger.getLogger(MJCompiler.class);

	public static void main(String[] args) throws Exception {

		LOG.info("MJCompiler v0.1 - RAJOVIC");

		File sourcePath = new File("test/test301.mj");

		try (Reader bufferedReader = new BufferedReader(new FileReader(sourcePath))) {
			LOG.info("Prevodim: " + sourcePath.getAbsolutePath());

			Yylex lexer = new Yylex(bufferedReader);

			MJParser p = new MJParser(lexer);
			Symbol s = p.parse(); // Parsiranje
			Program prog = (Program) (s.value);
			LOG.info(prog.toString("")); // Ispis sintaksnog stabla

			LOG.info("===================================");
			Tab.init();
			SemanticAnalyzer semanticPass = new SemanticAnalyzer();
			prog.traverseBottomUp(semanticPass);
			Tab.dump();

			// log.info(" Print count calls = " + v.printCallCount);

			// log.info(" Deklarisanih promenljivih ima = " + v.varDeclCount);

			if (semanticPass.getNumberOfErrors() > 0) {
				LOG.info("Semantički prolaz je detektovao " + semanticPass.getNumberOfErrors() + " gresaka");
			} else {
				LOG.info("Program " + prog.getProgramName().getName() + " je semanticki ispravan");
			}
			LOG.info("===================================");

		} catch (IOException ex) {
			LOG.error(ex.getMessage(), ex);
		}
	}
}