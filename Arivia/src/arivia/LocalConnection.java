package arivia;

import java.io.PrintStream;
import java.util.Locale;
import java.util.Scanner;

public class LocalConnection implements Connection {
	private final Scanner s;
	private final PrintStream ps;
	private final String name;
	private final Locale locale;
	
	public LocalConnection(Scanner s, PrintStream ps, String name, Locale l) {
		super();
		this.s = s;
		this.ps = ps;
		this.name = name;
		this.locale = l;
	}

	@Override
	public boolean hasNextLine() {
		return s.hasNextLine();
	}

	@Override
	public String nextLine() {
		return s.nextLine();
	}

	@Override
	public void out(String s) {
		ps.println(s);
	}

	@Override
	public String getUserName() {
		return name;
	}

	@Override
	public Locale getLocale() {
		return locale;
	}

	@Override
	public void prompt() {
		ps.print("> ");
	}

	@Override
	public boolean allowMaths() {
		return true;
	}

	@Override
	public boolean allowBees() {
		return true;
	}

}
