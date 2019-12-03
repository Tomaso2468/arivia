package arivia;

import java.util.Locale;

public interface Connection {
	public boolean hasNextLine();
	public String nextLine();
	public void out(String s);
	public String getUserName();
	public Locale getLocale();
	public void prompt();
	public boolean allowMaths();
	public boolean allowBees();
}
